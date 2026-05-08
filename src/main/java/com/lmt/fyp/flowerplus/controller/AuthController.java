package com.lmt.fyp.flowerplus.controller;
import com.lmt.fyp.flowerplus.dto.request.LoginRequest;
import com.lmt.fyp.flowerplus.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public String register(
            @RequestBody LoginRequest request
    ) {

        authService.register(request);

        return "Register success";
    }
}