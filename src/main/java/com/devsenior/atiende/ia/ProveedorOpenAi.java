package com.devsenior.atiende.ia;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Component
public class ProveedorOpenAi implements ProveedorIA {

    private final IaProperties properties;
    private final RestClient restClient;

    @Autowired
    public ProveedorOpenAi(IaProperties properties) {
        this(properties, crearRestClient(properties.getTimeoutSegundos()));
    }

    ProveedorOpenAi(IaProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public String nombre() {
        return "openai";
    }

    @Override
    public RespuestaIA completar(String instrucciones, String mensaje) {
        IaProperties.ProveedorConfig config = properties.getOpenai();
        String url = resolverUrl(config.getUrl());

        PeticionOpenAi peticion = new PeticionOpenAi(
                config.getModelo(),
                List.of(
                        new MensajeOpenAi("system", instrucciones),
                        new MensajeOpenAi("user", mensaje)),
                properties.getMaxTokens());

        long inicio = System.currentTimeMillis();

        RespuestaOpenAi respuesta = restClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + config.getLlave())
                .contentType(MediaType.APPLICATION_JSON)
                .body(peticion)
                .retrieve()
                .body(RespuestaOpenAi.class);

        long milisegundos = System.currentTimeMillis() - inicio;

        if (respuesta == null || respuesta.choices() == null || respuesta.choices().isEmpty()) {
            throw new IllegalStateException("Respuesta vacía o sin elecciones del proveedor " + nombre());
        }

        String texto = respuesta.choices().get(0).message() != null
                ? respuesta.choices().get(0).message().content()
                : null;

        if (texto == null || texto.isBlank()) {
            throw new IllegalStateException("Respuesta sin contenido de texto del proveedor " + nombre());
        }

        int tokensEntrada = respuesta.usage() != null ? respuesta.usage().promptTokens() : 0;
        int tokensSalida = respuesta.usage() != null ? respuesta.usage().completionTokens() : 0;
        BigDecimal costoEstimado = config.calcularCosto(tokensEntrada, tokensSalida);

        return new RespuestaIA(
                texto.trim(),
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
        return limpia.endsWith("/chat/completions") ? limpia : limpia + "/chat/completions";
    }

    private static RestClient crearRestClient(int timeoutSegundos) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(timeoutSegundos));
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSegundos));
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    record PeticionOpenAi(
            String model,
            List<MensajeOpenAi> messages,
            @JsonProperty("max_tokens") Integer maxTokens) {
    }

    record MensajeOpenAi(
            String role,
            String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespuestaOpenAi(
            String id,
            String model,
            List<EleccionOpenAi> choices,
            UsoOpenAi usage) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EleccionOpenAi(
            int index,
            MensajeOpenAi message,
            @JsonProperty("finish_reason") String finishReason) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record UsoOpenAi(
            @JsonProperty("prompt_tokens") int promptTokens,
            @JsonProperty("completion_tokens") int completionTokens,
            @JsonProperty("total_tokens") int totalTokens) {
    }
}

