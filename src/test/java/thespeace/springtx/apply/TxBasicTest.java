package thespeace.springtx.apply;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h1>트랜잭션 적용 확인</h1>
 * {@code @Transactional}을 통해 선언적 트랜잭션 방식을 사용하면 단순히 애노테이션 하나로 트랜잭션을 적용할 수 있다.
 * 그런데 이 기능은 트랜잭션 관련 코드가 눈에 보이지 않고, AOP를 기반으로 동작하기 때문에, 실제 트랜잭션이 적용되고
 * 있는지 아닌지를 확인하기가 어렵다.<p><p>
 *
 * 스프링 트랜잭션이 실제 적용되고 있는지 확인하는 방법을 알아보자.
 */
@Slf4j
@SpringBootTest
public class TxBasicTest {

    @Autowired BasicService basicService;

    /**
     * AopUtils.isAopProxy() : 선언적 트랜잭션 방식에서 스프링 트랜잭션은 AOP를 기반으로 동작한다.
     *         {@code @Transactional}을 메서드나 클래스에 붙이면 해당 객체는 트랜잭션 AOP 적용의 대상이 되고, 결과적으로
     *         실제 객체 대신에 트랜잭션을 처리해주는 프록시 객체가 스프링 빈에 등록된다. 그리고 주입을 받을 때도 실제
     *         객체 대신에 프록시 객체가 주입된다.
     */
    @Test
    void proxyCheck() {
        //프록시 클래스의 이름이 출력, BasicService$$EnhancerBySpringCGLIB...
        log.info("aop class={}", basicService.getClass());
        assertThat(AopUtils.isAopProxy(basicService)).isTrue();
    }

    @Test
    void txTest() {
        basicService.tx();
        basicService.nonTx();
    }

    @TestConfiguration
    static class TxApplyBasicConfig{
        @Bean
        BasicService basicService() {
            return new BasicService();
        }
    }

    @Slf4j
    static class BasicService {

        /**
         * <ul>
         *     <li>클라이언트가 basicService.tx() 를 호출하면, 프록시의 tx() 가 호출된다. 여기서 프록시는 tx() 메서드가
         *         트랜잭션을 사용할 수 있는지 확인해본다. tx() 메서드에는 @Transactional 이 붙어있으므로 트랜잭션 적용 대상이다.</li>
         *     <li>따라서 트랜잭션을 시작한 다음에 실제 basicService.tx() 를 호출한다.</li>
         *     <li>그리고 실제 basicService.tx() 의 호출이 끝나서 프록시로 제어가(리턴) 돌아오면 프록시는 트랜잭션 로직을
         *         커밋하거나 롤백해서 트랜잭션을 종료한다.</li>
         * </ul>
         */
        @Transactional
        public void tx() {
            log.info("call tx");

            //TransactionSynchronizationManager.isActualTransactionActive()
            //현재 쓰레드에 트랜잭션이 적용되어 있는지 확인할 수 있는 기능, 트랜잭션의 적용 여부를 가장 확실하게 확인할 수 있다.
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("tx active={}", txActive);
        }

        /**
         * <ul>
         *     <li>클라이언트가 basicService.nonTx() 를 호출하면, 트랜잭션 프록시의 nonTx() 가 호출된다. 여기서
         *         nonTx() 메서드가 트랜잭션을 사용할 수 있는지 확인해본다. nonTx() 에는 @Transactional 이 없으므로
         *         적용 대상이 아니다.</li>
         *     <li>따라서 트랜잭션을 시작하지 않고, basicService.nonTx() 를 호출하고 종료한다.</li>
         * </ul>
         */
        public void nonTx() {
            log.info("call tx");
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("tx active={}", txActive);
        }
    }
}
