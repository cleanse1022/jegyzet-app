package com.example.jegyzetapp.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.jegyzetapp.R;
import com.example.jegyzetapp.ui.MainActivity;

public class NotificationHelper {
    private static final String CHANNEL_ID = "jegyzet_app_channel";
    private static final String CHANNEL_NAME = "JegyzetApp értesítések";
    private static final String CHANNEL_DESC = "Értesítések a JegyzetApp-tól";

    public static void createNotificationChannel(Context context) {
        // Notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESC);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public static void showNoteReminderNotification(Context context, String title, String content) {
        // Alapértelmezett intent a főképernyőre - ha nincs megadva specifikus PendingIntent
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_IMMUTABLE
        );
        
        // Az eredeti metódust hívjuk meg az új, bővített verzióval
        showNoteReminderNotification(context, title, content, pendingIntent);
    }
    
    public static void showNoteReminderNotification(Context context, String title, String content, PendingIntent customPendingIntent) {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(customPendingIntent)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(1, builder.build());
        } catch (SecurityException e) {
            // Hiányzó értesítési jogosultság esetén
            android.util.Log.e("NotificationHelper", "Notification permission denied", e);
        }
    }
}
