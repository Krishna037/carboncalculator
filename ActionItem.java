package com.example.carbonfootprintcalculator;

public class ActionItem {
    private String name;
    private String date;
    private float carbonFootprint;
    private String category;
    private boolean completed;

    public ActionItem(String name, String date, float carbonFootprint) {
        this.name = name;
        this.date = date;
        this.carbonFootprint = carbonFootprint;
        this.completed = false;
        this.category = determineCategory(name);
    }

    private String determineCategory(String name) {
        name = name.toLowerCase();
        if (name.contains("transport") || name.contains("car") || name.contains("bus") || name.contains("flight")) {
            return "Transport";
        } else if (name.contains("electricity") || name.contains("power")) {
            return "Electricity";
        } else if (name.contains("food") || name.contains("meal")) {
            return "Food";
        } else {
            return "Other";
        }
    }

    public String getName() { return name; }
    public String getDate() { return date; }
    public float getCarbonFootprint() { return carbonFootprint; }
    public String getCategory() { return category; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}