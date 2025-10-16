/*
 * Copyright (c) 2016–2025 Arnaud GUYON
 * This source code is licensed under the MIT License.
 * See LICENSE file for details.
 */
package fr.arnaudguyon.spacevertex.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;

import fr.arnaudguyon.perm.Perm;
import fr.arnaudguyon.perm.PermResult;
import fr.arnaudguyon.spacevertex.R;
import fr.arnaudguyon.spacevertex.network.GameConnection;
import fr.arnaudguyon.spacevertex.network.WebSocketHelper;
import fr.arnaudguyon.spacevertex.network.WifiDirectBroadcastReceiver;
import fr.arnaudguyon.spacevertex.network.WifiHelper;

public class WifiDirectFragment extends Fragment implements WifiDirectBroadcastReceiver.WifiListener {

    private final String TAG = getClass().getName();
    private final String[] PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    private final String[] PERMISSIONS_13 = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES};

    private final int REQ_PERMISSIONS_CODE = 1;

    private View mView;

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private WifiDirectBroadcastReceiver mReceiver;
    private WifiP2pInfo mConnectionInfo;
    private WifiP2pGroup mGroup;
    private final @NonNull ArrayList<WifiP2pDevice> mPeers = new ArrayList<>();    // Use ArrayList instead of HashMap to preserve Order
    private WifiP2pDevice mClickedDevice;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.wifi_direct_fragment, container, false);
        return mView;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Perm perm = new Perm(this, getPermissionList());
        if (perm.areGranted()) {
            startWithPermission();
        } else {
            perm.askPermissions(REQ_PERMISSIONS_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_PERMISSIONS_CODE) {
            PermResult permResult = new PermResult(permissions, grantResults);
            if (permResult.areGranted()) {
                startWithPermission();
            } else {
                Toast.makeText(getActivity(), R.string.err_wifi_direct_permission, Toast.LENGTH_LONG).show();
                getActivity().finish();
            }
        }
    }

    private void startWithPermission() {
        final Context context = mView.getContext();

        mManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        if (mManager != null) {
            mChannel = mManager.initialize(context, context.getMainLooper(), () -> Log.d(TAG, "onChannelDisconnected"));

            firstSearch();
        } else {
            Toast.makeText(context, R.string.err_p2p, Toast.LENGTH_LONG).show();
            getActivity().finish();
        }
    }

    private void firstSearch() {

        final Context context = getActivity();

        mConnectionInfo = null;
        mGroup = null;
        mPeers.clear();
        refreshList();

        mReceiver = new WifiDirectBroadcastReceiver(mManager, mChannel, this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);       // P2p status changed
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);       // List of available peers changed
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);  // State of connectivity changed
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION); // Device details changed

        context.registerReceiver(mReceiver, intentFilter);

        mManager.requestConnectionInfo(mChannel, new WifiDirectBroadcastReceiver(mManager, mChannel, this));
    }

    @Override
    public void onDestroyView() {

        if (mReceiver != null) {
            final Context context = getActivity();
            context.unregisterReceiver(mReceiver);
        }

        super.onDestroyView();
    }

    private void restartSearch() {

        final Context context = getActivity();

        mConnectionInfo = null;
        mGroup = null;
        mPeers.clear();
        refreshList();

        WifiHelper.enableWifi(context, new WifiHelper.WifiListener() {
            @Override
            public void onWifiActivated() {
                mManager.requestConnectionInfo(mChannel, new WifiDirectBroadcastReceiver(mManager, mChannel, WifiDirectFragment.this));
            }

            @Override
            public void onWifiDeactivated() {
                // TODO: something to do?
            }
        });

    }

    private void refreshList() {

        final Context context = getActivity();
        if (context == null) {
            return;
        }

        LinearLayout peerListView = mView.findViewById(R.id.peerListView);
        peerListView.removeAllViews();
        View searchLayout = mView.findViewById(R.id.searchLayout);

        if (mClickedDevice != null) {
            searchLayout.setVisibility(View.VISIBLE);
            peerListView.setVisibility(View.INVISIBLE);
        } else {
            peerListView.setVisibility(View.VISIBLE);
            if (mPeers.isEmpty()) {
                searchLayout.setVisibility(View.VISIBLE);
            } else {
                searchLayout.setVisibility(View.INVISIBLE);
            }
        }

        for (final WifiP2pDevice device : mPeers) {
            final String deviceName = device.deviceName;
            if (!deviceName.isEmpty()) {

                ViewGroup viewGroup = (ViewGroup) View.inflate(context, R.layout.button_player, null);
                TextView textView = viewGroup.findViewById(R.id.textView);
                String format = getString(R.string.play_against_format);
                String text = String.format(format, deviceName);
                textView.setText(text);

                ImageView imageView = viewGroup.findViewById(R.id.imageView);
                if (isInGroup(device)) {
                    imageView.setImageResource(R.drawable.wifi_on);
                } else {
                    imageView.setImageResource(R.drawable.wifi_off);
                }

                peerListView.addView(viewGroup);

                viewGroup.setOnClickListener(v -> clickOnDevice(device));
            }
        }
    }

    private void addUniquePeer(WifiP2pDevice newPeer) {
        for (int i = 0; i < mPeers.size(); ++i) {
            WifiP2pDevice peer = mPeers.get(i);
            if (peer.deviceAddress.equals(newPeer.deviceAddress)) { // Update same Peer
                mPeers.remove(i);
                mPeers.add(i, newPeer);
                return;
            }
        }
        // Not found, add it to the end
        mPeers.add(newPeer);
    }

    private boolean isInGroup(WifiP2pDevice device) {
        if (mGroup != null) {
            final String deviceAddress = device.deviceAddress;
            if (mGroup.getOwner().deviceAddress.equals(deviceAddress)) {
                return true;
            }
            Collection<WifiP2pDevice> clients = mGroup.getClientList();
            for (WifiP2pDevice client : clients) {
                if (client.deviceAddress.equals(deviceAddress)) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressLint("MissingPermission")
    private void clickOnDevice(final WifiP2pDevice device) {
        mClickedDevice = device;

        // Update Loader message
        final Context context = getActivity();
        TextView loaderMessageView = mView.findViewById(R.id.loaderMessageView);
        String format = context.getString(R.string.wifidirect_connecting_to);
        String text = String.format(format, device.deviceName);
        loaderMessageView.setText(text);
        View searchLayout = mView.findViewById(R.id.searchLayout);
        searchLayout.setVisibility(View.VISIBLE);

        if (isInGroup(device)) {
            createWebSockets();
        } else {
            // Try to connect
            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = device.deviceAddress;
            config.wps.setup = WpsInfo.PBC;

            Perm perm = new Perm(this, getPermissionList());
            if (!perm.areGranted()) {
                perm.askPermissions(REQ_PERMISSIONS_CODE);
                return;
            }
            mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    String msg = "Connection success with " + device.deviceName + " isGroupOwner " + device.isGroupOwner();
                    Log.d(TAG, msg);
                }

                @Override
                public void onFailure(int reason) {
                    Log.d(TAG, "Connection to " + device.deviceName + " failed");
                    mClickedDevice = null;
                    // TODO: send warning on screen
                }
            });
        }
    }

    private void createWebSockets() {
        HomeActivity activity = (HomeActivity) getActivity();
        if ((activity != null) && (mConnectionInfo != null) && (mConnectionInfo.groupOwnerAddress != null)) {
            final boolean isServer = mConnectionInfo.isGroupOwner;
            final String serverIp = mConnectionInfo.groupOwnerAddress.getHostAddress();
            if (isServer) {
                int port = WifiHelper.getWifiDirectPort();
                GameConnection server = new WebSocketHelper.SocketServer(serverIp, port);
                activity.onGameConnectionCreated(server);
            }
        }
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        Log.d(TAG, "onConnectionInfoAvailable: " + info);
        if ((info != null) && info.groupFormed) {

            if (mConnectionInfo != null) {  // Info already received, under treatment
                Log.d(TAG, "ignore same info");
                if (TextUtils.equals(info.groupOwnerAddress.getHostAddress(), mConnectionInfo.groupOwnerAddress.getHostAddress())) {
                    mConnectionInfo = info;
                    refreshList();
                }
                return;
            }

            mConnectionInfo = info;

            String serverIp = mConnectionInfo.groupOwnerAddress.getHostAddress();
            if (info.isGroupOwner) {
                HomeActivity activity = (HomeActivity) getActivity();
                if (activity != null) {
                    activity.onCreateWifiDirectGameClicked(serverIp, mChannel);
                }
            } else {
                Activity activity = getActivity();
                if (activity instanceof HomeFragment.HomeListener) {
                    HomeFragment.HomeListener listener = (HomeFragment.HomeListener) activity;
                    listener.onJoinWifiDirectGameClicked(serverIp, mChannel);
                }
            }
        }

        searchForGroupInfo();
    }

    @Override
    public void onNewPeer(WifiP2pDevice peer) {
        Log.d(TAG, "onNewPeer: " + peer);
        if (!peer.deviceName.isEmpty()) {
            addUniquePeer(peer);
            //mPeers.put(peer.deviceAddress, peer);
        }
        refreshList();

    }

    @Override
    public void onConnectionChanged(boolean connected) {
        Log.d(TAG, "onConnectionChanged connected:" + connected);
        restartSearch();

    }

    @SuppressLint("MissingPermission")
    private void searchForGroupInfo() {
        Log.d(TAG, "searchForGroupInfo");
        Perm perm = new Perm(this, PERMISSIONS);
        if (!perm.areGranted()) {
            perm.askPermissions(REQ_PERMISSIONS_CODE);
            return;
        }
        mManager.requestGroupInfo(mChannel, group -> {
            Log.d(TAG, "onGroupInfoAvailable: " + group);
            mGroup = group;
            if (group != null) {
                Collection<WifiP2pDevice> clients = mGroup.getClientList();
                if (clients.size() > 1) {
                    // Disconnect group, cannot play more than 2 players
                    mGroup = null;
                    mConnectionInfo = null;
                    if (mManager != null) {
                        Log.d(TAG, "too many clients in same group, disconnect from group");
                        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "disconnected from Group");
                                restartSearch();
                            }

                            @Override
                            public void onFailure(int reason) {
                                Log.d(TAG, "disconnect from Group failed");
                                restartSearch();
                            }
                        });
                    } else {
                        restartSearch();
                    }
                    return;
                } else {
                    for (WifiP2pDevice client : clients) {
                        if (!client.deviceName.isEmpty()) {
                            addUniquePeer(client);
                            //mPeers.put(client.deviceAddress, client);
                        }
                    }
                }
            }
            refreshList();
            searchForDevices();
        });
    }

    @SuppressLint("MissingPermission")
    private void searchForDevices() {

        if (!isAdded()) {
            return;
        }

        Log.d(TAG, "searchForDevices");
        refreshList();

        Perm perm = new Perm(this, PERMISSIONS);
        if (!perm.areGranted()) {
            perm.askPermissions(REQ_PERMISSIONS_CODE);
            return;
        }

        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "discoverPeers ok");
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "discoverPeers failed " + reason);
            }
        });
    }

    private String[] getPermissionList() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return PERMISSIONS_13;
        } else {
            return PERMISSIONS;
        }
    }

}
