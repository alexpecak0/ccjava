package com.example.ccjava;

import java.util.Arrays;
import java.util.Map;

public class ThreeOfAKindFixer {
    
    /**
     * Directly check and fix Three of a Kind scoring issues.
     * @param diceValues Current dice values
     * @param scoreMap Map of categories to scores
     * @param rollCount Current roll count (1-3)
     * @return The category to select, or null if no fix is needed
     */
    public static String fixCategorySelection(int[] diceValues, Map<String, Integer> scoreMap, int rollCount) {
        // Only apply fix on the final roll (roll 3)
        if (rollCount < 3) {
            System.out.println("ThreeOfAKindFixer: Not final roll, not applying fix");
            return null;
        }
        
        // Only apply fix if both Three of a Kind and a numeric category are available
        if (!scoreMap.containsKey("Three of a Kind")) {
            return null;
        }
        
        int threeOfAKindScore = scoreMap.get("Three of a Kind");
        
        // If Three of a Kind score is 0, don't apply fix
        if (threeOfAKindScore <= 0) {
            return null;
        }
        
        // Check if we have three of a kind
        int[] counts = new int[7]; // 0-6, ignore 0
        for (int value : diceValues) {
            counts[value]++;
        }
        
        boolean hasThreeOfAKind = false;
        int threeValue = 0;
        
        for (int i = 1; i <= 6; i++) {
            if (counts[i] >= 3) {
                hasThreeOfAKind = true;
                threeValue = i;
                break;
            }
        }
        
        if (!hasThreeOfAKind) {
            return null;
        }
        
        // CRITICAL BUGFIX: If we have Four of a Kind AND the Four of a Kind category is available, 
        // don't recommend Three of a Kind as Four of a Kind is a higher priority category
        boolean hasFourOfKind = false;
        for (int i = 1; i <= 6; i++) {
            if (counts[i] >= 4) {
                hasFourOfKind = true;
                break;
            }
        }
        
        if (hasFourOfKind && scoreMap.containsKey("Four of a Kind")) {
            int fourOfAKindScore = scoreMap.get("Four of a Kind");
            
            // If Four of a Kind score is greater than or equal to Three of a Kind, 
            // do not recommend Three of a Kind - let the Four of a Kind fixer handle it
            if (fourOfAKindScore >= threeOfAKindScore) {
                System.out.println("ThreeOfAKindFixer: Detected Four of a Kind. Four of a Kind score: " + 
                                 fourOfAKindScore + ", Three of a Kind score: " + threeOfAKindScore);
                System.out.println("ThreeOfAKindFixer: NOT recommending Three of a Kind to allow Four of a Kind selection");
                return null;
            }
        }
        
        // Check if the numeric category for the three of a kind is available
        String numericCategory = getNumberCategory(threeValue);
        if (scoreMap.containsKey(numericCategory)) {
            int numericScore = scoreMap.get(numericCategory);
            
            System.out.println("ThreeOfAKindFixer: Found three " + threeValue + "s");
            System.out.println("ThreeOfAKindFixer: Three of a Kind score = " + threeOfAKindScore);
            System.out.println("ThreeOfAKindFixer: " + numericCategory + " score = " + numericScore);
            
            // If Three of a Kind scores higher, force it
            if (threeOfAKindScore > numericScore) {
                System.out.println("ThreeOfAKindFixer: FORCING Three of a Kind (" + threeOfAKindScore + 
                                  ") over " + numericCategory + " (" + numericScore + ")");
                return "Three of a Kind";
            }
        }
        
        // Also check all other numeric categories
        for (String category : Arrays.asList("Aces", "Twos", "Threes", "Fours", "Fives", "Sixes")) {
            if (scoreMap.containsKey(category)) {
                int numericScore = scoreMap.get(category);
                
                // If Three of a Kind scores higher than any numeric category
                if (threeOfAKindScore > numericScore) {
                    System.out.println("ThreeOfAKindFixer: Three of a Kind (" + threeOfAKindScore + 
                                      ") scores higher than " + category + " (" + numericScore + ")");
                                      
                    // Only force the selection if the score difference is significant
                    if (threeOfAKindScore > numericScore * 1.2) { // 20% higher threshold
                        System.out.println("ThreeOfAKindFixer: FORCING Three of a Kind over " + category);
                        return "Three of a Kind";
                    }
                }
            }
        }
        
        return null;
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
} 