package thespeace.springtx.order;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <ul>
 *     <li>여러 상황을 만들기 위해서 사용자 이름( username )에 따라서 처리 프로세스를 다르게 했다.
 *         <ul>
 *             <li>기본 : payStatus 를 완료 상태로 처리하고 정상 처리된다.</li>
 *             <li>예외 : RuntimeException("시스템 예외") 런타임 예외가 발생한다.</li>
 *             <li>잔고부족 :
 *                 <ul>
 *                     <li>payStatus 를 대기 상태로 처리한다.</li>
 *                     <li>NotEnoughMoneyException("잔고가 부족합니다") 체크 예외가 발생한다.</li>
 *                     <li>잔고 부족은 payStatus 를 대기 상태로 두고, 체크 예외가 발생하지만, order 데이터는 커밋되기를
 *                         기대한다.</li>
 *                 </ul>
 *             </li>
 *         </ul>
 *     </li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    //JPA는 트랜잭션 커밋 시점에 Order 데이터를 DB에 반영한다.
    @Transactional
    public void order(Order order) throws NotEnoughMoneyException {
        log.info("order 호출");
        orderRepository.save(order);

        log.info("결제 프로세스 진입");
        if(order.getUsername().equals("예외")) {
            log.info("시스템 예외 발생");
            throw new RuntimeException("시스템 예외");

        } else if (order.getUsername().equals("잔고부족")) {
            log.info("잔고 부족 비즈니스 예외 발생");
            order.setPayStatus("대기");
            throw new NotEnoughMoneyException("잔고가 부족합니다.");

        } else {
            //정상 승인
            log.info("정상 승인");
            order.setPayStatus("완료");
        }
        log.info("결제 프로세스 완료");
    }
}
