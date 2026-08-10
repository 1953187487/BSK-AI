package com.floatai.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    private View root;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        root = findViewById(android.R.id.content);

        try {
            TextView tvVersion = findViewById(R.id.splash_version);
            tvVersion.setText("v" + getPackageManager()
                .getPackageInfo(getPackageName(), 0).versionName);
        } catch (Exception ignored) {}

        View logo = findViewById(R.id.splash_logo);
        View sub = findViewById(R.id.splash_sub);
        Animation in = AnimationUtils.loadAnimation(this, R.anim.splash_in);
        logo.startAnimation(in);
        sub.startAnimation(in);

        handler.postDelayed(() -> {
            Animation out = AnimationUtils.loadAnimation(this, R.anim.splash_out);
            root.startAnimation(out);
            handler.postDelayed(() -> {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }, 400);
        }, 1500);
    }
}
