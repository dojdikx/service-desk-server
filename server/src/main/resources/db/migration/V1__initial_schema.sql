CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    max_id BIGINT NOT NULL UNIQUE,
    max_chat_id BIGINT,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    city VARCHAR(255) NOT NULL,
    printer_model VARCHAR(255),
    serial_number VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'none',
    avatar_url VARCHAR(512),
    avatar_source_url VARCHAR(512),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE support_staff (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role VARCHAR(50) NOT NULL DEFAULT 'support_agent',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE INDEX idx_support_staff_email ON support_staff (email);
CREATE INDEX idx_support_staff_role ON support_staff (role);

CREATE TABLE tickets (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users (id),
    max_user_id BIGINT,
    status VARCHAR(50) NOT NULL DEFAULT 'Первичный контакт',
    tags JSONB NOT NULL DEFAULT '[]'::jsonb,
    assigned_to INTEGER REFERENCES support_staff (id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason TEXT,
    printer_model VARCHAR(255),
    serial_number VARCHAR(255),
    tracking_number VARCHAR(255),
    delivery_address TEXT,
    warranty_case BOOLEAN,
    purchased_from VARCHAR(255),
    sku VARCHAR(255),
    delivery_cost NUMERIC(10, 2),
    solution_description TEXT,
    full_name VARCHAR(255),
    contact_phone VARCHAR(32),
    external_export_url TEXT,
    service_type VARCHAR(50),
    service_time DATE,
    replenishment_requirement VARCHAR(10),
    service_channel JSONB NOT NULL DEFAULT '[]'::jsonb,
    service_solution VARCHAR(100),
    warranty_status VARCHAR(50),
    issue_type VARCHAR(255),
    sub_issue_type VARCHAR(255),
    third_issue_type VARCHAR(255),
    qty_used INTEGER,
    replacement_qty INTEGER,
    replenishment_qty INTEGER,
    sku_remark TEXT,
    extra_serial_number VARCHAR(255),
    purchase_order_id VARCHAR(255),
    product_type VARCHAR(100),
    replacement_parts JSONB NOT NULL DEFAULT '[]'::jsonb
);

CREATE INDEX idx_tickets_assigned_to ON tickets (assigned_to);
CREATE INDEX idx_tickets_max_user_id ON tickets (max_user_id);
CREATE INDEX idx_tickets_user_id_status ON tickets (user_id, status);

CREATE TABLE messages (
    id SERIAL PRIMARY KEY,
    ticket_id INTEGER NOT NULL REFERENCES tickets (id) ON DELETE CASCADE,
    message_text TEXT,
    message_type VARCHAR(20) NOT NULL,
    media_type VARCHAR(20),
    media_url TEXT,
    media_caption TEXT,
    media_file_name TEXT,
    media_items JSONB,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    delivery_status VARCHAR(20) NOT NULL DEFAULT 'delivered',
    delivery_error TEXT,
    external_message_id TEXT,
    reply_to_id INTEGER REFERENCES messages (id) ON DELETE SET NULL,
    quote_text TEXT,
    quote_offset INTEGER
);

CREATE INDEX idx_messages_ticket_id ON messages (ticket_id);
CREATE INDEX idx_messages_ticket_external_id ON messages (ticket_id, external_message_id);

CREATE TABLE ticket_read_state (
    staff_id INTEGER NOT NULL REFERENCES support_staff (id) ON DELETE CASCADE,
    ticket_id INTEGER NOT NULL REFERENCES tickets (id) ON DELETE CASCADE,
    last_read_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (staff_id, ticket_id)
);

CREATE INDEX idx_ticket_read_state_ticket_id ON ticket_read_state (ticket_id);

CREATE TABLE analytics (
    id SERIAL PRIMARY KEY,
    path TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE OR REPLACE FUNCTION update_tickets_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_tickets_updated_at
    BEFORE UPDATE ON tickets
    FOR EACH ROW
    EXECUTE FUNCTION update_tickets_updated_at();
