-- Algunos accesos heredados usaban el email real del propietario, pero estaban
-- vinculados a un usuario sombra ASESOR_VENTA. Como el login tenant prioriza
-- storefront_credenciales, el propietario terminaba entrando como asesor.
--
-- Se conserva email_login y password_hash para no cambiar la clave que el
-- propietario ya usa en su storefront; solo se corrige la identidad asociada.

UPDATE storefront_credenciales credencial
JOIN revendedores revendedor
  ON revendedor.id = credencial.revendedor_id
JOIN usuarios propietario
  ON propietario.id = revendedor.usuario_id
LEFT JOIN storefront_credenciales credencial_propietario
  ON credencial_propietario.revendedor_id = revendedor.id
 AND credencial_propietario.usuario_id = propietario.id
 AND credencial_propietario.tipo_acceso = 'REVENDEDOR'
 AND credencial_propietario.id <> credencial.id
SET credencial.usuario_id = propietario.id,
    credencial.tipo_acceso = 'REVENDEDOR',
    credencial.activo = propietario.activo,
    credencial.updated_at = CURRENT_TIMESTAMP(6)
WHERE LOWER(credencial.email_login) = LOWER(propietario.email)
  AND (credencial.usuario_id <> propietario.id OR credencial.tipo_acceso <> 'REVENDEDOR')
  AND credencial_propietario.id IS NULL;
