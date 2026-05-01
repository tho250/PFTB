package com.example.pftb.service;

import com.example.pftb.dto.MessageEventRequest;
import com.example.pftb.dto.MessageEventResponse;
import com.example.pftb.dto.TransactionDto;
import com.example.pftb.dto.TransactionRequest;
import com.example.pftb.entity.MessageDetectedType;
import com.example.pftb.entity.MessageEvent;
import com.example.pftb.entity.MessageEventReviewStatus;
import com.example.pftb.entity.TransactionType;
import com.example.pftb.entity.User;
import com.example.pftb.repository.MessageEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class MessageEventService {
    private final MessageEventRepository messageEventRepository;
    private final UserContextService userContextService;
    private final MessageParsingService messageParsingService;
    private final TransactionService transactionService;

    public MessageEventService(MessageEventRepository messageEventRepository,
                               UserContextService userContextService,
                               MessageParsingService messageParsingService,
                               TransactionService transactionService) {
        this.messageEventRepository = messageEventRepository;
        this.userContextService = userContextService;
        this.messageParsingService = messageParsingService;
        this.transactionService = transactionService;
    }

    @Transactional
    public MessageEventResponse createEvent(String username, MessageEventRequest request) {
        User user = userContextService.getUserByUsername(username);
        MessageDetectedType detectedType = request.getDetectedType() == null ? MessageDetectedType.UNKNOWN : request.getDetectedType();
        MessageEvent event = MessageEvent.builder()
                .user(user)
                .rawText(request.getRawText())
                .sourceType(request.getSourceType())
                .matched(request.isMatched())
                .matchedRuleId(request.getMatchedRuleId())
                .detectedType(detectedType)
                .amount(request.getAmount())
                .currency(request.getCurrency() == null || request.getCurrency().isBlank() ? "RWF" : request.getCurrency())
                .counterparty(request.getCounterparty())
                .eventDate(request.getEventDate())
                .category(request.getCategory())
                .confidenceScore(request.getConfidenceScore())
                .reviewStatus(request.getReviewStatus() == null ? MessageEventReviewStatus.PENDING : request.getReviewStatus())
                .linkedPaymentObligationId(request.getLinkedPaymentObligationId())
                .build();
        return mapToResponse(messageEventRepository.save(event));
    }

    @Transactional(readOnly = true)
    public List<MessageEventResponse> listEvents(String username,
                                                 MessageEventReviewStatus reviewStatus,
                                                 MessageDetectedType detectedType,
                                                 Boolean matched) {
        User user = userContextService.getUserByUsername(username);
        List<MessageEvent> events;
        if (reviewStatus != null) {
            events = messageEventRepository.findByUserIdAndReviewStatusOrderByCreatedAtDesc(user.getId(), reviewStatus);
        } else if (detectedType != null) {
            events = messageEventRepository.findByUserIdAndDetectedTypeOrderByCreatedAtDesc(user.getId(), detectedType);
        } else if (matched != null) {
            events = messageEventRepository.findByUserIdAndMatchedOrderByCreatedAtDesc(user.getId(), matched);
        } else {
            events = messageEventRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        }
        return events.stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public TransactionDto convertToTransaction(String username, Long eventId) {
        User user = userContextService.getUserByUsername(username);
        MessageEvent event = messageEventRepository.findByIdAndUserId(eventId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Message event not found"));
        if (event.getAmount() == null || event.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Message event amount is required for conversion");
        }
        TransactionType transactionType = switch (event.getDetectedType()) {
            case INCOME, PAYMENT_RECEIVED, LOAN_RECEIVED -> TransactionType.INCOME;
            case EXPENSE, PAYMENT_SENT, LOAN_GIVEN, UNKNOWN -> TransactionType.EXPENSE;
        };
        TransactionRequest request = TransactionRequest.builder()
                .title(event.getCounterparty() == null ? "Message Event" : "Message: " + event.getCounterparty())
                .amount(event.getAmount())
                .type(transactionType)
                .category(event.getCategory() == null || event.getCategory().isBlank() ? "Parsed Message" : event.getCategory())
                .date(event.getEventDate() == null ? LocalDate.now() : event.getEventDate())
                .build();
        TransactionDto transaction = transactionService.createTransactionForUser(user, request);
        event.setReviewStatus(MessageEventReviewStatus.CONVERTED);
        event.setLinkedTransactionId(transaction.getId());
        messageEventRepository.save(event);
        return transaction;
    }

    @Transactional(readOnly = true)
    public MessageEventRequest parseAndBuildEventRequest(String username, String rawText, com.example.pftb.entity.MessageSourceType sourceType) {
        MessageParsingService.ParseResult result = messageParsingService.parse(username, rawText);
        MessageEventRequest request = new MessageEventRequest();
        request.setRawText(rawText);
        request.setSourceType(sourceType);
        request.setMatched(result.matched());
        request.setMatchedRuleId(result.matchedRuleId());
        request.setDetectedType(result.detectedType());
        request.setAmount(result.amount());
        request.setCurrency(result.currency());
        request.setCounterparty(result.counterparty());
        request.setCategory(result.category());
        request.setConfidenceScore(result.confidenceScore());
        request.setReviewStatus(MessageEventReviewStatus.PENDING);
        return request;
    }

    private MessageEventResponse mapToResponse(MessageEvent event) {
        return MessageEventResponse.builder()
                .id(event.getId())
                .rawText(event.getRawText())
                .sourceType(event.getSourceType())
                .matched(event.isMatched())
                .matchedRuleId(event.getMatchedRuleId())
                .detectedType(event.getDetectedType())
                .amount(event.getAmount())
                .currency(event.getCurrency())
                .counterparty(event.getCounterparty())
                .eventDate(event.getEventDate())
                .category(event.getCategory())
                .confidenceScore(event.getConfidenceScore())
                .reviewStatus(event.getReviewStatus())
                .linkedTransactionId(event.getLinkedTransactionId())
                .linkedPaymentObligationId(event.getLinkedPaymentObligationId())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
