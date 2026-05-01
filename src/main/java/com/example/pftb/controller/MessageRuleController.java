package com.example.pftb.controller;

import com.example.pftb.dto.MessageRuleRequest;
import com.example.pftb.dto.MessageRuleResponse;
import com.example.pftb.service.MessageRuleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/message-rules")
public class MessageRuleController {
    private final MessageRuleService messageRuleService;

    public MessageRuleController(MessageRuleService messageRuleService) {
        this.messageRuleService = messageRuleService;
    }

    @GetMapping
    public ResponseEntity<List<MessageRuleResponse>> list(Authentication authentication) {
        return ResponseEntity.ok(messageRuleService.getRules(authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<MessageRuleResponse> create(Authentication authentication, @Valid @RequestBody MessageRuleRequest request) {
        return ResponseEntity.ok(messageRuleService.createRule(authentication.getName(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MessageRuleResponse> update(Authentication authentication, @PathVariable Long id, @Valid @RequestBody MessageRuleRequest request) {
        return ResponseEntity.ok(messageRuleService.updateRule(authentication.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long id) {
        messageRuleService.deleteRule(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
