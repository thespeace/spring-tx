package thespeace.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

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


    /**
     * <h1>스프링 트랜잭션 전파 예제 : 물리 트랜잭션 커밋</h1>
     * <ul>
     *     <li>외부 트랜잭션이 수행중인데, 내부 트랜잭션을 추가로 수행했다.</li>
     *     <li>외부 트랜잭션은 처음 수행된 트랜잭션이다. 이 경우 신규 트랜잭션( isNewTransaction=true )이 된다.</li>
     *     <li>내부 트랜잭션을 시작하는 시점에는 이미 외부 트랜잭션이 진행중인 상태이다. 이 경우 내부 트랜잭션은 외부
     *         트랜잭션에 참여한다.</li>
     *     <li>트랜잭션 참여
     *         <ul>
     *             <li>내부 트랜잭션이 외부 트랜잭션에 참여한다는 뜻은 내부 트랜잭션이 외부 트랜잭션을 그대로 이어 받아서
     *                 따른다는 뜻이다.</li>
     *             <li>다른 관점으로 보면 외부 트랜잭션의 범위가 내부 트랜잭션까지 넓어진다는 뜻이다.</li>
     *             <li>외부에서 시작된 물리적인 트랜잭션의 범위가 내부 트랜잭션까지 넓어진다는 뜻이다.</li>
     *             <li>정리하면 외부 트랜잭션과 내부 트랜잭션이 하나의 물리 트랜잭션으로 묶이는 것이다.</li>
     *         </ul>
     *     </li>
     *     <li>내부 트랜잭션은 이미 진행중인 외부 트랜잭션에 참여한다. 이 경우 신규 트랜잭션이 아니다
     *         ( isNewTransaction=false ).</li>
     *     <li>예제에서는 둘다 성공적으로 커밋했다.</li>
     * </ul><br>
     *
     * 이 예제에서는 외부 트랜잭션과 내부 트랜잭션이 하나의 물리 트랜잭션으로 묶인다고 설명했다.<br>
     * 그런데 코드를 잘 보면 커밋을 두 번 호출했다. 트랜잭션을 생각해보면 하나의 커넥션에 커밋은 한번만 호출할 수 있다.
     * 커밋이나 롤백을 하면 해당 트랜잭션은 끝나버린다.<p>
     * <pre>
     *     txManager.commit(inner);
     *     txManager.commit(outer);
     * </pre>
     *
     * 스프링은 어떻게 어떻게 외부 트랜잭션과 내부 트랜잭션을 묶어서 하나의 물리 트랜잭션으로 묶어서 동작하게 하는지는
     * 아래의 문서로 자세히 알아보자.
     *
     * @see docs/08.Spring_transaction_propagation_example-commit.md
     */
    @Test
    void inner_commit() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("outer.isNewTransaction() = {}", outer.isNewTransaction());

        log.info("내부 트랜잭션 시작");
        TransactionStatus inner = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("inner.isNewTransaction() = {}", inner.isNewTransaction());

        log.info("내부 트랜잭션 커밋");
        txManager.commit(inner);

        log.info("외부 트랜잭션 커밋");
        txManager.commit(outer);
    }

    /**
     * <h1>스프링 트랜잭션 전파 예제 : 외부 트랜잭션 롤백</h1>
     * 논리 트랜잭션이 하나라도 롤백되면 전체 물리 트랜잭션은 롤백된다.<br>
     * 따라서 이 경우 내부 트랜잭션이 커밋했어도, 내부 트랜잭션 안에서 저장한 데이터도 모두 함께 롤백된다.
     *
     * @see docs/09.Spring_transaction_propagation_example-outer_rollback.md
     */
    @Test
    void outer_rollback() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("내부 트랜잭션 시작");
        TransactionStatus inner = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("내부 트랜잭션 커밋");
        txManager.commit(inner);

        log.info("외부 트랜잭션 롤백");
        txManager.rollback(outer);
    }


    /**
     * <h1>스프링 트랜잭션 전파 예제 : 내부 트랜잭션 롤백</h1>
     * 내부 트랜잭션은 롤백되는데, 외부 트랜잭션이 커밋되는 상황을 알아보자.<br>
     * 실행 결과를 보면 마지막에 외부 트랜잭션을 커밋할 때 `UnexpectedRollbackException.class`이
     * 발생하는 것을 확인할 수 있다. 아래의 문서로 알아보자.
     *
     * @see docs/10.Spring_transaction_propagation_example-inner_rollback.md
     */
    @Test
    void inner_rollback() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("내부 트랜잭션 시작");
        TransactionStatus inner = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("내부 트랜잭션 롤백");
        txManager.rollback(inner);

        log.info("외부 트랜잭션 커밋");
        assertThatThrownBy(() -> txManager.commit(outer))
                .isInstanceOf(UnexpectedRollbackException.class);
    }

    /**
     * <h1>스프링 트랜잭션 전파 : REQUIRES_NEW</h1>
     * 외부 트랜잭션과 내부 트랜잭션을 완전히 분리해서 사용하는 방법에 대해서 알아보자.<br>
     * 외부 트랜잭션과 내부 트랜잭션을 완전히 분리해서 각각 별도의 물리 트랜잭션을 사용하는 방법이다. 그래서 커밋과
     * 롤백도 각각 별도로 이루어지게 된다.<br><br>
     *
     * 이 방법은 내부 트랜잭션에 문제가 발생해서 롤백해도, 외부 트랜잭션에는 영향을 주지 않는다. 반대로 외부 트랜잭션에
     * 문제가 발생해도 내부 트랜잭션에 영향을 주지 않는다.<br>
     *
     * <ul>
     *     <li>내부 트랜잭션을 시작할 때 전파 옵션인 propagationBehavior 에 PROPAGATION_REQUIRES_NEW
     *         옵션을 주었다.</li>
     *     <li>이 전파 옵션을 사용하면 내부 트랜잭션을 시작할 때 기존 트랜잭션에 참여하는 것이 아니라 새로운 물리
     *         트랜잭션을 만들어서 시작하게 된다.</li>
     * </ul>
     *
     * 해당 코드와 아래의 문서를 통해 작동원리를 중점적으로 이해해보자.
     *
     * @see docs/11.Spring_transaction_propagation_example-REQUIRES_NEW.md
     */
    @Test
    void inner_rollback_requires_new() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("outer.isNewTransaction()={}", outer.isNewTransaction());

        log.info("내부 트랜잭션 시작");
        DefaultTransactionAttribute definition = new DefaultTransactionAttribute();

        definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionStatus inner = txManager.getTransaction(definition);
        log.info("inner.isNewTransaction()={}", inner.isNewTransaction());

        log.info("내부 트랜잭션 롤백");
        txManager.rollback(inner); //롤백

        log.info("외부 트랜잭션 커밋");
        txManager.commit(outer); //커밋
    }
}
