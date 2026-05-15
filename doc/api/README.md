# API — Auth Service

Documentación de la API del microservicio `auth/`.

## Contenido

- [`openapi.yaml`](./openapi.yaml) — especificación OpenAPI 3.1 completa, con
  todos los endpoints, esquemas y ejemplos. Importable en Swagger UI, Redoc,
  Postman, Bruno (vía import), Stoplight, etc.
- [`bruno/`](./bruno/) — colección Bruno lista para abrir, con un request por
  endpoint, dos entornos (`Local`, `Production`) y scripts que persisten
  `accessToken` / `refreshToken` / `roleId` en variables tras login y creación.

## Resumen de endpoints

Todos los paths están bajo el contexto raíz del servicio (en local: `http://localhost:8080`).

| Método | Path | Auth | Descripción |
|---|---|---|---|
| POST | `/auth/login` | público | Login email + password → par de tokens |
| POST | `/auth/refresh` | público | Renueva access token (rota refresh) |
| POST | `/auth/logout` | header `X-User-Id` | Revoca sesiones del usuario |
| GET | `/.well-known/jwks.json` | público | JWKS para verificar firmas |
| GET | `/auth/users` | SUPERUSER \| ADMIN | Lista usuarios |
| POST | `/auth/users` | SUPERUSER \| ADMIN | Crea usuario |
| GET | `/auth/users/{id}` | self \| SUPERUSER \| ADMIN | Detalle de usuario |
| PUT | `/auth/users/{id}` | SUPERUSER \| ADMIN | Actualiza displayName / enabled |
| DELETE | `/auth/users/{id}` | SUPERUSER \| ADMIN | Soft delete (enabled=false) |
| DELETE | `/auth/users/{id}/sessions` | SUPERUSER \| ADMIN | Revoca sesiones |
| GET | `/auth/users/{userId}/roles` | self \| SUPERUSER \| ADMIN | Lista roles del usuario |
| POST | `/auth/users/{userId}/roles/{roleId}` | SUPERUSER \| ADMIN | Asigna rol |
| DELETE | `/auth/users/{userId}/roles/{roleId}` | SUPERUSER \| ADMIN | Revoca rol |
| GET | `/auth/roles` | SUPERUSER \| ADMIN \| DOCENTE | Lista roles |
| POST | `/auth/roles` | SUPERUSER \| ADMIN | Crea rol |
| GET | `/auth/roles/{id}` | SUPERUSER \| ADMIN \| DOCENTE | Detalle de rol |
| PUT | `/auth/roles/{id}` | SUPERUSER \| ADMIN | Actualiza rol |
| DELETE | `/auth/roles/{id}` | SUPERUSER \| ADMIN | Elimina rol |
| GET | `/auth/roles/{roleId}/permissions` | SUPERUSER \| ADMIN | Lista permisos del rol |
| PUT | `/auth/roles/{roleId}/permissions/{resourceUuid}` | SUPERUSER \| ADMIN | Upsert permiso |
| DELETE | `/auth/roles/{roleId}/permissions/{resourceUuid}` | SUPERUSER \| ADMIN | Elimina permiso |
| GET | `/auth/roles/{roleId}/permissions/effective` | SUPERUSER \| ADMIN \| DOCENTE | Flags efectivos (?resourceUuid=) |

## Headers de identidad

Auth Service confía en los headers que el API Gateway propaga tras validar el JWT:

- `X-User-Id`: UUID del usuario (claim `sub`).
- `X-User-Roles`: lista de roles separados por coma, ej. `SUPERUSER,ADMIN`.

Para pruebas directas contra el servicio (sin Gateway) basta con enviar esos
headers explícitamente — la colección Bruno los inyecta desde variables de
entorno (`{{userId}}`, `{{userRoles}}`).

## Permisos Unix-style

| Bit | Valor | Significado |
|---|---|---|
| r | 4 | read |
| w | 2 | write |
| x | 1 | execute / acción de dominio |

Los flags se combinan vía OR: `rwx = 7`, `rw- = 6`, `r-- = 4`. El
`resource_uuid` es opaco para Auth — cada microservicio registra sus propios
recursos en su dominio.

## Uso de la colección Bruno

1. Instalar Bruno: <https://www.usebruno.com/>.
2. Abrir colección → seleccionar la carpeta `doc/api/bruno/`.
3. Elegir el entorno `Local` (o `Production`).
4. Ejecutar `auth/Login` con las credenciales del admin sembrado
   (`admin@edutrack.cl` / `changeme123!` en dev). El script guardará
   `accessToken` y `refreshToken` automáticamente.
5. (Opcional) Rellenar manualmente `userId`, `roleId`, `resourceUuid` en el
   entorno con los UUIDs que devuelvan los endpoints correspondientes — o crear
   un rol con `roles/Create role` que poblará `roleId` automáticamente.

## Códigos de respuesta comunes

| Código | Significado |
|---|---|
| 200 | OK |
| 201 | Recurso creado |
| 204 | Operación exitosa sin contenido |
| 400 | Validación fallida o body mal formado |
| 401 | Credenciales inválidas o token revocado |
| 403 | Rol insuficiente |
| 404 | Recurso no encontrado |
| 409 | Conflicto de unicidad (email/nombre duplicado) |
