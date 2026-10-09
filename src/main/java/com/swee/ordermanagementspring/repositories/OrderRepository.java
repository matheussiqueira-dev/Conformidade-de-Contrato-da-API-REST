package com.swee.ordermanagementspring.repositories;

import com.swee.ordermanagementspring.entities.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /** D019: SELECT ... FOR UPDATE; quem altera o status do pedido segura esta linha ate o commit. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findWithLockById(Long id);
}
