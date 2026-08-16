package com.lmt.fyp.flowerplus.module.email.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class EmailMessage {
    private boolean isHtml;
    private String receiver;
    private String subject;
    private String body;
}