CREATE TABLE tbldev_tracks (
    track_id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    description TEXT NULL,
    eta DATE NOT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (track_id),
    KEY idx_dev_tracks_created (created_at, track_id)
);

CREATE TABLE tbldev_track_files (
    file_id BIGINT NOT NULL AUTO_INCREMENT,
    track_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (file_id),
    KEY idx_dev_track_files_track (track_id)
);

CREATE TABLE tbldev_track_comments (
    comment_id BIGINT NOT NULL AUTO_INCREMENT,
    track_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    comment TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (comment_id),
    KEY idx_dev_track_comments_track (track_id, comment_id)
);
