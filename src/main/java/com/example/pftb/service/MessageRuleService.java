package com.example.pftb.service;

import com.example.pftb.dto.MessageRuleRequest;
import com.example.pftb.dto.MessageRuleResponse;
import com.example.pftb.entity.MessageRule;
import com.example.pftb.entity.User;
import com.example.pftb.repository.MessageRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class MessageRuleService {
    private final MessageRuleRepository messageRuleRepository;
    private final UserContextService userContextService;

    public MessageRuleService(MessageRuleRepository messageRuleRepository, UserContextService userContextService) {
        this.messageRuleRepository = messageRuleRepository;
        this.userContextService = userContextService;
    }

    @Transactional(readOnly = true)
    public List<MessageRuleResponse> getRules(String username) {
        User user = userContextService.getUserByUsername(username);
        return messageRuleRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public MessageRuleResponse createRule(String username, MessageRuleRequest request) {
        User user = userContextService.getUserByUsername(username);
        MessageRule rule = MessageRule.builder()
                .user(user)
                .name(request.getName().trim())
                .enabled(request.isEnabled())
                .directionType(request.getDirectionType())
                .keywords(new ArrayList<>(request.getKeywords().stream().map(String::trim).filter(s -> !s.isEmpty()).toList()))
                .amountRegex(request.getAmountRegex())
                .counterpartyRegex(request.getCounterpartyRegex())
                .category(request.getCategory())
                .priority(request.getPriority())
                .build();
        if (rule.getKeywords().isEmpty()) {
            throw new IllegalArgumentException("at least one keyword is required");
        }
        return mapToResponse(messageRuleRepository.save(rule));
    }

    @Transactional
    public MessageRuleResponse updateRule(String username, Long id, MessageRuleRequest request) {
        User user = userContextService.getUserByUsername(username);
        MessageRule rule = messageRuleRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Message rule not found"));
        List<String> keywords = new ArrayList<>(request.getKeywords().stream().map(String::trim).filter(s -> !s.isEmpty()).toList());
        if (keywords.isEmpty()) {
            throw new IllegalArgumentException("at least one keyword is required");
        }
        rule.setName(request.getName().trim());
        rule.setEnabled(request.isEnabled());
        rule.setDirectionType(request.getDirectionType());
        rule.setKeywords(keywords);
        rule.setAmountRegex(request.getAmountRegex());
        rule.setCounterpartyRegex(request.getCounterpartyRegex());
        rule.setCategory(request.getCategory());
        rule.setPriority(request.getPriority());
        return mapToResponse(messageRuleRepository.save(rule));
    }

    @Transactional
    public void deleteRule(String username, Long id) {
        User user = userContextService.getUserByUsername(username);
        MessageRule rule = messageRuleRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Message rule not found"));
        messageRuleRepository.delete(rule);
    }

    private MessageRuleResponse mapToResponse(MessageRule rule) {
        return MessageRuleResponse.builder()
                .id(rule.getId())
                .name(rule.getName())
                .enabled(rule.isEnabled())
                .directionType(rule.getDirectionType())
                .keywords(rule.getKeywords())
                .amountRegex(rule.getAmountRegex())
                .counterpartyRegex(rule.getCounterpartyRegex())
                .category(rule.getCategory())
                .priority(rule.getPriority())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }
}
