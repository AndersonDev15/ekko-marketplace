package com.ekko.notification_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class TemplateRenderer {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");
    private static final Map<String, Object> EMPTY_VARIABLES = Map.of();

    public String render(String template, Map<String, Object> variables) {
        if (template == null) {
            return null;
        }

        Map<String, Object> vars = variables != null ? variables : EMPTY_VARIABLES;
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String variableName = matcher.group(1);
            String placeholder = matcher.group(0);
            if (vars.containsKey(variableName)) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(String.valueOf(vars.get(variableName))));
            } else {
                log.warn("Variable '{}' not found for placeholder '{}'; leaving it as-is", variableName, placeholder);
                matcher.appendReplacement(result, Matcher.quoteReplacement(placeholder));
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }
}