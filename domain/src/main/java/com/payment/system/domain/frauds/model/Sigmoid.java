package com.payment.system.domain.frauds.model;

public final class Sigmoid {

    private static final double LN2_HI = 6.93147180369123816490e-01;
    private static final double LN2_LO = 1.90821492927058770002e-10;
    private static final double INV_LN2 = 1.44269504088896338700e+00;
    private static final double EXP_LIMIT = 700.0;

    private Sigmoid() {
    }

    public static double exp(double x) {
        if (x != x) return x;
        if (x > EXP_LIMIT) return Double.POSITIVE_INFINITY;
        if (x < -EXP_LIMIT) return 0.0;
        int k = (int) (x * INV_LN2 + (x >= 0 ? 0.5 : -0.5));
        double r = (x - k * LN2_HI) - k * LN2_LO;
        double p = 1.0;
        for (int n = 14; n >= 1; n--) p = 1.0 + p * r / n;
        return p * pow2(k);
    }

    private static double pow2(int k) {
        return Double.longBitsToDouble((long) (k + 1023) << 52);
    }

    public static double sigmoid(double z) {
        if (z != z) return z;
        if (z >= 0) return 1.0 / (1.0 + exp(-z));
        double e = exp(z);
        return e / (1.0 + e);
    }
}
