package com.noah.minecraftagent.server.bot;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.Packet;
import net.minecraft.text.Text;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

final class DummyClientConnection extends ClientConnection {

    DummyClientConnection() {
        super(NetworkSide.SERVERBOUND);
    }

    @Override
    public void send(Packet<?> packet) {
        // No-op: no real network behind a server-side Bot.
    }

    @Override
    public void send(Packet<?> packet, net.minecraft.network.PacketCallbacks callbacks) {
        // No-op: silently discard every packet the server attempts to push.
    }

    @Override
    public void disconnect(Text disconnectReason) {
        // No-op: Bot cannot be network-disconnected — don't touch internal state.
    }

    @Override
    public boolean isOpen() {
        // Always report "open" so Vanilla code paths treat the Bot as connected.
        return true;
    }

    @Override
    public SocketAddress getAddress() {
        // Return a dummy address so the ServerPlayNetworkHandler constructor
        // (which may derive ConnectedClientData from it) does not trip null checks.
        return new InetSocketAddress(0);
    }
}
