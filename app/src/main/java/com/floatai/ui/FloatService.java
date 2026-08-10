package com.floatai.ui;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.core.app.NotificationCompat;

public class FloatService extends Service {
    private static final String CHANNEL_ID = "float_service";
    private static final int NOTIF_ID = 1;
    private WindowManager windowManager;
    private TextView floatView;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FloatAI")
            .setContentText("悬浮窗已开启，查看系统进程")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build();
        startForeground(NOTIF_ID, notification);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (floatView != null) {
            try { windowManager.removeView(floatView); } catch (Exception ignored) {}
            floatView = null;
        }

        floatView = new TextView(this);
        floatView.setText("FloatAI 悬浮窗 - 进程查看");
        floatView.setTextSize(14f);
        floatView.setGravity(Gravity.CENTER);
        floatView.setPadding(24, 24, 24, 24);
        floatView.setBackgroundResource(android.R.drawable.editbox_background);

        int type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100;
        params.y = 200;
        try {
            windowManager.addView(floatView, params);
        } catch (Exception e) {
            stopSelf();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (floatView != null && windowManager != null) {
            try { windowManager.removeView(floatView); } catch (Exception ignored) {}
        }
        super.onDestroy();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "悬浮窗服务", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("FloatAI 悬浮窗后台服务");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
