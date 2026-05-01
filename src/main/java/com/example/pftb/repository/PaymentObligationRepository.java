package com.example.pftb.repository;

import com.example.pftb.entity.PaymentObligation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentObligationRepository extends JpaRepository<PaymentObligation, Long> {
    List<PaymentObligation> findByUserIdOrderByDueDateAsc(Long userId);

    Optional<PaymentObligation> findByIdAndUserId(Long id, Long userId);
}
