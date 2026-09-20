package com.emresahin.controller;

import com.emresahin.dto.SendMailRequestDto;
import com.emresahin.model.EmailRecipient;
import com.emresahin.service.EmailRecipientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mail")
@RequiredArgsConstructor
public class EmailRecipientController {

    private final EmailRecipientService service;

    @GetMapping("/recipients")
    public ResponseEntity<List<EmailRecipient>> getRecipients() {
        return ResponseEntity.ok(service.getAllRecipients());
    }

    @PostMapping("/recipients")
    public ResponseEntity<EmailRecipient> addRecipient(@RequestBody EmailRecipient recipient) {
        return ResponseEntity.ok(service.addRecipient(recipient));
    }

    @DeleteMapping("/recipients/{id}")
    public ResponseEntity<Void> deleteRecipient(@PathVariable Long id) {
        service.deleteRecipient(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMail(@RequestBody SendMailRequestDto request) {
        return ResponseEntity.ok(service.sendMailViaN8n(request));
    }
}
