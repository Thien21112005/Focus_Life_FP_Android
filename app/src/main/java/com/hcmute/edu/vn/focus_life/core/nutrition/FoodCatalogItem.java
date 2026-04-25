package com.hcmute.edu.vn.focus_life.core.nutrition;

public class FoodCatalogItem {
    public final String id;
    public final String emoji;
    public final String name;
    public final double defaultQuantity;
    public final String unitLabel;
    public final int calories;
    public final double protein;
    public final double carbs;
    public final double fat;
    public final double fiber;
    public final double sugar;
    public final double sodium;
    public final String healthNote;
    public final String[] aliases;

    public FoodCatalogItem(String id,
                           String emoji,
                           String name,
                           double defaultQuantity,
                           String unitLabel,
                           int calories,
                           double protein,
                           double carbs,
                           double fat,
                           double fiber,
                           double sugar,
                           double sodium,
                           String healthNote,
                           String... aliases) {
        this.id = id;
        this.emoji = emoji;
        this.name = name;
        this.defaultQuantity = defaultQuantity;
        this.unitLabel = unitLabel;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
        this.fiber = fiber;
        this.sugar = sugar;
        this.sodium = sodium;
        this.healthNote = healthNote;
        this.aliases = aliases == null ? new String[0] : aliases;
    }
}
