# Playbook Rapido - Alta de Storefront por Dominio en VPS

Uso: cuando llega un cliente nuevo (ejemplo "Juan Perez") y hay que apuntar su dominio al VPS y dejar Nginx + SSL operativos.

## Variables que cambian siempre

- `DOMINIO` = dominio principal del cliente (ejemplo: `juanstore.com`)
- `WWW` = `www.DOMINIO` (ejemplo: `www.juanstore.com`)
- `ARCHIVO_NGINX` = nombre del archivo en Nginx (normalmente igual a `DOMINIO`)
- `IP_VPS` = IP del servidor (actual: `66.97.44.145`)

## 1) DNS (en Donweb o proveedor)

Configurar:

- Registro `A` de `@` -> `IP_VPS`
- Registro `www`:
  - `CNAME` -> `DOMINIO` (recomendado), o
  - `A` -> `IP_VPS`

Ejemplo Juan:

- `@` -> `66.97.44.145`
- `www` -> `juanstore.com`

## 2) Nginx: que reemplazar en el bloque

En el template Nginx, solo cambian estos valores:

1. Ruta de archivo:
   - `/etc/nginx/sites-available/navarroventas.com`
   - `/etc/nginx/sites-enabled/navarroventas.com`
   por:
   - `/etc/nginx/sites-available/DOMINIO`
   - `/etc/nginx/sites-enabled/DOMINIO`

2. `server_name`:

```nginx
server_name DOMINIO WWW;
```

Ejemplo real:

```nginx
server_name juanstore.com www.juanstore.com;
```

Template:

```nginx
server {
    listen 80;
    listen [::]:80;
    server_name DOMINIO WWW;

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

    location / {
        try_files $uri /index.html;
    }
}
```

## 3) Habilitar sitio

```bash
sudo ln -s /etc/nginx/sites-available/DOMINIO /etc/nginx/sites-enabled/DOMINIO
sudo nginx -t
sudo systemctl reload nginx
```

## 4) SSL (HTTPS)

```bash
sudo certbot --nginx -d DOMINIO -d WWW --redirect
sudo nginx -t
sudo systemctl reload nginx
```

## 5) Validacion final

```bash
curl -I https://DOMINIO
curl -I https://WWW
curl "http://127.0.0.1:8081/public/storefront/resolve?host=DOMINIO"
```

Esperado:

- `https://DOMINIO` y `https://WWW` devuelven `200` o redireccion valida.
- `resolve?host=DOMINIO` devuelve JSON del storefront correcto.
