package com.noah.minecraftagent.common.provider;

public final class TokenEstimator {
    private TokenEstimator() {
    }

    public static int estimate(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int ascii = 0;
        int nonAscii = 0;
        for (int index = 0; index < text.length(); index++) {
            if (text.charAt(index) < 128) {
                ascii++;
            } else {
                nonAscii++;
            }
        }
        return Math.max(1, (int) Math.ceil(ascii / 4.0D + nonAscii / 1.6D));
    }

    public static int estimateImageTokens(int widthHint) {
        return widthHint <= 512 ? 765 : 1105;
    }
}
