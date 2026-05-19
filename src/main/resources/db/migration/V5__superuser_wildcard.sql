-- SUPERUSER se vuelve omnipotente vía un único grant sobre el recurso comodín
-- ALL (resource_uuid 0000...0000 = "todos los recursos, presentes y futuros").
-- El filtro hace OR de los flags del recurso concreto con los del comodín, así
-- que SUPERUSER cubre cualquier AuthResourceId actual o futuro sin migración.

-- 1. Otorga rwx (7) sobre el comodín.
INSERT INTO auth.role_permissions (role_id, resource_uuid, flags)
SELECT r.id, '00000000-0000-0000-0000-000000000000'::uuid, 7::smallint
FROM auth.roles r
WHERE r.name = 'SUPERUSER'
ON CONFLICT ON CONSTRAINT uq_role_permissions_role_resource
DO UPDATE SET flags = EXCLUDED.flags;

-- 2. Elimina las filas por recurso concreto de SUPERUSER sembradas en V4:
--    ya son redundantes y "no definirle permisos a recursos concretos".
DELETE FROM auth.role_permissions
WHERE role_id = (SELECT id FROM auth.roles WHERE name = 'SUPERUSER')
  AND resource_uuid <> '00000000-0000-0000-0000-000000000000'::uuid;
