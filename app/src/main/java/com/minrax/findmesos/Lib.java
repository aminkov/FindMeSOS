package com.minrax.findmesos;

import android.content.SharedPreferences;
import android.media.MediaPlayer;
import androidx.appcompat.app.AppCompatActivity;

public class Lib extends AppCompatActivity {

    public void playSoundIfOn() {
        if (readABooleanPreference("soundStatus")) {
            final MediaPlayer mp = MediaPlayer.create(this, R.raw.s3);
            mp.start();
        }
    }

    public String getPreferenceValue(String key) {
        SharedPreferences settings = getSharedPreferences("SettingsNew",0);
        return settings.getString(key,"");
    }

    public void writeToPreference(String key, String thePreference) {
        SharedPreferences.Editor editor = this.getSharedPreferences("SettingsNew", 0).edit();
        editor.putString(key, thePreference);
        editor.apply();
    }

    public boolean readABooleanPreference(String key) {
        SharedPreferences settings = getSharedPreferences("SettingsNew",0);
        if (settings.contains(key)) {
            return (settings.getBoolean(key, false));
        } else {
            return false;
        }
    }

    public void writeABooleanPreference(String key, Boolean thePreference) {
        SharedPreferences.Editor editor = this.getSharedPreferences("SettingsNew", 0).edit();
        editor.putBoolean(key, thePreference);
        editor.apply();
    }

}
