package org.telegram.messenger.mod;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Обмен фейк-подарками между мод-юзерами через небольшой ретранслятор.
 *
 * Ретранслятор — отдельный сервер, который поднимает владелец мода. Он хранит
 * только косметические фейк-подарки. Никакие данные Telegram через него не идут.
 */
public class ModRelay {

    public interface SendCallback {
        void onResult(boolean ok, String message);
    }

    public interface PendingCallback {
        void onResult(List<String[]> gifts);
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static boolean configured() {
        ModConfig cfg = ModConfig.get();
        return cfg.getRelayUrl() != null && !cfg.getRelayUrl().isEmpty();
    }

    public static boolean isConfigured() {
        return configured();
    }

    public static void send(final String from, final String to, final String giftId, final String giftName, final SendCallback cb) {
        if (!configured() || from == null || to == null) {
            post(cb, false, "Ретранслятор не настроен");
            return;
        }
        new Thread(() -> {
            try {
                ModConfig cfg = ModConfig.get();
                JSONObject body = new JSONObject();
                body.put("secret", cfg.getRelaySecret());
                body.put("from", strip(from));
                body.put("to", strip(to));
                body.put("gift", giftId);
                body.put("name", giftName);
                int code = http("POST", cfg.getRelayUrl() + "/send", body.toString());
                post(cb, code >= 200 && code < 300, code >= 200 && code < 300 ? "Отправлено (фейк)" : ("Ошибка " + code));
            } catch (Throwable t) {
                post(cb, false, "Ошибка сети");
            }
        }).start();
    }

    public static void fetchPending(final String user, final PendingCallback cb) {
        if (!configured() || user == null) {
            post(cb, new ArrayList<>());
            return;
        }
        new Thread(() -> {
            List<String[]> result = new ArrayList<>();
            try {
                ModConfig cfg = ModConfig.get();
                String url = cfg.getRelayUrl() + "/pending?secret="
                        + URLEncoder.encode(cfg.getRelaySecret(), "UTF-8")
                        + "&user=" + URLEncoder.encode(strip(user), "UTF-8");
                String resp = httpGet(url);
                JSONArray arr = new JSONArray(resp);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    result.add(new String[]{o.optString("gift"), o.optString("name")});
                }
            } catch (Throwable ignore) {
            }
            post(cb, result);
        }).start();
    }

    private static String strip(String s) {
        s = s == null ? "" : s.trim();
        return s.startsWith("@") ? s.substring(1) : s;
    }

    private static int http(String method, String url, String body) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod(method);
        c.setConnectTimeout(10000);
        c.setReadTimeout(10000);
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        try (OutputStream os = c.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        int code = c.getResponseCode();
        drain(c);
        c.disconnect();
        return code;
    }

    private static String httpGet(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod("GET");
        c.setConnectTimeout(10000);
        c.setReadTimeout(10000);
        int code = c.getResponseCode();
        String text = read(c.getInputStream());
        c.disconnect();
        return code >= 200 && code < 300 ? text : "[]";
    }

    private static void drain(HttpURLConnection c) {
        try {
            InputStream is = c.getResponseCode() < 400 ? c.getInputStream() : c.getErrorStream();
            read(is);
        } catch (Throwable ignore) {
        }
    }

    private static String read(InputStream is) {
        if (is == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line);
            }
        } catch (Throwable ignore) {
        }
        return sb.toString();
    }

    private static void post(final SendCallback cb, final boolean ok, final String msg) {
        if (cb != null) {
            MAIN.post(() -> cb.onResult(ok, msg));
        }
    }

    private static void post(final PendingCallback cb, final List<String[]> gifts) {
        if (cb != null) {
            MAIN.post(() -> cb.onResult(gifts));
        }
    }
}
