package com.connectingdots.core.service;

import com.connectingdots.core.domain.NgoProblemStatement;
import com.connectingdots.core.domain.TechCategory;
import com.connectingdots.core.repository.NgoProblemRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProblemService {

    private final NgoProblemRepository repository;

    public ProblemService(NgoProblemRepository repository) {
        this.repository = repository;
    }

    public List<NgoProblemStatement> getOpenProblems() {
        return repository.findByStatus("OPEN");
    }

    public NgoProblemStatement updateStatus(UUID id, String status) {
        NgoProblemStatement problem = repository.findById(id).orElseThrow(() -> new RuntimeException("Problem not found"));
        problem.setStatus(status);
        return repository.save(problem);
    }

    @CircuitBreaker(name = "aiService", fallbackMethod = "fallbackProcessProblem")
    public NgoProblemStatement processAndSaveProblem(String ngoName, String rawDescription) {
        // Simulate a call to the AI service. If it takes too long or throws an error, the circuit breaker will trip.
        // For demonstration, we simply parse it locally, but ideally this triggers an external HTTP call.
        
        NgoProblemStatement problem = NgoProblemStatement.builder()
                .ngoName(ngoName)
                .rawDescription(rawDescription)
                .structuredProblem(mockExternalAiParsing(rawDescription))
                .techCategory(TechCategory.SOFTWARE_WEB)
                .status("OPEN")
                .build();
        
        return repository.save(problem);
    }

    private String mockExternalAiParsing(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Input is empty");
        }
        if (input.contains("trigger_timeout")) {
            try {
                Thread.sleep(15000); // Trigger timeout
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (input.contains("trigger_error")) {
            throw new RuntimeException("AI processing failed");
        }
        return "Structured: " + input;
    }

    public NgoProblemStatement fallbackProcessProblem(String ngoName, String rawDescription, Throwable t) {
        // Graceful fallback when the AI service is down
        NgoProblemStatement problem = NgoProblemStatement.builder()
                .ngoName(ngoName)
                .rawDescription(rawDescription)
                .structuredProblem("Fallback structure: Could not reach AI service. Raw description saved.")
                .techCategory(TechCategory.PROCESS_AUTOMATION)
                .status("OPEN")
                .build();
        return repository.save(problem);
    }
}
