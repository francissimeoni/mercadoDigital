# Playbook - Subida de Frontend y Backend a Nube (VPS)

Fecha: 2026-04-26

## Objetivo

Tener un procedimiento unico, corto y repetible para publicar cambios de `frontend` y `backend` en el VPS de Mercado Digital con validacion y rollback.

Base tecnica tomada de:

- `backend/docs/guia_configuracion_nube_mercado_digital.md`
- `docs/playbook_despliegue_mercadopago_servidor.md`

## Contexto actual esperado

- Backend en `127.0.0.1:8081`
- Frontend estatico en `/var/www/mercado-digital-web`
- Jar backend en `/opt/mercado-digital/backend/app.jar`
- Servicio systemd: `mercado-digital`
- Nginx como reverse proxy principal

## Pre-check rapido (antes de subir)

En servidor:

```bash
sudo systemctl status nginx --no-pager
sudo systemctl status mercado-digital --no-pager
sudo ss -tulpn | grep -E '8081'
```

En local:

```powershell
cd C:\sistemas\MercadoDigital\frontend
npm run build
```

```powershell
cd C:\sistemas\MercadoDigital\backend
.\mvnw.cmd -DskipTests clean package
```

## A) Subida solo Frontend

### 1. Build local

```powershell
cd C:\sistemas\MercadoDigital\frontend
npm run build
```

### 2. Subir `dist` al servidor

Tu comando propuesto funciona:

```powershell
scp -P 5813 -r .\frontend\dist root@66.97.44.145:/tmp/mercado-digital-dist
```

Alternativa recomendada para copiar contenido (sin carpeta `dist` intermedia):

```powershell
scp -P 5813 -r .\frontend\dist\* root@66.97.44.145:/tmp/mercado-digital-dist/
```

### 3. Publicar en `/var/www`

```bash
ssh -p 5813 root@66.97.44.145
sudo mkdir -p /var/www/mercado-digital-web
sudo rsync -av --delete /tmp/mercado-digital-dist/ /var/www/mercado-digital-web/
sudo chown -R www-data:www-data /var/www/mercado-digital-web
sudo find /var/www/mercado-digital-web -type d -exec chmod 755 {} \;
sudo find /var/www/mercado-digital-web -type f -exec chmod 644 {} \;
sudo nginx -t
sudo systemctl reload nginx
```

### 4. Validacion post deploy frontend

- `https://mercado-digital.com.ar/portal/login` abre
- `https://<storefront>/` abre catalogo
- `https://<storefront>/quienes-somos` abre la vista institucional
- Navegacion catalogo -> producto -> checkout funciona

## B) Subida solo Backend

### 1. Build local

```powershell
cd C:\sistemas\MercadoDigital\backend
.\mvnw.cmd -DskipTests clean package
```

Artifact esperado:

- `backend\target\backend-*.jar`

### 2. Subir jar a temporal

```powershell
scp -P 5813 .\backend\target\backend-*.jar root@66.97.44.145:/tmp/mercado-digital-app.jar
```

### 3. Backup + publicacion + restart

```bash
ssh -p 5813 root@66.97.44.145
sudo mkdir -p /opt/mercado-digital/backups
sudo test -f /opt/mercado-digital/backend/app.jar && sudo cp /opt/mercado-digital/backend/app.jar /opt/mercado-digital/backups/app.jar.$(date +%Y%m%d_%H%M%S)
sudo mv /tmp/mercado-digital-app.jar /opt/mercado-digital/backend/app.jar
sudo chown root:root /opt/mercado-digital/backend/app.jar
sudo chmod 644 /opt/mercado-digital/backend/app.jar
sudo systemctl daemon-reload
sudo systemctl restart mercado-digital
sudo systemctl status mercado-digital --no-pager
```

### 4. Validacion post deploy backend

```bash
sudo journalctl -u mercado-digital -n 120 --no-pager
curl -I https://mercado-digital.com.ar/api/actuator/health
curl "http://127.0.0.1:8081/public/storefront/resolve?host=<dominio-storefront>"
```

Esperado:

- servicio `active (running)`
- puerto `8081` escuchando
- health check sin error

## C) Subida completa (Frontend + Backend)

Orden recomendado para minimizar impacto:

1. subir backend y reiniciar servicio
2. validar endpoints base
3. subir frontend
4. recargar nginx
5. validacion funcional final (portal + storefront + checkout)

## Rollback

### Rollback Frontend

Si guardaste backup de web previa:

```bash
sudo rsync -av --delete /var/www/mercado-digital-web-backup/ /var/www/mercado-digital-web/
sudo systemctl reload nginx
```

Si no hay backup, volver a subir el `dist` anterior conocido.

### Rollback Backend

```bash
ls -1t /opt/mercado-digital/backups/app.jar.* | head -n 3
sudo cp <backup-elegido> /opt/mercado-digital/backend/app.jar
sudo systemctl restart mercado-digital
sudo systemctl status mercado-digital --no-pager
```

## Checklist de cierre

- [ ] `mercado-digital` activo en `8081`
- [ ] `nginx -t` valido
- [ ] frontend publicado en `/var/www/mercado-digital-web`
- [ ] `/api` responde
- [ ] storefront principal abre
- [ ] catalogo, detalle y checkout navegan
- [ ] logs sin errores criticos en `journalctl`
