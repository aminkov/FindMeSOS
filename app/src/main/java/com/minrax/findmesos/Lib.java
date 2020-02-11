package com.minrax.findmesos;

import android.content.SharedPreferences;
import android.media.MediaPlayer;
import androidx.appcompat.app.AppCompatActivity;

public class Lib extends AppCompatActivity {


    protected String getPreferenceValue(String key) {
        SharedPreferences settings = getSharedPreferences("Settings",0);
        return settings.getString(key,"");
    }

    public void playSoundIfOn() {
        if (readABooleanPreference("soundStatus")) {
            final MediaPlayer mp = MediaPlayer.create(this, R.raw.s3);
            mp.start();
        }
    }

    public void writeToPreference(String key, String thePreference) {
        SharedPreferences.Editor editor = this.getSharedPreferences("Settings", 0).edit();
        editor.putString(key, thePreference);
        editor.commit();
    }

    public boolean readABooleanPreference(String key) {
        SharedPreferences settings = getSharedPreferences("Settings",0);
        if (settings.contains(key)) {
            return (settings.getBoolean(key, false));
        } else {
            return false;
        }
    }

    public void writeABooleanPreference(String key, Boolean thePreference) {
        SharedPreferences.Editor editor = this.getSharedPreferences("Settings", 0).edit();
        editor.putBoolean(key, thePreference);
        editor.commit();
    }

}
