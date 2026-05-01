package com.example.pftb.repository;

import com.example.pftb.entity.MessageRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MessageRuleRepository extends JpaRepository<MessageRule, Long> {
    List<MessageRule> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<MessageRule> findByUserIdAndEnabledTrueOrderByCreatedAtDesc(Long userId);

    Optional<MessageRule> findByIdAndUserId(Long id, Long userId);
}
