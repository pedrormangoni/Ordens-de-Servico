-- Migration: CPF obrigatorio e unico (banco service-orders)
-- Execute apenas se a tabela clients ja existir com a estrutura antiga.

UPDATE clients
SET document = regexp_replace(document, '\D', '', 'g')
WHERE document IS NOT NULL;

-- Corrija ou remova registros sem CPF de 11 digitos antes do proximo passo.

ALTER TABLE clients
    ALTER COLUMN document TYPE VARCHAR(11),
    ALTER COLUMN document SET NOT NULL;

ALTER TABLE clients
    ADD CONSTRAINT uk_clients_document UNIQUE (document);
