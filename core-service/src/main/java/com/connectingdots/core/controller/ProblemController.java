package com.connectingdots.core.controller;

import com.connectingdots.core.domain.NgoProblemStatement;
import com.connectingdots.core.service.ProblemService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/problems")
public class ProblemController {

    private final ProblemService problemService;

    public ProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    public record ProblemSubmitRequest(
            @NotBlank String ngoName,
            @NotBlank String rawDescription
    ) {}

    @PostMapping("/submit")
    public ResponseEntity<NgoProblemStatement> submit(@Valid @RequestBody ProblemSubmitRequest request) {
        NgoProblemStatement saved = problemService.processAndSaveProblem(request.ngoName(), request.rawDescription());
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/open")
    public ResponseEntity<List<NgoProblemStatement>> getOpenProblems() {
        return ResponseEntity.ok(problemService.getOpenProblems());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<NgoProblemStatement> updateStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        NgoProblemStatement updated = problemService.updateStatus(id, status);
        return ResponseEntity.ok(updated);
    }
}
