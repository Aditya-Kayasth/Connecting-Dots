package com.connectingdots.controller;

import com.connectingdots.domain.NgoProblemRepository;
import com.connectingdots.domain.NgoProblemStatement;
import com.connectingdots.service.GroqAiService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/problems")
public class ProblemController {

    private final GroqAiService groqAiService;
    private final NgoProblemRepository repository;

    public ProblemController(GroqAiService groqAiService, NgoProblemRepository repository) {
        this.groqAiService = groqAiService;
        this.repository = repository;
    }

    public record ProblemSubmitRequest(
            @NotBlank String ngoName,
            @NotBlank String rawDescription
    ) {}

    @PostMapping("/submit")
    public ResponseEntity<NgoProblemStatement> submit(@Valid @RequestBody ProblemSubmitRequest request) {
        NgoProblemStatement saved = groqAiService.processAndSaveProblem(request.ngoName(), request.rawDescription());
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/open")
    public ResponseEntity<List<NgoProblemStatement>> getOpenProblems() {
        return ResponseEntity.ok(repository.findByStatus("OPEN"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<NgoProblemStatement> updateStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        NgoProblemStatement updated = groqAiService.updateStatus(id, status);
        return ResponseEntity.ok(updated);
    }

    // ── Global error handlers ──────────────────────────────────────────────────

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(400).body(Map.of("error", ex.getMessage()));
    }
}
