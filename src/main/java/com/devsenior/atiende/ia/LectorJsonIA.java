package com.devsenior.atiende.ia;

import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class LectorJsonIA {

    private final ObjectMapper objectMapper;

    public LectorJsonIA(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> T leer(RespuestaIA respuesta, Class<T> clase) {
        if (respuesta == null || respuesta.texto() == null || respuesta.texto().isBlank()) {
            throw new RespuestaIaInvalidaException("La respuesta de la IA está vacía");
        }

        String texto = respuesta.texto();
        int inicio = texto.indexOf('{');
        int fin = texto.lastIndexOf('}');

        if (inicio == -1 || fin == -1 || inicio > fin) {
            throw new RespuestaIaInvalidaException("No se encontró ningún objeto JSON en la respuesta de la IA");
        }

        String json = texto.substring(inicio, fin + 1);

        try {
            T resultado = objectMapper.readValue(json, clase);
            if (resultado == null) {
                throw new RespuestaIaInvalidaException("El JSON deserializado resultó nulo");
            }
            return resultado;
        } catch (JacksonException e) {
            throw new RespuestaIaInvalidaException("Error al interpretar el JSON de la IA: " + e.getMessage(), e);
        }
    }
}
