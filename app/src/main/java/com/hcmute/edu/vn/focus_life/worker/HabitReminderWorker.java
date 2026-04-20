package com.hcmute.edu.vn.focus_life.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class HabitReminderWorker extends Worker {
    public HabitReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // TODO: doc habits co reminder_time phu hop va tao notification
        return Result.success();
    }
}
