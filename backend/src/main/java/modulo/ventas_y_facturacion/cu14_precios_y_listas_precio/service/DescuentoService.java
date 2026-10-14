package modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.service;

import comun.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.dto.ActualizarDescuentoRequest;
import modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.dto.CrearDescuentoRequest;
import modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.dto.DescuentoDTO;
import modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.entity.Descuento;
import modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.repository.DescuentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DescuentoService {

    private final DescuentoRepository descuentos;

    public List<DescuentoDTO> listarTodos() {
        return descuentos.findAllByOrderByActivoDescNombreAsc().stream()
                .map(this::toDTO)
                .toList();
    }

    public List<DescuentoDTO> listarActivos() {
        return descuentos.findByActivoTrue().stream()
                .map(this::toDTO)
                .toList();
    }

    public DescuentoDTO obtenerPorId(Integer id) {
        return toDTO(find(id));
    }

    @Transactional
    public DescuentoDTO crear(CrearDescuentoRequest req) {
        Descuento d = new Descuento();
        d.setNombre(req.nombre().trim());
        d.setTipo(req.tipo().trim().toUpperCase());
        d.setValor(req.valor());
        d.setFechaInicio(req.fechaInicio());
        d.setFechaFin(req.fechaFin());
        d.setActivo(true);
        return toDTO(descuentos.save(d));
    }

    @Transactional
    public DescuentoDTO actualizar(Integer id, ActualizarDescuentoRequest req) {
        Descuento d = find(id);
        d.setNombre(req.nombre().trim());
        d.setTipo(req.tipo().trim().toUpperCase());
        d.setValor(req.valor());
        d.setFechaInicio(req.fechaInicio());
        d.setFechaFin(req.fechaFin());
        if (req.activo() != null) {
            d.setActivo(req.activo());
        }
        return toDTO(descuentos.save(d));
    }

    @Transactional
    public DescuentoDTO cambiarEstado(Integer id, boolean activo) {
        Descuento d = find(id);
        d.setActivo(activo);
        return toDTO(descuentos.save(d));
    }

    @Transactional
    public void eliminar(Integer id) {
        Descuento d = find(id);
        descuentos.delete(d);
    }

    private Descuento find(Integer id) {
        return descuentos.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Regla de descuento no encontrada con ID: " + id));
    }

    private DescuentoDTO toDTO(Descuento d) {
        return new DescuentoDTO(
                d.getId(),
                d.getNombre(),
                d.getTipo(),
                d.getValor(),
                d.getFechaInicio(),
                d.getFechaFin(),
                Boolean.TRUE.equals(d.getActivo())
        );
    }
}
