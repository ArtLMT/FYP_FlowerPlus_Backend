package com.lmt.fyp.flowerplus.module.email.service;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * One outgoing message: which template to render, with what variables, to whom.
 * Built once and read once, so it carries no setters.
 */
@Getter
@Builder
public class EmailMessage {
    private final String templateName;
    private final Map<String, Object> variables;
    private final String receiver;
    private final String subject;
}
