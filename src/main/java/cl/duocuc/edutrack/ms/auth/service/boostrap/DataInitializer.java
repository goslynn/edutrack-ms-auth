package cl.duocuc.edutrack.ms.auth.service.boostrap;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/**
 * Dispara todos los {@link Seeder} del servicio al arranque.
 *
 * <p>No conoce las implementaciones concretas: inyecta el {@code Instance<Seeder>}
 * de CDI y las recorre. Añadir un seed nuevo no requiere tocar esta clase.</p>
 */
@ApplicationScoped
public class DataInitializer {

    @Inject
    @Any
    Instance<Seeder> seeders;

    void onStart(@Observes StartupEvent event) {
        seeders.forEach(Seeder::seed);
    }
}
