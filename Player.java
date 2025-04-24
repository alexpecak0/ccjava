package com.example.ccjava;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Player {
    protected String name;
    protected boolean isHuman;
    protected Map<String, Integer> scorecard;
    // Make filledCategories static so it's shared across all players
    protected static Map<String, Boolean> filledCategories = new HashMap<>();
    protected String lastCategory;
    protected int lastScore;

    public Player(String name) {
        this.name = name;
        this.isHuman = true;
        this.scorecard = new HashMap<>();
        this.lastCategory = "";
        this.lastScore = 0;
        initializeScorecard();
    }

    private void initializeScorecard() {
        // Initialize all categories with zero score
        for (String category : Constants.CATEGORIES) {
            scorecard.put(category, 0);
            // Only initialize filledCategories if it hasn't been initialized by another player
            if (!filledCategories.containsKey(category)) {
                filledCategories.put(category, false);
            }
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getAvailableCategories() {
        List<String> available = new ArrayList<>();
        for (String category : Constants.CATEGORIES) {
            if (!filledCategories.get(category)) {
                available.add(category);
            }
        }
        return available;
    }

    public boolean isCategoryFilled(String category) {
        return filledCategories.getOrDefault(category, false);
    }

    public void fillCategory(String category, int score) {
        if (!isCategoryFilled(category)) {
            scorecard.put(category, score);
            filledCategories.put(category, true);
            lastCategory = category;
            lastScore = score;
        }
    }

    // Add method to reset filled categories (useful for new games)
    public static void resetFilledCategories() {
        filledCategories.clear();
        for (String category : Constants.CATEGORIES) {
            filledCategories.put(category, false);
        }
    }

    public int getScore(String category) {
        return scorecard.getOrDefault(category, 0);
    }

    public int getTotalScore() {
        int total = 0;
        for (int score : scorecard.values()) {
            total += score;
        }
        return total;
    }

    public boolean allCategoriesFilled() {
        for (boolean filled : filledCategories.values()) {
            if (!filled) return false;
        }
        return true;
    }

    public String getLastCategory() {
        return lastCategory;
    }

    public int getLastScore() {
        return lastScore;
    }

    public String getGameState() {
        StringBuilder state = new StringBuilder();
        state.append("Player: ").append(name).append("\n");
        state.append("Scores:\n");

        for (String category : Constants.CATEGORIES) {
            if (filledCategories.get(category)) {
                state.append(category)
                        .append(": ")
                        .append(scorecard.get(category))
                        .append("\n");
            }
        }

        state.append("Total Score: ").append(getTotalScore()).append("\n");
        return state.toString();
    }

    // Rest of the methods remain unchanged
    public String announceBeforeRoll(int rollNumber) {
        return name + "'s turn - Roll #" + rollNumber;
    }

    public String announceAfterRoll(int[] diceValues) {
        StringBuilder announcement = new StringBuilder();
        announcement.append(name).append(" rolled: ");
        for (int value : diceValues) {
            announcement.append(value).append(" ");
        }
        return announcement.toString();
    }

    public String announceCategoryFilled(String category, int score) {
        return name + " scored " + score + " points in " + category;
    }

    public Map<String, Integer> getPotentialScores(int[] diceValues) {
        Map<String, Integer> potentialScores = new HashMap<>();
        for (String category : getAvailableCategories()) {
            int score = Helper.calculateScore(category, diceValues);
            if (score > 0) {
                potentialScores.put(category, score);
            }
        }
        return potentialScores;
    }

    public String getSaveString() {
        StringBuilder save = new StringBuilder();
        for (String category : Constants.CATEGORIES) {
            save.append(scorecard.get(category))
                    .append(" ")
                    .append(filledCategories.get(category))
                    .append("\n");
        }
        return save.toString();
    }

    public void loadFromString(String saveData) {
        String[] lines = saveData.split("\n");
        int index = 0;
        for (String category : Constants.CATEGORIES) {
            if (index < lines.length) {
                String[] parts = lines[index].split(" ");
                if (parts.length == 2) {
                    scorecard.put(category, Integer.parseInt(parts[0]));
                    filledCategories.put(category, Boolean.parseBoolean(parts[1]));
                }
            }
            index++;
        }
    }

    public Map<String, Integer> getScores() {
        return new HashMap<>(scorecard); // Return a copy to prevent modification
    }
}