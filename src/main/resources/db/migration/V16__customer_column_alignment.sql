-- Remaining drift from the pre-Flyway tblcustomers definition that V1 skipped:
-- these columns are narrower than the Customer entity declares, so long values
-- would fail on save. is_deleted is realigned to the TINYINT(1) V3 intended.
ALTER TABLE tblcustomers
    MODIFY referred_by VARCHAR(200) NULL,
    MODIFY city VARCHAR(120) NULL,
    MODIFY state VARCHAR(120) NULL,
    MODIFY is_deleted TINYINT(1) NOT NULL DEFAULT 0;
