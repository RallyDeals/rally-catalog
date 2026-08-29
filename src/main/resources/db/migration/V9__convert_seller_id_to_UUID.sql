ALTER TABLE products
ALTER COLUMN seller_id TYPE uuid
USING seller_id::uuid;