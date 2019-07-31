package org.rth.hans.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class UtilTests {


    @Test
    void loadResourceCorrectlyTest() throws IOException {
        Assertions.assertEquals("I am a resource", Utils.readResource("test.txt"));
    }



}
