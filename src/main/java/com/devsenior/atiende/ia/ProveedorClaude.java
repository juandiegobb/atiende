package com.devsenior.atiende.ia;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Component
public class ProveedorClaude implements ProveedorIA {

    private final IaProperties properties;
    private final RestClient restClient;

    @Autowired
    public ProveedorClaude(IaProperties properties) {
        this(properties, crearRestClient(properties.getTimeoutSegundos()));
    }

    ProveedorClaude(IaProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public String nombre() {
        return "claude";
    }

    @Override
    public RespuestaIA completar(String instrucciones, String mensaje) {
        IaProperties.ProveedorConfig config = properties.getClaude();
        String url = resolverUrl(config.getUrl());

        PeticionClaude peticion = new PeticionClaude(
                config.getModelo(),
                properties.getMaxTokens(),
                instrucciones,
                new ThinkingClaude("disabled"),
                List.of(new MensajeClaude("user", mensaje)));

        long inicio = System.currentTimeMillis();

        RespuestaClaude respuesta = restClient.post()
                .uri(url)
                .header("x-api-key", config.getLlave())
                .header("anthropic-version", "2023-06-01")
                .contentType(MediaType.APPLICATION_JSON)
                .body(peticion)
                .retrieve()
                .body(RespuestaClaude.class);

        long milisegundos = System.currentTimeMillis() - inicio;

        if (respuesta == null || respuesta.content() == null || respuesta.content().isEmpty()) {
            throw new IllegalStateException("Respuesta vacía o sin contenido del proveedor " + nombre());
        }

        String texto = respuesta.content().stream()
                .filter(b -> "text".equalsIgnoreCase(b.type()))
                .map(BloqueContenidoClaude::text)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"))
                .trim();

        if (texto.isBlank()) {
            throw new IllegalStateException("Respuesta sin bloques de texto válidos del proveedor " + nombre());
        }

        int tokensEntrada = respuesta.usage() != null ? respuesta.usage().inputTokens() : 0;
        int tokensSalida = respuesta.usage() != null ? respuesta.usage().outputTokens() : 0;
        BigDecimal costoEstimado = config.calcularCosto(tokensEntrada, tokensSalida);

        return new RespuestaIA(
                texto,
                nombre(),
                config.getModelo(),
                tokensEntrada,
                tokensSalida,
                costoEstimado,
                milisegundos);
    }

    private String resolverUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("URL no configurada para el proveedor " + nombre());
        }
        String limpia = baseUrl.replaceAll("/+$", "");
        return limpia.endsWith("/v1/messages") ? limpia : limpia + "/v1/messages";
    }

    private static RestClient crearRestClient(int timeoutSegundos) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(timeoutSegundos));
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSegundos));
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    record PeticionClaude(
            String model,
            @JsonProperty("max_tokens") int maxTokens,
            String system,
            ThinkingClaude thinking,
            List<MensajeClaude> messages) {
    }

    record ThinkingClaude(
            String type) {
    }

    record MensajeClaude(
            String role,
            String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespuestaClaude(
            String id,
            String type,
            String role,
            String model,
            List<BloqueContenidoClaude> content,
            @JsonProperty("stop_reason") String stopReason,
            UsoClaude usage) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BloqueContenidoClaude(
            String type,
            String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record UsoClaude(
            @JsonProperty("input_tokens") int inputTokens,
            @JsonProperty("output_tokens") int outputTokens) {
    }
}

