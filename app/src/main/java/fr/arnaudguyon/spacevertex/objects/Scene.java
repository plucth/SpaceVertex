/*
 * Copyright (c) 2016–2025 Arnaud GUYON
 * This source code is licensed under the MIT License.
 * See LICENSE file for details.
 */
package fr.arnaudguyon.spacevertex.objects;

import android.content.Context;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

import java.util.ArrayList;

import fr.arnaudguyon.spacevertex.network.GameConnection;
import fr.arnaudguyon.spacevertex.network.PackMsg;

public class Scene extends AppCompatImageView {

    private static final String TAG = "Scene";

    private static final long FRAME_DURATION_MS = 16;
    private static final int BACKGROUND_COLOR = 0xFF000015;
    private static final float NEAR_MOUSE_DISTANCE = 400;
    private static final float FAR_MOUSE_DISTANCE = 1000;

    private ShipLocal mLocalShip;
    private ShipRemote mRemoteShip;
    private SceneListener mListener;
    private SceneReadyListener mSceneReadyListener;
    private long mPreviousDraw;
    private final @NonNull ArrayList<SpaceObject> mObjects = new ArrayList<>();
    private final @NonNull ArrayList<SpaceObject> mKillingObjects = new ArrayList<>();

    private long mStartGameDate = 0;
    private int mChrono = 0;
    private int mFrameNumberToSend = 0;
    private GameConnection gameConnection;
    private final ArrayList<PackMsg> mLastReceivedMessages = new ArrayList<>();
    private long scoreUpdateDate = 0;

    public Scene(Context context) {
        super(context);
        mStartGameDate = SystemClock.uptimeMillis();
    }

    public Scene(Context context, AttributeSet attrs) {
        super(context, attrs);
        mStartGameDate = SystemClock.uptimeMillis();
    }

    public Scene(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mStartGameDate = SystemClock.uptimeMillis();
    }

    public void setGameConnection(GameConnection gameConnection) {
        this.gameConnection = gameConnection;
    }

    public void setLocalShip(ShipLocal ship) {
        mLocalShip = ship;
        addObject(ship);
    }

    public void removeLocalShip() {
        if (mLocalShip != null) {
            removeObject(mLocalShip);
            mLocalShip = null;
        }
    }

    public void removeRemoteShip() {
        if (mRemoteShip != null) {
            removeObject(mRemoteShip);
            mRemoteShip = null;
        }
    }

    public ShipLocal getLocalShip() {
        return mLocalShip;
    }

    // TODO: check called once
    public void startRendering() {
        startThread();
    }

    public long getElapsedTime() {
        long result = SystemClock.uptimeMillis() - mStartGameDate;
        return (result > 0) ? result : 0;
    }

    public void setRemoteShip(ShipRemote ship) {
        mRemoteShip = ship;
    }

    public ShipRemote getRemoteShip() {
        return mRemoteShip;
    }

    public int getFrameNumberToSend() {
        return mFrameNumberToSend;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        super.draw(canvas);

        if (isInEditMode()) {
            canvas.drawColor(0x000055);
            return;
        }

        final long now = SystemClock.uptimeMillis();
        final long frameDuration = (mPreviousDraw == 0) ? 16 : now - mPreviousDraw;
        mPreviousDraw = now;

        if (scoreUpdateDate == 0) {
            scoreUpdateDate = now;
        } else {
            // compute distance between ships
            if ((mLocalShip != null) && (mRemoteShip != null)) {
                float diffX = mLocalShip.getPosX() - mRemoteShip.getPosX();
                float diffY = mLocalShip.getPosY() - mRemoteShip.getPosY();
                float dist2 = (diffX * diffX) + (diffY * diffY);
                long refresh = 1000;
                if (dist2 < NEAR_MOUSE_DISTANCE * NEAR_MOUSE_DISTANCE) {
                    refresh = 500;
                } else if (dist2 > FAR_MOUSE_DISTANCE * FAR_MOUSE_DISTANCE) {
                    refresh = 2000;
                }
                if (now - scoreUpdateDate > refresh) {  // Time to update Score !
                    scoreUpdateDate = 0;
                    if (mListener != null) {
                        mListener.increaseMouseScore();
                    }
                }
            }
        }

        int chrono = (int) ((now - mStartGameDate) / 1000);
        if (chrono != mChrono) {
            mChrono = chrono;
            if (mListener != null) {
                mListener.onChronoChanged(chrono);
            }
        }

        canvas.drawColor(BACKGROUND_COLOR);

        // Prepare Scene Information to send to network...
        if (gameConnection != null) {
            ++mFrameNumberToSend;
        }

        // PreDraw Loop
        for (int i = 0; i < mObjects.size(); ++i) {
            SpaceObject object = mObjects.get(i);
            object.preDraw(this, frameDuration);
            if (object.isToBeDestroyed()) {
                mObjects.remove(i);
                synchronized (mKillingObjects) {
                    mKillingObjects.remove(object);
                }
                --i;
            } else {
                if (gameConnection != null) {       // get local information for network
                    PackMsg packMsg = object.prepareNetworkMessage(this, mFrameNumberToSend);
                    if (packMsg != null) {
                        gameConnection.sendMessage(packMsg);
                    }
                }
            }
        }

        synchronized (mLastReceivedMessages) {
            for(PackMsg messageReceived : mLastReceivedMessages) {
                for (int i = 0; i < mObjects.size(); ++i) {
                    SpaceObject object = mObjects.get(i);
                    object.onNetworkMessageReceived(this, messageReceived);
                }
            }
            mLastReceivedMessages.clear();
        }


        // Draw Loop
        float xShip = 0;
        float yShip = 0;
        if (mLocalShip != null) {
            xShip = mLocalShip.getPosX();
            yShip = mLocalShip.getPosY();
        }
        for (int i = 0; i < mObjects.size(); ++i) {
            SpaceObject object = mObjects.get(i);
            object.draw(canvas, xShip, yShip);
        }
    }

    private void startThread() {
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(FRAME_DURATION_MS);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Scene Thread error " + e.getMessage());
                }
                postInvalidate();
            }
        });
        thread.start();
    }

    public void addObject(SpaceObject object) {
        object.prepare(this, getWidth(), getHeight());
        mObjects.add(object);
        if (object.canKill()) {
            synchronized (mKillingObjects) {
                mKillingObjects.add(object);
            }
        }
    }

    public void removeAllObjects() {
        mObjects.clear();
    }

    private void removeObject(SpaceObject object) {
        mObjects.remove(object);
    }

    public ArrayList<SpaceObject> getKillersCopy() {
        final ArrayList<SpaceObject> copy;
        synchronized (mKillingObjects) {
            copy = new ArrayList<>(mKillingObjects);
        }
        return copy;
    }

    public void setSceneReadyListener(SceneReadyListener listener) {
        mSceneReadyListener = listener;
    }

    public void setListener(SceneListener listener) {
        mListener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (isInEditMode()) {
            return;
        }

        if (mSceneReadyListener != null) {
            mSceneReadyListener.onSceneReady(this);
        }
    }

    public void invertRoles() {

        synchronized (mKillingObjects) {
            mKillingObjects.clear();    // No Bullets can kill when switching
        }

        Ship.ShipType localType = mLocalShip.getShipType();
        if ((localType == Ship.ShipType.CAT) || (localType == Ship.ShipType.CAT_NEUTRAL)) {
            mLocalShip.setShipType(Ship.ShipType.MOUSE);
            mRemoteShip.setShipType(Ship.ShipType.CAT);
        } else {
            mLocalShip.setShipType(Ship.ShipType.CAT);
            mRemoteShip.setShipType(Ship.ShipType.MOUSE);
        }
        if (mListener != null) {
            mListener.onRolesHaveChanged();
        }
    }

    public void onMessageReceived(PackMsg packMsg) {
        synchronized (mLastReceivedMessages) {
            mLastReceivedMessages.add(packMsg);
        }
    }

    public void sendMessage(PackMsg message) {
        if (gameConnection != null) {
            gameConnection.sendMessage(message);
        }
    }

    public boolean isGameOver() {
        if (mListener != null) {
            return mListener.isGameOver();
        } else {
            return false;
        }
    }

    public void setNeutralShipType() {
        if (mLocalShip != null) {
            mLocalShip.setNeutralShipType();
        }
        if (mRemoteShip != null) {
            mRemoteShip.setNeutralShipType();
        }
    }

    public interface SceneListener {
        void onChronoChanged(int chrono);

        void increaseMouseScore();

        void onRolesHaveChanged();

        void sendMessage(PackMsg message);

        boolean isGameOver();
    }

    public interface SceneReadyListener {
        void onSceneReady(Scene scene);
    }


}
