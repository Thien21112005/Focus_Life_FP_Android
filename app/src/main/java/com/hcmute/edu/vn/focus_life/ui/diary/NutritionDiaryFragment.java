package com.hcmute.edu.vn.focus_life.ui.diary;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.core.nutrition.NutritionDaySummary;
import com.hcmute.edu.vn.focus_life.core.nutrition.NutritionInsightEngine;
import com.hcmute.edu.vn.focus_life.core.utils.Constants;
import com.hcmute.edu.vn.focus_life.core.utils.DateUtils;
import com.hcmute.edu.vn.focus_life.data.local.entity.NutritionEntryEntity;
import com.hcmute.edu.vn.focus_life.data.repository.NutritionDiaryRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NutritionDiaryFragment extends Fragment {

    private static final int DEFAULT_CALORIE_GOAL = 2000;
    private static final int DEFAULT_PROTEIN_GOAL = 120;
    private static final int DEFAULT_CARBS_GOAL = 250;
    private static final int DEFAULT_FAT_GOAL = 65;

    private final List<NutritionEntryEntity> currentEntries = new ArrayList<>();
    private NutritionDiaryRepository repository;

    private String currentDateKey = DateUtils.todayKey();

    private TextView tvSyncStatus;
    private TextView tvDiaryDate;
    private TextView tvCaloriesSummary;
    private TextView tvCaloriesRemaining;
    private ProgressBar progressCalories;
    private TextView tvProteinMeta;
    private TextView tvCarbsMeta;
    private TextView tvFatMeta;
    private ProgressBar progressProtein;
    private ProgressBar progressCarbs;
    private ProgressBar progressFat;
    private LinearLayout containerBreakfast;
    private LinearLayout containerLunch;
    private LinearLayout containerDinner;
    private LinearLayout containerSnack;
    private TextView tvBreakfastTotal;
    private TextView tvLunchTotal;
    private TextView tvDinnerTotal;
    private TextView tvSnackTotal;
    private TextView tvInsightHeadline;
    private TextView tvInsightBody;
    private TextView tvHealthWarnings;

    private ActivityResultLauncher<Intent> addFoodLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new NutritionDiaryRepository();

        addFoodLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                        loadDiary(true);
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_nutrition_diary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupListeners(view);
        renderDate();
        loadDiary(true);
    }

    private void bindViews(@NonNull View root) {
        tvSyncStatus = root.findViewById(R.id.tvSyncStatus);
        tvDiaryDate = root.findViewById(R.id.tvDiaryDate);
        tvCaloriesSummary = root.findViewById(R.id.tvCaloriesSummary);
        tvCaloriesRemaining = root.findViewById(R.id.tvCaloriesRemaining);
        progressCalories = root.findViewById(R.id.progressCalories);
        tvProteinMeta = root.findViewById(R.id.tvProteinMeta);
        tvCarbsMeta = root.findViewById(R.id.tvCarbsMeta);
        tvFatMeta = root.findViewById(R.id.tvFatMeta);
        progressProtein = root.findViewById(R.id.progressProtein);
        progressCarbs = root.findViewById(R.id.progressCarbs);
        progressFat = root.findViewById(R.id.progressFat);
        containerBreakfast = root.findViewById(R.id.containerBreakfast);
        containerLunch = root.findViewById(R.id.containerLunch);
        containerDinner = root.findViewById(R.id.containerDinner);
        containerSnack = root.findViewById(R.id.containerSnack);
        tvBreakfastTotal = root.findViewById(R.id.tvBreakfastTotal);
        tvLunchTotal = root.findViewById(R.id.tvLunchTotal);
        tvDinnerTotal = root.findViewById(R.id.tvDinnerTotal);
        tvSnackTotal = root.findViewById(R.id.tvSnackTotal);
        tvInsightHeadline = root.findViewById(R.id.tvInsightHeadline);
        tvInsightBody = root.findViewById(R.id.tvInsightBody);
        tvHealthWarnings = root.findViewById(R.id.tvHealthWarnings);
    }

    private void setupListeners(@NonNull View root) {
        root.findViewById(R.id.btnPrevDay).setOnClickListener(v -> {
            currentDateKey = DateUtils.shiftDate(currentDateKey, -1);
            renderDate();
            loadDiary(true);
        });
        root.findViewById(R.id.btnNextDay).setOnClickListener(v -> {
            currentDateKey = DateUtils.shiftDate(currentDateKey, 1);
            renderDate();
            loadDiary(true);
        });
        root.findViewById(R.id.btnRefreshDiary).setOnClickListener(v -> loadDiary(true));
        root.findViewById(R.id.btnAddBreakfast).setOnClickListener(v -> openAddFood(Constants.MEAL_BREAKFAST, null));
        root.findViewById(R.id.btnAddLunch).setOnClickListener(v -> openAddFood(Constants.MEAL_LUNCH, null));
        root.findViewById(R.id.btnAddDinner).setOnClickListener(v -> openAddFood(Constants.MEAL_DINNER, null));
        root.findViewById(R.id.btnAddSnack).setOnClickListener(v -> openAddFood(Constants.MEAL_SNACK, null));
    }

    private void renderDate() {
        tvDiaryDate.setText(DateUtils.formatDiaryDate(currentDateKey));
    }

    private void loadDiary(boolean refreshFromRemote) {
        if (!isAdded()) return;
        tvSyncStatus.setText(refreshFromRemote ? "Đang tải và đồng bộ dữ liệu..." : "Đang tải dữ liệu...");
        repository.loadEntriesForDate(currentDateKey, refreshFromRemote, (entries, fromRemote, infoMessage, error) -> {
            if (!isAdded()) return;
            currentEntries.clear();
            if (entries != null) currentEntries.addAll(entries);
            renderSummary();
            renderMealSections();
            renderInsight();
            if (!TextUtils.isEmpty(infoMessage)) {
                tvSyncStatus.setText(infoMessage);
            } else if (fromRemote) {
                tvSyncStatus.setText("Dữ liệu đã được đồng bộ theo tài khoản hiện tại.");
            } else {
                tvSyncStatus.setText("Hiển thị dữ liệu đang lưu trên thiết bị.");
            }
            if (error != null) {
                Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void renderSummary() {
        NutritionDaySummary summary = NutritionDaySummary.fromEntries(currentEntries, DEFAULT_CALORIE_GOAL);
        tvCaloriesSummary.setText(String.format(Locale.getDefault(), "%d", summary.calories));
        int remaining = summary.remainingCalories();
        if (remaining >= 0) {
            tvCaloriesRemaining.setText(String.format(Locale.getDefault(), "Còn khoảng %d kcal để chạm mục tiêu hôm nay", remaining));
        } else {
            tvCaloriesRemaining.setText(String.format(Locale.getDefault(), "Đang vượt khoảng %d kcal so với mục tiêu", Math.abs(remaining)));
        }

        progressCalories.setProgress(percent(summary.calories, DEFAULT_CALORIE_GOAL));
        tvProteinMeta.setText(String.format(Locale.getDefault(), "Protein %.0fg / %dg", summary.protein, DEFAULT_PROTEIN_GOAL));
        tvCarbsMeta.setText(String.format(Locale.getDefault(), "Carbs %.0fg / %dg", summary.carbs, DEFAULT_CARBS_GOAL));
        tvFatMeta.setText(String.format(Locale.getDefault(), "Fat %.0fg / %dg", summary.fat, DEFAULT_FAT_GOAL));
        progressProtein.setProgress(percent(summary.protein, DEFAULT_PROTEIN_GOAL));
        progressCarbs.setProgress(percent(summary.carbs, DEFAULT_CARBS_GOAL));
        progressFat.setProgress(percent(summary.fat, DEFAULT_FAT_GOAL));
    }

    private void renderMealSections() {
        renderMealBlock(containerBreakfast, filterByMeal(Constants.MEAL_BREAKFAST), tvBreakfastTotal, "Bữa sáng");
        renderMealBlock(containerLunch, filterByMeal(Constants.MEAL_LUNCH), tvLunchTotal, "Bữa trưa");
        renderMealBlock(containerDinner, filterByMeal(Constants.MEAL_DINNER), tvDinnerTotal, "Bữa tối");
        renderMealBlock(containerSnack, filterByMeal(Constants.MEAL_SNACK), tvSnackTotal, "Bữa phụ");
    }

    private void renderMealBlock(@NonNull LinearLayout container,
                                 @NonNull List<NutritionEntryEntity> entries,
                                 @NonNull TextView totalView,
                                 @NonNull String emptyMealLabel) {
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int totalCalories = 0;
        for (NutritionEntryEntity entry : entries) {
            if (entry != null && !entry.deleted) {
                totalCalories += entry.calories;
            }
        }
        totalView.setText(totalCalories + " kcal");

        if (entries.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("Chưa có món nào trong " + emptyMealLabel.toLowerCase(Locale.ROOT) + ".");
            empty.setTextColor(requireContext().getColor(R.color.on_surface_variant));
            empty.setPadding(0, dp(8), 0, dp(4));
            container.addView(empty);
            return;
        }

        for (NutritionEntryEntity entry : entries) {
            View row = inflater.inflate(R.layout.item_nutrition_entry, container, false);
            TextView tvFoodName = row.findViewById(R.id.tvFoodName);
            TextView tvFoodMeta = row.findViewById(R.id.tvFoodMeta);
            TextView tvFoodFlags = row.findViewById(R.id.tvFoodFlags);
            TextView btnDelete = row.findViewById(R.id.btnDeleteEntry);

            tvFoodName.setText(entry.foodName);
            tvFoodMeta.setText(String.format(Locale.getDefault(), "%s %s · %d kcal · P %.0fg / C %.0fg / F %.0fg",
                    valueOfQuantity(entry.quantity),
                    safeUnit(entry.unit),
                    entry.calories,
                    entry.protein,
                    entry.carbs,
                    entry.fat));
            tvFoodFlags.setText(buildFlagText(entry));
            btnDelete.setOnClickListener(v -> confirmDelete(entry));
            container.addView(row);
        }
    }

    private void renderInsight() {
        NutritionDaySummary summary = NutritionDaySummary.fromEntries(currentEntries, DEFAULT_CALORIE_GOAL);
        NutritionInsightEngine.DayInsight insight = NutritionInsightEngine.buildDayInsight(currentEntries, summary);
        tvInsightHeadline.setText(insight.headline);
        tvInsightBody.setText(insight.recommendation);
        if (insight.warnings.isEmpty()) {
            tvHealthWarnings.setText("✓ Chưa có cảnh báo dinh dưỡng nổi bật trong ngày này.");
        } else {
            StringBuilder builder = new StringBuilder();
            for (String warning : insight.warnings) {
                if (builder.length() > 0) builder.append("\n• ");
                else builder.append("• ");
                builder.append(warning);
            }
            tvHealthWarnings.setText(builder.toString());
        }
    }

    private void openAddFood(@NonNull String mealType, @Nullable String prefillName) {
        if (!isAdded()) return;
        Intent intent = new Intent(requireContext(), AddFoodActivity.class);
        intent.putExtra(AddFoodActivity.EXTRA_DATE_KEY, currentDateKey);
        intent.putExtra(AddFoodActivity.EXTRA_MEAL_TYPE, mealType);
        intent.putExtra(AddFoodActivity.EXTRA_PREFILL_NAME, prefillName);
        addFoodLauncher.launch(intent);
    }

    private void confirmDelete(@NonNull NutritionEntryEntity entry) {
        if (!isAdded()) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa món ăn")
                .setMessage("Bạn muốn xóa “" + entry.foodName + "” khỏi nhật ký ngày này?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> repository.softDeleteEntry(entry.entryUuid, (infoMessage, error) -> {
                    if (!isAdded()) return;
                    if (error != null) {
                        Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    Toast.makeText(requireContext(), infoMessage == null ? "Đã xóa món ăn." : infoMessage, Toast.LENGTH_SHORT).show();
                    loadDiary(true);
                }))
                .show();
    }

    @NonNull
    private List<NutritionEntryEntity> filterByMeal(@NonNull String mealType) {
        List<NutritionEntryEntity> result = new ArrayList<>();
        for (NutritionEntryEntity entry : currentEntries) {
            if (entry != null && !entry.deleted && mealType.equals(entry.mealType)) {
                result.add(entry);
            }
        }
        return result;
    }

    private String buildFlagText(@NonNull NutritionEntryEntity entry) {
        List<String> flags = new ArrayList<>();
        if (!TextUtils.isEmpty(entry.healthFlags)) {
            if (entry.healthFlags.contains("natri_cao")) flags.add("Natri cao");
            if (entry.healthFlags.contains("duong_cao")) flags.add("Đường cao");
            if (entry.healthFlags.contains("protein_tot")) flags.add("Protein tốt");
            if (entry.healthFlags.contains("giau_chat_xo")) flags.add("Giàu xơ");
        }
        if (flags.isEmpty()) {
            flags.add("Đã lưu theo ngày cho tài khoản hiện tại");
        }
        return TextUtils.join(" · ", flags);
    }

    private int percent(double value, int goal) {
        if (goal <= 0) return 0;
        return Math.max(0, Math.min(100, (int) Math.round((value * 100d) / goal)));
    }

    private String valueOfQuantity(double quantity) {
        if (Math.abs(quantity - Math.rint(quantity)) < 0.0001d) {
            return String.valueOf((int) Math.rint(quantity));
        }
        return String.format(Locale.getDefault(), "%.1f", quantity);
    }

    private String safeUnit(@Nullable String unit) {
        return TextUtils.isEmpty(unit) ? "khẩu phần" : unit;
    }

    private int dp(int value) {
        if (!isAdded()) return value;
        return Math.round(requireContext().getResources().getDisplayMetrics().density * value);
    }
}
