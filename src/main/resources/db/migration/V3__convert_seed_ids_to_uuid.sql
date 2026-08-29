-- =====================================================================
-- Convert deterministic seed product IDs (dummy-<n>) to UUID-formatted IDs
-- so the /products/lookup contract stays compatible with consumers that
-- deserialize product IDs as java.util.UUID (e.g. Order Service's
-- CatalogLookupResponse / CatalogProduct). Order Service maps lookup keys
-- to Map<UUID, ...>, so non-UUID ids like 'dummy-0078' would fail to bind.
-- Deterministic: id = md5(old_id)::uuid::text. Category and seller IDs are
-- left unchanged (no cross-service consumer deserializes them as UUID).
-- =====================================================================
UPDATE products
SET id = (md5(id)::uuid)::text
WHERE id LIKE 'dummy-%';