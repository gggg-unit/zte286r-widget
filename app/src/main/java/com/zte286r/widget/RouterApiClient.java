package com.zte286r.widget;

import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * ZTE MF286R (286R) router API istemcisi.
 * Router web arayüzü goform endpoint'lerini kullanır.
 */
public class RouterApiClient {

    private static final String TAG = "RouterApiClient";
    private static final int TIMEOUT_MS = 10000;

    private final String baseUrl;
    private final String username;
    private final String password;
    private String sessionCookie;

    public RouterApiClient(String routerIp, String username, String password) {
        // IP zaten http:// içeriyorsa kullan, değilse ekle
        if (routerIp.startsWith("http://") || routerIp.startsWith("https://")) {
            this.baseUrl = routerIp;
        } else {
            this.baseUrl = "http://" + routerIp;
        }
        this.username = username;
        this.password = password;
    }

    /**
     * Router'a login olur ve session cookie'sini saklar.
     */
    public boolean login() throws Exception {
        String loginUrl = baseUrl + "/goform/goform_set_cmd_process";

        // ZTE router'lar şifreyi base64 olarak gönderir
        String encodedPassword = Base64.encodeToString(password.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);

        Map<String, String> params = new HashMap<>();
        params.put("cmd", "LOGIN");
        params.put("password", encodedPassword);

        String response = doPost(loginUrl, params);

        // Login başarılı mı kontrol et
        if (response != null && response.contains("success")) {
            return true;
        }

        // Bazı modellerde farklı yanıt formatı olabilir
        if (response != null && response.contains("result")) {
            try {
                JSONObject json = new JSONObject(response);
                String result = json.optString("result", "");
                return "success".equalsIgnoreCase(result);
            } catch (Exception e) {
                Log.e(TAG, "Login response parse error", e);
            }
        }

        return false;
    }

    /**
     * Veri kullanım istatistiklerini çeker.
     * @return Map içinde used_bytes, rx_bytes, tx_bytes anahtarları
     */
    public Map<String, Long> getTrafficStatistics() throws Exception {
        String statsUrl = baseUrl + "/goform/goform_get_cmd_process?cmd=traffic_stat";

        String response = doGet(statsUrl);
        Map<String, Long> result = new HashMap<>();

        if (response == null || response.isEmpty()) {
            throw new Exception("Boş yanıt alındı");
        }

        Log.d(TAG, "Traffic response: " + response);

        try {
            JSONObject json = new JSONObject(response);
            JSONObject traffic = json.optJSONObject("traffic_stat");
            if (traffic != null) {
                long rxBytes = traffic.optLong("rx_bytes", 0);
                long txBytes = traffic.optLong("tx_bytes", 0);
                result.put("rx_bytes", rxBytes);
                result.put("tx_bytes", txBytes);
                result.put("used_bytes", rxBytes + txBytes);
                return result;
            }
        } catch (Exception e) {
            Log.e(TAG, "JSON parse error", e);
        }

        // Alternatif: doğrudan alanlar
        try {
            JSONObject json = new JSONObject(response);
            long rxBytes = json.optLong("rx_bytes", -1);
            long txBytes = json.optLong("tx_bytes", -1);
            if (rxBytes >= 0 && txBytes >= 0) {
                result.put("rx_bytes", rxBytes);
                result.put("tx_bytes", txBytes);
                result.put("used_bytes", rxBytes + txBytes);
                return result;
            }
        } catch (Exception e) {
            Log.e(TAG, "JSON parse error alt", e);
        }

        throw new Exception("İstatistik verisi ayrıştırılamadı: " + response);
    }

    /**
     * Aylık veri kullanımını çeker (bazı modellerde mevcut).
     */
    public Map<String, Long> getMonthlyTraffic() throws Exception {
        String statsUrl = baseUrl + "/goform/goform_get_cmd_process?cmd=monthly_rx_bytes,monthly_tx_bytes";

        String response = doGet(statsUrl);
        Map<String, Long> result = new HashMap<>();

        if (response == null || response.isEmpty()) {
            throw new Exception("Boş yanıt alındı");
        }

        Log.d(TAG, "Monthly response: " + response);

        try {
            JSONObject json = new JSONObject(response);
            long rxBytes = json.optLong("monthly_rx_bytes", 0);
            long txBytes = json.optLong("monthly_tx_bytes", 0);
            result.put("rx_bytes", rxBytes);
            result.put("tx_bytes", txBytes);
            result.put("used_bytes", rxBytes + txBytes);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "JSON parse error", e);
            throw new Exception("Aylık veri ayrıştırılamadı: " + response);
        }
    }

    /**
     * Bağlantıyı test eder - login olmayı dener.
     */
    public boolean testConnection() {
        try {
            return login();
        } catch (Exception e) {
            Log.e(TAG, "Connection test failed", e);
            return false;
        }
    }

    private String doGet(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setRequestProperty("Accept", "application/json, text/plain, */*");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

        if (sessionCookie != null) {
            conn.setRequestProperty("Cookie", sessionCookie);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == 401) {
            // Session expired, try to re-login
            if (login()) {
                return doGet(urlString);
            }
            throw new Exception("Yetkilendirme hatası (401)");
        }

        if (responseCode >= 200 && responseCode < 300) {
            // Cookie'yi sakla
            String setCookie = conn.getHeaderField("Set-Cookie");
            if (setCookie != null) {
                sessionCookie = setCookie.split(";")[0];
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return sb.toString();
        }

        throw new Exception("HTTP hata kodu: " + responseCode);
    }

    private String doPost(String urlString, Map<String, String> params) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

        if (sessionCookie != null) {
            conn.setRequestProperty("Cookie", sessionCookie);
        }

        // Form body oluştur
        StringBuilder body = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (body.length() > 0) body.append("&");
            body.append(entry.getKey()).append("=").append(java.net.URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();

        // Cookie'yi sakla
        String setCookie = conn.getHeaderField("Set-Cookie");
        if (setCookie != null) {
            sessionCookie = setCookie.split(";")[0];
        }

        if (responseCode >= 200 && responseCode < 300) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return sb.toString();
        }

        throw new Exception("HTTP hata kodu: " + responseCode);
    }
}