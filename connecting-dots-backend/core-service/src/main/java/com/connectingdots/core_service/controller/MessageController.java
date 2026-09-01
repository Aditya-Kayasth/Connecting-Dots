package com.connectingdots.core_service.controller;

import com.connectingdots.core_service.dto.MessageRequest;
import com.connectingdots.core_service.dto.MessageRequestLegacy;
import com.connectingdots.core_service.entity.Message;
import com.connectingdots.core_service.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping("/api/v1/core/applications/{applicationId}/messages")
    public ResponseEntity<Message> sendMessage(
            @PathVariable UUID applicationId,
            @Valid @RequestBody MessageRequest request) {
        Message message = messageService.sendMessage(applicationId, null, request.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    @GetMapping("/api/v1/core/applications/{applicationId}/messages")
    public ResponseEntity<List<Message>> getMessagesForApplication(@PathVariable UUID applicationId) {
        List<Message> messages = messageService.getMessagesForApplication(applicationId);
        return ResponseEntity.ok(messages);
    }

    // --- Legacy / Frontend Compatibility Endpoints ---

    @PostMapping("/api/v1/core/messages")
    public ResponseEntity<Message> sendMessageLegacy(@Valid @RequestBody MessageRequestLegacy request) {
        Message message = messageService.sendMessage(request.applicationId(), null, request.message());
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    @GetMapping("/api/v1/core/messages/application/{applicationId}")
    public ResponseEntity<List<Message>> getMessagesForApplicationLegacy(@PathVariable UUID applicationId) {
        List<Message> messages = messageService.getMessagesForApplication(applicationId);
        return ResponseEntity.ok(messages);
    }
}
