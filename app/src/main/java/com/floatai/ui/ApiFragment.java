package com.floatai.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ApiFragment extends Fragment {
    private EditText etUrl;
    private EditText etKey;
    private EditText etModel;
    private TextView tvResult;
    private Button btnTest;
    private Button btnSave;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_api, container, false);
        etUrl = v.findViewById(R.id.api_url);
        etKey = v.findViewById(R.id.api_key);
        etModel = v.findViewById(R.id.model_name);
        tvResult = v.findViewById(R.id.tv_result);
        btnTest = v.findViewById(R.id.btn_test);
        btnSave = v.findViewById(R.id.btn_save);

        SharedPreferences sp = requireContext().getSharedPreferences("float_ai_prefs", 0);
        etUrl.setText(sp.getString("api_url", ""));
        etKey.setText(sp.getString("api_key", ""));
        etModel.setText(sp.getString("api_model", ""));

        btnTest.setOnClickListener(view -> testModel());
        btnSave.setOnClickListener(view -> saveConfig());
        return v;
    }

    private void saveConfig() {
        SharedPreferences.Editor ed = requireContext()
            .getSharedPreferences("float_ai_prefs", 0).edit();
        ed.putString("api_url", etUrl.getText().toString().trim());
        ed.putString("api_key", etKey.getText().toString().trim());
        ed.putString("api_model", etModel.getText().toString().trim());
        ed.apply();
        Toast.makeText(requireContext(), "配置已保存", Toast.LENGTH_SHORT).show();
    }

    private void testModel() {
        final String url = etUrl.getText().toString().trim();
        final String key = etKey.getText().toString().trim();
        final String model = etModel.getText().toString().trim();
        if (url.isEmpty() || key.isEmpty() || model.isEmpty()) {
            tvResult.setText("请填写 Base URL、API Key 和模型名称");
            return;
        }
        btnTest.setEnabled(false);
        tvResult.setText("测试中，请稍候...");
        new Thread(() -> {
            try {
                String chatUrl = url.endsWith("/") ? url + "chat/completions" : url + "/chat/completions";
                URL u = new URL(chatUrl);
                HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + key);
                conn.setDoOutput(true);
                JSONObject body = new JSONObject();
                body.put("model", model);
                JSONArray messages = new JSONArray();
                JSONObject msg = new JSONObject();
                msg.put("role", "user");
                msg.put("content", "你好，请回复：连接成功");
                messages.put(msg);
                body.put("messages", messages);
                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();

                int code = conn.getResponseCode();
                StringBuilder sb = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(
                    code >= 400 ? conn.getErrorStream() : conn.getInputStream()));
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                conn.disconnect();

                String result;
                if (code == 200) {
                    JSONObject resp = new JSONObject(sb.toString());
                    String reply = resp.getJSONArray("choices").getJSONObject(0)
                        .getJSONObject("message").getString("content");
                    result = "连接成功！模型回复：" + reply.trim();
                } else {
                    result = "请求失败 (HTTP " + code + ")：" + sb.toString();
                }
                handler.post(() -> {
                    tvResult.setText(result);
                    btnTest.setEnabled(true);
                });
            } catch (Exception e) {
                handler.post(() -> {
                    tvResult.setText("测试失败：" + e.getMessage());
                    btnTest.setEnabled(true);
                });
            }
        }).start();
    }
}
