package com.payment.system.domain.utils;

import com.payment.system.domain.UnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CnpjUtilsTest extends UnitTest {

    @ParameterizedTest
    @CsvSource({
            "44.489.589/0001-09",
            "75.253.571/0001-37",
            "38.627.087/0001-02",
            "74.482.371/0001-93",
            "71.675.827/0001-43",
            "70515021000125"
    })
    void givenAValidCnpj_whenValidateCnpj_thenReturnTrue(final String aCnpj) {
        final var aResult = CnpjUtils.validateCnpj(aCnpj);

        Assertions.assertTrue(aResult);
    }

    @ParameterizedTest
    @CsvSource({
            "00.000.000/0000-00",
            "11.111.111/1111-11",
            "22.222.222/2222-22",
            "33.333.333/3333-33",
            "44.444.444/4444-44",
            "55.555.555/5555-55",
            "66.666.666/6666-66",
            "77.777.777/7777-77",
            "88.888.888/8888-88",
            "99.999.999/9999-99",
            "44.489.589/0001-00",
            "75.253.571/0001-38",
            "38.627.087/0001-03",
            "74.482.371/0001-94",
            "71.675.827/0001-44",
            "70515021000126",
            "7051502100012",
    })
    void givenAnInvalidCnpj_whenValidateCnpj_thenReturnFalse(final String aCnpj) {
        final var aResult = CnpjUtils.validateCnpj(aCnpj);

        Assertions.assertFalse(aResult);
    }

    @Test
    void givenAnNullCnpj_whenValidateCnpj_thenReturnFalse() {
        final var aResult = CnpjUtils.validateCnpj(null);

        Assertions.assertFalse(aResult);
    }

    @Test
    void givenAnEmptyCnpj_whenValidateCnpj_thenReturnFalse() {
        final var aResult = CnpjUtils.validateCnpj("");

        Assertions.assertFalse(aResult);
    }

    @ParameterizedTest
    @CsvSource({
            "44.489.589/0001-09, 44489589000109",
            "75.253.571/0001-37, 75253571000137",
            "38.627.087/0001-02, 38627087000102",
            "74.482.371/0001-93, 74482371000193",
            "71.675.827/0001-43, 71675827000143",
            "70515021000125, 70515021000125"
    })
    void givenACnpj_whenCleanCnpj_thenReturnCleanedCnpj(final String aCnpj, final String aExpected) {
        final var aResult = CnpjUtils.cleanCnpj(aCnpj);

        Assertions.assertEquals(aExpected, aResult);
    }

    @ParameterizedTest
    @CsvSource({
            "44489589000109, 44.489.589/0001-09",
            "75253571000137, 75.253.571/0001-37",
            "38627087000102, 38.627.087/0001-02",
            "74482371000193, 74.482.371/0001-93",
            "71675827000143, 71.675.827/0001-43",
            "70515021000125, 70.515.021/0001-25"
    })
    void givenACnpj_whenFormatCnpj_thenReturnFormattedCnpj(final String aCnpj, final String aExpected) {
        final var aResult = CnpjUtils.formatCnpj(aCnpj);

        Assertions.assertEquals(aExpected, aResult);
    }
}
