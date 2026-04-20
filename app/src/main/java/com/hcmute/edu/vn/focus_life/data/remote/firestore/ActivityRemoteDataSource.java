package com.hcmute.edu.vn.focus_life.data.remote.firestore;

import com.google.firebase.firestore.FirebaseFirestore;
import com.hcmute.edu.vn.focus_life.core.utils.Constants;

import java.util.Map;

public class ActivityRemoteDataSource {
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public void upsertDailySummary(String uid, String dateKey, Map<String, Object> data) {
        firestore.collection(Constants.FIRESTORE_USERS)
                .document(uid)
                .collection(Constants.FIRESTORE_DAILY_SUMMARIES)
                .document(dateKey)
                .set(data);
    }

    public void upsertPomodoroSession(String uid, String sessionId, Map<String, Object> data) {
        firestore.collection(Constants.FIRESTORE_USERS)
                .document(uid)
                .collection("pomodoro_sessions")
                .document(sessionId)
                .set(data);
    }

    public void upsertStepRecord(String uid, String recordId, Map<String, Object> data) {
        firestore.collection(Constants.FIRESTORE_USERS)
                .document(uid)
                .collection(Constants.FIRESTORE_STEP_RECORDS)
                .document(recordId)
                .set(data);
    }
}