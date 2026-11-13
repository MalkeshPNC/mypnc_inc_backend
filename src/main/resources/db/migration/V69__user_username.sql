ALTER TABLE tblusers
    ADD COLUMN username VARCHAR(320) NULL;

UPDATE tblusers
SET username = email
WHERE username IS NULL OR username = '';

ALTER TABLE tblusers
    MODIFY username VARCHAR(320) NOT NULL,
    DROP INDEX uk_users_email,
    MODIFY email VARCHAR(320) NULL,
    ADD UNIQUE KEY uk_users_username (username);
