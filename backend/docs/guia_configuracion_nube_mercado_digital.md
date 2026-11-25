# Guia completa de configuracion en nube - Mercado Digital

## 1) Objetivo

Este documento concentra la configuracion productiva de Mercado Digital en VPS con Nginx, separada de QDM/Subasta Inversa.

Estado objetivo:

- QDM/Subasta Inversa:
  - Dominio: `subastainversa.online`
  - Backend: `127.0.0.1:8080`
  - No tocar
- Mercado Digital:
  - Dominio operativo: `mercado-digital.com.ar` (portal)
  - Dominios de storefront: por revendedor (ej: `www.mercado-digital.online`)
  - Backend: `127.0.0.1:8081`
  - Frontend estatico: `/var/www/mercado-digital-web`

---

## 2) Topologia en produccion

```text
Internet
  |
  +-- https://subastainversa.online
  |      +-- Nginx -> QDM frontend
  |      +-- /api -> 127.0.0.1:8080
  |
  +-- https://mercado-digital.com.ar
         +-- Nginx -> /var/www/mercado-digital-web (SPA)
         +-- /api -> 127.0.0.1:8081
         +-- /webhooks/psp -> 127.0.0.1:8081
         +-- /public/assets/storefront/ -> 127.0.0.1:8081/public/assets/storefront/
```

Nota: cada dominio nuevo de storefront debe resolver al mismo VPS y tener bloque Nginx propio con SSL.

---

## 3) Estructura de rutas en servidor

### Backend Mercado Digital

- Jar: `/opt/mercado-digital/backend/app.jar`
- Env: `/opt/mercado-digital/backend/.env`
- Assets storefront (disco): `/opt/mercado-digital/backend/storage/storefront-assets`

### Frontend Mercado Digital

- Build desplegado: `/var/www/mercado-digital-web`

### Nginx

- Sitio principal MD: `/etc/nginx/sites-available/mercado-digital.com.ar`
- Sitios storefront custom: `/etc/nginx/sites-available/<dominio>`

### Systemd

- Servicio backend MD: `/etc/systemd/system/mercado-digital.service`

---

## 4) Backend: servicio systemd

Archivo recomendado:

```ini
[Unit]
Description=Mercado Digital Backend
After=network.target

[Service]
User=root
WorkingDirectory=/opt/mercado-digital/backend
EnvironmentFile=/opt/mercado-digital/backend/.env
ExecStart=/usr/bin/java -jar /opt/mercado-digital/backend/app.jar --spring.profiles.active=prod --server.port=8081
Restart=always
RestartSec=5
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
```

Comandos:

```bash
sudo systemctl daemon-reload
sudo systemctl enable mercado-digital
sudo systemctl restart mercado-digital
sudo systemctl status mercado-digital --no-pager
sudo ss -tulpn | grep -E '8080|8081'
```

---

## 5) Variables de entorno backend (.env)

Ruta: `/opt/mercado-digital/backend/.env`

Base minima:

```env
SERVER_PORT=8081

SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3307/mercado_digital?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=admin
SPRING_DATASOURCE_PASSWORD=CAMBIAR

APP_SECURITY_JWT_SECRET=CAMBIAR
APP_SECURITY_JWT_EXPIRATION_SECONDS=3600

POST_COMPRA_EMAIL_TRANSPORT=log
POST_COMPRA_EMAIL_BASE_PUBLIC_URL=https://mercado-digital.com.ar

MERCADOPAGO_ACCESS_TOKEN=
MERCADOPAGO_PUBLIC_KEY=
MERCADOPAGO_CLIENT_ID=
MERCADOPAGO_CLIENT_SECRET=
MERCADOPAGO_REDIRECT_URI=https://mercado-digital.com.ar/public/mercadopago/oauth/callback
MERCADOPAGO_ADMIN_REDIRECT_URL=https://mercado-digital.com.ar/admin/storefronts
MERCADOPAGO_RETURN_URL=https://mercado-digital.com.ar/pago/retorno
MERCADOPAGO_WEBHOOK_URL=https://mercado-digital.com.ar/webhooks/psp/mercadopago
MERCADOPAGO_CURRENCY=ARS

SERVER_FORWARD_HEADERS_STRATEGY=framework
SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE=8MB
SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE=10MB
```

Notas:

- `SERVER_FORWARD_HEADERS_STRATEGY=framework` evita URLs absolutas incorrectas en uploads detras de proxy.
- limites multipart previenen error 500 en cargas de hero mas pesadas.

---

## 6) Frontend: build y despliegue

Local:

```powershell
cd C:\sistemas\MercadoDigital\frontend
npm install
npm run build
```

En servidor:

```bash
sudo mkdir -p /var/www/mercado-digital-web
sudo rsync -av --delete dist/ /var/www/mercado-digital-web/
sudo chown -R www-data:www-data /var/www/mercado-digital-web
sudo find /var/www/mercado-digital-web -type d -exec chmod 755 {} \;
sudo find /var/www/mercado-digital-web -type f -exec chmod 644 {} \;
```

---

## 7) Nginx: reglas clave para Mercado Digital

Dentro del server HTTPS de cada dominio MD/storefront:

```nginx
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
```

Para portal en raiz de `mercado-digital.com.ar`:

```nginx
location = / {
    return 302 /portal/login;
}
```

Validacion:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

---

## 8) SSL y certificados

Certbot por dominio:

```bash
sudo certbot --nginx -d mercado-digital.com.ar -d www.mercado-digital.com.ar
sudo certbot --nginx -d mercado-digital.online -d www.mercado-digital.online
```

Verificar renovacion:

```bash
sudo systemctl status certbot.timer --no-pager
sudo certbot renew --dry-run
```

---

## 9) DB y storefronts

El storefront se resuelve por host exacto (campo `revendedores.dominio`), mas `revendedor.activo=1` y `storefront.publicada=1`.

Consulta de control:

```sql
USE mercado_digital;
SELECT r.id, r.nombre, r.dominio, r.activo, s.publicada, s.logo_url, s.hero_imagen_url, s.favicon_url
FROM revendedores r
LEFT JOIN revendedor_storefront s ON s.revendedor_id = r.id;
```

Recomendacion:

- Guardar `logo_url`, `hero_imagen_url` y `favicon_url` como rutas relativas:
  - `/public/assets/storefront/<archivo>`

---

## 10) Comandos operativos utiles

### Salud de servicios

```bash
sudo systemctl status nginx --no-pager
sudo systemctl status mercado-digital --no-pager
sudo ss -tulpn | grep -E '8080|8081|3306|3307'
```

### Logs backend

```bash
sudo journalctl -u mercado-digital -n 200 --no-pager
sudo journalctl -u mercado-digital -f
```

### Resolucion storefront

```bash
curl "http://127.0.0.1:8081/public/storefront/resolve?host=www.mercado-digital.online"
```

### Verificacion de assets por dominio

```bash
curl -I "https://www.mercado-digital.online/public/assets/storefront/<archivo>"
```

Debe responder `content-type: image/*`, no `text/html`.

---

## 11) Checklist de cierre de entorno

- [ ] `mercado-digital.service` activo en `8081`.
- [ ] QDM sigue activo en `8080` y no fue alterado.
- [ ] Nginx validado con `nginx -t`.
- [ ] `/api/` y `/webhooks/psp` proxy correcto a `8081`.
- [ ] `/public/assets/storefront/` proxy correcto a backend.
- [ ] SSL valido en todos los dominios activos.
- [ ] `curl /public/storefront/resolve?host=<dominio>` responde 200 para cada tienda publicada.
- [ ] assets storefront devuelven `image/*`.
- [ ] login portal operativo.
- [ ] checkout/webhook operativo.

