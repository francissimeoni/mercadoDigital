-- Seed de desarrollo para cargar 50 articulos completos en el circuito real:
-- proveedor -> producto -> stock -> markup revendedor -> catalogo canonico -> storefront
--
-- Uso sugerido:
-- 1. Ajustar @proveedor_id y @revendedor_id
-- 2. Ejecutar sobre la base local de MySQL
-- 3. Verificar /public/catalogo?revendedorId=<revendedor_id>
--
-- Notas:
-- - Si el proveedor no existe, el script lo crea.
-- - El revendedor debe existir previamente para que se creen los markups.
-- - El script es idempotente por modelo canonico, SKU, stock, markup y media principal.

SET @proveedor_id = '33333333-3333-3333-3333-333333333302';
SET @revendedor_id = '33333333-3333-3333-3333-333333333301';
SET @now = NOW(6);

INSERT INTO proveedores (id, nombre, activo, created_at, updated_at)
SELECT @proveedor_id, 'Proveedor Seed Catalogo 50', b'1', @now, @now
WHERE NOT EXISTS (
  SELECT 1
  FROM proveedores
  WHERE id = @proveedor_id
);

DROP TEMPORARY TABLE IF EXISTS tmp_catalogo_seed_50;

CREATE TEMPORARY TABLE tmp_catalogo_seed_50 (
  orden INT NOT NULL PRIMARY KEY,
  sku VARCHAR(80) NOT NULL,
  marca VARCHAR(80) NOT NULL,
  modelo VARCHAR(120) NOT NULL,
  nombre_publico VARCHAR(200) NOT NULL,
  nombre_proveedor VARCHAR(180) NOT NULL,
  descripcion TEXT NULL,
  categoria_paquete VARCHAR(30) NOT NULL,
  precio_costo DECIMAL(19,2) NOT NULL,
  stock INT NOT NULL,
  markup_porcentaje DECIMAL(5,2) NOT NULL,
  imagen_url VARCHAR(500) NOT NULL
);

INSERT INTO tmp_catalogo_seed_50 (
  orden, sku, marca, modelo, nombre_publico, nombre_proveedor, descripcion,
  categoria_paquete, precio_costo, stock, markup_porcentaje, imagen_url
) VALUES
  (1, 'PV50-0001', 'DemoTech', 'DT-0001', 'Auriculares Inalambricos Lite', 'Auriculares Inalambricos Lite - Proveedor', 'Auriculares inalambricos con estuche de carga y microfono integrado.', 'CHICO', 8900.00, 18, 0.20, 'https://images.unsplash.com/photo-1583394838336-acd977736f90?auto=format&fit=crop&w=800&q=60&sig=1'),
  (2, 'PV50-0002', 'DemoTech', 'DT-0002', 'Auriculares Inalambricos Pro', 'Auriculares Inalambricos Pro - Proveedor', 'Auriculares premium con cancelacion pasiva y bateria extendida.', 'CHICO', 11200.00, 16, 0.22, 'https://images.unsplash.com/photo-1546435770-a3e426bf472b?auto=format&fit=crop&w=800&q=60&sig=2'),
  (3, 'PV50-0003', 'DemoSound', 'DS-0003', 'Parlante Portatil Mini', 'Parlante Portatil Mini - Proveedor', 'Parlante bluetooth compacto para uso diario.', 'CHICO', 9700.00, 22, 0.20, 'https://images.unsplash.com/photo-1511467687858-23d96c32e4ae?auto=format&fit=crop&w=800&q=60&sig=3'),
  (4, 'PV50-0004', 'DemoSound', 'DS-0004', 'Parlante Portatil Outdoor', 'Parlante Portatil Outdoor - Proveedor', 'Parlante resistente con bateria de larga duracion.', 'CHICO', 13400.00, 15, 0.25, 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?auto=format&fit=crop&w=800&q=60&sig=4'),
  (5, 'PV50-0005', 'DemoInput', 'DI-0005', 'Teclado Mecanico Compacto', 'Teclado Mecanico Compacto - Proveedor', 'Teclado mecanico compacto con switches tactiles.', 'CHICO', 15400.00, 19, 0.20, 'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=800&q=60&sig=5'),
  (6, 'PV50-0006', 'DemoInput', 'DI-0006', 'Teclado Mecanico RGB', 'Teclado Mecanico RGB - Proveedor', 'Teclado full size con iluminacion RGB configurable.', 'CHICO', 17100.00, 13, 0.22, 'https://images.unsplash.com/photo-1517336714739-489689fd1ca8?auto=format&fit=crop&w=800&q=60&sig=6'),
  (7, 'PV50-0007', 'DemoInput', 'DI-0007', 'Mouse Gamer Ergonomico', 'Mouse Gamer Ergonomico - Proveedor', 'Mouse ergonomico con sensor de alta precision.', 'CHICO', 7600.00, 28, 0.18, 'https://images.unsplash.com/photo-1583394838336-acd977736f90?auto=format&fit=crop&w=800&q=60&sig=7'),
  (8, 'PV50-0008', 'DemoInput', 'DI-0008', 'Mouse Inalambrico Oficina', 'Mouse Inalambrico Oficina - Proveedor', 'Mouse liviano para escritorio y movilidad.', 'CHICO', 5400.00, 30, 0.20, 'https://images.unsplash.com/photo-1546435770-a3e426bf472b?auto=format&fit=crop&w=800&q=60&sig=8'),
  (9, 'PV50-0009', 'DemoView', 'DV-0009', 'Monitor Full HD 24', 'Monitor Full HD 24 - Proveedor', 'Monitor 24 pulgadas con panel IPS y HDMI.', 'MEDIANO', 68900.00, 9, 0.18, 'https://images.unsplash.com/photo-1511467687858-23d96c32e4ae?auto=format&fit=crop&w=800&q=60&sig=9'),
  (10, 'PV50-0010', 'DemoView', 'DV-0010', 'Monitor Full HD 27', 'Monitor Full HD 27 - Proveedor', 'Monitor 27 pulgadas ideal para trabajo y entretenimiento.', 'MEDIANO', 82400.00, 7, 0.18, 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?auto=format&fit=crop&w=800&q=60&sig=10'),
  (11, 'PV50-0011', 'DemoCam', 'DC-0011', 'Webcam HD USB', 'Webcam HD USB - Proveedor', 'Webcam HD con microfono incorporado para videollamadas.', 'CHICO', 12400.00, 17, 0.20, 'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=800&q=60&sig=11'),
  (12, 'PV50-0012', 'DemoCam', 'DC-0012', 'Webcam Full HD Autofocus', 'Webcam Full HD Autofocus - Proveedor', 'Webcam Full HD con autofocus para streaming.', 'CHICO', 19300.00, 12, 0.22, 'https://images.unsplash.com/photo-1517336714739-489689fd1ca8?auto=format&fit=crop&w=800&q=60&sig=12'),
  (13, 'PV50-0013', 'DemoAudio', 'DA-0013', 'Microfono USB Streaming', 'Microfono USB Streaming - Proveedor', 'Microfono condensador para grabacion y streaming.', 'CHICO', 21300.00, 11, 0.20, 'https://images.unsplash.com/photo-1583394838336-acd977736f90?auto=format&fit=crop&w=800&q=60&sig=13'),
  (14, 'PV50-0014', 'DemoOffice', 'DO-0014', 'Silla Oficina Ergonomica', 'Silla Oficina Ergonomica - Proveedor', 'Silla ergonomica con soporte lumbar regulable.', 'GRANDE', 95400.00, 6, 0.18, 'https://images.unsplash.com/photo-1546435770-a3e426bf472b?auto=format&fit=crop&w=800&q=60&sig=14'),
  (15, 'PV50-0015', 'DemoCompute', 'DCP-0015', 'Notebook Ultraliviana 14', 'Notebook Ultraliviana 14 - Proveedor', 'Notebook liviana para trabajo remoto y estudio.', 'MEDIANO', 312000.00, 5, 0.15, 'https://images.unsplash.com/photo-1511467687858-23d96c32e4ae?auto=format&fit=crop&w=800&q=60&sig=15'),
  (16, 'PV50-0016', 'DemoCompute', 'DCP-0016', 'Tablet Android 10', 'Tablet Android 10 - Proveedor', 'Tablet Android con pantalla de 10 pulgadas.', 'CHICO', 102000.00, 8, 0.18, 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?auto=format&fit=crop&w=800&q=60&sig=16'),
  (17, 'PV50-0017', 'DemoWear', 'DW-0017', 'Smartwatch Deportivo', 'Smartwatch Deportivo - Proveedor', 'Reloj inteligente con monitoreo de actividad.', 'CHICO', 43600.00, 14, 0.22, 'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=800&q=60&sig=17'),
  (18, 'PV50-0018', 'DemoPrint', 'DP-0018', 'Impresora Multifuncion WiFi', 'Impresora Multifuncion WiFi - Proveedor', 'Impresora con escaner y conectividad inalambrica.', 'MEDIANO', 128000.00, 7, 0.18, 'https://images.unsplash.com/photo-1517336714739-489689fd1ca8?auto=format&fit=crop&w=800&q=60&sig=18'),
  (19, 'PV50-0019', 'DemoStorage', 'DST-0019', 'Disco SSD Externo 1TB', 'Disco SSD Externo 1TB - Proveedor', 'Unidad externa de alta velocidad y 1TB.', 'CHICO', 55800.00, 15, 0.20, 'https://images.unsplash.com/photo-1583394838336-acd977736f90?auto=format&fit=crop&w=800&q=60&sig=19'),
  (20, 'PV50-0020', 'DemoNet', 'DN-0020', 'Router WiFi 6 Hogar', 'Router WiFi 6 Hogar - Proveedor', 'Router WiFi 6 para cobertura domestica ampliada.', 'CHICO', 38400.00, 13, 0.20, 'https://images.unsplash.com/photo-1546435770-a3e426bf472b?auto=format&fit=crop&w=800&q=60&sig=20'),
  (21, 'PV50-0021', 'DemoHome', 'DH-0021', 'Lampara LED Escritorio', 'Lampara LED Escritorio - Proveedor', 'Lampara regulable para escritorio y estudio.', 'CHICO', 9400.00, 24, 0.25, 'https://images.unsplash.com/photo-1511467687858-23d96c32e4ae?auto=format&fit=crop&w=800&q=60&sig=21'),
  (22, 'PV50-0022', 'DemoHome', 'DH-0022', 'Cafetera Capsulas Compacta', 'Cafetera Capsulas Compacta - Proveedor', 'Cafetera compacta para cocina u oficina.', 'MEDIANO', 47400.00, 10, 0.18, 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?auto=format&fit=crop&w=800&q=60&sig=22'),
  (23, 'PV50-0023', 'DemoHome', 'DH-0023', 'Aspiradora Robot Smart', 'Aspiradora Robot Smart - Proveedor', 'Aspiradora robot con mapeo basico.', 'MEDIANO', 146000.00, 6, 0.16, 'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=800&q=60&sig=23'),
  (24, 'PV50-0024', 'DemoKitchen', 'DK-0024', 'Freidora de Aire 5L', 'Freidora de Aire 5L - Proveedor', 'Freidora de aire con panel digital.', 'MEDIANO', 61200.00, 11, 0.18, 'https://images.unsplash.com/photo-1517336714739-489689fd1ca8?auto=format&fit=crop&w=800&q=60&sig=24'),
  (25, 'PV50-0025', 'DemoKitchen', 'DK-0025', 'Batidora Planetaria 1200W', 'Batidora Planetaria 1200W - Proveedor', 'Batidora con bowl de gran capacidad.', 'MEDIANO', 85200.00, 8, 0.18, 'https://images.unsplash.com/photo-1583394838336-acd977736f90?auto=format&fit=crop&w=800&q=60&sig=25'),
  (26, 'PV50-0026', 'DemoKitchen', 'DK-0026', 'Juego de Sartenes 3 Piezas', 'Juego de Sartenes 3 Piezas - Proveedor', 'Set antiadherente para cocina diaria.', 'MEDIANO', 28900.00, 20, 0.22, 'https://images.unsplash.com/photo-1546435770-a3e426bf472b?auto=format&fit=crop&w=800&q=60&sig=26'),
  (27, 'PV50-0027', 'DemoTravel', 'DTR-0027', 'Mochila Urbana 20L', 'Mochila Urbana 20L - Proveedor', 'Mochila urbana con compartimento para notebook.', 'CHICO', 18600.00, 18, 0.25, 'https://images.unsplash.com/photo-1511467687858-23d96c32e4ae?auto=format&fit=crop&w=800&q=60&sig=27'),
  (28, 'PV50-0028', 'DemoTravel', 'DTR-0028', 'Valija Cabina Rigida', 'Valija Cabina Rigida - Proveedor', 'Valija de cabina con ruedas dobles.', 'GRANDE', 57200.00, 9, 0.20, 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?auto=format&fit=crop&w=800&q=60&sig=28'),
  (29, 'PV50-0029', 'DemoWear', 'DW-0029', 'Campera Impermeable Urbana', 'Campera Impermeable Urbana - Proveedor', 'Campera liviana para uso diario.', 'MEDIANO', 43200.00, 12, 0.22, 'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=800&q=60&sig=29'),
  (30, 'PV50-0030', 'DemoWear', 'DW-0030', 'Zapatillas Running Flex', 'Zapatillas Running Flex - Proveedor', 'Zapatillas livianas para entrenamiento.', 'MEDIANO', 51900.00, 16, 0.20, 'https://images.unsplash.com/photo-1517336714739-489689fd1ca8?auto=format&fit=crop&w=800&q=60&sig=30'),
  (31, 'PV50-0031', 'DemoMove', 'DM-0031', 'Bicicleta Plegable Urbana', 'Bicicleta Plegable Urbana - Proveedor', 'Bicicleta plegable para trayectos cortos.', 'GRANDE', 184000.00, 4, 0.15, 'https://images.unsplash.com/photo-1583394838336-acd977736f90?auto=format&fit=crop&w=800&q=60&sig=31'),
  (32, 'PV50-0032', 'DemoTools', 'DTL-0032', 'Kit Herramientas 45 Piezas', 'Kit Herramientas 45 Piezas - Proveedor', 'Maletin con herramientas para hogar y taller.', 'MEDIANO', 33800.00, 14, 0.22, 'https://images.unsplash.com/photo-1546435770-a3e426bf472b?auto=format&fit=crop&w=800&q=60&sig=32'),
  (33, 'PV50-0033', 'DemoTools', 'DTL-0033', 'Taladro Percutor 650W', 'Taladro Percutor 650W - Proveedor', 'Taladro percutor para trabajos generales.', 'MEDIANO', 46800.00, 10, 0.20, 'https://images.unsplash.com/photo-1511467687858-23d96c32e4ae?auto=format&fit=crop&w=800&q=60&sig=33'),
  (34, 'PV50-0034', 'DemoGarden', 'DG-0034', 'Jardinera Electrica Compacta', 'Jardinera Electrica Compacta - Proveedor', 'Herramienta electrica para mantenimiento de jardin.', 'GRANDE', 73400.00, 6, 0.18, 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?auto=format&fit=crop&w=800&q=60&sig=34'),
  (35, 'PV50-0035', 'DemoLiving', 'DL-0035', 'Juego de Comedor 4 Sillas', 'Juego de Comedor 4 Sillas - Proveedor', 'Juego de comedor para ambientes compactos.', 'GRANDE', 214000.00, 3, 0.15, 'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=800&q=60&sig=35'),
  (36, 'PV50-0036', 'DemoLiving', 'DL-0036', 'Mesa Ratona Nordica', 'Mesa Ratona Nordica - Proveedor', 'Mesa ratona de estilo nordico.', 'GRANDE', 49800.00, 8, 0.20, 'https://images.unsplash.com/photo-1517336714739-489689fd1ca8?auto=format&fit=crop&w=800&q=60&sig=36'),
  (37, 'PV50-0037', 'DemoPhone', 'DPH-0037', 'Smartphone 128GB', 'Smartphone 128GB - Proveedor', 'Telefono inteligente con 128GB de almacenamiento.', 'CHICO', 198000.00, 7, 0.15, 'https://images.unsplash.com/photo-1583394838336-acd977736f90?auto=format&fit=crop&w=800&q=60&sig=37'),
  (38, 'PV50-0038', 'DemoPhone', 'DPH-0038', 'Smartphone 256GB', 'Smartphone 256GB - Proveedor', 'Telefono inteligente con 256GB y carga rapida.', 'CHICO', 254000.00, 5, 0.15, 'https://images.unsplash.com/photo-1546435770-a3e426bf472b?auto=format&fit=crop&w=800&q=60&sig=38'),
  (39, 'PV50-0039', 'DemoTV', 'DTV-0039', 'Smart TV 43', 'Smart TV 43 - Proveedor', 'Televisor 43 pulgadas con apps integradas.', 'GRANDE', 236000.00, 5, 0.15, 'https://images.unsplash.com/photo-1511467687858-23d96c32e4ae?auto=format&fit=crop&w=800&q=60&sig=39'),
  (40, 'PV50-0040', 'DemoTV', 'DTV-0040', 'Smart TV 50 4K', 'Smart TV 50 4K - Proveedor', 'Televisor 4K de 50 pulgadas.', 'GRANDE', 348000.00, 4, 0.15, 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?auto=format&fit=crop&w=800&q=60&sig=40'),
  (41, 'PV50-0041', 'DemoClimate', 'DCL-0041', 'Ventilador Torre', 'Ventilador Torre - Proveedor', 'Ventilador de torre con control remoto.', 'GRANDE', 45200.00, 12, 0.18, 'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=800&q=60&sig=41'),
  (42, 'PV50-0042', 'DemoClimate', 'DCL-0042', 'Caloventor Ceramico', 'Caloventor Ceramico - Proveedor', 'Caloventor compacto para espacios pequenos.', 'MEDIANO', 27600.00, 18, 0.20, 'https://images.unsplash.com/photo-1517336714739-489689fd1ca8?auto=format&fit=crop&w=800&q=60&sig=42'),
  (43, 'PV50-0043', 'DemoFit', 'DF-0043', 'Balanza Inteligente', 'Balanza Inteligente - Proveedor', 'Balanza digital con medicion corporal.', 'CHICO', 14800.00, 19, 0.22, 'https://images.unsplash.com/photo-1583394838336-acd977736f90?auto=format&fit=crop&w=800&q=60&sig=43'),
  (44, 'PV50-0044', 'DemoFit', 'DF-0044', 'Mancuernas Ajustables', 'Mancuernas Ajustables - Proveedor', 'Set de mancuernas regulables para entrenamiento.', 'MEDIANO', 63800.00, 9, 0.18, 'https://images.unsplash.com/photo-1546435770-a3e426bf472b?auto=format&fit=crop&w=800&q=60&sig=44'),
  (45, 'PV50-0045', 'DemoBaby', 'DB-0045', 'Coche Paraguitas', 'Coche Paraguitas - Proveedor', 'Coche liviano plegable para bebe.', 'GRANDE', 72400.00, 6, 0.18, 'https://images.unsplash.com/photo-1511467687858-23d96c32e4ae?auto=format&fit=crop&w=800&q=60&sig=45'),
  (46, 'PV50-0046', 'DemoBaby', 'DB-0046', 'Silla Comer Bebe', 'Silla Comer Bebe - Proveedor', 'Silla de comer plegable y facil de limpiar.', 'GRANDE', 68400.00, 7, 0.18, 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?auto=format&fit=crop&w=800&q=60&sig=46'),
  (47, 'PV50-0047', 'DemoPet', 'DPT-0047', 'Fuente Agua Mascotas', 'Fuente Agua Mascotas - Proveedor', 'Fuente de agua automatica para mascotas.', 'CHICO', 17400.00, 15, 0.22, 'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=800&q=60&sig=47'),
  (48, 'PV50-0048', 'DemoPet', 'DPT-0048', 'Rascador Gatos Torre', 'Rascador Gatos Torre - Proveedor', 'Rascador vertical para entretenimiento felino.', 'GRANDE', 36800.00, 10, 0.20, 'https://images.unsplash.com/photo-1517336714739-489689fd1ca8?auto=format&fit=crop&w=800&q=60&sig=48'),
  (49, 'PV50-0049', 'DemoAuto', 'DAU-0049', 'Aspiradora Auto 12V', 'Aspiradora Auto 12V - Proveedor', 'Aspiradora compacta para limpieza de vehiculos.', 'CHICO', 13200.00, 16, 0.25, 'https://images.unsplash.com/photo-1583394838336-acd977736f90?auto=format&fit=crop&w=800&q=60&sig=49'),
  (50, 'PV50-0050', 'DemoAuto', 'DAU-0050', 'Compresor Portatil Auto', 'Compresor Portatil Auto - Proveedor', 'Compresor portatil para inflado rapido.', 'CHICO', 21600.00, 12, 0.22, 'https://images.unsplash.com/photo-1546435770-a3e426bf472b?auto=format&fit=crop&w=800&q=60&sig=50');

INSERT INTO productos_canonicos (
  id, marca, modelo, nombre_publico, atributos_json, activo, created_at, updated_at
)
SELECT
  UUID(),
  t.marca,
  t.modelo,
  t.nombre_publico,
  JSON_OBJECT(
    'origen', 'seed_sql_dev',
    'sku', t.sku,
    'categoriaPaquete', t.categoria_paquete,
    'proveedorId', @proveedor_id
  ),
  b'1',
  @now,
  @now
FROM tmp_catalogo_seed_50 t
LEFT JOIN productos_canonicos pc ON pc.modelo = t.modelo
WHERE pc.id IS NULL;

INSERT INTO productos (
  id, proveedor_id, producto_canonico_id, sku, nombre, descripcion, precio_costo,
  moneda, categoria_paquete, activo, created_at, updated_at
)
SELECT
  UUID(),
  @proveedor_id,
  pc.id,
  t.sku,
  t.nombre_proveedor,
  t.descripcion,
  t.precio_costo,
  'ARS',
  t.categoria_paquete,
  b'1',
  @now,
  @now
FROM tmp_catalogo_seed_50 t
INNER JOIN productos_canonicos pc ON pc.modelo = t.modelo
LEFT JOIN productos p ON p.sku = t.sku
WHERE p.id IS NULL;

INSERT INTO proveedor_stock (
  id, proveedor_id, producto_id, stock, precio_costo_actual, created_at, updated_at
)
SELECT
  UUID(),
  @proveedor_id,
  p.id,
  t.stock,
  t.precio_costo,
  @now,
  @now
FROM tmp_catalogo_seed_50 t
INNER JOIN productos p ON p.sku = t.sku
LEFT JOIN proveedor_stock ps
  ON ps.proveedor_id = @proveedor_id
 AND ps.producto_id = p.id
WHERE ps.id IS NULL;

INSERT INTO revendedor_markups (
  id, revendedor_id, producto_id, markup_porcentaje, markup_fijo, activo, created_at, updated_at
)
SELECT
  UUID(),
  @revendedor_id,
  p.id,
  t.markup_porcentaje,
  0.00,
  b'1',
  @now,
  @now
FROM tmp_catalogo_seed_50 t
INNER JOIN productos p ON p.sku = t.sku
INNER JOIN revendedores r ON r.id = @revendedor_id
LEFT JOIN revendedor_markups rm
  ON rm.revendedor_id = @revendedor_id
 AND rm.producto_id = p.id
WHERE rm.id IS NULL;

INSERT INTO producto_media (
  id, producto_id, tipo_media, url, orden, es_principal, created_at, updated_at
)
SELECT
  UUID(),
  p.id,
  'IMAGEN',
  t.imagen_url,
  1,
  b'1',
  @now,
  @now
FROM tmp_catalogo_seed_50 t
INNER JOIN productos p ON p.sku = t.sku
LEFT JOIN producto_media pm
  ON pm.producto_id = p.id
 AND pm.es_principal = b'1'
WHERE pm.id IS NULL;

SELECT 'productos_canonicos' AS tabla, COUNT(*) AS cantidad
FROM productos_canonicos pc
INNER JOIN tmp_catalogo_seed_50 t ON t.modelo = pc.modelo;

SELECT 'productos' AS tabla, COUNT(*) AS cantidad
FROM productos p
INNER JOIN tmp_catalogo_seed_50 t ON t.sku = p.sku;

SELECT 'proveedor_stock' AS tabla, COUNT(*) AS cantidad
FROM proveedor_stock ps
INNER JOIN productos p ON p.id = ps.producto_id
INNER JOIN tmp_catalogo_seed_50 t ON t.sku = p.sku;

SELECT 'revendedor_markups' AS tabla, COUNT(*) AS cantidad
FROM revendedor_markups rm
INNER JOIN productos p ON p.id = rm.producto_id
INNER JOIN tmp_catalogo_seed_50 t ON t.sku = p.sku
WHERE rm.revendedor_id = @revendedor_id;

SELECT 'producto_media' AS tabla, COUNT(*) AS cantidad
FROM producto_media pm
INNER JOIN productos p ON p.id = pm.producto_id
INNER JOIN tmp_catalogo_seed_50 t ON t.sku = p.sku;
