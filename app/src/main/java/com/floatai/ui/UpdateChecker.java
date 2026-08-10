package com.floatai.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {
    public interface Callback {
        void onResult(String latestVersion, String changelog);
        void onError(String message);
    }

    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public UpdateChecker(Context context) {
        this.context = context.getApplicationContext();
    }

    public void check(final Callback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://api.github.com/repos/1953187487/FloatAI/releases/latest");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("Accept", "application/vnd.github+json");
                int code = conn.getResponseCode();
                if (code != 200) {
                    handler.post(() -> callback.onError("获取版本失败 (HTTP " + code + ")"));
                    return;
                }
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                conn.disconnect();

                JSONObject json = new JSONObject(sb.toString());
                String tag = json.optString("tag_name", "v0.1");
                String body = json.optString("body", "");
                handler.post(() -> callback.onResult(tag, body));
            } catch (Exception e) {
                handler.post(() -> callback.onError("检查更新失败：" + e.getMessage()));
            }
        }).start();
    }
}
