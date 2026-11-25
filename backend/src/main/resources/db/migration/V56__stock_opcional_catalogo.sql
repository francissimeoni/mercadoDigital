-- Ausencia de seguimiento para nuevas fichas de catálogo; no convierte ni actualiza filas existentes.
-- El servicio sigue exigiendo stock en tiendas que conservan el circuito anterior.
ALTER TABLE revendedor_productos MODIFY COLUMN stock INT NULL DEFAULT NULL;
