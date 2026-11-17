CREATE TABLE tblapp_configurations (
    config_key VARCHAR(100) NOT NULL,
    config_value VARCHAR(500) NOT NULL,
    description VARCHAR(255) NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (config_key)
);

INSERT INTO tblapp_configurations (config_key, config_value, description, updated_at)
VALUES (
    'salesperson.defaultCommission',
    '0',
    'Default commission (%) when assigning a salesperson to a customer',
    CURRENT_TIMESTAMP
);
