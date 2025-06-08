package com.example.myapplication;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.app.Activity;
import android.Manifest;


public class NotificationPermissionHelper {
    public static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;

    public static boolean areNotificationsEnabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            return nm != null && nm.areNotificationsEnabled();
        }
        return true;
    }

    public static void requestNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST_CODE
            );
        }
    }
}
