package cl.duocuc.edutrack.ms.auth.service.boostrap;

/**
 * Contrato de un poblador de datos de arranque.
 *
 * <p>Cada implementación es un bean CDI descubierto automáticamente por
 * {@link DataInitializer} (vía {@code Instance<Seeder>}) y ejecutado una sola
 * vez en el {@code StartupEvent}. Añadir un seed nuevo es declarar un bean que
 * implemente esta interfaz — no se toca el inicializador.</p>
 *
 * <p><b>Las implementaciones deben ser idempotentes:</b> {@link #seed()} corre
 * en cada arranque, así que debe verificar la existencia del dato antes de
 * insertarlo y no duplicar filas.</p>
 */
public interface Seeder {

    /** Inserta el dato semilla si aún no existe. Idempotente. */
    void seed();
}
