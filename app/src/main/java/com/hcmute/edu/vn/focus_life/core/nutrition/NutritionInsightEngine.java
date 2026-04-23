package com.hcmute.edu.vn.focus_life.core.nutrition;

import androidx.annotation.NonNull;

import com.hcmute.edu.vn.focus_life.data.local.entity.NutritionEntryEntity;

import java.util.ArrayList;
import java.util.List;

public final class NutritionInsightEngine {

    public static class DayInsight {
        public String headline;
        public String recommendation;
        public final List<String> warnings = new ArrayList<>();
    }

    private NutritionInsightEngine() {
    }

    @NonNull
    public static DayInsight buildDayInsight(List<NutritionEntryEntity> entries, NutritionDaySummary summary) {
        DayInsight insight = new DayInsight();
        if (summary == null || entries == null || entries.isEmpty()) {
            insight.headline = "Hôm nay bạn chưa log món nào";
            insight.recommendation = "Bắt đầu bằng cách thêm món thủ công hoặc quét ảnh để app đoán món, ước tính kcal và cảnh báo nhanh.";
            return insight;
        }

        int remain = summary.remainingCalories();
        if (remain > 450) {
            insight.headline = "Bạn còn thiếu khoảng " + remain + " kcal cho mục tiêu hôm nay";
            insight.recommendation = "Ưu tiên thêm bữa phụ giàu đạm vừa phải như sữa chua Hy Lạp, ức gà hoặc trái cây + protein để no lâu hơn.";
        } else if (remain >= 0) {
            insight.headline = "Bạn đang bám khá sát mục tiêu năng lượng";
            insight.recommendation = "Một bữa nhẹ nhỏ, ít natri và giàu chất xơ sẽ giúp ngày ăn uống cân bằng hơn.";
        } else {
            insight.headline = "Bạn đã vượt mục tiêu khoảng " + Math.abs(remain) + " kcal";
            insight.recommendation = "Các bữa còn lại nên ưu tiên món nhẹ, ít đường và ít dầu để cân bằng tổng năng lượng trong ngày.";
        }

        if (summary.sodium > 2000) {
            insight.warnings.add("Tổng natri hôm nay đang khá cao (" + Math.round(summary.sodium) + "mg). Nên giảm nước dùng, nước chấm và đồ chế biến mặn.");
        }
        if (summary.sugar > 36) {
            insight.warnings.add("Lượng đường đang cao hơn mức nên kiểm soát. Hạn chế thêm trà sữa, nước ngọt hoặc topping ngọt vào cuối ngày.");
        }
        if (summary.fiber < 25 && summary.calories > 700) {
            insight.warnings.add("Chất xơ còn thấp. Bạn nên thêm rau xanh, salad hoặc trái cây nguyên quả để hỗ trợ tiêu hóa.");
        }
        if (summary.protein < 60 && summary.calories < summary.calorieGoal) {
            insight.warnings.add("Protein hôm nay còn hơi thấp so với lượng kcal đã nạp. Có thể bổ sung ức gà, cá, trứng hoặc sữa chua Hy Lạp.");
        }
        return insight;
    }

    public static String buildEntryFlags(FoodCatalogItem item) {
        if (item == null) return "";
        return buildEntryFlags(item.protein, item.fiber, item.sugar, item.sodium);
    }

    public static String buildEntryFlags(double protein, double fiber, double sugar, double sodium) {
        List<String> flags = new ArrayList<>();
        if (sodium >= 700) flags.add("natri_cao");
        if (sugar >= 18) flags.add("duong_cao");
        if (protein >= 20) flags.add("protein_tot");
        if (fiber >= 4) flags.add("giau_chat_xo");

        StringBuilder builder = new StringBuilder();
        for (String flag : flags) {
            if (builder.length() > 0) builder.append(',');
            builder.append(flag);
        }
        return builder.toString();
    }
}
