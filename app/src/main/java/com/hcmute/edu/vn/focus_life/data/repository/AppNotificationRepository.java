package com.hcmute.edu.vn.focus_life.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.hcmute.edu.vn.focus_life.core.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AppNotificationRepository {
    public static final String TYPE_WATER = "water";
    public static final String TYPE_MOTIVATION = "motivation";
    public static final String TYPE_FOCUS = "focus";
    public static final String TYPE_RUNNING = "running";
    public static final String TYPE_SYSTEM = "system";

    public interface NotificationCallback {
        void onResult(@NonNull List<AppNotification> notifications, @Nullable Exception error);
    }

    public static class AppNotification {
        public String id;
        public String type;
        public String title;
        public String message;
        public String channelId;
        public long createdAt;
        public boolean read;

        public String typeLabel() {
            if (TYPE_WATER.equals(type)) return "Uống nước";
            if (TYPE_MOTIVATION.equals(type)) return "Động lực";
            if (TYPE_FOCUS.equals(type)) return "Focus";
            if (TYPE_RUNNING.equals(type)) return "Chạy bộ";
            return "Hệ thống";
        }
    }

    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final FirebaseAuth auth = FirebaseAuth.getInstance();

    public static void log(@NonNull String type,
                           @NonNull String title,
                           @NonNull String message,
                           @NonNull String channelId) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        long now = System.currentTimeMillis();
        String id = type + "_" + now;
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("type", type);
        data.put("title", title);
        data.put("message", message);
        data.put("channelId", channelId);
        data.put("createdAt", now);
        data.put("date", new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(now)));
        data.put("read", false);
        db.collection(Constants.FIRESTORE_USERS)
                .document(user.getUid())
                .collection("notifications")
                .document(id)
                .set(data, SetOptions.merge());
    }

    public static void loadRecent(@NonNull NotificationCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onResult(new ArrayList<>(), new IllegalStateException("Bạn cần đăng nhập để xem thông báo"));
            return;
        }
        db.collection(Constants.FIRESTORE_USERS)
                .document(user.getUid())
                .collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(snapshot -> callback.onResult(map(snapshot), null))
                .addOnFailureListener(e -> callback.onResult(new ArrayList<>(), e));
    }

    private static List<AppNotification> map(QuerySnapshot snapshot) {
        List<AppNotification> notifications = new ArrayList<>();
        if (snapshot == null) return notifications;
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            AppNotification item = new AppNotification();
            item.id = string(doc.get("id"), doc.getId());
            item.type = string(doc.get("type"), TYPE_SYSTEM);
            item.title = string(doc.get("title"), "Thông báo FocusLife");
            item.message = string(doc.get("message"), "");
            item.channelId = string(doc.get("channelId"), "");
            Object createdAt = doc.get("createdAt");
            item.createdAt = createdAt instanceof Number ? ((Number) createdAt).longValue() : 0L;
            Object read = doc.get("read");
            item.read = read instanceof Boolean && (Boolean) read;
            notifications.add(item);
        }
        return notifications;
    }

    private static String string(Object value, String fallback) {
        String text = value == null ? "" : String.valueOf(value);
        return text.trim().isEmpty() ? fallback : text;
    }
}
