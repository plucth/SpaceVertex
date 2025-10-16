/*
 * Copyright (c) 2016–2025 Arnaud GUYON
 * This source code is licensed under the MIT License.
 * See LICENSE file for details.
 */
package fr.arnaudguyon.spacevertex.network.wifi;

import androidx.annotation.NonNull;

import org.java_websocket.WebSocket;

import fr.arnaudguyon.spacevertex.network.GameDevice;

public class WifiGameDevice extends GameDevice {

    private final @NonNull WebSocket device;

    public WifiGameDevice(@NonNull WebSocket device) {
        this.device = device;
    }

    public @NonNull
    WebSocket getDevice() {
        return device;
    }


    @Override
    public boolean equalsTo(@NonNull GameDevice other) {
        if (other instanceof WifiGameDevice) {
            return device.equals(((WifiGameDevice) other).device);
        }
        return false;
    }

}
