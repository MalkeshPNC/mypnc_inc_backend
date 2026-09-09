-- The legacy Access quote form carried a Sams Review flag alongside ITARC and
-- BERRYC. V19 shipped without it, so add it in its original place.
ALTER TABLE tblquotes
    ADD COLUMN sams_review TINYINT(1) NOT NULL DEFAULT 0 AFTER berryc;
