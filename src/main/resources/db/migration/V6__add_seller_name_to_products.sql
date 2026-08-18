-- =====================================================================
-- seller_name: denormalized copy of the seller's display name, captured
-- from the X-User-Name header at product-creation time. Names are
-- immutable in this system, so this column never needs to be refreshed
-- after it is first set.
--
-- Backfill: pre-existing rows predate this column, so their seller_name
-- is set explicitly here per known seller_id.
-- =====================================================================

ALTER TABLE products ADD COLUMN seller_name VARCHAR(255);

UPDATE products SET seller_name = 'Northgate Co.' WHERE seller_id = '018f3a38-c398-7517-a9a7-961de3330001';
UPDATE products SET seller_name = 'Wrenfield Co.'     WHERE seller_id = 'a1b2c3d4-1111-4a1b-8c2d-000000000001';
UPDATE products SET seller_name = 'Coastal Co.'  WHERE seller_id = 'a1b2c3d4-2222-4a1b-8c2d-000000000002';
UPDATE products SET seller_name = 'Lumen Co.'       WHERE seller_id = 'a1b2c3d4-3333-4a1b-8c2d-000000000003';
UPDATE products SET seller_name = 'Stride Co.'        WHERE seller_id = 'a1b2c3d4-4444-4a1b-8c2d-000000000004';
UPDATE products SET seller_name = 'Vantage Co.'     WHERE seller_id = 'f8e5d6c7-b8a9-4012-9345-6789abcdef01';