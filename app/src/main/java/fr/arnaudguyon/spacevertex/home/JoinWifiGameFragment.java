/*
 * Copyright (c) 2016–2025 Arnaud GUYON
 * This source code is licensed under the MIT License.
 * See LICENSE file for details.
 */
package fr.arnaudguyon.spacevertex.home;

import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import fr.arnaudguyon.spacevertex.R;
import fr.arnaudguyon.spacevertex.network.GameConnection;
import fr.arnaudguyon.spacevertex.network.UDPDiscover;
import fr.arnaudguyon.spacevertex.network.WebSocketHelper;
import fr.arnaudguyon.spacevertex.network.WifiHelper;

public class JoinWifiGameFragment extends Fragment {

    private final String TAG = getClass().getName();
    private View mView;
    private String mServerIpWifiDirect;
    private final @NonNull Handler mHandler = new Handler();

    public void setServerIpWifiDirect(String serverIpWifiDirect) {
        mServerIpWifiDirect = serverIpWifiDirect;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.join_wifi_game_fragment, container, false);
        return mView;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final View loadingView = view.findViewById(R.id.loadingView);
        loadingView.setVisibility(View.GONE);
        final View contentView = view.findViewById(R.id.contentView);
        contentView.setVisibility(View.VISIBLE);

        if (mServerIpWifiDirect != null) {
            view.findViewById(R.id.noticeWifi).setVisibility(View.GONE);
            int serverPort = WifiHelper.getWifiDirectPort();
            createWifiDirectClientServer(serverPort);
        } else {
            UDPDiscover.getInstance().startSearchServer((serverIp, serverPort) -> mHandler.post(() -> {
                createWifiClientServer(serverIp, serverPort);
                Log.i(TAG, "onServerFound UDP " + serverIp + ":" + serverPort);
            }));
        }

    }

    private void createWifiDirectClientServer(int serverPort) {
        final HomeActivity activity = (HomeActivity) getActivity();
        if ((activity != null) && (mServerIpWifiDirect != null)) {
            displayLoading();
            GameConnection gameConnection = new WebSocketHelper.SocketClient(mServerIpWifiDirect, serverPort);
            activity.onGameConnectionCreated(gameConnection);
        }
    }

    private void createWifiClientServer(String serverIp, int serverPort) {
        Log.i(TAG, "createWifiClientServer " + serverIp + ":" + serverPort);
        final HomeActivity activity = (HomeActivity) getActivity();
        if (activity != null) {
            displayLoading();
            GameConnection gameConnection = new WebSocketHelper.SocketClient(serverIp, serverPort);
            activity.onGameConnectionCreated(gameConnection);
        }
    }

    private void displayLoading() {
        final View loadingView = mView.findViewById(R.id.loadingView);
        loadingView.setVisibility(View.VISIBLE);
        final View contentView = mView.findViewById(R.id.contentView);
        contentView.setVisibility(View.GONE);
    }

}
