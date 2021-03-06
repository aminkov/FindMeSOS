package com.minrax.findmesos;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class MainActivity extends Lib implements LocationListener {
    private static final long REFRESH_BUTTON_CLICK_INTERVAL = 2000;
    private TextView latitudeField;
    private TextView longitudeField;
    private static LocationManager locManager;
    private static final long LOCATION_REFRESH_TIME = 3000;
    private static final long LOCATION_REFRESH_DISTANCE = 5;
    private static final String ENCODEDAPIKEY = BuildConfig.FindMeSOS_EncodedApiKey;
    private static long mLastRefreshClickTime;
    private static final int GPS_PERMISSION_CODE = 121;
    private static Location location;

    //On create method goes here
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Initialize loc manager
        locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // Check if GPS permission is granted
        checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, GPS_PERMISSION_CODE);
    }

    //All other methods go here
    void doEverything() {
        //Initialize location fields
        latitudeField = findViewById(R.id.textview1);
        longitudeField = findViewById(R.id.textview2);
        //Check if GPS is enabled and prompt user to enable if it's not
        checkAndPromptIfGPSIsDisabled();
        //if all OK, set location
        setLocation();
        setLocationMapThroughMapboxAPI();
    }

    public void checkPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {
            // Requesting the permission
            ActivityCompat.requestPermissions(MainActivity.this, new String[] { permission }, requestCode);
        }
        else {
            doEverything();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == GPS_PERMISSION_CODE) {
            // Checking whether user granted the permission or not.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Showing the toast message
//                Toast.makeText(MainActivity.this, "GPS Permission Granted", Toast.LENGTH_SHORT).show();
                doEverything();
            } else {
                Toast.makeText(MainActivity.this, "GPS Permission was Denied...", Toast.LENGTH_SHORT).show();
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.gps_permission_needed); // GPS not found
                builder.setMessage(R.string.gps_permission_needed_message); // Want to enable?
                builder.setCancelable(true);
                builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", getPackageName(), null));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        dialog.dismiss();
                        finish();
                    }
                });
                builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.dismiss();
                        finish();
                    }
                });
                final AlertDialog alert = builder.create();
                alert.show();
            }
        }
    }

    private String decodeApiKey(String key) {
        byte[] data = Base64.decode(key, Base64.DEFAULT);
        String decodedKey;
        decodedKey = new String(data, StandardCharsets.UTF_8);
        return decodedKey;
    }

    private void checkAndPromptIfGPSIsDisabled() {
        Log.d("debug", "checkAndPromptIfGPSIsDisabled executing....");
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

    @SuppressLint("MissingPermission")
    protected void setLocation() {
        Log.d("setLoc", "get location....");
        location = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        // Initialize the location fields
        if (location != null) {
            Toast.makeText(getApplicationContext(), getString(R.string.loc_provider_initialized), Toast.LENGTH_SHORT).show();
            String lat = formatLatitude(location.getLatitude());
            String lon = formatLongitude(location.getLongitude());

            if (!lat.equals("") || !lon.equals("")) {
                latitudeField.setText(lat);
                longitudeField.setText(lon);
                writeLocationToPreferences(lat, lon);
            }
            TextView elevationTextView2 = findViewById(R.id.tvaltvalgps);
            int a = (int) location.getAltitude();
            String aa = a + " m";
            elevationTextView2.setText(aa);
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.oops_loc_not_available_msg), Toast.LENGTH_SHORT).show();
            latitudeField.setText(getString(R.string.loc_not_available_field));
            longitudeField.setText(getString(R.string.loc_not_available_field));
            //trying again to get the latest location
            location = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
        //request location updates if permission Ok
        locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME, LOCATION_REFRESH_DISTANCE, this);
        getElevationFromAPI();
    }

    public void writeLocationToPreferences(String lat, String lon) {
        //saving location in preferences for widget use
        writeToPreference("latitude", lat);
        writeToPreference("longitude", lon);
        writeToPreference("timeLatLon", String.valueOf(SystemClock.elapsedRealtime()));
        writeToPreference("rawLocation", returnRawLocation(5));
    }

    @SuppressLint("MissingPermission")
    protected String returnRawLocation(int decSymbols) {
        //Returns Latitude and Longitude in string format, accuracy of up to 5 digits after the comma, and separated by "43.38352,23.45767", used by the send SMS, copy and other functions.
        if (locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            location = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                double lat = location.getLatitude();
                double lon = location.getLongitude();
                //get coordinates after the decimal pointer:  -123.[ 123456789012 ]
                String latstrright = String.valueOf(lat).substring(String.valueOf(lat).indexOf(".") + 1);
                String lonstrright = String.valueOf(lon).substring(String.valueOf(lon).indexOf(".") + 1);
                //shorten the decimal part to "decSymbols" number of symbols and lose the rest - [if decSymbols=5 - 12345.... ]
                if (latstrright.length() > decSymbols) {
                    latstrright = latstrright.substring(0, decSymbols);
                }
                if (lonstrright.length() > decSymbols) {
                    lonstrright = lonstrright.substring(0, decSymbols);
                }
                //get the coordinates before the decimal delimiter: [ -123 ].123456789012
                String latstrleft = String.valueOf(lat).substring(0, String.valueOf(lat).indexOf("."));
                String lonstrleft = String.valueOf(lon).substring(0, String.valueOf(lon).indexOf("."));
                //combine left and right parts and get coordinates with accuracy of up to "decSymbols" symbols: [ -123 ] + [ 12345 ] = -123.12345
                String lonstr = lonstrleft + "." + lonstrright;
                String latstr = latstrleft + "." + latstrright;
                //return latitude and longitude with "decSymbols" symbols accuracy in the form: [ -123.12345,123.12345 ]
                return latstr + "," + lonstr;
                //return "42.64941,23.37352";
            } else {
                return "NULL";
            }
        } else {
            return null;
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
        latitudeField.setText(formatLatitude(location.getLatitude()));
        longitudeField.setText(formatLongitude(location.getLongitude()));
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        /* This is called when the GPS status alters */
        switch (status) {
            case LocationProvider.OUT_OF_SERVICE:
                Log.d("Location", "Status Changed:  Out of Service");
                Toast.makeText(this, "Status Changed: Out of Service",
                        Toast.LENGTH_SHORT).show();
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                Log.d("Location", "Status Changed: Temporarily Unavailable");

                break;
            case LocationProvider.AVAILABLE:
                Log.d("Location", "Yuupee...:  Status Changed: Location Available");
                break;
        }
    }

    protected void onResume() {
        Log.d("debug", "onResume: resuming app...");
        super.onResume();
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            setLocation();
            setLocationMapThroughMapboxAPI();
        }
    }

    @Override
    protected void onPause() {
        /* Remove the locationlistener updates when Activity is paused */
        super.onPause();
        locManager.removeUpdates(this);
    }

    public void onProviderEnabled(String provider) {
        Toast.makeText(this, getString(R.string.on_provider_enabled_method) +" "+ provider,
                Toast.LENGTH_SHORT).show();
    }

    public void onProviderDisabled(String provider) {
        Toast.makeText(this, getString(R.string.dissable_provider) +" "+ provider, Toast.LENGTH_SHORT).show();
        locManager.removeUpdates(this);
    }

    @SuppressLint("MissingPermission")
    private void setLocationMapThroughMapboxAPI() {
        if(checkIfInternetConnection()) {
            location = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                String URL = createMapboxMapsAPIURL();
                ImageView mapview = findViewById(R.id.mapview);
                Picasso.get().load(URL).into(mapview);
            }
        } else {
            Toast.makeText(getApplicationContext(), "No connection to the Internet, map will not show...", Toast.LENGTH_LONG).show();
        }
    }

    private boolean checkIfInternetConnection() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    private String createMapboxMapsAPIURL() {
        String MAP_SIZE = "380x280";
        int ZOOM;
        if (getPreferenceValue("mapzoom").equals("")) { ZOOM = 13;} else {ZOOM = (Integer.parseInt(getPreferenceValue("mapzoom")) - 3);}
        final String MAPTYPE;
        final String SCALE;
        final String IMAGE_FORMAT = "jpg-baseline";   //available formats are: png8, png32, gif, jpg, jpg-baseline
        final String MAP_MARKER_COLOR = "Red";
        if (readABooleanPreference("terrainon")) {MAPTYPE = "satellite-v9";} else {MAPTYPE = "outdoors-v11";}
        final String[] coordinates = returnRawLocation(4).split(",");
        return "https://api.mapbox.com/styles/v1/mapbox/"+MAPTYPE+"/static/pin-l-l+3bb("+coordinates[1]+","+coordinates[0]+")/"+coordinates[1]+","+coordinates[0]+","+ZOOM+",0,0/"+MAP_SIZE+"?access_token="+decodeApiKey(ENCODEDAPIKEY);
    }

    //Button functions
    public void goToSettings(View view) {
        Intent intent = new Intent(MainActivity.this, Settings.class);
        startActivity(intent);
        playSoundIfOn();
    }

    public void sendSMS(View view) {
        String message = getPreferenceValue("smsMessage") + " https://www.google.com/maps/place/"+ returnRawLocation(5);
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("smsto:"+getPreferenceValue("p1")));
        // This ensures only SMS apps respond
        intent.putExtra("sms_body", message);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
            playSoundIfOn();
        }
    }

    public void launchGMaps(View view) {
        //opens Google Maps with current lat and long
        String label = "My location";
        String uriBegin = "geo:" + returnRawLocation(5);
        String query = returnRawLocation(5) + "(" + label + ")";
        String encodedQuery = Uri.encode(query);
        String uriString = uriBegin + "?q=" + encodedQuery + "&z=16";
        Uri uri = Uri.parse(uriString);
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW, uri);
        startActivity(intent);
        playSoundIfOn();
    }

    public void shareLocationButton(View view) {
        playSoundIfOn();
        if (readABooleanPreference("addMap")) {
            //request file permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                //Request permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
            //add image to sharing message if option enabled in settings
            ImageView mapView = findViewById(R.id.mapview);
            if (null != mapView.getDrawable()) {
                Drawable mDrawable = mapView.getDrawable();
                Bitmap mBitmap = ((BitmapDrawable) mDrawable).getBitmap();
                String path = MediaStore.Images.Media.insertImage(getContentResolver(), mBitmap, "Location", null);
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("image/*");
                i.putExtra(Intent.EXTRA_STREAM, Uri.parse(path));
                i.putExtra(Intent.EXTRA_EMAIL, new String[]{getPreferenceValue("e1")});
                i.putExtra(Intent.EXTRA_SUBJECT, "My location");
                i.putExtra(Intent.EXTRA_TEXT, getPreferenceValue("smsMessage") + " https://www.google.com/maps/place/" + returnRawLocation(5));
                startActivity(Intent.createChooser(i, getString(R.string.sharing_intent_title)));
            } else {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_EMAIL  , new String[]{getPreferenceValue("e1")});
                i.putExtra(Intent.EXTRA_SUBJECT, "My location");
                i.putExtra(Intent.EXTRA_TEXT, getPreferenceValue("smsMessage") + " https://www.google.com/maps/place/"+ returnRawLocation(5));
                startActivity(Intent.createChooser(i, getString(R.string.sharing_intent_title)));
            }
        } else {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_EMAIL, new String[]{getPreferenceValue("e1")});
            i.putExtra(Intent.EXTRA_SUBJECT, "My location");
            i.putExtra(Intent.EXTRA_TEXT, getPreferenceValue("smsMessage") + " https://www.google.com/maps/place/" + returnRawLocation(5));
            startActivity(Intent.createChooser(i, getString(R.string.sharing_intent_title)));
        }
    }

    public void refreshLocationButton(View view) {
        if (SystemClock.elapsedRealtime() - mLastRefreshClickTime > REFRESH_BUTTON_CLICK_INTERVAL) {
            doEverything();
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.crazy_clicking), Toast.LENGTH_SHORT).show();
        }
        mLastRefreshClickTime = SystemClock.elapsedRealtime();
        playSoundIfOn();
    }

    @SuppressLint("MissingPermission")
    public void copyLocationToClipboard(View view) {
        playSoundIfOn();
        location = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
            String coordinates = "https://www.google.com/maps/place/"+ returnRawLocation(5);
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

    private void getElevationFromAPI() {
        final String elevationURL = "https://api.opentopodata.org/v1/eudem25m?locations="+returnRawLocation(5);
        RequestQueue localRequestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest jReq = new JsonObjectRequest(Request.Method.GET, elevationURL, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                TextView elevationTextView = findViewById(R.id.tvaltvalmap);
                try {
                    JSONArray jArr = response.getJSONArray("results");
                    double elevation = jArr.getJSONObject(0).getDouble("elevation");
                    int roundedElevation = (int) elevation;
                    String finalElevationString = roundedElevation+" m";
                    elevationTextView.setText(finalElevationString);
                } catch (JSONException e) {
                    e.printStackTrace();
                    elevationTextView.setText("JSONArray is doing dirty tricks again...");
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                TextView elevationTextView = findViewById(R.id.tvaltvalmap);
                elevationTextView.setText("Error event, no response");
            }
        });
        // Access the RequestQueue through your singleton class.
        localRequestQueue.add(jReq);
    }

    public void sosButtonClick(View view) {
            playSoundIfOn();
            if(checkFlashlightAvailability()){
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    //Request permission if not granted already
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
                } else {
                    try {
                        sosLightOn();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
        } else {
                Toast.makeText(getApplicationContext(), "There is no Flashlight available on this device!", Toast.LENGTH_LONG).show();
            }
    }

    private boolean checkFlashlightAvailability() {
        //checking if flashlight is available on the device
        return getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    protected void sosLightOn() throws InterruptedException {
        //Start SOS algorithm
        String sosString = "000111000";
        Camera camera = Camera.open();
        Camera.Parameters params = camera.getParameters();
            for (int i=0; i<sosString.length(); i++) {
                if(sosString.charAt(i) == '1') {
                        params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        camera.setParameters(params);
                        camera.startPreview();
                        Thread.sleep(400);
                        params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        camera.setParameters(params);
                        camera.stopPreview();
                        Thread.sleep(300);
                } else {
                        params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        camera.setParameters(params);
                        camera.startPreview();
                        Thread.sleep(50);
                        params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        camera.setParameters(params);
                        camera.stopPreview();
                        Thread.sleep(300);
                }
            }
        Thread.sleep(1000);
    }
}