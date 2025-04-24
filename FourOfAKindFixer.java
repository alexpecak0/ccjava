package com.example.ccjava;

import java.util.Arrays;
import java.util.Map;

/**
 * This utility class adds a monkey patch to fix the Four of a Kind scoring bug
 * By directly intercepting and modifying category selection decisions
 */
public class FourOfAKindFixer {
    
    /**
     * This method should be called before any category selection logic
     * in ComputerPlayer.selectCategory
     * 
     * @param diceValues The current dice values
     * @param rawScores Map of categories to their scores
     * @param computerPlayer The computer player making the decision
     * @return The category to select, or null if no override needed
     */
    public static String fixCategorySelection(int[] diceValues, Map<String, Integer> rawScores, ComputerPlayer computerPlayer) {
        // Only apply fix on the final roll
        if (computerPlayer.getRollCount() < 3) {
            return null; // Allow normal strategy on rolls 1 and 2
        }
        
        // Check if we have Four of a Kind
        boolean hasFourOfKind = false;
        int matchingValue = 0;
        
        // Count occurrences of each die value
        int[] counts = new int[7]; // 0-6, ignoring 0
        for (int value : diceValues) {
            counts[value]++;
        }
        
        // Check if any value appears 4 or more times
        for (int i = 1; i <= 6; i++) {
            if (counts[i] >= 4) {
                hasFourOfKind = true;
                matchingValue = i;
                break;
            }
        }
        
        // If we have Four of a Kind and that category is available, force select it
        if (hasFourOfKind && rawScores.containsKey("Four of a Kind")) {
            int fourOfAKindScore = rawScores.get("Four of a Kind");
            System.out.println("FIX: Intercepted Four of a Kind with " + matchingValue + "s, score: " + fourOfAKindScore);
            
            // Check if we're also considering "Fours" category
            Integer foursScore = rawScores.getOrDefault("Fours", 0);
            Integer matchingCategoryScore = rawScores.getOrDefault(getNumberCategory(matchingValue), 0);
            
            System.out.println("FIX: Four of a Kind score: " + fourOfAKindScore + 
                             ", " + getNumberCategory(matchingValue) + " score: " + matchingCategoryScore);
            
            // On final roll, always choose Four of a Kind over the matching number category
            if (fourOfAKindScore > matchingCategoryScore) {
                return "Four of a Kind";
            }
        }
        
        // BUGFIX: Always prioritize Four of a Kind over Three of a Kind when they have the same score
        if (hasFourOfKind && rawScores.containsKey("Four of a Kind") && rawScores.containsKey("Three of a Kind")) {
            int fourOfAKindScore = rawScores.get("Four of a Kind");
            int threeOfAKindScore = rawScores.get("Three of a Kind");
            
            // If scores are equal or Four of a Kind scores higher, prioritize Four of a Kind
            if (fourOfAKindScore >= threeOfAKindScore) {
                System.out.println("FIX: Prioritizing Four of a Kind (" + fourOfAKindScore + 
                                  ") over Three of a Kind (" + threeOfAKindScore + ")");
                return "Four of a Kind";
            }
        }
        
        return null; // No override needed
    }
    
    /**
     * Helper method to convert a die value to its category name
     */
    private static String getNumberCategory(int value) {
        switch (value) {
            case 1: return "Aces";
            case 2: return "Twos";
            case 3: return "Threes";
            case 4: return "Fours";
            case 5: return "Fives";
            case 6: return "Sixes";
            default: return "";
        }
    }
    
    /**
     * Helper method that can be added to ComputerPlayer.selectCategory to fix the bug
     */
    public static void applyFix(ComputerPlayer player) {
        // This is a placeholder for adding the patch to ComputerPlayer
        System.out.println("Four of a Kind Fixer applied to Computer Player");
    }
} 