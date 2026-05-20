package cl.duocuc.edutrack.ms.infrastructure.jackson;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.inject.Singleton;

/**
 * Hace de {@link Views.Base} la vista por defecto de Jackson, evitando declarar
 * {@code @JsonView(Views.Base.class)} explícitamente en cada endpoint cuya
 * respuesta es la vista base.
 *
 * <p>La vista solo aplica como <em>default</em>: cualquier
 * {@code @JsonView(...)} sobre un método de recurso (o un parámetro de body)
 * sigue ganando — RESTEasy Reactive Jackson aplica el writer/reader
 * {@code .withView(...)} por request, lo que sobreescribe la vista por defecto
 * de la {@code SerializationConfig} / {@code DeserializationConfig}.</p>
 *
 * <p>Adicionalmente desactiva {@link MapperFeature#DEFAULT_VIEW_INCLUSION}: con
 * una vista activa, solo se serializan/deserializan las propiedades anotadas
 * con una vista compatible. Como todos los componentes de los DTOs declaran
 * {@code @JsonView}, el efecto es estricto y predecible.</p>
 */
@Singleton
public class JacksonCustomConfig implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper mapper) {
        mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        mapper.setConfig(mapper.getSerializationConfig().withView(Views.Base.class));
        mapper.setConfig(mapper.getDeserializationConfig().withView(Views.Base.class));
    }
}
