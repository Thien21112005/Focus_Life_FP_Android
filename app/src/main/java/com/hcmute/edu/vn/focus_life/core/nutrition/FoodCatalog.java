package com.hcmute.edu.vn.focus_life.core.nutrition;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class FoodCatalog {
    public static class MatchResult {
        @Nullable
        public FoodCatalogItem item;
        public float confidence;
        public final List<String> matchedLabels = new ArrayList<>();
    }

    private static final List<FoodCatalogItem> ITEMS;

    static {
        List<FoodCatalogItem> list = new ArrayList<>();
        list.add(new FoodCatalogItem("bun_bo_hue", "🍜", "Bún bò Huế", 1, "tô (500g)", 380, 28, 45, 8, 2, 3, 820,
                "Natri khá cao. Nếu bạn đang giữ nước hoặc kiểm soát huyết áp, nên giảm nước dùng.",
                "bun bo", "bun bo hue", "beef noodle soup", "noodle", "soup", "beef", "broth", "bowl", "dish", "cuisine"));
        list.add(new FoodCatalogItem("com_tam", "🍛", "Cơm tấm sườn bì chả", 1, "phần", 650, 32, 78, 21, 3, 7, 980,
                "Năng lượng cao, phù hợp bữa chính. Natri tương đối cao nếu thêm nước mắm.",
                "com tam", "broken rice", "rice", "pork", "meat", "plate", "dish", "cuisine"));
        list.add(new FoodCatalogItem("pho_bo", "🥣", "Phở bò", 1, "tô (450g)", 430, 24, 52, 11, 2, 4, 760,
                "Khá cân bằng cho bữa sáng hoặc trưa, nhưng vẫn nên chú ý lượng muối trong nước dùng.",
                "pho", "pho bo", "beef pho", "noodle soup", "soup", "beef noodle", "broth", "bowl", "dish", "cuisine"));
        list.add(new FoodCatalogItem("banh_mi_trung", "🥖", "Bánh mì trứng", 1, "ổ", 350, 15, 37, 16, 2, 4, 540,
                "Phù hợp bữa sáng nhanh. Bạn có thể thêm rau hoặc trái cây để cân bằng chất xơ.",
                "banh mi", "sandwich", "bread", "egg sandwich", "toast", "bakery"));
        list.add(new FoodCatalogItem("sua_chua_hy_lap", "🥣", "Sữa chua Hy Lạp", 1, "hũ (150g)", 130, 12, 10, 4, 0, 8, 55,
                "Bổ sung protein khá tốt cho bữa phụ. Chọn loại ít đường sẽ tối ưu hơn.",
                "yogurt", "greek yogurt", "dessert", "cup", "dairy"));
        list.add(new FoodCatalogItem("chuoi", "🍌", "Chuối", 1, "quả", 105, 1.3, 27, 0.3, 3, 14, 1,
                "Phù hợp bữa phụ trước tập hoặc khi cần nạp nhanh carb dễ tiêu.",
                "banana", "fruit", "yellow fruit", "produce"));
        list.add(new FoodCatalogItem("uc_ga", "🍗", "Ức gà áp chảo", 1, "phần (150g)", 248, 38, 0, 8, 0, 0, 320,
                "Protein cao, phù hợp phục hồi cơ bắp. Nên ăn kèm rau để đỡ khô và tăng vi chất.",
                "chicken breast", "chicken", "grilled chicken", "meat", "protein"));
        list.add(new FoodCatalogItem("salad_ca_ngu", "🥗", "Salad cá ngừ", 1, "tô", 220, 23, 12, 9, 4, 5, 410,
                "Ít calo, giàu đạm và chất xơ. Phù hợp khi bạn cần bữa tối nhẹ hơn.",
                "salad", "tuna salad", "fish salad", "vegetable", "greens", "leafy"));
        list.add(new FoodCatalogItem("bun_bo_xao", "🍝", "Bún bò xào", 1, "đĩa", 420, 25, 38, 14, 2, 6, 690,
                "Hàm lượng carb và chất béo trung bình, phù hợp bữa trưa vận động vừa phải.",
                "stir fried noodle", "fried noodle", "beef noodle", "plate noodle", "fried", "noodle"));
        list.add(new FoodCatalogItem("trai_cay_mix", "🍎", "Trái cây hỗn hợp", 1, "phần", 140, 2, 33, 0.5, 5, 24, 3,
                "Giàu vi chất và chất xơ, nhưng nếu đang siết đường thì nên chú ý khẩu phần.",
                "fruit bowl", "fruit", "apple", "orange", "berries", "produce"));
        list.add(new FoodCatalogItem("sinh_to_bo", "🥑", "Sinh tố bơ", 1, "ly", 280, 4, 24, 18, 6, 14, 120,
                "Năng lượng khá cao do chất béo tốt. Hợp bữa phụ tăng năng lượng nhưng không nên quá muộn buổi tối.",
                "smoothie", "avocado smoothie", "drink", "juice"));
        list.add(new FoodCatalogItem("com_ga", "🍗", "Cơm gà", 1, "phần", 540, 30, 64, 16, 2, 4, 720,
                "Bữa chính nhiều năng lượng. Có thể giảm da gà để bớt chất béo nếu đang cắt mỡ.",
                "chicken rice", "rice chicken", "rice dish", "plate", "chicken", "rice"));
        ITEMS = Collections.unmodifiableList(list);
    }

    private FoodCatalog() {
    }

    public static List<FoodCatalogItem> getQuickPicks() {
        return ITEMS.subList(0, Math.min(8, ITEMS.size()));
    }

    public static List<FoodCatalogItem> search(@Nullable String query) {
        String normalized = normalize(query);
        if (normalized.isEmpty()) {
            return new ArrayList<>(ITEMS.subList(0, Math.min(10, ITEMS.size())));
        }

        List<FoodCatalogItem> result = new ArrayList<>();
        for (FoodCatalogItem item : ITEMS) {
            if (score(item, normalized) > 0) {
                result.add(item);
            }
        }
        result.sort(Comparator.comparingInt((FoodCatalogItem item) -> score(item, normalized)).reversed());
        return result;
    }

    @Nullable
    public static FoodCatalogItem findByName(@Nullable String name) {
        String normalized = normalize(name);
        for (FoodCatalogItem item : ITEMS) {
            if (normalize(item.name).equals(normalized)) {
                return item;
            }
        }
        return null;
    }

    @NonNull
    public static List<FoodCatalogItem> rankFromLabels(@Nullable List<String> labels) {
        return rankScores(labels).rankedItems;
    }

    public static MatchResult matchFromLabels(@Nullable List<String> labels) {
        MatchResult result = new MatchResult();
        if (labels == null || labels.isEmpty()) return result;

        for (String label : labels) {
            String normalized = normalize(label);
            if (!normalized.isEmpty()) {
                result.matchedLabels.add(normalized);
            }
        }

        ScoreBundle bundle = rankScores(labels);
        if (bundle.rankedItems.isEmpty()) {
            return result;
        }

        result.item = bundle.rankedItems.get(0);
        int topScore = bundle.scores.get(result.item);
        int secondScore = bundle.rankedItems.size() > 1 ? bundle.scores.get(bundle.rankedItems.get(1)) : 0;

        float confidence = bundle.genericOnly ? 0.34f : 0.5f;
        confidence += Math.min(0.24f, topScore * 0.04f);
        confidence += Math.min(0.1f, Math.max(0, topScore - secondScore) * 0.03f);
        result.confidence = Math.max(0.28f, Math.min(0.92f, confidence));
        return result;
    }

    @NonNull
    private static ScoreBundle rankScores(@Nullable List<String> labels) {
        ScoreBundle bundle = new ScoreBundle();
        if (labels == null || labels.isEmpty()) {
            return bundle;
        }

        String combined = joinLabels(labels);
        bundle.genericOnly = isGenericFoodSignalOnly(combined);
        for (FoodCatalogItem item : ITEMS) {
            bundle.scores.put(item, score(item, combined));
        }
        applyHeuristicScores(bundle.scores, combined);
        for (Map.Entry<FoodCatalogItem, Integer> entry : bundle.scores.entrySet()) {
            if (entry.getValue() > 0) {
                bundle.rankedItems.add(entry.getKey());
            }
        }
        bundle.rankedItems.sort((left, right) -> Integer.compare(bundle.scores.get(right), bundle.scores.get(left)));
        return bundle;
    }

    private static void applyHeuristicScores(@NonNull Map<FoodCatalogItem, Integer> scores, @NonNull String combined) {
        if (containsAny(combined, "banana", "fruit", "produce", "yellow fruit")) {
            boost(scores, "Chuối", 7);
            boost(scores, "Trái cây hỗn hợp", 4);
        }
        if (containsAny(combined, "bread", "sandwich", "toast", "bakery")) {
            boost(scores, "Bánh mì trứng", 8);
        }
        if (containsAny(combined, "yogurt", "dessert", "dairy", "cup")) {
            boost(scores, "Sữa chua Hy Lạp", 7);
        }
        if (containsAny(combined, "salad", "vegetable", "greens", "leaf", "leafy")) {
            boost(scores, "Salad cá ngừ", 7);
            boost(scores, "Trái cây hỗn hợp", 2);
        }
        if (containsAny(combined, "smoothie", "drink", "juice", "avocado")) {
            boost(scores, "Sinh tố bơ", 8);
        }
        if (containsAny(combined, "chicken", "grilled chicken", "protein", "meat")) {
            boost(scores, "Ức gà áp chảo", 5);
            boost(scores, "Cơm gà", 4);
        }
        if (containsAny(combined, "rice", "plate", "pork", "rice dish")) {
            boost(scores, "Cơm tấm sườn bì chả", 7);
            boost(scores, "Cơm gà", 4);
        }
        if (containsAny(combined, "noodle", "soup", "broth", "bowl", "ramen")) {
            boost(scores, "Phở bò", 6);
            boost(scores, "Bún bò Huế", 6);
            boost(scores, "Bún bò xào", 3);
        }
        if (containsAny(combined, "beef", "beef noodle", "beef pho")) {
            boost(scores, "Phở bò", 6);
            boost(scores, "Bún bò Huế", 5);
            boost(scores, "Bún bò xào", 4);
        }
        if (containsAny(combined, "fried", "stir fried", "plate noodle")) {
            boost(scores, "Bún bò xào", 8);
        }
        if (containsAny(combined, "food", "dish", "cuisine", "meal", "recipe", "ingredient")
                && isGenericFoodSignalOnly(combined)) {
            boost(scores, "Phở bò", 4);
            boost(scores, "Bún bò Huế", 3);
            boost(scores, "Cơm tấm sườn bì chả", 2);
        }
    }

    private static void boost(@NonNull Map<FoodCatalogItem, Integer> scores, @NonNull String name, int delta) {
        FoodCatalogItem item = findByName(name);
        if (item == null) return;
        Integer current = scores.get(item);
        scores.put(item, (current == null ? 0 : current) + delta);
    }

    private static int score(FoodCatalogItem item, String query) {
        if (query == null || query.trim().isEmpty()) return 0;
        String normalizedName = normalize(item.name);
        int score = 0;
        if (normalizedName.contains(query) || query.contains(normalizedName)) {
            score += 8;
        }
        for (String alias : item.aliases) {
            String normalizedAlias = normalize(alias);
            if (query.contains(normalizedAlias)) {
                score += 4;
            }
            for (String token : normalizedAlias.split(" ")) {
                if (!token.isEmpty() && query.contains(token)) {
                    score += 1;
                }
            }
        }
        for (String token : normalizedName.split(" ")) {
            if (!token.isEmpty() && query.contains(token)) {
                score += 2;
            }
        }
        return score;
    }

    private static boolean containsAny(@NonNull String combined, String... keywords) {
        for (String keyword : keywords) {
            if (combined.contains(normalize(keyword))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isGenericFoodSignalOnly(@NonNull String combined) {
        String[] tokens = combined.split("\\s+");
        int specificCount = 0;
        for (String token : tokens) {
            if (token.isEmpty()) continue;
            if (!isGenericFoodToken(token)) {
                specificCount++;
            }
        }
        return specificCount == 0;
    }

    private static boolean isGenericFoodToken(@NonNull String token) {
        return token.equals("food")
                || token.equals("dish")
                || token.equals("cuisine")
                || token.equals("meal")
                || token.equals("ingredient")
                || token.equals("recipe")
                || token.equals("plant")
                || token.equals("flower")
                || token.equals("produce")
                || token.equals("tableware")
                || token.equals("serveware");
    }

    @NonNull
    private static String joinLabels(@Nullable List<String> labels) {
        StringBuilder builder = new StringBuilder();
        if (labels != null) {
            for (String label : labels) {
                String normalized = normalize(label);
                if (!normalized.isEmpty()) {
                    builder.append(normalized).append(' ');
                }
            }
        }
        return builder.toString().trim();
    }

    private static String normalize(@Nullable String raw) {
        if (raw == null) return "";
        return raw.toLowerCase(Locale.ROOT).trim();
    }

    private static class ScoreBundle {
        final Map<FoodCatalogItem, Integer> scores = new LinkedHashMap<>();
        final List<FoodCatalogItem> rankedItems = new ArrayList<>();
        boolean genericOnly;
    }
}
