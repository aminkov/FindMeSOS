package com.minrax.findmesos;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link QuickLocationShareConfigureActivity QuickLocationShareConfigureActivity}
 */

public class QuickLocationShare extends AppWidgetProvider {

    static long locsaveTime;

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        String lat, lon, rawLocation, p1, message;

//        CharSequence spinnerChoice = QuickLocationShareConfigureActivity.loadTitlePref(context, appWidgetId);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.quick_location_share);

        SharedPreferences settings = context.getSharedPreferences("SettingsNew",0);
        locsaveTime=Long.parseLong(settings.getString("timeLatLon","0"));

        lat = settings.getString("latitude","not found");
        lon = settings.getString("longitude","not found");
        rawLocation = settings.getString("rawLocation", "no raw location");
        p1 = settings.getString("p1", "");
        message = settings.getString("smsMessage","not found") + " https://www.google.com/maps/place/"+ rawLocation;

        Log.d("widget", "Yuupee...:  Lat is:"+lat);
        Log.d("widget", "Yuupee...:  Lon is:"+lon);
        Log.d("widget", "Yuupee...:  time is:"+locsaveTime);
        Log.d("widget", "Yuupee...:  time is:"+rawLocation);

        views.setTextViewText(R.id.widget_lat_value, lat);
        views.setTextViewText(R.id.widget_lon_value, lon);

        if (locsaveTime+(60000*10) < SystemClock.elapsedRealtime()) {
            Toast.makeText(context, R.string.widget_location_too_old, Toast.LENGTH_LONG).show();
            views.setTextViewText(R.id.widget_lat_value, "not refreshed");
            views.setTextViewText(R.id.widget_lon_value, "not refreshed");
        }
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("smsto:"+p1));
        // This ensures only SMS apps respond
        intent.putExtra("sms_body", message);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            Toast.makeText(context, R.string.opening_sms_app, Toast.LENGTH_SHORT).show();
            PendingIntent configPendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            views.setOnClickPendingIntent(R.id.sostemp, configPendingIntent);
        }
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        for (int appWidgetId : appWidgetIds) {
            QuickLocationShareConfigureActivity.deleteTitlePref(context, appWidgetId);
        }
    }
    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }
    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

