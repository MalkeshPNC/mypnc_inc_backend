ALTER TABLE tbldev_tracks
    ADD COLUMN status_updated_at DATETIME NULL;

UPDATE tbldev_tracks
    SET status_updated_at = created_at
    WHERE status_updated_at IS NULL;

ALTER TABLE tbldev_tracks
    MODIFY COLUMN status_updated_at DATETIME NOT NULL;
