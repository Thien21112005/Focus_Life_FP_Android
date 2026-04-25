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

    public static final String EXTRA_EDIT_ENTRY_UUID = "extra_edit_entry_uuid";
    public static final String EXTRA_EDIT_FOOD_NAME = "extra_edit_food_name";
    public static final String EXTRA_EDIT_UNIT = "extra_edit_unit";
    public static final String EXTRA_EDIT_QUANTITY = "extra_edit_quantity";
    public static final String EXTRA_EDIT_CALORIES = "extra_edit_calories";
    public static final String EXTRA_EDIT_PROTEIN = "extra_edit_protein";
    public static final String EXTRA_EDIT_CARBS = "extra_edit_carbs";
    public static final String EXTRA_EDIT_FAT = "extra_edit_fat";
    public static final String EXTRA_EDIT_FIBER = "extra_edit_fiber";
    public static final String EXTRA_EDIT_SUGAR = "extra_edit_sugar";
    public static final String EXTRA_EDIT_SODIUM = "extra_edit_sodium";

    private NutritionDiaryRepository repository;

    private NestedScrollView scrollAddFood;
    private EditText etSearchFood;
    private LinearLayout layoutQuickPicks;
    private LinearLayout layoutResults;
    private LinearLayout bottomSheet;
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
    private String editingEntryUuid;
    private double editFiber;
    private double editSugar;
    private double editSodium;

    private final TextWatcher searchWatcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) {
            String query = s == null ? "" : s.toString();
            renderResults(FoodCatalog.search(query));
            updateCustomEntryCard(query);
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

        if (isEditingManualEntry()) {
            enterEditMode();
        } else {
            autoPrefillIfNeeded();
            updateSelectedCard();
        }
    }

    private void bindViews() {
        findViewById(R.id.btnBackAddFood).setOnClickListener(v -> finish());
        scrollAddFood = findViewById(R.id.scrollAddFood);
        etSearchFood = findViewById(R.id.etSearchFood);
        layoutQuickPicks = findViewById(R.id.layoutQuickPicks);
        layoutResults = findViewById(R.id.layoutResults);
        bottomSheet = findViewById(R.id.bottomSheet);
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
        editingEntryUuid = getIntent().getStringExtra(EXTRA_EDIT_ENTRY_UUID);
        editFiber = getIntent().getDoubleExtra(EXTRA_EDIT_FIBER, 0d);
        editSugar = getIntent().getDoubleExtra(EXTRA_EDIT_SUGAR, 0d);
        editSodium = getIntent().getDoubleExtra(EXTRA_EDIT_SODIUM, 0d);
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
            if (quantityCount < 99) {
                quantityCount++;
                updateSelectedCard();
            }
        });

        cardManualEntry.setOnClickListener(v -> {
            selectCustomItem(cleanText(etSearchFood.getText()));
            scrollToCustomEditor(true);
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
            empty.setText("Chưa thấy món phù hợp. Bạn có thể bấm ‘Thêm món mới’ để tự nhập nhanh kcal và macro.");
            empty.setTextColor(getColor(R.color.on_surface_variant));
            empty.setBackgroundResource(R.drawable.bg_card_surface_low);
            empty.setPadding(dp(16), dp(16), dp(16), dp(16));
            layoutResults.addView(empty);
            return;
        }

        for (FoodCatalogItem item : items) {
            View row = inflater.inflate(R.layout.item_food_result, layoutResults, false);
            TextView tvEmoji = row.findViewById(R.id.tvFoodEmoji);
            TextView tvName = row.findViewById(R.id.tvFoodResultName);
            TextView tvMeta = row.findViewById(R.id.tvFoodResultMeta);
            TextView tvMacros = row.findViewById(R.id.tvFoodResultMacros);
            View btnAdd = row.findViewById(R.id.btnAddFoodResult);

            tvEmoji.setText(item.emoji);
            tvName.setText(item.name);
            tvMeta.setText(String.format(Locale.getDefault(), "%s · %d kcal", item.unitLabel, item.calories));
            tvMacros.setText(String.format(Locale.getDefault(), "P: %.0fg · C: %.0fg · F: %.0fg", item.protein, item.carbs, item.fat));
            row.setOnClickListener(v -> selectItem(item));
            btnAdd.setOnClickListener(v -> selectItem(item));
            layoutResults.addView(row);
        }
    }

    private void autoPrefillIfNeeded() {
        String prefillName = getIntent().getStringExtra(EXTRA_PREFILL_NAME);
        if (TextUtils.isEmpty(prefillName)) {
            return;
        }
        FoodCatalogItem item = FoodCatalog.findByName(prefillName);
        if (item == null) {
            List<FoodCatalogItem> search = FoodCatalog.search(prefillName);
            if (!search.isEmpty()) item = search.get(0);
        }
        if (item != null) {
            selectItem(item);
        } else {
            etSearchFood.setText(prefillName);
            etSearchFood.setSelection(prefillName.length());
            selectCustomItem(prefillName);
            scrollToCustomEditor(false);
        }
    }

    private void enterEditMode() {
        customMode = true;
        quantityCount = Math.max(1, (int) Math.round(getIntent().getDoubleExtra(EXTRA_EDIT_QUANTITY, 1d)));
        suppressCustomWatcher = true;
        try {
            String foodName = safeText(getIntent().getStringExtra(EXTRA_EDIT_FOOD_NAME));
            String unit = safeText(getIntent().getStringExtra(EXTRA_EDIT_UNIT));
            int calories = getIntent().getIntExtra(EXTRA_EDIT_CALORIES, 0);
            double protein = getIntent().getDoubleExtra(EXTRA_EDIT_PROTEIN, 0d);
            double carbs = getIntent().getDoubleExtra(EXTRA_EDIT_CARBS, 0d);
            double fat = getIntent().getDoubleExtra(EXTRA_EDIT_FAT, 0d);
            etSearchFood.setText(foodName);
            etCustomName.setText(foodName);
            etCustomUnit.setText(unit);
            etCustomCalories.setText(calories > 0 ? String.valueOf(calories) : "");
            etCustomProtein.setText(protein > 0 ? trimDecimal(protein) : "");
            etCustomCarbs.setText(carbs > 0 ? trimDecimal(carbs) : "");
            etCustomFat.setText(fat > 0 ? trimDecimal(fat) : "");
        } finally {
            suppressCustomWatcher = false;
        }
        syncCustomItemFromInputs(false);
        updateCustomEntryCard(etSearchFood.getText() == null ? "" : etSearchFood.getText().toString());
        updateSelectedCard();
        scrollToCustomEditor(false);
    }

    private void selectItem(@NonNull FoodCatalogItem item) {
        customMode = false;
        selectedItem = item;
        selectedSource = Constants.NUTRITION_SOURCE_MANUAL;
        selectedConfidence = 0f;
        selectedImageUri = null;
        updateSelectedCard();
    }

    private void selectCustomItem(@Nullable String suggestedName) {
        customMode = true;
        selectedSource = Constants.NUTRITION_SOURCE_MANUAL;
        selectedConfidence = 0f;
        selectedImageUri = null;
        if (!isEditingManualEntry()) {
            if (TextUtils.isEmpty(cleanText(etCustomName.getText())) && !TextUtils.isEmpty(cleanText(suggestedName))) {
                etCustomName.setText(cleanText(suggestedName));
                etCustomName.setSelection(etCustomName.getText().length());
            }
        }
        syncCustomItemFromInputs(true);
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
                TextUtils.isEmpty(name) ? "Món mới" : name,
                1,
                TextUtils.isEmpty(unit) ? "đơn vị" : unit,
                calories,
                protein,
                carbs,
                fat,
                0,
                0,
                0,
                buildCustomHealthNote(calories, protein, carbs, fat),
                name
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
            tvUnitLabel.setText("đơn vị");
            tvSelectedCalories.setText("0 kcal");
            tvSelectedMacros.setText("Protein 0g · Carbs 0g · Fat 0g");
            tvSelectedHealthNote.setText("Gợi ý dinh dưỡng sẽ hiện ở đây");
            btnSaveFood.setText("Lưu vào nhật ký");
            return;
        }

        int calories = (int) Math.round(selectedItem.calories * quantityCount);
        double protein = selectedItem.protein * quantityCount;
        double carbs = selectedItem.carbs * quantityCount;
        double fat = selectedItem.fat * quantityCount;

        if (customMode) {
            String inputName = cleanText(etCustomName.getText());
            String inputUnit = cleanText(etCustomUnit.getText());
            tvSelectedFoodName.setText(TextUtils.isEmpty(inputName) ? "🍽️ Món mới" : "🍽️ " + inputName);
            if (TextUtils.isEmpty(inputName) || TextUtils.isEmpty(inputUnit)) {
                tvSelectedFoodMeta.setText("Điền tên món, đơn vị và macro mỗi khẩu phần rồi bấm lưu.");
            } else {
                tvSelectedFoodMeta.setText(String.format(Locale.getDefault(), "%d × %s · tự nhập", quantityCount, inputUnit));
            }
            tvUnitLabel.setText(TextUtils.isEmpty(inputUnit) ? "khẩu phần" : inputUnit);
            btnSaveFood.setText(isEditingManualEntry() ? "Cập nhật món" : "Lưu món mới");
        } else {
            tvSelectedFoodName.setText(selectedItem.emoji + " " + selectedItem.name);
            tvSelectedFoodMeta.setText(String.format(Locale.getDefault(), "%d × %s", quantityCount, selectedItem.unitLabel));
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
            String inputName = cleanText(etCustomName.getText());
            if (TextUtils.isEmpty(inputName)) {
                Toast.makeText(this, "Bạn nhập tên món trước đã nhé.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedItem.calories <= 0) {
                Toast.makeText(this, "Bạn nên nhập kcal cho món tự đo để nhật ký tính đúng tổng ngày.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        try {
            saving = true;
            updateSelectedCard();
            NutritionEntryEntity entry = new NutritionEntryEntity();
            if (!TextUtils.isEmpty(editingEntryUuid)) {
                entry.entryUuid = editingEntryUuid;
            }
            entry.mealType = selectedMealType;
            entry.foodName = customMode ? cleanText(etCustomName.getText()) : selectedItem.name;
            entry.quantity = quantityCount;
            entry.unit = customMode && !TextUtils.isEmpty(cleanText(etCustomUnit.getText())) ? cleanText(etCustomUnit.getText()) : selectedItem.unitLabel;
            entry.calories = (int) Math.round(selectedItem.calories * quantityCount);
            entry.protein = selectedItem.protein * quantityCount;
            entry.carbs = selectedItem.carbs * quantityCount;
            entry.fat = selectedItem.fat * quantityCount;
            entry.fiber = customMode ? editFiber * quantityCount : selectedItem.fiber * quantityCount;
            entry.sugar = customMode ? editSugar * quantityCount : selectedItem.sugar * quantityCount;
            entry.sodium = customMode ? editSodium * quantityCount : selectedItem.sodium * quantityCount;
            entry.entryDate = selectedDateKey;
            entry.source = Constants.NUTRITION_SOURCE_MANUAL;
            entry.mlConfidence = 0f;
            entry.imageUri = null;
            entry.imageUrl = null;
            entry.imagePublicId = null;
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
            tvCustomEntryTitle.setText("Thêm món mới");
            tvCustomEntrySubtitle.setText("Tự nhập nhanh món bạn vừa ăn.");
        } else {
            tvCustomEntryTitle.setText("Thêm món mới");
            tvCustomEntrySubtitle.setText("Tạo nhanh cho “" + cleanQuery + "”.");
        }
    }

    private String buildCustomHealthNote(int calories, double protein, double carbs, double fat) {
        if (protein >= 20) {
            return "Món này có lượng đạm khá ổn. Nếu đây là bữa chính, bạn có thể cân bằng thêm rau và chất xơ.";
        }
        if (fat >= 18) {
            return "Món này có chất béo tương đối cao. Bạn nên chú ý tổng fat của cả ngày để dễ cân bằng hơn.";
        }
        if (carbs >= 45 && protein < 10) {
            return "Món này thiên về carb. Nếu ăn một mình, bạn có thể cân nhắc thêm nguồn đạm cho no lâu hơn.";
        }
        if (calories <= 180) {
            return "Khá hợp làm bữa phụ nhẹ. Bạn có thể kết hợp thêm đạm hoặc trái cây nếu muốn no lâu hơn.";
        }
        return "Bạn có thể chỉnh lại kcal và macro theo đúng khẩu phần mình vừa ăn để nhật ký phản ánh sát hơn.";
    }

    private void scrollToCustomEditor(boolean smooth) {
        if (scrollAddFood == null || layoutCustomEditor == null || bottomSheet == null) return;
        layoutCustomEditor.setVisibility(View.VISIBLE);
        scrollAddFood.post(() -> {
            int y = Math.max(0, bottomSheet.getTop() + layoutCustomEditor.getTop() - dp(24));
            if (smooth) {
                scrollAddFood.smoothScrollTo(0, y);
                scrollAddFood.postDelayed(() -> scrollAddFood.smoothScrollTo(0, y), 120L);
            } else {
                scrollAddFood.scrollTo(0, y);
            }
            etCustomName.requestFocus();
        });
    }

    private boolean isEditingManualEntry() {
        return !TextUtils.isEmpty(editingEntryUuid);
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private int parseInt(@Nullable CharSequence value, int fallback) {
        try {
            return TextUtils.isEmpty(value) ? fallback : Integer.parseInt(value.toString().trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private double parseDouble(@Nullable CharSequence value, double fallback) {
        try {
            return TextUtils.isEmpty(value) ? fallback : Double.parseDouble(value.toString().trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private String trimDecimal(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001d) {
            return String.valueOf((int) Math.rint(value));
        }
        return String.format(Locale.getDefault(), "%.1f", value);
    }

    private String cleanText(@Nullable CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }

    private String safeText(@Nullable String value) {
        return value == null ? "" : value;
    }

    private String safeMeal(@Nullable String meal) {
        if (Constants.MEAL_BREAKFAST.equals(meal)
                || Constants.MEAL_LUNCH.equals(meal)
                || Constants.MEAL_DINNER.equals(meal)
                || Constants.MEAL_SNACK.equals(meal)) {
            return meal;
        }
        return Constants.MEAL_LUNCH;
    }

    private String readableMeal(@NonNull String meal) {
        switch (meal) {
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
}
