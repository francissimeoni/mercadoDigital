# Plan de despliegue producción - Mercado Digital con Nginx

## Contexto

El servidor VPS ya tiene una aplicación funcionando correctamente: **Subasta Inversa / Quien Da Menos (QDM)**.

Esa aplicación **NO debe tocarse ni modificarse**.

Actualmente el servidor tiene una mezcla histórica de nombres de carpetas porque inicialmente QDM fue subido bajo el dominio `mercado-digital.com.ar` y luego se desacopló hacia `subastainversa.online`.

Por ese motivo, **no se debe confiar en los nombres actuales de carpetas para decidir qué app es cuál**.

---

## Estado actual observado en el servidor

### Frontend actual

En `/var/www` existen estas carpetas:

```text
/var/www/html
/var/www/mercado-digital
/var/www/qdm-frontend
```

La carpeta `/var/www/mercado-digital` contiene un build Vite con `index.html` y assets.

La carpeta `/var/www/qdm-frontend` también contiene un build Vite, pero actualmente no parece ser la carpeta usada por Nginx para el dominio activo de Subasta Inversa.

---

### Backend actual

En `/opt` existe:

```text
/opt/quien-da-menos
```

El único proceso Java actualmente corriendo es:

```text
/usr/bin/java -jar /opt/quien-da-menos/backend/app.jar
```

Ese backend escucha en:

```text
localhost:8080
```

Este backend corresponde a **Subasta Inversa / QDM**.

---

### Nginx actual

Existen estos sitios habilitados:

```text
/etc/nginx/sites-enabled/mercado-digital.com.ar
/etc/nginx/sites-enabled/subastainversa.online
```

Actualmente ambos dominios apuntan a la misma carpeta frontend:

```text
root /var/www/mercado-digital;
```

Y ambos derivan `/api/` al mismo backend:

```text
proxy_pass http://127.0.0.1:8080;
```

Conclusión:

```text
mercado-digital.com.ar    -> frontend histórico /var/www/mercado-digital -> backend QDM 8080
subastainversa.online     -> frontend histórico /var/www/mercado-digital -> backend QDM 8080
```

Por lo tanto, **Mercado Digital todavía no está desplegado como aplicación independiente**.

---

## Objetivo

Desplegar **Mercado Digital** en producción sin romper ni modificar Subasta Inversa.

El resultado buscado es:

```text
Subasta Inversa / QDM
- Dominio: subastainversa.online
- Backend: /opt/quien-da-menos/backend/app.jar
- Puerto backend: 8080
- Estado: NO TOCAR

Mercado Digital
- Dominio: mercado-digital.com.ar
- Backend: /opt/mercado-digital/backend/app.jar
- Puerto backend: 8081
- Frontend: /var/www/mercado-digital-web
- Estado: NUEVO despliegue separado
```

---

## Reglas obligatorias

### NO tocar

No modificar ni borrar:

```text
/opt/quien-da-menos
/var/www/qdm-frontend
/etc/nginx/sites-available/subastainversa.online
/etc/nginx/sites-enabled/subastainversa.online
```

No detener ni reiniciar innecesariamente el backend de QDM.

No usar el puerto `8080` para Mercado Digital.

No asumir que `/var/www/mercado-digital` pertenece a Mercado Digital real, porque hoy está históricamente mezclado con QDM.

---

### SÍ crear/modificar

Crear estructura nueva y limpia para Mercado Digital:

```text
/opt/mercado-digital/backend
/var/www/mercado-digital-web
/etc/systemd/system/mercado-digital.service
```

Modificar únicamente:

```text
/etc/nginx/sites-available/mercado-digital.com.ar
```

para que apunte al nuevo frontend y al nuevo backend de Mercado Digital.

---

## Arquitectura objetivo

```text
Internet
│
├── https://subastainversa.online
│   ├── Frontend QDM existente
│   └── Backend QDM localhost:8080
│
└── https://mercado-digital.com.ar
    ├── Frontend Mercado Digital /var/www/mercado-digital-web
    └── Backend Mercado Digital localhost:8081
```

---

## Backend Mercado Digital

### Puerto

Mercado Digital debe correr en:

```properties
server.port=8081
```

Nunca debe correr en `8080`.

---

### Perfil productivo

Usar perfil:

```text
prod
```

El backend debe levantarse con:

```bash
java -jar /opt/mercado-digital/backend/app.jar --spring.profiles.active=prod --server.port=8081
```

---

### Variables sensibles

No hardcodear secretos en el repo.

Usar archivo de entorno en el servidor:

```text
/opt/mercado-digital/backend/.env
```

Ejemplos de variables esperadas:

```env
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/mercado_digital
SPRING_DATASOURCE_USERNAME=mercado_digital_user
SPRING_DATASOURCE_PASSWORD=CAMBIAR
JWT_SECRET=CAMBIAR
MOBBEX_API_KEY=CAMBIAR
MOBBEX_ACCESS_TOKEN=CAMBIAR
```

Adaptar los nombres exactos a los usados por el proyecto.

---

## Servicio systemd para Mercado Digital

Crear:

```text
/etc/systemd/system/mercado-digital.service
```

Contenido sugerido:

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

Luego ejecutar:

```bash
sudo systemctl daemon-reload
sudo systemctl enable mercado-digital
sudo systemctl start mercado-digital
sudo systemctl status mercado-digital
```

Validar que escuche en 8081:

```bash
sudo ss -tulpn | grep 8081
```

---

## Frontend Mercado Digital

### Build

No usar Vite dev server en producción.

No usar puerto `5173` en producción.

Generar build:

```bash
npm install
npm run build
```

Copiar el contenido de `dist` a:

```text
/var/www/mercado-digital-web
```

Ejemplo:

```bash
sudo mkdir -p /var/www/mercado-digital-web
sudo rsync -av --delete dist/ /var/www/mercado-digital-web/
```

---

### Variables frontend

El frontend productivo debe apuntar al dominio real.

Ejemplo `.env.production`:

```env
VITE_API_BASE_URL=https://mercado-digital.com.ar/api
VITE_STOREFRONT_HOST=mercado-digital.com.ar
```

Ajustar según cómo esté definido el cliente Axios del proyecto.

---

## Nginx Mercado Digital

Modificar únicamente:

```text
/etc/nginx/sites-available/mercado-digital.com.ar
```

Config objetivo sugerida:

```nginx
server {
    listen 80;
    listen [::]:80;
    server_name mercado-digital.com.ar www.mercado-digital.com.ar;

    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;

    server_name mercado-digital.com.ar www.mercado-digital.com.ar;

    ssl_certificate     /etc/letsencrypt/live/mercado-digital.com.ar/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/mercado-digital.com.ar/privkey.pem;

    client_max_body_size 20M;

    add_header X-Frame-Options DENY always;
    add_header X-Content-Type-Options nosniff always;

    root /var/www/mercado-digital-web;
    index index.html;

    location /api/ {
        proxy_pass http://127.0.0.1:8081;
        proxy_http_version 1.1;

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        proxy_connect_timeout 10s;
        proxy_read_timeout 60s;
        proxy_send_timeout 60s;
    }

    location /webhooks/psp {
        proxy_pass http://127.0.0.1:8081;
        proxy_http_version 1.1;

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

Importante:

- Si el backend de Mercado Digital ya expone endpoints bajo `/public/**`, `/auth/login`, `/webhooks/psp`, etc., revisar si el frontend llama con prefijo `/api` o sin prefijo.
- No cambiar el archivo de Nginx de `subastainversa.online`.
- Verificar el path real del webhook PSP antes de cerrar la configuración final.

---

## Validación segura

Antes de recargar Nginx:

```bash
sudo nginx -t
```

Si está OK:

```bash
sudo systemctl reload nginx
```

Validaciones:

```bash
curl -I https://mercado-digital.com.ar
curl -I https://subastainversa.online
curl http://127.0.0.1:8081/actuator/health
```

Si no existe actuator, validar con algún endpoint público real, por ejemplo:

```bash
curl http://127.0.0.1:8081/public/storefront/resolve?host=mercado-digital.com.ar
```

---

## Checklist final

- [ ] QDM sigue funcionando en `subastainversa.online`.
- [ ] El proceso Java de QDM sigue en puerto `8080`.
- [ ] Mercado Digital corre en puerto `8081`.
- [ ] `mercado-digital.service` existe y queda habilitado.
- [ ] Nginx de `mercado-digital.com.ar` apunta a `/var/www/mercado-digital-web`.
- [ ] Nginx de `mercado-digital.com.ar` proxya backend a `127.0.0.1:8081`.
- [ ] No se modificó `subastainversa.online`.
- [ ] No se modificó `/opt/quien-da-menos`.
- [ ] No se modificó el puerto `8080`.
- [ ] El storefront público de Mercado Digital resuelve correctamente.
- [ ] El login operativo funciona.
- [ ] El flujo de checkout público crea pedido.
- [ ] El inicio PSP funciona.
- [ ] El webhook PSP queda accesible desde Internet.

---

## Prompt sugerido para Codex

```text
OBJETIVO
Preparar el proyecto Mercado Digital para despliegue productivo en un VPS con Nginx, sin tocar la aplicación existente Subasta Inversa / QDM.

CONTEXTO SERVIDOR
- QDM ya funciona y NO debe modificarse.
- QDM usa backend Java en /opt/quien-da-menos/backend/app.jar.
- QDM usa puerto 8080.
- Mercado Digital debe desplegarse separado.
- Mercado Digital debe usar backend en puerto 8081.
- Mercado Digital debe usar frontend estático en /var/www/mercado-digital-web.

TAREAS
1) Revisar configuración actual del proyecto para producción.
2) Confirmar variables necesarias para backend prod.
3) Confirmar variables necesarias para frontend prod.
4) Preparar instrucciones o scripts seguros para:
   - build backend
   - build frontend
   - copia de frontend dist
   - ubicación del jar
   - creación de servicio systemd mercado-digital
   - configuración Nginx mercado-digital.com.ar
5) No modificar nada relacionado con QDM/Subasta Inversa.
6) No usar puerto 8080 para Mercado Digital.
7) Documentar los pasos finales en docs/acta_deploy_produccion_nginx.md.

RESTRICCIONES
- No tocar /opt/quien-da-menos.
- No tocar /etc/nginx/sites-available/subastainversa.online.
- No tocar /var/www/qdm-frontend.
- No usar puerto 8080.
- No hardcodear secretos.
- Mantener compilable.
- No inventar endpoints.
- Verificar el path real de webhooks PSP según el backend actual.

TESTS / VALIDACIONES
- Backend compila.
- Frontend compila.
- Revisar que el frontend apunte correctamente al backend productivo.
- Validar que la configuración propuesta no afecte QDM.

ACTA
Crear docs/acta_deploy_produccion_nginx.md con:
- archivos tocados
- decisiones tomadas
- variables requeridas
- comandos de despliegue
- checklist de validación
```
