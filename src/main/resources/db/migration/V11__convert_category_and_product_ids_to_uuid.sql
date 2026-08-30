-- =====================================================================
-- Convert categories.id, products.id, products.category_id to native UUID type
-- =====================================================================

-- Drop all FK constraints that reference columns being converted
ALTER TABLE products DROP CONSTRAINT IF EXISTS products_category_id_fkey;
ALTER TABLE product_images DROP CONSTRAINT IF EXISTS product_images_product_id_fkey;
ALTER TABLE product_tags DROP CONSTRAINT IF EXISTS product_tags_product_id_fkey;

-- Convert categories.id to UUID
ALTER TABLE categories ALTER COLUMN id DROP DEFAULT;
ALTER TABLE categories ALTER COLUMN id TYPE uuid USING id::uuid;
ALTER TABLE categories ALTER COLUMN id SET DEFAULT gen_random_uuid();

-- Convert products.id to UUID
ALTER TABLE products ALTER COLUMN id DROP DEFAULT;
ALTER TABLE products ALTER COLUMN id TYPE uuid USING id::uuid;
ALTER TABLE products ALTER COLUMN id SET DEFAULT gen_random_uuid();

-- Convert products.category_id to UUID
ALTER TABLE products ALTER COLUMN category_id TYPE uuid USING category_id::uuid;

-- Convert product_images.product_id to UUID
ALTER TABLE product_images ALTER COLUMN product_id TYPE uuid USING product_id::uuid;

-- Convert product_tags.product_id to UUID
ALTER TABLE product_tags ALTER COLUMN product_id TYPE uuid USING product_id::uuid;

-- Re-add FK constraints
ALTER TABLE products
    ADD CONSTRAINT products_category_id_fkey
    FOREIGN KEY (category_id) REFERENCES categories(id);

ALTER TABLE product_images
    ADD CONSTRAINT product_images_product_id_fkey
    FOREIGN KEY (product_id) REFERENCES products(id);

ALTER TABLE product_tags
    ADD CONSTRAINT product_tags_product_id_fkey
    FOREIGN KEY (product_id) REFERENCES products(id);
