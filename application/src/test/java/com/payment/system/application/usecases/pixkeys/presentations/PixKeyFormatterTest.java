package com.payment.system.application.usecases.pixkeys.presentations;

import com.payment.system.domain.UnitTest;
import com.payment.system.domain.pixkeys.PixKeyType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PixKeyFormatterTest extends UnitTest {

    @Test
    void givenAValidCpfCleaned_whenCallsFormat_thenShouldReturnFormattedCpf() {
        final var expectedCpf = "117.686.790-37";
        final var aCleanedCpf = "11768679037";

        final var aFormattedCpf = PixKeyFormatter.format(aCleanedCpf, PixKeyType.CPF);

        Assertions.assertEquals(expectedCpf, aFormattedCpf);
    }

    @Test
    void givenAValidCnpjCleaned_whenCallsFormat_thenShouldReturnFormattedCnpj() {
        final var expectedCnpj = "83.112.584/0001-14";
        final var aCleanedCnpj = "83112584000114";

        final var aFormattedCnpj = PixKeyFormatter.format(aCleanedCnpj, PixKeyType.CNPJ);

        Assertions.assertEquals(expectedCnpj, aFormattedCnpj);
    }

    @Test
    void givenAValidEmail_whenCallsFormat_thenShouldReturnEmail() {
        final var expectedEmail = "april.shephard@richestmag.test";

        final var aFormattedEmail = PixKeyFormatter.format(expectedEmail, PixKeyType.EMAIL);

        Assertions.assertEquals(expectedEmail, aFormattedEmail);
    }
}
