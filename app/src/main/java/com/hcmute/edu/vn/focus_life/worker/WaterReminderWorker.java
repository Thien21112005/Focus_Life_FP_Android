package com.hcmute.edu.vn.focus_life.worker;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.hcmute.edu.vn.focus_life.core.water.WaterReminderScheduler;
import com.hcmute.edu.vn.focus_life.receiver.WaterReminderReceiver;

public class WaterReminderWorker extends Worker {
    public WaterReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context appContext = getApplicationContext();
        boolean activateDefault = getInputData().getBoolean("activate_default", false);
        if (activateDefault) {
            WaterReminderScheduler.ensureDailyDefaultIfNeeded(appContext);
            return Result.success();
        }

        Intent intent = new Intent(appContext, WaterReminderReceiver.class);
        intent.setAction(WaterReminderScheduler.ACTION_REMIND);
        intent.putExtra(WaterReminderScheduler.EXTRA_CUP_INDEX, getInputData().getInt(WaterReminderScheduler.EXTRA_CUP_INDEX, 1));
        intent.putExtra(WaterReminderScheduler.EXTRA_CUP_TOTAL, getInputData().getInt(WaterReminderScheduler.EXTRA_CUP_TOTAL, WaterReminderScheduler.targetGlasses(appContext)));
        intent.putExtra(WaterReminderScheduler.EXTRA_HOUR, getInputData().getInt(WaterReminderScheduler.EXTRA_HOUR, WaterReminderScheduler.startHour(appContext)));
        intent.putExtra(WaterReminderScheduler.EXTRA_MINUTE, getInputData().getInt(WaterReminderScheduler.EXTRA_MINUTE, 0));
        new WaterReminderReceiver().onReceive(appContext, intent);
        return Result.success();
    }
}
