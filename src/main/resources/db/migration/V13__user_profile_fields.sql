ALTER TABLE tblusers
    ADD COLUMN date_of_joining DATE NULL,
    ADD COLUMN department VARCHAR(120) NULL,
    ADD COLUMN branch VARCHAR(120) NULL,
    ADD COLUMN home_address VARCHAR(500) NULL,
    ADD COLUMN date_of_birth DATE NULL,
    ADD COLUMN designation VARCHAR(120) NULL,
    ADD COLUMN regular_timing VARCHAR(80) NULL,
    ADD COLUMN contact_number VARCHAR(40) NULL;
