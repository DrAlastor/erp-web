package modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modulo.ventas_y_facturacion.compartido.entity.Venta;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.DosificacionRequest;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.DosificacionResponse;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.entity.SecuenciaFacturacion;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.mapper.VentaMapper;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.repository.SecuenciaFacturacionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/**
 * Dosificaciones del SIN (tabla secuencia_facturacion). Las facturas se numeran con la
 * dosificación activa; solo puede haber una activa a la vez. Una dosificación no se edita
 * ni se borra: sus datos los fija el SIN y respaldan facturas ya emitidas.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DosificacionService {

    private final SecuenciaFacturacionRepository repository;
    private final VentaMapper mapper;

    @Transactional(readOnly = true)
    public List<DosificacionResponse> listar() {
        LocalDate hoy = LocalDate.now();
        return repository.findAllByOrderByIdDesc().stream().map(s -> mapper.toDosificacionResponse(s, hoy)).toList();
    }

    @Transactional
    public DosificacionResponse registrar(DosificacionRequest request) {
        String autorizacion = request.numeroAutorizacion().trim();
        if (Venta.AUTORIZACION_NOTA_VENTA.equalsIgnoreCase(autorizacion)) {
            throw rechazo("Ese número de autorización está reservado.");
        }
        if (request.numeroFinal() < request.numeroInicial()) {
            throw rechazo("El número final no puede ser menor que el inicial.");
        }
        if (repository.existsByNumeroAutorizacion(autorizacion)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una dosificación con la autorización " + autorizacion + ".");
        }
        desactivarActivas();
        SecuenciaFacturacion nueva = repository.save(SecuenciaFacturacion.builder()
                .numeroAutorizacion(autorizacion)
                .leyendaSin(request.leyendaSin().trim())
                .numeroInicial(request.numeroInicial())
                .numeroFinal(request.numeroFinal())
                .numeroActual(request.numeroInicial() - 1)
                .fechaLimite(request.fechaLimite())
                .activo(true)
                .build());
        log.info("Dosificación {} registrada y activada (facturas {} a {})",
                autorizacion, request.numeroInicial(), request.numeroFinal());
        return mapper.toDosificacionResponse(nueva, LocalDate.now());
    }

    @Transactional
    public DosificacionResponse cambiarEstado(Long id, boolean activo) {
        SecuenciaFacturacion dosificacion = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dosificación no encontrada."));
        LocalDate hoy = LocalDate.now();
        if (activo) {
            if (dosificacion.getFechaLimite().isBefore(hoy)) {
                throw rechazo("La dosificación venció el " + dosificacion.getFechaLimite() + "; no se puede activar.");
            }
            if (dosificacion.getNumeroActual() >= dosificacion.getNumeroFinal()) {
                throw rechazo("La dosificación ya usó todos sus números; no se puede activar.");
            }
            desactivarActivas();
        }
        dosificacion.setActivo(activo);
        return mapper.toDosificacionResponse(dosificacion, hoy);
    }

    private void desactivarActivas() {
        repository.findByActivoTrue().forEach(s -> s.setActivo(false));
        repository.flush();
    }

    private static ResponseStatusException rechazo(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}
