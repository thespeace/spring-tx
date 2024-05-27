package thespeace.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
     * MemberService    @Transactional:OFF
     * MemberRepository @Transactional:ON
     * LogRepository    @Transactional:ON
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
}