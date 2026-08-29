-- =====================================================================
-- SKU, visibility flag, and tags for products
-- sku: seller-facing identifier for the listing.
-- visible: when FALSE the product is hidden from public browse/search.
-- product_tags: free-form labels used for search and discovery.
-- =====================================================================

ALTER TABLE products ADD COLUMN sku VARCHAR(64);
ALTER TABLE products ADD COLUMN visible BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_products_sku      ON products (sku);
CREATE INDEX idx_products_visible  ON products (visible);

CREATE TABLE product_tags (
                              product_id  VARCHAR(36) NOT NULL REFERENCES products(id),
                              tag         VARCHAR(50) NOT NULL,
                              tags_order  INTEGER NOT NULL,
                              PRIMARY KEY (product_id, tags_order)
);

CREATE INDEX idx_product_tags_tag ON product_tags (tag);