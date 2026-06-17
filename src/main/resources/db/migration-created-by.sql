-- Migração: registrar quem cadastrou cliente, serviço e lançamento de caixa
-- Execute em bancos já existentes:
--   psql -U postgres -d service-orders -f migration-created-by.sql

ALTER TABLE clients
    ADD COLUMN IF NOT EXISTS created_by_id BIGINT;

ALTER TABLE clients
    DROP CONSTRAINT IF EXISTS fk_clients_created_by;

ALTER TABLE clients
    ADD CONSTRAINT fk_clients_created_by
        FOREIGN KEY (created_by_id) REFERENCES users (id);

ALTER TABLE services
    ADD COLUMN IF NOT EXISTS created_by_id BIGINT;

ALTER TABLE services
    DROP CONSTRAINT IF EXISTS fk_services_created_by;

ALTER TABLE services
    ADD CONSTRAINT fk_services_created_by
        FOREIGN KEY (created_by_id) REFERENCES users (id);

ALTER TABLE cash_flow
    ADD COLUMN IF NOT EXISTS created_by_id BIGINT;

ALTER TABLE cash_flow
    DROP CONSTRAINT IF EXISTS fk_cash_flow_created_by;

ALTER TABLE cash_flow
    ADD CONSTRAINT fk_cash_flow_created_by
        FOREIGN KEY (created_by_id) REFERENCES users (id);
