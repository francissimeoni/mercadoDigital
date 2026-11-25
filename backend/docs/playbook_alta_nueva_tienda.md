# Playbook operativo - alta de nueva tienda (dominio + storefront)

## Objetivo

Habilitar una nueva tienda storefront en produccion de forma controlada:

1. DNS del dominio nuevo.
2. Nginx + SSL en VPS.
3. Publicacion de host en BD/Admin.
4. Validaciones tecnicas y funcionales.

Incluye pasos para despliegue de release (frontend/backend) cuando sea necesario.

---

## Convenciones usadas

- VPS: `66.97.44.145`
- SSH puerto: `5813`
- Usuario SSH: `root`
- Backend MD: `127.0.0.1:8081`
- Frontend MD: `/var/www/mercado-digital-web`
- Servicio systemd: `mercado-digital`
- Dominio ejemplo tienda: `mercado-digital.online` y `www.mercado-digital.online`

---

## Fase A - prerequisitos

### A1. Confirmar servicios base

```bash
sudo systemctl status nginx --no-pager
sudo systemctl status mercado-digital --no-pager
sudo ss -tulpn | grep -E '8080|8081'
```

Esperado:

- Nginx `active (running)`
- Backend MD `active (running)` en `8081`
- QDM en `8080` sigue vivo

---

## Fase B - DNS del dominio nuevo

En el proveedor DNS del dominio:

1. Crear registro `A`:
   - Nombre: `@`
   - Valor: `66.97.44.145`
2. Crear registro `A`:
   - Nombre: `www`
   - Valor: `66.97.44.145`

Verificacion (local):

```powershell
nslookup mercado-digital.online
nslookup www.mercado-digital.online
```

Debe resolver a la IP del VPS.

---

## Fase C - Nginx del dominio nuevo

### C1. Crear archivo del sitio

```bash
sudo nano /etc/nginx/sites-available/mercado-digital.online
```

Pegar:

```nginx
server {
    listen 80;
    listen [::]:80;
    server_name mercado-digital.online www.mercado-digital.online;

    return 301 https://www.mercado-digital.online$request_uri;
}

server {
    listen 443 ssl;
    listen [::]:443 ssl;
    server_name mercado-digital.online;

    ssl_certificate /etc/letsencrypt/live/mercado-digital.online/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/mercado-digital.online/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;

    return 301 https://www.mercado-digital.online$request_uri;
}

server {
    listen 443 ssl;
    listen [::]:443 ssl;
    server_name www.mercado-digital.online;

    ssl_certificate /etc/letsencrypt/live/mercado-digital.online/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/mercado-digital.online/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;

    client_max_body_size 20M;

    root /var/www/mercado-digital-web;
    index index.html;

    location /api/ {
        proxy_pass http://127.0.0.1:8081/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /webhooks/psp {
        proxy_pass http://127.0.0.1:8081;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /public/assets/storefront/ {
        proxy_pass http://127.0.0.1:8081/public/assets/storefront/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

### C2. Activar y validar

```bash
sudo ln -s /etc/nginx/sites-available/mercado-digital.online /etc/nginx/sites-enabled/mercado-digital.online
sudo nginx -t
sudo systemctl reload nginx
```

---

## Fase D - SSL con certbot

```bash
sudo certbot --nginx -d mercado-digital.online -d www.mercado-digital.online
sudo nginx -t
sudo systemctl reload nginx
```

Validacion:

```bash
curl -I https://mercado-digital.online
curl -I https://www.mercado-digital.online
```

---

## Fase E - alta de tienda en aplicacion (Admin/DB)

### E1. Cargar dominio exacto de la tienda

En Admin `/admin/storefronts` o por SQL:

```sql
USE mercado_digital;
UPDATE revendedores
SET dominio = 'www.mercado-digital.online', updated_at = NOW(6)
WHERE id = '<REVENDEDOR_ID>';
```

### E2. Storefront publicada y revendedor activo

```sql
USE mercado_digital;
SELECT r.id, r.nombre, r.dominio, r.activo, s.publicada
FROM revendedores r
LEFT JOIN revendedor_storefront s ON s.revendedor_id = r.id
WHERE r.id = '<REVENDEDOR_ID>';
```

Esperado: `activo=1` y `publicada=1`.

### E3. Branding/assets

En Admin subir logo, hero, favicon y guardar storefront.

Recomendacion:

- guardar URLs como rutas relativas:
  - `/public/assets/storefront/<archivo>`

Chequeo:

```sql
USE mercado_digital;
SELECT logo_url, hero_imagen_url, favicon_url, color_primario, color_secundario
FROM revendedor_storefront
WHERE revendedor_id = '<REVENDEDOR_ID>';
```

---

## Fase F - validaciones tecnicas

### F1. Resolver storefront por host

```bash
curl "http://127.0.0.1:8081/public/storefront/resolve?host=www.mercado-digital.online"
```

Debe devolver `publicada=true` y branding no nulo.

### F2. Verificar assets por dominio final

```bash
curl -I "https://www.mercado-digital.online/public/assets/storefront/<hero-o-logo>"
```

Esperado:

- `HTTP/2 200`
- `content-type: image/*`

Si devuelve `text/html`, falta/corrige `location /public/assets/storefront/` en Nginx.

### F3. Verificacion browser

- Abrir `https://www.mercado-digital.online`
- `Ctrl+F5`
- Validar logo, hero, colores, favicon

---

## Fase G - despliegue de release (cuando hay cambios de app)

Esta fase solo si hubo cambios de codigo. Para alta de dominio puro puede no hacer falta.

### G1. Backend (build + copia jar)

Local Windows:

```powershell
cd C:\sistemas\MercadoDigital\backend
.\mvnw.cmd -DskipTests package
scp -P 5813 .\target\backend-0.0.1-SNAPSHOT.jar root@66.97.44.145:/tmp/mercado-digital-app.jar
```

En VPS:

```bash
sudo mkdir -p /opt/mercado-digital/backend
sudo mv /tmp/mercado-digital-app.jar /opt/mercado-digital/backend/app.jar
sudo systemctl restart mercado-digital
sudo systemctl status mercado-digital --no-pager
```

### G2. Frontend (build + rsync)

Local:

```powershell
cd C:\sistemas\MercadoDigital\frontend
npm install
npm run build
```

Copiar `dist/` al VPS (elegir metodo):

- `scp -P 5813` por archivo/carpeta
- `rsync` si esta disponible

En VPS:

```bash
sudo mkdir -p /var/www/mercado-digital-web
sudo rsync -av --delete /ruta/copiada/dist/ /var/www/mercado-digital-web/
sudo chown -R www-data:www-data /var/www/mercado-digital-web
sudo find /var/www/mercado-digital-web -type d -exec chmod 755 {} \;
sudo find /var/www/mercado-digital-web -type f -exec chmod 644 {} \;
sudo nginx -t
sudo systemctl reload nginx
```

---

## Troubleshooting rapido

### 1) `scp` timeout al puerto 22

El VPS usa puerto SSH custom:

```powershell
scp -P 5813 ...
```

### 2) Login devuelve 400 al usar `admin`

El login valida email. Usar email valido como usuario.

### 3) Storefront 404 por host

Revisar typo en dominio (`mercado-digital` vs `mercadodigital`) y `www` vs no `www`.

### 4) Upload hero 500

Ajustar limites multipart en `.env` y reiniciar:

```env
SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE=8MB
SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE=10MB
```

### 5) Imagen devuelve HTML en vez de imagen

Falta proxy de `/public/assets/storefront/` en Nginx o esta debajo de un bloque que no matchea.

---

## Checklist final (alta tienda)

- [ ] DNS `@` y `www` apuntan al VPS.
- [ ] Nginx del dominio creado y activo.
- [ ] Certificado SSL emitido para apex + www.
- [ ] Dominio exacto cargado en `revendedores.dominio`.
- [ ] Revendedor activo y storefront publicada.
- [ ] logo/hero/favicon guardados y accesibles.
- [ ] `resolve?host=<dominio>` devuelve 200.
- [ ] storefront visual correcta en browser.
- [ ] portal/admin siguen operativos.
- [ ] QDM no afectado.

