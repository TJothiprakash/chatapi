package com.jp.chatapi.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ChatMessageDto {
    private Long messageId;
    private Long chatId;
    private Integer senderId;
    private String senderUsername;
    private String content;
    private String messageType;
    private Long replyTo;
    private boolean edited;
    private LocalDateTime timestamp;
    private String status; // sent/delivered/read (for current user if available)
}
