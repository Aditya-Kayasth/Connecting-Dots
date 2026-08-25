package com.connectingdots.core_service.controller;

import com.connectingdots.core_service.dto.CloudinarySignatureResponse;
import com.connectingdots.core_service.service.FileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/core/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping("/signature")
    public ResponseEntity<CloudinarySignatureResponse> getUploadSignature(
            @RequestParam(defaultValue = "connecting-dots/problems") String folder) {
        return ResponseEntity.ok(fileService.generateUploadSignature(folder));
    }
}