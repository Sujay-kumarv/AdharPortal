package com.adhar.newapp.dto;

import lombok.Data;

@Data
public class MfaRequest {
    private String sessionId;
    private String code;
}
