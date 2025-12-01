package com.jp.chatapi.chat.repository;

import com.jp.chatapi.chat.dto.ChatListItem;
import com.jp.chatapi.chat.dto.ChatMessageDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ChatQueryRepository {

    private final JdbcTemplate jdbc;
    public ChatQueryRepository(JdbcTemplate jdbc){ this.jdbc = jdbc; }

    /**
     * Return compact list of chats for the given user.
     * For 1:1 chats the displayName+avatar are from the other participant (peer).
     * For groups, displayName is chat_name and avatarUrl is null.
     */
    public List<ChatListItem> listCompactChatsForUser(int userId) {
        String sql = """
            SELECT
              c.id AS chat_id,
              c.is_group,
              c.chat_name,
              -- peer info for 1:1
              (SELECT u.username
               FROM chat_participants cp2
               JOIN users u ON u.id = cp2.user_id
               WHERE cp2.chat_id = c.id AND cp2.user_id <> ?
               LIMIT 1) AS peer_username,
              (SELECT u.avatar_url
               FROM chat_participants cp2
               JOIN users u ON u.id = cp2.user_id
               WHERE cp2.chat_id = c.id AND cp2.user_id <> ?
               LIMIT 1) AS peer_avatar
            FROM chats c
            JOIN chat_participants cp_me ON cp_me.chat_id = c.id AND cp_me.user_id = ?
            ORDER BY c.created_at DESC;
            """;

        return jdbc.query(sql, (rs, rn) -> {
            boolean isGroup = rs.getBoolean("is_group");
            String displayName = isGroup ? rs.getString("chat_name") : rs.getString("peer_username");
            String avatar = isGroup ? null : rs.getString("peer_avatar");
            return new ChatListItem(
                    rs.getLong("chat_id"),
                    displayName,
                    avatar,
                    isGroup
            );
        }, userId, userId, userId);
    }

    /**
     * Fetch full message history for a chat with pagination.
     * Ordered ASC so frontend can render from older -> newer.
     */
    public List<ChatMessageDto> fetchMessages(long chatId, int userId, int limit, int offset) {
        String sql = """
            SELECT
              m.id AS message_id,
              m.chat_id,
              m.sender_id,
              u.username AS sender_username,
              m.content,
              m.message_type,
              m.reply_to,
              m.edited,
              m.timestamp,
              ms.status
            FROM messages m
            LEFT JOIN users u ON u.id = m.sender_id
            LEFT JOIN message_status ms ON ms.message_id = m.id AND ms.user_id = ?
            WHERE m.chat_id = ?
            ORDER BY m.timestamp ASC
            LIMIT ? OFFSET ?;
            """;

        return jdbc.query(sql, (rs, rn) -> new ChatMessageDto(
                rs.getLong("message_id"),
                rs.getLong("chat_id"),
                rs.getObject("sender_id") == null ? null : rs.getInt("sender_id"),
                rs.getString("sender_username"),
                rs.getString("content"),
                rs.getString("message_type"),
                rs.getObject("reply_to") == null ? null : rs.getLong("reply_to"),
                rs.getBoolean("edited"),
                rs.getTimestamp("timestamp").toLocalDateTime(),
                rs.getString("status")
        ), userId, chatId, limit, offset);
    }
}
