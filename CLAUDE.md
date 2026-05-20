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

## Código compartido (`cl.duocuc.edutrack.ms.infrastructure`)

El paquete `infrastructure` aloja **código transversal** a todos los microservicios — sin reglas de negocio ni nombres de Auth. En un futuro cercano se extraerá como un artefacto de librería interna (`edutrack-commons`) consumido por dependencia; hasta entonces se duplica en cada MS con la misma forma y nombres. Ver `infrastructure/package-info.java` para el contrato detallado de cada subpaquete.

**Resumen de subpaquetes:**

| Subpaquete | Contenido |
|---|---|
| `infrastructure.security` | `@RequirePermission(resource = <UUID-string>, value = …)`, enum `Permission`, `ResourceIds` (wildcard `ALL`), contrato `PermissionEvaluator`, `RequirePermissionFilter`. **No** conoce los recursos de Auth — esos viven en `auth.security.AuthResourceId`. |
| `infrastructure.context` | Intérprete único de cabeceras del Gateway: `InternalHeader`, `RequestHeaders`, `RequestContext`, `HeaderValidationMode`. |
| `infrastructure.exception` | `GlobalExceptionMappers`, envelope `ErrorResponse`, jerarquía `DomainException` + sugar (`ConflictException`/`NotFoundException`/`ForbiddenException`). |
| `infrastructure.jackson` | Interfaz `Views` (vistas estándar) + `JacksonCustomConfig` que la fija como vista por defecto. Las vistas propias de auth (`Login`, `Refresh`) viven en `auth.model.dto.AuthViews`. |
| `infrastructure.validation` | Interfaz `Validations` con `OnCreate` + secuencia `Create`. Los grupos propios de auth (`OnLogin`, `OnRefresh`, `Login`, `Refresh`) viven en `auth.model.dto.AuthValidations`. |

**Regla dura:** ningún archivo bajo `infrastructure.*` puede importar paquetes `ms.auth.*` (ni los de otros MS). Los puntos de extensión específicos del dominio se exponen como contratos CDI (`PermissionEvaluator` es el primer ejemplo) y cada MS aporta su implementación.

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

**Jerarquía de vistas** — el núcleo transversal vive en `cl.duocuc.edutrack.ms.infrastructure.jackson.Views` (compartido entre todos los MS) y este servicio agrega las vistas propias del dominio en `cl.duocuc.edutrack.ms.auth.model.dto.AuthViews`:

```
// infrastructure.jackson.Views (compartido)
Base                    // campos siempre visibles
Extra                   // campos opt-in para listados/admin
Detailed extends Base   // GET /{id}, respuestas tras POST/PUT
Create   extends Base   // body de POST
Update   extends Base   // body de PUT
Patch    extends Base, Extra   // body de PATCH
List     extends Base, Extra   // GET colecciones
Admin    extends Base, Extra   // vistas con campos sensibles/auditoría
Internal                // serialización service-to-service

// auth.model.dto.AuthViews (específico del MS)
Login    extends Views.Base   // body de POST /auth/login
Refresh  extends Views.Base   // body de POST /auth/refresh
```

**DTOs actuales** (9 archivos):

| DTO | Componentes y vistas |
|---|---|
| `UserRequest` | `email,password` → `Create`; `displayName` → `Create/Update/Patch`; `enabled` → `Update/Patch/Admin` |
| `UserResponse` | `id,email,displayName` → `Base`; `enabled` → `Base/Admin`; `createdAt,updatedAt,roleIds` → `Detailed/Admin` |
| `RoleRequest` | `name,description` → `Create/Update/Patch` |
| `RoleResponse` | `id,name` → `Base`; `description` → `Base/Extra`; timestamps → `Detailed/Admin` |
| `PermissionRequest` | `flags` → `Create/Update/Patch` |
| `PermissionResponse` | base + `flagsLabel` (`Base/Extra`); helper estático `toLabel(short)` |
| `AuthRequest` | `email,password` → `AuthViews.Login`; `refreshToken` → `AuthViews.Refresh` |
| `AuthResponse` | `accessToken,refreshToken,tokenType,expiresIn` → `Base` |
| `AccessResponse` | `allowed,resourceUuid,required,effectiveFlags,effectiveLabel` → `Base` (solo response; entrada por query params) |

**Convención de anotación de endpoints:**

- `GET` de colección → `@JsonView(Views.List.class)` sobre el método
- `GET /{id}` y respuestas tras `POST`/`PUT` → `@JsonView(Views.Detailed.class)`
- Body de `POST` → parámetro anotado con `@JsonView(Views.Create.class)`
- Body de `PUT` → parámetro anotado con `@JsonView(Views.Update.class)`
- `AuthResource.login` usa `AuthViews.Login` en el req; el resp es `Views.Base` por **default global** (ver siguiente sub-sección), por lo que no se anota.

### `Views.Base` como vista por defecto

La vista por defecto de Jackson se configura globalmente en `cl.duocuc.edutrack.ms.infrastructure.jackson.JacksonCustomConfig` (`ObjectMapperCustomizer` `@Singleton`):

```java
mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
mapper.setConfig(mapper.getSerializationConfig().withView(Views.Base.class));
mapper.setConfig(mapper.getDeserializationConfig().withView(Views.Base.class));
```

`DEFAULT_VIEW_INCLUSION=false` ⇒ con una vista activa solo se serializan/deserializan propiedades anotadas con una vista compatible. Como todos los componentes de los DTOs declaran `@JsonView`, el efecto es estricto y predecible.

Consecuencia práctica: **no anotar `@JsonView(Views.Base.class)`** en endpoints/parámetros — es redundante. Cualquier otro `@JsonView(...)` sobre el método/parámetro se aplica per-request vía `writer/reader.withView(...)` y sobreescribe la vista default.

### Contrato `*Response.fromEntity(...)` (auto-construcción)

Cada `XxxResponse` expone un factory estático que **sabe construirse desde su fuente**. La instanciación con `new XxxResponse(...)` queda confinada al propio DTO; los call sites pasan siempre por el factory.

- **Entity-backed:** `public static XxxResponse fromEntity(XxxEntity entity)`. Cuando la entidad no basta porque un colaborador externo aporta datos que no son columna, el factory acepta ese colaborador como parámetro: `UserResponse.fromEntity(User user, List<UUID> roleIds)` — los `roleIds` se resuelven en el servicio (`UserRoleRepository`) y se pasan al DTO.
- **Computado, no entity-backed:** `public static XxxResponse of(...)`. Lo usan los DTOs que no respaldan una sola fila: `AuthResponse.of(accessToken, refreshToken, expiresInSeconds)` (la constante de protocolo `tokenType="Bearer"` vive en el factory), `AccessResponse.of(allowed, resourceUuid, required, effectiveFlags)` (la etiqueta `rwx` la deriva el DTO). Si para una misma entidad se necesita además una construcción computada (p. ej. flags efectivos OR-eados sobre varios roles), conviven `fromEntity(...)` y `of(...)` en el mismo DTO — caso `PermissionResponse`.

Los servicios mantienen `toResponse(Entity)` cuando aportan I/O (lookup de colaboradores) — ese método se vuelve un delegado al factory:

```java
public UserResponse toResponse(User user) {
    return UserResponse.fromEntity(user, userRoleRepository.findRoleIdsByUserId(user.id));
}
```

Cuando no hay I/O, el servicio puede omitirse del flujo y el recurso llamar al factory directo (p. ej. `RoleResponse.fromEntity(role)`).

## Validaciones de datos del request (regla dura)

**TODA** validación de datos del request (body) pasa por la API de Bean Validation de Jakarta. **Prohibido** validar datos con sentencias `if` dentro de la implementación de un endpoint o en la capa de servicio (p. ej. `if (req.email() == null || req.email().isBlank()) throw 400`, o `if (flags < 0 || flags > 7) throw 422`). Las restricciones (`@NotBlank`, `@Email`, `@Size`, `@Min`, `@Max`, …) se declaran sobre los componentes del record y se disparan con `@Valid` en el parámetro del recurso; `ConstraintViolationException` ⇒ `400` lo mapea automáticamente la extensión `quarkus-hibernate-validator`.

**Validación condicional por endpoint con un único record (validation groups):** como hay un solo `XxxRequest` compartido entre `Create`/`Update`/`Login`/etc., la presencia obligatoria de un campo varía por endpoint. Se modela con **grupos de Bean Validation** (paralelo a las vistas `@JsonView`). El núcleo transversal vive en `cl.duocuc.edutrack.ms.infrastructure.validation.Validations` (compartido); los grupos propios del MS viven en `cl.duocuc.edutrack.ms.auth.model.dto.AuthValidations`:

```
// infrastructure.validation.Validations (compartido)
Validations.OnCreate                            // marcador de presencia para POST de creación
Validations.Create   = @GroupSequence({Default, OnCreate})

// auth.model.dto.AuthValidations (específico del MS)
AuthValidations.OnLogin / OnRefresh             // marcadores propios de auth
AuthValidations.Login    = @GroupSequence({Default, OnLogin})
AuthValidations.Refresh  = @GroupSequence({Default, OnRefresh})
```

Convención de anotación:

- Restricciones de **formato** (`@Email`, `@Size`, `@Min`, `@Max`) → grupo `Default` (sin `groups`): siempre se evalúan y son null-safe (pasan cuando el campo no viaja en esa vista).
- Restricciones de **presencia** (`@NotBlank`, `@NotNull`) → `groups = Validations.OnXxx.class`: solo se evalúan en su endpoint.
- En el recurso, el body de un endpoint con presencia obligatoria se anota `@Valid @ConvertGroup(from = Default.class, to = Validations.Xxx.class)`. El `@GroupSequence` ejecuta primero `Default` (formato) y luego el grupo de presencia.
- Los endpoints sin presencia obligatoria (`PUT`/`PATCH`) usan `@Valid` a secas (solo `Default`), por lo que los `@NotBlank` de `OnCreate` no se disparan y los campos omitidos son válidos.

`PermissionRequest.flags` usa `@Min(0) @Max(7)` en `Default`; el `PUT` lo valida con `@Valid` simple (sin grupo).

**Frontera de alcance:** la regla cubre *validación de datos*. Los guards de **autenticación/identidad** no son validación de datos: en `AuthResource.logout`, la ausencia de identidad propagada es `401` (`RequestContext.headers().requireUserId()`) — sin identidad no hay a quién revocar; el *formato* malformado del UUID sí es dato y lo resuelve el intérprete de cabeceras (`RequestContext`) según su modo (`EAGER` ⇒ `400`). Las reglas de **negocio** (unicidad de email/nombre, "último SUPERUSER", rol aún asignado) tampoco son validación de datos y siguen como checks de dominio en el servicio devolviendo `409`.

## Manejo de errores: `GlobalExceptionMappers` + `DomainException`

Todo error que escape de un recurso sale en el mismo envelope JSON, emitido por `cl.duocuc.edutrack.ms.infrastructure.exception.GlobalExceptionMappers` (un único bean `@ApplicationScoped` con varios `@ServerExceptionMapper` por tipo). El bean resuelve el más específico primero:

| Tipo de excepción | Status | `code` |
|---|---|---|
| `DomainException` (y subclases) | el que carga | el de la excepción |
| `ConstraintViolationException` (Bean Validation) | `400` | `VALIDATION.CONSTRAINT` (+ `metadata.violations[]` con `path`/`message`) |
| `WebApplicationException` (legacy / framework) | el de su Response | `null` |
| `Throwable` (catch-all) | `500` | `INTERNAL.UNEXPECTED` (loguea ERROR; el resto loguea DEBUG) |

### `ErrorResponse` (envelope JSON único)

```json
{
  "timestamp": "2026-05-19T20:11:42.118Z",
  "status": 409,
  "error": "Conflict",
  "code": "AUTH.USER.LAST_SUPERUSER",
  "message": "Cannot disable the last active SUPERUSER",
  "path": "/auth/users/.../disable",
  "metadata": { "userId": "..." }
}
```

Campos opcionales (`code`, `metadata`, `trace`) se omiten cuando son `null`/vacíos (`@JsonInclude(NON_EMPTY)`).

### Stack trace opcional

`edutrack.errors.expose-stacktrace=false` (default). Cuando se activa, el envelope incluye `trace[]` con hasta 25 frames de `StackTraceElement.toString()`. Trade-off: el costo de cómputo es despreciable (la traza ya está en la excepción), pero el body crece y se filtran paquetes/clases internas; mantenerlo apagado en prod, encenderlo en dev/staging.

| Propiedad | Default | Descripción |
|---|---|---|
| `edutrack.errors.expose-stacktrace` | `false` | `true` ⇒ incluye `ErrorResponse.trace` (≤ 25 frames) |

### `DomainException` y sugar (`ConflictException`, `NotFoundException`, `ForbiddenException`)

Las reglas de negocio se lanzan como `DomainException` (o sus sugar 409/404/403). La firma carga:

- **status**: int o `Response.Status`;
- **code**: string estable de dominio (`AUTH.USER.EMAIL_EXISTS`) — el cliente switchea por él, no por el mensaje;
- **message**: humano-legible;
- **metadata**: encadenable vía `.with(k, v)`.

Convención de `code`: `AUTH.<ENTIDAD>.<CONDICION>` en SCREAMING_SNAKE. Es contrato: aunque se reformule el `message`, el `code` no cambia entre versiones.

### Cómo reemplazar excepciones actuales

| Antes (`WebApplicationException` crudo) | Después |
|---|---|
| `throw new WebApplicationException(Response.Status.NOT_FOUND)` (entidad no encontrada) | `throw new NotFoundException("AUTH.USER.NOT_FOUND", "User not found").with("id", id)` |
| `throw new WebApplicationException(Response.Status.CONFLICT)` (unicidad) | `throw new ConflictException("AUTH.USER.EMAIL_EXISTS", "Email already in use").with("email", email)` |
| `throw new WebApplicationException(Response.status(409).entity(Map.of("error", "...")).build())` | `throw new ConflictException("<CODE>", "<message>").with("<key>", <value>)` — la metadata viaja estructurada, no como string libre dentro de un map |
| `throw new WebApplicationException(Response.Status.FORBIDDEN)` (autorización) | `throw new ForbiddenException("AUTH.PERMISSION.DENIED", "Insufficient permissions").with("resource", resourceUuid).with("required", "WRITE")` |

Ejemplos ya migrados en este servicio:

- `UserService.guardLastSuperuser` ⇒ `ConflictException("AUTH.USER.LAST_SUPERUSER", "...").with("userId", id)`.
- `RoleService.delete` (último constraint) ⇒ `ConflictException("AUTH.ROLE.STILL_ASSIGNED", "...").with("roleId", id).with("assignedCount", n)`.

Los demás `throw new WebApplicationException(Response.Status.XXX)` quedan **funcionales** (los toma el mapper de `WebApplicationException`, status correcto, sin `code`), pero su migración progresiva a `DomainException` mejora la trazabilidad: el cliente sabe *qué* falló sin parsear el mensaje, y la metadata estructurada se preserva.

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
| `edutrack.errors.expose-stacktrace` | `false` | `true` ⇒ incluye `ErrorResponse.trace[]` (≤ 25 frames). Apagar en prod. |

## Autorización: algoritmo único y endpoint público de verificación

El algoritmo de decisión Unix-style **vive en un solo lugar**: `PermissionService`, que implementa el contrato compartido `cl.duocuc.edutrack.ms.infrastructure.security.PermissionEvaluator`.

- `effectiveFlags(roleIds, resourceUuid)` → flags efectivos OR-eados, **incluyendo el comodín `ResourceIds.ALL`** (un grant sobre `ALL` — p. ej. SUPERUSER — cubre cualquier recurso presente/futuro). `ResourceIds.ALL` vive en `infrastructure.security` porque el wildcard es transversal a todos los MS.
- `hasPermission(roleIds, resourceUuid, requiredBits)` → `(effectiveFlags & required) == required`. Es el método del contrato `PermissionEvaluator`.

Los UUIDs concretos de los recursos de este servicio viven en `cl.duocuc.edutrack.ms.auth.security.AuthResourceId` (enum con campo `uuid` para uso en código + interfaz anidada `AuthResourceId.Uuid` con los mismos UUIDs como `String` para usarlos como valor de anotación `@RequirePermission(resource = AuthResourceId.Uuid.USERS, ...)`).

Tanto `RequirePermissionFilter` (anotación `@RequirePermission`, decisión interna ⇒ `403`, inyecta `PermissionEvaluator`) como el endpoint público delegan en estos métodos: **no se duplica la lógica de bits/comodín**.

**`GET /auth/access`** — expone ese mismo algoritmo hacia afuera para que otros MS verifiquen el acceso del usuario propagado por el Gateway (toma sus roles de `RequestContext`, no de un `roleId` de path como `/effective`).

- Query params: `resourceUuid` (UUID, `@NotNull` ⇒ ausente = `400` vía Bean Validation; malformado lo coerciona JAX-RS ⇒ RESTEasy Reactive responde `404`), `permission` (`Permission` enum, `@DefaultValue("READ")`; valor inválido ⇒ `404`).
- **Negociación de contenido:** `text/plain` (default, `qs=1.0`, extra ligero) ⇒ cuerpo `"1"` / `"0"`; `application/json` (`qs=0.9`, solo si se pide explícito) ⇒ `AccessResponse` con `allowed`, `resourceUuid`, `required`, `effectiveFlags`, `effectiveLabel`.
- **Público tras el Gateway**, sin `@RequirePermission`. Diseño: sin identidad propagada simplemente no hay grants que sumar ⇒ la respuesta es `"0"` / `allowed=false`, no `403`. Esto evita un *meta-guard* circular (necesitar permiso para preguntar por permisos) y deja al endpoint cumplir su rol de verificador barato consumido por otros MS.
