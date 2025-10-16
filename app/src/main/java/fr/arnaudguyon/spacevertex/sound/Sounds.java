/*
 * Copyright (c) 2016–2025 Arnaud GUYON
 * This source code is licensed under the MIT License.
 * See LICENSE file for details.
 */
package fr.arnaudguyon.spacevertex.sound;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import fr.arnaudguyon.spacevertex.R;
import fr.arnaudguyon.spacevertex.objects.Ship;

public class Sounds {

    private static Sounds sInstance;

    private SoundPool mPool;

    private final int mShootId;
    private final int mExplosionId;
    private final int mEngineId;
    private int mEngineThreadId = -1;
    private final int mTurbo;
    private final int mTic;
//    private int mWin;
//    private int mLose;
    private boolean mEnabled = true;

    public static Sounds getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new Sounds(context);
        }
        return sInstance;
    }

    private Sounds(Context context) {
        mPool = new SoundPool(6, AudioManager.STREAM_MUSIC, 0);
        mShootId = mPool.load(context, R.raw.shoot, 1);
        mExplosionId = mPool.load(context, R.raw.explosion, 1);
        mEngineId = mPool.load(context, R.raw.engine, 1);
        mTurbo = mPool.load(context, R.raw.turbo, 1);
        mTic = mPool.load(context, R.raw.tic, 1);
//        mWin = mPool.load(context, R.raw.win, 1);
//        mLose = mPool.load(context, R.raw.lose, 1);
    }

    public void setEnabled(boolean enabled) {
        if (mEnabled != enabled) {
            mEnabled = enabled;
            if (enabled)  {
                playShoot();
            } else {
                if (mEngineThreadId != -1) {
                    mPool.stop(mEngineThreadId);
                    mEngineThreadId = -1;
                }
            }
        }
    }

    public void stopAll() {
        mPool.release();
        mPool = null;
        sInstance = null;
    }

    public void playShoot() {
        if (mEnabled) {
            mPool.play(mShootId, 1, 1, 0, 0, 1);
        }
    }
    public void playRemoteShoot(Ship localShip, Ship remoteShip) {
        if (mEnabled) {
            playRemoteSound(mShootId, localShip, remoteShip);
        }
    }
    public void playExplosion() {
        if (mEnabled) {
            mPool.play(mExplosionId, 1, 1, 0, 0, 1);
        }
    }
    public void playRemoteExplosion(Ship localShip, Ship remoteShip) {
        if (mEnabled) {
            playRemoteSound(mExplosionId, localShip, remoteShip);
        }
    }

    public void playTurbo() {
        if (mEnabled) {
            mPool.play(mTurbo, 1, 1, 0, 0, 1);
        }
    }
    public void playRemoteTurbo(Ship localShip, Ship remoteShip) {
        if (mEnabled) {
            playRemoteSound(mTurbo, localShip, remoteShip);
        }
    }

    public void playTic(boolean isLocal) {
        if (mEnabled) {
            float left = isLocal ? 0.5f : 0.2f;
            float right = isLocal ? 0.2f : 0.5f;
            mPool.play(mTic, left, right, 0, 0, 1);
        }
    }

    public void playWin() {
       // mPool.play(mWin, 1, 1, 0, 0, 1);
    }
    public void playLose() {
        //mPool.play(mLose, 1, 1, 0, 0, 1);
    }

    private void playRemoteSound(int soundId, Ship localShip, Ship remoteShip) {
        float dx = remoteShip.getPosX() - localShip.getPosX();
        float dy = remoteShip.getPosY() - localShip.getPosY();
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        float volume = 1 - (distance / 2000);
        if (volume < 0.2f) {
            volume = 0.2f;
        }

        if (distance == 0) {
            mPool.play(soundId, 1, 1, 0, 0, 1);
        } else {
            float left;
            float right;
            dx /= distance;
            dy = Math.abs(dy / distance);

            if (dx > 0) {
                right = (float) (volume * Math.cos(dx));
                left = (float) (volume * Math.sin(dy));
            } else {
                left = (float) (volume * Math.cos(-dx));
                right = (float) (volume * Math.sin(dy));
            }
            mPool.play(soundId, left, right, 0, 0, 1);
        }
    }

    public void startEngine() {
        if (mEngineThreadId >= 0) {
            return;
        }
        mEngineThreadId = mPool.play(mEngineId, 1, 1, 0, -1, 1);
    }
    public void stopEngine() {
        if (mEngineThreadId >= 0) {
            mPool.pause(mEngineThreadId);
            mEngineThreadId = -1;
        }
    }

}
