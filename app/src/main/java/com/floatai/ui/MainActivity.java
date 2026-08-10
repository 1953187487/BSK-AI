package com.floatai.ui;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS = "float_ai_prefs";
    private static final String KEY_AGREED = "protocol_agreed";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView nav = findViewById(R.id.nav_bar);
        nav.setOnItemSelectedListener(item -> {
            FrameLayout container = findViewById(R.id.container);
            int id = item.getItemId();
            if (id == R.id.nav_chat) {
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, new ChatFragment()).commit();
            } else if (id == R.id.nav_api) {
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, new ApiFragment()).commit();
            } else if (id == R.id.nav_settings) {
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, new SettingsFragment()).commit();
            }
            return true;
        });

        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (!sp.getBoolean(KEY_AGREED, false)) {
            showProtocolDialog(sp);
        } else {
            nav.setSelectedItemId(R.id.nav_chat);
        }
    }

    private void showProtocolDialog(SharedPreferences sp) {
        String content = getString(R.string.protocol_user_notice) + "\n\n"
            + getString(R.string.protocol_open_source);
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.protocol_title))
            .setMessage(content)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.protocol_agree), (dialog, which) -> {
                sp.edit().putBoolean(KEY_AGREED, true).apply();
                BottomNavigationView nav = findViewById(R.id.nav_bar);
                nav.setSelectedItemId(R.id.nav_chat);
                dialog.dismiss();
            })
            .setNegativeButton(getString(R.string.protocol_exit), (dialog, which) -> finish())
            .setNeutralButton(getString(R.string.protocol_repo), (dialog, which) -> {
                Intent i = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(getString(R.string.repo_url)));
                startActivity(i);
            })
            .show();
    }
}
