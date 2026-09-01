CREATE TABLE tblpassword_reset_tokens (
    token_id BIGINT NOT NULL AUTO_INCREMENT,
    token_hash CHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    PRIMARY KEY (token_id),
    UNIQUE KEY uk_reset_token_hash (token_hash),
    KEY idx_reset_token_user (user_id),
    CONSTRAINT fk_reset_token_user FOREIGN KEY (user_id) REFERENCES tblusers (user_id)
);
