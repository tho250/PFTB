package com.example.pftb.controller;

import com.example.pftb.dto.MessageEventRequest;
import com.example.pftb.dto.MessageEventResponse;
import com.example.pftb.dto.MessageParsePreviewRequest;
import com.example.pftb.dto.MessageParsePreviewResponse;
import com.example.pftb.dto.TransactionDto;
import com.example.pftb.entity.MessageDetectedType;
import com.example.pftb.entity.MessageEventReviewStatus;
import com.example.pftb.service.MessageEventService;
import com.example.pftb.service.MessageParsingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/message-events")
public class MessageEventController {
    private final MessageParsingService messageParsingService;
    private final MessageEventService messageEventService;

    public MessageEventController(MessageParsingService messageParsingService, MessageEventService messageEventService) {
        this.messageParsingService = messageParsingService;
        this.messageEventService = messageEventService;
    }

    @PostMapping("/parse-preview")
    public ResponseEntity<MessageParsePreviewResponse> parsePreview(Authentication authentication,
                                                                    @Valid @RequestBody MessageParsePreviewRequest request) {
        return ResponseEntity.ok(messageParsingService.parsePreview(authentication.getName(), request.getRawText()));
    }

    @PostMapping
    public ResponseEntity<MessageEventResponse> createEvent(Authentication authentication, @Valid @RequestBody MessageEventRequest request) {
        return ResponseEntity.ok(messageEventService.createEvent(authentication.getName(), request));
    }

    @GetMapping
    public ResponseEntity<List<MessageEventResponse>> listEvents(Authentication authentication,
                                                                 @RequestParam(required = false) MessageEventReviewStatus reviewStatus,
                                                                 @RequestParam(required = false) MessageDetectedType detectedType,
                                                                 @RequestParam(required = false) Boolean matched) {
        return ResponseEntity.ok(messageEventService.listEvents(authentication.getName(), reviewStatus, detectedType, matched));
    }

    @PostMapping("/{id}/convert-to-transaction")
    public ResponseEntity<TransactionDto> convertToTransaction(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(messageEventService.convertToTransaction(authentication.getName(), id));
    }
}
