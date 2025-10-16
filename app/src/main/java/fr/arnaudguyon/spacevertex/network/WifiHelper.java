/*
 * Copyright (c) 2016–2025 Arnaud GUYON
 * This source code is licensed under the MIT License.
 * See LICENSE file for details.
 */
package fr.arnaudguyon.spacevertex.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.text.format.Formatter;

import java.io.IOException;
import java.net.ServerSocket;

public class WifiHelper {

    public static void disableWifi(final Context context) {
        WifiManager wifiManager =(WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            wifiManager.setWifiEnabled(false);
        }
    }
    public static void enableWifi(final Context context, final WifiListener listener) {
        WifiManager wifiManager =(WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            if (wifiManager.isWifiEnabled()) {
                if (listener != null) {
                    listener.onWifiActivated();
                }
            } else {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
                BroadcastReceiver mWifiReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (context != null) {
                            final String action = intent.getAction();
                            if (TextUtils.equals(action, WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                                boolean connected = (networkInfo.getState() == NetworkInfo.State.CONNECTED);
                                if (connected) {
                                    context.unregisterReceiver(this);
                                    if (listener != null) {
                                        listener.onWifiActivated();
                                    }
                                } else {
                                    if (listener != null) {
                                        listener.onWifiDeactivated();
                                    }
                                }
                            }
                        }
                    }
                };
                context.registerReceiver(mWifiReceiver, intentFilter);
                wifiManager.setWifiEnabled(true);
            }
        }
    }

    public static int getIpInt(Context context) {
        WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        return (wm != null) ? wm.getConnectionInfo().getIpAddress() : 0;
    }
    public static String getIpString(Context context) {
        int ipAddress = getIpInt(context);
        return Formatter.formatIpAddress(ipAddress);
    }

    public static int getWifiDirectPort() {
        return 8888;
    }
    public static int getFreePort() {
        try {
            ServerSocket socket = new ServerSocket(0);
            int port = socket.getLocalPort();
            socket.close();
            return port;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public interface WifiListener {
        void onWifiActivated();
        void onWifiDeactivated();
    }
}
