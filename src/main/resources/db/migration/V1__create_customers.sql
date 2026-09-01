CREATE TABLE IF NOT EXISTS tblcustomers (
    cust_id BIGINT NOT NULL AUTO_INCREMENT,
    customer VARCHAR(200) NOT NULL,
    company_logo VARCHAR(500),
    commission DECIMAL(10, 2),
    cust_entry_dt DATETIME NOT NULL,
    referred_by VARCHAR(200),
    remarks TEXT,
    address VARCHAR(500),
    city VARCHAR(120),
    state VARCHAR(120),
    zip VARCHAR(20),
    billto_address VARCHAR(500),
    shipto_address VARCHAR(500),
    automail_on BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (cust_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
