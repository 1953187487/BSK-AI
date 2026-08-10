package com.floatai.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_settings, container, false);
        CheckBox cbShizuku = v.findViewById(R.id.cb_shizuku);
        CheckBox cbDhizuku = v.findViewById(R.id.cb_dhizuku);
        EditText etThemeColor = v.findViewById(R.id.et_theme_color);
        TextView tvFont = v.findViewById(R.id.tv_font_path);
        // 支持 Shizuku / Dhizuku 授权、UI 颜色、字体导入
        return v;
    }
}
