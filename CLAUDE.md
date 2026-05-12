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

Todas las entidades extienden `PanacheEntityBase` (Active Record), PKs son `UUID` con `@GeneratedValue(strategy = GenerationType.UUID)`, y todas las asociaciones usan `FetchType.LAZY`. Los timestamps (`createdAt`, `updatedAt`) se gestionan via `@PrePersist` / `@PreUpdate` con `Instant`.

| Entidad | Tabla | Notas clave |
|---|---|---|
| `User` | `auth.users` | `passwordHash` (nunca plaintext); `enabled` flag; relaciones `userRoles` y `refreshTokens` con `cascade=ALL, orphanRemoval=true` |
| `Role` | `auth.roles` | `name` único; roles precargados: `SUPERUSER`, `ADMIN`, `DOCENTE` (V2 seed) |
| `UserRole` | `auth.user_roles` | PK compuesta `@EmbeddedId UserRoleId(userId, roleId)`; constructor conveniente `UserRole(User, Role)` |
| `UserRoleId` | — | `@Embeddable Serializable`; implementa `equals`/`hashCode` sobre los dos UUIDs |
| `RolePermission` | `auth.role_permissions` | `resourceUuid` sin FK (opaco); `flags short` 0–7; constraint única `(role_id, resource_uuid)` |
| `RefreshToken` | `auth.refresh_tokens` | Solo `tokenHash` almacenado (nunca el token raw); `revoked` + `revokedAt` para revocación explícita |

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
- Los DTOs van en `cl.duocuc.edutrack.ms.auth.dto`
- Los repositorios Panache (`PanacheRepositoryBase`) van en `cl.duocuc.edutrack.ms.auth.model.repository`; las entidades mantienen Active Record para CRUD básico y los repositorios exponen queries de dominio
- Campos de entidad `public` (convención Panache Active Record) — no generar getters/setters
- El hash de contraseñas debe hacerse con Argon2 o bcrypt; **nunca** almacenar ni loguear contraseñas en claro
- Los refresh tokens se almacenan hasheados (`tokenHash`); el token raw solo se entrega al cliente en el momento de emisión
