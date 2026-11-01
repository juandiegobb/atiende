package com.devsenior.atiende.ia;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "atiende.ia")
public class IaProperties {

    private int timeoutSegundos = 30;
    private int maxTokens = 1024;
    private ProveedorConfig openai = new ProveedorConfig();
    private ProveedorConfig claude = new ProveedorConfig();

    public int getTimeoutSegundos() {
        return timeoutSegundos;
    }

    public void setTimeoutSegundos(int timeoutSegundos) {
        this.timeoutSegundos = timeoutSegundos;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public ProveedorConfig getOpenai() {
        return openai;
    }

    public void setOpenai(ProveedorConfig openai) {
        this.openai = openai;
    }

    public ProveedorConfig getClaude() {
        return claude;
    }

    public void setClaude(ProveedorConfig claude) {
        this.claude = claude;
    }

    public static class ProveedorConfig {
        private String url;
        private String modelo;
        private String llave;
        private BigDecimal precioEntradaMillon = BigDecimal.ZERO;
        private BigDecimal precioSalidaMillon = BigDecimal.ZERO;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getModelo() {
            return modelo;
        }

        public void setModelo(String modelo) {
            this.modelo = modelo;
        }

        public String getLlave() {
            return llave;
        }

        public void setLlave(String llave) {
            this.llave = llave;
        }

        public BigDecimal getPrecioEntradaMillon() {
            return precioEntradaMillon;
        }

        public void setPrecioEntradaMillon(BigDecimal precioEntradaMillon) {
            this.precioEntradaMillon = precioEntradaMillon;
        }

        public BigDecimal getPrecioSalidaMillon() {
            return precioSalidaMillon;
        }

        public void setPrecioSalidaMillon(BigDecimal precioSalidaMillon) {
            this.precioSalidaMillon = precioSalidaMillon;
        }

        public BigDecimal calcularCosto(int tokensEntrada, int tokensSalida) {
            BigDecimal entrada = precioEntradaMillon != null ? precioEntradaMillon : BigDecimal.ZERO;
            BigDecimal salida = precioSalidaMillon != null ? precioSalidaMillon : BigDecimal.ZERO;

            BigDecimal divisor = BigDecimal.valueOf(1_000_000);
            BigDecimal costoEntrada = entrada.multiply(BigDecimal.valueOf(tokensEntrada))
                    .divide(divisor, 8, RoundingMode.HALF_UP);
            BigDecimal costoSalida = salida.multiply(BigDecimal.valueOf(tokensSalida))
                    .divide(divisor, 8, RoundingMode.HALF_UP);

            return costoEntrada.add(costoSalida);
        }
    }
}

