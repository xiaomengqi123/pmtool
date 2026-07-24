package com.pmtool;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {
    @Test void wrapsSuccessfulPayloadWithStandardContract() {
        ApiResponse<String> response = ApiResponse.ok("value");
        assertEquals(0, response.code());
        assertEquals("ok", response.message());
        assertEquals("value", response.data());
    }
}
