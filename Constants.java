package com.example.ccjava;

public class Constants {
    // Category names in order they appear on scorecard
    public static final String[] CATEGORIES = {
            "Aces",             // Sum of all 1s
            "Twos",             // Sum of all 2s
            "Threes",           // Sum of all 3s
            "Fours",            // Sum of all 4s
            "Fives",            // Sum of all 5s
            "Sixes",            // Sum of all 6s
            "Three of a Kind",  // Sum of all dice if 3+ are same number
            "Four of a Kind",   // Sum of all dice if 4+ are same number
            "Full House",       // 25 points if 3 of one number and 2 of another
            "Small Straight",   // 30 points for sequence of 4
            "Large Straight",   // 40 points for sequence of 5
            "Yahtzee"          // 50 points for all 5 dice showing same number
    };

    // Category fixed scores
    public static final int FULL_HOUSE_SCORE = 25;
    public static final int SMALL_STRAIGHT_SCORE = 30;
    public static final int LARGE_STRAIGHT_SCORE = 40;
    public static final int YAHTZEE_SCORE = 50;

    // Game rules
    public static final int DICE_COUNT = 5;
    public static final int MAX_ROLLS = 3;
    public static final int DIE_MIN_VALUE = 1;
    public static final int DIE_MAX_VALUE = 6;

    // Optimal scores for each category (used for AI and help system)
    public static final int[] OPTIMAL_SCORES = {
            5,   // Aces (all ones)
            10,  // Twos (all twos)
            15,  // Threes (all threes)
            20,  // Fours (all fours)
            25,  // Fives (all fives)
            30,  // Sixes (all sixes)
            30,  // Three of a Kind (all sixes)
            30,  // Four of a Kind (all sixes)
            25,  // Full House (fixed score)
            30,  // Small Straight (fixed score)
            40,  // Large Straight (fixed score)
            50   // Yahtzee (fixed score)
    };

    // Category descriptions for help system
    public static final String[] CATEGORY_DESCRIPTIONS = {
            "Sum of all dice showing 1",
            "Sum of all dice showing 2",
            "Sum of all dice showing 3",
            "Sum of all dice showing 4",
            "Sum of all dice showing 5",
            "Sum of all dice showing 6",
            "Sum of all dice if at least three are the same number",
            "Sum of all dice if at least four are the same number",
            "25 points if three of one number and two of another",
            "30 points for a sequence of four numbers",
            "40 points for a sequence of five numbers",
            "50 points if all five dice show the same number"
    };

    // Helper methods to get category info
    public static String getCategoryDescription(String category) {
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (CATEGORIES[i].equals(category)) {
                return CATEGORY_DESCRIPTIONS[i];
            }
        }
        return "";
    }

    public static int getOptimalScore(String category) {
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (CATEGORIES[i].equals(category)) {
                return OPTIMAL_SCORES[i];
            }
        }
        return 0;
    }

    public static int getCategoryIndex(String category) {
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (CATEGORIES[i].equals(category)) {
                return i;
            }
        }
        return -1;
    }

    // UI-related constants
    public static final int DICE_IMAGE_SIZE = 60;  // dp
    public static final int DICE_MARGIN = 5;       // dp
    public static final String DICE_RESOURCE_PREFIX = "dice_";
    public static final int COMPUTER_TURN_DELAY = 1000;  // milliseconds
    public static final int ANIMATION_DURATION = 500;    // milliseconds

    // Save/Load constants
    public static final String SAVE_FILE_NAME = "yahtzee_save.dat";
    public static final String SAVE_FILE_EXTENSION = ".dat";
}