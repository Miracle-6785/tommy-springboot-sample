package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/")
@Tag(name = "Hello", description = "Hello API")
public class HelloController {

    @GetMapping()
    @Operation(summary = "Get hello message", description = "Returns a simple hello message")
    public String hello() {
        return "Hello from Spring Boot!";
    }
}
