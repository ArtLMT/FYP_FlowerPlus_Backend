package com.lmt.fyp.flowerplus.module.email.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
public class EmailMessage {
    private String templateName;
    private Map<String, Object> variables;
    private String receiver;
    private String subject;
}