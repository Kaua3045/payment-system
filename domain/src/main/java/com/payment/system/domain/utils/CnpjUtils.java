package com.payment.system.domain.utils;

public final class CnpjUtils {

    private static final int[] WEIGHT_DIGIT1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] WEIGHT_DIGIT2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private CnpjUtils() {}

    public static boolean validateCnpj(final String aRawCnpj) {
        if (aRawCnpj == null || aRawCnpj.isBlank()) return false;
        final var aCnpj = cleanCnpj(aRawCnpj);

        if (cnpjLengthIsNotValid(aCnpj)) return false;
        if (areAllDigitsTheRepeated(aCnpj)) return false;

        final var digit1 = calculateDigit(aCnpj, WEIGHT_DIGIT1);
        final var digit2 = calculateDigit(aCnpj, WEIGHT_DIGIT2);
        final var actualDigits = extractDigits(aCnpj);
        final var validatedDigits = digit1 + "" + digit2;
        return actualDigits.equals(validatedDigits);
    }

    public static String cleanCnpj(final String rawCnpj) {
        return rawCnpj.replaceAll("\\D", "").trim();
    }

    public static String formatCnpj(final String aCnpj) {
        final var aCleanedCnpj = cleanCnpj(aCnpj);
        return aCleanedCnpj.substring(0, 2) + "." +
                aCleanedCnpj.substring(2, 5) + "." +
                aCleanedCnpj.substring(5, 8) + "/" +
                aCleanedCnpj.substring(8, 12) + "-" +
                aCleanedCnpj.substring(12);
    }

    private static String extractDigits(final String aCnpj) {
        return aCnpj.substring(12);
    }

    private static int calculateDigit(final String aCnpj, final int[] weight) {
        int total = 0;
        for (int i = 0; i < weight.length; i++) {
            int num = Character.getNumericValue(aCnpj.charAt(i));
            total += num * weight[i];
        }
        final var rest = total % 11;
        return (rest < 2) ? 0 : 11 - rest;
    }

    private static boolean cnpjLengthIsNotValid(final String aCnpj) {
        return aCnpj.trim().length() != 14;
    }

    private static boolean areAllDigitsTheRepeated(final String str) {
        for (int i = 0; i < str.length() - 1; i++) {
            if (str.charAt(i) != str.charAt(i + 1)) {
                return false;
            }
        }
        return true;
    }
}
