package modulo.ventas_y_facturacion.pagos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;

public class StripeCheckoutRequestDTO {

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.50", message = "El monto mínimo es de 0.50")
    private BigDecimal monto;

    @NotBlank(message = "La moneda es obligatoria")
    private String moneda;

    @NotBlank(message = "El nombre o concepto es obligatorio")
    private String concepto;

    private String descripcion;
    private String clienteEmail;
    private String clienteNombre;
    private String referenciaId;
    private String successUrl;
    private String cancelUrl;
    private Map<String, String> metadata;

    public StripeCheckoutRequestDTO() {}

    public StripeCheckoutRequestDTO(BigDecimal monto, String moneda, String concepto, String descripcion,
                                    String clienteEmail, String clienteNombre, String referenciaId,
                                    String successUrl, String cancelUrl, Map<String, String> metadata) {
        this.monto = monto;
        this.moneda = moneda;
        this.concepto = concepto;
        this.descripcion = descripcion;
        this.clienteEmail = clienteEmail;
        this.clienteNombre = clienteNombre;
        this.referenciaId = referenciaId;
        this.successUrl = successUrl;
        this.cancelUrl = cancelUrl;
        this.metadata = metadata;
    }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }

    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getClienteEmail() { return clienteEmail; }
    public void setClienteEmail(String clienteEmail) { this.clienteEmail = clienteEmail; }

    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }

    public String getReferenciaId() { return referenciaId; }
    public void setReferenciaId(String referenciaId) { this.referenciaId = referenciaId; }

    public String getSuccessUrl() { return successUrl; }
    public void setSuccessUrl(String successUrl) { this.successUrl = successUrl; }

    public String getCancelUrl() { return cancelUrl; }
    public void setCancelUrl(String cancelUrl) { this.cancelUrl = cancelUrl; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
}
