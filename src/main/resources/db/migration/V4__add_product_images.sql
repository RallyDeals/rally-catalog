-- =====================================================================
-- product_images
-- Multiple image paths per product. image_url on products stays as the
-- primary cover image; this table holds the full gallery.
-- =====================================================================

CREATE TABLE product_images (
    product_id   VARCHAR(36) NOT NULL REFERENCES products(id),
    image_url    VARCHAR(500) NOT NULL,
    images_order INTEGER NOT NULL,
    PRIMARY KEY (product_id, images_order)
);