package com.example.ccjava;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class ComputerPlayer extends Player {
    private boolean[] selectedDice;
    private String currentStrategy;
    private String lastCategory;
    private int lastScore;
    private int[] scorecard;

    public ComputerPlayer() {
        super("Computer");
        selectedDice = new boolean[5];
        scorecard = new int[13];  // One for each category
        lastCategory = "";
        lastScore = 0;
    }

    public String analyzeRoll(int[] diceValues) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("Current Dice: ").append(Arrays.toString(diceValues)).append("\n\n");

        // Calculate potential scores for each category
        Map<String, Integer> potentialScores = new HashMap<>();
        for (String category : getAvailableCategories()) {
            int score = Helper.calculateScore(category, diceValues);
            if (score > 0) {
                potentialScores.put(category, score);
            }
        }

        // Determine best strategy based on current roll
        determineStrategy(diceValues, potentialScores);

        // Build detailed explanation as required by rubric
        analysis.append("Available Categories:\n");
        for (Map.Entry<String, Integer> entry : potentialScores.entrySet()) {
            analysis.append(entry.getKey())
                    .append(" (Potential points: ")
                    .append(entry.getValue())
                    .append(")\n");
        }

        // Add strategic explanation
        analysis.append("\nStrategy Analysis:\n");
        analysis.append("Current Strategy: ").append(currentStrategy).append("\n");

        // Explain dice selection
        analysis.append("\nDice Selection:\n");
        int selectedCount = 0;
        for (int i = 0; i < diceValues.length; i++) {
            if (selectedDice[i]) {
                selectedCount++;
                analysis.append("Keeping ").append(diceValues[i]);
                if (currentStrategy.contains("Yahtzee")) {
                    analysis.append(" (Potential Yahtzee)");
                } else if (currentStrategy.contains("Straight")) {
                    analysis.append(" (Part of Straight)");
                } else if (currentStrategy.contains("Full House")) {
                    analysis.append(" (Part of Full House)");
                } else if (currentStrategy.contains("Kind")) {
                    analysis.append(" (Part of ").append(currentStrategy).append(")");
                } else {
                    analysis.append(" (For ").append(currentStrategy).append(")");
                }
                analysis.append("\n");
            }
        }

        // Explain why we're keeping these dice
        analysis.append("\nReasoning:\n");
        if (selectedCount == 0) {
            analysis.append("No good combinations found. Will try for a better roll.\n");
        } else if (selectedCount == 5) {
            analysis.append("Have a complete combination! Ready to score.\n");
        } else {
            analysis.append("Keeping ").append(selectedCount).append(" dice to build towards ").append(currentStrategy).append("\n");
        }

        return analysis.toString();
    }

    private void determineStrategy(int[] diceValues, Map<String, Integer> potentialScores) {
        // Reset dice selection
        Arrays.fill(selectedDice, false);

        // Calculate current game state
        int upperSectionScore = calculateUpperSectionScore();
        int remainingRolls = 3; // This should be passed in from the game state
        boolean needsUpperBonus = upperSectionScore < 63;

        // Find best potential category considering game state
        String bestCategory = null;
        int bestScore = 0;
        double bestExpectedValue = 0;

        for (Map.Entry<String, Integer> entry : potentialScores.entrySet()) {
            String category = entry.getKey();
            int score = entry.getValue();
            double expectedValue = calculateExpectedValue(category, score, needsUpperBonus, remainingRolls);

            if (expectedValue > bestExpectedValue) {
                bestExpectedValue = expectedValue;
                bestScore = score;
                bestCategory = category;
            }
        }

        if (bestCategory != null) {
            switch (bestCategory) {
                case "Yahtzee":
                    handleYahtzeeStrategy(diceValues);
                    break;

                case "Large Straight":
                    handleLargeStraightStrategy(diceValues);
                    break;

                case "Small Straight":
                    handleSmallStraightStrategy(diceValues);
                    break;

                case "Full House":
                    handleFullHouseStrategy(diceValues);
                    break;

                case "Four of a Kind":
                    handleMultipleOfKindStrategy(diceValues, 4);
                    break;

                case "Three of a Kind":
                    handleMultipleOfKindStrategy(diceValues, 3);
                    break;

                default:
                    handleNumberStrategy(diceValues, bestCategory);
                    break;
            }
        }
    }

    private int calculateUpperSectionScore() {
        int score = 0;
        for (String category : Arrays.asList("Aces", "Twos", "Threes", "Fours", "Fives", "Sixes")) {
            if (!getAvailableCategories().contains(category)) {
                score += getLastScoreForCategory(category);
            }
        }
        return score;
    }

    private double calculateExpectedValue(String category, int currentScore, boolean needsUpperBonus, int remainingRolls) {
        double baseValue = currentScore;

        // Adjust for upper section bonus potential
        if (category.equals("Aces") || category.equals("Twos") ||
                category.equals("Threes") || category.equals("Fours") ||
                category.equals("Fives") || category.equals("Sixes")) {
            if (needsUpperBonus) {
                baseValue += 35.0 / remainingRolls; // Potential bonus value
            }
        }

        // Adjust for Yahtzee bonus potential
        if (category.equals("Yahtzee")) {
            baseValue *= 1.5; // Yahtzee bonus potential
        }

        // Adjust for difficulty of category
        switch (category) {
            case "Yahtzee":
                baseValue *= 1.2; // Hard to get, worth more
                break;
            case "Large Straight":
                baseValue *= 1.1;
                break;
            case "Full House":
                baseValue *= 1.05;
                break;
            case "Small Straight":
                baseValue *= 1.0;
                break;
            case "Four of a Kind":
                baseValue *= 0.9;
                break;
            case "Three of a Kind":
                baseValue *= 0.8;
                break;
            default: // Upper section
                baseValue *= 0.7;
                break;
        }

        return baseValue;
    }

    private int getLastScoreForCategory(String category) {
        // This should be implemented to return the last score for a category
        // For now, returning 0 as placeholder
        return 0;
    }

    private void handleYahtzeeStrategy(int[] diceValues) {
        int mostCommon = findMostCommonValue(diceValues);
        currentStrategy = "Pursuing Yahtzee with " + mostCommon + "s";
        for (int i = 0; i < diceValues.length; i++) {
            selectedDice[i] = (diceValues[i] == mostCommon);
        }
    }

    private void handleLargeStraightStrategy(int[] diceValues) {
        currentStrategy = "Pursuing Large Straight";
        // Reset selection array
        Arrays.fill(selectedDice, false);

        // Keep sequential dice for large straight
        for (int i = 0; i < diceValues.length; i++) {
            int value = diceValues[i];
            // Only set true if part of the sequence
            selectedDice[i] = isPartOfLargestSequence(diceValues, value);
        }
    }

    private void handleSmallStraightStrategy(int[] diceValues) {
        currentStrategy = "Pursuing Small Straight";
        // Similar to large straight but only needs 4 sequential
        for (int i = 0; i < diceValues.length; i++) {
            int value = diceValues[i];
            if (isPartOfSmallestSequence(diceValues, value)) {
                selectedDice[i] = true;
            }
        }
    }

    private void handleFullHouseStrategy(int[] diceValues) {
        currentStrategy = "Pursuing Full House";
        int[] counts = getCounts(diceValues);
        int threeOfAKind = -1;
        int pair = -1;

        // Find the three of a kind and pair
        for (int i = 1; i <= 6; i++) {
            if (counts[i] >= 3) threeOfAKind = i;
            else if (counts[i] >= 2) pair = i;
        }

        // Keep appropriate dice
        for (int i = 0; i < diceValues.length; i++) {
            if (diceValues[i] == threeOfAKind || diceValues[i] == pair) {
                selectedDice[i] = true;
            }
        }
    }

    private void handleMultipleOfKindStrategy(int[] diceValues, int count) {
        int value = findMostCommonValue(diceValues);
        currentStrategy = "Pursuing " + count + " of a kind with " + value + "s";
        for (int i = 0; i < diceValues.length; i++) {
            selectedDice[i] = (diceValues[i] == value);
        }
    }

    private void handleNumberStrategy(int[] diceValues, String category) {
        int targetNumber = getCategoryNumber(category);
        currentStrategy = "Collecting " + category;
        for (int i = 0; i < diceValues.length; i++) {
            selectedDice[i] = (diceValues[i] == targetNumber);
        }
    }

    // Helper methods
    private int findMostCommonValue(int[] diceValues) {
        int[] counts = getCounts(diceValues);
        int maxCount = 0;
        int mostCommon = 1;

        for (int i = 1; i <= 6; i++) {
            if (counts[i] > maxCount) {
                maxCount = counts[i];
                mostCommon = i;
            }
        }
        return mostCommon;
    }

    private int[] getCounts(int[] diceValues) {
        int[] counts = new int[7];  // 0-6, ignore 0
        for (int value : diceValues) {
            counts[value]++;
        }
        return counts;
    }

    private boolean isPartOfLargestSequence(int[] diceValues, int value) {
        // Check if this value is part of the longest possible straight
        boolean[] present = new boolean[7];
        for (int v : diceValues) present[v] = true;

        int maxLen = 0;
        int currentLen = 0;
        for (int i = 1; i <= 6; i++) {
            if (present[i]) {
                currentLen++;
                maxLen = Math.max(maxLen, currentLen);
            } else {
                currentLen = 0;
            }
        }
        return present[value] && maxLen >= 4;
    }

    private boolean isPartOfSmallestSequence(int[] diceValues, int value) {
        // Similar to largest sequence but only needs 4 in a row
        return isPartOfLargestSequence(diceValues, value);
    }

    private int getCategoryNumber(String category) {
        switch (category) {
            case "Aces": return 1;
            case "Twos": return 2;
            case "Threes": return 3;
            case "Fours": return 4;
            case "Fives": return 5;
            case "Sixes": return 6;
            default: return 0;
        }
    }

    public boolean[] getSelectedDice() {
        return selectedDice;
    }

    public boolean shouldRollAgain() {
        // If we have no strategy (no dice selected), we should roll again
        boolean hasStrategy = false;
        for (boolean isSelected : selectedDice) {
            if (isSelected) {
                hasStrategy = true;
                break;
            }
        }
        if (!hasStrategy) return true;

        // If we have a strategy but no dice match it, we should roll again
        boolean hasMatchingDice = false;
        for (boolean isSelected : selectedDice) {
            if (isSelected) {
                hasMatchingDice = true;
                break;
            }
        }
        if (!hasMatchingDice) return true;

        // If we have all the dice we want for our current strategy, don't roll again
        int matchingDiceCount = 0;
        for (boolean isSelected : selectedDice) {
            if (isSelected) matchingDiceCount++;
        }

        // For different strategies, we need different numbers of matching dice
        switch (currentStrategy) {
            case "Yahtzee":
                return matchingDiceCount < 5;
            case "Four of a Kind":
                return matchingDiceCount < 4;
            case "Three of a Kind":
                return matchingDiceCount < 3;
            case "Full House":
                return matchingDiceCount < 5;
            case "Large Straight":
            case "Small Straight":
                return matchingDiceCount < 4;
            default: // For number categories (Aces, Twos, etc.)
                return matchingDiceCount < 3;
        }
    }

    public String selectCategory(int[] diceValues) {
        Map<String, Integer> scores = new HashMap<>();
        List<String> availableCategories = getAvailableCategories();
        for (String category : availableCategories) {
            scores.put(category, Helper.calculateScore(category, diceValues));
        }

        String bestCategory = null;
        int bestScore = -1;
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestScore = entry.getValue();
                bestCategory = entry.getKey();
            }
        }

        // If no scoring category found, pick the first available upper section category
        // or the first available category if no upper section categories remain
        if (bestScore == 0) {
            for (String category : availableCategories) {
                if (category.equals("Aces") || category.equals("Twos") ||
                        category.equals("Threes") || category.equals("Fours") ||
                        category.equals("Fives") || category.equals("Sixes")) {
                    bestCategory = category;
                    break;
                }
            }
            if (bestCategory == null && !availableCategories.isEmpty()) {
                bestCategory = availableCategories.get(0);
            }
        }

        if (bestCategory != null) {
            lastCategory = bestCategory;
            lastScore = scores.get(bestCategory);
            super.fillCategory(bestCategory, lastScore);
        }

        return bestCategory;
    }

    private int getCategoryIndex(String category) {
        // Convert category name to index
        return Arrays.asList(Constants.CATEGORIES).indexOf(category);
    }

    // Use parent class implementations for these methods
    @Override
    public List<String> getAvailableCategories() {
        return super.getAvailableCategories();
    }

    @Override
    public boolean allCategoriesFilled() {
        return super.allCategoriesFilled();
    }

    @Override
    public int getTotalScore() {
        return super.getTotalScore();
    }

    public String getLastCategory() {
        return lastCategory;
    }

    public int getLastScore() {
        return lastScore;
    }
}