package com.hcmute.edu.vn.focus_life.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.hcmute.edu.vn.focus_life.core.motivation.MotivationNotificationHelper;
import com.hcmute.edu.vn.focus_life.core.motivation.MotivationPreferences;
import com.hcmute.edu.vn.focus_life.core.motivation.MotivationQuote;
import com.hcmute.edu.vn.focus_life.core.motivation.MotivationReminderScheduler;
import com.hcmute.edu.vn.focus_life.core.motivation.MotivationRepository;

import java.util.Random;

public class MotivationReminderReceiver extends BroadcastReceiver {
    private static final String TAG = "MotivationAlarm";
    private static final long DUPLICATE_WINDOW_MILLIS = 2 * 60 * 1000L;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        MotivationPreferences preferences = new MotivationPreferences(context);
        String action = intent.getAction();
        Log.d(TAG, "Receiver action=" + action);
        if (isRescheduleAction(action)) {
            MotivationReminderScheduler.scheduleConfiguredDailyReminder(context);
            return;
        }

        if (Intent.ACTION_USER_PRESENT.equals(action)
                || MotivationReminderScheduler.ACTION_UNLOCK_REMINDER.equals(action)) {
            if (!preferences.isEnabled()) return;
            MotivationNotificationHelper.showMotivationNotification(context, randomQuote(preferences));
            return;
        }

        if (MotivationReminderScheduler.ACTION_MOTIVATION_REMINDER.equals(action)) {
            if (!preferences.isEnabled()) return;
            int slot = intent.getIntExtra(MotivationReminderScheduler.EXTRA_SLOT, 0);
            if (preferences.wasRecentlyNotified(slot, DUPLICATE_WINDOW_MILLIS)) {
                MotivationReminderScheduler.scheduleNextForSlot(context, slot);
                return;
            }
            MotivationQuote quote = randomQuote(preferences);
            MotivationNotificationHelper.showMotivationNotification(context, quote);
            preferences.markSlotNotifiedNow(slot);
            MotivationReminderScheduler.scheduleNextForSlot(context, slot);
        }
    }

    private boolean isRescheduleAction(String action) {
        return Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || MotivationReminderScheduler.ACTION_BOOT_RESCHEDULE.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)
                || "com.htc.intent.action.QUICKBOOT_POWERON".equals(action);
    }

    private MotivationQuote randomQuote(MotivationPreferences preferences) {
        int count = MotivationRepository.getQuoteCount();
        int current = preferences.getQuoteCursor();
        int index = new Random(System.currentTimeMillis()).nextInt(Math.max(1, count));
        if (count > 1 && index == current) {
            index = (index + 1) % count;
        }
        while (preferences.getQuoteCursor() != index) {
            preferences.nextQuoteCursor(count);
        }
        return MotivationRepository.getQuoteByIndex(index);
    }
}
