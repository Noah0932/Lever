package com.noah.minecraftagent.common.provider;

public interface StreamListener {
    void onToken(String token);

    void onStatus(String status);
}
