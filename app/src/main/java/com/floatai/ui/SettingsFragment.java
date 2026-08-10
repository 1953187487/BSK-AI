package com.floatai.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {
    private EditText etThemeColor;
    private TextView tvFontPath;
    private CheckBox cbShizuku;
    private CheckBox cbDhizuku;
    private String fontPath = "";
    private ActivityResultLauncher<String[]> fontPicker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fontPicker = registerForActivityResult(new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    fontPath = uri.toString();
                    tvFontPath.setText("已选择字体：" + fontPath);
                }
            });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_settings, container, false);
        etThemeColor = v.findViewById(R.id.et_theme_color);
        tvFontPath = v.findViewById(R.id.tv_font_path);
        cbShizuku = v.findViewById(R.id.cb_shizuku);
        cbDhizuku = v.findViewById(R.id.cb_dhizuku);
        Button btnImportFont = v.findViewById(R.id.btn_import_font);
        Button btnSave = v.findViewById(R.id.btn_save_settings);
        Button btnOpenRepo = v.findViewById(R.id.btn_open_repo);
        Button btnCheckUpdate = v.findViewById(R.id.btn_check_update_settings);

        SharedPreferences sp = requireContext().getSharedPreferences("float_ai_prefs", 0);
        etThemeColor.setText(sp.getString("theme_color", "#FF6B6B"));
        cbShizuku.setChecked(sp.getBoolean("shizuku", false));
        cbDhizuku.setChecked(sp.getBoolean("dhizuku", false));

        btnImportFont.setOnClickListener(view -> fontPicker.launch(new String[]{"font/*", "application/x-font-ttf"}));

        btnSave.setOnClickListener(view -> {
            String color = etThemeColor.getText().toString().trim();
            if (!color.startsWith("#") || (color.length() != 7 && color.length() != 9)) {
                Toast.makeText(requireContext(), "颜色格式应为 #RRGGBB 或 #AARRGGBB", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                Color.parseColor(color);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "颜色值无效", Toast.LENGTH_SHORT).show();
                return;
            }
            sp.edit().putString("theme_color", color)
                .putBoolean("shizuku", cbShizuku.isChecked())
                .putBoolean("dhizuku", cbDhizuku.isChecked())
                .putString("font_path", fontPath)
                .apply();
            Toast.makeText(requireContext(), "设置已保存，重启应用后生效", Toast.LENGTH_SHORT).show();
        });

        btnOpenRepo.setOnClickListener(view -> {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.repo_url)));
            startActivity(i);
        });

        btnCheckUpdate.setOnClickListener(view -> {
            Toast.makeText(requireContext(), "正在检查更新...", Toast.LENGTH_SHORT).show();
            new UpdateChecker(requireContext()).check(new UpdateChecker.Callback() {
                @Override
                public void onResult(String latestVersion, String changelog) {
                    String current = "v0.1";
                    try {
                        current = "v" + requireContext().getPackageManager()
                            .getPackageInfo(requireContext().getPackageName(), 0).versionName;
                    } catch (Exception ignored) {}
                    new AlertDialog.Builder(requireContext())
                        .setTitle("最新版本：" + latestVersion)
                        .setMessage("当前版本：" + current + "\n\n更新记录：\n"
                            + (changelog.isEmpty() ? "（无）" : changelog))
                        .setPositiveButton("查看更新", (dialog, which) -> {
                            Intent i = new Intent(Intent.ACTION_VIEW,
                                Uri.parse(getString(R.string.repo_releases_url)));
                            startActivity(i);
                        })
                        .setNegativeButton("关闭", null)
                        .show();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
            });
        });
        return v;
    }
}
