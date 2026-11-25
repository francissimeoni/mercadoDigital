# Acta deploy produccion Nginx - Mercado Digital

## Objetivo

Preparar Mercado Digital para despliegue productivo en VPS con Nginx, separado de Subasta Inversa / QDM.

QDM no debe tocarse. Mercado Digital debe quedar en:

- Backend: `/opt/mercado-digital/backend/app.jar`
- Puerto backend: `8081`
- Frontend: `/var/www/mercado-digital-web`
- Dominio: `mercado-digital.com.ar`

## Archivos agregados

- `backend/src/main/resources/application-prod.yml`
- `frontend/.env.production.example`
- `backend/docs/templates/mercado-digital.service`
- `backend/docs/templates/mercado-digital.com.ar.nginx`
- `backend/docs/templates/mercado-digital.backend.env.example`

## Decisiones

### Perfil prod

Se agrego `application-prod.yml` para evitar depender de valores locales de `application.yml`.

El backend productivo debe levantarse con:

```bash
java -jar /opt/mercado-digital/backend/app.jar --spring.profiles.active=prod --server.port=8081
```

### Puerto

Mercado Digital usa `8081`.

No usar `8080`, porque pertenece a QDM.

### Variables sensibles

No se hardcodean secretos en el repo. El servidor debe usar:

```text
/opt/mercado-digital/backend/.env
```

Base sugerida:

```bash
sudo mkdir -p /opt/mercado-digital/backend
sudo cp backend/docs/templates/mercado-digital.backend.env.example /opt/mercado-digital/backend/.env
sudo nano /opt/mercado-digital/backend/.env
```

Variables principales:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `APP_SECURITY_JWT_SECRET`
- `MERCADOPAGO_CLIENT_ID`
- `MERCADOPAGO_CLIENT_SECRET`
- `MERCADOPAGO_ACCESS_TOKEN`
- `MERCADOPAGO_PUBLIC_KEY`
- `MERCADOPAGO_RETURN_URL`
- `MERCADOPAGO_WEBHOOK_URL`

### Frontend productivo

El frontend debe compilar con:

```env
VITE_API_BASE_URL=https://mercado-digital.com.ar/api
VITE_STOREFRONT_HOST=mercado-digital.com.ar
```

Referencia:

```text
frontend/.env.production.example
```

### Nginx y prefijo `/api`

El frontend llama al backend usando `VITE_API_BASE_URL=https://mercado-digital.com.ar/api`.

Por eso Nginx debe quitar el prefijo `/api` antes de pasar al backend Spring.

La configuracion correcta usa barra final:

```nginx
location /api/ {
    proxy_pass http://127.0.0.1:8081/;
}
```

Sin esa barra final, Spring recibiria rutas como `/api/auth/login`, pero el backend expone `/auth/login`.

El webhook queda directo sin `/api`:

```text
https://mercado-digital.com.ar/webhooks/psp/mercadopago
```

## Comandos de build

### Backend

Desde `backend`:

```bash
./mvnw -DskipTests package
```

En Windows local:

```powershell
.\mvnw.cmd -DskipTests package
```

Jar esperado:

```text
backend/target/backend-0.0.1-SNAPSHOT.jar
```

Copiar al servidor como:

```bash
sudo mkdir -p /opt/mercado-digital/backend
sudo cp backend-0.0.1-SNAPSHOT.jar /opt/mercado-digital/backend/app.jar
```

### Frontend

Desde `frontend`:

```bash
npm install
cp .env.production.example .env.production
npm run build
```

Copiar `dist`:

```bash
sudo mkdir -p /var/www/mercado-digital-web
sudo rsync -av --delete dist/ /var/www/mercado-digital-web/
```

## Systemd

Instalar servicio:

```bash
sudo cp backend/docs/templates/mercado-digital.service /etc/systemd/system/mercado-digital.service
sudo systemctl daemon-reload
sudo systemctl enable mercado-digital
sudo systemctl start mercado-digital
sudo systemctl status mercado-digital
```

Validar puerto:

```bash
sudo ss -tulpn | grep 8081
```

## Nginx

Modificar solo:

```text
/etc/nginx/sites-available/mercado-digital.com.ar
```

Usar como base:

```text
backend/docs/templates/mercado-digital.com.ar.nginx
```

Validar:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

## Validaciones

Backend local servidor:

```bash
curl http://127.0.0.1:8081/actuator/health
```

Si actuator no responde, validar endpoint publico:

```bash
curl "http://127.0.0.1:8081/public/storefront/resolve?host=mercado-digital.com.ar"
```

Dominio Mercado Digital:

```bash
curl -I https://mercado-digital.com.ar
curl -I https://mercado-digital.com.ar/api/auth/login
```

QDM debe seguir funcionando:

```bash
curl -I https://subastainversa.online
sudo ss -tulpn | grep 8080
```

## Checklist

- [ ] No se modifico `/opt/quien-da-menos`.
- [ ] No se modifico `/etc/nginx/sites-available/subastainversa.online`.
- [ ] No se modifico `/var/www/qdm-frontend`.
- [ ] QDM sigue en puerto `8080`.
- [ ] Mercado Digital corre en puerto `8081`.
- [ ] `mercado-digital.service` queda activo.
- [ ] Nginx de Mercado Digital apunta a `/var/www/mercado-digital-web`.
- [ ] `/api/` proxya a `127.0.0.1:8081/` con barra final.
- [ ] `/webhooks/psp` proxya a `127.0.0.1:8081`.
- [ ] Login operativo funciona.
- [ ] Storefront publico resuelve.
- [ ] Checkout publico crea pedido.
- [ ] Inicio PSP funciona.
- [ ] Webhook PSP queda accesible desde Internet.
