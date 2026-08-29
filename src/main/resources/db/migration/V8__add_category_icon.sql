ALTER TABLE categories ADD COLUMN icon VARCHAR(50);

UPDATE categories SET icon = 'device' WHERE name = 'Electronics';
UPDATE categories SET icon = 'watch' WHERE name = 'Watches';
UPDATE categories SET icon = 'shoe_cleats' WHERE name = 'Shoes';
UPDATE categories SET icon = 'chair' WHERE name = 'Home & Furniture';
UPDATE categories SET icon = 'face_2' WHERE name = 'Beauty & Fragrance';
UPDATE categories SET icon = 'checkroom' WHERE name = 'Menswear';