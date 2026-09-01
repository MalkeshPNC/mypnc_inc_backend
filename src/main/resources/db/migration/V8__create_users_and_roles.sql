CREATE TABLE tblroles (
    role_id BIGINT NOT NULL AUTO_INCREMENT,
    role_code VARCHAR(50) NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    is_system TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (role_id),
    UNIQUE KEY uk_roles_code (role_code)
);

CREATE TABLE tblusers (
    user_id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_users_email (email)
);

CREATE TABLE tbluser_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES tblusers (user_id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES tblroles (role_id)
);

INSERT INTO tblroles (role_code, role_name, is_system) VALUES
    ('ADMIN', 'Administrator', 1),
    ('USER', 'User', 1);
