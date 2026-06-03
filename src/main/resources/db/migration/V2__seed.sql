-- Seed consolidado: roles base + permisos Unix-style por recurso.
--
-- Recursos protegidos por Auth Service (resource_key estables, coinciden con
-- el enum cl.duocuc.edutrack.ms.auth.security.AuthResourceId):
--   auth.users, auth.roles, auth.permissions,
--   auth.permissions.effective, auth.user-roles
--
-- Comodín ALL ('*'): grant transversal — un único registro cubre todos los
-- recursos presentes y futuros (usado por SUPERUSER).
--
-- Nota: el usuario administrador inicial (admin@edutrack.cl) NO se siembra
-- aquí — lo crea `AdminSeeder.seedIfNeeded()` al primer arranque si la tabla
-- `auth.users` está vacía, con password hasheado vía bcrypt.

INSERT INTO auth.roles (name, description, creator_user, updater_user) VALUES
    ('SUPERUSER', 'Superusuario con acceso total al sistema', '019e5c76-eaed-72c1-ad2c-c3bd0536d71d', '019e5c76-eaed-72c1-ad2c-c3bd0536d71d'),
    ('ADMIN',     'Administrador del establecimiento educacional', '019e5c76-eaed-72c1-ad2c-c3bd0536d71d', '019e5c76-eaed-72c1-ad2c-c3bd0536d71d'),
    ('DOCENTE',   'Docente del establecimiento', '019e5c76-eaed-72c1-ad2c-c3bd0536d71d', '019e5c76-eaed-72c1-ad2c-c3bd0536d71d');

INSERT INTO auth.role_permissions (role_id, resource_key, flags, creator_user, updater_user)
SELECT r.id, seed.resource_key, seed.flags, '019e5c76-eaed-72c1-ad2c-c3bd0536d71d', '019e5c76-eaed-72c1-ad2c-c3bd0536d71d'
FROM (VALUES
    -- SUPERUSER: comodín ALL ⇒ rwx sobre cualquier recurso.
    ('SUPERUSER', '*',                          7::smallint),
    -- ADMIN: rwx explícito sobre cada recurso de Auth.
    ('ADMIN',     'auth.users',                 7::smallint),
    ('ADMIN',     'auth.roles',                 7::smallint),
    ('ADMIN',     'auth.permissions',           7::smallint),
    ('ADMIN',     'auth.permissions.effective', 7::smallint),
    ('ADMIN',     'auth.user-roles',            7::smallint),
    -- DOCENTE: solo lectura de roles y consulta de flags efectivos.
    ('DOCENTE',   'auth.roles',                 4::smallint),
    ('DOCENTE',   'auth.permissions.effective', 4::smallint)
) AS seed(role_name, resource_key, flags)
JOIN auth.roles r ON r.name = seed.role_name;
