package com.hcmute.edu.vn.focus_life.ui.diary;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.core.nutrition.FoodCatalog;
import com.hcmute.edu.vn.focus_life.core.nutrition.FoodCatalogItem;
import com.hcmute.edu.vn.focus_life.core.nutrition.NutritionInsightEngine;
import com.hcmute.edu.vn.focus_life.core.utils.Constants;
import com.hcmute.edu.vn.focus_life.core.utils.DateUtils;
import com.hcmute.edu.vn.focus_life.data.local.entity.NutritionEntryEntity;
import com.hcmute.edu.vn.focus_life.data.repository.NutritionDiaryRepository;

import java.util.List;
import java.util.Locale;

public class AddFoodActivity extends AppCompatActivity {

    public static final String EXTRA_DATE_KEY = "extra_date_key";
    public static final String EXTRA_MEAL_TYPE = "extra_meal_type";
    public static final String EXTRA_PREFILL_NAME = "extra_prefill_name";
    public static final String EXTRA_SOURCE = "extra_source";
    public static final String EXTRA_CONFIDENCE = "extra_confidence";
    public static final String EXTRA_IMAGE_URI = "extra_image_uri";

    private NutritionDiaryRepository repository;

    private NestedScrollView scrollAddFood;
    private View bottomSheet;
    private EditText etSearchFood;
    private LinearLayout layoutQuickPicks;
    private LinearLayout layoutResults;
    private TextView chipBreakfast;
    private TextView chipLunch;
    private TextView chipDinner;
    private TextView chipSnack;
    private TextView tvAddFoodSubtitle;
    private TextView tvSelectedFoodName;
    private TextView tvSelectedFoodMeta;
    private TextView tvQuantity;
    private TextView tvUnitLabel;
    private TextView tvSelectedCalories;
    private TextView tvSelectedMacros;
    private TextView tvSelectedHealthNote;
    private TextView btnSaveFood;
    private LinearLayout cardManualEntry;
    private TextView tvCustomEntryTitle;
    private TextView tvCustomEntrySubtitle;
    private LinearLayout layoutCustomEditor;
    private EditText etCustomName;
    private EditText etCustomUnit;
    private EditText etCustomCalories;
    private EditText etCustomProtein;
    private EditText etCustomCarbs;
    private EditText etCustomFat;

    private FoodCatalogItem selectedItem;
    private String selectedMealType;
    private String selectedDateKey;
    private String selectedSource;
    private float selectedConfidence;
    private String selectedImageUri;
    private int quantityCount = 1;
    private boolean saving = false;
    private boolean customMode = false;
    private boolean suppressCustomWatcher = false;

    private final TextWatcher searchWatcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) {
            String query = s == null ? "" : s.toString();
            renderResults(FoodCatalog.search(query));
            updateCustomEntryCard(query);
            if (customMode && TextUtils.isEmpty(cleanText(etCustomName.getText()))) {
                syncCustomItemFromInputs(true);
            }
        }
    };

    private final TextWatcher customWatcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) {
            if (customMode && !suppressCustomWatcher) {
                syncCustomItemFromInputs(true);
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_food);

        repository = new NutritionDiaryRepository();
        bindViews();
        readExtras();
        setupListeners();
        renderQuickPicks();
        updateMealSelectionUI();
        updateSubtitle();
        renderResults(FoodCatalog.search(getIntent().getStringExtra(EXTRA_PREFILL_NAME)));
        updateCustomEntryCard(getIntent().getStringExtra(EXTRA_PREFILL_NAME));
        autoPrefillIfNeeded();
        updateSelectedCard();
    }

    private void bindViews() {
        findViewById(R.id.btnBackAddFood).setOnClickListener(v -> finish());
        scrollAddFood = findViewById(R.id.scrollAddFood);
        bottomSheet = findViewById(R.id.bottomSheet);
        etSearchFood = findViewById(R.id.etSearchFood);
        layoutQuickPicks = findViewById(R.id.layoutQuickPicks);
        layoutResults = findViewById(R.id.layoutResults);
        chipBreakfast = findViewById(R.id.chipBreakfast);
        chipLunch = findViewById(R.id.chipLunch);
        chipDinner = findViewById(R.id.chipDinner);
        chipSnack = findViewById(R.id.chipSnack);
        tvAddFoodSubtitle = findViewById(R.id.tvAddFoodSubtitle);
        tvSelectedFoodName = findViewById(R.id.tvSelectedFoodName);
        tvSelectedFoodMeta = findViewById(R.id.tvSelectedFoodMeta);
        tvQuantity = findViewById(R.id.tvQuantity);
        tvUnitLabel = findViewById(R.id.tvUnitLabel);
        tvSelectedCalories = findViewById(R.id.tvSelectedCalories);
        tvSelectedMacros = findViewById(R.id.tvSelectedMacros);
        tvSelectedHealthNote = findViewById(R.id.tvSelectedHealthNote);
        btnSaveFood = findViewById(R.id.btnSaveFood);
        cardManualEntry = findViewById(R.id.cardManualEntry);
        tvCustomEntryTitle = findViewById(R.id.tvCustomEntryTitle);
        tvCustomEntrySubtitle = findViewById(R.id.tvCustomEntrySubtitle);
        layoutCustomEditor = findViewById(R.id.layoutCustomEditor);
        etCustomName = findViewById(R.id.etCustomFoodName);
        etCustomUnit = findViewById(R.id.etCustomFoodUnit);
        etCustomCalories = findViewById(R.id.etCustomCalories);
        etCustomProtein = findViewById(R.id.etCustomProtein);
        etCustomCarbs = findViewById(R.id.etCustomCarbs);
        etCustomFat = findViewById(R.id.etCustomFat);
    }

    private void readExtras() {
        selectedMealType = safeMeal(getIntent().getStringExtra(EXTRA_MEAL_TYPE));
        selectedDateKey = getIntent().getStringExtra(EXTRA_DATE_KEY);
        if (TextUtils.isEmpty(selectedDateKey)) {
            selectedDateKey = DateUtils.todayKey();
        }
        selectedSource = Constants.NUTRITION_SOURCE_MANUAL;
        selectedConfidence = 0f;
        selectedImageUri = null;
    }

    private void setupListeners() {
        etSearchFood.addTextChangedListener(searchWatcher);
        etCustomName.addTextChangedListener(customWatcher);
        etCustomUnit.addTextChangedListener(customWatcher);
        etCustomCalories.addTextChangedListener(customWatcher);
        etCustomProtein.addTextChangedListener(customWatcher);
        etCustomCarbs.addTextChangedListener(customWatcher);
        etCustomFat.addTextChangedListener(customWatcher);

        chipBreakfast.setOnClickListener(v -> switchMeal(Constants.MEAL_BREAKFAST));
        chipLunch.setOnClickListener(v -> switchMeal(Constants.MEAL_LUNCH));
        chipDinner.setOnClickListener(v -> switchMeal(Constants.MEAL_DINNER));
        chipSnack.setOnClickListener(v -> switchMeal(Constants.MEAL_SNACK));

        findViewById(R.id.btnMinusQty).setOnClickListener(v -> {
            if (quantityCount > 1) {
                quantityCount--;
                updateSelectedCard();
            }
        });
        findViewById(R.id.btnPlusQty).setOnClickListener(v -> {
            if (quantityCount < 10) {
                quantityCount++;
                updateSelectedCard();
            }
        });

        cardManualEntry.setOnClickListener(v -> {
            selectCustomItem(cleanText(etSearchFood.getText()), true);
            scrollToEditor();
        });
        btnSaveFood.setOnClickListener(v -> saveSelectedFood());
    }

    private void switchMeal(String meal) {
        selectedMealType = meal;
        updateMealSelectionUI();
        updateSubtitle();
    }

    private void renderQuickPicks() {
        layoutQuickPicks.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (FoodCatalogItem item : FoodCatalog.getQuickPicks()) {
            TextView chip = (TextView) inflater.inflate(R.layout.item_food_quick_pick, layoutQuickPicks, false);
            chip.setText(item.name);
            chip.setOnClickListener(v -> {
                etSearchFood.setText(item.name);
                etSearchFood.setSelection(item.name.length());
                selectItem(item);
            });
            layoutQuickPicks.addView(chip);
        }
    }

    private void renderResults(@NonNull List<FoodCatalogItem> items) {
        layoutResults.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        if (items.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Chưa thấy món trùng khớp. Bạn có thể bấm ‘Tự thêm món thủ công’ để nhập kcal và macro.");
            empty.setTextColor(getColor(R.color.on_surface_variant));
            empty.setBackgroundResource(R.drawable.bg_card_surface_low);
            empty.setPadding(dp(16), dp(16), dp(16), dp(16));
            layoutResults.addView(empty);
            return;
        }

        for (FoodCatalogItem item : items) {
            View row = inflater.inflate(R.layout.item_food_result, layoutResults, false);
            TextView tvFoodEmoji = row.findViewById(R.id.tvFoodEmoji);
            TextView tvFoodName = row.findViewById(R.id.tvFoodResultName);
            TextView tvFoodMeta = row.findViewById(R.id.tvFoodResultMeta);
            TextView tvFoodMacros = row.findViewById(R.id.tvFoodResultMacros);
            TextView btnAdd = row.findViewById(R.id.btnAddFoodResult);

            tvFoodEmoji.setText(item.emoji);
            tvFoodName.setText(item.name);
            tvFoodMeta.setText(String.format(Locale.getDefault(), "%s · %d kcal", item.unitLabel, item.calories));
            tvFoodMacros.setText(String.format(Locale.getDefault(), "P: %.0fg · C: %.0fg · F: %.0fg", item.protein, item.carbs, item.fat));

            View.OnClickListener selectListener = v -> {
                selectItem(item);
                scrollToEditor();
            };
            row.setOnClickListener(selectListener);
            btnAdd.setOnClickListener(selectListener);
            layoutResults.addView(row);
        }
    }

    private void autoPrefillIfNeeded() {
        String prefillName = getIntent().getStringExtra(EXTRA_PREFILL_NAME);
        if (TextUtils.isEmpty(prefillName)) {
            return;
        }
        etSearchFood.setText(prefillName);
        etSearchFood.setSelection(prefillName.length());

        FoodCatalogItem item = FoodCatalog.findByName(prefillName);
        if (item == null) {
            List<FoodCatalogItem> search = FoodCatalog.search(prefillName);
            if (!search.isEmpty()) {
                item = search.get(0);
            }
        }
        if (item != null) {
            selectItem(item);
        } else {
            selectCustomItem(prefillName, false);
        }
    }

    private void selectItem(@NonNull FoodCatalogItem item) {
        customMode = false;
        selectedItem = item;
        selectedSource = Constants.NUTRITION_SOURCE_MANUAL;
        selectedConfidence = 0f;
        selectedImageUri = null;
        if (quantityCount < 1) quantityCount = 1;
        updateSelectedCard();
    }

    private void selectCustomItem(@Nullable String suggestedName, boolean userInitiated) {
        customMode = true;
        selectedSource = Constants.NUTRITION_SOURCE_MANUAL;
        selectedConfidence = 0f;
        selectedImageUri = null;
        suppressCustomWatcher = true;
        try {
            String resolvedName = cleanText(suggestedName);
            etCustomName.setText(resolvedName);
            etCustomUnit.setText("");
            etCustomCalories.setText("");
            etCustomProtein.setText("");
            etCustomCarbs.setText("");
            etCustomFat.setText("");
        } finally {
            suppressCustomWatcher = false;
        }
        syncCustomItemFromInputs(true);
        if (userInitiated) {
            etCustomName.requestFocus();
        }
    }

    private void syncCustomItemFromInputs(boolean refreshUi) {
        String name = cleanText(etCustomName.getText());
        String unit = cleanText(etCustomUnit.getText());
        int calories = parseInt(etCustomCalories.getText(), 0);
        double protein = parseDouble(etCustomProtein.getText(), 0d);
        double carbs = parseDouble(etCustomCarbs.getText(), 0d);
        double fat = parseDouble(etCustomFat.getText(), 0d);

        selectedItem = new FoodCatalogItem(
                "custom_manual_item",
                "🍽️",
                name,
                1,
                unit,
                calories,
                protein,
                carbs,
                fat,
                0,
                0,
                0,
                buildCustomHealthNote(calories, protein, carbs, fat),
                TextUtils.isEmpty(name) ? "manual" : name
        );
        if (refreshUi) {
            updateSelectedCard();
        }
    }

    private void updateSelectedCard() {
        tvQuantity.setText(String.valueOf(quantityCount));
        layoutCustomEditor.setVisibility(customMode ? View.VISIBLE : View.GONE);

        if (selectedItem == null) {
            tvSelectedFoodName.setText("Chọn món để xem chi tiết");
            tvSelectedFoodMeta.setText("Bạn có thể chọn món có sẵn hoặc tự nhập món mới");
            tvUnitLabel.setText("Đơn vị");
            tvSelectedCalories.setText("0 kcal");
            tvSelectedMacros.setText("Protein 0g · Carbs 0g · Fat 0g");
            tvSelectedHealthNote.setText("Gợi ý dinh dưỡng sẽ hiện ở đây");
            btnSaveFood.setAlpha(0.55f);
            btnSaveFood.setText("Lưu vào nhật ký");
            return;
        }

        int calories = (int) Math.round(selectedItem.calories * quantityCount);
        double protein = selectedItem.protein * quantityCount;
        double carbs = selectedItem.carbs * quantityCount;
        double fat = selectedItem.fat * quantityCount;

        if (customMode) {
            String displayName = cleanText(etCustomName.getText());
            String displayUnit = cleanText(etCustomUnit.getText());
            tvSelectedFoodName.setText(TextUtils.isEmpty(displayName) ? "🍽️ Món tự nhập" : "🍽️ " + displayName);
            if (TextUtils.isEmpty(displayUnit)) {
                tvSelectedFoodMeta.setText("Nhập tên món, đơn vị và kcal theo khẩu phần bạn đang theo dõi");
                tvUnitLabel.setText("Đơn vị");
            } else {
                tvSelectedFoodMeta.setText(quantityCount + " × " + displayUnit + " · tự nhập");
                tvUnitLabel.setText(displayUnit);
            }
            btnSaveFood.setText("Lưu món tự nhập");
        } else {
            tvSelectedFoodName.setText(selectedItem.emoji + " " + selectedItem.name);
            tvSelectedFoodMeta.setText(quantityCount + " × " + selectedItem.unitLabel);
            tvUnitLabel.setText(selectedItem.unitLabel);
            btnSaveFood.setText("Lưu vào nhật ký");
        }

        tvSelectedCalories.setText(calories + " kcal");
        tvSelectedMacros.setText(String.format(Locale.getDefault(), "Protein %.0fg · Carbs %.0fg · Fat %.0fg", protein, carbs, fat));
        tvSelectedHealthNote.setText(selectedItem.healthNote);
        btnSaveFood.setAlpha(saving ? 0.65f : 1f);
    }

    private void saveSelectedFood() {
        if (saving) return;
        if (selectedItem == null) {
            Toast.makeText(this, "Bạn hãy chọn món ăn trước khi lưu nhé.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (customMode) {
            syncCustomItemFromInputs(false);
            if (TextUtils.isEmpty(cleanText(etCustomName.getText()))) {
                Toast.makeText(this, "Bạn nhập tên món trước đã nhé.", Toast.LENGTH_SHORT).show();
                etCustomName.requestFocus();
                scrollToEditor();
                return;
            }
            if (TextUtils.isEmpty(cleanText(etCustomUnit.getText()))) {
                Toast.makeText(this, "Bạn nhập đơn vị như phần, gram hay ly để lưu rõ hơn nhé.", Toast.LENGTH_SHORT).show();
                etCustomUnit.requestFocus();
                scrollToEditor();
                return;
            }
            if (selectedItem.calories <= 0) {
                Toast.makeText(this, "Bạn nên nhập kcal cho món tự đo để nhật ký tính đúng tổng ngày.", Toast.LENGTH_SHORT).show();
                etCustomCalories.requestFocus();
                scrollToEditor();
                return;
            }
            updateSelectedCard();
        }

        try {
            saving = true;
            updateSelectedCard();

            NutritionEntryEntity entry = new NutritionEntryEntity();
            entry.mealType = selectedMealType;
            entry.foodName = selectedItem.name;
            entry.quantity = selectedItem.defaultQuantity * quantityCount;
            entry.unit = selectedItem.unitLabel;
            entry.calories = (int) Math.round(selectedItem.calories * quantityCount);
            entry.protein = selectedItem.protein * quantityCount;
            entry.carbs = selectedItem.carbs * quantityCount;
            entry.fat = selectedItem.fat * quantityCount;
            entry.fiber = selectedItem.fiber * quantityCount;
            entry.sugar = selectedItem.sugar * quantityCount;
            entry.sodium = selectedItem.sodium * quantityCount;
            entry.entryDate = selectedDateKey;
            entry.source = Constants.NUTRITION_SOURCE_MANUAL;
            entry.mlConfidence = 0f;
            entry.imageUri = null;
            entry.healthFlags = NutritionInsightEngine.buildEntryFlags(entry.protein, entry.fiber, entry.sugar, entry.sodium);

            repository.saveEntry(entry, (savedEntry, infoMessage, error) -> {
                saving = false;
                updateSelectedCard();
                if (error != null) {
                    Toast.makeText(AddFoodActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                    return;
                }
                Toast.makeText(AddFoodActivity.this, infoMessage == null ? "Đã lưu món ăn." : infoMessage, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            });
        } catch (Exception e) {
            saving = false;
            updateSelectedCard();
            Toast.makeText(this, "Có lỗi khi lưu món ăn. Bạn thử lại nhé.", Toast.LENGTH_LONG).show();
        }
    }

    private void updateMealSelectionUI() {
        applyMealChipStyle(chipBreakfast, Constants.MEAL_BREAKFAST.equals(selectedMealType));
        applyMealChipStyle(chipLunch, Constants.MEAL_LUNCH.equals(selectedMealType));
        applyMealChipStyle(chipDinner, Constants.MEAL_DINNER.equals(selectedMealType));
        applyMealChipStyle(chipSnack, Constants.MEAL_SNACK.equals(selectedMealType));
    }

    private void applyMealChipStyle(TextView chip, boolean selected) {
        chip.setBackgroundResource(selected ? R.drawable.bg_gradient_primary : R.drawable.bg_chip_selected);
        chip.setTextColor(getColor(selected ? R.color.on_primary : R.color.primary));
    }

    private void updateSubtitle() {
        tvAddFoodSubtitle.setText(readableMeal(selectedMealType) + " · " + DateUtils.formatDiaryDate(selectedDateKey));
    }

    private void updateCustomEntryCard(@Nullable String query) {
        String cleanQuery = cleanText(query);
        if (TextUtils.isEmpty(cleanQuery)) {
            tvCustomEntryTitle.setText("Tự thêm món thủ công");
            tvCustomEntrySubtitle.setText("Dùng khi món không có sẵn hoặc bạn đã tự đo kcal và macro.");
        } else {
            tvCustomEntryTitle.setText("Tự thêm: " + cleanQuery);
            tvCustomEntrySubtitle.setText("Không cần món có sẵn trong danh sách. Bạn có thể nhập kcal, protein, carb và fat theo cách bạn đang theo dõi.");
        }
    }

    private String buildCustomHealthNote(int calories, double protein, double carbs, double fat) {
        if (protein >= 20) {
            return "Món tự nhập này có lượng đạm khá ổn. Nếu đây là bữa chính, bạn chỉ cần cân đối thêm rau và chất xơ.";
        }
        if (fat >= 18) {
            return "Món tự nhập này có chất béo tương đối cao. Bạn nên chú ý tổng fat của cả ngày để dễ cân bằng hơn.";
        }
        if (carbs >= 50) {
            return "Lượng carb của món này khá đáng kể. Hợp trước vận động, còn nếu ăn muộn bạn nên cân lại khẩu phần.";
        }
        if (calories <= 180 && calories > 0) {
            return "Khá hợp làm bữa phụ nhẹ. Bạn có thể kết hợp thêm đạm hoặc trái cây nếu muốn no lâu hơn.";
        }
        return "Bạn có thể chỉnh kcal và macro tự do cho đúng cách mình đang đo món này.";
    }

    private String readableMeal(String mealType) {
        switch (safeMeal(mealType)) {
            case Constants.MEAL_BREAKFAST:
                return "Bữa sáng";
            case Constants.MEAL_DINNER:
                return "Bữa tối";
            case Constants.MEAL_SNACK:
                return "Bữa phụ";
            case Constants.MEAL_LUNCH:
            default:
                return "Bữa trưa";
        }
    }

    private String safeMeal(String raw) {
        if (Constants.MEAL_BREAKFAST.equals(raw)
                || Constants.MEAL_LUNCH.equals(raw)
                || Constants.MEAL_DINNER.equals(raw)
                || Constants.MEAL_SNACK.equals(raw)) {
            return raw;
        }
        return Constants.MEAL_LUNCH;
    }

    private String cleanText(@Nullable CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }

    private int parseInt(@Nullable CharSequence value, int fallback) {
        try {
            String raw = cleanText(value);
            return raw.isEmpty() ? fallback : Integer.parseInt(raw);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private double parseDouble(@Nullable CharSequence value, double fallback) {
        try {
            String raw = cleanText(value).replace(',', '.');
            return raw.isEmpty() ? fallback : Double.parseDouble(raw);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private void scrollToEditor() {
        if (scrollAddFood == null || bottomSheet == null) return;
        scrollAddFood.post(() -> scrollAddFood.smoothScrollTo(0, Math.max(0, bottomSheet.getTop() - dp(12))));
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}
