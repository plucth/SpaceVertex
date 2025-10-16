/*
 * Copyright (c) 2016–2025 Arnaud GUYON
 * This source code is licensed under the MIT License.
 * See LICENSE file for details.
 */
package fr.arnaudguyon.spacevertex;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {

    private static final String DEVICE_NAME = "deviceName";
    private static final String OPTION_MUSIC = "option_music";
    private static final String OPTION_SOUND = "option_sound";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences("SpaceVertex", Context.MODE_PRIVATE);
    }

    public static void setDeviceName(Context context, String name) {
        SharedPreferences prefs = getPrefs(context);
        prefs.edit().putString(DEVICE_NAME, name).apply();
    }
    public static String getDeviceName(Context context) {
        SharedPreferences prefs = getPrefs(context);
        return prefs.getString(DEVICE_NAME, "unknown");
    }

    public static void setMusicEnable(Context context, boolean enable) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putBoolean(OPTION_MUSIC, enable).apply();
    }
    public static boolean isMusicEnabled(Context context) {
        SharedPreferences prefs = getPrefs(context);
        return prefs.getBoolean(OPTION_MUSIC, true);
    }
    public static void setSoundEnable(Context context, boolean enable) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putBoolean(OPTION_SOUND, enable).apply();
    }
    public static boolean isSoundEnabled(Context context) {
        SharedPreferences prefs = getPrefs(context);
        return prefs.getBoolean(OPTION_SOUND, true);
    }

}
