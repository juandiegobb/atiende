package com.devsenior.atiende.ia;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class LectorPrompts {

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String leer(String nombrePrompt) {
        return cache.computeIfAbsent(nombrePrompt, this::cargarDesdeClasspath);
    }

    private String cargarDesdeClasspath(String nombrePrompt) {
        String ruta = "prompts/" + nombrePrompt + ".txt";
        ClassPathResource recurso = new ClassPathResource(ruta);
        try (InputStream in = recurso.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cargar el archivo de prompt: " + ruta, e);
        }
    }
}

