package com.connectingdots.controller;

import com.connectingdots.domain.NgoProblemRepository;
import com.connectingdots.domain.NgoProblemStatement;
import com.connectingdots.service.GroqAiService;
import com.connectingdots.service.PdfParsingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/problems")
public class ProblemController {

    private final GroqAiService groqAiService;
    private final NgoProblemRepository repository;
    private final PdfParsingService pdfParsingService;

    public ProblemController(GroqAiService groqAiService,
                             NgoProblemRepository repository,
                             PdfParsingService pdfParsingService) {
        this.groqAiService = groqAiService;
        this.repository = repository;
        this.pdfParsingService = pdfParsingService;
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

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<NgoProblemStatement> uploadPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam("ngoName") String ngoName) {
        String extractedText = pdfParsingService.extractText(file);
        NgoProblemStatement saved = groqAiService.processAndSaveProblem(ngoName, extractedText);
        return ResponseEntity.status(201).body(saved);
    }
}
