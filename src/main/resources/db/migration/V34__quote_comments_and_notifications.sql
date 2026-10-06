CREATE TABLE tblnotifications (
    notification_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body VARCHAR(500) NOT NULL,
    route VARCHAR(200) NOT NULL,
    read_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (notification_id),
    KEY idx_notifications_user_created (user_id, created_at)
);

CREATE TABLE tblquote_comments (
    comment_id BIGINT NOT NULL AUTO_INCREMENT,
    qid BIGINT NOT NULL,
    author_user_id BIGINT NOT NULL,
    body VARCHAR(4000) NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (comment_id),
    KEY idx_quote_comments_qid_created (qid, created_at)
);

CREATE TABLE tblquote_comment_mentions (
    comment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (comment_id, user_id)
);

CREATE TABLE tblquote_comment_images (
    image_id BIGINT NOT NULL AUTO_INCREMENT,
    comment_id BIGINT NOT NULL,
    qid BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    PRIMARY KEY (image_id),
    UNIQUE KEY uk_quote_comment_images_storage (storage_key),
    KEY idx_quote_comment_images_comment (comment_id)
);
