ALTER TABLE pedidos
ADD COLUMN origen_pedido VARCHAR(40) NOT NULL DEFAULT 'OPERATIVO';

UPDATE pedidos
SET origen_pedido = 'OPERATIVO'
WHERE origen_pedido IS NULL;
