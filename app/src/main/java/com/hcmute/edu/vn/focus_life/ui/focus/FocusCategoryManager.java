package com.hcmute.edu.vn.focus_life.ui.focus;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FocusCategoryManager {
    private static final String PREF_NAME = "focus_categories_pref";
    private static final String KEY_CATEGORIES = "categories";
    private static final String SEPARATOR = "\\|";
    private static final String JOIN_SEPARATOR = "|";

    private static final String[] DEFAULT_CATEGORIES = {
            "Học tập", "Công việc", "Dự án", "Cá nhân", "Sức khỏe", "Thiết kế", "Mua sắm", "Khác"
    };

    private final SharedPreferences preferences;

    public FocusCategoryManager(@NonNull Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        ensureDefaults();
    }

    @NonNull
    public List<String> getCategories() {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        String raw = preferences.getString(KEY_CATEGORIES, "");
        if (raw != null && !raw.trim().isEmpty()) {
            String[] values = raw.split(SEPARATOR);
            for (String value : values) {
                String normalized = normalize(migrateDefaultName(value));
                if (!normalized.isEmpty()) result.add(normalized);
            }
        }
        if (result.isEmpty()) {
            for (String category : DEFAULT_CATEGORIES) result.add(category);
        }
        save(result);
        return new ArrayList<>(result);
    }

    public boolean addCategory(@Nullable String value) {
        String category = normalize(value);
        if (category.isEmpty()) return false;
        LinkedHashSet<String> categories = new LinkedHashSet<>(getCategories());
        boolean changed = categories.add(category);
        if (changed) save(categories);
        return changed;
    }

    public boolean renameCategory(@Nullable String oldValue, @Nullable String newValue) {
        String oldCategory = normalize(oldValue);
        String newCategory = normalize(newValue);
        if (oldCategory.isEmpty() || newCategory.isEmpty()) return false;

        List<String> current = getCategories();
        LinkedHashSet<String> updated = new LinkedHashSet<>();
        boolean changed = false;

        for (String category : current) {
            if (category.equalsIgnoreCase(oldCategory)) {
                updated.add(newCategory);
                changed = true;
            } else if (!category.equalsIgnoreCase(newCategory)) {
                updated.add(category);
            }
        }

        if (changed) save(updated);
        return changed;
    }

    public boolean deleteCategory(@Nullable String value) {
        String categoryToRemove = normalize(value);
        if (categoryToRemove.isEmpty()) return false;

        LinkedHashSet<String> categories = new LinkedHashSet<>();
        boolean removed = false;
        for (String category : getCategories()) {
            if (category.equalsIgnoreCase(categoryToRemove)) {
                removed = true;
            } else {
                categories.add(category);
            }
        }

        if (removed) save(categories);
        return removed;
    }

    private void ensureDefaults() {
        if (preferences.contains(KEY_CATEGORIES)) {
            getCategories();
            return;
        }
        LinkedHashSet<String> categories = new LinkedHashSet<>();
        for (String category : DEFAULT_CATEGORIES) categories.add(category);
        save(categories);
    }

    private void save(@NonNull Set<String> categories) {
        StringBuilder builder = new StringBuilder();
        for (String category : categories) {
            String normalized = normalize(category);
            if (normalized.isEmpty()) continue;
            if (builder.length() > 0) builder.append(JOIN_SEPARATOR);
            builder.append(normalized.replace(JOIN_SEPARATOR, ""));
        }
        preferences.edit().putString(KEY_CATEGORIES, builder.toString()).apply();
    }

    @NonNull
    private String migrateDefaultName(@Nullable String value) {
        String category = normalize(value);
        if (category.equalsIgnoreCase("Study")) return "Học tập";
        if (category.equalsIgnoreCase("Work")) return "Công việc";
        if (category.equalsIgnoreCase("Design")) return "Thiết kế";
        if (category.equalsIgnoreCase("Project")) return "Dự án";
        if (category.equalsIgnoreCase("Personal")) return "Cá nhân";
        if (category.equalsIgnoreCase("Health")) return "Sức khỏe";
        if (category.equalsIgnoreCase("Shopping")) return "Mua sắm";
        if (category.equalsIgnoreCase("Other")) return "Khác";
        return category;
    }

    @NonNull
    private String normalize(@Nullable String value) {
        if (value == null) return "";
        String trimmed = value.trim().replace(JOIN_SEPARATOR, "");
        if (trimmed.isEmpty()) return "";
        return trimmed.substring(0, 1).toUpperCase(Locale.ROOT) + trimmed.substring(1);
    }
}
