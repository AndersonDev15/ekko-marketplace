package com.ekko.notification_service.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class TemplateRendererTest {

    private final TemplateRenderer renderer = new TemplateRenderer();

    private Logger rendererLogger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void attachLogAppender() {
        rendererLogger = (Logger) LoggerFactory.getLogger(TemplateRenderer.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        rendererLogger.addAppender(logAppender);
    }

    @AfterEach
    void detachLogAppender() {
        rendererLogger.detachAppender(logAppender);
    }

    @Test
    @DisplayName("render reemplaza el placeholder cuando la variable existe")
    void render_replacesPlaceholderWhenVariableExists() {
        String result = renderer.render("Hola {{nombre}}", Map.of("nombre", "Ekko"));

        assertThat(result).isEqualTo("Hola Ekko");
    }

    @Test
    @DisplayName("render reemplaza múltiples placeholders distintos en el mismo template")
    void render_replacesMultipleDistinctPlaceholders() {
        String result = renderer.render("{{saludo}} {{nombre}}, tu pedido {{numero}}",
                Map.of("saludo", "Hola", "nombre", "Ekko", "numero", "EKK-1"));

        assertThat(result).isEqualTo("Hola Ekko, tu pedido EKK-1");
    }

    @Test
    @DisplayName("render deja el placeholder intacto y registra WARN cuando la variable no existe")
    void render_leavesMissingPlaceholderIntactAndLogsWarn() {
        String result = renderer.render("Hola {{variableFaltante}}", Map.of());

        assertThat(result).isEqualTo("Hola {{variableFaltante}}");
        assertThat(logAppender.list)
                .anyMatch(event -> event.getLevel() == Level.WARN
                        && event.getFormattedMessage().contains("variableFaltante"));
    }

    @Test
    @DisplayName("render retorna null cuando el template es null")
    void render_returnsNullWhenTemplateIsNull() {
        assertThat(renderer.render(null, Map.of())).isNull();
    }

    @Test
    @DisplayName("render trata variables null como Map vacío, dejando los placeholders intactos")
    void render_treatsNullVariablesAsEmptyMap() {
        assertThatCode(() -> {
            String result = renderer.render("Hola {{nombre}}", null);
            assertThat(result).isEqualTo("Hola {{nombre}}");
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("render devuelve el texto tal cual cuando no hay placeholders")
    void render_returnsUnchangedTextWhenNoPlaceholders() {
        String template = "Texto sin placeholders";

        assertThat(renderer.render(template, Map.of("nombre", "Ekko"))).isEqualTo(template);
    }

    @Test
    @DisplayName("render convierte valores no-String con String.valueOf")
    void render_convertsNonStringValuesWithStringValueOf() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("cantidad", 5);
        variables.put("fecha", LocalDateTime.of(2025, 8, 9, 13, 0));

        String result = renderer.render("Pedido {{cantidad}} items, creado el {{fecha}}", variables);

        assertThat(result).isEqualTo("Pedido 5 items, creado el " + LocalDateTime.of(2025, 8, 9, 13, 0));
    }
}