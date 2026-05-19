# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Responsabilidad del servicio

Auth Service es el único emisor de JWT (RS256) en EduTrack. Gestiona usuarios, roles dinámicos y un modelo de permisos Unix-style (`r=4, w=2, x=1`) almacenados como `flags SMALLINT` por par `(role_uuid, resource_uuid)`. El `resource_uuid` es opaco aquí: cada otro MS registra los suyos y Auth solo almacena el flag numérico.

El API Gateway valida el JWT y propaga `sub` (UUID del usuario) y `roles[]` como headers internos — Auth Service **no** es consultado en cada request, solo en login/refresh/revocación.

## Stack

- Quarkus 3.34.6 + Java 21
- `quarkus-hibernate-orm-panache`, `quarkus-jdbc-postgresql`, `quarkus-flyway`, `quarkus-rest`, `quarkus-jackson`
- PostgreSQL schema `auth` en instancia RDS compartida (Shared DB, schemas separados)
- JWT RS256 (por implementar: `quarkus-smallrye-jwt`)

## Comandos

```bash
./mvnw quarkus:dev          # dev mode con hot reload
./mvnw test                 # tests unitarios
./mvnw verify               # tests de integración (requiere DB)
./mvnw test -Dtest=NombreDelTest
./mvnw package              # build JAR
```

## Modelo de datos (`cl.duocuc.edutrack.ms.auth.model`)

Las PKs son `UUID` con `@GeneratedValue(strategy = GenerationType.UUID)` y todas las asociaciones usan `FetchType.LAZY`. Los timestamps (`createdAt`, `updatedAt`) se gestionan via `@PrePersist` / `@PreUpdate` con `Instant`, pero **no se duplican en cada entidad**: se heredan de superclases `@MappedSuperclass` (ver "Herencia de entidades" más abajo).

| Entidad | Tabla | Superclase | Notas clave |
|---|---|---|---|
| `User` | `auth.users` | `AuditableEntity` | `passwordHash` (nunca plaintext); `enabled` flag; relaciones `userRoles` y `refreshTokens` con `cascade=ALL, orphanRemoval=true` |
| `Role` | `auth.roles` | `AuditableEntity` | `name` único; roles precargados: `SUPERUSER`, `ADMIN`, `DOCENTE` (V2 seed) |
| `UserRole` | `auth.user_roles` | `PanacheEntityBase` | PK compuesta `@EmbeddedId UserRoleId(userId, roleId)`; usa `assigned_at` (semántica distinta a `createdAt`), por eso no hereda de Auditable |
| `UserRoleId` | — | — | `@Embeddable Serializable`; implementa `equals`/`hashCode` sobre los dos UUIDs |
| `RolePermission` | `auth.role_permissions` | `AuditableEntity` | `resourceUuid` sin FK (opaco); `flags short` 0–7; constraint única `(role_id, resource_uuid)` |
| `RefreshToken` | `auth.refresh_tokens` | `CreatableEntity` | Inmutable salvo revocación: solo `createdAt`, sin `updatedAt`; `revoked` + `revokedAt` para revocación explícita |

### Herencia de entidades (DRY)

Para evitar repetir `id`, `createdAt`, `updatedAt` y sus callbacks en cada entidad, el paquete `entity` define dos `@MappedSuperclass` apilados:

```
PanacheEntityBase
   └── CreatableEntity        // id UUID + createdAt + @PrePersist
          └── AuditableEntity // agrega updatedAt + @PreUpdate
```

- `CreatableEntity` se usa en entidades **inmutables tras su creación** (p. ej. `RefreshToken` — solo se marca `revoked`/`revokedAt`, no se "actualiza" en sentido auditable).
- `AuditableEntity` se usa en entidades **mutables** que requieren registrar la última modificación (`User`, `Role`, `RolePermission`).
- Una entidad con semántica de auditoría propia (como `UserRole`, que usa `assigned_at`) **no** debe heredar de estas superclases — extiende `PanacheEntityBase` directamente.

Ejemplo de definición y uso:

```java
@MappedSuperclass
public abstract class CreatableEntity extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    public UUID id;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }
}

@MappedSuperclass
public abstract class AuditableEntity extends CreatableEntity {
    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @PrePersist
    void onCreateAuditable() { updatedAt = Instant.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}

@Entity
@Table(name = "users", schema = "auth")
public class User extends AuditableEntity {
    // sin id, createdAt, updatedAt, ni callbacks duplicados
    @Column(nullable = false, unique = true)
    public String email;
    // ...
}

@Entity
@Table(name = "refresh_tokens", schema = "auth")
public class RefreshToken extends CreatableEntity {
    // sin updatedAt: el token es inmutable salvo revocación
    @Column(name = "token_hash", nullable = false, unique = true)
    public String tokenHash;
    // ...
}
```

**Regla general:** si una columna aparece en *todas* o la *gran mayoría* de las tablas, súbela a una `@MappedSuperclass`. Nunca dupliques `@PrePersist`/`@PreUpdate` en una entidad si el padre ya los define — las callbacks se heredan y Hibernate ejecuta primero las del padre y luego las del hijo.

## Migraciones Flyway

- Ubicación: `src/main/resources/db/migration/`
- Formato: `V{n}__{descripcion}.sql`
- Flyway corre automáticamente al arrancar (`migrate-at-start=true`), schema target: `auth`
- **Nunca modificar migraciones ya aplicadas** — siempre agregar una nueva versión
- En dev, Hibernate valida (`database.generation=validate`) que el mapeo JPA coincida con el DDL; si hay divergencia el arranque falla

## Variables de entorno

| Variable | Default dev | Descripción |
|---|---|---|
| `DB_HOST` | `localhost` | Host PostgreSQL |
| `DB_NAME` | `edutrack` | Nombre de la base de datos |
| `DB_USER` | `auth_user` | Usuario exclusivo al schema `auth` |
| `DB_PASSWORD` | `auth_pass` | Contraseña |

No usar `.env` ni archivos de secrets versionados. Los defaults en `application.properties` son solo para desarrollo local.

## Convenciones del servicio

- Paquete base: `cl.duocuc.edutrack.ms.auth`
- Los recursos JAX-RS van en `cl.duocuc.edutrack.ms.auth` (mismo nivel que `AuthResource`)
- Los servicios/lógica de negocio van en `cl.duocuc.edutrack.ms.auth.service`
- Los DTOs van en `cl.duocuc.edutrack.ms.auth.model.dto` — **máximo 2 por entidad/recurso** (un `XxxRequest`, un `XxxResponse`); la granularidad por endpoint se controla con `@JsonView` (ver sección "DTOs y `@JsonView`")
- Los repositorios Panache (`PanacheRepositoryBase`) van en `cl.duocuc.edutrack.ms.auth.model.repository`; las entidades mantienen Active Record para CRUD básico y los repositorios exponen queries de dominio
- Campos de entidad `public` (convención Panache Active Record) — no generar getters/setters
- El hash de contraseñas debe hacerse con Argon2 o bcrypt; **nunca** almacenar ni loguear contraseñas en claro
- Los refresh tokens se almacenan hasheados (`tokenHash`); el token raw solo se entrega al cliente en el momento de emisión

## DTOs y `@JsonView`

Los DTOs están en `cl.duocuc.edutrack.ms.auth.model.dto`. Hay exactamente **un Request y un Response por entidad/recurso**; la diferencia entre los campos que viajan en cada endpoint se modela con `@JsonView` sobre los componentes del record.

**Jerarquía de vistas** (`Views.java`):

```
Base                    // campos siempre visibles
Extra                   // campos opt-in para listados/admin
Detailed extends Base   // GET /{id}, respuestas tras POST/PUT
Create   extends Base   // body de POST
Update   extends Base   // body de PUT
Patch    extends Base, Extra   // body de PATCH
List     extends Base, Extra   // GET colecciones
Admin    extends Base, Extra   // vistas con campos sensibles/auditoría
Internal                // serialización service-to-service
Login    extends Base   // body de POST /auth/login
Refresh  extends Base   // body de POST /auth/refresh
```

**DTOs actuales** (8 archivos):

| DTO | Componentes y vistas |
|---|---|
| `UserRequest` | `email,password` → `Create`; `displayName` → `Create/Update/Patch`; `enabled` → `Update/Patch/Admin` |
| `UserResponse` | `id,email,displayName` → `Base`; `enabled` → `Base/Admin`; `createdAt,updatedAt,roleIds` → `Detailed/Admin` |
| `RoleRequest` | `name,description` → `Create/Update/Patch` |
| `RoleResponse` | `id,name` → `Base`; `description` → `Base/Extra`; timestamps → `Detailed/Admin` |
| `PermissionRequest` | `flags` → `Create/Update/Patch` |
| `PermissionResponse` | base + `flagsLabel` (`Base/Extra`); helper estático `toLabel(short)` |
| `AuthRequest` | `email,password` → `Login`; `refreshToken` → `Refresh` |
| `AuthResponse` | `accessToken,refreshToken,tokenType,expiresIn` → `Base` |

**Convención de anotación de endpoints:**

- `GET` de colección → `@JsonView(Views.List.class)` sobre el método
- `GET /{id}` y respuestas tras `POST`/`PUT` → `@JsonView(Views.Detailed.class)`
- Body de `POST` → parámetro anotado con `@JsonView(Views.Create.class)`
- Body de `PUT` → parámetro anotado con `@JsonView(Views.Update.class)`
- `AuthResource.login` usa `Views.Login` (req) / `Views.Base` (resp); `refresh` usa `Views.Refresh` (req) / `Views.Base` (resp)

## Validaciones de datos del request (regla dura)

**TODA** validación de datos del request (body) pasa por la API de Bean Validation de Jakarta. **Prohibido** validar datos con sentencias `if` dentro de la implementación de un endpoint o en la capa de servicio (p. ej. `if (req.email() == null || req.email().isBlank()) throw 400`, o `if (flags < 0 || flags > 7) throw 422`). Las restricciones (`@NotBlank`, `@Email`, `@Size`, `@Min`, `@Max`, …) se declaran sobre los componentes del record y se disparan con `@Valid` en el parámetro del recurso; `ConstraintViolationException` ⇒ `400` lo mapea automáticamente la extensión `quarkus-hibernate-validator`.

**Validación condicional por endpoint con un único record (validation groups):** como hay un solo `XxxRequest` compartido entre `Create`/`Update`/`Login`/etc., la presencia obligatoria de un campo varía por endpoint. Se modela con **grupos de Bean Validation** definidos en `model/dto/Validations.java` (paralelo a las vistas `@JsonView`):

```
Validations.OnCreate / OnLogin / OnRefresh     // marcadores de presencia, por endpoint
Validations.Create  = @GroupSequence({Default, OnCreate})
Validations.Login   = @GroupSequence({Default, OnLogin})
Validations.Refresh = @GroupSequence({Default, OnRefresh})
```

Convención de anotación:

- Restricciones de **formato** (`@Email`, `@Size`, `@Min`, `@Max`) → grupo `Default` (sin `groups`): siempre se evalúan y son null-safe (pasan cuando el campo no viaja en esa vista).
- Restricciones de **presencia** (`@NotBlank`, `@NotNull`) → `groups = Validations.OnXxx.class`: solo se evalúan en su endpoint.
- En el recurso, el body de un endpoint con presencia obligatoria se anota `@Valid @ConvertGroup(from = Default.class, to = Validations.Xxx.class)`. El `@GroupSequence` ejecuta primero `Default` (formato) y luego el grupo de presencia.
- Los endpoints sin presencia obligatoria (`PUT`/`PATCH`) usan `@Valid` a secas (solo `Default`), por lo que los `@NotBlank` de `OnCreate` no se disparan y los campos omitidos son válidos.

`PermissionRequest.flags` usa `@Min(0) @Max(7)` en `Default`; el `PUT` lo valida con `@Valid` simple (sin grupo).

**Frontera de alcance:** la regla cubre *validación de datos*. Los guards de **autenticación/identidad** no son validación de datos: en `AuthResource.logout`, la ausencia de identidad propagada es `401` (`RequestContext.headers().requireUserId()`) — sin identidad no hay a quién revocar; el *formato* malformado del UUID sí es dato y lo resuelve el intérprete de cabeceras (`RequestContext`) según su modo (`EAGER` ⇒ `400`). Las reglas de **negocio** (unicidad de email/nombre, "último SUPERUSER", rol aún asignado) tampoco son validación de datos y siguen como checks de dominio en el servicio devolviendo `409`.

## Cabeceras internas: `RequestContext` (intérprete único)

El API Gateway propaga la identidad ya autenticada como cabeceras internas `X-User-Id` (UUID del usuario) y `X-User-Roles` (UUIDs de rol separados por coma). **Ningún endpoint, filtro ni servicio lee esas cabeceras a mano** (`@HeaderParam("X-...")` / `getHeaderString("X-...")` están prohibidos): hay un único intérprete, `cl.duocuc.edutrack.ms.infrastructure.context.RequestContext`.

Componentes del paquete `infrastructure.context`:

| Tipo | Rol |
|---|---|
| `InternalHeader` (enum) | Única fuente de verdad de los nombres de cabecera (`X-User-Id`, `X-User-Roles`). Abstrae el string del cable. |
| `RequestHeaders` (record) | Value object inmutable ya interpretado: `Optional<UUID> userId`, `List<UUID> roleIds` (nunca `null`). Helpers `hasIdentity()` y `requireUserId()` (⇒ `401` si ausente). |
| `RequestContext` | Bean `@RequestScoped` proxyable. Interpreta y valida **una sola vez por request** en `@PostConstruct` y expone el record vía `headers()`. Es lo que se inyecta. |
| `HeaderValidationMode` (enum) | `EAGER` / `WARN`. |

Uso:

```java
@Inject RequestContext requestContext;
...
UUID uid       = requestContext.headers().requireUserId(); // 401 si no hay identidad
List<UUID> rol = requestContext.headers().roleIds();        // [] si no viajan roles
```

**Por qué un holder `@RequestScoped` y no `@Produces @RequestScoped RequestHeaders`:** ArC prohíbe que un productor de bean *normal-scoped* devuelva un `record` (los records son `final`, no se pueden proxiar). El holder `RequestContext` sí es proxyable, conserva la semántica "se computa 1 vez por request" y es inyectable con resolución por-request incluso en singletons como `RequirePermissionFilter`. El record sigue siendo el value object inmutable que circula.

**Reglas de interpretación:**

1. Header presente y bien formado ⇒ valor tipado.
2. Header **ausente** ⇒ valor vacío (`Optional.empty()` / lista vacía). **No** es fallo de validación; ausencia de identidad la decide el consumidor (p. ej. `requireUserId()` ⇒ `401`).
3. Header **presente pero malformado** ⇒ según `edutrack.headers.validation.mode`:
   - `EAGER` (default): aborta el request con `400 Bad Request`.
   - `WARN`: loguea `WARN` y trata el valor como ausente; el request continúa.

`RequirePermissionFilter` consume `RequestContext` (no parsea cabeceras): por eso un `X-User-Roles` malformado pasó de `403` a `400` en modo `EAGER` — la validación del *dato* cabecera ahora es responsabilidad del intérprete, antes que la autorización.

| Propiedad | Default | Descripción |
|---|---|---|
| `edutrack.headers.validation.mode` | `EAGER` | `EAGER` ⇒ cabecera malformada = `400`; `WARN` ⇒ se loguea y se trata como ausente |
