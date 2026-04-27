package com.hcmute.edu.vn.focus_life.receiver;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.core.focus.PomodoroAlarmScheduler;
import com.hcmute.edu.vn.focus_life.core.utils.NotificationHelper;
import com.hcmute.edu.vn.focus_life.data.repository.AppNotificationRepository;
import com.hcmute.edu.vn.focus_life.service.PomodoroService;
import com.hcmute.edu.vn.focus_life.ui.MainActivity;

public class PomodoroAlarmReceiver extends BroadcastReceiver {
    private static final int NOTIFICATION_ID = 9202;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !PomodoroAlarmScheduler.ACTION_COMPLETE.equals(intent.getAction())) return;
        String taskTitle = intent.getStringExtra(PomodoroAlarmScheduler.EXTRA_TASK_TITLE);
        if (taskTitle == null || taskTitle.trim().isEmpty()) taskTitle = "Phiên Focus";
        int durationMinutes = intent.getIntExtra(PomodoroAlarmScheduler.EXTRA_DURATION_MINUTES, 25);

        Intent stopIntent = new Intent(context, PomodoroService.class);
        stopIntent.setAction(PomodoroService.ACTION_STOP);
        context.stopService(stopIntent);

        showCompletedNotification(context, taskTitle, durationMinutes);
    }

    private void showCompletedNotification(Context context, String taskTitle, int durationMinutes) {
        NotificationHelper.createChannels(context);
        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.putExtra(MainActivity.EXTRA_START_TAB, MainActivity.TAB_FOCUS);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent openPendingIntent = PendingIntent.getActivity(context, 9203, openIntent, flags);

        String title = "Focus hoàn thành rồi";
        String text = taskTitle + " · " + durationMinutes + " phút tập trung đã xong. Nghỉ nhẹ một chút nha.";
        Notification notification = new NotificationCompat.Builder(context, NotificationHelper.FOCUS_REMINDER_CHANNEL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(openPendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .build();

        AppNotificationRepository.log(AppNotificationRepository.TYPE_FOCUS, title, text, NotificationHelper.FOCUS_REMINDER_CHANNEL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            try {
                manager.notify(NOTIFICATION_ID, notification);
            } catch (SecurityException ignored) {
                // Android 13+ can block notifications when the permission is revoked.
            }
        }
    }
}
