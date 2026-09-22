CREATE TABLE tblchat_conversations (
    conversation_id BIGINT NOT NULL AUTO_INCREMENT,
    user_lo BIGINT NOT NULL,
    user_hi BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (conversation_id),
    UNIQUE KEY uk_chat_pair (user_lo, user_hi),
    CONSTRAINT chk_chat_pair CHECK (user_lo < user_hi),
    CONSTRAINT fk_chat_conv_user_lo FOREIGN KEY (user_lo) REFERENCES tblusers (user_id),
    CONSTRAINT fk_chat_conv_user_hi FOREIGN KEY (user_hi) REFERENCES tblusers (user_id)
);

CREATE TABLE tblchat_participants (
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    last_read_at DATETIME NULL,
    PRIMARY KEY (conversation_id, user_id),
    CONSTRAINT fk_chat_part_conv FOREIGN KEY (conversation_id) REFERENCES tblchat_conversations (conversation_id),
    CONSTRAINT fk_chat_part_user FOREIGN KEY (user_id) REFERENCES tblusers (user_id)
);

CREATE TABLE tblchat_messages (
    message_id BIGINT NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    body VARCHAR(2000) NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (message_id),
    KEY idx_chat_messages_conv_created (conversation_id, created_at),
    CONSTRAINT fk_chat_msg_conv FOREIGN KEY (conversation_id) REFERENCES tblchat_conversations (conversation_id),
    CONSTRAINT fk_chat_msg_sender FOREIGN KEY (sender_id) REFERENCES tblusers (user_id)
);
