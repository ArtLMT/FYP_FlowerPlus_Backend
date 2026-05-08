package com.lmt.fyp.flowerplus.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    private String email;
    private String password;
    private String fullName;
}
