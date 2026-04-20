package com.hcmute.edu.vn.focus_life.data.repository;

import com.hcmute.edu.vn.focus_life.FocusLifeApp;
import com.hcmute.edu.vn.focus_life.core.session.SessionManager;
import com.hcmute.edu.vn.focus_life.data.local.db.AppDatabase;
import com.hcmute.edu.vn.focus_life.data.local.entity.DailySummaryEntity;
import com.hcmute.edu.vn.focus_life.data.local.entity.PomodoroSessionEntity;
import com.hcmute.edu.vn.focus_life.data.local.entity.StepRecordEntity;
import com.hcmute.edu.vn.focus_life.data.mapper.FirestoreMapper;
import com.hcmute.edu.vn.focus_life.data.remote.firestore.ActivityRemoteDataSource;

import java.util.ArrayList;
import java.util.List;

public class SyncRepository {
    private final AppDatabase database;
    private final SessionManager sessionManager;
    private final ActivityRemoteDataSource activityRemoteDataSource;

    public SyncRepository() {
        database = FocusLifeApp.getInstance().getDatabase();
        sessionManager = FocusLifeApp.getInstance().getSessionManager();
        activityRemoteDataSource = new ActivityRemoteDataSource();
    }

    public void syncUnsyncedDailySummaries() {
        String uid = sessionManager.requireUid();
        if (uid == null) return;

        for (DailySummaryEntity entity : database.dailySummaryDao().getUnsynced()) {
            activityRemoteDataSource.upsertDailySummary(
                    uid,
                    entity.date,
                    FirestoreMapper.mapDailySummary(entity)
            );
            entity.synced = true;
            database.dailySummaryDao().upsert(entity);
        }
    }

    public void syncUnsyncedPomodoroSessions() {
        String uid = sessionManager.requireUid();
        if (uid == null) return;

        for (PomodoroSessionEntity entity : database.pomodoroDao().getUnsynced()) {
            activityRemoteDataSource.upsertPomodoroSession(
                    uid,
                    entity.sessionUuid,
                    FirestoreMapper.mapPomodoroSession(entity)
            );
            entity.synced = true;
            database.pomodoroDao().insert(entity);
        }
    }

    public void syncUnsyncedStepRecords() {
        String uid = sessionManager.requireUid();
        if (uid == null) return;

        List<StepRecordEntity> unsynced = database.stepDao().getUnsynced();
        if (unsynced == null || unsynced.isEmpty()) return;

        List<Long> syncedIds = new ArrayList<>();
        for (StepRecordEntity entity : unsynced) {
            activityRemoteDataSource.upsertStepRecord(
                    uid,
                    String.valueOf(entity.id),
                    FirestoreMapper.mapStepRecord(entity)
            );
            syncedIds.add(entity.id);
        }

        if (!syncedIds.isEmpty()) {
            database.stepDao().markSynced(syncedIds);
        }
    }
}