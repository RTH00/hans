package org.rth.hans;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.fail;

public class UtilTests {


    @Test
    void loadResourceCorrectlyTest() throws IOException {
        Assertions.assertEquals("I am a resource", Utils.readResource("test.txt"));
    }



}
