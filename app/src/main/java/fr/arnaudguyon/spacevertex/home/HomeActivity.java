/*
 * Copyright (c) 2016–2025 Arnaud GUYON
 * This source code is licensed under the MIT License.
 * See LICENSE file for details.
 */
package fr.arnaudguyon.spacevertex.home;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import fr.arnaudguyon.spacevertex.BuildConfig;
import fr.arnaudguyon.spacevertex.GameFragment;
import fr.arnaudguyon.spacevertex.Prefs;
import fr.arnaudguyon.spacevertex.R;
import fr.arnaudguyon.spacevertex.network.GameConnection;
import fr.arnaudguyon.spacevertex.network.GameDevice;
import fr.arnaudguyon.spacevertex.network.PackMsg;
import fr.arnaudguyon.spacevertex.network.UDPDiscover;
import fr.arnaudguyon.spacevertex.network.WebSocketHelper;
import fr.arnaudguyon.spacevertex.objects.Scene;
import fr.arnaudguyon.spacevertex.objects.StarFieldMenu;
import fr.arnaudguyon.spacevertex.network.WifiHelper;
import fr.arnaudguyon.spacevertex.sound.Music;

public class HomeActivity extends AppCompatActivity implements HomeFragment.HomeListener, GameConnection.MessageListener, GameFragment.GameFragmentListener, GameConnection.PlayerConnectionListener {

    private final String TAG = getClass().getName();
    private Scene mScene;
    private final Handler mHandler = new Handler();
    private GameConnection gameConnection;
    private StarFieldMenu mStarFieldBack;
    private StarFieldMenu mStarFieldFront;
    private boolean mClosingConnections = false;
    private WifiP2pManager.Channel mWifiDirectChannel;
    private Music mMusic;
    private GameDevice otherPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.home_activity);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Disable IPV6 (bugs with WebSocket library)
        java.lang.System.setProperty("java.net.preferIPv6Addresses", "false");
        java.lang.System.setProperty("java.net.preferIPv4Stack", "true");

        // SCENE
        mScene = (Scene) findViewById(R.id.sceneView);
        mScene.setSceneReadyListener(scene -> {

            mScene.removeAllObjects();

            // Back Starfield
            if (mStarFieldBack == null) {
                mStarFieldBack = new StarFieldMenu(80, false);
            }
            mScene.addObject(mStarFieldBack);

            // Front Starfield
            if (mStarFieldFront == null) {
                mStarFieldFront = new StarFieldMenu(20, true);
            }
            mScene.addObject(mStarFieldFront);

            mScene.startRendering();
        });

        UDPDiscover.getInstance().acquireMulticast(this);

        pushHomeFragment();
    }

    @Override
    protected void onDestroy() {

        UDPDiscover.getInstance().releaseMulticast();

        if (mMusic != null) {
            mMusic.release();
        }
        super.onDestroy();
    }

    private void pushHomeFragment() {
        HomeFragment fragment = new HomeFragment();
        fragment.setListener(this);
        pushFragment(fragment, false);
    }

    private void pushFragment(Fragment fragment, boolean immersiveMode) {
        if (immersiveMode) {
            enterImmersiveMode();
        } else {
            exitImmersiveMode();
        }
        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction().replace(R.id.fragmentHolder, fragment).commit();
    }

    @Override
    protected void onPause() {
        if (mMusic != null) {
            mMusic.onPause();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mMusic == null) {
            boolean musicOn = Prefs.isMusicEnabled(this);
            mMusic = new Music(this, musicOn);
        } else {
            mMusic.onResume();
        }

        WifiHelper.enableWifi(this, new WifiHelper.WifiListener() {
            @Override
            public void onWifiActivated() {
                Log.d(TAG, "Wifi enabled");
            }

            @Override
            public void onWifiDeactivated() {
                Log.d(TAG, "Wifi disabled");
            }
        });
    }

    public void changeMusicState(boolean musicOn) {
        if (mMusic != null) {
            mMusic.setMusicOn(musicOn);
        }
    }

    @Override
    public void onJoinWifiGameClicked() {
        JoinWifiGameFragment fragment = new JoinWifiGameFragment();
        pushFragment(fragment, false);
    }

    @Override
    public void onJoinWifiDirectGameClicked(String wifiDirectServerIp, WifiP2pManager.Channel wifiDirectChannel) {
        if (wifiDirectServerIp != null) {
            addDebugText("Connected as Wifi-Direct Client to Server at " + wifiDirectServerIp, false);
        }
        mWifiDirectChannel = wifiDirectChannel;
        JoinWifiGameFragment fragment = new JoinWifiGameFragment();
        fragment.setServerIpWifiDirect(wifiDirectServerIp);
        pushFragment(fragment, false);
    }

    @Override
    public void onCreateWifiGameClicked() {
        CreateWifiGameFragment fragment = new CreateWifiGameFragment();
        pushFragment(fragment, false);
    }

    @Override
    public void onCreateWifiDirectGameClicked(@Nullable String wifiDirectServerIp, @Nullable WifiP2pManager.Channel wifiDirectChannel) {
        if (wifiDirectServerIp != null) {
            addDebugText("Connected as Wifi-Direct Server at " + wifiDirectServerIp, false);
        }
        mWifiDirectChannel = wifiDirectChannel;
        CreateWifiGameFragment fragment = new CreateWifiGameFragment();
        fragment.setServerIpWifiDirect(wifiDirectServerIp);
        pushFragment(fragment, false);
    }

    @Override
    public void onWifiDirectClicked() {
        if (getSystemService(Context.WIFI_P2P_SERVICE) != null) {
            WifiDirectFragment fragment = new WifiDirectFragment();
            pushFragment(fragment, false);
        } else {
            Toast.makeText(this, R.string.err_p2p, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onGameConnectionCreated(@NonNull GameConnection connection) {
        this.gameConnection = connection;
        connection.registerConnectionListener(this);
        if (connection.isServer()) {
            addDebugText("Server created", false);
            connection.addMessageListener(PackMsg.MsgType.NETWORK_VERSION, this);
            connection.addMessageListener(PackMsg.MsgType.QUITTING, this);
        } else {
            addDebugText("Client created", false);
            connection.addMessageListener(PackMsg.MsgType.SESSION_FULL, this);
            connection.addMessageListener(PackMsg.MsgType.WRONG_NETWORK_VERSION, this);
            connection.addMessageListener(PackMsg.MsgType.START_GAME, this);
            connection.addMessageListener(PackMsg.MsgType.QUITTING, this);
        }
    }

    @Override
    public void onPlayerJoined(@NonNull GameConnection gameConnection, @NonNull GameDevice device) {
        addDebugText("Player " + device + " joined", false);
        runOnUiThread(() -> {
            if (gameConnection.isServer()) {
                // Do nothing, wait for NetworkVersion so that we are sure his connection is ready to speak
            } else {
                PackMsg.NetworkVersion networkVersion = new PackMsg.NetworkVersion(WebSocketHelper.getNetworkVersion(), device);
                sendMessage(networkVersion);
            }
        });
    }

    @Override
    public void onPlayerLeft(@NonNull GameConnection gameConnection, @NonNull GameDevice device) {
        addDebugText("Player " + device + " left", false);
        runOnUiThread(() -> {
            Log.i(TAG, gameConnection + " left the game");
            if (device.equalsTo(otherPlayer)) {
                closeConnections();
                pushHomeFragment();
            }
        });
    }

    @Override
    public void onConnectionError(@NonNull GameConnection gameConnection, @NonNull GameDevice device, @Nullable String errorMessage) {
//    public void onWebSocketError(WebSocketHelper wsHelper, WebSocket wsConnection, Exception exception) {
        addDebugText("Connection Error: " + errorMessage, true);
        runOnUiThread(() -> {
            Toast.makeText(HomeActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            if (!isFinishing()) {
                pushHomeFragment();
            }
        });
    }

    @Override
    public void onMessageReceived(@NonNull GameConnection someGameConnection, @NonNull PackMsg packMsg) {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        PackMsg.MsgType type = packMsg.getType();
        addDebugText("Receive " + type.name(), false);
        switch (type) {
            case NETWORK_VERSION:
                checkNetworkVersion((PackMsg.NetworkVersion) packMsg);
                break;
            case WRONG_NETWORK_VERSION:
                displayWrongVersion();
                break;
            case SESSION_FULL:
                sessionFull();
                break;
            case START_GAME:
                startGame((PackMsg.StartGame) packMsg);
                break;
            case QUITTING:
                closeConnections();
                runOnUiThread(() -> pushHomeFragment());
                break;
        }

    }

    // we are Server side here
    private void checkNetworkVersion(PackMsg.NetworkVersion receivedMsg) {
        GameDevice otherDevice = receivedMsg.getDevice();
        if (receivedMsg.version != WebSocketHelper.getNetworkVersion()) {
            sendMessage(new PackMsg.WrongNetworkVersion(otherDevice));
        } else {
            // Correct version Number
            if ((otherPlayer == null) || (otherPlayer.equalsTo(otherDevice))) {
                // Let's Play!
                otherPlayer = otherDevice;
                sendMessage(new PackMsg.StartGame(otherDevice));
                startToPlay(gameConnection);
                UDPDiscover.getInstance().stopBroadcast();
            } else {
                // Full!
                sendMessage(new PackMsg.SessionFull(otherDevice));
            }
        }
    }

    private void displayWrongVersion() {
        AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
        builder.setMessage(R.string.err_wrong_network_version);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.ok_button, (dialog, which) -> onBackPressed());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void sessionFull() {
        addDebugText("Session is Full", true);
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
            builder.setMessage(R.string.err_session_full);
            builder.setCancelable(false);
            builder.setPositiveButton(R.string.ok_button, (dialog, which) -> onBackPressed());
            AlertDialog dialog = builder.create();
            dialog.show();
        });
    }

    // we are client side
    private void startGame(PackMsg.StartGame packMsg) {
        otherPlayer = packMsg.getDevice();
        startToPlay(gameConnection);
    }

    private void startToPlay(final GameConnection gameConnection) {

        if (otherPlayer == null) {
            addDebugText("startToPlay no other player", true);
        }
//        if (!mScene.setOtherPlayer(otherPlayer)) {
//            addDebugText("startToPlay Scene has no other player", true);
//        }

        runOnUiThread(() -> {
            mStarFieldFront.scrollStars(false, 0.3f);
            mStarFieldBack.scrollStars(false, 0.3f);
            GameFragment fragment = new GameFragment();
            fragment.initGameInformation(mScene, gameConnection, otherPlayer, HomeActivity.this);
            pushFragment(fragment, true);
        });

    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragmentHolder);
        if (fragment != null) {
            if (fragment instanceof HomeFragment) {
                super.onBackPressed();
            } else if (fragment instanceof GameFragment) {
                onQuitGameFragment();
            } else if (fragment instanceof CreateWifiGameFragment) {
                UDPDiscover.getInstance().stopBroadcast();
                pushHomeFragment();
            } else if (fragment instanceof JoinWifiGameFragment) {
                UDPDiscover.getInstance().stopSearchServer();
                pushHomeFragment();
            } else {
                pushHomeFragment();
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onQuitGameFragment() {
        PackMsg.QuitGame quitMessage = new PackMsg.QuitGame(otherPlayer);
        gameConnection.sendMessage(quitMessage);
        mHandler.postDelayed(() -> {
            pushHomeFragment();
            closeConnections();
        }, 250);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void sendMessage(PackMsg message) {
        if (gameConnection != null) {
            gameConnection.sendMessage(message);
        }
    }

    private void closeConnections() {
        mClosingConnections = true;

        // TODO: move this code into WifiConnection
        if (mWifiDirectChannel != null) {
            WifiP2pManager wifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
            if (wifiP2pManager != null) {
                wifiP2pManager.removeGroup(mWifiDirectChannel, null);
            }
            mWifiDirectChannel = null;
        }

        if (gameConnection != null) {
            gameConnection.unregisterConnectionListener();
            gameConnection.removeMessageListeners();
            gameConnection.disconnect();
            gameConnection = null;
        }

        mClosingConnections = false;
        otherPlayer = null;
        mScene.removeLocalShip();
        mScene.removeRemoteShip();
        onGameOver(true);
    }

    public void onGameOver(final boolean gameOver) {
        mStarFieldFront.scrollStars(gameOver, 1);
        mStarFieldBack.scrollStars(gameOver, 1);
    }

    public void addDebugText(final String text, final boolean isError) {
        if (isError) {
            Log.e(TAG, text);
        } else {
            Log.i(TAG, text);
        }
        runOnUiThread(() -> {
            if (BuildConfig.DEBUG_NETWORK) {
                TextView textView = new TextView(HomeActivity.this);
                textView.setTextColor(isError ? 0xFFff4444 : 0xFF44ff44);
                textView.setText(text);
                LinearLayout debutNetworkLayout = findViewById(R.id.debugNetworkLayout);
                if (debutNetworkLayout != null) {
                    debutNetworkLayout.addView(textView);
                    if (debutNetworkLayout.getChildCount() > 3) {
                        debutNetworkLayout.removeViewAt(0);
                    }
                }
            }
        });
    }

    private void enterImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_FULLSCREEN
        );
    }

    private void exitImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }
}
