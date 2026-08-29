-- =====================================================================
-- Convert seed category IDs (cat-seed-<slug>) to UUID-formatted IDs
-- so that category IDs across the system are real UUIDs.
-- Deterministic: id = (md5(id)::uuid)::text.
-- Foreign key references in products are updated accordingly.
-- =====================================================================

ALTER TABLE products DROP CONSTRAINT IF EXISTS products_category_id_fkey;

UPDATE products
SET category_id = (md5(category_id)::uuid)::text
WHERE category_id LIKE 'cat-seed-%';

UPDATE categories
SET id = (md5(id)::uuid)::text
WHERE id LIKE 'cat-seed-%';

ALTER TABLE products
ADD CONSTRAINT products_category_id_fkey
FOREIGN KEY (category_id) REFERENCES categories(id);
