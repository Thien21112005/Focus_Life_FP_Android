package com.hcmute.edu.vn.focus_life.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.hcmute.edu.vn.focus_life.core.utils.Constants;
import com.hcmute.edu.vn.focus_life.domain.model.FocusTask;

import java.util.ArrayList;
import java.util.List;

public class FocusTaskRepository {

    public interface TasksCallback {
        void onLoaded(@NonNull List<FocusTask> tasks);
    }

    public interface TaskCallback {
        void onLoaded(@Nullable FocusTask task);
    }

    public interface ActionCallback {
        void onComplete(boolean success, @Nullable Exception error);
    }

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    private CollectionReference collection(String uid) {
        return firestore.collection(Constants.FIRESTORE_USERS)
                .document(uid)
                .collection(Constants.FIRESTORE_FOCUS_TASKS);
    }

    public void loadTasks(@Nullable String uid, @NonNull TasksCallback callback) {
        if (uid == null || uid.trim().isEmpty()) {
            callback.onLoaded(new ArrayList<>());
            return;
        }

        collection(uid)
                .orderBy("dueAt")
                .get()
                .addOnSuccessListener(result -> {
                    List<FocusTask> tasks = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot snapshot : result.getDocuments()) {
                        FocusTask task = FocusTask.fromSnapshot(snapshot);
                        if (task != null && !task.deleted) {
                            tasks.add(task);
                        }
                    }
                    callback.onLoaded(tasks);
                })
                .addOnFailureListener(error -> callback.onLoaded(new ArrayList<>()));
    }

    public void getTask(@Nullable String uid, @Nullable String taskId, @NonNull TaskCallback callback) {
        if (uid == null || uid.trim().isEmpty() || taskId == null || taskId.trim().isEmpty()) {
            callback.onLoaded(null);
            return;
        }

        collection(uid)
                .document(taskId)
                .get()
                .addOnSuccessListener(snapshot -> callback.onLoaded(FocusTask.fromSnapshot(snapshot)))
                .addOnFailureListener(error -> callback.onLoaded(null));
    }

    @Nullable
    public ListenerRegistration observeTasks(@Nullable String uid, @NonNull TasksCallback callback) {
        if (uid == null || uid.trim().isEmpty()) {
            callback.onLoaded(new ArrayList<>());
            return null;
        }

        return collection(uid)
                .orderBy("dueAt")
                .addSnapshotListener((result, error) -> {
                    if (error != null || result == null) {
                        callback.onLoaded(new ArrayList<>());
                        return;
                    }

                    List<FocusTask> tasks = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot snapshot : result.getDocuments()) {
                        FocusTask task = FocusTask.fromSnapshot(snapshot);
                        if (task != null && !task.deleted) {
                            tasks.add(task);
                        }
                    }
                    callback.onLoaded(tasks);
                });
    }

    @Nullable
    public ListenerRegistration observeTask(@Nullable String uid,
                                            @Nullable String taskId,
                                            @NonNull TaskCallback callback) {
        if (uid == null || uid.trim().isEmpty() || taskId == null || taskId.trim().isEmpty()) {
            callback.onLoaded(null);
            return null;
        }

        return collection(uid)
                .document(taskId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || !snapshot.exists()) {
                        callback.onLoaded(null);
                        return;
                    }

                    FocusTask task = FocusTask.fromSnapshot(snapshot);
                    if (task != null && !task.deleted) {
                        callback.onLoaded(task);
                    } else {
                        callback.onLoaded(null);
                    }
                });
    }

    public void saveTask(@Nullable String uid, @NonNull FocusTask task, @Nullable ActionCallback callback) {
        if (uid == null || uid.trim().isEmpty()) {
            if (callback != null) callback.onComplete(false, new IllegalStateException("Missing uid"));
            return;
        }

        if (task.id == null || task.id.trim().isEmpty()) {
            task.id = collection(uid).document().getId();
        }

        long now = System.currentTimeMillis();
        if (task.createdAt <= 0L) task.createdAt = now;
        task.updatedAt = now;
        task.status = task.resolveStatus(now);

        collection(uid)
                .document(task.id)
                .set(task.toMap(), SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onComplete(true, null);
                })
                .addOnFailureListener(error -> {
                    if (callback != null) callback.onComplete(false, error);
                });
    }

    public void updateCompleted(@Nullable String uid,
                                @Nullable String taskId,
                                boolean completed,
                                @Nullable ActionCallback callback) {
        if (uid == null || uid.trim().isEmpty() || taskId == null || taskId.trim().isEmpty()) {
            if (callback != null) callback.onComplete(false, new IllegalStateException("Missing uid/taskId"));
            return;
        }

        long now = System.currentTimeMillis();
        collection(uid)
                .document(taskId)
                .update(
                        "completed", completed,
                        "completedAt", completed ? now : 0L,
                        "updatedAt", now,
                        "status", completed ? "completed" : "in_progress"
                )
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onComplete(true, null);
                })
                .addOnFailureListener(error -> {
                    if (callback != null) callback.onComplete(false, error);
                });
    }

    public void incrementCompletedPomodoro(@Nullable String uid,
                                           @Nullable String taskId,
                                           @Nullable ActionCallback callback) {
        if (uid == null || uid.trim().isEmpty() || taskId == null || taskId.trim().isEmpty()) {
            if (callback != null) callback.onComplete(false, new IllegalStateException("Missing uid/taskId"));
            return;
        }

        DocumentReference reference = collection(uid).document(taskId);
        firestore.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot snapshot = transaction.get(reference);
            Long current = snapshot.getLong("completedPomodoros");
            long next = (current == null ? 0L : current) + 1L;
            transaction.update(reference,
                    "completedPomodoros", next,
                    "updatedAt", System.currentTimeMillis());
            return null;
        }).addOnSuccessListener(unused -> {
            if (callback != null) callback.onComplete(true, null);
        }).addOnFailureListener(error -> {
            if (callback != null) callback.onComplete(false, error);
        });
    }

    public void deleteTask(@Nullable String uid, @Nullable String taskId, @Nullable ActionCallback callback) {
        if (uid == null || uid.trim().isEmpty() || taskId == null || taskId.trim().isEmpty()) {
            if (callback != null) callback.onComplete(false, new IllegalStateException("Missing uid/taskId"));
            return;
        }

        collection(uid)
                .document(taskId)
                .delete()
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onComplete(true, null);
                })
                .addOnFailureListener(error -> {
                    if (callback != null) callback.onComplete(false, error);
                });
    }

    public void deleteTasks(@Nullable String uid,
                            @NonNull List<String> taskIds,
                            @Nullable ActionCallback callback) {
        if (uid == null || uid.trim().isEmpty() || taskIds.isEmpty()) {
            if (callback != null) callback.onComplete(false, new IllegalStateException("Missing uid/taskIds"));
            return;
        }

        com.google.firebase.firestore.WriteBatch batch = firestore.batch();
        for (String taskId : taskIds) {
            if (taskId != null && !taskId.trim().isEmpty()) {
                batch.delete(collection(uid).document(taskId));
            }
        }

        batch.commit()
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onComplete(true, null);
                })
                .addOnFailureListener(error -> {
                    if (callback != null) callback.onComplete(false, error);
                });
    }
}