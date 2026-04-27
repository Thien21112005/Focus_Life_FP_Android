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
import com.hcmute.edu.vn.focus_life.core.focus.FocusTaskStartReminderScheduler;
import com.hcmute.edu.vn.focus_life.core.utils.NotificationHelper;
import com.hcmute.edu.vn.focus_life.data.repository.AppNotificationRepository;
import com.hcmute.edu.vn.focus_life.ui.MainActivity;
import com.hcmute.edu.vn.focus_life.ui.focus.FocusTaskDetailActivity;

public class FocusTaskStartReminderReceiver extends BroadcastReceiver {
    private static final int NOTIFICATION_BASE_ID = 9400;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !FocusTaskStartReminderScheduler.ACTION_TASK_START_REMINDER.equals(intent.getAction())) {
            return;
        }

        String taskId = intent.getStringExtra(FocusTaskStartReminderScheduler.EXTRA_TASK_ID);
        String taskTitle = intent.getStringExtra(FocusTaskStartReminderScheduler.EXTRA_TASK_TITLE);
        if (taskTitle == null || taskTitle.trim().isEmpty()) {
            taskTitle = "Task Focus";
        }

        showStartNotification(context, taskId, taskTitle);
    }

    private void showStartNotification(Context context, String taskId, String taskTitle) {
        NotificationHelper.createChannels(context);

        Intent openIntent;
        if (taskId != null && !taskId.trim().isEmpty()) {
            openIntent = new Intent(context, FocusTaskDetailActivity.class);
            openIntent.putExtra(FocusTaskDetailActivity.EXTRA_TASK_ID, taskId);
        } else {
            openIntent = new Intent(context, MainActivity.class);
            openIntent.putExtra(MainActivity.EXTRA_START_TAB, MainActivity.TAB_FOCUS);
        }
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int requestCode = taskId == null ? NOTIFICATION_BASE_ID : FocusTaskStartReminderScheduler.requestCode(taskId);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent openPendingIntent = PendingIntent.getActivity(context, requestCode, openIntent, flags);

        String title = "Đến giờ bắt đầu Focus";
        String text = taskTitle + " đã đến giờ bắt đầu. Mở task và bắt đầu Pomodoro nhé.";

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
                manager.notify(NOTIFICATION_BASE_ID + (requestCode % 1000), notification);
            } catch (SecurityException ignored) {
                // Android 13+ can block notifications when permission is revoked.
            }
        }
    }
}
