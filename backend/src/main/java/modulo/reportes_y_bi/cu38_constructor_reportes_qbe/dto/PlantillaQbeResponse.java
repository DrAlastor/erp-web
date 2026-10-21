package modulo.reportes_y_bi.cu38_constructor_reportes_qbe.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Plantilla guardada en {@code REPORTES_QBE}, con su definición lista para ejecutar. */
public record PlantillaQbeResponse(
        Long id,
        String nombre,
        String descripcion,
        String modulo,
        String entidad,
        String entidadEtiqueta,
        String autor,
        LocalDateTime fechaCreacion,
        DefinicionQbe definicion,
        List<String> advertencias) {
}
