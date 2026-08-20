CREATE TABLE product_active_deals (
    deal_id         VARCHAR(36)  PRIMARY KEY,
    product_id      VARCHAR(36)  NOT NULL,
    deal_price      NUMERIC(10,2) NOT NULL,
    deal_stock      INTEGER      NOT NULL,
    current_participants INTEGER  NOT NULL DEFAULT 0,
    min_participants INTEGER     NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    end_time        TIMESTAMPTZ,
    duration_minutes INTEGER,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_pad_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE INDEX idx_pad_product_status ON product_active_deals (product_id, status);
