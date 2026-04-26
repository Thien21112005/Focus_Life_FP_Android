package com.hcmute.edu.vn.focus_life.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.hcmute.edu.vn.focus_life.core.motivation.MotivationNotificationHelper;
import com.hcmute.edu.vn.focus_life.core.motivation.MotivationPreferences;
import com.hcmute.edu.vn.focus_life.core.motivation.MotivationQuote;
import com.hcmute.edu.vn.focus_life.core.motivation.MotivationReminderScheduler;
import com.hcmute.edu.vn.focus_life.core.motivation.MotivationRepository;

public class MotivationReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        MotivationPreferences preferences = new MotivationPreferences(context);
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || MotivationReminderScheduler.ACTION_BOOT_RESCHEDULE.equals(action)) {
            MotivationReminderScheduler.scheduleConfiguredDailyReminder(context);
            return;
        }

        if (Intent.ACTION_USER_PRESENT.equals(action)
                || MotivationReminderScheduler.ACTION_UNLOCK_REMINDER.equals(action)) {
            if (!preferences.isEnabled()) return;
            MotivationNotificationHelper.showMotivationNotification(context, MotivationRepository.getQuoteForNow());
            return;
        }

        if (MotivationReminderScheduler.ACTION_MOTIVATION_REMINDER.equals(action)) {
            if (!preferences.isEnabled()) return;
            int slot = intent.getIntExtra(MotivationReminderScheduler.EXTRA_SLOT, 0);
            MotivationQuote quote = MotivationRepository.getQuoteForSlot(slot);
            MotivationNotificationHelper.showMotivationNotification(context, quote);
            MotivationReminderScheduler.scheduleNextForSlot(context, slot);
        }
    }
}
