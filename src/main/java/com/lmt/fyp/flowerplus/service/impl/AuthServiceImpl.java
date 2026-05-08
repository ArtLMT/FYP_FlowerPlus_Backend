package com.lmt.fyp.flowerplus.service.impl;

import com.lmt.fyp.flowerplus.entity.User;
import com.lmt.fyp.flowerplus.repository.UserRepository;
import com.lmt.fyp.flowerplus.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;


    @Override
    public void register(com.lmt.fyp.flowerplus.dto.request.LoginRequest request) {

        User user = User.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .fullName(request.getFullName())
                .build();

        userRepository.save(user);
    }
}
