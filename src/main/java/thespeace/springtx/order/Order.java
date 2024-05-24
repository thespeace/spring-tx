package thespeace.springtx.order;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * <ul>
 *     <li>JPA를 사용하는 Order 엔티티이다.</li>
 *     <li>예제를 단순하게 하기 위해 @Getter , @Setter 를 사용했다. 참고로 실무에서 엔티티에 @Setter를
 *         남발해서 불필요한 변경 포인트를 노출하는 것은 좋지 않다.</li>
 *     <li>주의! @Table(name = "orders") 라고 했는데, 테이블 이름을 지정하지 않으면 테이블 이름이 클래스 이름인
 *         order 가 된다. order 는 데이터베이스 예약어( order by )여서 사용할 수 없다. 그래서 orders 라는
 *         테이블 이름을 따로 지정해주었다.</li>
 * </ul>
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order {

    @Id
    @GeneratedValue
    private Long id;

    private String username; //정상, 예외, 잔고부족
    private String payStatus; //대기, 완료
}
