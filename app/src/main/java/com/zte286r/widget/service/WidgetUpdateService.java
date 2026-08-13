package com.zte286r.widget.service;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.zte286r.widget.DataUsageWidgetProvider;
import com.zte286r.widget.RouterApiClient;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Widget verilerini router'dan çekip güncelleyen servis.
 */
public class WidgetUpdateService extends Service {

    private static final String TAG = "WidgetUpdateService";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        executor.execute(() -> {
            try {
                fetchAndUpdate();
            } catch (Exception e) {
                Log.e(TAG, "Widget update failed", e);
            } finally {
                stopSelf(startId);
            }
        });
        return START_NOT_STICKY;
    }

    private void fetchAndUpdate() {
        SharedPreferences prefs = getSharedPreferences(DataUsageWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE);
        String routerIp = prefs.getString(DataUsageWidgetProvider.KEY_ROUTER_IP, "");
        String username = prefs.getString(DataUsageWidgetProvider.KEY_USERNAME, "");
        String password = prefs.getString(DataUsageWidgetProvider.KEY_PASSWORD, "");

        if (routerIp.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Log.w(TAG, "Router ayarları eksik, widget güncellenemedi");
            return;
        }

        RouterApiClient client = new RouterApiClient(routerIp, username, password);

        try {
            // Login ol
            boolean loggedIn = client.login();
            if (!loggedIn) {
                Log.e(TAG, "Router login başarısız");
                return;
            }

            // Trafik istatistiklerini çek
            Map<String, Long> stats = client.getTrafficStatistics();
            long usedBytes = stats.getOrDefault("used_bytes", 0L);
            long rxBytes = stats.getOrDefault("rx_bytes", 0L);
            long txBytes = stats.getOrDefault("tx_bytes", 0L);

            // Verileri kaydet
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(DataUsageWidgetProvider.KEY_USED, formatBytes(usedBytes));
            editor.putString(DataUsageWidgetProvider.KEY_TOTAL, formatBytes(rxBytes + txBytes));
            editor.putLong(DataUsageWidgetProvider.KEY_LAST_UPDATE, System.currentTimeMillis());
            editor.apply();

            // Widget'ı güncelle
            updateAllWidgets();

        } catch (Exception e) {
            Log.e(TAG, "Veri çekme hatası", e);
        }
    }

    private void updateAllWidgets() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName thisWidget = new ComponentName(this, DataUsageWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int appWidgetId : appWidgetIds) {
            DataUsageWidgetProvider.updateWidget(this, appWidgetManager, appWidgetId);
        }
    }

    /**
     * Byte değerini okunabilir formata çevirir (örn: 199.88GB)
     */
    public static String formatBytes(long bytes) {
        if (bytes < 0) return "0 B";

        double value = bytes;
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;

        while (value >= 1024 && unitIndex < units.length - 1) {
            value /= 1024;
            unitIndex++;
        }

        if (unitIndex == 0) {
            return String.format(java.util.Locale.getDefault(), "%.0f %s", value, units[unitIndex]);
        } else {
            return String.format(java.util.Locale.getDefault(), "%.2f %s", value, units[unitIndex]);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}