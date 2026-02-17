package com.percussion.testing;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class HandlerSampleParameterizedTest {

    @ParameterizedTest(name = "value={0}")
    @ValueSource(ints = {1, 2})
    void paramTest(int value) {
        if (value == 2) {
            fail("failing param");
        }
    }
}
