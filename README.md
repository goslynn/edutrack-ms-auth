# Auth Service — EduTrack

Único emisor de **JWT RS256** en la plataforma. Gestiona usuarios, roles dinámicos y un modelo de permisos Unix-style (`r=4, w=2, x=1`) almacenados como flags por par `(role, resource_key)`. El Gateway valida el JWT en cada request; Auth solo se consulta en login, refresh, revocación y verificación de permisos.

- **Path raíz (Gateway):** `/auth`
- **Schema BD:** `auth` (PostgreSQL compartido)
- **App Fly.io:** `edutrack-auth`

---

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| Runtime | Java 21 + Quarkus 3.34.6 |
| API | RESTEasy Reactive (JAX-RS) |
| Persistencia | Hibernate ORM Panache — Active Record |
| Base de datos | PostgreSQL 15+ |
| Migraciones | Flyway (corre automáticamente al arrancar) |
| JWT | SmallRye JWT (RS256) |
| Hash contraseñas | BCrypt (`at.favre.lib:bcrypt` 0.10.2) |
| Serialización | Jackson + `@JsonView` |
| Validación | Hibernate Validator (Jakarta Bean Validation) |
| Documentación | SmallRye OpenAPI + Swagger UI |
| Seguridad | `edutrack-ms-commons` — cabeceras internas del Gateway |
| Build | Maven Wrapper (`./mvnw`) |

---

## Prerrequisitos

- Java 21
- PostgreSQL accesible con schema `auth` y usuario `auth_user`
- `edutrack-ms-commons:1.0.0` instalado en el repositorio Maven local

### Instalar commons

```bash
# Desde la raíz del monorepo
cd ../commons
./mvnw install -DskipTests
```

---

## Configuración local

Variables de entorno con defaults para desarrollo (definidas en `application.properties`):

| Variable | Default | Descripción |
|---|---|---|
| `DB_HOST` | `localhost` | Host PostgreSQL |
| `DB_PORT` | `5432` | Puerto PostgreSQL |
| `DB_NAME` | `edutrack` | Base de datos |
| `DB_USER` | `auth_user` | Usuario del schema `auth` |
| `DB_PASSWORD` | `auth_pass` | Contraseña |
| `JWT_PRIVATE_KEY_LOCATION` | `file:.certs/privateKey.pem` | Clave privada RS256 |
| `JWT_PUBLIC_KEY_LOCATION` | `file:.certs/publicKey.pem` | Clave pública RS256 |

### Generar llaves RS256 (primera vez)

```bash
mkdir .certs
openssl genrsa -out .certs/privateKey.pem 2048
openssl rsa -in .certs/privateKey.pem -pubout -out .certs/publicKey.pem
```

### Base de datos

```sql
CREATE DATABASE edutrack;
CREATE USER auth_user WITH PASSWORD 'auth_pass';
GRANT ALL PRIVILEGES ON DATABASE edutrack TO auth_user;
```

Flyway crea el schema `auth` y todas las tablas automáticamente al arrancar.

---

## Levantar el servicio

```bash
./mvnw quarkus:dev
```

El servicio queda disponible en `http://localhost:8080/auth`.

Swagger UI: `http://localhost:8080/auth/q/swagger-ui`  
Health check: `http://localhost:8080/auth/q/health`

---

## Endpoints

Base URL: `/auth`

### Autenticación (públicos — sin JWT)

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/login` | Login con email + password → AccessToken + RefreshToken |
| `POST` | `/refresh` | Renueva el par de tokens con un RefreshToken válido |
| `POST` | `/logout` | Revoca todas las sesiones del usuario autenticado |
| `GET` | `/.well-known/jwks.json` | JWKS público para validación externa del JWT |

### Usuarios

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `GET` | `/users` | `auth.users:READ` | Listar usuarios |
| `POST` | `/users` | `auth.users:WRITE` | Crear usuario |
| `GET` | `/users/{id}` | `auth.users:READ` | Obtener usuario |
| `PUT` | `/users/{id}` | `auth.users:WRITE` | Actualizar usuario |
| `DELETE` | `/users/{id}` | `auth.users:WRITE` | Deshabilitar usuario |
| `DELETE` | `/users/{id}/sessions` | `auth.users:WRITE` | Revocar sesiones de un usuario |

### Roles y permisos

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `GET` | `/roles` | `auth.roles:READ` | Listar roles |
| `POST` | `/roles` | `auth.roles:WRITE` | Crear rol |
| `GET` | `/roles/{id}` | `auth.roles:READ` | Obtener rol |
| `PUT` | `/roles/{id}` | `auth.roles:WRITE` | Actualizar rol |
| `DELETE` | `/roles/{id}` | `auth.roles:WRITE` | Eliminar rol |
| `GET` | `/user-roles` | `auth.users:READ` | Listar asignaciones usuario-rol |
| `POST` | `/user-roles` | `auth.users:WRITE` | Asignar rol a usuario |
| `DELETE` | `/user-roles` | `auth.users:WRITE` | Revocar rol de usuario |
| `GET` | `/role-permissions` | `auth.roles:READ` | Listar permisos de roles |
| `POST` | `/role-permissions` | `auth.roles:WRITE` | Asignar permiso a rol |
| `DELETE` | `/role-permissions/{id}` | `auth.roles:WRITE` | Revocar permiso |

### Verificación de acceso (uso inter-servicio)

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/access?resourceKey=&permission=` | `"1"` si tiene permiso, `"0"` si no. Con `Accept: application/json` retorna objeto con flags efectivos. |
| `GET` | `/meta/resources` | Catálogo de resource keys que este servicio protege |

El endpoint `/access` es el que los demás MS consultan (vía `commons`) para verificar permisos sin duplicar lógica.

---

## Modelo de permisos Unix-style

Los permisos se almacenan como flags numéricos por par `(role_uuid, resource_key)`:

| Flag | Valor | Significado |
|---|---|---|
| `READ` | 4 | Lectura |
| `WRITE` | 2 | Escritura |
| `EXECUTE` | 1 | Ejecución |

Se combinan con OR de bits (ej: `READ+WRITE = 6`). El comodín `resource_key = "*"` aplica a todos los recursos (usado por SUPERUSER).

---

## Esquema de base de datos

Schema: `auth`

```
auth.users
├── id              UUID  PK
├── email           VARCHAR  UNIQUE NOT NULL
├── password_hash   VARCHAR  NOT NULL
├── display_name    VARCHAR
├── enabled         BOOLEAN  DEFAULT true
├── created_at      TIMESTAMPTZ
├── updated_at      TIMESTAMPTZ
├── creator_user    UUID
└── updater_user    UUID

auth.roles
├── id           UUID  PK
├── name         VARCHAR  UNIQUE NOT NULL
├── description  VARCHAR
└── (auditoría)

auth.user_roles
├── user_id      UUID  FK → auth.users  ┐ PK compuesta
├── role_id      UUID  FK → auth.roles  ┘
└── assigned_at  TIMESTAMPTZ

auth.role_permissions
├── id            UUID  PK
├── role_id       UUID  FK → auth.roles
├── resource_key  VARCHAR(150)
├── flags         SMALLINT  (0–7)
└── UNIQUE (role_id, resource_key)

auth.refresh_tokens
├── id          UUID  PK
├── user_id     UUID  FK → auth.users
├── token_hash  VARCHAR  UNIQUE
├── expires_at  TIMESTAMPTZ
├── revoked     BOOLEAN
├── revoked_at  TIMESTAMPTZ  nullable
└── created_at  TIMESTAMPTZ
```

---

## Estructura del proyecto

```
src/main/java/.../auth/
├── model/entity/        ← User, Role, UserRole, RolePermission, RefreshToken
│                          (herencia DRY: AuditableEntity → CreatableEntity → PanacheEntityBase)
├── model/dto/           ← UserRequest/Response, AuthRequest/Response, AccessResponse, AuthViews, AuthValidations
├── resource/            ← AuthResource, UserResource, RoleResource, AccessResource, JwksResource
├── service/             ← AuthService, UserService, TokenService, PermissionService, PasswordService
├── service/bootstrap/   ← DataInitializer (seed de admin y roles iniciales)
├── repository/          ← UserRepository, RoleRepository, RolePermissionRepository, RefreshTokenRepository
└── security/            ← AuthResourceId (catálogo de resource keys: auth.users, auth.roles, …)
```

---

## Pruebas

```bash
# Unitarios
./mvnw test

# Integración
./mvnw verify

# Un solo test
./mvnw test -Dtest=NombreDelTest
```

---

## Build y empaquetado

```bash
./mvnw clean package -DskipTests
java -jar target/quarkus-app/quarkus-run.jar
```

La imagen Docker JVM está en `src/main/docker/Dockerfile.jvm`.
