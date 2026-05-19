-- Permisos Unix-style (r=4, w=2, x=1) de los roles semilla (V2) sobre los
-- recursos que el propio Auth Service protege. Los resource_uuid son fijos y
-- deben coincidir con el enum AuthResourceId:
--   a001 = USERS, a002 = ROLES, a003 = PERMISSIONS,
--   a004 = PERMISSIONS_EFFECTIVE, a005 = USER_ROLES
--
-- Los roles V2 se insertan con UUID aleatorio, por eso se resuelven por nombre.
-- ON CONFLICT permite re-aplicar la política sin duplicar filas.

INSERT INTO auth.role_permissions (role_id, resource_uuid, flags)
SELECT r.id, seed.resource_uuid, seed.flags
FROM (VALUES
    -- SUPERUSER: control total sobre todos los recursos de Auth
    ('SUPERUSER', '00000000-0000-0000-0000-00000000a001'::uuid, 7::smallint),
    ('SUPERUSER', '00000000-0000-0000-0000-00000000a002'::uuid, 7::smallint),
    ('SUPERUSER', '00000000-0000-0000-0000-00000000a003'::uuid, 7::smallint),
    ('SUPERUSER', '00000000-0000-0000-0000-00000000a004'::uuid, 7::smallint),
    ('SUPERUSER', '00000000-0000-0000-0000-00000000a005'::uuid, 7::smallint),
    -- ADMIN: control total sobre todos los recursos de Auth
    ('ADMIN',     '00000000-0000-0000-0000-00000000a001'::uuid, 7::smallint),
    ('ADMIN',     '00000000-0000-0000-0000-00000000a002'::uuid, 7::smallint),
    ('ADMIN',     '00000000-0000-0000-0000-00000000a003'::uuid, 7::smallint),
    ('ADMIN',     '00000000-0000-0000-0000-00000000a004'::uuid, 7::smallint),
    ('ADMIN',     '00000000-0000-0000-0000-00000000a005'::uuid, 7::smallint),
    -- DOCENTE: solo lectura de roles y consulta de flags efectivos
    ('DOCENTE',   '00000000-0000-0000-0000-00000000a002'::uuid, 4::smallint),
    ('DOCENTE',   '00000000-0000-0000-0000-00000000a004'::uuid, 4::smallint)
) AS seed(role_name, resource_uuid, flags)
JOIN auth.roles r ON r.name = seed.role_name
ON CONFLICT ON CONSTRAINT uq_role_permissions_role_resource
DO UPDATE SET flags = EXCLUDED.flags;
