/*
 * Copyright (c) 2016–2025 Arnaud GUYON
 * This source code is licensed under the MIT License.
 * See LICENSE file for details.
 */
package fr.arnaudguyon.spacevertex.home;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import fr.arnaudguyon.spacevertex.R;
import fr.arnaudguyon.spacevertex.network.GameConnection;
import fr.arnaudguyon.spacevertex.network.UDPDiscover;
import fr.arnaudguyon.spacevertex.network.WebSocketHelper;
import fr.arnaudguyon.spacevertex.network.WifiHelper;

public class CreateWifiGameFragment extends Fragment {

    private String mServerIpWifiDirect;

    public void setServerIpWifiDirect(String serverIpWifiDirect) {
        mServerIpWifiDirect = serverIpWifiDirect;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.create_wifi_game_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (mServerIpWifiDirect != null) {
            view.findViewById(R.id.noticeWifi).setVisibility(View.GONE);
        }
        startServer();

    }

    private void startServer() {
        int port;
        String serverIp;
        HomeActivity activity = (HomeActivity) getActivity();
        boolean shouldBroadcast = false;
        if (activity != null) {
            if (mServerIpWifiDirect != null) {
                port = WifiHelper.getWifiDirectPort();
                serverIp = mServerIpWifiDirect;
            } else {
                port = WifiHelper.getFreePort();
                serverIp = WifiHelper.getIpString(activity);
                shouldBroadcast = true;
            }
            GameConnection server = new WebSocketHelper.SocketServer(serverIp, port);
            activity.onGameConnectionCreated(server);
            if (shouldBroadcast) {
                UDPDiscover.getInstance().startBroadcast(serverIp, port);
            }
        }
    }

}
