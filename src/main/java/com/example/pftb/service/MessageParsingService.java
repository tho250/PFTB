package com.example.pftb.service;

import com.example.pftb.dto.MessageParsePreviewResponse;
import com.example.pftb.entity.MessageDetectedType;
import com.example.pftb.entity.MessageDirectionType;
import com.example.pftb.entity.MessageRule;
import com.example.pftb.entity.User;
import com.example.pftb.repository.MessageRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MessageParsingService {
    private static final Pattern DEFAULT_AMOUNT_PATTERN = Pattern.compile("(\\d{1,3}(?:[\\s,]\\d{3})+|\\d+)(?:\\.\\d+)?\\s*(RWF|USD|EUR)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern DEFAULT_COUNTERPARTY_PATTERN = Pattern.compile("\\b(?:from|to)\\s+([A-Za-z][A-Za-z\\s]+)", Pattern.CASE_INSENSITIVE);

    private final MessageRuleRepository messageRuleRepository;
    private final UserContextService userContextService;

    public MessageParsingService(MessageRuleRepository messageRuleRepository, UserContextService userContextService) {
        this.messageRuleRepository = messageRuleRepository;
        this.userContextService = userContextService;
    }

    @Transactional(readOnly = true)
    public MessageParsePreviewResponse parsePreview(String username, String rawText) {
        ParseResult result = parse(username, rawText);
        return MessageParsePreviewResponse.builder()
                .matched(result.matched())
                .detectedType(result.detectedType())
                .amount(result.amount())
                .currency(result.currency())
                .counterparty(result.counterparty())
                .category(result.category())
                .matchedRuleId(result.matchedRuleId())
                .confidenceScore(result.confidenceScore())
                .build();
    }

    @Transactional(readOnly = true)
    public ParseResult parse(String username, String rawText) {
        User user = userContextService.getUserByUsername(username);
        List<MessageRule> rules = messageRuleRepository.findByUserIdAndEnabledTrueOrderByCreatedAtDesc(user.getId());
        String lowered = rawText.toLowerCase(Locale.ROOT);

        Optional<MessageRule> matchedRule = rules.stream()
                .filter(rule -> rule.getKeywords().stream().anyMatch(keyword -> lowered.contains(keyword.toLowerCase(Locale.ROOT))))
                .min(Comparator.comparing((MessageRule r) -> r.getPriority() == null ? "zzz" : r.getPriority())
                        .thenComparing(MessageRule::getId));

        if (matchedRule.isEmpty()) {
            BigDecimal fallbackAmount = extractAmount(rawText, null);
            return new ParseResult(
                    false,
                    MessageDetectedType.UNKNOWN,
                    fallbackAmount,
                    "RWF",
                    extractCounterparty(rawText, null),
                    "Parsed Message",
                    null,
                    fallbackAmount != null ? 0.35d : 0.15d
            );
        }

        MessageRule rule = matchedRule.get();
        BigDecimal amount = extractAmount(rawText, rule.getAmountRegex());
        String counterparty = extractCounterparty(rawText, rule.getCounterpartyRegex());
        double confidence = 0.70d;
        if (amount != null) {
            confidence += 0.15d;
        }
        if (counterparty != null) {
            confidence += 0.10d;
        }
        if (confidence > 0.95d) {
            confidence = 0.95d;
        }

        return new ParseResult(
                true,
                mapDirection(rule.getDirectionType()),
                amount,
                "RWF",
                counterparty,
                rule.getCategory() == null || rule.getCategory().isBlank() ? "Parsed Message" : rule.getCategory(),
                rule.getId(),
                confidence
        );
    }

    private MessageDetectedType mapDirection(MessageDirectionType directionType) {
        return switch (directionType) {
            case INCOME -> MessageDetectedType.INCOME;
            case EXPENSE -> MessageDetectedType.EXPENSE;
            case LOAN_GIVEN -> MessageDetectedType.LOAN_GIVEN;
            case LOAN_RECEIVED -> MessageDetectedType.LOAN_RECEIVED;
            case PAYMENT_RECEIVED -> MessageDetectedType.PAYMENT_RECEIVED;
            case PAYMENT_SENT -> MessageDetectedType.PAYMENT_SENT;
        };
    }

    private BigDecimal extractAmount(String rawText, String customRegex) {
        Matcher matcher = (customRegex != null && !customRegex.isBlank()
                ? Pattern.compile(customRegex, Pattern.CASE_INSENSITIVE)
                : DEFAULT_AMOUNT_PATTERN)
                .matcher(rawText);
        if (!matcher.find()) {
            return null;
        }
        String raw = matcher.group(1);
        if (raw == null) {
            return null;
        }
        return new BigDecimal(raw.replace(",", "").replace(" ", ""));
    }

    private String extractCounterparty(String rawText, String customRegex) {
        Matcher matcher = (customRegex != null && !customRegex.isBlank()
                ? Pattern.compile(customRegex, Pattern.CASE_INSENSITIVE)
                : DEFAULT_COUNTERPARTY_PATTERN)
                .matcher(rawText);
        if (!matcher.find()) {
            return null;
        }
        String value = matcher.group(1);
        return value == null ? null : value.trim();
    }

    public record ParseResult(
            boolean matched,
            MessageDetectedType detectedType,
            BigDecimal amount,
            String currency,
            String counterparty,
            String category,
            Long matchedRuleId,
            Double confidenceScore
    ) {
    }
}
