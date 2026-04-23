package com.hcmute.edu.vn.focus_life.data.remote.firestore;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.QuerySnapshot;
import com.hcmute.edu.vn.focus_life.core.utils.Constants;
import com.hcmute.edu.vn.focus_life.data.local.entity.NutritionEntryEntity;
import com.hcmute.edu.vn.focus_life.data.mapper.FirestoreMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NutritionRemoteDataSource {

    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(@NonNull Exception e);
    }

    private final com.google.firebase.firestore.FirebaseFirestore firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance();

    public void upsertNutritionEntry(String uid, String entryUuid, Map<String, Object> data) {
        firestore.collection(Constants.FIRESTORE_USERS)
                .document(uid)
                .collection(Constants.FIRESTORE_NUTRITION_ENTRIES)
                .document(entryUuid)
                .set(data);
    }

    public void upsertNutritionEntry(String uid, String entryUuid, Map<String, Object> data, DataCallback<Void> callback) {
        firestore.collection(Constants.FIRESTORE_USERS)
                .document(uid)
                .collection(Constants.FIRESTORE_NUTRITION_ENTRIES)
                .document(entryUuid)
                .set(data)
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    public void fetchNutritionEntriesByDate(String uid, String dateKey, DataCallback<List<NutritionEntryEntity>> callback) {
        firestore.collection(Constants.FIRESTORE_USERS)
                .document(uid)
                .collection(Constants.FIRESTORE_NUTRITION_ENTRIES)
                .whereEqualTo("entryDate", dateKey)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (callback == null) return;
                    callback.onSuccess(mapSnapshot(snapshot));
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    private List<NutritionEntryEntity> mapSnapshot(QuerySnapshot snapshot) {
        List<NutritionEntryEntity> items = new ArrayList<>();
        if (snapshot == null) return items;
        for (com.google.firebase.firestore.DocumentSnapshot document : snapshot.getDocuments()) {
            NutritionEntryEntity entity = FirestoreMapper.mapNutritionEntry(document);
            if (entity != null) {
                items.add(entity);
            }
        }
        return items;
    }
}
