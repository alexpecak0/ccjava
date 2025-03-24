package com.example.ccjava;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Helper {

    public static String getAdvice(int[] diceValues, boolean[] selectedDice, int rollCount, List<String> availableCategories) {
        StringBuilder advice = new StringBuilder();

        // Calculate potential scores for all available categories
        Map<String, Integer> potentialScores = new HashMap<>();
        for (String category : availableCategories) {
            int score = calculateScore(category, diceValues);
            potentialScores.put(category, score);
        }

        // Find best potential category
        String bestCategory = null;
        int bestScore = 0;
        for (Map.Entry<String, Integer> entry : potentialScores.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestScore = entry.getValue();
                bestCategory = entry.getKey();
            }
        }

        // Build comprehensive advice as required by rubric
        advice.append("Current Dice: ");
        for (int value : diceValues) {
            advice.append(value).append(" ");
        }
        advice.append("\n\n");

        // Show all available categories and potential scores
        advice.append("Available Categories:\n");
        for (Map.Entry<String, Integer> entry : potentialScores.entrySet()) {
            advice.append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue())
                    .append(" points possible\n");
        }
        advice.append("\n");

        // Recommend best strategy
        if (bestCategory != null) {
            advice.append("Recommendation:\n");
            advice.append("Pursue ").append(bestCategory)
                    .append(" (Potential: ").append(bestScore).append(" points)\n");

            // Recommend which dice to keep/reroll
            boolean[] recommendedHolds = recommendDiceToKeep(diceValues, bestCategory);
            advice.append("Suggested dice to keep: ");
            for (int i = 0; i < diceValues.length; i++) {
                if (recommendedHolds[i]) {
                    advice.append(diceValues[i]).append(" ");
                }
            }
            advice.append("\n");

            // Roll again or stand recommendation
            if (rollCount < 3 && bestScore < getOptimalScore(bestCategory)) {
                advice.append("Consider rolling again to improve your score.");
            } else {
                advice.append("Consider standing with your current dice.");
            }
        }

        return advice.toString();
    }

    public static int calculateScore(String category, int[] dice) {
        switch (category) {
            case "Aces": return sumOfNumber(dice, 1);
            case "Twos": return sumOfNumber(dice, 2);
            case "Threes": return sumOfNumber(dice, 3);
            case "Fours": return sumOfNumber(dice, 4);
            case "Fives": return sumOfNumber(dice, 5);
            case "Sixes": return sumOfNumber(dice, 6);
            case "Three of a Kind": return hasCount(dice, 3) ? sum(dice) : 0;
            case "Four of a Kind": return hasCount(dice, 4) ? sum(dice) : 0;
            case "Full House": return isFullHouse(dice) ? 25 : 0;
            case "Small Straight": return isSmallStraight(dice) ? 30 : 0;
            case "Large Straight": return isLargeStraight(dice) ? 40 : 0;
            case "Yahtzee": return isYahtzee(dice) ? 50 : 0;
            default: return 0;
        }
    }

    private static boolean[] recommendDiceToKeep(int[] dice, String targetCategory) {
        boolean[] keep = new boolean[5];

        switch (targetCategory) {
            case "Yahtzee":
                int mostCommon = findMostCommonValue(dice);
                for (int i = 0; i < dice.length; i++) {
                    keep[i] = (dice[i] == mostCommon);
                }
                break;

            case "Large Straight":
                // Keep sequential dice
                for (int i = 0; i < dice.length; i++) {
                    keep[i] = isPartOfLongestSequence(dice, dice[i]);
                }
                break;

            case "Small Straight":
                // Similar to large straight but need only 4 sequential
                for (int i = 0; i < dice.length; i++) {
                    keep[i] = isPartOfSequence(dice, dice[i], 4);
                }
                break;

            case "Full House":
                int[] counts = getCounts(dice);
                int threeValue = -1;
                int pairValue = -1;

                // Find three of a kind and pair values
                for (int i = 1; i <= 6; i++) {
                    if (counts[i] >= 3) threeValue = i;
                    else if (counts[i] >= 2) pairValue = i;
                }

                // Keep matching dice
                for (int i = 0; i < dice.length; i++) {
                    keep[i] = (dice[i] == threeValue || dice[i] == pairValue);
                }
                break;

            case "Three of a Kind":
            case "Four of a Kind":
                int targetCount = targetCategory.equals("Three of a Kind") ? 3 : 4;
                int value = findMostCommonValue(dice);
                for (int i = 0; i < dice.length; i++) {
                    keep[i] = (dice[i] == value);
                }
                break;

            default:
                // For number categories (Aces through Sixes)
                int targetNumber = getCategoryNumber(targetCategory);
                if (targetNumber > 0) {
                    for (int i = 0; i < dice.length; i++) {
                        keep[i] = (dice[i] == targetNumber);
                    }
                }
                break;
        }

        return keep;
    }

    private static int getOptimalScore(String category) {
        switch (category) {
            case "Aces": return 5;  // All ones
            case "Twos": return 10; // All twos
            case "Threes": return 15;
            case "Fours": return 20;
            case "Fives": return 25;
            case "Sixes": return 30;
            case "Three of a Kind": return 30; // All sixes
            case "Four of a Kind": return 30;
            case "Full House": return 25;
            case "Small Straight": return 30;
            case "Large Straight": return 40;
            case "Yahtzee": return 50;
            default: return 0;
        }
    }

    // Scoring helper methods
    private static int sumOfNumber(int[] dice, int number) {
        int sum = 0;
        for (int value : dice) {
            if (value == number) sum += value;
        }
        return sum;
    }

    private static int sum(int[] dice) {
        int sum = 0;
        for (int value : dice) {
            sum += value;
        }
        return sum;
    }

    private static boolean hasCount(int[] dice, int count) {
        int[] counts = getCounts(dice);
        for (int i = 1; i <= 6; i++) {  // Start from 1 since dice values are 1-6
            if (counts[i] >= count) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFullHouse(int[] dice) {
        int[] counts = getCounts(dice);
        boolean hasThree = false;
        boolean hasTwo = false;
        for (int count : counts) {
            if (count == 3) hasThree = true;
            if (count == 2) hasTwo = true;
        }
        return hasThree && hasTwo;
    }

    private static boolean isSmallStraight(int[] dice) {
        return hasSequenceOfLength(dice, 4);
    }

    private static boolean isLargeStraight(int[] dice) {
        return hasSequenceOfLength(dice, 5);
    }

    private static boolean isYahtzee(int[] dice) {
        int first = dice[0];
        for (int value : dice) {
            if (value != first) return false;
        }
        return true;
    }

    private static int[] getCounts(int[] dice) {
        int[] counts = new int[7];  // 0-6, ignore 0
        for (int value : dice) {
            counts[value]++;
        }
        return counts;
    }

    private static boolean hasSequenceOfLength(int[] dice, int length) {
        boolean[] present = new boolean[7];
        for (int value : dice) present[value] = true;

        int consecutive = 0;
        for (int i = 1; i <= 6; i++) {
            if (present[i]) {
                consecutive++;
                if (consecutive >= length) return true;
            } else {
                consecutive = 0;
            }
        }
        return false;
    }

    private static int findMostCommonValue(int[] dice) {
        int[] counts = getCounts(dice);
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

    private static boolean isPartOfLongestSequence(int[] dice, int value) {
        boolean[] present = new boolean[7];
        for (int v : dice) present[v] = true;

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

    private static boolean isPartOfSequence(int[] dice, int value, int targetLength) {
        return isPartOfLongestSequence(dice, value);  // Simplified for now
    }

    private static int getCategoryNumber(String category) {
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
}