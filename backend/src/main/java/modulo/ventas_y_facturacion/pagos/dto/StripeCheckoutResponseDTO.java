package modulo.ventas_y_facturacion.pagos.dto;

public class StripeCheckoutResponseDTO {

    private String sessionId;
    private String checkoutUrl;
    private String estado;
    private String referenciaId;
    private String publicKey;

    public StripeCheckoutResponseDTO() {}

    public StripeCheckoutResponseDTO(String sessionId, String checkoutUrl, String estado, String referenciaId, String publicKey) {
        this.sessionId = sessionId;
        this.checkoutUrl = checkoutUrl;
        this.estado = estado;
        this.referenciaId = referenciaId;
        this.publicKey = publicKey;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String sessionId;
        private String checkoutUrl;
        private String estado;
        private String referenciaId;
        private String publicKey;

        public Builder sessionId(String sessionId) { this.sessionId = sessionId; return this; }
        public Builder checkoutUrl(String checkoutUrl) { this.checkoutUrl = checkoutUrl; return this; }
        public Builder estado(String estado) { this.estado = estado; return this; }
        public Builder referenciaId(String referenciaId) { this.referenciaId = referenciaId; return this; }
        public Builder publicKey(String publicKey) { this.publicKey = publicKey; return this; }

        public StripeCheckoutResponseDTO build() {
            return new StripeCheckoutResponseDTO(sessionId, checkoutUrl, estado, referenciaId, publicKey);
        }
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getCheckoutUrl() { return checkoutUrl; }
    public void setCheckoutUrl(String checkoutUrl) { this.checkoutUrl = checkoutUrl; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getReferenciaId() { return referenciaId; }
    public void setReferenciaId(String referenciaId) { this.referenciaId = referenciaId; }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
}
