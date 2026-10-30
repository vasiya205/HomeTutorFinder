package com.example.notificationservice.domain;

/**
 * QUEUED  - saved, waiting to be sent
 * SENT    - accepted by the SMTP server
 * LOGGED  - SMTP not configured (or a demo address), so the email was only stored/printed
 * FAILED  - the SMTP server rejected it (see error_message)
 */
public enum EmailStatus {
    QUEUED,
    SENT,
    LOGGED,
    FAILED
}
