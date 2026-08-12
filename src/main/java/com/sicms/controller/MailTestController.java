package com.sicms.controller;

import com.sicms.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/test-email", "/api/test-mail"})
public class MailTestController {

    private final EmailService emailService;

    public MailTestController(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping
    public ResponseEntity<String> testEmail(
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "email", required = false) String email
    ) {
        String recipient = (to != null && !to.isBlank()) ? to : email;
        if (recipient == null || recipient.isBlank()) {
            recipient = "b5073a001@smtp-brevo.com";
        }
        emailService.sendTestEmail(recipient);
        return ResponseEntity.ok("Test email sent through Brevo SMTP to " + recipient);
    }
}
