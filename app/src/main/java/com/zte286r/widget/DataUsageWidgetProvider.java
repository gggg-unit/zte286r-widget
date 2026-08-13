package com.zte286r.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import com.zte286r.widget.service.WidgetUpdateService;

public class DataUsageWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_REFRESH = "com.zte286r.widget.ACTION_REFRESH";
    public static final String PREFS_NAME = "zte286r_widget_prefs";
    public static final String KEY_USED = "used_data";
    public static final String KEY_TOTAL = "total_data";
    public static final String KEY_LAST_UPDATE = "last_update";
    public static final String KEY_ROUTER_IP = "router_ip";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_PASSWORD = "password";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
        // Widget eklendiğinde verileri router'dan çek
        Intent serviceIntent = new Intent(context, WidgetUpdateService.class);
        context.startService(serviceIntent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_REFRESH.equals(intent.getAction())) {
            // Servisi başlatarak verileri router'dan çek
            Intent serviceIntent = new Intent(context, WidgetUpdateService.class);
            context.startService(serviceIntent);
        }
    }

    public static void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_data_usage);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        String used = prefs.getString(KEY_USED, "--");
        String total = prefs.getString(KEY_TOTAL, "--");
        long lastUpdate = prefs.getLong(KEY_LAST_UPDATE, 0L);

        views.setTextViewText(R.id.widget_used_value, used);
        views.setTextViewText(R.id.widget_used_label, total);

        if (lastUpdate > 0) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
            views.setTextViewText(R.id.widget_last_update, "Son güncelleme: " + sdf.format(new java.util.Date(lastUpdate)));
        } else {
            views.setTextViewText(R.id.widget_last_update, "Son güncelleme: --");
        }

        // Refresh button
        Intent refreshIntent = new Intent(context, DataUsageWidgetProvider.class);
        refreshIntent.setAction(ACTION_REFRESH);
        PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(
                context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.btn_refresh, refreshPendingIntent);

        // Open settings activity on widget click
        Intent settingsIntent = new Intent(context, SettingsActivity.class);
        PendingIntent settingsPendingIntent = PendingIntent.getActivity(
                context, 1, settingsIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, settingsPendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}