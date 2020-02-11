package com.minrax.findmesos;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.EditText;
import android.content.Intent;
import android.view.View;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class Settings extends Lib {

        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_settings);
            loadSavedSettings();
        }

        public void goToMainApp(View view) {
            playSoundIfOn();
            Intent intent = new Intent(Settings.this, MainActivity.class);
            startActivity(intent);
            saveSettingsValues();
        }

        private void saveSettingsValues() {
            //saving default phone numbers
            EditText phone1 = findViewById(R.id.phonenumber1);
            EditText phone2 = findViewById(R.id.phonenumber2);
            String strphone1 = phone1.getText().toString();
            String strphone2 = phone2.getText().toString();
            writeToPreference("p1", strphone1);
            writeToPreference("p2", strphone2);

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
            Boolean terrainon = maptype.isChecked();
            writeABooleanPreference("terrainon", terrainon);

            //Saving Map ZOOM value
            SeekBar mapzoom = findViewById(R.id.mapZoomSlider);
            String mapzoomvalue = String.valueOf(mapzoom.getProgress());
            writeToPreference("mapzoom", mapzoomvalue);

            //Sound On / OFF
            ToggleButton sound = findViewById(R.id.soundonoff);
            Boolean soundon = sound.isChecked();
            writeABooleanPreference("soundStatus", soundon);

            //Add Map to message with location
            ToggleButton mapAddedBut = findViewById(R.id.addMapOnOff);
            Boolean mapAdded = mapAddedBut.isChecked();
            writeABooleanPreference("addMap", mapAdded);
        }

        private void loadSavedSettings() {
            EditText phone1 = findViewById(R.id.phonenumber1);
            EditText phone2 = findViewById(R.id.phonenumber2);
            phone1.setText(getPreferenceValue("p1"));
            phone2.setText(getPreferenceValue("p2"));

            //E-mail field
            EditText email1 = findViewById(R.id.sendtoemail1);
            email1.setText(getPreferenceValue("e1"));

            //SMS message field
            EditText smsmsg = findViewById(R.id.smsMessage);
            smsmsg.setText(getPreferenceValue("smsMessage"));

            //Map type switch
            ToggleButton maptype = findViewById(R.id.maptypeswitch);
            maptype.setChecked(readABooleanPreference("terrainon"));

            //Map ZOOM field
            SeekBar mapzoom = findViewById(R.id.mapZoomSlider);
            int mapZoomValue;
            if (getPreferenceValue("mapzoom") == "") { mapZoomValue = 16;} else {mapZoomValue = Integer.parseInt(getPreferenceValue("mapzoom"));}
            mapzoom.setProgress(mapZoomValue);
            TextView MapZoomTitleBox = findViewById(R.id.mapzoomtitle);
            String mapZoom = "Map ZOOM: " + mapZoomValue;
            MapZoomTitleBox.setText(mapZoom);

            //Sound On / OFF
            ToggleButton sound = findViewById(R.id.soundonoff);
            sound.setChecked(readABooleanPreference("soundStatus"));

            //Add map to message with location On / OFF
            ToggleButton mapAddedBut = findViewById(R.id.addMapOnOff);
            mapAddedBut.setChecked(readABooleanPreference("addMap"));
        }

        public void requestTorchPermissionIfAbsent(View view) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                //Request permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }
    }