package com.floatai.ui;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.view.WindowManager;
import android.widget.TextView;

public class FloatService extends Service {
    private WindowManager windowManager;
    private TextView floatView;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        floatView = new TextView(this);
        floatView.setText("FloatAI 悬浮窗 - 进程查看");
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT);
        params.x = 100; params.y = 200;
        windowManager.addView(floatView, params);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatView != null) windowManager.removeView(floatView);
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
