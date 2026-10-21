package modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Frase transcrita por el Speech-to-Text y confirmada por el usuario. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InterpretarComandoRequest {

    @NotBlank(message = "Dicta o escribe la consulta que deseas realizar.")
    @Size(max = 500, message = "La consulta no puede superar los 500 caracteres.")
    private String texto;
}
