package com.zte286r.widget;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.zte286r.widget.service.WidgetUpdateService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends AppCompatActivity {

    private EditText etRouterIp;
    private EditText etUsername;
    private EditText etPassword;
    private Button btnSave;
    private Button btnTest;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        etRouterIp = findViewById(R.id.et_router_ip);
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnSave = findViewById(R.id.btn_save);
        btnTest = findViewById(R.id.btn_test);
        progressBar = findViewById(R.id.progress_bar);
        tvStatus = findViewById(R.id.tv_status);

        // Mevcut ayarları yükle
        SharedPreferences prefs = getSharedPreferences(DataUsageWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE);
        etRouterIp.setText(prefs.getString(DataUsageWidgetProvider.KEY_ROUTER_IP, "192.168.1.1"));
        etUsername.setText(prefs.getString(DataUsageWidgetProvider.KEY_USERNAME, "admin"));
        etPassword.setText(prefs.getString(DataUsageWidgetProvider.KEY_PASSWORD, ""));

        btnSave.setOnClickListener(v -> saveSettings());
        btnTest.setOnClickListener(v -> testConnection());
    }

    private void saveSettings() {
        String routerIp = etRouterIp.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (routerIp.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Tüm alanları doldurun", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences(DataUsageWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(DataUsageWidgetProvider.KEY_ROUTER_IP, routerIp);
        editor.putString(DataUsageWidgetProvider.KEY_USERNAME, username);
        editor.putString(DataUsageWidgetProvider.KEY_PASSWORD, password);
        editor.apply();

        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();

        // Widget'ı hemen güncelle
        Intent serviceIntent = new Intent(this, WidgetUpdateService.class);
        startService(serviceIntent);
    }

    private void testConnection() {
        String routerIp = etRouterIp.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (routerIp.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Tüm alanları doldurun", Toast.LENGTH_SHORT).show();
            return;
        }

        btnTest.setEnabled(false);
        btnSave.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText(R.string.settings_testing);
        tvStatus.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            RouterApiClient client = new RouterApiClient(routerIp, username, password);
            boolean success = client.testConnection();

            mainHandler.post(() -> {
                progressBar.setVisibility(View.GONE);
                btnTest.setEnabled(true);
                btnSave.setEnabled(true);

                if (success) {
                    tvStatus.setText(R.string.settings_test_success);
                    tvStatus.setTextColor(getColor(R.color.widget_accent));
                } else {
                    tvStatus.setText(R.string.settings_test_fail);
                    tvStatus.setTextColor(getColor(R.color.widget_error_color));
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}