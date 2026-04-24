package com.hcmute.edu.vn.focus_life.core.ml;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.hcmute.edu.vn.focus_life.core.exception.UserFacingException;
import com.hcmute.edu.vn.focus_life.core.nutrition.FoodCatalogItem;

import java.util.ArrayList;
import java.util.List;

public class NutritionMlKitAnalyzer {

    public interface Callback {
        void onSuccess(@NonNull ScanResult result);
        void onError(@NonNull UserFacingException error);
    }

    public static class ScanResult {
        public FoodCatalogItem matchedItem;
        public float confidence;
        public final List<String> labels = new ArrayList<>();
        public final List<String> warnings = new ArrayList<>();
        public String summary;
    }

    public void analyze(@NonNull Context context, @NonNull Uri imageUri, @NonNull Callback callback) {
        callback.onError(new UserFacingException("Tính năng quét ảnh món ăn hiện đã được tắt trong bản này."));
    }
}
