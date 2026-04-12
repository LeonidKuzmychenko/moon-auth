package lk.tech.moonauth.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
public class UserDashboardController {

    @GetMapping("/profile")
    public ResponseEntity<Map<String, String>> getProfile() {
        return ResponseEntity.ok(Map.of("message", "Welcome to your profile."));
    }
}
