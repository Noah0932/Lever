package com.noah.minecraftagent.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecureLogTest {

    @Test
    void shouldMaskBearerToken() {
        String input = "Authorization: Bearer sk-1234567890abcdef";
        String result = SecureLog.mask(input);
        assertFalse(result.contains("sk-1234567890abcdef"));
        assertTrue(result.contains("***"));
    }

    @Test
    void shouldMaskApiKeyInJson() {
        String input = "{\"api_key\":\"sk-secret-key-123\"}";
        String result = SecureLog.mask(input);
        assertFalse(result.contains("sk-secret-key-123"));
    }

    @Test
    void shouldMaskXApiKeyHeader() {
        String input = "x-api-key: my-secret-key-value";
        String result = SecureLog.mask(input);
        assertFalse(result.contains("my-secret-key-value"));
    }

    @Test
    void shouldMaskQuerySecret() {
        String input = "https://api.example.com?api_key=abcdef123456&other=value";
        String result = SecureLog.mask(input);
        assertFalse(result.contains("abcdef123456"));
    }

    @Test
    void shouldMaskProxyPassword() {
        String input = "http://user:password123@proxy.example.com:8080";
        String result = SecureLog.mask(input);
        assertFalse(result.contains("password123"));
    }

    @Test
    void shouldReturnNullForNullInput() {
        assertNull(SecureLog.mask(null));
    }

    @Test
    void shouldNotAlterSafeText() {
        String input = "The AI Agent started successfully.";
        String result = SecureLog.mask(input);
        assertEquals(input, result);
    }

    @Test
    void shouldMaskAuthHeaderCaseInsensitively() {
        String result = SecureLog.mask("AUTHORIZATION: Bearer abc123def456");
        assertFalse(result.contains("abc123def456"));
    }
}
