package thespeace.springtx.order;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 스프링 데이터 JPA를 사용한다.
 */
public interface OrderRepository extends JpaRepository<Order, Long> {
}
