package org.batukhtin.transcontbot.controllers;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.integration.mail.ImapMailReceiver;
import org.springframework.integration.mail.MailReceiver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/docs")
@RequiredArgsConstructor
public class CommonController {

}
