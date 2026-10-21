package modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.service;

import comun.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.Producto;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.repository.ProductoRepository;
import modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.dto.ActualizarListaPrecioRequest;
import modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.dto.ActualizarPreciosMatrizRequest;
import modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.dto.CrearListaPrecioRequest;
import modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.dto.ItemPrecioProductoDTO;
import modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.dto.ListaPrecioDTO;
import modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.entity.ListaPrecio;
import modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.entity.PrecioProducto;
import modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.repository.ListaPrecioRepository;
import modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.repository.PrecioProductoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListaPrecioService {

    private final ListaPrecioRepository listas;
    private final PrecioProductoRepository preciosProducto;
    private final ProductoRepository productos;

    public List<ListaPrecioDTO> listarListas() {
        return listas.findAllByOrderByEsPrincipalDescNombreAsc().stream()
                .map(this::toDTO)
                .toList();
    }

    public ListaPrecioDTO obtenerPorId(Integer id) {
        return toDTO(find(id));
    }

    @Transactional
    public ListaPrecioDTO crearLista(CrearListaPrecioRequest req) {
        String codigo = req.codigo().trim().toUpperCase();
        if (listas.existsByCodigoIgnoreCase(codigo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una lista de precios con el código '" + codigo + "'.");
        }

        ListaPrecio lista = new ListaPrecio();
        lista.setCodigo(codigo);
        lista.setNombre(req.nombre().trim());
        lista.setDescripcion(req.descripcion() != null ? req.descripcion().trim() : null);
        lista.setActivo(true);
        lista.setEsPrincipal(Boolean.TRUE.equals(req.esPrincipal()));

        ListaPrecio guardada = listas.save(lista);

        if (guardada.getEsPrincipal()) {
            listas.desmarcarOtrasComoPrincipal(guardada.getId());
        }

        return toDTO(guardada);
    }

    @Transactional
    public ListaPrecioDTO actualizarLista(Integer id, ActualizarListaPrecioRequest req) {
        ListaPrecio lista = find(id);

        lista.setNombre(req.nombre().trim());
        lista.setDescripcion(req.descripcion() != null ? req.descripcion().trim() : null);
        if (req.activo() != null) {
            lista.setActivo(req.activo());
        }
        if (req.esPrincipal() != null) {
            lista.setEsPrincipal(req.esPrincipal());
        }

        ListaPrecio guardada = listas.save(lista);

        if (guardada.getEsPrincipal()) {
            listas.desmarcarOtrasComoPrincipal(guardada.getId());
        }

        return toDTO(guardada);
    }

    @Transactional
    public ListaPrecioDTO cambiarEstado(Integer id, boolean activo) {
        ListaPrecio lista = find(id);
        if (!activo && Boolean.TRUE.equals(lista.getEsPrincipal())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede desactivar la lista de precios predeterminada.");
        }
        lista.setActivo(activo);
        return toDTO(listas.save(lista));
    }

    public List<ItemPrecioProductoDTO> obtenerMatrizProductos(Integer listaId) {
        find(listaId); // validar existencia

        List<Producto> catalogo = productos.findByActivoTrue();
        Map<Long, PrecioProducto> preciosMap = preciosProducto.findByListaPrecioId(listaId).stream()
                .collect(Collectors.toMap(p -> p.getProducto().getId(), Function.identity(), (a, b) -> a));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        List<ItemPrecioProductoDTO> resultado = new ArrayList<>();

        for (Producto prod : catalogo) {
            PrecioProducto pp = preciosMap.get(prod.getId());
            BigDecimal precioAsignado = pp != null ? pp.getPrecio() : prod.getPrecioVenta();
            BigDecimal costo = prod.getCostoPromedio() != null ? prod.getCostoPromedio() : BigDecimal.ZERO;

            BigDecimal margen = BigDecimal.ZERO;
            if (costo.compareTo(BigDecimal.ZERO) > 0 && precioAsignado != null) {
                margen = precioAsignado.subtract(costo)
                        .divide(costo, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            String fechaAct = pp != null && pp.getFechaActualizacion() != null
                    ? pp.getFechaActualizacion().format(fmt)
                    : "No fijado";

            String catNombre = prod.getCategoria() != null ? prod.getCategoria().getNombre() : "General";

            resultado.add(new ItemPrecioProductoDTO(
                    prod.getId(),
                    prod.getCodigoSku(),
                    prod.getNombre(),
                    catNombre,
                    costo,
                    prod.getPrecioVenta(),
                    precioAsignado,
                    margen,
                    fechaAct
            ));
        }

        return resultado;
    }

    @Transactional
    public void actualizarPreciosMatriz(Integer listaId, ActualizarPreciosMatrizRequest req) {
        ListaPrecio lista = find(listaId);

        Map<Long, Producto> productosMap = productos.findAllById(
                req.items().stream().map(ActualizarPreciosMatrizRequest.ItemActualizarPrecio::productoId).toList()
        ).stream().collect(Collectors.toMap(Producto::getId, Function.identity()));

        // Validar Regla RN19: Protección de Margen
        for (ActualizarPreciosMatrizRequest.ItemActualizarPrecio item : req.items()) {
            Producto prod = productosMap.get(item.productoId());
            if (prod == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado con ID: " + item.productoId());
            }

            BigDecimal costo = prod.getCostoPromedio() != null ? prod.getCostoPromedio() : BigDecimal.ZERO;
            if (costo.compareTo(BigDecimal.ZERO) > 0 && item.precio().compareTo(costo) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("Violación de Regla RN19 (Protección de Margen): El precio asignado Bs %s para '%s' es inferior a su costo de compra (Bs %s).",
                                item.precio(), prod.getNombre(), costo));
            }
        }

        // Persistir o actualizar cada precio
        for (ActualizarPreciosMatrizRequest.ItemActualizarPrecio item : req.items()) {
            Producto prod = productosMap.get(item.productoId());
            PrecioProducto pp = preciosProducto.findByListaPrecioIdAndProductoId(listaId, item.productoId())
                    .orElseGet(() -> {
                        PrecioProducto nuevo = new PrecioProducto();
                        nuevo.setListaPrecio(lista);
                        nuevo.setProducto(prod);
                        return nuevo;
                    });

            pp.setPrecio(item.precio());
            pp.setFechaActualizacion(LocalDateTime.now());
            preciosProducto.save(pp);
        }
    }

    private ListaPrecio find(Integer id) {
        return listas.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lista de precios inexistente con ID: " + id));
    }

    private ListaPrecioDTO toDTO(ListaPrecio l) {
        long count = preciosProducto.countByListaPrecioId(l.getId());
        return new ListaPrecioDTO(
                l.getId(),
                l.getCodigo(),
                l.getNombre(),
                l.getDescripcion(),
                Boolean.TRUE.equals(l.getEsPrincipal()),
                Boolean.TRUE.equals(l.getActivo()),
                count
        );
    }
}
