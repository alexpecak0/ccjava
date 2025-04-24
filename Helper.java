package com.example.ccjava;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Helper {

    public static String getAdvice(int[] diceValues, boolean[] selectedDice, int rollCount, 
                                  List<String> availableCategories, Map<String, Integer> scores) {
        StringBuilder advice = new StringBuilder();
        
        // Calculate potential scores
        Map<String, Integer> potentialScores = new HashMap<>();
        for (String category : availableCategories) {
            int score = calculateScore(category, diceValues);
            potentialScores.put(category, score);
        }

        advice.append("AVAILABLE CATEGORIES:\n");
        for (String category : availableCategories) {
            int score = potentialScores.get(category);
            advice.append("- ").append(category).append(": ")
                  .append("Current Score: ").append(score);
            if (rollCount < 3) {
                int maxPossible = getOptimalScore(category);
                advice.append(", Potential: ").append(score).append("-").append(maxPossible);
            }
            advice.append("\n");
        }
        advice.append("\n");

        // Current roll info
        advice.append("Roll #").append(rollCount).append(" of 3\n");
        advice.append("Current Dice: ");
        for (int i = 0; i < diceValues.length; i++) {
            if (selectedDice[i]) {
                advice.append("[").append(diceValues[i]).append("] ");
            } else {
                advice.append(diceValues[i]).append(" ");
            }
        }
        advice.append("\n\n");

        // Strategy recommendation
        advice.append("RECOMMENDED STRATEGIES:\n");
        List<String> strategies = determineStrategyOptions(diceValues, availableCategories, rollCount);
        for (String strategy : strategies) {
            advice.append("- ").append(strategy).append("\n");
        }
        advice.append("\n");

        // Final recommendation
        advice.append("RECOMMENDATION: ");
        if (rollCount == 3) {
            // Check for Four of a Kind first
            boolean hasFourOfKind = false;
            int fourValue = 0;
            int[] counts = getCounts(diceValues);
            
            for (int i = 1; i <= 6; i++) {
                if (counts[i] >= 4) {
                    hasFourOfKind = true;
                    fourValue = i;
                    break;
                }
            }
            
            // Direct override for the Four of a Kind bug
            if (hasFourOfKind && availableCategories.contains("Four of a Kind") && 
                availableCategories.contains(getNumberCategory(fourValue))) {
                
                int fourOfAKindScore = sum(diceValues);
                int numberCategoryScore = fourValue * counts[fourValue];
                
                // Force Four of a Kind if it scores higher
                if (fourOfAKindScore > numberCategoryScore) {
                    advice.append("Score Four of a Kind for ").append(fourOfAKindScore)
                          .append(" points\n");
                    advice.append("WHY: You have four ").append(fourValue)
                          .append("s which gives you ").append(fourOfAKindScore)
                          .append(" points, higher than scoring in ").append(getNumberCategory(fourValue))
                          .append(" (").append(numberCategoryScore).append(" points).");
                    return advice.toString();
                }
            }
            
            // Original logic - recommend best scoring option
            String bestCategory = findBestCategory(potentialScores);
            if (bestCategory != null) {
                advice.append("Score ").append(bestCategory)
                      .append(" for ").append(potentialScores.get(bestCategory))
                      .append(" points\n");
                advice.append("WHY: This gives you the highest possible score with your current dice.");
            }
        } else {
            // For rolls 1 and 2, give clear dice-keeping advice
            String bestStrategy = determineBestStrategy(diceValues, availableCategories, rollCount);
            advice.append("Pursue ").append(bestStrategy).append("\n");
            
            // Initialize diceToKeep array
            boolean[] diceToKeep = recommendDiceToKeep(diceValues, bestStrategy);
            
            advice.append("KEEP THESE DICE: ");
            boolean anyKept = false;
            for (int i = 0; i < diceValues.length; i++) {
                if (diceToKeep[i]) {
                    advice.append(diceValues[i]).append(" ");
                    anyKept = true;
                }
            }
            if (!anyKept) {
                advice.append("none");
            }
            advice.append("\n");
            
            advice.append("REROLL THESE DICE: ");
            boolean anyReroll = false;
            for (int i = 0; i < diceValues.length; i++) {
                if (!diceToKeep[i]) {
                    advice.append(diceValues[i]).append(" ");
                    anyReroll = true;
                }
            }
            if (!anyReroll) {
                advice.append("none");
            }
            advice.append("\n");
            
            // Simple explanation
            advice.append("\nWHY: ").append(explainStrategy(bestStrategy, diceValues, diceToKeep, rollCount));
        }

        return advice.toString();
    }

    private static List<String> determineStrategyOptions(int[] dice, List<String> availableCategories, int rollCount) {
        List<String> strategies = new ArrayList<>();

        // IMPROVED STRATEGY: Check for potential Large Straight with 1,3,4,5 pattern
        boolean has1 = false, has3 = false, has4 = false, has5 = false;
        int oneCount = 0;
        
        for (int d : dice) {
            if (d == 1) {
                has1 = true;
                oneCount++;
            }
            if (d == 3) has3 = true;
            if (d == 4) has4 = true;
            if (d == 5) has5 = true;
        }
        
        boolean has1345Pattern = has1 && has3 && has4 && has5;
        
        // Add Large Straight strategy at the top of the list if we have 1,3,4,5 pattern
        if (has1345Pattern && availableCategories.contains("Large Straight")) {
            strategies.add("Complete Large Straight (40 points) - need a 2");
        }

        // Check for Yahtzee potential
        if (availableCategories.contains("Yahtzee")) {
            int[] counts = getCounts(dice);
            for (int i = 1; i <= 6; i++) {
                if (counts[i] >= 3) {
                    strategies.add("Build Yahtzee with " + i + "s (50 points)");
                }
            }
        }

        // Check for Large Straight potential
        if (availableCategories.contains("Large Straight") && hasSequenceOfLength(dice, 4)) {
            strategies.add("Complete Large Straight (40 points)");
        }

        // Check for Small Straight potential
        if (availableCategories.contains("Small Straight") && hasSequenceOfLength(dice, 3)) {
            strategies.add("Complete Small Straight (30 points)");
        }

        // Check for Full House potential
        if (availableCategories.contains("Full House")) {
            int[] counts = getCounts(dice);
            boolean hasThree = false;
            boolean hasTwo = false;
            for (int count : counts) {
                if (count >= 3) hasThree = true;
                else if (count >= 2) hasTwo = true;
            }
            if (hasThree || hasTwo) {
                strategies.add("Complete Full House (25 points)");
            }
        }

        // Check for high-scoring number categories
        for (String category : Arrays.asList("Sixes", "Fives", "Fours")) {
            if (availableCategories.contains(category)) {
                int value = getCategoryNumber(category);
                int count = getCounts(dice)[value];
                if (count >= 2) {
                    strategies.add("Collect " + category + " (" + (count * value) + " points)");
                }
            }
        }

        // Check for low-scoring number categories - only suggest if have many or few categories left
        for (String category : Arrays.asList("Threes", "Twos", "Aces")) {
            if (availableCategories.contains(category)) {
                int value = getCategoryNumber(category);
                int count = getCounts(dice)[value];
                
                // Only recommend low-value categories if:
                // 1. We have a lot of those numbers (≥ 3 for 1s/2s, ≥ 4 for 3s)
                // 2. We're on the final roll and need to choose something
                // 3. Few categories remain (suggesting we're later in the game)
                boolean fewCategoriesLeft = availableCategories.size() <= 5;
                boolean finalRoll = rollCount == 3;
                boolean hasMany = (value <= 2) ? count >= 3 : count >= 2;
                
                if (hasMany || finalRoll || fewCategoriesLeft) {
                    strategies.add("Collect " + category + " (" + (count * value) + " points)");
                }
            }
        }

        // Default to highest frequency
        if (strategies.isEmpty()) {
            int[] counts = getCounts(dice);
            int maxCount = 0;
            int bestValue = 6; // Prefer higher numbers if equal frequency
            for (int i = 6; i >= 1; i--) {
                if (counts[i] > maxCount) {
                    maxCount = counts[i];
                    bestValue = i;
                }
            }
            strategies.add("Collect " + bestValue + "s (" + (maxCount * bestValue) + " points)");
        }

        return strategies;
    }

    private static double calculateExpectedValue(String category, int[] dice, int rollCount) {
        int currentScore = calculateScore(category, dice);
        if (rollCount == 3) return currentScore;

        int remainingRolls = 3 - rollCount;
        int[] counts = getCounts(dice);
        
        switch (category) {
            case "Yahtzee":
                // Find the most common value
                int maxCount = 0;
                int maxValue = 0;
                for (int i = 1; i <= 6; i++) {
                    if (counts[i] > maxCount) {
                        maxCount = counts[i];
                        maxValue = i;
                    }
                }
                
                if (maxCount == 5) return 50; // Already have Yahtzee
                if (maxCount == 0) return 0.01; // No dice yet, very unlikely
                
                // Calculate probability based on remaining dice needed and rolls
                int neededDice = 5 - maxCount;
                
                // For each roll, we need to get the right value on all remaining dice
                // Probability of getting the right value on one die is 1/6
                // Formula: (1/6)^neededDice for a single roll
                // For multiple rolls, we use a simplified approach
                double singleRollProb = Math.pow(1.0/6.0, neededDice);
                double probability = 0;
                
                // For 1 remaining roll
                if (remainingRolls == 1) {
                    probability = singleRollProb;
                } 
                // For 2 remaining rolls, we have multiple chances - simplified approximation
                else {
                    // We're not doing a full probability calculation here, but a reasonable approximation
                    if (neededDice == 1) probability = 0.30; // ~30% for 1 die in 2 rolls
                    else if (neededDice == 2) probability = 0.08; // ~8% for 2 dice in 2 rolls
                    else probability = 0.01; // Very low for 3+ dice in 2 rolls
                }
                
                return probability * 50; // Yahtzee score

            case "Large Straight":
                // Check how close we are to a Large Straight
                if (isLargeStraight(dice)) return 40; // Already have it
                
                // Count how many unique values we have in the 1-5 and 2-6 ranges
                int uniqueIn1to5 = 0;
                int uniqueIn2to6 = 0;
                
                for (int i = 1; i <= 5; i++) {
                    if (counts[i] > 0) uniqueIn1to5++;
                }
                for (int i = 2; i <= 6; i++) {
                    if (counts[i] > 0) uniqueIn2to6++;
                }
                
                int bestUniqueCount = Math.max(uniqueIn1to5, uniqueIn2to6);
                
                // Calculate probability based on missing values and remaining rolls
                int missingValues = 5 - bestUniqueCount;
                if (missingValues == 0) return 40; // Should already be caught above, but just in case
                
                // Similar approach to Yahtzee but for straight combinations
                if (missingValues == 1) {
                    if (remainingRolls == 1) probability = 0.167; // 1/6 for the specific number
                    else probability = 0.30; // ~30% chance with 2 rolls
                } else if (missingValues == 2) {
                    if (remainingRolls == 1) probability = 0.028; // (1/6)^2
                    else probability = 0.08; // ~8% with 2 rolls
                } else {
                    probability = 0.01; // Very unlikely with 3+ missing
                }
                
                return probability * 40;

            case "Small Straight":
                if (isSmallStraight(dice)) return 30; // Already have it
                
                // Similar to Large Straight but easier to complete
                // Check for 3-value sequences (1-2-3, 2-3-4, 3-4-5, 4-5-6)
                boolean has123 = counts[1] > 0 && counts[2] > 0 && counts[3] > 0;
                boolean has234 = counts[2] > 0 && counts[3] > 0 && counts[4] > 0;
                boolean has345 = counts[3] > 0 && counts[4] > 0 && counts[5] > 0;
                boolean has456 = counts[4] > 0 && counts[5] > 0 && counts[6] > 0;
                
                // If we have a 3-value sequence, we only need 1 more specific value
                if (has123 || has234 || has345 || has456) {
                    if (remainingRolls == 1) probability = 0.167; // 1/6 for the specific number
                    else probability = 0.30; // ~30% with 2 rolls
                } 
                // If we have a 2-value sequence in the right position, we need 2 specific values
                else if ((counts[1] > 0 && counts[2] > 0) || 
                         (counts[2] > 0 && counts[3] > 0) || 
                         (counts[3] > 0 && counts[4] > 0) || 
                         (counts[4] > 0 && counts[5] > 0) || 
                         (counts[5] > 0 && counts[6] > 0)) {
                    if (remainingRolls == 1) probability = 0.028; // (1/6)^2
                    else probability = 0.08; // ~8% with 2 rolls
                } else {
                    probability = 0.01; // Very unlikely to complete from scratch
                }
                
                return probability * 30;

            case "Full House":
                if (isFullHouse(dice)) return 25; // Already have it
                
                // Check if we have a Three of a Kind
                boolean hasThreeOfKind = false;
                int threeOfKindValue = 0;
                
                // Check if we have a Pair
                boolean hasPair = false;
                int pairValue = 0;
                
                for (int i = 1; i <= 6; i++) {
                    if (counts[i] >= 3) {
                        hasThreeOfKind = true;
                        threeOfKindValue = i;
                    } else if (counts[i] >= 2) {
                        hasPair = true;
                        pairValue = i;
                    }
                }
                
                // Calculate probability based on what we have and what we need
                if (hasThreeOfKind && hasPair) {
                    return 25; // Already have Full House
                } else if (hasThreeOfKind) {
                    // Need a pair of any other value
                    if (remainingRolls == 1) probability = 0.15; // ~15% chance to get a pair in 1 roll
                    else probability = 0.40; // ~40% with 2 rolls
                } else if (hasPair) {
                    // Need another pair or to convert the pair to three of a kind
                    if (remainingRolls == 1) probability = 0.10; // ~10% chance
                    else probability = 0.25; // ~25% with 2 rolls
                } else {
                    // Starting from scratch - need both a three of a kind and a pair
                    probability = 0.05; // Very low probability
                }
                
                return probability * 25;

            case "Four of a Kind":
                if (hasCount(dice, 4)) return sum(dice); // Already have it
                
                // Find the highest count and that value
                maxCount = 0;
                maxValue = 0;
                for (int i = 1; i <= 6; i++) {
                    if (counts[i] > maxCount) {
                        maxCount = counts[i];
                        maxValue = i;
                    }
                }
                
                int expectedScore = 0;
                
                // Calculate probability based on current count
                if (maxCount == 3) {
                    // Need just one more die to match
                    if (remainingRolls == 1) probability = 0.167; // 1/6 for a specific value
                    else probability = 0.30; // ~30% with 2 rolls
                    expectedScore = maxValue * 4 + (5 - 4) * 3; // Approximation of total score
                } else if (maxCount == 2) {
                    // Need 2 more dice to match
                    if (remainingRolls == 1) probability = 0.028; // (1/6)^2
                    else probability = 0.08; // ~8% with 2 rolls
                    expectedScore = maxValue * 4 + (5 - 4) * 3; // Approximation of total score
                } else {
                    // Very unlikely to get Four of a Kind from scratch
                    probability = 0.01;
                    expectedScore = 6 * 4 + 2; // Approximation using sixes (highest value)
                }
                
                return probability * expectedScore;

            case "Three of a Kind":
                if (hasCount(dice, 3)) return sum(dice); // Already have it
                
                // Similar logic to Four of a Kind
                maxCount = 0;
                maxValue = 0;
                for (int i = 1; i <= 6; i++) {
                    if (counts[i] > maxCount) {
                        maxCount = counts[i];
                        maxValue = i;
                    }
                }
                
                expectedScore = 0;
                
                if (maxCount == 2) {
                    // Need just one more die to match
                    if (remainingRolls == 1) probability = 0.167; // 1/6 for a specific value
                    else probability = 0.30; // ~30% with 2 rolls
                    expectedScore = maxValue * 3 + (5 - 3) * 3; // Approximation of total score
                } else if (maxCount == 1) {
                    // Need 2 more dice to match
                    if (remainingRolls == 1) probability = 0.028; // (1/6)^2
                    else probability = 0.08; // ~8% with 2 rolls
                    expectedScore = maxValue * 3 + (5 - 3) * 3; // Approximation of total score
                } else {
                    // Starting from scratch
                    probability = 0.05;
                    expectedScore = 6 * 3 + 4; // Approximation using sixes (highest value)
                }
                
                return probability * expectedScore;

            default: 
                // Upper section categories (Aces through Sixes)
                int targetNumber = getCategoryNumber(category);
                if (targetNumber > 0) {
                    int currentCount = counts[targetNumber];
                    double expectedAdditional = 0;
                    
                    // Calculate expected additional dice of this value
                    // Probability of rolling the target number is 1/6 per die
                    double probPerDie = 1.0/6.0;
                    
                    // On each roll, we have (5 - currentCount) dice that could potentially be the target number
                    // Each has a 1/6 chance
                    int remainingDice = 5 - currentCount;
                    
                    if (remainingRolls == 1) {
                        expectedAdditional = remainingDice * probPerDie;
                    } else {
                        // For 2 rolls, we use a simplified model
                        // First roll expected value
                        double firstRollExpected = remainingDice * probPerDie;
                        // After first roll, we'd have approximately (remainingDice - firstRollExpected) dice left
                        double secondRollRemaining = remainingDice - firstRollExpected;
                        double secondRollExpected = secondRollRemaining * probPerDie;
                        expectedAdditional = firstRollExpected + secondRollExpected;
                    }
                    
                    // Calculate expected score
                    return (currentCount + expectedAdditional) * targetNumber;
                }
        }
        
        return currentScore;
    }

    private static int getLongestSequence(int[] dice) {
        // First mark which values are present
        boolean[] present = new boolean[7];
        for (int value : dice) present[value] = true;
        
        // Check for specific sequences (1-5 and 2-6 for large straights)
        if ((present[1] && present[2] && present[3] && present[4] && present[5]) ||
            (present[2] && present[3] && present[4] && present[5] && present[6])) {
            return 5;
        }
        
        // Check for small straight patterns
        if ((present[1] && present[2] && present[3] && present[4]) ||
            (present[2] && present[3] && present[4] && present[5]) ||
            (present[3] && present[4] && present[5] && present[6])) {
            return 4;
        }
        
        // Check for 3-value sequences
        if ((present[1] && present[2] && present[3]) ||
            (present[2] && present[3] && present[4]) ||
            (present[3] && present[4] && present[5]) ||
            (present[4] && present[5] && present[6])) {
            return 3;
        }
        
        // Check for 2-value sequences
        if ((present[1] && present[2]) ||
            (present[2] && present[3]) ||
            (present[3] && present[4]) ||
            (present[4] && present[5]) ||
            (present[5] && present[6])) {
            return 2;
        }
        
        // If we don't find any sequences, return the length of the longest consecutive run
        int maxSequence = 0;
        int currentSequence = 0;
        for (int i = 1; i <= 6; i++) {
            if (present[i]) {
                currentSequence++;
                maxSequence = Math.max(maxSequence, currentSequence);
            } else {
                currentSequence = 0;
            }
        }
        return maxSequence;
    }

    private static int calculateImprovementProbability(int currentCount, int rollCount) {
        int remainingRolls = 3 - rollCount;
        if (remainingRolls == 0) return 0;

        // Simplified probability calculation
        return (int)((5 - currentCount) * 20 * remainingRolls);
    }

    private static int calculateStraightCompletionProbability(int[] dice, int targetLength, int rollCount) {
        int remainingRolls = 3 - rollCount;
        if (remainingRolls == 0) return 0;

        // Simplified probability calculation
        int missingNumbers = targetLength - countConsecutive(dice);
        return (int)((6 - missingNumbers) * 15 * remainingRolls);
    }

    private static int countConsecutive(int[] dice) {
        boolean[] present = new boolean[7];
        for (int value : dice) present[value] = true;

        int maxConsecutive = 0;
        int currentConsecutive = 0;
        for (int i = 1; i <= 6; i++) {
            if (present[i]) {
                currentConsecutive++;
                maxConsecutive = Math.max(maxConsecutive, currentConsecutive);
            } else {
                currentConsecutive = 0;
            }
        }
        return maxConsecutive;
    }

    private static String assessRisk(int[] dice, String strategy, int rollCount) {
        int[] counts = getCounts(dice);
        int remainingRolls = 3 - rollCount;
        StringBuilder assessment = new StringBuilder();

        if (strategy.startsWith("Build Yahtzee")) {
            int value = Integer.parseInt(strategy.split(" ")[3].replace("s", ""));
            int currentCount = counts[value];
            int needed = 5 - currentCount;
            
            if (needed <= remainingRolls) {
                assessment.append("Medium-High risk: Need ").append(needed)
                         .append(" more ").append(value).append("s in ")
                         .append(remainingRolls).append(" rolls. ");
                assessment.append("Success chance: ").append(String.format("%.1f%%", 
                    remainingRolls * (1.0/6.0) * 100)).append(" per die. ");
                assessment.append("High reward (50 points) justifies the risk.");
            } else {
                assessment.append("Very High risk: Need ").append(needed)
                         .append(" more ").append(value).append("s in only ")
                         .append(remainingRolls).append(" rolls. ");
                assessment.append("Consider alternative strategies unless no better options exist.");
            }
        } 
        else if (strategy.contains("Large Straight")) {
            int sequenceLength = getLongestSequence(dice);
            int needed = 5 - sequenceLength;
            double probability = remainingRolls * (needed <= 1 ? 0.167 : 0.033);
            
            assessment.append(needed <= 1 ? "Medium risk: " : "High risk: ");
            assessment.append("Need ").append(needed).append(" more numbers for Large Straight. ");
            assessment.append("Success chance: ").append(String.format("%.1f%%", probability * 100)).append(". ");
            assessment.append("Worth pursuing for 40 points if no safer high-scoring options exist.");
        }
        else if (strategy.contains("Small Straight")) {
            int sequenceLength = getLongestSequence(dice);
            int needed = 4 - sequenceLength;
            double probability = remainingRolls * (needed <= 1 ? 0.33 : 0.167);
            
            assessment.append(needed <= 1 ? "Low-Medium risk: " : "Medium risk: ");
            assessment.append("Need ").append(needed).append(" more numbers for Small Straight. ");
            assessment.append("Success chance: ").append(String.format("%.1f%%", probability * 100)).append(". ");
            assessment.append("Good balance of risk vs. reward for 30 points.");
        }
        else if (strategy.contains("Full House")) {
            boolean hasThree = false;
            boolean hasTwo = false;
            int threeValue = -1;
            int pairValue = -1;
            
            for (int i = 1; i <= 6; i++) {
                if (counts[i] >= 3) {
                    hasThree = true;
                    threeValue = i;
                } else if (counts[i] >= 2) {
                    hasTwo = true;
                    pairValue = i;
                }
            }
            
            if (hasThree && hasTwo) {
                assessment.append("No risk: Already have Full House (25 points).");
            } else if (hasThree) {
                assessment.append("Medium risk: Have three ").append(threeValue)
                         .append("s, need a pair in ").append(remainingRolls)
                         .append(" rolls. Success chance: ")
                         .append(String.format("%.1f%%", remainingRolls * 0.33 * 100)).append(".");
            } else if (hasTwo) {
                assessment.append("Medium-High risk: Have pair of ").append(pairValue)
                         .append("s, need three of a kind in ").append(remainingRolls)
                         .append(" rolls. Success chance: ")
                         .append(String.format("%.1f%%", remainingRolls * 0.167 * 100)).append(".");
            } else {
                assessment.append("High risk: Need both three of a kind and pair. ");
                assessment.append("Consider alternative strategies.");
            }
        }
        else {
            // For number categories
            String[] parts = strategy.split(" ");
            if (parts.length >= 2) {
                String numberStr = parts[1].replace("s", "");
                try {
                    int targetNumber = Integer.parseInt(numberStr);
                    int currentCount = counts[targetNumber];
                    double expectedAdditional = remainingRolls * (5 - currentCount) * (1.0/6.0);
                    int expectedTotal = currentCount + (int)Math.round(expectedAdditional);
                    
                    assessment.append("Low risk: Currently have ").append(currentCount)
                             .append(" ").append(targetNumber).append("s. ");
                    assessment.append("Expect ~").append(expectedTotal).append(" total for ")
                             .append(expectedTotal * targetNumber).append(" points. ");
                    assessment.append("Safe choice that helps build upper section score.");
                } catch (NumberFormatException e) {
                    assessment.append("Low risk: Focus on collecting matching numbers for consistent scoring.");
                }
            }
        }
        
        return assessment.toString();
    }

    private static String determineBestStrategy(int[] dice, List<String> availableCategories, int rollCount) {
        // Check for 1,3,4,5 pattern and prioritize Large Straight
        boolean has1 = false, has3 = false, has4 = false, has5 = false;
        int oneCount = 0;
        
        for (int d : dice) {
            if (d == 1) {
                has1 = true;
                oneCount++;
            }
            if (d == 3) has3 = true;
            if (d == 4) has4 = true;
            if (d == 5) has5 = true;
        }
        
        boolean has1345 = has1 && has3 && has4 && has5;
        
        if (has1345 && availableCategories.contains("Large Straight")) {
            return "Complete Large Straight";
        }
        
        // First, check if we have 4 of a kind - this should take precedence over most other strategies
        int[] counts = getCounts(dice);
        for (int i = 1; i <= 6; i++) {
            if (counts[i] >= 4) {
                // Only recommend Yahtzee if the category is still available
                if (availableCategories.contains("Yahtzee")) {
                    System.out.println("STRATEGY: Found 4 " + i + "s - recommending Yahtzee attempt");
                    return "Build Yahtzee with " + i + "s";
                } else {
                    System.out.println("STRATEGY: Found 4 " + i + "s but Yahtzee category is filled - finding alternative");
                    // Continue to other strategies instead of immediately recommending Yahtzee
                }
            }
        }
        
        // Calculate expected values for all categories
        Map<String, Double> expectedValues = new HashMap<>();
        for (String category : availableCategories) {
            expectedValues.put(category, calculateExpectedValue(category, dice, rollCount));
        }
        
        // SPECIAL CASE: Check for 5 of a kind and prioritize number category if it's higher scoring
        boolean hasFiveOfAKind = false;
        int fiveOfAKindValue = 0;
        for (int i = 1; i <= 6; i++) {
            if (counts[i] == 5) {
                hasFiveOfAKind = true;
                fiveOfAKindValue = i;
                break;
            }
        }
        
        if (hasFiveOfAKind) {
            String numberCategory = getNumberCategory(fiveOfAKindValue);
            
            // If the number category is available, prioritize it over Full House if it scores higher
            if (availableCategories.contains(numberCategory)) {
                int numberScore = fiveOfAKindValue * 5; // 5 dice of same value
                
                // Only use Full House if it's available and the player explicitly wants it
                if (availableCategories.contains("Full House") && numberScore < 25) {
                    return "Complete Full House";
                } else {
                    return "Collect " + fiveOfAKindValue + "s";
                }
            }
        }
        
        // Check for guaranteed high scores first
        if (isYahtzee(dice) && availableCategories.contains("Yahtzee")) {
            return "Build Yahtzee";
        }
        if (isLargeStraight(dice) && availableCategories.contains("Large Straight")) {
            return "Complete Large Straight";
        }
        if (isSmallStraight(dice) && availableCategories.contains("Small Straight")) {
            return "Complete Small Straight";
        }
        if (isFullHouse(dice) && availableCategories.contains("Full House")) {
            return "Complete Full House";
        }
        
        // Check for near-complete high-value combinations
        
        // Near Yahtzee (3 of a kind)
        if (availableCategories.contains("Yahtzee")) {
            for (int i = 1; i <= 6; i++) {
                if (counts[i] >= 3) {
                    return "Build Yahtzee with " + i + "s";
                }
            }
        }
        
        // Check for Full House potential (at least one pair)
        if (availableCategories.contains("Full House")) {
            boolean hasThreeOfKind = false;
            boolean hasPair = false;
            int pairValue1 = 0;
            int pairValue2 = 0;
            
            for (int i = 1; i <= 6; i++) {
                if (counts[i] >= 3) {
                    hasThreeOfKind = true;
                } else if (counts[i] >= 2) {
                    if (pairValue1 == 0) {
                        pairValue1 = i;
                    } else {
                        pairValue2 = i;
                    }
                    hasPair = true;
                }
            }
            
            // If we have a three of a kind or two pairs
            if (hasThreeOfKind || (hasPair && pairValue2 > 0)) {
                return "Complete Full House";
            }
        }
        
        // Near Large Straight (4 sequential)
        if (availableCategories.contains("Large Straight") && hasSequenceOfLength(dice, 4)) {
            if (expectedValues.get("Large Straight") >= 20) { // Only if decent probability
                return "Complete Large Straight";
            }
        }
        
        // Near Small Straight (3 sequential)
        if (availableCategories.contains("Small Straight") && hasSequenceOfLength(dice, 3)) {
            if (expectedValues.get("Small Straight") >= 15) { // Only if decent probability
                return "Complete Small Straight";
            }
        }
        
        // Check upper section opportunities
        // Prioritize high-value categories first (Sixes, Fives, Fours)
        int bestUpperValue = 0;
        double bestUpperExpected = 0;
        
        // First check high-value categories
        for (String category : Arrays.asList("Sixes", "Fives", "Fours")) {
            if (availableCategories.contains(category)) {
                double expected = expectedValues.get(category);
                int value = getCategoryNumber(category);
                
                // Apply a bonus to high-value categories
                expected *= 1.2;
                
                if (expected > bestUpperExpected) {
                    bestUpperExpected = expected;
                    bestUpperValue = value;
                }
            }
        }
        
        // Then check low-value categories with a penalty unless we have many
        for (String category : Arrays.asList("Threes", "Twos", "Aces")) {
            if (availableCategories.contains(category)) {
                double expected = expectedValues.get(category);
                int value = getCategoryNumber(category);
                int count = counts[value];
                
                // Apply a penalty to low-value categories unless we have many of them
                // or few categories are left (implying late in the game)
                boolean fewCategoriesLeft = availableCategories.size() <= 5;
                if (!fewCategoriesLeft && count < 3) {
                    expected *= 0.8; // 20% penalty
                }
                
                if (expected > bestUpperExpected) {
                    bestUpperExpected = expected;
                    bestUpperValue = value;
                }
            }
        }
        
        // If we found a good upper section opportunity
        if (bestUpperValue > 0 && bestUpperExpected >= bestUpperValue * 2) { // At least 2 dice of that value expected
            return "Collect " + bestUpperValue + "s";
        }
        
        // If nothing else looks promising, go for the highest expected value
        String bestCategory = null;
        double bestExpected = 0;
        for (Map.Entry<String, Double> entry : expectedValues.entrySet()) {
            if (entry.getValue() > bestExpected) {
                bestExpected = entry.getValue();
                bestCategory = entry.getKey();
            }
        }
        
        if (bestCategory != null) {
            if (bestCategory.equals("Yahtzee")) return "Build Yahtzee";
            if (bestCategory.equals("Large Straight")) return "Complete Large Straight";
            if (bestCategory.equals("Small Straight")) return "Complete Small Straight";
            if (bestCategory.equals("Full House")) return "Complete Full House";
            int value = getCategoryNumber(bestCategory);
            if (value > 0) return "Collect " + value + "s";
        }
        
        // Default to collecting highest frequency dice
        int maxCount = 0;
        int bestValue = 6; // Prefer higher numbers if equal frequency
        for (int i = 6; i >= 1; i--) {
            if (counts[i] > maxCount) {
                maxCount = counts[i];
                bestValue = i;
            }
        }
        
        // Ensure we have at least one of this value
        if (maxCount > 0) {
            return "Collect " + bestValue + "s";
        } else {
            // Fallback to highest value
            return "Collect 6s"; // Default to highest value if no clear pattern
        }
    }

    private static String explainStrategy(String strategy, int[] dice, boolean[] keeps, int rollCount) {
        StringBuilder explanation = new StringBuilder();
        
        // Add special explanation for 4 of a kind
        int[] counts = getCounts(dice);
        for (int i = 1; i <= 6; i++) {
            if (counts[i] >= 4) {
                explanation.append("You have FOUR ").append(i).append("s! ")
                          .append("Even though ").append(i).append("s are ");
                
                if (i <= 3) {
                    explanation.append("low-value dice, ");
                } else {
                    explanation.append("medium-value dice, ");
                }
                
                // Check if strategy includes Yahtzee or if Yahtzee is in the name
                if (strategy.toLowerCase().contains("yahtzee")) {
                    explanation.append("having 4 of a kind is a great opportunity to go for a Yahtzee (50 points), ")
                              .append("which is the highest scoring combination in the game. ")
                              .append("With only ").append(3 - rollCount).append(" roll").append(3 - rollCount == 1 ? "" : "s")
                              .append(" remaining, you have a ").append(3 - rollCount == 1 ? "16.7%" : "30%")
                              .append(" chance to get that last die to match.");
                } else {
                    explanation.append("this is a good opportunity for a high-scoring combination. ")
                              .append("Since Yahtzee is already taken, consider using these for ")
                              .append("Four of a Kind or a high-scoring number category.");
                }
                          
                return explanation.toString();
            }
        }
        
        // Original logic for other strategies
        if (strategy.startsWith("Build Yahtzee")) {
            int value = Integer.parseInt(strategy.split(" ")[3].replace("s", ""));
            int count = 0;
            for (int die : dice) {
                if (die == value) count++;
            }
            
            if (count >= 3) {
                explanation.append("You have ").append(count).append(" ").append(value).append("s. ")
                          .append("This is a good start for a Yahtzee, which scores 50 points - the highest possible score. ")
                          .append("With ").append(3 - rollCount).append(" rolls remaining, ")
                          .append("you have a decent chance to complete it.");
            } else {
                explanation.append("You have ").append(count).append(" ").append(value).append("s. ")
                          .append("Yahtzee scores 50 points, which is the highest possible score. ")
                          .append("However, with only ").append(count).append(" matching dice, ")
                          .append("it's a risky strategy but has an enormous payoff if successful.");
            }
        } 
        else if (strategy.contains("Large Straight")) {
            explanation.append("You have a good sequence of numbers that could form a Large Straight. ")
                      .append("A Large Straight scores 40 points, which is one of the highest scoring categories. ")
                      .append("Keep the sequential numbers and try to complete the straight.");
        }
        else if (strategy.contains("Small Straight")) {
            explanation.append("You have a sequence that could form a Small Straight. ")
                      .append("A Small Straight scores 30 points. ")
                      .append("Keep the sequential numbers and try to complete the straight.");
        }
        else if (strategy.contains("Full House")) {
            explanation.append("You have a good start for a Full House. ")
                      .append("A Full House scores 25 points. ")
                      .append("Keep the matching dice and try to complete the pattern of three of a kind and a pair.");
        }
        else {
            // For number categories
            String[] parts = strategy.split(" ");
            if (parts.length >= 2) {
                String numberStr = parts[1].replace("s", "");
                try {
                    int targetNumber = Integer.parseInt(numberStr);
                    int count = 0;
                    for (int die : dice) {
                        if (die == targetNumber) count++;
                    }
                    
                    // Special explanations for low-value categories
                    if (targetNumber <= 3) {
                        if (count >= 3) {
                            explanation.append("You have ").append(count).append(" ").append(targetNumber).append("s, ")
                                    .append("which gives a reasonable score. Each ").append(targetNumber)
                                    .append(" is worth ").append(targetNumber).append(" points, for a total of ")
                                    .append(count * targetNumber).append(" points.");
                        } else {
                            explanation.append("You only have ").append(count).append(" ").append(targetNumber)
                                    .append("s, which would give a low score. Consider rerolling to try for a better combination, ")
                                    .append("especially since ").append(targetNumber)
                                    .append("s are low-value dice. Higher numbers or special combinations like Full House ")
                                    .append("would give more points.");
                        }
                    } else {
                        // Standard explanation for higher numbers
                        explanation.append("You have ").append(count).append(" ").append(targetNumber).append("s. ")
                                .append("Each ").append(targetNumber).append(" is worth ").append(targetNumber)
                                .append(" points. Keep them and try to get more.");
                    }
                } catch (NumberFormatException e) {
                    explanation.append("Keep the highest value dice to maximize your score potential.");
                }
            }
        }
        
        return explanation.toString();
    }

    private static int calculatePercentageOfMax(String category, int score) {
        int maxPossible = getOptimalScore(category);
        return maxPossible > 0 ? (score * 100) / maxPossible : 0;
    }

    private static String findBestCategory(Map<String, Integer> scores) {
        // IMPORTANT: First check for conflicts between categories that could cause confusion
        // Priority check for Four of a Kind vs Fours and other similar conflicts
        String[] upperCategories = {"Aces", "Twos", "Threes", "Fours", "Fives", "Sixes"};
        String[] conflictingCategories = {"Three of a Kind", "Four of a Kind"};
        
        // Check all potential conflicts between "Three/Four of a Kind" and number categories
        for (String conflictCategory : conflictingCategories) {
            if (scores.containsKey(conflictCategory)) {
                int conflictScore = scores.get(conflictCategory);
                
                for (String upperCategory : upperCategories) {
                    if (scores.containsKey(upperCategory)) {
                        int upperScore = scores.get(upperCategory);
                        
                        // If the conflict category scores higher, print debug info
                        if (conflictScore > upperScore) {
                            System.out.println("PRIORITY CHECK: " + conflictCategory + " (" + conflictScore + 
                                              ") scores higher than " + upperCategory + " (" + upperScore + ")");
                            
                            // Return the conflict category immediately if it scores significantly higher
                            if (conflictScore >= upperScore * 1.5) {
                                System.out.println("PRIORITY RULE: Choosing " + conflictCategory + 
                                                 " as it scores significantly higher");
                                return conflictCategory;
                            }
                        }
                    }
                }
            }
        }
        
        // Find categories with the same maximum score and prioritize by difficulty
        int maxScore = 0;
        for (int score : scores.values()) {
            if (score > maxScore) {
                maxScore = score;
            }
        }
        
        // If we have multiple categories with the max score, prioritize by difficulty
        List<String> categoriesWithMaxScore = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            if (entry.getValue() == maxScore) {
                categoriesWithMaxScore.add(entry.getKey());
            }
        }
        
        if (categoriesWithMaxScore.size() > 1) {
            System.out.println("Multiple categories with the same score " + maxScore + ": " + categoriesWithMaxScore);
            
            // Define difficulty order (hardest to easiest)
            List<String> difficultyOrder = Arrays.asList(
                "Yahtzee", "Large Straight", "Small Straight", "Full House", 
                "Four of a Kind", "Three of a Kind", 
                "Sixes", "Fives", "Fours", "Threes", "Twos", "Aces"
            );
            
            // Find the hardest category that has the max score
            String hardestCategory = null;
            int lowestDifficultyIndex = Integer.MAX_VALUE;
            
            for (String category : categoriesWithMaxScore) {
                int difficultyIndex = difficultyOrder.indexOf(category);
                if (difficultyIndex != -1 && difficultyIndex < lowestDifficultyIndex) {
                    lowestDifficultyIndex = difficultyIndex;
                    hardestCategory = category;
                }
            }
            
            if (hardestCategory != null) {
                System.out.println("STRATEGY: Prioritizing " + hardestCategory + 
                                  " as it's harder to score in future rounds");
                return hardestCategory;
            }
        }
        
        // Otherwise find the category with the highest score
        String bestCategory = null;
        int highestScore = 0;
        
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            if (entry.getValue() > highestScore) {
                highestScore = entry.getValue();
                bestCategory = entry.getKey();
            }
        }
        
        return bestCategory;
    }

    public static int calculateScore(String category, int[] dice) {
        System.out.println("Calculating score for " + category + " with dice: " + Arrays.toString(dice));
        
        switch (category) {
            case "Aces": return sumOfNumber(dice, 1);
            case "Twos": return sumOfNumber(dice, 2);
            case "Threes": return sumOfNumber(dice, 3);
            case "Fours": return sumOfNumber(dice, 4);
            case "Fives": return sumOfNumber(dice, 5);
            case "Sixes": return sumOfNumber(dice, 6);
            case "Three of a Kind": 
                if (hasCount(dice, 3)) return sum(dice);
                return 0;
            case "Four of a Kind": 
                if (hasCount(dice, 4)) return sum(dice);
                return 0;
            case "Full House": return isFullHouse(dice) ? 25 : 0;
            case "Small Straight": return isSmallStraight(dice) ? 30 : 0;
            case "Large Straight": return isLargeStraight(dice) ? 40 : 0;
            case "Yahtzee": return isYahtzee(dice) ? 50 : 0;
            default: return 0;
        }
    }

    private static boolean[] recommendDiceToKeep(int[] dice, String strategy) {
        boolean[] keep = new boolean[5];
        
        // SPECIAL CASE: If we have 4 of any kind, prioritize keeping them all for Yahtzee potential
        int[] counts = getCounts(dice);
        for (int i = 1; i <= 6; i++) {
            if (counts[i] >= 4) {
                System.out.println("DICE STRATEGY: Found 4 " + i + "s - keeping all for Yahtzee potential");
                // Keep all dice of this value
                for (int j = 0; j < dice.length; j++) {
                    keep[j] = (dice[j] == i);
                }
                return keep; // Return immediately to bypass other logic
            }
        }
        
        if (strategy.startsWith("Build Yahtzee")) {
            int value = Integer.parseInt(strategy.split(" ")[3].replace("s", ""));
            for (int i = 0; i < dice.length; i++) {
                keep[i] = (dice[i] == value);
            }
        } else if (strategy.contains("Straight")) {
            // Check if we're going for a Large or Small Straight
            boolean isLargeStraight = strategy.contains("Large");
            
            // Track which values we've already decided to keep (to handle duplicates)
            boolean[] valueKept = new boolean[7];
            
            // First identify which values are part of a potential straight
            boolean[] isPartOfStraight = new boolean[7];
            for (int i = 1; i <= 6; i++) {
                if (isLargeStraight) {
                    // Check if i could be part of a Large Straight (either 1-5 or 2-6)
                    isPartOfStraight[i] = ((i >= 1 && i <= 5) || (i >= 2 && i <= 6));
                } else {
                    // Check if i could be part of a Small Straight (1-4, 2-5, or 3-6)
                    isPartOfStraight[i] = ((i >= 1 && i <= 4) || (i >= 2 && i <= 5) || (i >= 3 && i <= 6));
                }
            }
            
            // IMPROVED LOGIC FOR SPECIAL PATTERNS
            // Check for 1,3,4,5 pattern for Large Straight
            boolean has1 = false, has3 = false, has4 = false, has5 = false;
            
            for (int d : dice) {
                if (d == 1) has1 = true;
                if (d == 3) has3 = true;
                if (d == 4) has4 = true;
                if (d == 5) has5 = true;
            }
            
            boolean has1345 = has1 && has3 && has4 && has5;
            
            // Check for 2,3,4,5 pattern
            boolean has2 = false;
            for (int d : dice) {
                if (d == 2) has2 = true;
            }
            
            boolean has2345 = has2 && has3 && has4 && has5;
            
            // Handle special patterns
            if (isLargeStraight && (has1345 || has2345)) {
                for (int i = 0; i < dice.length; i++) {
                    int val = dice[i];
                    
                    if (has1345 && (val == 3 || val == 4 || val == 5)) {
                        // Always keep 3,4,5
                        keep[i] = true;
                    } else if (has1345 && val == 1 && !valueKept[1]) {
                        // Keep only one 1
                        keep[i] = true;
                        valueKept[1] = true;
                    } else if (has2345 && (val == 3 || val == 4 || val == 5)) {
                        // Always keep 3,4,5
                        keep[i] = true;
                    } else if (has2345 && val == 2 && !valueKept[2]) {
                        // Keep only one 2
                        keep[i] = true;
                        valueKept[2] = true;
                    } else {
                        keep[i] = false;
                    }
                }
                return keep;
            }
            
            // General case for straights - keep unique values that could form a straight
            for (int i = 0; i < dice.length; i++) {
                int val = dice[i];
                // Only keep this die if the value is part of a potential straight
                // and we haven't already kept a die with this value
                if (isPartOfStraight[val] && !valueKept[val]) {
                    keep[i] = true;
                    valueKept[val] = true;
                } else {
                    keep[i] = false;
                }
            }
        } else if (strategy.contains("Full House")) {
            counts = getCounts(dice); // We already computed this at the top, but if we reach here, recalculate
            int threeValue = -1;
            int pairValue1 = -1;
            int pairValue2 = -1;
            
            // Find the values with counts of 3 or 2
            for (int i = 1; i <= 6; i++) {
                if (counts[i] >= 3) {
                    threeValue = i;
                } else if (counts[i] >= 2) {
                    if (pairValue1 == -1) {
                        pairValue1 = i;
                    } else {
                        pairValue2 = i;
                    }
                }
            }
            
            // Keep three of a kind and a pair
            for (int i = 0; i < dice.length; i++) {
                keep[i] = (dice[i] == threeValue || dice[i] == pairValue1 || dice[i] == pairValue2);
            }
        } else {
            // For number categories, keep matching numbers
            String[] parts = strategy.split(" ");
            if (parts.length >= 2) {
                String numberStr = parts[1].replace("s", "");
                try {
                    int targetNumber = Integer.parseInt(numberStr);
                    
                    // MODIFIED LOGIC for low-value dice
                    // Always keep 3 or more of any number
                    if (getCounts(dice)[targetNumber] >= 3) {
                        for (int i = 0; i < dice.length; i++) {
                            keep[i] = (dice[i] == targetNumber);
                        }
                    }
                    // Be more strategic with low-value dice (1s and 2s) only when we have very few
                    else if ((targetNumber == 1 || targetNumber == 2) && getCounts(dice)[targetNumber] < 2) {
                        // If we only have 1 of the low-value dice, don't keep any - recommend rerolling all
                        for (int i = 0; i < dice.length; i++) {
                            keep[i] = false;
                        }
                    } else {
                        // Normal case - keep all matching dice
                        for (int i = 0; i < dice.length; i++) {
                            keep[i] = (dice[i] == targetNumber);
                        }
                    }
                } catch (NumberFormatException e) {
                    // If parsing fails, keep highest value dice
                    int highestValue = 0;
                    for (int value : dice) {
                        highestValue = Math.max(highestValue, value);
                    }
                    for (int i = 0; i < dice.length; i++) {
                        keep[i] = (dice[i] == highestValue);
                    }
                }
            }
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
            case "Three of a Kind": return 6 + 6 + 6 + 6 + 6; // Max would be 5 sixes = 30
            case "Four of a Kind": return 6 + 6 + 6 + 6 + 6; // Max would be 5 sixes = 30
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
        if (dice == null || dice.length == 0) return false;
        
        // Print debug info
        System.out.println("Checking for " + count + " of a kind in dice: " + Arrays.toString(dice));
        
        int[] counts = getCounts(dice);
        System.out.println("Counts array: " + Arrays.toString(counts));
        
        for (int i = 1; i <= 6; i++) {  // Start from 1 since dice values are 1-6
            if (counts[i] >= count) {
                System.out.println("Found " + count + " of a kind with " + i + "s");
                return true;
            }
        }
        System.out.println("Did NOT find " + count + " of a kind");
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
        // First mark which values are present
        boolean[] present = new boolean[7];
        for (int value : dice) present[value] = true;
        
        // Check for the three possible small straight patterns
        return (present[1] && present[2] && present[3] && present[4]) ||  // 1-2-3-4
               (present[2] && present[3] && present[4] && present[5]) ||  // 2-3-4-5
               (present[3] && present[4] && present[5] && present[6]);    // 3-4-5-6
    }

    private static boolean isLargeStraight(int[] dice) {
        // First mark which values are present
        boolean[] present = new boolean[7];
        for (int value : dice) present[value] = true;
        
        // Check for the two possible large straight patterns
        return (present[1] && present[2] && present[3] && present[4] && present[5]) ||  // 1-2-3-4-5
               (present[2] && present[3] && present[4] && present[5] && present[6]);    // 2-3-4-5-6
    }

    private static boolean isYahtzee(int[] dice) {
        int first = dice[0];
        for (int value : dice) {
            if (value != first) return false;
        }
        return true;
    }

    private static int[] getCounts(int[] dice) {
        if (dice == null) {
            System.out.println("WARNING: Null dice array passed to getCounts");
            return new int[7]; // Return empty counts
        }
        
        System.out.println("Counting dice: " + Arrays.toString(dice));
        int[] counts = new int[7];  // 0-6, ignore 0
        for (int value : dice) {
            if (value >= 1 && value <= 6) {
                counts[value]++;
            } else {
                System.out.println("WARNING: Invalid die value: " + value);
            }
        }
        System.out.println("Counts array: " + Arrays.toString(counts));
        return counts;
    }

    private static boolean hasSequenceOfLength(int[] dice, int length) {
        boolean[] present = new boolean[7];
        for (int value : dice) present[value] = true;

        // For Small Straight (length=4), we check if we have either 1,2,3,4 or 2,3,4,5 or 3,4,5,6
        if (length == 4) {
            return (present[1] && present[2] && present[3] && present[4]) ||
                   (present[2] && present[3] && present[4] && present[5]) ||
                   (present[3] && present[4] && present[5] && present[6]);
        }
        // For Large Straight (length=5), we check if we have either 1,2,3,4,5 or 2,3,4,5,6
        else if (length == 5) {
            return (present[1] && present[2] && present[3] && present[4] && present[5]) ||
                   (present[2] && present[3] && present[4] && present[5] && present[6]);
        }
        
        // For other lengths, use the original consecutive counting approach
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

        // Check if this value is part of a potential Small Straight (1-4, 2-5, 3-6)
        if ((value >= 1 && value <= 4 && present[1] && present[2] && present[3] && present[4]) ||
            (value >= 2 && value <= 5 && present[2] && present[3] && present[4] && present[5]) ||
            (value >= 3 && value <= 6 && present[3] && present[4] && present[5] && present[6])) {
            return true;
        }
        
        // Check if this value is part of a potential Large Straight (1-5 or 2-6)
        if ((value >= 1 && value <= 5 && present[1] && present[2] && present[3] && present[4] && present[5]) ||
            (value >= 2 && value <= 6 && present[2] && present[3] && present[4] && present[5] && present[6])) {
            return true;
        }
        
        // Check if it's part of the longest potential sequence
        // First find all possible sequences containing this value
        for (int startVal = Math.max(1, value - 3); startVal <= Math.min(value, 3); startVal++) {
            int endVal = startVal + 3; // Looking for 4 consecutive values for Small Straight
            if (endVal <= 6) {
                int count = 0;
                for (int i = startVal; i <= endVal; i++) {
                    if (present[i]) count++;
                }
                // If we have at least 3 values of a possible 4-value sequence that includes our value
                if (count >= 3 && present[value]) return true;
            }
        }
        
        for (int startVal = Math.max(1, value - 4); startVal <= Math.min(value, 2); startVal++) {
            int endVal = startVal + 4; // Looking for 5 consecutive values for Large Straight
            if (endVal <= 6) {
                int count = 0;
                for (int i = startVal; i <= endVal; i++) {
                    if (present[i]) count++;
                }
                // If we have at least 4 values of a possible 5-value sequence that includes our value
                if (count >= 4 && present[value]) return true;
            }
        }
        
        return false;
    }

    private static boolean isPartOfSequence(int[] dice, int value, int targetLength) {
        boolean[] present = new boolean[7];
        for (int v : dice) present[v] = true;
        
        // Check if this value is part of any sequence that could form a sequence of targetLength
        // For each position in a potential sequence, check if this value could be part
        for (int start = Math.max(1, value - (targetLength - 1)); start <= Math.min(7 - targetLength, value); start++) {
            boolean possible = true;
            int numbersPresent = 0;
            
            // Check if we have enough numbers in this potential sequence
            for (int i = 0; i < targetLength; i++) {
                if (present[start + i]) {
                    numbersPresent++;
                }
            }
            
            // If we have value in this range and enough present numbers to form targetLength with remaining rolls
            if (present[value] && numbersPresent >= targetLength - 2) {
                return true;
            }
        }
        
        return false;
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


