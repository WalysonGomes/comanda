-- Suporta a leitura filtrada do painel de pedidos (task 1.4 / design.md "Migration Plan"):
-- listar por tenant + status, ordenado por created_at desc.
CREATE INDEX idx_orders_tenant_status_created ON orders (tenant_id, status, created_at DESC);
