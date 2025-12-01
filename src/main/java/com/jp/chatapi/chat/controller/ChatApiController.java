package com.jp.chatapi.chat.controller;

import com.jp.chatapi.chat.dto.ChatListItem;
import com.jp.chatapi.chat.dto.ChatMessageDto;
import com.jp.chatapi.chat.repository.ChatQueryRepository;
import com.jp.chatapi.security.jwt.JwtUtil;
import com.jp.chatapi.security.repository.UserRepository;
import com.jp.chatapi.chat.modal.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class ChatApiController {

    private final ChatQueryRepository repo;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public ChatApiController(ChatQueryRepository repo, JwtUtil jwtUtil, UserRepository userRepository) {
        this.repo = repo;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    private Optional<Integer> extractUserId(String auth){
        try {
            if (auth == null || !auth.startsWith("Bearer ")) return Optional.empty();
            String token = auth.substring(7);
            String email = jwtUtil.getEmailFromToken(token);
            return userRepository.findByEmail(email).map(User::getId);
        } catch(Exception e){
            return Optional.empty();
        }
    }

    /**
     * GET /api/chats/list
     * Returns compact list: only chatId, displayName and avatarUrl (and isGroup)
     */
    @GetMapping("/chats/list")
    public ResponseEntity<?> getCompactChats(@RequestHeader(value = "Authorization", required = false) String auth){
        var uidOpt = extractUserId(auth);
        if (uidOpt.isEmpty()) return ResponseEntity.status(401).body("Unauthorized");

        List<ChatListItem> list = repo.listCompactChatsForUser(uidOpt.get());
        return ResponseEntity.ok(list);
    }

    /**
     * GET /api/messages/{chatId}?limit=50&offset=0
     * Returns paginated message history with full message fields.
     */
    @GetMapping("/messages/{chatId}")
    public ResponseEntity<?> getMessages(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable long chatId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        var uidOpt = extractUserId(auth);
        if (uidOpt.isEmpty()) return ResponseEntity.status(401).body("Unauthorized");

        List<ChatMessageDto> msgs = repo.fetchMessages(chatId, uidOpt.get(), limit, offset);
        return ResponseEntity.ok(msgs);
    }
}
