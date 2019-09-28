package com.minrax.findmesos;
import android.widget.EditText;
import android.content.SharedPreferences;
import android.content.Intent;
import android.view.View;
import android.os.Bundle;
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
            startActivity(intent);
            saveSettingsValues();
        }

        private String getPreferenceValue(String key) {
            SharedPreferences settings = this.getSharedPreferences("Settings", 0);
            return settings.getString(key, "");
        }

        private void writeToPreference(String key, String thePreference) {
            SharedPreferences.Editor editor = this.getSharedPreferences("Settings", 0).edit();
            editor.putString(key, thePreference);
            editor.commit();
        }

        private void saveSettingsValues() {
            EditText phone1 = findViewById(R.id.phonenumber1);
            EditText phone2 = findViewById(R.id.phonenumber2);
            EditText phone3 = findViewById(R.id.phonenumber3);
            EditText email1 = findViewById(R.id.sendtoemail1);
            EditText smsmsg = findViewById(R.id.smsMessage);
            ToggleButton maptype = findViewById(R.id.maptypeswitch);
            String strphone1 = phone1.getText().toString();
            String strphone2 = phone2.getText().toString();
            String strphone3 = phone3.getText().toString();
            String strsendtoemail1 = email1.getText().toString();
            String strSMSMessage = smsmsg.getText().toString();
            String terrainon = String.valueOf(maptype.isChecked());
            writeToPreference("p1", strphone1);
            writeToPreference("p2", strphone2);
            writeToPreference("p3", strphone3);
            writeToPreference("e1", strsendtoemail1);
            writeToPreference("smsMessage", strSMSMessage);
            writeToPreference("terrainon", terrainon);

        }

        private void loadSavedSettings() {
            EditText phone1 = findViewById(R.id.phonenumber1);
            EditText phone2 = findViewById(R.id.phonenumber2);
            EditText phone3 = findViewById(R.id.phonenumber3);
            EditText email1 = findViewById(R.id.sendtoemail1);
            EditText smsmsg = findViewById(R.id.smsMessage);
            ToggleButton maptype = findViewById(R.id.maptypeswitch);
            phone1.setText(getPreferenceValue("p1"));
            phone2.setText(getPreferenceValue("p2"));
            phone3.setText(getPreferenceValue("p3"));
            email1.setText(getPreferenceValue("e1"));
            smsmsg.setText(getPreferenceValue("smsMessage"));
            maptype.setChecked(Boolean.parseBoolean(getPreferenceValue("terrainon")));
        }
    }
