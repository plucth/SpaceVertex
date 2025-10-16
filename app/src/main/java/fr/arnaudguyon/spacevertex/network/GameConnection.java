/*
 * Copyright (c) 2016–2025 Arnaud GUYON
 * This source code is licensed under the MIT License.
 * See LICENSE file for details.
 */
package fr.arnaudguyon.spacevertex.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface GameConnection {

    void addMessageListener(@NonNull PackMsg.MsgType type, @NonNull MessageListener listener);
    void removeMessageListeners();

    void sendMessage(@NonNull PackMsg packMsg);

    void registerConnectionListener(@NonNull PlayerConnectionListener listener);
    void unregisterConnectionListener();

    void disconnect();
    boolean isServer();

    interface MessageListener {
        void onMessageReceived(@NonNull GameConnection gameConnection, @NonNull PackMsg packMsg);
    }

    interface PlayerConnectionListener {
        void onGameConnectionCreated(@NonNull GameConnection gameConnection);
        void onPlayerJoined(@NonNull GameConnection gameConnection, @NonNull GameDevice device);
        void onPlayerLeft(@NonNull GameConnection gameConnection, @NonNull GameDevice device);
        void onConnectionError(@NonNull GameConnection gameConnection, @NonNull GameDevice device, @Nullable String errorMessage);
    }

}
