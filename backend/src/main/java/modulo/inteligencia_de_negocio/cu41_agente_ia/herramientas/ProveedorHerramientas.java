package modulo.inteligencia_de_negocio.cu41_agente_ia.herramientas;

import java.util.List;

/** CU41 - Cada grupo de herramientas (ventas, inventario, modelos, negocio) es un bean que las aporta. */
public interface ProveedorHerramientas {
    List<Herramienta> herramientas();
}
