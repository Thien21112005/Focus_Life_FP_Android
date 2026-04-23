package com.hcmute.edu.vn.focus_life.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.hcmute.edu.vn.focus_life.data.repository.SyncRepository;

public class FirebaseSyncWorker extends Worker {
    public FirebaseSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            SyncRepository repository = new SyncRepository();
            repository.syncUnsyncedDailySummaries();
            repository.syncUnsyncedPomodoroSessions();
            repository.syncUnsyncedStepRecords();
            repository.syncUnsyncedNutritionEntries();
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
