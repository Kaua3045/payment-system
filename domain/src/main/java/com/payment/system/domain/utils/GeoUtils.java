package com.payment.system.domain.utils;

import com.payment.system.domain.frauds.GeoPoint;

public final class GeoUtils {
    private static final double EARTH_RADIUS_KM = 6371.0088;
    private GeoUtils() {}

    /** Distância haversine entre dois pontos (graus). Erro de ~0,5%, suficiente para "viagem impossível". */
    public static double haversineKm(GeoPoint a, GeoPoint b) {
        double dLat = Math.toRadians(b.lat() - a.lat());
        double dLon = Math.toRadians(b.lon() - a.lon());
        double lat1 = Math.toRadians(a.lat());
        double lat2 = Math.toRadians(b.lat());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(h));
    }
}
