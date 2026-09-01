package com.connectingdots.core_service.controller;

import com.connectingdots.core_service.dto.ProblemStatementRequest;
import com.connectingdots.core_service.entity.ProblemStatement;
import com.connectingdots.core_service.service.ProblemStatementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/core/problem-statements")
@RequiredArgsConstructor
public class ProblemStatementController {

    private final ProblemStatementService problemStatementService;

    @PostMapping
    public ResponseEntity<ProblemStatement> createProblemStatement(@RequestBody ProblemStatementRequest request) {
        ProblemStatement createdProblem = problemStatementService.createProblemStatement(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProblem);
    }

    @GetMapping
    public ResponseEntity<Page<ProblemStatement>> getProblemStatements(
            @RequestParam(required = false) String domain,
            @RequestParam(defaultValue = "OPEN") String status,
            Pageable pageable) {
        return ResponseEntity.ok(problemStatementService.getProblemStatements(domain, status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProblemStatement> getProblemStatementById(
            @PathVariable UUID id,
            @RequestParam(required = false) String targetLang) {
        return ResponseEntity.ok(problemStatementService.getProblemStatementById(id, targetLang));
    }

    @PostMapping("/{id}/ingest")
    public ResponseEntity<String> triggerIngestion(@PathVariable UUID id) {
        problemStatementService.triggerIngestion(id);
        return ResponseEntity.ok("Ingestion task published to QStash successfully.");
    }

    @PutMapping("/{id}/ai-update")
    public ResponseEntity<String> updateProblemStatementWithAiResults(
            @PathVariable UUID id,
            @RequestBody com.connectingdots.core_service.dto.AiUpdatePayload payload
    ) {
        problemStatementService.updateProblemStatementWithAiResults(id, payload);
        return ResponseEntity.ok("Problem statement updated with AI results successfully.");
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProblemStatement> updateProblemStatement(
            @PathVariable UUID id,
            @RequestBody ProblemStatementRequest request) {
        ProblemStatement updated = problemStatementService.updateProblemStatement(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProblemStatement(@PathVariable UUID id) {
        problemStatementService.deleteProblemStatement(id);
        return ResponseEntity.noContent().build();
    }
}