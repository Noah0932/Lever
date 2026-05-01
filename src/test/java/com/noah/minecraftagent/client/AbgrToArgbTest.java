package com.noah.minecraftagent.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD: ABGR → ARGB pixel channel swap for NativeImage to BufferedImage conversion.
 */
class AbgrToArgbTest {

    @Test
    void shouldSwapRedBlueChannels() {
        int abgr = 0xFF_00_00_FF; // A=255, B=0, G=0, R=255
        int argb = swapAbgrToArgb(abgr);
        assertEquals(0xFF_FF_00_00, argb); // A=255, R=255, G=0, B=0
    }

    @Test
    void shouldPreserveAlphaAndGreen() {
        int abgr = 0x80_00_FF_00; // A=128, B=0, G=255, R=0
        int argb = swapAbgrToArgb(abgr);
        assertEquals(0x80_00_FF_00, argb); // alpha and green unchanged
    }

    @Test
    void shouldHandleBlackPixel() {
        int abgr = 0xFF_00_00_00;
        int argb = swapAbgrToArgb(abgr);
        assertEquals(0xFF_00_00_00, argb);
    }

    @Test
    void shouldHandleWhitePixel() {
        int abgr = 0xFF_FF_FF_FF;
        int argb = swapAbgrToArgb(abgr);
        assertEquals(0xFF_FF_FF_FF, argb);
    }

    @Test
    void shouldSwapMidtoneRedBlue() {
        int abgr = 0xFF_44_00_BB; // red=0x44, blue=0xBB
        int argb = swapAbgrToArgb(abgr);
        assertEquals(0xFF_BB_00_44, argb); // red=0xBB, blue=0x44
    }

    @Test
    void shouldHandleArrayBulkConversion() {
        int[] abgrPixels = {0xFF_00_00_FF, 0x80_00_FF_00, 0xFF_FF_FF_FF, 0xFF_00_00_00};
        int[] argbPixels = swapAbgrToArgbBulk(abgrPixels);
        assertEquals(0xFF_FF_00_00, argbPixels[0]);
        assertEquals(0x80_00_FF_00, argbPixels[1]);
        assertEquals(0xFF_FF_FF_FF, argbPixels[2]);
        assertEquals(0xFF_00_00_00, argbPixels[3]);
    }

    @Test
    void shouldHandleSemiTransparentPixel() {
        int abgr = 0x40_AB_CD_EF;
        int argb = swapAbgrToArgb(abgr);
        assertEquals(0x40, (argb >>> 24) & 0xFF); // alpha preserved
        assertEquals(0xCD, (argb >>> 8) & 0xFF);  // green preserved
        assertEquals(0xEF, (argb >>> 16) & 0xFF); // original red (EF) now in red channel
        assertEquals(0xAB, argb & 0xFF);           // original blue (AB) now in blue channel
    }

    static int swapAbgrToArgb(int abgr) {
        return (abgr & 0xFF00FF00) | ((abgr & 0x00FF0000) >>> 16) | ((abgr & 0x000000FF) << 16);
    }

    static int[] swapAbgrToArgbBulk(int[] abgrPixels) {
        int[] result = new int[abgrPixels.length];
        for (int i = 0; i < abgrPixels.length; i++) {
            result[i] = swapAbgrToArgb(abgrPixels[i]);
        }
        return result;
    }
}
