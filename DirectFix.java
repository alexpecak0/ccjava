package com.example.ccjava;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DirectFix {
    public static String fixComputerChoice(int[] diceValues, List<String> availableCategories) {
        // First check if we have a Four of a Kind
        System.out.println("DirectFix checking dice: " + Arrays.toString(diceValues));
        
        // Count the occurrences of each number
        int[] counts = new int[7]; // 0-6, ignoring 0
        for (int value : diceValues) {
            counts[value]++;
        }
        
        // Check if any value appears 4 or more times
        int fourKindValue = -1;
        for (int i = 1; i <= 6; i++) {
            if (counts[i] >= 4) {
                fourKindValue = i;
                System.out.println("DirectFix: Found Four of a Kind with " + i + "s");
                break;
            }
        }
        
        // If we have Four of a Kind and both categories are available, choose the better one
        if (fourKindValue != -1) {
            String numCategory = "";
            switch (fourKindValue) {
                case 1: numCategory = "Aces"; break;
                case 2: numCategory = "Twos"; break;
                case 3: numCategory = "Threes"; break;
                case 4: numCategory = "Fours"; break;
                case 5: numCategory = "Fives"; break;
                case 6: numCategory = "Sixes"; break;
            }
            
            boolean hasFourOfAKind = availableCategories.contains("Four of a Kind");
            boolean hasNumCategory = availableCategories.contains(numCategory);
            
            System.out.println("DirectFix: Four of a Kind available? " + hasFourOfAKind);
            System.out.println("DirectFix: " + numCategory + " available? " + hasNumCategory);
            
            if (hasFourOfAKind && hasNumCategory) {
                int fourOfAKindScore = 0;
                for (int value : diceValues) {
                    fourOfAKindScore += value;
                }
                
                int numCategoryScore = fourKindValue * counts[fourKindValue];
                
                System.out.println("DirectFix: Four of a Kind score = " + fourOfAKindScore);
                System.out.println("DirectFix: " + numCategory + " score = " + numCategoryScore);
                
                if (fourOfAKindScore > numCategoryScore) {
                    System.out.println("DirectFix: FORCING Four of a Kind");
                    return "Four of a Kind";
                }
            } else if (hasFourOfAKind) {
                System.out.println("DirectFix: Only Four of a Kind available, selecting it");
                return "Four of a Kind";
            }
        }
        
        // If we didn't select Four of a Kind, return null to let normal logic continue
        return null;
    }
} 