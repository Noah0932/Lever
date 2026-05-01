package com.noah.minecraftagent.common.provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.junit.jupiter.api.Assertions.*;

class TokenEstimatorTest {

    @Test
    void shouldEstimateAsciiText() {
        int tokens = TokenEstimator.estimate("Hello world, this is a test.");
        assertTrue(tokens > 0);
        assertTrue(tokens <= 15);
    }

    @Test
    void shouldEstimateChineseText() {
        int tokens = TokenEstimator.estimate("你好世界这是一段测试文本");
        assertTrue(tokens > 0);
        assertTrue(tokens >= 8);
    }

    @Test
    void shouldEstimateSmallImageTokens() {
        int tokens = TokenEstimator.estimateImageTokens(256);
        assertEquals(765, tokens);
    }

    @Test
    void shouldEstimateLargeImageTokens() {
        int tokens = TokenEstimator.estimateImageTokens(1024);
        assertEquals(1105, tokens);
    }

    @Test
    void shouldEstimateImageTokensAtBoundary() {
        int tokensAt512 = TokenEstimator.estimateImageTokens(512);
        assertEquals(765, tokensAt512);
        int tokensAt513 = TokenEstimator.estimateImageTokens(513);
        assertEquals(1105, tokensAt513);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldReturnZeroForNullOrEmptyText(String input) {
        assertEquals(0, TokenEstimator.estimate(input));
    }

    @Test
    void shouldReturnAtLeastOneForNonEmptyText() {
        int tokens = TokenEstimator.estimate("a");
        assertEquals(1, tokens);
    }
}
