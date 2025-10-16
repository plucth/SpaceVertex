/*
 * Copyright (c) 2016–2025 Arnaud GUYON
 * This source code is licensed under the MIT License.
 * See LICENSE file for details.
 */
package fr.arnaudguyon.spacevertex.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Collection;

public class WifiDirectBroadcastReceiver extends BroadcastReceiver implements WifiP2pManager.PeerListListener, WifiP2pManager.ConnectionInfoListener {

    private static final String TAG = "WifiBroadcastReceiver";

    private final @NonNull WifiP2pManager mManager;
    private final @NonNull WifiP2pManager.Channel mChannel;
    private final @NonNull WifiListener mListener;
    private boolean mIsConnected = false;

    public WifiDirectBroadcastReceiver(@NonNull WifiP2pManager manager, @NonNull WifiP2pManager.Channel channel, @NonNull WifiListener listener) {
        mManager = manager;
        mChannel = channel;
        mListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
//            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
//            mListener.setIsWifiP2pEnabled(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED);
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // The peer list has changed!
            Log.d(TAG, "Peer list has changed");
            mManager.requestPeers(mChannel, this);
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Connection state changed!  We should probably do something about that.
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            Log.d(TAG, "Connection state changed: " + networkInfo.getState());
            if (networkInfo.isConnected()) {
                // We are connected with the other device, request connection info to find group owner IP
                mManager.requestConnectionInfo(mChannel, this);
                if (!mIsConnected) {
                    mIsConnected = true;
                    mListener.onConnectionChanged(true);
                }
            } else {
                if (networkInfo.getState() == NetworkInfo.State.DISCONNECTED) {
                    if (mIsConnected) {
                        mIsConnected = false;
                        mListener.onConnectionChanged(false);
                    }
                }
            }


        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            Log.d(TAG, "this device changed action " + device);
        }
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {

        Collection<WifiP2pDevice> peers = peerList.getDeviceList();
        Log.d(TAG, "onPeersAvailable, " + peers.size() + " devices");
        for (WifiP2pDevice peer : peers) {
            Log.d(TAG, "Device " + peer.deviceName + ", " + peer.deviceAddress);
            mListener.onNewPeer(peer);
        }
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {

        Log.d(TAG, "onConnectionInfoAvailable " + info);
        mListener.onConnectionInfoAvailable(info);
    }

    public interface WifiListener {
        void onConnectionInfoAvailable(WifiP2pInfo info);

        void onNewPeer(WifiP2pDevice peer);

        void onConnectionChanged(boolean connected);
    }
}
