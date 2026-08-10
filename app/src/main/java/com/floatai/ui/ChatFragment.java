package com.floatai.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ChatFragment extends Fragment {
    private Switch floatSwitch;
    private TextView tvProcess;
    private TextView tvVersion;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_chat, container, false);
        floatSwitch = v.findViewById(R.id.float_switch);
        tvProcess = v.findViewById(R.id.tv_process);
        tvVersion = v.findViewById(R.id.tv_version);
        Button btnCheckUpdate = v.findViewById(R.id.btn_check_update);
        Button btnProcess = v.findViewById(R.id.btn_process);

        floatSwitch.setChecked(isFloatServiceRunning());
        floatSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(requireContext())) {
                    floatSwitch.setChecked(false);
                    Toast.makeText(requireContext(), "需要悬浮窗权限", Toast.LENGTH_SHORT).show();
                    openOverlayPermission();
                } else {
                    requireContext().startForegroundService(new Intent(requireContext(), FloatService.class));
                }
            } else {
                requireContext().stopService(new Intent(requireContext(), FloatService.class));
            }
        });

        btnProcess.setOnClickListener(view -> showProcesses());
        btnCheckUpdate.setOnClickListener(view -> checkUpdate());

        try {
            tvVersion.setText("当前版本 v" + requireContext().getPackageManager()
                .getPackageInfo(requireContext().getPackageName(), 0).versionName);
        } catch (Exception ignored) {}
        return v;
    }

    private boolean isFloatServiceRunning() {
        return false;
    }

    private void openOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + requireContext().getPackageName()));
        startActivity(intent);
    }

    private void openUpdatePage() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.repo_releases_url)));
        startActivity(intent);
    }

    private void checkUpdate() {
        Toast.makeText(requireContext(), "正在检查更新...", Toast.LENGTH_SHORT).show();
        new UpdateChecker(requireContext()).check(new UpdateChecker.Callback() {
            @Override
            public void onResult(String latestVersion, String changelog) {
                String current = "v0.1";
                try {
                    current = "v" + requireContext().getPackageManager()
                        .getPackageInfo(requireContext().getPackageName(), 0).versionName;
                } catch (Exception ignored) {}
                if (latestVersion.equalsIgnoreCase(current)) {
                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("已是最新版本")
                        .setMessage("当前版本 " + current + "，无需更新。")
                        .setPositiveButton("确定", null)
                        .show();
                } else {
                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("发现新版本 " + latestVersion)
                        .setMessage("更新记录：\n" + (changelog.isEmpty() ? "（无）" : changelog)
                            + "\n\n点击查看更新页面")
                        .setPositiveButton("去更新", (dialog, which) -> openUpdatePage())
                        .setNegativeButton("取消", null)
                        .show();
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                openUpdatePage();
            }
        });
    }

    private void showProcesses() {
        StringBuilder sb = new StringBuilder();
        try {
            Process p = Runtime.getRuntime().exec("ps -A");
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            int count = 0;
            while ((line = br.readLine()) != null && count < 30) {
                sb.append(line).append("\n");
                count++;
            }
            br.close();
        } catch (Exception e) {
            sb.append("无权限读取进程，请授予 Shizuku / Dhizuku 权限");
        }
        tvProcess.setText(sb.length() > 0 ? sb.toString() : "无进程信息");
    }
}
