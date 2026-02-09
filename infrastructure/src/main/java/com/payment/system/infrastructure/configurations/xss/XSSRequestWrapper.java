package com.payment.system.infrastructure.configurations.xss;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

public class XSSRequestWrapper extends HttpServletRequestWrapper {

    public XSSRequestWrapper(HttpServletRequest servletRequest) {
        super(servletRequest);
    }

    @Override
    public String getParameter(String name) {
        return SanitizeUtils.sanitize(super.getParameter(name));
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) return null;
        for (int i = 0; i < values.length; i++) {
            values[i] = SanitizeUtils.sanitize(values[i]);
        }
        return values;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> originalMap = super.getParameterMap();

        // Cria uma cópia mutável do mapa
        Map<String, String[]> copy = new LinkedHashMap<>();

        for (Map.Entry<String, String[]> entry : originalMap.entrySet()) {
            String[] values = entry.getValue();
            String[] sanitizedValues = new String[values.length];

            for (int i = 0; i < values.length; i++) {
                sanitizedValues[i] = SanitizeUtils.sanitize(values[i]);
            }
            copy.put(entry.getKey(), sanitizedValues);
        }

        return copy;
    }

    @Override
    public String getHeader(String name) {
        return SanitizeUtils.sanitize(super.getHeader(name));
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        Enumeration<String> headers = super.getHeaders(name);
        return new Enumeration<>() {
            @Override
            public boolean hasMoreElements() {
                return headers.hasMoreElements();
            }

            @Override
            public String nextElement() {
                return SanitizeUtils.sanitize(headers.nextElement());
            }
        };
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return super.getHeaderNames();
    }
}
