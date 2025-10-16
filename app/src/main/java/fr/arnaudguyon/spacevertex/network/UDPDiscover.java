/*
 * Copyright (c) 2016–2025 Arnaud GUYON
 * This source code is licensed under the MIT License.
 * See LICENSE file for details.
 */
package fr.arnaudguyon.spacevertex.network;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.annotation.NonNull;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPDiscover {

    private static final String TAG = UDPDiscover.class.getSimpleName();
    private static final int BROADCAST_PORT = 8889;
    private static final String MESSAGE_HEADER = "SPACE_VERTEX_SERVER";
    private SearchThread searchThread;
    private BroadcastThread broadcastThread;
    private static WifiManager.MulticastLock multicastLock;

    private static final @NonNull UDPDiscover instance = new UDPDiscover();

    private UDPDiscover() {
    }

    @NonNull
    public static UDPDiscover getInstance() {
        return instance;
    }

    private static class BroadcastThread extends Thread {

        private volatile boolean running = true;
        private final @NonNull String serverIp;
        private final int serverPort;

        private BroadcastThread(@NonNull String serverIp, int serverPort) {
            super();
            this.serverIp = serverIp;
            this.serverPort = serverPort;
        }

        public void stopThread() {
            running = false;
        }

        @Override
        public void run() {
            try (DatagramSocket socket = new DatagramSocket()) {
                InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
                String message = MESSAGE_HEADER + ":" + serverIp + ":" + serverPort;
                byte[] buffer = message.getBytes();
                while (running) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, BROADCAST_PORT);
                    socket.send(packet);
                    Log.i(TAG, "Broadcast UDP");
                    Thread.sleep(500);
                }
            } catch (Exception e) {
                Log.e(TAG, "BroadcastThread error " + e.getMessage());
            }
            Log.i(TAG, "End of Broadcast UDP");
        }
    }

    public void startBroadcast(String serverIp, int serverPort) {
        Log.i(TAG, "startBroadcast UDP");

        if (broadcastThread != null) {
            broadcastThread.stopThread();
            broadcastThread = null;
        }
        broadcastThread = new BroadcastThread(serverIp, serverPort);
        broadcastThread.start();
    }

    public void stopBroadcast() {
        Log.i(TAG, "stopBroadcast UDP");
        if (broadcastThread != null) {
            broadcastThread.stopThread();
        }
    }

    private static class SearchThread extends Thread {
        private volatile boolean running = true;

        private final @NonNull SearchThreadListener listener;

        private SearchThread(@NonNull SearchThreadListener listener) {
            super();
            this.listener = listener;
        }

        public void stopThread() {
            running = false;
        }

        @Override
        public void run() {
            DatagramSocket socket = null;
            try {
                socket = new DatagramSocket(BROADCAST_PORT);
                socket.setBroadcast(true);
                byte[] buffer = new byte[256];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                while (running) {
                    socket.receive(packet);
                    String data = new String(packet.getData(), 0, packet.getLength());
                    if (data.startsWith(MESSAGE_HEADER)) {
                        String[] parts = data.split(":");
                        String serverIp = parts[1];
                        int serverPort = Integer.parseInt(parts[2]);
                        listener.onServerFound(serverIp, serverPort);
                        break;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "SearchThread error " + e.getMessage());
            }
            if ((socket != null) && !socket.isClosed()) {
                socket.close();
            }
            running = false;
        }
    }

    public void startSearchServer(@NonNull SearchThreadListener listener) {

        if (searchThread != null) {
            searchThread.stopThread();
            searchThread = null;
        }
        searchThread = new SearchThread(listener);
        searchThread.start();
        Log.i(TAG, "startSearchServer");
    }

    public void stopSearchServer() {
        if (searchThread != null) {
            searchThread.stopThread();
            searchThread = null;
        }
    }

    public interface SearchThreadListener {
        void onServerFound(@NonNull String serverIp, int serverPort);
    }

    public void acquireMulticast(Context context) {
        if (multicastLock == null) {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                multicastLock = wifiManager.createMulticastLock("UDP_Broadcast");
                multicastLock.setReferenceCounted(true);
                multicastLock.acquire();
            }
        }
    }

    public void releaseMulticast() {
        if (multicastLock != null && multicastLock.isHeld()) {
            multicastLock.release();
            multicastLock = null;
        }
    }
}
