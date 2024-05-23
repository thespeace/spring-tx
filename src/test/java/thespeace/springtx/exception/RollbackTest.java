package thespeace.springtx.exception;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h1>예외와 트랜잭션 커밋, 롤백 - 기본</h1>
 * 예외가 발생했는데, 내부에서 예외를 처리하지 못하고, 트랜잭션 범위( @Transactional가 적용된 AOP ) 밖으로
 * 예외를 던지면 어떻게 될까?<p><p>
 *
 * 1.예외 발생(Repository) -> 2.예외 던짐(Service) -> 3.예외 던짐(@Transactional AOP Proxy) ->
 *      4.예외와 트랜잭션 커밋,롤백 정책: 4_1.런타임 예외: 롤백 / 4_2.체크 예외: 커밋 -> 5.예외 던짐(Controller)
 * <p><p>
 *
 * 예외 발생시 스프링 트랜잭션 AOP는 예외의 종류에 따라 트랜잭션을 커밋하거나 롤백한다.
 * <ul>
 *     <li>언체크 예외인 RuntimeException , Error 와 그 하위 예외가 발생하면 트랜잭션을 롤백한다.</li>
 *     <li>체크 예외인 Exception 과 그 하위 예외가 발생하면 트랜잭션을 커밋한다.</li>
 *     <li>물론 정상 응답(리턴)하면 트랜잭션을 커밋한다.</li>
 * </ul>
 *
 * 실제 어떻게 동작하는지 확인해보자.
 */
@SpringBootTest
public class RollbackTest {

    @Autowired
    RollbackService service;

    @Test
    void runtimeException() {
        Assertions.assertThatThrownBy(() -> service.runtimeException())
                        .isInstanceOf(RuntimeException.class);
    }

    @Test
    void checkedException() {
        Assertions.assertThatThrownBy(() -> service.checkedException())
                .isInstanceOf(MyException.class);
    }

    @Test
    void rollbackFor() {
        Assertions.assertThatThrownBy(() -> service.rollbackFor())
                .isInstanceOf(MyException.class);
    }

    @TestConfiguration
    static class RollbackTestConfig {

        @Bean
        RollbackService rollbackService() {
            return new RollbackService();
        }
    }

    @Slf4j
    static class RollbackService {

        /**
         * <h2>런타임 예외 발생: 롤백</h2>
         * RuntimeException 이 발생하므로 트랜잭션이 롤백된다.
         */
        @Transactional
        public void runtimeException() {
            log.info("call runtimeException");
            throw new RuntimeException();
        }

        /**
         * <h2>체크 예외 발생: 커밋</h2>
         * MyException 은 Exception 을 상속받은 체크 예외이다. 따라서 예외가 발생해도 트랜잭션이 커밋된다.
         */
        @Transactional
        public void checkedException() throws MyException {
            log.info("call checkedException");
            throw new MyException();
        }

        /**
         * <h2>체크 예외 rollbackFor 지정: 롤백</h2>
         * <ul>
         *     <li>기본 정책과 무관하게 특정 예외를 강제로 롤백하고 싶으면 rollbackFor 를 사용하면 된다.
         *         (해당 예외의 자식도 포함된다.)</li>
         *     <li>rollbackFor = MyException.class 을 지정했기 때문에 MyException 이 발생하면 체크 예외이지만
         *         트랜잭션이 롤백된다.</li>
         * </ul><br>
         *
         * <h2>rollbackFor</h2>
         * 이 옵션을 사용하면 기본 정책에 추가로 어떤 예외가 발생할 때 롤백할 지 지정할 수 있다.
         */
        @Transactional(rollbackFor = MyException.class) //자식 타입도 롤백.
        public void rollbackFor() throws MyException {
            log.info("call rollbackFor");
            throw new MyException();
        }
    }

    static class MyException extends Exception {

    }
}
