ALTER TABLE revendedor_storefront
    ADD COLUMN quienes_titulo VARCHAR(180) NULL,
    ADD COLUMN quienes_texto TEXT NULL,
    ADD COLUMN quienes_galeria_json TEXT NULL,
    ADD COLUMN instagram_url VARCHAR(500) NULL,
    ADD COLUMN facebook_url VARCHAR(500) NULL,
    ADD COLUMN tiktok_url VARCHAR(500) NULL,
    ADD COLUMN youtube_url VARCHAR(500) NULL;
