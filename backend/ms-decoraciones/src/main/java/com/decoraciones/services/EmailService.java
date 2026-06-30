package com.decoraciones.services;

public interface EmailService {
    void sendEmail(String to, String subject, String body);
    void sendEmailWithAttachment(String to, String subject, String body, String attachmentName, byte[] attachmentData);
}
