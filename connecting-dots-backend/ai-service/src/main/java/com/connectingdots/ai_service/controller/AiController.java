package com.connectingdots.ai_service.controller;

import com.connectingdots.ai_service.dto.ProblemStatementRequest;
import com.connectingdots.ai_service.service.AiProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiProblemService aiProblemService;

    @PostMapping("/structure-problem")
    public Object structureProblem(@RequestBody String rawText, HttpServletRequest request) {
        return aiProblemService.structureProblem(rawText, request);
    }   
}