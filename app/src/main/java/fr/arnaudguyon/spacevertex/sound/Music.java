/*
 * Copyright (c) 2016–2025 Arnaud GUYON
 * This source code is licensed under the MIT License.
 * See LICENSE file for details.
 */
package fr.arnaudguyon.spacevertex.sound;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;

import fr.arnaudguyon.spacevertex.R;

public class Music implements MediaPlayer.OnErrorListener {

    private static final String TAG = "Music";
    private MediaPlayer mMediaPlayer;
    private boolean mMusicOn;

    public Music(Context context, boolean musicOn) {

        mMusicOn = musicOn;

        createMediaPlayer(context, R.raw.fairy128, mp -> {
            mMediaPlayer = mp;
            mMediaPlayer.setLooping(true);
            if (mMusicOn) {
                mMediaPlayer.start();
            }
        });
    }

    private void createMediaPlayer(Context context, int resId, MediaPlayer.OnPreparedListener listener ) {
        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        //mediaPlayer.setDataSource(context.getApplicationContext(), myUri);
        try {
            AssetFileDescriptor afd = context.getResources().openRawResourceFd(resId);
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
        } catch (IOException e) {
            Log.e(TAG, "createMediaPlayer error " + e.getMessage());
        }
        mediaPlayer.setOnPreparedListener(listener);
        mediaPlayer.prepareAsync();
    }

    public void setMusicOn(boolean musicOn) {
        if (mMusicOn != musicOn) {
            mMusicOn = musicOn;
            if (musicOn) {
                onResume();
            } else {
                onPause();
            }
        }
    }

    public void onPause() {
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
            }
        }
    }
    public void onResume() {
        if (mMediaPlayer != null) {
            if (mMusicOn) {
                mMediaPlayer.start();
            }
        }
    }
    public void release() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.w(TAG, "Music MediaPlaye error " + what);
        return false;
    }
}
