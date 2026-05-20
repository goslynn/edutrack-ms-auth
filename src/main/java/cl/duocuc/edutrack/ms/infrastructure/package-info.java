/**
 * <h2>Código compartido entre microservicios de EduTrack</h2>
 *
 * <p>Todo lo que vive bajo {@code cl.duocuc.edutrack.ms.infrastructure} es
 * transversal — <b>no</b> contiene reglas de negocio ni nombres específicos de
 * Auth Service. En un futuro cercano este paquete se extraerá como un artefacto
 * de librería interna (p. ej. {@code edutrack-commons}) consumido como
 * dependencia por cada microservicio del monorepo. Hasta entonces se duplica
 * intencionalmente en cada MS, manteniendo la misma forma y nombres.</p>
 *
 * <p><b>Reglas que ordenan este paquete:</b></p>
 * <ul>
 *   <li><b>Sin acoplamiento al dominio de un MS.</b> Ninguna clase aquí debe
 *       importar paquetes {@code ms.<servicio>.*}. Cuando se necesita un punto
 *       de extensión específico (p. ej. evaluación de permisos del MS host),
 *       se expone como contrato CDI — ver {@link cl.duocuc.edutrack.ms.infrastructure.security.PermissionEvaluator}.</li>
 *   <li><b>Sin referencias a nombres específicos.</b> Los identificadores
 *       (UUIDs de recursos, vistas, grupos de validación) propios de un MS
 *       viven en el paquete del MS. Lo común — el wildcard
 *       {@link cl.duocuc.edutrack.ms.infrastructure.security.ResourceIds#ALL},
 *       {@code Views.Base/...}, {@code Validations.OnCreate}, etc. — vive aquí.</li>
 *   <li><b>Subpaquetes por responsabilidad técnica:</b>
 *     <ul>
 *       <li>{@link cl.duocuc.edutrack.ms.infrastructure.security} —
 *           autorización Unix-style: anotación {@code @RequirePermission},
 *           {@link cl.duocuc.edutrack.ms.infrastructure.security.Permission Permission} (bits),
 *           {@link cl.duocuc.edutrack.ms.infrastructure.security.ResourceIds} (wildcard),
 *           {@link cl.duocuc.edutrack.ms.infrastructure.security.PermissionEvaluator} (contrato).</li>
 *       <li>{@link cl.duocuc.edutrack.ms.infrastructure.context} — intérprete
 *           único de cabeceras internas propagadas por el API Gateway
 *           ({@code RequestContext}, {@code RequestHeaders}).</li>
 *       <li>{@link cl.duocuc.edutrack.ms.infrastructure.exception} — handler
 *           global + envelope JSON único y jerarquía {@code DomainException}.</li>
 *       <li>{@link cl.duocuc.edutrack.ms.infrastructure.jackson} — vistas
 *           {@code @JsonView} estándar + configuración del {@code ObjectMapper}.</li>
 *       <li>{@link cl.duocuc.edutrack.ms.infrastructure.validation} — grupos
 *           de Bean Validation transversales ({@code OnCreate}, secuencia
 *           {@code Create}).</li>
 *     </ul>
 *   </li>
 * </ul>
 */
package cl.duocuc.edutrack.ms.infrastructure;
