package com.hcmute.edu.vn.focus_life.worker;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.hcmute.edu.vn.focus_life.core.motivation.MotivationReminderScheduler;
import com.hcmute.edu.vn.focus_life.receiver.MotivationReminderReceiver;

public class MotivationReminderWorker extends Worker {
    public MotivationReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context appContext = getApplicationContext();
        Intent intent = new Intent(appContext, MotivationReminderReceiver.class);
        intent.setAction(MotivationReminderScheduler.ACTION_MOTIVATION_REMINDER);
        intent.putExtra(MotivationReminderScheduler.EXTRA_SLOT, getInputData().getInt(MotivationReminderScheduler.EXTRA_SLOT, 0));
        new MotivationReminderReceiver().onReceive(appContext, intent);
        return Result.success();
    }
}
