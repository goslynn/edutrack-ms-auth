package cl.duocuc.edutrack.ms.auth.service.boostrap;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class DataInitializer {

    @Inject
    AdminSeeder adminSeeder;

    void onStart(@Observes StartupEvent event) {
        adminSeeder.seedIfNeeded();
    }
}
