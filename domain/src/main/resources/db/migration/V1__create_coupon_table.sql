CREATE TABLE coupon (
    id UUID PRIMARY KEY,
    raw_code VARCHAR(255) NOT NULL,
    code VARCHAR(6) NOT NULL,
    description VARCHAR(255) NOT NULL,
    discount_value DECIMAL(19, 2) NOT NULL,
    expiration_date DATE NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL
);
