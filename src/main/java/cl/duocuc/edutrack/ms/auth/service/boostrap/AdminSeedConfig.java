package cl.duocuc.edutrack.ms.auth.service.boostrap;

import io.smallrye.config.ConfigMapping;

/**
 * Credenciales del administrador inicial sembrado al arranque.
 *
 * <p>Interfaz {@code @ConfigMapping} <b>sin defaults</b>: las tres propiedades
 * son obligatorias. Si el entorno no las inyecta (vía {@code ADMIN_EMAIL},
 * {@code ADMIN_PASSWORD}, {@code ADMIN_NAME} en {@code application.properties}),
 * SmallRye Config aborta el arranque con un error de configuración faltante. Es
 * deliberado: un SUPERUSER con credenciales por defecto incrustadas en el
 * binario sería un agujero de seguridad — preferimos fallar a sembrar un admin
 * predecible.</p>
 */
@ConfigMapping(prefix = "auth.seed.admin")
public interface AdminSeedConfig {

    String email();

    String password();

    String displayName();
}
