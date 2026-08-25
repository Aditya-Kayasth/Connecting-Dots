package com.connectingdots.core_service.service;

import com.cloudinary.Cloudinary;
import com.connectingdots.core_service.dto.CloudinarySignatureResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class FileService {

    private final Cloudinary cloudinary;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    public FileService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public CloudinarySignatureResponse generateUploadSignature(String folder) {
        long timestamp = System.currentTimeMillis() / 1000L;
        
        Map<String, Object> paramsToSign = new HashMap<>();
        paramsToSign.put("timestamp", timestamp);
        paramsToSign.put("folder", folder);

        String signature = cloudinary.apiSignRequest(paramsToSign, apiSecret);

        return new CloudinarySignatureResponse(
                signature,
                timestamp,
                apiKey,
                cloudName,
                folder
        );
    }
}