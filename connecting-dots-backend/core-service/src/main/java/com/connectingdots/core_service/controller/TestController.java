package com.connectingdots.core_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/core")
public class TestController {

    @GetMapping("/ping")
    public String ping() {
        return "Core Service is successfully responding through the Gateway!";
    }
}