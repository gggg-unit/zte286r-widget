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
import java.security.MessageDigest;
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
        // Önce login durumunu kontrol et
        if (checkLoginStatus()) {
            return true;
        }

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

        // Yöntem 4: Şifre düz metin olarak gönderilir
        if (tryLoginMethod4()) {
            return true;
        }

        // Yöntem 5: cmd=LOGIN, password düz metin, isTest=false
        if (tryLoginMethod5()) {
            return true;
        }

        // Yöntem 6: Referer header ile
        if (tryLoginMethod6()) {
            return true;
        }

        // Yöntem 7: pass parametresi ile (bazı firmware'ler)
        if (tryLoginMethod7()) {
            return true;
        }

        // Yöntem 8: isTest=true ile
        if (tryLoginMethod8()) {
            return true;
        }

        // Yöntem 9: pass parametresi + isTest=false
        if (tryLoginMethod9()) {
            return true;
        }

        // Yöntem 10: pwd parametresi ile
        if (tryLoginMethod10()) {
            return true;
        }

        // Yöntem 11: MD5 hash ile
        if (tryLoginMethod11()) {
            return true;
        }

        // Yöntem 12: MD5 hash + isTest=false
        if (tryLoginMethod12()) {
            return true;
        }

        // Yöntem 13: passwd parametresi + base64
        if (tryLoginMethod13()) {
            return true;
        }

        // Yöntem 14: passwd parametresi + MD5
        if (tryLoginMethod14()) {
            return true;
        }

        lastError = "Login başarısız: " + lastError;
        return false;
    }

    private boolean checkLoginStatus() {
        try {
            String statusUrl = baseUrl + "/goform/goform_get_cmd_process?cmd=is_login";
            String response = doGet(statusUrl);
            if (response != null && response.contains("1")) {
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Login status check failed", e);
        }
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

    private boolean tryLoginMethod4() {
        try {
            String loginUrl = baseUrl + "/goform/goform_set_cmd_process";

            Map<String, String> params = new HashMap<>();
            params.put("cmd", "LOGIN");
            params.put("password", password);

            String response = doPost(loginUrl, params);
            if (isLoginSuccess(response)) {
                return true;
            }
            lastError = "Yöntem 4 başarısız: " + response;
        } catch (Exception e) {
            lastError = "Yöntem 4 hata: " + e.getMessage();
            Log.e(TAG, "Login method 4 failed", e);
        }
        return false;
    }

    private boolean tryLoginMethod5() {
        try {
            String loginUrl = baseUrl + "/goform/goform_set_cmd_process";

            Map<String, String> params = new HashMap<>();
            params.put("cmd", "LOGIN");
            params.put("password", password);
            params.put("isTest", "false");

            String response = doPost(loginUrl, params);
            if (isLoginSuccess(response)) {
                return true;
            }
            lastError = "Yöntem 5 başarısız: " + response;
        } catch (Exception e) {
            lastError = "Yöntem 5 hata: " + e.getMessage();
            Log.e(TAG, "Login method 5 failed", e);
        }
        return false;
    }

    private boolean tryLoginMethod6() {
        try {
            String loginUrl = baseUrl + "/goform/goform_set_cmd_process";
            String encodedPassword = Base64.encodeToString(password.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);

            Map<String, String> params = new HashMap<>();
            params.put("cmd", "LOGIN");
            params.put("password", encodedPassword);

            String response = doPostWithReferer(loginUrl, params);
            if (isLoginSuccess(response)) {
                return true;
            }
            lastError = "Yöntem 6 başarısız: " + response;
        } catch (Exception e) {
            lastError = "Yöntem 6 hata: " + e.getMessage();
            Log.e(TAG, "Login method 6 failed", e);
        }
        return false;
    }

    private boolean tryLoginMethod7() {
        try {
            String loginUrl = baseUrl + "/goform/goform_set_cmd_process";
            String encodedPassword = Base64.encodeToString(password.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);

            Map<String, String> params = new HashMap<>();
            params.put("cmd", "LOGIN");
            params.put("pass", encodedPassword);

            String response = doPost(loginUrl, params);
            if (isLoginSuccess(response)) {
                return true;
            }
            lastError = "Yöntem 7 başarısız: " + response;
        } catch (Exception e) {
            lastError = "Yöntem 7 hata: " + e.getMessage();
            Log.e(TAG, "Login method 7 failed", e);
        }
        return false;
    }

    private boolean tryLoginMethod8() {
        try {
            String loginUrl = baseUrl + "/goform/goform_set_cmd_process";
            String encodedPassword = Base64.encodeToString(password.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);

            Map<String, String> params = new HashMap<>();
            params.put("cmd", "LOGIN");
            params.put("password", encodedPassword);
            params.put("isTest", "true");

            String response = doPost(loginUrl, params);
            if (isLoginSuccess(response)) {
                return true;
            }
            lastError = "Yöntem 8 başarısız: " + response;
        } catch (Exception e) {
            lastError = "Yöntem 8 hata: " + e.getMessage();
            Log.e(TAG, "Login method 8 failed", e);
        }
        return false;
    }

    private boolean tryLoginMethod9() {
        try {
            String loginUrl = baseUrl + "/goform/goform_set_cmd_process";
            String encodedPassword = Base64.encodeToString(password.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);

            Map<String, String> params = new HashMap<>();
            params.put("cmd", "LOGIN");
            params.put("pass", encodedPassword);
            params.put("isTest", "false");

            String response = doPost(loginUrl, params);
            if (isLoginSuccess(response)) {
                return true;
            }
            lastError = "Yöntem 9 başarısız: " + response;
        } catch (Exception e) {
            lastError = "Yöntem 9 hata: " + e.getMessage();
            Log.e(TAG, "Login method 9 failed", e);
        }
        return false;
    }

    private boolean tryLoginMethod10() {
        try {
            String loginUrl = baseUrl + "/goform/goform_set_cmd_process";
            String encodedPassword = Base64.encodeToString(password.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);

            Map<String, String> params = new HashMap<>();
            params.put("cmd", "LOGIN");
            params.put("pwd", encodedPassword);

            String response = doPost(loginUrl, params);
            if (isLoginSuccess(response)) {
                return true;
            }
            lastError = "Yöntem 10 başarısız: " + response;
        } catch (Exception e) {
            lastError = "Yöntem 10 hata: " + e.getMessage();
            Log.e(TAG, "Login method 10 failed", e);
        }
        return false;
    }

    private boolean tryLoginMethod11() {
        try {
            String loginUrl = baseUrl + "/goform/goform_set_cmd_process";
            String md5Password = md5(password);

            Map<String, String> params = new HashMap<>();
            params.put("cmd", "LOGIN");
            params.put("password", md5Password);

            String response = doPost(loginUrl, params);
            if (isLoginSuccess(response)) {
                return true;
            }
            lastError = "Yöntem 11 başarısız: " + response;
        } catch (Exception e) {
            lastError = "Yöntem 11 hata: " + e.getMessage();
            Log.e(TAG, "Login method 11 failed", e);
        }
        return false;
    }

    private boolean tryLoginMethod12() {
        try {
            String loginUrl = baseUrl + "/goform/goform_set_cmd_process";
            String md5Password = md5(password);

            Map<String, String> params = new HashMap<>();
            params.put("cmd", "LOGIN");
            params.put("password", md5Password);
            params.put("isTest", "false");

            String response = doPost(loginUrl, params);
            if (isLoginSuccess(response)) {
                return true;
            }
            lastError = "Yöntem 12 başarısız: " + response;
        } catch (Exception e) {
            lastError = "Yöntem 12 hata: " + e.getMessage();
            Log.e(TAG, "Login method 12 failed", e);
        }
        return false;
    }

    private boolean tryLoginMethod13() {
        try {
            String loginUrl = baseUrl + "/goform/goform_set_cmd_process";
            String encodedPassword = Base64.encodeToString(password.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);

            Map<String, String> params = new HashMap<>();
            params.put("cmd", "LOGIN");
            params.put("passwd", encodedPassword);

            String response = doPost(loginUrl, params);
            if (isLoginSuccess(response)) {
                return true;
            }
            lastError = "Yöntem 13 başarısız: " + response;
        } catch (Exception e) {
            lastError = "Yöntem 13 hata: " + e.getMessage();
            Log.e(TAG, "Login method 13 failed", e);
        }
        return false;
    }

    private boolean tryLoginMethod14() {
        try {
            String loginUrl = baseUrl + "/goform/goform_set_cmd_process";
            String md5Password = md5(password);

            Map<String, String> params = new HashMap<>();
            params.put("cmd", "LOGIN");
            params.put("passwd", md5Password);

            String response = doPost(loginUrl, params);
            if (isLoginSuccess(response)) {
                return true;
            }
            lastError = "Yöntem 14 başarısız: " + response;
        } catch (Exception e) {
            lastError = "Yöntem 14 hata: " + e.getMessage();
            Log.e(TAG, "Login method 14 failed", e);
        }
        return false;
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "MD5 error", e);
            return input;
        }
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
        return doPostInternal(urlString, params, false);
    }

    private String doPostWithReferer(String urlString, Map<String, String> params) throws Exception {
        return doPostInternal(urlString, params, true);
    }

    private String doPostInternal(String urlString, Map<String, String> params, boolean withReferer) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
        conn.setRequestProperty("Accept", "application/json, text/javascript, */*; q=0.01");

        if (withReferer) {
            conn.setRequestProperty("Referer", baseUrl + "/index.html");
        }

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
