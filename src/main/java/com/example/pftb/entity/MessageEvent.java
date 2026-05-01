package com.example.pftb.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "message_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 4000)
    private String rawText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageSourceType sourceType;

    @Column(nullable = false)
    private boolean matched;

    private Long matchedRuleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageDetectedType detectedType;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    private String currency;

    private String counterparty;

    private LocalDate eventDate;

    private String category;

    private Double confidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageEventReviewStatus reviewStatus;

    private Long linkedTransactionId;

    private Long linkedPaymentObligationId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
