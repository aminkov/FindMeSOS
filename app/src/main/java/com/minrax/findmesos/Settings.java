package com.minrax.findmesos;
import android.media.MediaPlayer;
import android.widget.EditText;
import android.content.SharedPreferences;
import android.content.Intent;
import android.view.View;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

    public class Settings extends AppCompatActivity {

        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_settings);
            loadSavedSettings();
        }

        public void goToMainApp(View view) {
            Intent intent = new Intent(Settings.this, MainActivity.class);
            PalySoundIfOn();
            startActivity(intent);
            saveSettingsValues();
        }

        private String getPreferenceValue(String key) {
            SharedPreferences settings = this.getSharedPreferences("Settings", 0);
            return settings.getString(key, "");
        }

        private void PalySoundIfOn() {
            if (getPreferenceValue("soundStatus") == "true") {
                final MediaPlayer mp = MediaPlayer.create(this, R.raw.s3);
                mp.start();
            }
        }

        private void writeToPreference(String key, String thePreference) {
            SharedPreferences.Editor editor = this.getSharedPreferences("Settings", 0).edit();
            editor.putString(key, thePreference);
            editor.commit();
        }

        private void saveSettingsValues() {
            //saving default phone numbers
            EditText phone1 = findViewById(R.id.phonenumber1);
            EditText phone2 = findViewById(R.id.phonenumber2);
            //EditText phone3 = findViewById(R.id.phonenumber3);
            String strphone1 = phone1.getText().toString();
            String strphone2 = phone2.getText().toString();
            //String strphone3 = phone3.getText().toString();
            writeToPreference("p1", strphone1);
            writeToPreference("p2", strphone2);
            //writeToPreference("p3", strphone3);

            //saving default e-mail address
            EditText email1 = findViewById(R.id.sendtoemail1);
            String strsendtoemail1 = email1.getText().toString();
            writeToPreference("e1", strsendtoemail1);

            //saving SMS message
            EditText smsmsg = findViewById(R.id.smsMessage);
            String strSMSMessage = smsmsg.getText().toString();
            writeToPreference("smsMessage", strSMSMessage);

            //Saving map type
            ToggleButton maptype = findViewById(R.id.maptypeswitch);
            String terrainon = String.valueOf(maptype.isChecked());
            writeToPreference("terrainon", terrainon);

            //Saving Map ZOOM value
            SeekBar mapzoom = findViewById(R.id.mapZoomSlider);
            String mapzoomvalue = String.valueOf(mapzoom.getProgress());
            writeToPreference("mapzoom", mapzoomvalue);

            //Sound On / OFF
            ToggleButton sound = findViewById(R.id.soundonoff);
            String soundon = String.valueOf(sound.isChecked());
            writeToPreference("soundStatus", soundon);

            //Add Map to message with location
            ToggleButton mapAddedBut = findViewById(R.id.addMapOnOff);
            String mapAdded = String.valueOf(mapAddedBut.isChecked());
            writeToPreference("addMap", mapAdded);
        }

        private void loadSavedSettings() {
            EditText phone1 = findViewById(R.id.phonenumber1);
            EditText phone2 = findViewById(R.id.phonenumber2);
            //EditText phone3 = findViewById(R.id.phonenumber3);
            phone1.setText(getPreferenceValue("p1"));
            phone2.setText(getPreferenceValue("p2"));
            //phone3.setText(getPreferenceValue("p3"));

            //E-mail field
            EditText email1 = findViewById(R.id.sendtoemail1);
            email1.setText(getPreferenceValue("e1"));

            //SMS message field
            EditText smsmsg = findViewById(R.id.smsMessage);
            smsmsg.setText(getPreferenceValue("smsMessage"));

            //Map type switch
            ToggleButton maptype = findViewById(R.id.maptypeswitch);
            maptype.setChecked(Boolean.parseBoolean(getPreferenceValue("terrainon")));

            //Map ZOOM field
            SeekBar mapzoom = findViewById(R.id.mapZoomSlider);
            Integer mapZoomValue;
            if (getPreferenceValue("mapzoom") == "") { mapZoomValue = 16;} else {mapZoomValue = Integer.parseInt(getPreferenceValue("mapzoom"));}
            mapzoom.setProgress(mapZoomValue);
            TextView MapZoomTitleBox = findViewById(R.id.mapzoomtitle);
            MapZoomTitleBox.setText("Map ZOOM: " + mapZoomValue);

            //Sound On / OFF
            ToggleButton sound = findViewById(R.id.soundonoff);
            sound.setChecked(Boolean.parseBoolean(getPreferenceValue("soundStatus")));

            //Add map to message with location On / OFF
            ToggleButton mapAddedBut = findViewById(R.id.addMapOnOff);
            mapAddedBut.setChecked(Boolean.parseBoolean(getPreferenceValue("addMap")));
        }
    }