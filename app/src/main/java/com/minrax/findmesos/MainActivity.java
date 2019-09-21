package com.minrax.findmesos;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.location.LocationListener;
import com.squareup.picasso.Picasso;

public class MainActivity extends AppCompatActivity implements LocationListener {
    private TextView latitudeField;
    private TextView longitudeField;
    private LocationManager locManager;
    private static final long LOCATION_REFRESH_TIME = 0;
    private static final long LOCATION_REFRESH_DISTANCE = 0;
    private static final String APIKEY = "AIzaSyDipuynz7RexUFaLzaRfEXNOXEMWKLDnqo";

    //On create method goes here
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        while (!checkPermissionsNew()) {
            checkPermissionsNew();
        }
        latitudeField = findViewById(R.id.textview1);
        longitudeField = findViewById(R.id.textview2);
        // Get the location manager
        locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //Check if GPS is enabled and prompt user to enable if it's not
        checkAndPromptIfGPSIsDisabled();
        // Check if permission is granted
        setLocation();
        setLocationMapThroughGoogleAPI();
    }
    //All other methods go here
    public void copyLocationToClipboard(View view) {
        Location location = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
            String coordinates = "https://www.google.com/maps/place/"+ returnRawLocation();
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("My location", coordinates);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getApplicationContext(), getString(R.string.loc_copies_msg), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.oops_loc_not_available_msg), Toast.LENGTH_LONG).show();
            checkAndPromptIfGPSIsDisabled();
            setLocation();
        }
    }
    private void checkAndPromptIfGPSIsDisabled() {
        if (!locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.gps_not_found_title); // GPS not found
            builder.setMessage(R.string.gps_not_found_message); // Want to enable?
            builder.setCancelable(true);
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                            dialog.dismiss();
                        }
                    });
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            dialog.dismiss();
                        }
                    });
            final AlertDialog alert = builder.create();
            alert.show();
        }
    }
    protected boolean checkPermissionsNew() {
        boolean isPermission;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Request permission
            isPermission = false;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            isPermission = true;
        }
        return isPermission;
    }
    protected void setLocation() {
        Location location = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//        // Initialize the location fields
        if (location != null) {
            //Toast.makeText(getApplicationContext(), getString(R.string.loc_provider_initialized), Toast.LENGTH_LONG).show();
            latitudeField.setText(formatLatitude(location.getLatitude()));
            longitudeField.setText(formatLongitude(location.getLongitude()));
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.oops_loc_not_available_msg), Toast.LENGTH_LONG).show();
            latitudeField.setText(getString(R.string.loc_not_available_field));
            longitudeField.setText(getString(R.string.loc_not_available_field));
        }
        //request location updates if permission Ok
        locManager.requestLocationUpdates(locManager.GPS_PROVIDER, LOCATION_REFRESH_TIME, LOCATION_REFRESH_DISTANCE, this);
    }
    protected String returnRawLocation() {
        //Returns Latitude and Longitude in string format separated by "43.383525,23.4576457", used by the send SMS, copy and other functions.
        Location location = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
            //Toast.makeText(getApplicationContext(), "Location provider initialized, returning location...", Toast.LENGTH_LONG).show();
            double lat = location.getLatitude();
            double lon = location.getLongitude();
            return lat + "," + lon;
        } else {
            checkAndPromptIfGPSIsDisabled();
            setLocation();
            //Toast.makeText(getApplicationContext(), getString(R.string.oops_loc_not_available_msg), Toast.LENGTH_LONG).show();
        }
        return "NULL";
    }
    protected String getPreferenceValue(String key) {
        SharedPreferences settings = getSharedPreferences("Settings",0);
        return settings.getString(key,"");
    }
    public void sendSMS(View view) {
        String message = getPreferenceValue("smsMessage") + " https://www.google.com/maps/place/"+ returnRawLocation();
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("smsto:"+getPreferenceValue("p1")));
        // This ensures only SMS apps respond
        intent.putExtra("sms_body", message);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }
    private String formatLatitude(double latitude) {
        //returns formatted longitude with the following format: N 40°42'46.02132"
        StringBuilder builder = new StringBuilder();
        if (latitude < 0) {
            builder.append("S ");
        } else {
            builder.append("N ");
        }
        String latitudeDegrees = Location.convert(Math.abs(latitude), Location.FORMAT_SECONDS);
        String[] latitudeSplit = latitudeDegrees.split(":");
        builder.append(latitudeSplit[0]);
        builder.append("°");
        builder.append(latitudeSplit[1]);
        builder.append("'");
        builder.append(latitudeSplit[2]);
        builder.append("\"");
        return builder.toString();
    }
    private String formatLongitude(double longitude) {
        //returns formatted longitude with the following format:  W 74°0'21.38868"
        StringBuilder builder = new StringBuilder();
        if (longitude < 0) {
            builder.append("W ");
        } else {
            builder.append("E ");
        }
        String longitudeDegrees = Location.convert(Math.abs(longitude), Location.FORMAT_SECONDS);
        String[] longitudeSplit = longitudeDegrees.split(":");
        builder.append(longitudeSplit[0]);
        builder.append("°");
        builder.append(longitudeSplit[1]);
        builder.append("'");
        builder.append(longitudeSplit[2]);
        builder.append("\"");
        return builder.toString();
    }
    public void launchGMaps(View view) {
        //opens Google Maps with current lat and long
        String label = "My location";
        String uriBegin = "geo:" + returnRawLocation();
        String query = returnRawLocation() + "(" + label + ")";
        String encodedQuery = Uri.encode(query);
        String uriString = uriBegin + "?q=" + encodedQuery + "&z=16";
        Uri uri = Uri.parse(uriString);
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }
    public void shareLocationButton(View view) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_EMAIL  , new String[]{getPreferenceValue("e1")});
        i.putExtra(Intent.EXTRA_SUBJECT, "My location");
        i.putExtra(Intent.EXTRA_TEXT, getPreferenceValue("smsMessage") + " https://www.google.com/maps/place/"+ returnRawLocation());
        startActivity(Intent.createChooser(i, getString(R.string.sharing_intent_title)));
    }
    public void refreshLocationButton(View view) {
        checkAndPromptIfGPSIsDisabled();
        setLocation();
        setLocationMapThroughGoogleAPI();
    }
    @Override
    public void onLocationChanged(Location location) {
        //Toast.makeText(getApplicationContext(), "Location just updated...", Toast.LENGTH_LONG).show();
        latitudeField.setText(formatLatitude(location.getLatitude()));
        longitudeField.setText(formatLongitude(location.getLongitude()));
    }
    public void onStatusChanged(String provider, int status, Bundle extras) {
        //Toast.makeText(getApplicationContext(), "Status change happening...", Toast.LENGTH_LONG).show();
        /* This is called when the GPS status alters */
        switch (status) {
            case LocationProvider.OUT_OF_SERVICE:
                Log.d("Lovstion", "Status Changed: Out of Service");
                Toast.makeText(this, "Status Changed: Out of Service",
                        Toast.LENGTH_SHORT).show();
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                Log.d("Location", "Status Changed: Temporarily Unavailable");
                Toast.makeText(this, "Status Changed: Temporarily Unavailable",
                        Toast.LENGTH_SHORT).show();
                break;
            case LocationProvider.AVAILABLE:
                Log.d("Location", "Yuupee...:  Status Changed: Location Available");
                Toast.makeText(this, "Status Changed: Available",
                        Toast.LENGTH_SHORT).show();
                break;
        }
    }
    protected void onResume() {
        super.onResume();
        Toast.makeText(getApplicationContext(), getString(R.string.resume_location), Toast.LENGTH_LONG).show();
        setLocation();
    }
    @Override
    protected void onPause() {
        /* Remove the locationlistener updates when Activity is paused */
        super.onPause();
        Toast.makeText(getApplicationContext(), getString(R.string.pause_location), Toast.LENGTH_LONG).show();
        locManager.removeUpdates(this);
    }
    public void onProviderEnabled(String provider) {
        Toast.makeText(this, getString(R.string.on_provider_enabled_method) + provider,
                Toast.LENGTH_SHORT).show();
        //setLocation();
    }
    public void onProviderDisabled(String provider) {
        Toast.makeText(this, getString(R.string.dissable_provider) + provider,
                Toast.LENGTH_SHORT).show();
        locManager.removeUpdates(this);
    }
    public void goToSettings(View view) {
        Intent intent = new Intent(MainActivity.this, Settings.class);
        startActivity(intent);
    }
    private void setLocationMapThroughGoogleAPI() {
        if(checkIfInternetConnection()) {
        Location location = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
        String URL = "https://maps.googleapis.com/maps/api/staticmap?center="+location.getLatitude()+","+location.getLongitude()+"&zoom=15&size=300x230&maptype=roadmap&markers=color:red%7Clabel:L%7C"+location.getLatitude()+","+location.getLongitude()+"&key="+APIKEY;
        ImageView mapview = findViewById(R.id.mapview);
        Picasso.with(this).load(URL).into(mapview);
            }
        } else {
            Toast.makeText(getApplicationContext(), "No connection to the Internet, map will not show...", Toast.LENGTH_LONG).show();
        }
    }
    private boolean checkIfInternetConnection() {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }
}

