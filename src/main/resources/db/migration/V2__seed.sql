-- Seed consolidado: roles base + permisos Unix-style por recurso.
--
-- Recursos protegidos por Auth Service (resource_uuid fijos, coinciden con
-- el enum cl.duocuc.edutrack.ms.auth.security.AuthResourceId):
--   a001 = USERS, a002 = ROLES, a003 = PERMISSIONS,
--   a004 = PERMISSIONS_EFFECTIVE, a005 = USER_ROLES
--
-- Comodín ALL (0000...0000): grant transversal — un único registro cubre
-- todos los recursos presentes y futuros (usado por SUPERUSER).
--
-- Nota: el usuario administrador inicial (admin@edutrack.cl) NO se siembra
-- aquí — lo crea `AdminSeeder.seedIfNeeded()` al primer arranque si la tabla
-- `auth.users` está vacía, con password hasheado vía bcrypt.

INSERT INTO auth.roles (name, description) VALUES
    ('SUPERUSER', 'Superusuario con acceso total al sistema'),
    ('ADMIN',     'Administrador del establecimiento educacional'),
    ('DOCENTE',   'Docente del establecimiento');

INSERT INTO auth.role_permissions (role_id, resource_uuid, flags)
SELECT r.id, seed.resource_uuid, seed.flags
FROM (VALUES
    -- SUPERUSER: comodín ALL ⇒ rwx sobre cualquier recurso.
    ('SUPERUSER', '00000000-0000-0000-0000-000000000000'::uuid, 7::smallint),
    -- ADMIN: rwx explícito sobre cada recurso de Auth.
    ('ADMIN',     '00000000-0000-0000-0000-00000000a001'::uuid, 7::smallint),
    ('ADMIN',     '00000000-0000-0000-0000-00000000a002'::uuid, 7::smallint),
    ('ADMIN',     '00000000-0000-0000-0000-00000000a003'::uuid, 7::smallint),
    ('ADMIN',     '00000000-0000-0000-0000-00000000a004'::uuid, 7::smallint),
    ('ADMIN',     '00000000-0000-0000-0000-00000000a005'::uuid, 7::smallint),
    -- DOCENTE: solo lectura de roles y consulta de flags efectivos.
    ('DOCENTE',   '00000000-0000-0000-0000-00000000a002'::uuid, 4::smallint),
    ('DOCENTE',   '00000000-0000-0000-0000-00000000a004'::uuid, 4::smallint)
) AS seed(role_name, resource_uuid, flags)
JOIN auth.roles r ON r.name = seed.role_name;
