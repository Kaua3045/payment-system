package com.kaua.template.infrastructure.configurations.xss;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.Enumeration;
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
        Map<String, String[]> map = super.getParameterMap();
        map.replaceAll((key, value) -> {
            for (int i = 0; i < value.length; i++) {
                value[i] = SanitizeUtils.sanitize(value[i]);
            }
            return value;
        });
        return map;
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
