package com.example.pftb.service;

import com.example.pftb.dto.TransactionDto;
import com.example.pftb.dto.TransactionRequest;
import com.example.pftb.entity.Transaction;
import com.example.pftb.entity.User;
import com.example.pftb.repository.TransactionRepository;
import com.example.pftb.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public TransactionService(TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> getTransactionsByUser(String username) {
        User user = getUserByUsername(username);
        return transactionRepository.findByUserId(user.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public TransactionDto createTransaction(String username, TransactionRequest request) {
        User user = getUserByUsername(username);

        Transaction transaction = Transaction.builder()
                .title(request.getTitle())
                .amount(request.getAmount())
                .type(request.getType())
                .category(request.getCategory())
                .date(request.getDate())
                .user(user)
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);
        return mapToDto(savedTransaction);
    }
    
    @Transactional
    public void deleteTransaction(String username, Long transactionId) {
        User user = getUserByUsername(username);
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to delete this transaction");
        }

        transactionRepository.delete(transaction);
    }

    @Transactional(readOnly = true)
    public void exportCsvToStream(String username, Writer writer) throws IOException {
        User user = getUserByUsername(username);
        writer.write("ID,Title,Amount,Type,Category,Date\n");
        try (Stream<Transaction> transactionStream = transactionRepository.findAllByUserId(user.getId())) {
            transactionStream.forEach(t -> {
                try {
                    writer.write(String.join(",",
                            t.getId().toString(),
                            escapeSpecialCharacters(t.getTitle()),
                            t.getAmount().toString(),
                            t.getType().toString(),
                            escapeSpecialCharacters(t.getCategory()),
                            t.getDate().toString()
                    ) + "\n");
                } catch (IOException e) {
                    throw new RuntimeException("Error writing CSV data", e);
                }
            });
        }
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private TransactionDto mapToDto(Transaction transaction) {
        return TransactionDto.builder()
                .id(transaction.getId())
                .title(transaction.getTitle())
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .category(transaction.getCategory())
                .date(transaction.getDate())
                .build();
    }

    private String escapeSpecialCharacters(String data) {
        if (data == null) {
            return "";
        }
        String escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "\"\"");
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }
}