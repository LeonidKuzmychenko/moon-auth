package lk.tech.moonauth.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, String>> getDashboard() {
        return ResponseEntity.ok(Map.of("message", "Welcome to the admin dashboard."));
    }
}
