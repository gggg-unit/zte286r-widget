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
    private String lastError = "";

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

    public String getLastError() {
        return lastError;
    }

    /**
     * Router'a login olur ve session cookie'sini saklar.
     * Birden fazla login yöntemi dener (firmware sürümlerine göre).
     */
    public boolean login() throws Exception {
        // Yöntem 1: Standart ZTE login (cmd=LOGIN, password=base64)
        if (tryLoginMethod1()) {
            return true;
        }

        // Yöntem 2: cmd=login (küçük harf)
        if (tryLoginMethod2()) {
            return true;
        }

        // Yöntem 3: isTest parametresi ile
        if (tryLoginMethod3()) {
            return true;
        }

        lastError = "Login başarısız: " + lastError;
        return false;
    }

    private boolean tryLoginMethod1() {
        try {
            String loginUrl = baseUrl + "/goform/goform_set_cmd_process";
            String encodedPassword = Base64.encodeToString(password.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);

            Map<String, String> params = new HashMap<>();
            params.put("cmd", "LOGIN");
            params.put("password", encodedPassword);

            String response = doPost(loginUrl, params);
            if (isLoginSuccess(response)) {
                return true;
            }
            lastError = "Yöntem 1 başarısız: " + response;
        } catch (Exception e) {
            lastError = "Yöntem 1 hata: " + e.getMessage();
            Log.e(TAG, "Login method 1 failed", e);
        }
        return false;
    }

    private boolean tryLoginMethod2() {
        try {
            String loginUrl = baseUrl + "/goform/goform_set_cmd_process";
            String encodedPassword = Base64.encodeToString(password.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);

            Map<String, String> params = new HashMap<>();
            params.put("cmd", "login");
            params.put("password", encodedPassword);

            String response = doPost(loginUrl, params);
            if (isLoginSuccess(response)) {
                return true;
            }
            lastError = "Yöntem 2 başarısız: " + response;
        } catch (Exception e) {
            lastError = "Yöntem 2 hata: " + e.getMessage();
            Log.e(TAG, "Login method 2 failed", e);
        }
        return false;
    }

    private boolean tryLoginMethod3() {
        try {
            String loginUrl = baseUrl + "/goform/goform_set_cmd_process";
            String encodedPassword = Base64.encodeToString(password.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);

            Map<String, String> params = new HashMap<>();
            params.put("cmd", "LOGIN");
            params.put("password", encodedPassword);
            params.put("isTest", "false");

            String response = doPost(loginUrl, params);
            if (isLoginSuccess(response)) {
                return true;
            }
            lastError = "Yöntem 3 başarısız: " + response;
        } catch (Exception e) {
            lastError = "Yöntem 3 hata: " + e.getMessage();
            Log.e(TAG, "Login method 3 failed", e);
        }
        return false;
    }

    private boolean isLoginSuccess(String response) {
        if (response == null) return false;

        // "success" içeriyorsa başarılı
        if (response.contains("success")) {
            return true;
        }

        // JSON formatında result alanı kontrol et
        try {
            JSONObject json = new JSONObject(response);
            String result = json.optString("result", "");
            if ("success".equalsIgnoreCase(result)) {
                return true;
            }
        } catch (Exception e) {
            // JSON değilse devam et
        }

        return false;
    }

    /**
     * Veri kullanım istatistiklerini çeker.
     * @return Map içinde used_bytes, rx_bytes, tx_bytes anahtarları
     */
    public Map<String, Long> getTrafficStatistics() throws Exception {
        // Önce login olmayı dene
        if (sessionCookie == null) {
            if (!login()) {
                throw new Exception("Login başarısız: " + lastError);
            }
        }

        // Yöntem 1: traffic_stat
        try {
            String statsUrl = baseUrl + "/goform/goform_get_cmd_process?cmd=traffic_stat";
            String response = doGet(statsUrl);
            Map<String, Long> result = parseTrafficResponse(response);
            if (result != null) {
                return result;
            }
        } catch (Exception e) {
            Log.e(TAG, "traffic_stat failed", e);
        }

        // Yöntem 2: traffic_statistics
        try {
            String statsUrl = baseUrl + "/goform/goform_get_cmd_process?cmd=traffic_statistics";
            String response = doGet(statsUrl);
            Map<String, Long> result = parseTrafficResponse(response);
            if (result != null) {
                return result;
            }
        } catch (Exception e) {
            Log.e(TAG, "traffic_statistics failed", e);
        }

        // Yöntem 3: rx_bytes, tx_bytes
        try {
            String statsUrl = baseUrl + "/goform/goform_get_cmd_process?cmd=rx_bytes,tx_bytes";
            String response = doGet(statsUrl);
            Map<String, Long> result = parseTrafficResponse(response);
            if (result != null) {
                return result;
            }
        } catch (Exception e) {
            Log.e(TAG, "rx_bytes,tx_bytes failed", e);
        }

        // Yöntem 4: monthly_rx_bytes, monthly_tx_bytes
        try {
            String statsUrl = baseUrl + "/goform/goform_get_cmd_process?cmd=monthly_rx_bytes,monthly_tx_bytes";
            String response = doGet(statsUrl);
            Map<String, Long> result = parseTrafficResponse(response);
            if (result != null) {
                return result;
            }
        } catch (Exception e) {
            Log.e(TAG, "monthly failed", e);
        }

        throw new Exception("Trafik istatistikleri alınamadı. Son hata: " + lastError);
    }

    private Map<String, Long> parseTrafficResponse(String response) {
        if (response == null || response.isEmpty()) {
            return null;
        }

        Log.d(TAG, "Traffic response: " + response);

        try {
            JSONObject json = new JSONObject(response);

            // traffic_stat içinde nested olabilir
            JSONObject traffic = json.optJSONObject("traffic_stat");
            if (traffic != null) {
                long rxBytes = traffic.optLong("rx_bytes", -1);
                long txBytes = traffic.optLong("tx_bytes", -1);
                if (rxBytes >= 0 && txBytes >= 0) {
                    Map<String, Long> result = new HashMap<>();
                    result.put("rx_bytes", rxBytes);
                    result.put("tx_bytes", txBytes);
                    result.put("used_bytes", rxBytes + txBytes);
                    return result;
                }
            }

            // Doğrudan alanlar
            long rxBytes = json.optLong("rx_bytes", -1);
            long txBytes = json.optLong("tx_bytes", -1);
            if (rxBytes >= 0 && txBytes >= 0) {
                Map<String, Long> result = new HashMap<>();
                result.put("rx_bytes", rxBytes);
                result.put("tx_bytes", txBytes);
                result.put("used_bytes", rxBytes + txBytes);
                return result;
            }

            // monthly_rx_bytes, monthly_tx_bytes
            long monthlyRx = json.optLong("monthly_rx_bytes", -1);
            long monthlyTx = json.optLong("monthly_tx_bytes", -1);
            if (monthlyRx >= 0 && monthlyTx >= 0) {
                Map<String, Long> result = new HashMap<>();
                result.put("rx_bytes", monthlyRx);
                result.put("tx_bytes", monthlyTx);
                result.put("used_bytes", monthlyRx + monthlyTx);
                return result;
            }

        } catch (Exception e) {
            Log.e(TAG, "JSON parse error", e);
        }

        return null;
    }

    /**
     * Bağlantıyı test eder - login olmayı dener.
     */
    public boolean testConnection() {
        try {
            boolean success = login();
            if (!success) {
                Log.e(TAG, "Connection test failed: " + lastError);
            }
            return success;
        } catch (Exception e) {
            lastError = e.getMessage();
            Log.e(TAG, "Connection test exception", e);
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
            sessionCookie = null;
            if (login()) {
                return doGet(urlString);
            }
            throw new Exception("Yetkilendirme hatası (401): " + lastError);
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