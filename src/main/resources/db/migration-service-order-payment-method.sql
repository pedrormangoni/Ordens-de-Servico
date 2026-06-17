-- Migração: persistir forma de pagamento na ordem de serviço
-- Execute em bancos já existentes:
--   psql -U postgres -d service-orders -f migration-service-order-payment-method.sql

ALTER TABLE service_orders
    ADD COLUMN IF NOT EXISTS payment_method VARCHAR(50);
