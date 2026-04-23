package com.hcmute.edu.vn.focus_life.data.repository;

import android.net.Uri;
import android.text.TextUtils;

import com.hcmute.edu.vn.focus_life.FocusLifeApp;
import com.hcmute.edu.vn.focus_life.core.exception.AppExceptionLogger;
import com.hcmute.edu.vn.focus_life.core.exception.UserFacingException;
import com.hcmute.edu.vn.focus_life.core.session.SessionManager;
import com.hcmute.edu.vn.focus_life.data.local.db.AppDatabase;
import com.hcmute.edu.vn.focus_life.data.local.entity.DailySummaryEntity;
import com.hcmute.edu.vn.focus_life.data.local.entity.NutritionEntryEntity;
import com.hcmute.edu.vn.focus_life.data.local.entity.PomodoroSessionEntity;
import com.hcmute.edu.vn.focus_life.data.local.entity.StepRecordEntity;
import com.hcmute.edu.vn.focus_life.data.mapper.FirestoreMapper;
import com.hcmute.edu.vn.focus_life.data.remote.cloudinary.CloudinaryImageUploader;
import com.hcmute.edu.vn.focus_life.data.remote.firestore.ActivityRemoteDataSource;
import com.hcmute.edu.vn.focus_life.data.remote.firestore.NutritionRemoteDataSource;

import java.util.ArrayList;
import java.util.List;

public class SyncRepository {
    private final AppDatabase database;
    private final SessionManager sessionManager;
    private final ActivityRemoteDataSource activityRemoteDataSource;
    private final NutritionRemoteDataSource nutritionRemoteDataSource;
    private final CloudinaryImageUploader cloudinaryImageUploader;

    public SyncRepository() {
        database = FocusLifeApp.getInstance().getDatabase();
        sessionManager = FocusLifeApp.getInstance().getSessionManager();
        activityRemoteDataSource = new ActivityRemoteDataSource();
        nutritionRemoteDataSource = new NutritionRemoteDataSource();
        cloudinaryImageUploader = new CloudinaryImageUploader();
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

    public void syncUnsyncedNutritionEntries() {
        String uid = sessionManager.requireUid();
        if (uid == null) return;

        List<NutritionEntryEntity> unsynced = database.nutritionDao().getUnsynced(uid);
        if (unsynced == null || unsynced.isEmpty()) return;

        for (NutritionEntryEntity entity : unsynced) {
            tryUploadImage(entity, uid);
            nutritionRemoteDataSource.upsertNutritionEntry(
                    uid,
                    entity.entryUuid,
                    FirestoreMapper.mapNutritionEntry(entity)
            );
            entity.synced = !needsImageBackup(entity);
            database.nutritionDao().upsert(entity);
        }
    }

    private void tryUploadImage(NutritionEntryEntity entity, String uid) {
        if (!needsImageBackup(entity)) {
            return;
        }
        if (!cloudinaryImageUploader.isConfigured()) {
            return;
        }
        try {
            CloudinaryImageUploader.UploadResult result = cloudinaryImageUploader.uploadNutritionImage(
                    FocusLifeApp.getInstance(),
                    Uri.parse(entity.imageUri),
                    uid,
                    entity.entryDate == null ? "unknown-date" : entity.entryDate,
                    entity.entryUuid
            );
            entity.imageUrl = result.secureUrl;
            entity.imagePublicId = result.publicId;
        } catch (UserFacingException e) {
            AppExceptionLogger.log("nutrition_sync_cloudinary", e);
        } catch (Exception e) {
            AppExceptionLogger.log("nutrition_sync_cloudinary_unknown", e);
        }
    }

    private boolean needsImageBackup(NutritionEntryEntity entity) {
        return !TextUtils.isEmpty(entity.imageUri) && TextUtils.isEmpty(entity.imageUrl);
    }
}
