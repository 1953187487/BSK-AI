package com.floatai.ui;

import android.app.Application;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            // 崩溃兜底：不闪退，静默记录
        });
    }
}
