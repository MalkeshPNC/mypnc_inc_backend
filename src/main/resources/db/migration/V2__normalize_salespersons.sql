CREATE TABLE IF NOT EXISTS tblcontacts (
    cont_id BIGINT NOT NULL AUTO_INCREMENT,
    cust_id BIGINT NOT NULL,
    first_name VARCHAR(120),
    last_name VARCHAR(120),
    phone VARCHAR(40),
    email VARCHAR(320),
    contact_person VARCHAR(200),
    PRIMARY KEY (cont_id),
    INDEX idx_contacts_cust_id (cust_id),
    CONSTRAINT fk_contacts_customer FOREIGN KEY (cust_id) REFERENCES tblcustomers (cust_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tblsalespersons (
    sp_id BIGINT NOT NULL AUTO_INCREMENT,
    sales_person VARCHAR(200) NOT NULL,
    sp_email VARCHAR(320),
    PRIMARY KEY (sp_id),
    UNIQUE KEY uk_salespersons_email (sp_email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tblcustomer_salespersons (
    csp_id BIGINT NOT NULL AUTO_INCREMENT,
    cust_id BIGINT NOT NULL,
    sp_id BIGINT NOT NULL,
    commission DECIMAL(10, 2),
    PRIMARY KEY (csp_id),
    UNIQUE KEY uk_customer_salesperson (cust_id, sp_id),
    CONSTRAINT fk_csp_customer FOREIGN KEY (cust_id) REFERENCES tblcustomers (cust_id) ON DELETE CASCADE,
    CONSTRAINT fk_csp_salesperson FOREIGN KEY (sp_id) REFERENCES tblsalespersons (sp_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- INSERT IGNORE INTO tblsalespersons (sales_person, sp_email)
-- SELECT DISTINCT sales_person, sp_email
-- FROM tblcustomers
-- WHERE sales_person IS NOT NULL;

-- INSERT IGNORE INTO tblcustomer_salespersons (cust_id, sp_id, commission)
-- SELECT c.cust_id, sp.sp_id, c.commission
-- FROM tblcustomers c
-- JOIN tblsalespersons sp
--   ON sp.sales_person = c.sales_person
--  AND (sp.sp_email = c.sp_email OR (sp.sp_email IS NULL AND c.sp_email IS NULL))
-- WHERE c.sales_person IS NOT NULL;

-- ALTER TABLE tblcustomers
--     DROP COLUMN sales_person,
--     DROP COLUMN sp_email,
--     DROP COLUMN commission;