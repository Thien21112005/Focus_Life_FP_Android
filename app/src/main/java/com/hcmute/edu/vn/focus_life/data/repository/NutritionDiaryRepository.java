package com.hcmute.edu.vn.focus_life.data.repository;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hcmute.edu.vn.focus_life.FocusLifeApp;
import com.hcmute.edu.vn.focus_life.core.common.AppExecutors;
import com.hcmute.edu.vn.focus_life.core.exception.AppExceptionLogger;
import com.hcmute.edu.vn.focus_life.core.exception.FirebaseExceptionMapper;
import com.hcmute.edu.vn.focus_life.core.exception.UserFacingException;
import com.hcmute.edu.vn.focus_life.core.session.SessionManager;
import com.hcmute.edu.vn.focus_life.core.storage.LocalNutritionImageStore;
import com.hcmute.edu.vn.focus_life.data.local.dao.NutritionDao;
import com.hcmute.edu.vn.focus_life.data.local.db.AppDatabase;
import com.hcmute.edu.vn.focus_life.data.local.entity.NutritionEntryEntity;
import com.hcmute.edu.vn.focus_life.data.mapper.FirestoreMapper;
import com.hcmute.edu.vn.focus_life.data.remote.cloudinary.CloudinaryImageUploader;
import com.hcmute.edu.vn.focus_life.data.remote.firestore.NutritionRemoteDataSource;

import java.util.List;
import java.util.UUID;

public class NutritionDiaryRepository {

    public interface DayLoadCallback {
        void onLoaded(@NonNull List<NutritionEntryEntity> entries, boolean fromRemote, @Nullable String infoMessage, @Nullable Throwable error);
    }

    public interface EntryMutationCallback {
        void onComplete(@NonNull NutritionEntryEntity entry, @Nullable String infoMessage, @Nullable Throwable error);
    }

    public interface SimpleCallback {
        void onComplete(@Nullable String infoMessage, @Nullable Throwable error);
    }

    private final NutritionDao nutritionDao;
    private final SessionManager sessionManager;
    private final NutritionRemoteDataSource remoteDataSource;
    private final CloudinaryImageUploader cloudinaryImageUploader;
    private final LocalNutritionImageStore localNutritionImageStore;
    private final AppExecutors executors;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public NutritionDiaryRepository() {
        AppDatabase database = FocusLifeApp.getInstance().getDatabase();
        nutritionDao = database.nutritionDao();
        sessionManager = FocusLifeApp.getInstance().getSessionManager();
        remoteDataSource = new NutritionRemoteDataSource();
        cloudinaryImageUploader = new CloudinaryImageUploader();
        localNutritionImageStore = new LocalNutritionImageStore();
        executors = new AppExecutors();
    }

    public void loadEntriesForDate(@NonNull String dateKey, boolean refreshFromRemote, @NonNull DayLoadCallback callback) {
        String uid = sessionManager.requireUid();
        if (TextUtils.isEmpty(uid)) {
            post(() -> callback.onLoaded(java.util.Collections.emptyList(), false, null,
                    new UserFacingException("Bạn cần đăng nhập để xem nhật ký dinh dưỡng.")));
            return;
        }

        executors.diskIO().execute(() -> {
            List<NutritionEntryEntity> localEntries = nutritionDao.getByDate(uid, dateKey);
            post(() -> callback.onLoaded(localEntries, false, null, null));

            if (!refreshFromRemote) return;
            remoteDataSource.fetchNutritionEntriesByDate(uid, dateKey, new NutritionRemoteDataSource.DataCallback<List<NutritionEntryEntity>>() {
                @Override
                public void onSuccess(List<NutritionEntryEntity> data) {
                    executors.diskIO().execute(() -> {
                        mergeRemoteEntries(uid, data);
                        List<NutritionEntryEntity> merged = nutritionDao.getByDate(uid, dateKey);
                        post(() -> callback.onLoaded(merged, true, "Đã làm mới dữ liệu từ cloud cho đúng tài khoản hiện tại.", null));
                    });
                }

                @Override
                public void onError(@NonNull Exception e) {
                    AppExceptionLogger.log("nutrition_load_remote", e);
                    UserFacingException mapped = new UserFacingException(FirebaseExceptionMapper.toUserMessage(e), e);
                    executors.diskIO().execute(() -> {
                        List<NutritionEntryEntity> fallback = nutritionDao.getByDate(uid, dateKey);
                        post(() -> callback.onLoaded(fallback, true, "Đang hiển thị dữ liệu offline đã lưu cho tài khoản này.", mapped));
                    });
                }
            });
        });
    }

    public void saveEntry(@NonNull NutritionEntryEntity entry, @NonNull EntryMutationCallback callback) {
        String uid = sessionManager.requireUid();
        if (TextUtils.isEmpty(uid)) {
            post(() -> callback.onComplete(entry, null, new UserFacingException("Bạn cần đăng nhập trước khi lưu món ăn.")));
            return;
        }

        executors.diskIO().execute(() -> {
            long now = System.currentTimeMillis();
            if (TextUtils.isEmpty(entry.entryUuid)) {
                entry.entryUuid = UUID.randomUUID().toString();
            }
            entry.uid = uid;
            if (entry.createdAt == 0L) entry.createdAt = now;
            entry.updatedAt = now;
            entry.deleted = false;
            entry.synced = false;

            String backupMessage = backupAndUploadImageIfNeeded(entry, uid);
            nutritionDao.upsert(entry);

            remoteDataSource.upsertNutritionEntry(uid, entry.entryUuid, FirestoreMapper.mapNutritionEntry(entry), new NutritionRemoteDataSource.DataCallback<Void>() {
                @Override
                public void onSuccess(Void data) {
                    boolean fullySynced = !requiresImageBackup(entry);
                    executors.diskIO().execute(() -> nutritionDao.markSynced(uid, entry.entryUuid, fullySynced));
                    entry.synced = fullySynced;
                    post(() -> callback.onComplete(entry, buildSuccessMessage(entry, backupMessage), null));
                }

                @Override
                public void onError(@NonNull Exception e) {
                    AppExceptionLogger.log("nutrition_save_remote", e);
                    post(() -> callback.onComplete(entry, buildOfflineFallbackMessage(entry, backupMessage), null));
                }
            });
        });
    }

    public void softDeleteEntry(@NonNull String entryUuid, @NonNull SimpleCallback callback) {
        String uid = sessionManager.requireUid();
        if (TextUtils.isEmpty(uid)) {
            post(() -> callback.onComplete(null, new UserFacingException("Bạn cần đăng nhập trước khi xóa món ăn.")));
            return;
        }

        executors.diskIO().execute(() -> {
            NutritionEntryEntity existing = nutritionDao.getByEntryUuid(uid, entryUuid);
            if (existing == null) {
                post(() -> callback.onComplete(null, new UserFacingException("Không tìm thấy món ăn của tài khoản hiện tại để xóa.")));
                return;
            }

            existing.deleted = true;
            existing.synced = false;
            existing.updatedAt = System.currentTimeMillis();
            nutritionDao.upsert(existing);

            remoteDataSource.upsertNutritionEntry(uid, existing.entryUuid, FirestoreMapper.mapNutritionEntry(existing), new NutritionRemoteDataSource.DataCallback<Void>() {
                @Override
                public void onSuccess(Void data) {
                    executors.diskIO().execute(() -> {
                        nutritionDao.markSynced(uid, existing.entryUuid, true);
                        nutritionDao.pruneDeleted(uid, System.currentTimeMillis() - (2L * 24L * 60L * 60L * 1000L));
                    });
                    post(() -> callback.onComplete("Đã xóa món ăn.", null));
                }

                @Override
                public void onError(@NonNull Exception e) {
                    AppExceptionLogger.log("nutrition_delete_remote", e);
                    post(() -> callback.onComplete("Đã ẩn món ăn trên máy. Cloud sẽ cập nhật lại khi mạng ổn định.", null));
                }
            });
        });
    }

    private void mergeRemoteEntries(@NonNull String uid, @Nullable List<NutritionEntryEntity> remoteEntries) {
        if (remoteEntries == null) return;
        for (NutritionEntryEntity remote : remoteEntries) {
            if (remote == null || TextUtils.isEmpty(remote.entryUuid)) continue;
            remote.uid = uid;
            remote.synced = !requiresImageBackup(remote);
            NutritionEntryEntity local = nutritionDao.getByEntryUuid(uid, remote.entryUuid);
            if (local == null || remote.updatedAt >= local.updatedAt) {
                nutritionDao.upsert(remote);
            }
        }
    }

    @Nullable
    private String backupAndUploadImageIfNeeded(@NonNull NutritionEntryEntity entry, @NonNull String uid) {
        if (TextUtils.isEmpty(entry.imageUri)) {
            return null;
        }

        try {
            String persistedLocalUri = localNutritionImageStore.persistIfNeeded(FocusLifeApp.getInstance(), entry.imageUri, entry.entryUuid);
            if (!TextUtils.isEmpty(persistedLocalUri)) {
                entry.imageUri = persistedLocalUri;
            }
        } catch (UserFacingException e) {
            AppExceptionLogger.log("nutrition_local_image_copy", e);
            return "Đã lưu món ăn nhưng chưa sao chép được ảnh vào bộ nhớ ứng dụng.";
        }

        if (!requiresImageBackup(entry)) {
            return "Ảnh món ăn đã có sẵn trên cloud.";
        }

        if (!cloudinaryImageUploader.isConfigured()) {
            return "Cloudinary chưa được cấu hình upload_preset nên ảnh mới chỉ lưu cục bộ trên máy.";
        }

        try {
            CloudinaryImageUploader.UploadResult uploadResult = cloudinaryImageUploader.uploadNutritionImage(
                    FocusLifeApp.getInstance(),
                    Uri.parse(entry.imageUri),
                    uid,
                    entry.entryDate == null ? "unknown-date" : entry.entryDate,
                    entry.entryUuid
            );
            entry.imageUrl = uploadResult.secureUrl;
            entry.imagePublicId = uploadResult.publicId;
            return "Ảnh món ăn đã được sao lưu lên Cloudinary.";
        } catch (UserFacingException e) {
            AppExceptionLogger.log("nutrition_cloudinary_upload", e);
            return "Chưa upload được ảnh món ăn lên Cloudinary, app sẽ thử sao lưu lại sau.";
        }
    }

    private boolean requiresImageBackup(@NonNull NutritionEntryEntity entry) {
        return !TextUtils.isEmpty(entry.imageUri) && TextUtils.isEmpty(entry.imageUrl);
    }

    @NonNull
    private String buildSuccessMessage(@NonNull NutritionEntryEntity entry, @Nullable String backupMessage) {
        if (requiresImageBackup(entry)) {
            return appendMessages("Đã lưu món ăn và đồng bộ dữ liệu. Ảnh món sẽ tiếp tục sao lưu lên Cloudinary khi mạng ổn định.", backupMessage);
        }
        if (!TextUtils.isEmpty(entry.imageUrl)) {
            return appendMessages("Đã lưu món ăn, sao lưu ảnh lên Cloudinary và đồng bộ cloud.", backupMessage);
        }
        return appendMessages("Đã lưu món ăn và đồng bộ lên cloud cho tài khoản hiện tại.", backupMessage);
    }

    @NonNull
    private String buildOfflineFallbackMessage(@NonNull NutritionEntryEntity entry, @Nullable String backupMessage) {
        if (!TextUtils.isEmpty(entry.imageUrl)) {
            return appendMessages("Đã lưu trên máy và sao lưu ảnh món lên Cloudinary. Dữ liệu món ăn sẽ đồng bộ cloud sau.", backupMessage);
        }
        return appendMessages("Đã lưu trên máy. Khi mạng ổn định hơn app sẽ đồng bộ lại lên cloud.", backupMessage);
    }

    @NonNull
    private String appendMessages(@NonNull String primary, @Nullable String secondary) {
        if (TextUtils.isEmpty(secondary) || primary.contains(secondary)) {
            return primary;
        }
        return primary + " " + secondary;
    }

    private void post(Runnable runnable) {
        mainHandler.post(runnable);
    }
}
