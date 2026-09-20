package com.emresahin.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMailRequestDto {
    private String email;
    private String title;
    private String message;

    // Backward compatibility getters
    public String getRecipientEmail() {
        return email;
    }

    public String getSubject() {
        return title;
    }
}
