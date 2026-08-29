CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =====================================================================
-- categories
-- =====================================================================

CREATE TABLE categories (
                            id          VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
                            name        VARCHAR(100) NOT NULL UNIQUE,
                            description TEXT,
                            created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- =====================================================================
-- products
-- =====================================================================

CREATE TABLE products (
                          id                VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
                          seller_id         VARCHAR(36) NOT NULL,
                          name              VARCHAR(255) NOT NULL,
                          description       TEXT NOT NULL DEFAULT '',
                          category_id       VARCHAR(36) REFERENCES categories(id),
                          base_price        NUMERIC(10,2) NOT NULL CHECK (base_price >= 0),
                          image_url         VARCHAR(500),

                          status            VARCHAR(20) NOT NULL DEFAULT 'PENDING_APPROVAL'
                              CHECK (status IN (
                                                'PENDING_APPROVAL',
                                                'APPROVED',
                                                'REJECTED'
                                  )),
                          rejection_reason  TEXT,

                          created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
                          updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),

                          deleted_at        TIMESTAMPTZ
);

CREATE INDEX idx_products_seller_id    ON products (seller_id);
CREATE INDEX idx_products_category_id  ON products (category_id);
CREATE INDEX idx_products_status       ON products (status);
CREATE INDEX idx_products_deleted_at   ON products (deleted_at);

-- Full-text search index for name + description
CREATE INDEX idx_products_fts
    ON products
    USING GIN (to_tsvector('english', name || ' ' || description));

-- Keep updated_at in sync on every row update
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_products_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
