package com.hcmute.edu.vn.focus_life.receiver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.core.utils.Constants;
import com.hcmute.edu.vn.focus_life.core.water.WaterReminderScheduler;
import com.hcmute.edu.vn.focus_life.ui.MainActivity;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class WaterReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "water_reminder_alarm_channel";
    private static final int NOTIFICATION_ID = 7208;
    private static final String ACTION_LOG_WATER = "com.hcmute.edu.vn.focus_life.action.LOG_WATER_FROM_REMINDER";
    private static final String ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
    private static final long[] VIBRATION_PATTERN = new long[]{0, 550, 180, 550, 180, 700};

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? "" : intent.getAction();
        if (ACTION_BOOT_COMPLETED.equals(action)) {
            WaterReminderScheduler.rescheduleSavedPlan(context);
            return;
        }

        if (ACTION_LOG_WATER.equals(action)) {
            int cupIndex = readCupIndex(intent);
            logWaterCup(context, cupIndex, true);
            WaterReminderScheduler.markCupLogged(context, cupIndex);
            showLoggedNotification(context, cupIndex);
            return;
        }

        if (!WaterReminderScheduler.shouldNotifyNow(context)) return;
        int cupIndex = readCupIndex(intent);
        int cupTotal = intent == null ? WaterReminderScheduler.targetGlasses(context) : intent.getIntExtra(WaterReminderScheduler.EXTRA_CUP_TOTAL, WaterReminderScheduler.targetGlasses(context));
        int hour = intent == null ? 8 : intent.getIntExtra(WaterReminderScheduler.EXTRA_HOUR, 8);
        int minute = intent == null ? 0 : intent.getIntExtra(WaterReminderScheduler.EXTRA_MINUTE, 0);

        if (WaterReminderScheduler.isCupLogged(context, cupIndex)) {
            WaterReminderScheduler.scheduleNextDayForCup(context, cupIndex, cupTotal, hour, minute);
            return;
        }

        wakeScreenBriefly(context);
        ensureChannel(context);
        showReminderNotification(context, cupIndex, cupTotal, hour, minute);
        WaterReminderScheduler.scheduleNextDayForCup(context, cupIndex, cupTotal, hour, minute);
    }

    private void showReminderNotification(Context context, int cupIndex, int cupTotal, int hour, int minute) {
        PendingIntent openIntent = openHealthPendingIntent(context, 7209);
        PendingIntent logIntent = logWaterPendingIntent(context, cupIndex);
        String title = "Đến giờ uống ly nước thứ " + cupIndex;
        String text = String.format(Locale.getDefault(), "%02d:%02d · Hôm nay cần %d ly. Uống một ly nước để giữ năng lượng ổn định nhé.", hour, minute, cupTotal);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(openIntent)
                .addAction(R.mipmap.ic_launcher, "Đã uống", logIntent)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                .setVibrate(VIBRATION_PATTERN)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(openIntent, true);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(NOTIFICATION_ID + cupIndex, builder.build());
    }

    private void showLoggedNotification(Context context, int cupIndex) {
        ensureChannel(context);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Đã ghi nhận nước")
                .setContentText("FocusLife đã lưu ly nước thứ " + cupIndex + " hôm nay.")
                .setContentIntent(openHealthPendingIntent(context, 7210))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(NOTIFICATION_ID + 100 + cupIndex, builder.build());
    }

    private void logWaterCup(Context context, int cupIndex, boolean fromNotification) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String today = WaterReminderScheduler.todayKey();
        String monthKey = today.length() >= 7 ? today.substring(0, 7) : today;
        long now = System.currentTimeMillis();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(Constants.FIRESTORE_USERS)
                .document(user.getUid())
                .collection("water_records")
                .document(today)
                .get()
                .addOnSuccessListener(document -> {
                    int current = 0;
                    if (document != null && document.exists() && document.get("glasses") instanceof Number) {
                        current = ((Number) document.get("glasses")).intValue();
                    }
                    if (current >= 8) return;
                    int next = current + 1;
                    Map<String, Object> data = new HashMap<>();
                    data.put("date", today);
                    data.put("monthKey", monthKey);
                    data.put("glasses", next);
                    data.put("milliliters", next * 250);
                    data.put("targetGlasses", 8);
                    data.put("targetMilliliters", 2000);
                    data.put("completed", next >= 8);
                    data.put("lastCupIndex", cupIndex);
                    data.put("source", fromNotification ? "water_reminder_notification" : "manual");
                    data.put("updatedAt", now);
                    db.collection(Constants.FIRESTORE_USERS)
                            .document(user.getUid())
                            .collection("water_records")
                            .document(today)
                            .set(data, SetOptions.merge());

                    Map<String, Object> detail = new HashMap<>();
                    detail.put("date", today);
                    detail.put("monthKey", monthKey);
                    detail.put("cupIndex", next);
                    detail.put("scheduledCupIndex", cupIndex);
                    detail.put("milliliters", 250);
                    detail.put("source", fromNotification ? "notification" : "manual");
                    detail.put("createdAt", now);
                    db.collection(Constants.FIRESTORE_USERS)
                            .document(user.getUid())
                            .collection("water_record_details")
                            .document(today + "_cup_" + next)
                            .set(detail, SetOptions.merge());
                });
    }

    private PendingIntent openHealthPendingIntent(Context context, int requestCode) {
        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.putExtra(MainActivity.EXTRA_START_TAB, MainActivity.TAB_HEALTH);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getActivity(context, requestCode, openIntent, flags);
    }

    private PendingIntent logWaterPendingIntent(Context context, int cupIndex) {
        Intent intent = new Intent(context, WaterReminderReceiver.class);
        intent.setAction(ACTION_LOG_WATER);
        intent.putExtra(WaterReminderScheduler.EXTRA_CUP_INDEX, cupIndex);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context, 7300 + cupIndex, intent, flags);
    }

    private int readCupIndex(Intent intent) {
        if (intent == null) return 1;
        return Math.max(1, intent.getIntExtra(WaterReminderScheduler.EXTRA_CUP_INDEX, 1));
    }

    private void wakeScreenBriefly(Context context) {
        try {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (powerManager == null) return;
            int flags = PowerManager.PARTIAL_WAKE_LOCK;
            if (Build.VERSION.SDK_INT < 33) {
                flags = PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE;
            }
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(flags, "FocusLife:WaterReminderWakeLock");
            wakeLock.acquire(8000L);
        } catch (Exception ignored) {
            // Notification still works even if the device blocks wake-up behavior.
        }
    }

    private void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Nhắc uống nước",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Nhắc uống nước theo từng ly trong ngày");
        channel.enableVibration(true);
        channel.setVibrationPattern(VIBRATION_PATTERN);
        channel.setSound(soundUri, audioAttributes);
        channel.enableLights(true);
        manager.createNotificationChannel(channel);
    }
}
