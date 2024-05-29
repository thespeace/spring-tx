package thespeace.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.UnexpectedRollbackException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>트랜잭션 전파 활용 - 예제 프로젝트 시작</h1>
 * 트랜잭션 전파에 대한 내용을 실제 예제를 통해서 이해해보자.<p>
 *
 * <h3>비즈니스 요구사항</h3>
 * <ul>
 *     <li>회원을 등록하고 조회한다.</li>
 *     <li>회원에 대한 변경 이력을 추적할 수 있도록 회원 데이터가 변경될 때 변경 이력을 DB LOG 테이블에 남겨야 한다.
 *         <ul>
 *             <li>여기서는 예제를 단순화 하기 위해 회원 등록시에만 DB LOG 테이블에 남긴다.</li>
 *         </ul>
 *     </li>
 * </ul>
 *
 * <h3>참고</h3>
 * <ul>
 *     <li>JPA의 구현체인 하이버네이트가 테이블을 자동으로 생성해준다.</li>
 *     <li>메모리 DB이기 때문에 모든 테스트가 완료된 이후에 DB는 사라진다.</li>
 *     <li>여기서는 각각의 테스트가 완료된 시점에 데이터를 삭제하지 않는다. 따라서 username 은 테스트별로 각각
 *         다르게 설정해야 한다. 그렇지 않으면 다음 테스트에 영향을 준다. (모든 테스트가 완료되어야 DB가 사라진다.)</li>
 * </ul><br>
 *
 * <h3>JPA와 데이터 변경</h3>
 * <ul>
 *     <li>JPA를 통한 모든 데이터 변경(등록, 수정, 삭제)에는 트랜잭션이 필요하다. (조회는 트랜잭션 없이 가능하다.)
 *         <ul>
 *             <li>현재 코드에서 서비스 계층에 트랜잭션이 없기 때문에 리포지토리에 트랜잭션이 있다.</li>
 *         </ul>
 *     </li>
 * </ul>
 */
@Slf4j
@SpringBootTest
class MemberServiceTest {

    @Autowired MemberService memberService;
    @Autowired MemberRepository memberRepository;
    @Autowired LogRepository logRepository;

    /**
     * <pre>
     * MemberService    @Transactional:OFF
     * MemberRepository @Transactional:ON
     * LogRepository    @Transactional:ON
     * </pre>
     */
    @Test
    void outerTxOff_success() {
        //given
        String username = "outerTxOff_success";

        //when
        memberService.joinV1(username);

        //then: 모든 데이터가 정상 저장된다.
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());
    }

    /**
     * <pre>
     * MemberService    @Transactional:OFF
     * MemberRepository @Transactional:ON
     * LogRepository    @Transactional:ON Exception
     * </pre>
     * <h2>서비스 계층에 트랜잭션이 없을 때 - 커밋, 롤백</h2>
     * <ul>
     *     <li>사용자 이름에 로그예외 라는 단어가 포함되어 있으면 LogRepository 에서 런타임 예외가 발생한다.</li>
     *     <li>트랜잭션 AOP는 해당 런타임 예외를 확인하고 롤백 처리한다.</li>
     * </ul><br>
     *
     * 이 경우 회원은 저장되지만, 회원 이력 로그는 롤백된다. 따라서 데이터 정합성에 문제가 발생할 수 있다.<br>
     * 둘을 하나의 트랜잭션으로 묶어서 처리해보자.<p><p>
     *
     * 참고 : 트랜잭션 AOP도 결국 내부에서는 트랜잭션 매니저를 사용하게 된다.
     */
    @Test
    void outerTxOff_fail() {
        //given
        String username = "로그예외_outerTxOff_fail";

        //when
        assertThatThrownBy(() -> memberService.joinV1(username))
                .isInstanceOf(RuntimeException.class);

        //then: 완전히 롤백되지 않고, member 데이터가 남아서 저장된다.
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isEmpty());
    }

    /**
     * <pre>
     * MemberService    @Transactional:ON
     * MemberRepository @Transactional:OFF
     * LogRepository    @Transactional:OFF
     * </pre>
     * <h2>트랜잭션 하나만 사용하기</h2>
     * 회원 리포지토리와 로그 리포지토리를 하나의 트랜잭션으로 묶는 가장 간단한 방법은 이 둘을 호출하는
     * 회원 서비스에만 트랜잭션을 사용하는 것이다.
     *
     * @see docs/13.Utilizing_spring_transaction_propagation-single_transaction.md
     */
    @Test
    void singleTx() {
        //given
        String username = "singleTx";

        //when
        memberService.joinV1(username);

        //then: 모든 데이터가 정상 저장된다.
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());
    }

    /**
     * <pre>
     * MemberService    @Transactional:ON
     * MemberRepository @Transactional:ON
     * LogRepository    @Transactional:ON
     * </pre>
     * <h2>트랜잭션 전파 활용 - 전파 커밋</h2>
     *
     * @see docs/14.Utilizing_spring_transaction_propagation-propagation_commit.md
     */
    @Test
    void outerTxOn_success() {
        //given
        String username = "outerTxOn_success";

        //when
        memberService.joinV1(username);

        //then: 모든 데이터가 정상 저장된다.
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());
    }

    /**
     * <pre>
     * MemberService    @Transactional:ON
     * MemberRepository @Transactional:ON
     * LogRepository    @Transactional:ON Exception
     * </pre>
     * <h2>트랜잭션 전파 활용 - 전파 롤백</h2>
     * LogRepository 에서 예외가 발생해서 전체 트랜잭션이 롤백되는 경우를 알아보자.
     *
     * @see docs/15.Utilizing_spring_transaction_propagation-propagation_rollback.md
     */
    @Test
    void outerTxOn_fail() {
        //given
        String username = "로그예외_outerTxOn_fail";

        //when
        assertThatThrownBy(() -> memberService.joinV1(username))
                .isInstanceOf(RuntimeException.class);

        //then: 모든 데이터가 롤백된다.
        assertTrue(memberRepository.find(username).isEmpty());
        assertTrue(logRepository.find(username).isEmpty());
    }

    /**
     * <pre>
     * MemberService    @Transactional:ON
     * MemberRepository @Transactional:ON
     * LogRepository    @Transactional:ON Exception
     * </pre>
     * <h2>트랜잭션 전파 활용 - 복구 REQUIRED</h2>
     *
     * @see docs/16.Utilizing_spring_transaction_propagation-recover_required.md
     */
    @Test
    void recoverException_fail() {
        //given
        String username = "로그예외_recoverException_fail";

        //when
        assertThatThrownBy(() -> memberService.joinV2(username))
                .isInstanceOf(UnexpectedRollbackException.class);

        //then: 모든 데이터가 롤백된다.
        assertTrue(memberRepository.find(username).isEmpty());
        assertTrue(logRepository.find(username).isEmpty());
    }

    /**
     * <pre>
     * MemberService    @Transactional:ON
     * MemberRepository @Transactional:ON
     * LogRepository    @Transactional(REQUIRES_NEW) Exception
     * </pre>
     * <h2>트랜잭션 전파 활용 - 복구 REQUIRES_NEW</h2>
     * 회원 가입을 시도한 로그를 남기는데 실패하더라도 회원 가입은 유지되어야 한다.<p><p>
     *
     * 이 요구사항을 만족하기 위해서 로그와 관련된 물리 트랜잭션을 별도로 분리해보자.<br>
     * 바로 REQUIRES_NEW 를 사용하는 것이다.(LogRepository - save())
     *
     * @see docs/17.Utilizing_spring_transaction_propagation-recover_requires_new.md
     */
    @Test
    void recoverException_success() {
        //given
        String username = "로그예외_recoverException_success";

        //when
        memberService.joinV2(username);

        //then: member 저장, log 롤백
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isEmpty());
    }
}