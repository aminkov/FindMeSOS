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
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.location.LocationListener;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.squareup.picasso.Picasso;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

public class MainActivity extends AppCompatActivity implements LocationListener {
    private static final long REFRESH_BUTTON_CLICK_INTERVAL = 3000;
    private TextView latitudeField;
    private TextView longitudeField;
    private LocationManager locManager;
    private static final long LOCATION_REFRESH_TIME = 3000;
    private static final long LOCATION_REFRESH_DISTANCE = 5;
    private static final String APIKEY = BuildConfig.FindMeSOS_ApiKey;
    private static long mLastRefreshClickTime;

    //On create method goes here
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Check if permission is granted
        while (!checkPermissionsNew()) {
            checkPermissionsNew();
        }
        //Initialize location fields
        latitudeField = findViewById(R.id.textview1);
        longitudeField = findViewById(R.id.textview2);
        // Get the location manager
        locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //Check if GPS is enabled and prompt user to enable if it's not
        checkAndPromptIfGPSIsDisabled();
        //if all OK, set location
        setLocation();
        setLocationMapThroughGoogleAPI();
    }
    //All other methods go here
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
            Toast.makeText(getApplicationContext(), getString(R.string.loc_provider_initialized), Toast.LENGTH_SHORT).show();
            latitudeField.setText(formatLatitude(location.getLatitude()));
            longitudeField.setText(formatLongitude(location.getLongitude()));


            TextView elevationTextView2 = findViewById(R.id.tvaltvalgps);
            int a = (int) location.getAltitude();
           elevationTextView2.setText(a+" m");

        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.oops_loc_not_available_msg), Toast.LENGTH_SHORT).show();
            latitudeField.setText(getString(R.string.loc_not_available_field));
            longitudeField.setText(getString(R.string.loc_not_available_field));
        }
        //request location updates if permission Ok
        locManager.requestLocationUpdates(locManager.GPS_PROVIDER, LOCATION_REFRESH_TIME, LOCATION_REFRESH_DISTANCE, this);
        getElevationGoogleAPI();
    }
    protected String returnRawLocation() {
        //Returns Latitude and Longitude in string format, accuracy of up to 5 digits after the comma, and separated by "43.38352,23.45767", used by the send SMS, copy and other functions.

        if (!locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            //TODO think what to do here and if you need it at all...
        } else {
            Location location = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                double lat = location.getLatitude();
                double lon = location.getLongitude();
                //get coordinates after the decimal pointer:  -123.[ 123456789012 ]
                String latstrright = String.valueOf(lat).substring(String.valueOf(lat).indexOf(".") + 1);
                String lonstrright = String.valueOf(lon).substring(String.valueOf(lon).indexOf(".") + 1);
                //shorten the decimal part to 5 symbols and lose the rest - [ 12345.... ]
                if (latstrright.length() > 5) {
                    latstrright = latstrright.substring(0, 5);
                }
                if (lonstrright.length() > 5) {
                    lonstrright = lonstrright.substring(0, 5);
                }
                //get the coordinates before the decimal delimiter: [ -123 ].123456789012
                String latstrleft = String.valueOf(lat).substring(0, String.valueOf(lat).indexOf("."));
                String lonstrleft = String.valueOf(lon).substring(0, String.valueOf(lon).indexOf("."));
                //combine left and right parts and get coordinates with accuracy of up to 5 symbols: [ -123 ] + [ 12345 ] = -123.12345
                String lonstr = lonstrleft + "." + lonstrright;
                String latstr = latstrleft + "." + latstrright;
                //return latitude and longitude with 5 symbols accuracy in the form: [ -123.12345,123.12345 ]
                return latstr + "," + lonstr;
                //return "42.64941,23.37352";
            }
        }
        return "NULL";
    }

    protected String getPreferenceValue(String key) {
        SharedPreferences settings = getSharedPreferences("Settings",0);
        return settings.getString(key,"");
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
        return builder.toString().substring(0, 12);
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
        return builder.toString().substring(0, 12);
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
//                Toast.makeText(this, "Status Changed: Temporarily Unavailable",
//                        Toast.LENGTH_SHORT).show();
                break;
            case LocationProvider.AVAILABLE:
                Log.d("Location", "Yuupee...:  Status Changed: Location Available");
//                Toast.makeText(this, "Status Changed: Available",
//                        Toast.LENGTH_SHORT).show();
                break;
        }
    }
    protected void onResume() {
        super.onResume();
        //Toast.makeText(getApplicationContext(), getString(R.string.resume_location), Toast.LENGTH_LONG).show();
        setLocation();
    }
    @Override
    protected void onPause() {
        /* Remove the locationlistener updates when Activity is paused */
        super.onPause();
        //Toast.makeText(getApplicationContext(), getString(R.string.pause_location), Toast.LENGTH_LONG).show();
        locManager.removeUpdates(this);
    }
    public void onProviderEnabled(String provider) {
        Toast.makeText(this, getString(R.string.on_provider_enabled_method) +" "+ provider,
                Toast.LENGTH_SHORT).show();
        //setLocation();
    }
    public void onProviderDisabled(String provider) {
        Toast.makeText(this, getString(R.string.dissable_provider) +" "+ provider,
                Toast.LENGTH_SHORT).show();
        locManager.removeUpdates(this);
    }
    private void setLocationMapThroughGoogleAPI() {
        if(checkIfInternetConnection()) {
            Location location = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                String URL = createGoogleMapsAPIURL();
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
    private String createGoogleMapsAPIURL() {
        String SIZE = "300x230";
        int ZOOM = 18;
        String MAPTYPE;
        String SCALE = "4";
        String IMAGE_FORMAT = "jpg-baseline";   //available formats are: png8, png32, gif, jpg, jpg-baseline
        String MAP_MARKER_COLOR = "Red";
        if (getPreferenceValue("terrainon") == "true") {MAPTYPE = "satellite";} else {MAPTYPE = "roadmap";}
        return "https://maps.googleapis.com/maps/api/staticmap?center="+returnRawLocation()+"&maptype="+MAPTYPE+"&scale="+SCALE+"&zoom="+ZOOM+"&format="+IMAGE_FORMAT+"&size="+SIZE+"&maptype="+MAPTYPE+"&markers=color:"+MAP_MARKER_COLOR+"%7Clabel:L%7C"+returnRawLocation()+"&key="+APIKEY;
    }
    //Button functions
    public void goToSettings(View view) {
        Intent intent = new Intent(MainActivity.this, Settings.class);
        startActivity(intent);
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
        //Commented code needs storage permission //TODO - check how to share image without storage permissions
//        ImageView mapView = findViewById(R.id.mapview);
//        if (null!=mapView.getDrawable()) {
//            Drawable mDrawable = mapView.getDrawable();
//            Bitmap mBitmap = ((BitmapDrawable) mDrawable).getBitmap();
//            String path = MediaStore.Images.Media.insertImage(getContentResolver(),
//                    mBitmap, "Location", null);
//            Intent i = new Intent(Intent.ACTION_SEND);
//            i.setType("image/*");
//            i.putExtra(Intent.EXTRA_STREAM, Uri.parse(path));
//            i.putExtra(Intent.EXTRA_EMAIL, new String[]{getPreferenceValue("e1")});
//            i.putExtra(Intent.EXTRA_SUBJECT, "My location");
//            i.putExtra(Intent.EXTRA_TEXT, getPreferenceValue("smsMessage") + " https://www.google.com/maps/place/" + returnRawLocation());
//            startActivity(Intent.createChooser(i, getString(R.string.sharing_intent_title)));
//        } else {
//            //if drawable empty
//        }
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_EMAIL  , new String[]{getPreferenceValue("e1")});
        i.putExtra(Intent.EXTRA_SUBJECT, "My location");
        i.putExtra(Intent.EXTRA_TEXT, getPreferenceValue("smsMessage") + " https://www.google.com/maps/place/"+ returnRawLocation());
        startActivity(Intent.createChooser(i, getString(R.string.sharing_intent_title)));
    }

    public void refreshLocationButton(View view) {

        if (SystemClock.elapsedRealtime() - mLastRefreshClickTime > REFRESH_BUTTON_CLICK_INTERVAL) {
            checkAndPromptIfGPSIsDisabled();
            setLocation();
            setLocationMapThroughGoogleAPI();
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.crazy_clicking), Toast.LENGTH_SHORT).show();
        }
        mLastRefreshClickTime = SystemClock.elapsedRealtime();
      }

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
    private void getElevationGoogleAPI() {
        final String elevationURL = "https://maps.googleapis.com/maps/api/elevation/json?locations="+returnRawLocation()+"&key="+APIKEY;
        RequestQueue localRequestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest jReq = new JsonObjectRequest(Request.Method.GET, elevationURL, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                TextView elevationTextView = findViewById(R.id.tvaltvalmap);
                try {
                    JSONArray jArr = response.getJSONArray("results");
                    double elevation = jArr.getJSONObject(0).getDouble("elevation");
                    int roundedElevation = (int) elevation;
                    elevationTextView.setText(roundedElevation+" m");
                } catch (JSONException e) {
                    e.printStackTrace();
                    elevationTextView.setText("JSONArray is doing dirty tricks again...");
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                TextView elevationTextView = findViewById(R.id.tvaltvalmap);
                elevationTextView.setText("Error event, no response ");
            }
        });
        // Access the RequestQueue through your singleton class.
        localRequestQueue.add(jReq);
        }
}

