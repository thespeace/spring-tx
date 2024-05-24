package thespeace.springtx.order;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>예외와 트랜잭션 커밋, 롤백 - 활용</h1>
 * 스프링은 왜 체크 예외는 커밋하고, 언체크(런타임) 예외는 롤백할까?<br>
 * 스프링 기본적으로 체크 예외는 비즈니스 의미가 있을 때 사용하고, 런타임(언체크) 예외는 복구 불가능한 예외로 가정한다.<p>
 * <ul>
 *     <li>체크 예외: 비즈니스 의미가 있을 때 사용</li>
 *     <li>언체크 예외: 복구 불가능한 예외</li>
 * </ul>
 * 참고로 꼭 이런 정책을 따를 필요는 없다. 그때는 앞서 배운 rollbackFor 라는 옵션을 사용해서 체크 예외도 롤백하면 된다.<p><p>
 *
 * 그런데 비즈니스 의미가 있는 비즈니스 예외라는 것이 무슨 뜻일까? 간단한 예제로 알아보자.<p><p>
 *
 * <h2>비즈니스 요구사항</h2>
 * 주문을 하는데 상황에 따라 다음과 같이 조치한다.
 * <ol>
 *     <li>정상: 주문시 결제를 성공하면 주문 데이터를 저장하고 결제 상태를 완료 로 처리한다.</li>
 *     <li>시스템 예외: 주문시 내부에 복구 불가능한 예외가 발생하면 전체 데이터를 롤백한다.</li>
 *     <li>비즈니스 예외: 주문시 결제 잔고가 부족하면 주문 데이터를 저장하고, 결제 상태를 대기 로 처리한다.<br>
 *         이 경우 고객에게 잔고 부족을 알리고 별도의 계좌로 입금하도록 안내한다.</li>
 * </ol><br>
 * 이때 결제 잔고가 부족하면 NotEnoughMoneyException 이라는 체크 예외가 발생한다고 가정하겠다.
 * 이 예외는 시스템에 문제가 있어서 발생하는 시스템 예외가 아니다.
 * 시스템은 정상 동작했지만, 비즈니스 상황에서 문제가 되기 때문에 발생한 예외이다.
 * 더 자세히 설명하자면, 고객의 잔고가 부족한 것은 시스템에 문제가 있는 것이 아니다.
 * 오히려 시스템은 문제 없이 동작한 것이고, 비즈니스 상황이 예외인 것이다.
 * 이런 예외를 비즈니스 예외라 한다.
 * 그리고 비즈니스 예외는 매우 중요하고, 반드시 처리해야 하는 경우가 많으므로 체크 예외를 고려할 수 있다.<p><p>
 *
 * <h2>정리</h2>
 * <ul>
 *     <li>NotEnoughMoneyException 은 시스템에 문제가 발생한 것이 아니라, 비즈니스 문제 상황을 예외를 통해 알려준다.
 *         마치 예외가 리턴 값 처럼 사용된다. 따라서 이 경우에는 트랜잭션을 커밋하는 것이 맞다.
 *         이 경우 롤백하면 생성한 Order 자체가 사라진다. 그러면 고객에게 잔고 부족을 알리고 별도의 계좌로 입금하도록
 *         안내해도 주문( Order ) 자체가 사라지기 때문에 문제가 된다.</li>
 *     <li>그런데 비즈니스 상황에 따라 체크 예외의 경우에도 트랜잭션을 커밋하지 않고, 롤백하고 싶을 수 있다.
 *         이때는 rollbackFor 옵션을 사용하면 된다.</li>
 *     <li>런타임 예외는 항상 롤백된다. 체크 예외의 경우 rollbackFor 옵션을 사용해서 비즈니스 상황에 따라서 커밋과
 *         롤백을 선택하면 된다.</li>
 * </ul>
 */
@Slf4j
@SpringBootTest
class OrderServiceTest {

    @Autowired OrderService orderService;
    @Autowired OrderRepository orderRepository;

    /**
     * 사용자 이름을 정상 으로 설정했다. 모든 프로세스가 정상 수행된다.
     */
    @Test
    void complete() throws NotEnoughMoneyException {
        //given
        Order order = new Order();
        order.setUsername("정상");

        //when
        orderService.order(order);

        //then
        Order findOrder = orderRepository.findById(order.getId()).get();
        assertThat(findOrder.getPayStatus()).isEqualTo("완료"); //데이터가 완료 상태로 저장 되었는지 검증.
    }

    /**
     * 사용자 이름을 예외 로 설정했다.<br>
     * RuntimeException("시스템 예외") 이 발생한다.<br>
     * 런타임 예외로 롤백이 수행되었기 때문에 Order 데이터가 비어 있는 것을 확인할 수 있다.
     */
    @Test
    void runtimeException() throws NotEnoughMoneyException {
        //given
        Order order = new Order();
        order.setUsername("예외");

        //when
        Assertions.assertThatThrownBy(() -> orderService.order(order))
                        .isInstanceOf(RuntimeException.class);

        //then
        Optional<Order> orderOptional = orderRepository.findById(order.getId());
        assertThat(orderOptional.isEmpty()).isTrue();
    }

    /**
     * 사용자 이름을 잔고부족 으로 설정했다.<br>
     * NotEnoughMoneyException("잔고가 부족합니다") 이 발생한다.<br>
     * 체크 예외로 커밋이 수행되었기 때문에 Order 데이터가 저장된다.
     */
    @Test
    void bizException() {
        //given
        Order order = new Order();
        order.setUsername("잔고부족");

        //when
        try {
            orderService.order(order);
        } catch (NotEnoughMoneyException e) {
            log.info("고객에게 잔고 부족을 알리고 별도의 계좌로 입금하도록 안내");
        }

        //then
        Order findOrder = orderRepository.findById(order.getId()).get();
        assertThat(findOrder.getPayStatus()).isEqualTo("대기"); //데이터가 대기 상태로 잘 저장 되었는지 검증.
    }
}