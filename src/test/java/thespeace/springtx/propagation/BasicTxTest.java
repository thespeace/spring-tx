package thespeace.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

import javax.sql.DataSource;

/**
 * <h1>스프링 트랜잭션 전파1 - 커밋, 롤백</h1>
 * 트랜잭션이 둘 이상 있을 때 어떻게 동작하는지 자세히 알아보고, 스프링이 제공하는 트랜잭션 전파(propagation)라는
 * 개념도 알아보자.<br>
 * 트랜잭션 전파를 이해하는 과정을 통해서 스프링 트랜잭션의 동작 원리도 더 깊이있게 이해할 수 있을 것이다.<p><p>
 *
 * 먼저 간단한 스프링 트랜잭션 코드를 통해 기본 원리를 학습하고, 이후에 실제 예제를 통해 어떻게 활용하는지 알아보자.
 */
@Slf4j
@SpringBootTest
public class BasicTxTest {

    @Autowired
    PlatformTransactionManager txManager;

    /**
     * <h2>해당 테스트에서 필요한 스프링 설정 추가</h2>
     * DataSourceTransactionManager를 스프링 빈으로 등록.<br>
     * 이후 트랜잭션 매니저인 PlatformTransactionManager를 주입 받으면 방금 등록한
     * DataSourceTransactionManager가 주입.
     */
    @TestConfiguration
    static class Config {

        @Bean
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }

    @Test
    void commit() {
        log.info("트랜잭션 시작");
        //트랜잭션 매니저를 통해 트랜잭션을 시작(획득).
        TransactionStatus status = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("트랜잭션 커밋 시작");
        txManager.commit(status);
        log.info("트랜잭션 커밋 완료");
    }

    @Test
    void rollback() {
        log.info("트랜잭션 시작");
        //트랜잭션 매니저를 통해 트랜잭션을 시작(획득).
        TransactionStatus status = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("트랜잭션 롤백 시작");
        txManager.rollback(status);
        log.info("트랜잭션 롤백 완료");
    }

    /**
     * <h1>스프링 트랜잭션 전파2 - 트랜잭션 두 번 사용</h1>
     * 로그를 보면 트랜잭션1과 트랜잭션2가 같은 conn0 커넥션을 사용중이다. 이것은 중간에 커넥션 풀 때문에 그런 것이다.
     * 트랜잭션1은 conn0 커넥션을 모두 사용하고 커넥션 풀에 반납까지 완료했다. 이후에 트랜잭션2가 conn0 를 커넥션
     * 풀에서 획득한 것이다. 따라서 둘은 완전히 다른 커넥션으로 인지하는 것이 맞다.<p><p>
     *
     * 그렇다면 둘을 구분할 수 있는 다른 방법은 없을까?<p><p>
     *
     * 히카리 커넥션 풀에서 커넥션을 획득하면 실제 커넥션을 그대로 반환하는 것이 아니라 내부 관리를 위해 히카리 프록시
     * 커넥션이라는 객체를 생성해서 반환한다. 물론 내부에는 실제 커넥션이 포함되어 있다. 이 객체의 주소를 확인하면 커넥션
     * 풀에서 획득한 커넥션을 구분할 수 있다.<p><p>
     *
     * <ul>
     *     <li>트랜잭션1: Acquired Connection [HikariProxyConnection@1000000 wrapping conn0]</li>
     *     <li>트랜잭션2: Acquired Connection [HikariProxyConnection@2000000 wrapping conn0]</li>
     * </ul>
     *
     * 히카리 커넥션풀이 반환해주는 커넥션을 다루는 프록시 객체의 주소가 트랜잭션1은
     * HikariProxyConnection@1000000 이고, 트랜잭션2는 HikariProxyConnection@2000000 으로 서로 다
     * 른 것을 확인할 수 있다.<br>
     * 결과적으로 conn0 을 통해 커넥션이 재사용 된 것을 확인할 수 있고, HikariProxyConnection@1000000,
     * HikariProxyConnection@2000000 을 통해 각각 커넥션 풀에서 커넥션을 조회한 것을 확인할 수 있다.
     */
    @Test
    void double_commit() {
        log.info("트랜잭션1 시작");
        TransactionStatus tx1 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션1 커밋");
        txManager.commit(tx1);

        log.info("트랜잭션2 시작");
        TransactionStatus tx2 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션2 커밋");
        txManager.commit(tx2);
    }

    /**
     * 커밋과 롤백 트랜잭션을 사용해도 위와 마찬가지로 결국 다른 커넥션을 쓴다.<p><p>
     *
     * 그렇다면 트랜잭션을 이미 하고 있는데 그 안에서 또 트랜잭션이 발생하면 어떻게 될까?
     */
    @Test
    void double_commit_rollback() {
        log.info("트랜잭션1 시작");
        TransactionStatus tx1 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션1 커밋");
        txManager.commit(tx1);

        log.info("트랜잭션2 시작");
        TransactionStatus tx2 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션2 롤백");
        txManager.rollback(tx2);
    }
}
