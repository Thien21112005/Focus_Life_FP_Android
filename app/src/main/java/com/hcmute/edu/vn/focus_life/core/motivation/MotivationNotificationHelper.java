package com.hcmute.edu.vn.focus_life.core.motivation;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.core.session.OnboardingPreferences;
import com.hcmute.edu.vn.focus_life.ui.MainActivity;

public final class MotivationNotificationHelper {
    public static final String CHANNEL_ID = "focuslife_motivation";

    private MotivationNotificationHelper() {}

    public static void showMotivationNotification(Context context, MotivationQuote quote) {
        createChannel(context);

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.putExtra(MainActivity.EXTRA_START_TAB, MainActivity.TAB_HOME);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                context,
                8301,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String displayName = new OnboardingPreferences(context).getDisplayName();
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(MotivationRepository.getNotificationTitle(displayName))
                .setContentText(quote.getText())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(quote.getText()))
                .setContentIntent(openPendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setSound(sound)
                .setVibrate(new long[]{0, 180, 90, 180})
                .setColor(Color.rgb(70, 72, 212));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationManagerCompat.from(context).notify(8300, builder.build());
    }

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "FocusLife Motivation",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Nhắc động lực sức khỏe và tập trung mỗi ngày");
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{0, 180, 90, 180});
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        channel.setSound(sound, null);
        manager.createNotificationChannel(channel);
    }
}
