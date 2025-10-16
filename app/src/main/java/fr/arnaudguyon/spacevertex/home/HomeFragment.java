/*
 * Copyright (c) 2016–2025 Arnaud GUYON
 * This source code is licensed under the MIT License.
 * See LICENSE file for details.
 */
package fr.arnaudguyon.spacevertex.home;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;


import fr.arnaudguyon.spacevertex.Prefs;
import fr.arnaudguyon.spacevertex.R;
import fr.arnaudguyon.spacevertex.network.WifiHelper;
import fr.arnaudguyon.spacevertex.sound.Sounds;

public class HomeFragment extends Fragment {

    private View mView;
    private HomeListener mListener;

    public void setListener(HomeListener listener) {
        mListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.home_fragment, container, false);
        return mView;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Context context = view.getContext();

        // Create Button
        View createButton = mView.findViewById(R.id.createButton);
        createButton.setOnClickListener(v -> {

            if (WifiHelper.getIpInt(context) == 0) {
                displayWifiProblem();
                return;
            }

            if (mListener != null) {
                mListener.onCreateWifiGameClicked();
            }
        });

        // Join Button
        View joinButton = mView.findViewById(R.id.joinButton);
        joinButton.setOnClickListener(v -> {

            if (WifiHelper.getIpInt(context) == 0) {
                displayWifiProblem();
                return;
            }

            if (mListener != null) {
                mListener.onJoinWifiGameClicked();
            }
        });

        // Direct Connection
        View wifidirectButton = mView.findViewById(R.id.wifidirectButton);
        wifidirectButton.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onWifiDirectClicked();
            }
        });

        // MUSIC
        {
            final ImageView musicButton = view.findViewById(R.id.musicButton);
            updateMusicButton(context, musicButton);
            musicButton.setOnClickListener(v -> {
                boolean isOn = !Prefs.isMusicEnabled(context);
                Prefs.setMusicEnable(context, isOn);
                updateMusicButton(context, musicButton);
                HomeActivity activity = (HomeActivity) getActivity();
                if (activity != null) {
                    activity.changeMusicState(isOn);
                }
            });
        }

        // SOUND
        {
            final ImageView soundButton = view.findViewById(R.id.soundButton);
            updateSoundButton(context, soundButton);
            soundButton.setOnClickListener(v -> {
                boolean isOn = !Prefs.isSoundEnabled(context);
                Prefs.setSoundEnable(context, isOn);
                updateSoundButton(context, soundButton);
                Sounds.getInstance(context).setEnabled(isOn);
            });
        }
        Sounds.getInstance(context).setEnabled(Prefs.isSoundEnabled(context));

    }

    private void updateMusicButton(Context context, ImageView musicButton) {
        boolean musicOn = Prefs.isMusicEnabled(context);
        musicButton.setImageResource(musicOn ? R.drawable.music_on : R.drawable.music_off);
        musicButton.setAlpha(musicOn ? 1 : 0.5f);
    }

    private void updateSoundButton(Context context, ImageView soundButton) {
        boolean soundOn = Prefs.isSoundEnabled(context);
        soundButton.setImageResource(soundOn ? R.drawable.ic_sound_on : R.drawable.ic_sound_off);
        soundButton.setAlpha(soundOn ? 1 : 0.5f);
    }

    private void displayWifiProblem() {
        Toast.makeText(getActivity(), R.string.err_wifi_problem, Toast.LENGTH_SHORT).show();
    }

    public interface HomeListener {
        void onCreateWifiGameClicked();

        void onCreateWifiDirectGameClicked(String wifiDirectServerIp, WifiP2pManager.Channel wifiDirectChannel);

        void onJoinWifiGameClicked();

        void onJoinWifiDirectGameClicked(String wifiDirectServerIp, WifiP2pManager.Channel wifiDirectChannel);

        void onWifiDirectClicked();
    }

}
