package com.floatai.ui;

import android.os.Bundle;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView nav = findViewById(R.id.nav_bar);
        nav.setOnItemSelectedListener(item -> {
            FrameLayout container = findViewById(R.id.container);
            if (item.getItemId() == R.id.nav_chat) {
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, new ChatFragment()).commit();
            } else if (item.getItemId() == R.id.nav_api) {
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, new ApiFragment()).commit();
            } else if (item.getItemId() == R.id.nav_settings) {
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, new SettingsFragment()).commit();
            }
            return true;
        });
        nav.setSelectedItemId(R.id.nav_chat);
    }
}
