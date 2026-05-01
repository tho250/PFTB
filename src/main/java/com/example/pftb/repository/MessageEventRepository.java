package com.example.pftb.repository;

import com.example.pftb.entity.MessageDetectedType;
import com.example.pftb.entity.MessageEvent;
import com.example.pftb.entity.MessageEventReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MessageEventRepository extends JpaRepository<MessageEvent, Long> {
    List<MessageEvent> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<MessageEvent> findByUserIdAndReviewStatusOrderByCreatedAtDesc(Long userId, MessageEventReviewStatus reviewStatus);

    List<MessageEvent> findByUserIdAndDetectedTypeOrderByCreatedAtDesc(Long userId, MessageDetectedType detectedType);

    List<MessageEvent> findByUserIdAndMatchedOrderByCreatedAtDesc(Long userId, boolean matched);

    Optional<MessageEvent> findByIdAndUserId(Long id, Long userId);
}
