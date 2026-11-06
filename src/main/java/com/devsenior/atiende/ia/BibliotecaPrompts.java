package com.devsenior.atiende.ia;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class BibliotecaPrompts {

    public String cargar(String nombre) {
        return cargar(nombre, Collections.emptyMap());
    }

    public String cargar(String nombre, Map<String, String> variables) {
        String ruta = "prompts/" + nombre + ".txt";
        ClassPathResource recurso = new ClassPathResource(ruta);

        if (!recurso.exists()) {
            throw new IllegalStateException("El archivo de prompt no existe: " + ruta);
        }

        String contenido;
        try (InputStream in = recurso.getInputStream()) {
            contenido = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new IllegalStateException("Error al leer el archivo de prompt: " + ruta, e);
        }

        if (variables != null) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                String marcador = "{{" + entry.getKey() + "}}";
                contenido = contenido.replace(marcador, entry.getValue() != null ? entry.getValue() : "");
            }
        }

        if (contenido.contains("{{")) {
            throw new IllegalStateException("Quedan variables sin reemplazar en el prompt: " + nombre);
        }

        return contenido;
    }
}

