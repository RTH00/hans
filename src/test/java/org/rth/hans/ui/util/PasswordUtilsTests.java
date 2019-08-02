package org.rth.hans.ui.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.rth.hans.ui.User;

import java.io.File;
import java.time.temporal.ChronoUnit;

class PasswordUtilsTests {

    @Test
    void generatedPasswordHashIsWorkingTest() throws Exception {
        final String password = "toto";
        final User.Identification identification = PasswordUtils.generateHashing(password);
        Assertions.assertNotEquals(password, identification.getHashedPassword());
        Assertions.assertNotEquals(password, identification.getSalt());

        Assertions.assertTrue(PasswordUtils.verifyPassword(identification, password));
        Assertions.assertFalse(PasswordUtils.verifyPassword(identification, "tata"));
        Assertions.assertFalse(PasswordUtils.verifyPassword(identification, ""));
    }

}