CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE clients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    document VARCHAR(20),
    phone VARCHAR(20),
    email VARCHAR(150),
    address TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE services (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    price NUMERIC(10,2) NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE service_orders (
    id BIGSERIAL PRIMARY KEY,
    number VARCHAR(30) NOT NULL UNIQUE,
    client_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    description TEXT,
    total_amount NUMERIC(10,2) NOT NULL DEFAULT 0,
    opened_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT fk_service_order_client
        FOREIGN KEY (client_id)
        REFERENCES clients(id),
    CONSTRAINT fk_service_order_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
);

CREATE TABLE service_order_items (
    id BIGSERIAL PRIMARY KEY,
    service_order_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    quantity NUMERIC(10,2) NOT NULL DEFAULT 1,
    unit_price NUMERIC(10,2) NOT NULL,
    total_price NUMERIC(10,2) NOT NULL,
    notes TEXT,

    CONSTRAINT fk_item_service_order
        FOREIGN KEY (service_order_id)
        REFERENCES service_orders(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_item_service
        FOREIGN KEY (service_id)
        REFERENCES services(id)
);
CREATE TABLE cash_flow (
    id BIGSERIAL PRIMARY KEY,

    service_order_id BIGINT,

    type VARCHAR(20) NOT NULL,
    -- INCOME / EXPENSE

    description VARCHAR(255) NOT NULL,

    amount NUMERIC(10,2) NOT NULL,

    transaction_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    payment_method VARCHAR(50),

    CONSTRAINT fk_cash_flow_service_order
        FOREIGN KEY (service_order_id)
        REFERENCES service_orders(id)
);
