package com.floatai.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

public class ApiFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_api, container, false);
        EditText etUrl = v.findViewById(R.id.api_url);
        EditText etKey = v.findViewById(R.id.api_key);
        Button btnTest = v.findViewById(R.id.btn_test);
        TextView tvResult = v.findViewById(R.id.tv_result);
        btnTest.setOnClickListener(view -> {
            String url = etUrl.getText().toString();
            String key = etKey.getText().toString();
            if (url.isEmpty() || key.isEmpty()) {
                tvResult.setText("URL 和 Key 不能为空");
                return;
            }
            tvResult.setText("测试中... 支持自定义服务商 API（所有模型）");
        });
        return v;
    }
}
