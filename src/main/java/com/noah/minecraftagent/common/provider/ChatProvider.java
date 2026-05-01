package com.noah.minecraftagent.common.provider;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public interface ChatProvider {
    CompletableFuture<ChatResponse> complete(ChatRequest request, StreamListener listener, AtomicBoolean cancelled, Executor executor);
}
