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
    private int rollCount;

    public ComputerPlayer() {
        super("Computer");
        selectedDice = new boolean[5];
        scorecard = new int[13];  // One for each category
        lastCategory = "";
        lastScore = 0;
        rollCount = 0;
    }

    public String analyzeRoll(int[] diceValues) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("Current Dice: ").append(Arrays.toString(diceValues)).append("\n\n");

        // Calculate potential scores
        Map<String, Integer> potentialScores = new HashMap<>();
        for (String category : getAvailableCategories()) {
            int score = Helper.calculateScore(category, diceValues);
            potentialScores.put(category, score);
        }

        // Build detailed explanation of available categories
        analysis.append("AVAILABLE CATEGORIES:\n");
        analysis.append(String.format("%-20s %-10s\n", "Category", "Points"));
        analysis.append("------------------------------\n");
        
        // Sort categories by score (highest to lowest)
        List<Map.Entry<String, Integer>> sortedCategories = new ArrayList<>(potentialScores.entrySet());
        sortedCategories.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        
        for (Map.Entry<String, Integer> entry : sortedCategories) {
            analysis.append(String.format("%-20s %-10d", entry.getKey(), entry.getValue()));
            
            // Add indicator for best scoring options
            if (entry.getValue() > 0) {
                if (entry.getValue() >= 25) {
                    analysis.append(" ★★★"); // High value
                } else if (entry.getValue() >= 15) {
                    analysis.append(" ★★"); // Medium value
                } else if (entry.getValue() > 0) {
                    analysis.append(" ★"); // Low value
                }
            }
            
            analysis.append("\n");
        }
        analysis.append("\n");

        // Add roll count information
        analysis.append("Roll #").append(rollCount).append(" of 3\n\n");

        // Determine best strategy based on current roll
        determineStrategy(diceValues, potentialScores);

        // Add strategic explanation
        analysis.append("STRATEGY ANALYSIS:\n");
        analysis.append("Current Strategy: ").append(currentStrategy != null ? currentStrategy : "None").append("\n\n");

        // Explain dice selection
        analysis.append("DICE SELECTION:\n");
        int selectedCount = 0;
        for (int i = 0; i < diceValues.length; i++) {
            if (selectedDice[i]) {
                selectedCount++;
                analysis.append("Keeping ").append(diceValues[i]);
                if (currentStrategy != null) {
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
                }
                analysis.append("\n");
            }
        }

        // Explain why we're keeping these dice
        analysis.append("\nREASONING:\n");
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
        
        // ULTRA SPECIFIC CHECK: Find exact 2,2,3,4,5 pattern immediately
        boolean hasExact22345 = false;
        int twoCount = 0;
        int threeCount = 0;
        int fourCount = 0;
        int fiveCount = 0;
        
        // Count the values directly
        for (int val : diceValues) {
            if (val == 2) twoCount++;
            else if (val == 3) threeCount++;
            else if (val == 4) fourCount++;
            else if (val == 5) fiveCount++;
        }
        
        // Check for the exact pattern
        if (twoCount == 2 && threeCount == 1 && fourCount == 1 && fiveCount == 1) {
            hasExact22345 = true;
            System.out.println("FOUND EXACT [2,2,3,4,5] PATTERN! Dice values: " + Arrays.toString(diceValues));
        }
        
        // If we have exactly 2,2,3,4,5 and Large Straight is available, always pursue it
        if (hasExact22345 && getAvailableCategories().contains("Large Straight")) {
            System.out.println("FORCING STRATEGY FOR EXACT [2,2,3,4,5] PATTERN!");
            
            // Set strategy
            currentStrategy = "Pursuing Large Straight";
            
            // Keep exactly one 2 and all 3,4,5
            boolean keptOne2 = false;
            for (int i = 0; i < diceValues.length; i++) {
                int value = diceValues[i];
                
                if (value == 2) {
                    if (!keptOne2) {
                        selectedDice[i] = true;
                        keptOne2 = true;
                        System.out.println("Keeping first 2 at position " + i);
                    } else {
                        selectedDice[i] = false;
                        System.out.println("Rerolling second 2 at position " + i);
                    }
                } else if (value == 3 || value == 4 || value == 5) {
                    selectedDice[i] = true;
                    System.out.println("Keeping " + value + " at position " + i);
                } else {
                    selectedDice[i] = false;
                    System.out.println("Discarding " + value + " at position " + i);
                }
            }
            
            // Verify correct selection
            System.out.println("FINAL SELECTION ARRAY: " + Arrays.toString(selectedDice));
            return;
        }
        
        // SPECIAL CASE: Check for 4 of a kind FIRST - this should override everything else
        int[] counts = getCounts(diceValues);
        for (int i = 1; i <= 6; i++) {
            if (counts[i] >= 4) {
                System.out.println("STRATEGY FIX: Found 4 " + i + "s - forcing Yahtzee strategy");
                currentStrategy = "Pursuing Yahtzee with " + i + "s";
                // Keep all dice of this value
                for (int j = 0; j < diceValues.length; j++) {
                    selectedDice[j] = (diceValues[j] == i);
                }
                return; // Exit immediately
            }
        }
        
        // Check for Yahtzee first - if we have a Yahtzee (all 5 dice the same), prioritize it
        boolean hasYahtzee = isYahtzee(diceValues);
        List<String> availableCategories = getAvailableCategories();
        
        // If we have a Yahtzee and Yahtzee category is available, always choose it
        if (hasYahtzee && availableCategories.contains("Yahtzee")) {
            currentStrategy = "Scoring Yahtzee";
            // Mark all dice as selected (we're keeping everything)
            Arrays.fill(selectedDice, true);
            return;
        }

        // CRITICAL FIX: Detect the special pattern [2,2,3,4,5] early and prioritize Large Straight
        boolean[] hasValue = new boolean[7]; // 0-6, ignore 0
        for (int v : diceValues) {
            hasValue[v] = true;
        }
        
        boolean hasTwo = hasValue[2];
        boolean hasThree = hasValue[3];
        boolean hasFour = hasValue[4];
        boolean hasFive = hasValue[5];
        boolean has2345 = hasTwo && hasThree && hasFour && hasFive;
        boolean has2Duplicate = counts[2] > 1;
        
        if (has2345 && has2Duplicate && availableCategories.contains("Large Straight")) {
            System.out.println("CRITICAL FIX: Detected [2,2,3,4,5] pattern - prioritizing Large Straight strategy");
            currentStrategy = "Pursuing Large Straight";
            
            // Keep one 2 and all 3,4,5
            boolean keptOne2 = false;
            for (int i = 0; i < diceValues.length; i++) {
                int value = diceValues[i];
                if (value == 2) {
                    if (!keptOne2) {
                        selectedDice[i] = true;
                        keptOne2 = true;
                        System.out.println("Keeping one 2 for Large Straight potential");
                    } else {
                        selectedDice[i] = false;
                        System.out.println("Rerolling duplicate 2 to try for 1 or 6 for Large Straight");
                    }
                } else if (value == 3 || value == 4 || value == 5) {
                    selectedDice[i] = true;
                    System.out.println("Keeping " + value + " for Straight");
                } else {
                    selectedDice[i] = false;
                    System.out.println("Discarding " + value);
                }
            }
            return;
        }

        // If only one category is available, focus on that category
        if (availableCategories.size() == 1) {
            String onlyCategory = availableCategories.get(0);
            currentStrategy = "Pursuing " + onlyCategory;

            switch (onlyCategory) {
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
                    handleNumberStrategy(diceValues, onlyCategory);
                    break;
            }
            return;
        }

        // SPECIAL CASE: Explicitly check for potential Small Straight or Large Straight
        // This will override normal strategy selection for certain dice combinations
        // Check for 1,3,4,5 or 2,3,4,5 pattern (close to Large Straight)
        boolean hasOne = hasValue[1];
        boolean hasSix = hasValue[6];
        
        // CRITICAL FIX: Special patterns for Large Straight
        boolean has1345 = hasOne && hasThree && hasFour && hasFive;
        
        // If we have a promising pattern for Large Straight, prioritize it
        if ((has1345 || has2345) && availableCategories.contains("Large Straight") && rollCount < 3) {
            System.out.println("CRITICAL FIX: Found pattern for Large Straight - forcing Large Straight strategy");
            currentStrategy = "Pursuing Large Straight";
            
            // Count occurrences of each value - need to handle duplicate 3s 
            int[] valueCounts = new int[7];
            for (int value : diceValues) {
                valueCounts[value]++;
            }
            
            // For 1,3,4,5 pattern, keep one 1 and all 3,4,5
            // For 2,3,4,5 pattern, keep one 2 and all 3,4,5
            Arrays.fill(selectedDice, false); // Reset selection
            
            boolean keptOne = false;
            boolean keptTwo = false;
            boolean keptThree = false; // Track if we've kept one 3 already
            
            for (int i = 0; i < diceValues.length; i++) {
                if (has1345 && diceValues[i] == 1 && !keptOne) {
                    selectedDice[i] = true;
                    keptOne = true;
                } else if (has2345 && diceValues[i] == 2 && !keptTwo) {
                    selectedDice[i] = true;
                    keptTwo = true;
                } else if (diceValues[i] == 3) {
                    // BUGFIX: Only keep one 3, especially when we have duplicates
                    if (!keptThree) {
                        selectedDice[i] = true;
                        keptThree = true;
                    } else {
                        selectedDice[i] = false;  // Don't keep additional 3s
                    }
                } else if (diceValues[i] == 4 || diceValues[i] == 5) {
                    selectedDice[i] = true;
                }
            }
            
            // Log results for debugging
            if (valueCounts[3] > 1) {
                System.out.println("CRITICAL BUGFIX: Found duplicate 3s - only keeping one 3 to allow for a 2 in Large Straight");
            }
            
            return;
        }
        
        // Check for consecutive values (potential straights)
        int bestSequenceLength = 0;
        int currentLength = 0;
        
        for (int i = 1; i <= 6; i++) {
            if (hasValue[i]) {
                currentLength++;
                bestSequenceLength = Math.max(bestSequenceLength, currentLength);
            } else {
                currentLength = 0;
            }
        }
        
        // BUGFIX: Check for multiple pairs - this is a good start for Full House
        // Check this BEFORE the Small Straight logic
        int pairCount = 0;
        int threeOfAKindValue = -1;
        for (int i = 1; i <= 6; i++) {
            if (counts[i] >= 3) {
                threeOfAKindValue = i;
            } else if (counts[i] == 2) {
                pairCount++;
            }
        }
        
        // If we have two pairs or a three of a kind + a pair, prioritize Full House
        if ((pairCount >= 2 || (threeOfAKindValue > 0 && pairCount >= 1)) && 
            availableCategories.contains("Full House")) {
            System.out.println("STRATEGY OVERRIDE: Found multiple pairs or three of a kind + pair - prioritizing Full House");
            currentStrategy = "Pursuing Full House";
            handleFullHouseStrategy(diceValues);
            return;
        }
        
        // If we have 3 or more consecutive values, prioritize straight strategy over Aces/low values
        if (bestSequenceLength >= 3 && availableCategories.contains("Small Straight")) {
            System.out.println("STRATEGY OVERRIDE: Found " + bestSequenceLength + 
                              " consecutive values - forcing Small Straight strategy");
            currentStrategy = "Pursuing Small Straight";
            handleSmallStraightStrategy(diceValues);
            return;
        }
        
        // Calculate current game state
        int upperSectionScore = calculateUpperSectionScore();
        boolean needsUpperBonus = upperSectionScore < 63;

        // Find best potential category considering game state
        String bestCategory = null;
        int bestScore = 0;
        double bestExpectedValue = 0;

        // Special case for lots of 1s
        boolean hasMany1s = counts[1] >= 3;
        
        for (Map.Entry<String, Integer> entry : potentialScores.entrySet()) {
            String category = entry.getKey();
            int score = entry.getValue();
            
            // Special case: If we have a Yahtzee, always prefer Yahtzee category if available
            if (hasYahtzee && category.equals("Yahtzee")) {
                bestCategory = "Yahtzee";
                bestScore = score;
                break; // No need to check other categories
            }
            
            // If we have many 1s, reduce the expected value of Aces unless it's one of our only options
            double expectedValue = calculateExpectedValue(category, score, needsUpperBonus, 3);
            
            // Reduce expected value for low-value categories early in the game
            if (availableCategories.size() > 3) {
                if ((hasMany1s && category.equals("Aces")) || 
                    (counts[2] >= 3 && category.equals("Twos")) || 
                    (counts[3] >= 3 && category.equals("Threes"))) {
                    expectedValue *= 0.5; // Reduce expected value by half
                    System.out.println("Reducing expected value for " + category);
                }
            }
            
            // Even more aggressively penalize Aces when we have few of them
            if (category.equals("Aces") && counts[1] <= 2) {
                expectedValue *= 0.2; // Severely reduce expected value for Aces when we only have 1 or 2
                System.out.println("Severely reducing expected value for Aces - only have " + counts[1]);
            }

            if (expectedValue > bestExpectedValue) {
                bestExpectedValue = expectedValue;
                bestScore = score;
                bestCategory = category;
            }
        }

        // If no good strategy found, set currentStrategy to null
        if (bestCategory == null) {
            currentStrategy = null;
            return;
        }

        // Apply the best strategy found
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

        // For a completed Four of a Kind (or better), prioritize that over upper section
        if ((category.equals("Four of a Kind") || category.equals("Yahtzee")) && 
            currentScore > 0 && rollCount == 3) {
            return baseValue * 2.5; // Significantly increase priority to ensure Four of a Kind gets priority
        }
        
        // BUGFIX: Special case for Full House when we have multiple pairs or three of a kind
        if (category.equals("Full House")) {
            // Count pairs and three of a kinds in current dice
            int[] counts = getCounts(null); // We don't have access to dice values here
            int pairCount = 0;
            boolean hasThreeOfKind = false;
            
            // If we're on roll 1, boost Full House slightly to encourage that strategy
            if (rollCount == 1) {
                baseValue *= 1.2;
            }
            
            // On final roll, if our current score is 25 (full house), prioritize this highly
            if (rollCount == 3 && currentScore == 25) {
                return baseValue * 1.5; // Significantly boost completed Full House
            }
        }

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
                baseValue *= 1.1; // Increased from 1.05 to value it more
                break;
            case "Small Straight":
                baseValue *= 1.0;
                break;
            case "Four of a Kind":
                baseValue *= 0.95; // Increased from 0.9 to value it more
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
        // IMPROVED: Find the value with the most occurrences, prioritizing higher counts
        int[] counts = getCounts(diceValues);
        int maxCount = 0;
        int bestValue = 0;
        
        // First check for 4 of a kind - that's an automatic priority
        for (int i = 1; i <= 6; i++) {
            if (counts[i] >= 4) {
                System.out.println("YAHTZEE STRATEGY: Found 4 of a kind with " + i + "s - prioritizing for Yahtzee");
                bestValue = i;
                maxCount = counts[i];
                break; // No need to look further
            }
        }
        
        // If no 4 of a kind, find the most common value
        if (bestValue == 0) {
            for (int i = 1; i <= 6; i++) {
                if (counts[i] > maxCount) {
                    maxCount = counts[i];
                    bestValue = i;
                } else if (counts[i] == maxCount && i > bestValue) {
                    // If tied, prefer higher values
                    bestValue = i;
                }
            }
        }
        
        // Set strategy and keep dice
        if (bestValue > 0) {
            currentStrategy = "Pursuing Yahtzee with " + bestValue + "s";
            for (int i = 0; i < diceValues.length; i++) {
                selectedDice[i] = (diceValues[i] == bestValue);
            }
            System.out.println("YAHTZEE STRATEGY: Keeping all " + bestValue + "s for Yahtzee potential");
        } else {
            // Fallback to original logic if something went wrong
            int mostCommon = findMostCommonValue(diceValues);
            currentStrategy = "Pursuing Yahtzee with " + mostCommon + "s";
            for (int i = 0; i < diceValues.length; i++) {
                selectedDice[i] = (diceValues[i] == mostCommon);
            }
        }
    }

    private void handleLargeStraightStrategy(int[] diceValues) {
        currentStrategy = "Pursuing Large Straight";
        // Reset selection array
        Arrays.fill(selectedDice, false);

        // BUGFIX: Properly handle the case of 1,3,3,4,5 for Large Straight
        boolean[] hasValue = new boolean[7]; // 0-6, ignore 0
        int[] valueCounts = new int[7]; // Count of each dice value
        
        // Count occurrences of each value
        for (int value : diceValues) {
            hasValue[value] = true;
            valueCounts[value]++;
        }
        
        // Check for special patterns like 1,3,3,4,5 or 2,3,3,4,5
        boolean has1345 = hasValue[1] && hasValue[3] && hasValue[4] && hasValue[5];
        boolean has2345 = hasValue[2] && hasValue[3] && hasValue[4] && hasValue[5];
        
        if ((has1345 || has2345) && valueCounts[3] > 1) {
            System.out.println("LARGE STRAIGHT FIX: Detected duplicate 3s with 1,3,4,5 or 2,3,4,5 pattern");
            
            // Keep 1/2,3,4,5 but only ONE 3
            boolean keptOne3 = false;
            
            for (int i = 0; i < diceValues.length; i++) {
                int value = diceValues[i];
                if (value == 1 && has1345) {
                    selectedDice[i] = true;
                } else if (value == 2 && has2345) {
                    selectedDice[i] = true;
                } else if (value == 3) {
                    // Only keep one 3
                    if (!keptOne3) {
                        selectedDice[i] = true;
                        keptOne3 = true;
                    } else {
                        selectedDice[i] = false; // Reroll additional 3s
                    }
                } else if (value == 4 || value == 5) {
                    selectedDice[i] = true;
                }
            }
            
            System.out.println("LARGE STRAIGHT FIX: Keeping one 3 and rerolling duplicates to try for 2");
            return;
        }
        
        // Original logic for other cases
        for (int i = 0; i < diceValues.length; i++) {
            int value = diceValues[i];
            // Only set true if part of the sequence
            selectedDice[i] = isPartOfLargestSequence(diceValues, value);
        }
    }

    private void handleSmallStraightStrategy(int[] diceValues) {
        currentStrategy = "Pursuing Small Straight";
        
        // Analyze current dice values
        boolean[] hasValue = new boolean[7]; // 0-6, ignore 0
        int[] valueCounts = new int[7]; // Count of each dice value
        
        // Fill the arrays
        for (int value : diceValues) {
            hasValue[value] = true;
            valueCounts[value]++;
        }
        
        // Special handling for [2,2,3,4,5] pattern - prioritize going for Large Straight
        boolean has2345 = hasValue[2] && hasValue[3] && hasValue[4] && hasValue[5];
        boolean has2Duplicate = valueCounts[2] > 1;
        
        if (has2345 && has2Duplicate) {
            System.out.println("STRATEGIC PATTERN: Detected [2,2,3,4,5] - keeping one 2 and going for Large Straight");
            
            // Keep one 2 and all 3, 4, 5
            boolean keptOne2 = false;
            for (int i = 0; i < diceValues.length; i++) {
                int value = diceValues[i];
                if (value == 2) {
                    if (!keptOne2) {
                        selectedDice[i] = true;
                        keptOne2 = true;
                        System.out.println("Keeping one 2 for potential Large Straight");
                    } else {
                        selectedDice[i] = false;
                        System.out.println("Rerolling duplicate 2 to try for 1 or 6 to complete Large Straight");
                    }
                } else if (value == 3 || value == 4 || value == 5) {
                    selectedDice[i] = true;
                    System.out.println("Keeping " + value + " for Straight");
                } else {
                    selectedDice[i] = false;
                    System.out.println("Discarding " + value);
                }
            }
            
            return;
        }
        
        // Count how many dice are part of a potential Small Straight
        int countInSequence = 0;
        for (int value : diceValues) {
            if (isPartOfSmallestSequence(diceValues, value)) {
                countInSequence++;
            }
        }

        // If we already have 4 dice in sequence, hold them and re-roll the others
        if (countInSequence >= 4) {
            for (int i = 0; i < diceValues.length; i++) {
                int value = diceValues[i];
                if (isPartOfSmallestSequence(diceValues, value)) {
                    // This is duplicated logic for the special case [2,2,3,4,5]
                    // If we have a duplicate 2 and already kept one, don't keep the second
                    if (value == 2 && valueCounts[2] > 1) {
                        boolean alreadyKeptA2 = false;
                        for (int j = 0; j < i; j++) {
                            if (diceValues[j] == 2 && selectedDice[j]) {
                                alreadyKeptA2 = true;
                                break;
                            }
                        }
                        if (alreadyKeptA2) {
                            selectedDice[i] = false;
                            continue;
                        }
                    }
                    selectedDice[i] = true;
                } else {
                    selectedDice[i] = false;
                }
            }
            return;
        }

        // Original strategy for fewer matching dice
        for (int i = 0; i < diceValues.length; i++) {
            if (isPartOfSmallestSequence(diceValues, diceValues[i])) {
                selectedDice[i] = true;
            } else {
                selectedDice[i] = false;
            }
        }
    }

    private void handleFullHouseStrategy(int[] diceValues) {
        currentStrategy = "Pursuing Full House";
        int[] counts = getCounts(diceValues);
        int threeOfAKind = -1;
        int pairValue1 = -1;
        int pairValue2 = -1;

        // First find any three of a kind
        for (int i = 1; i <= 6; i++) {
            if (counts[i] >= 3) {
                threeOfAKind = i;
                break;
            }
        }

        // Then look for pairs (or a second three of a kind)
        for (int i = 6; i >= 1; i--) {
            if (i != threeOfAKind && counts[i] >= 2) {
                if (pairValue1 == -1) {
                    pairValue1 = i;
                } else if (pairValue2 == -1) {
                    pairValue2 = i;
                }
            }
        }

        // IMPROVED: Handle the case where we have two pairs but no three of a kind
        if (threeOfAKind == -1 && pairValue1 != -1 && pairValue2 != -1) {
            System.out.println("FULL HOUSE: Have two pairs (" + pairValue1 + " and " + pairValue2 + ") but no three of a kind");
            
            // Choose the higher value pair to try to make into three of a kind
            int pairToUse = Math.max(pairValue1, pairValue2);
            int otherPair = Math.min(pairValue1, pairValue2);
            
            System.out.println("FULL HOUSE: Trying to make " + pairToUse + " into three of a kind, keeping pair of " + otherPair);
            
            // Keep all dice of the selected pairs
            for (int i = 0; i < diceValues.length; i++) {
                selectedDice[i] = (diceValues[i] == pairToUse || diceValues[i] == otherPair);
            }
            return;
        }
        
        // IMPROVED: Handle scenario where we have one pair and a single die (try to make the pair into three of a kind)
        if (threeOfAKind == -1 && pairValue1 != -1 && pairValue2 == -1) {
            // Keep the pair and look for any single die that could be used for a second pair
            System.out.println("FULL HOUSE: Have a pair of " + pairValue1 + " but no three of a kind");
            
            // If we have a pair of low value (1s or 2s), try for three of a kind
            // Otherwise, keep the pair and any other highest value dice
            for (int i = 0; i < diceValues.length; i++) {
                if (diceValues[i] == pairValue1) {
                    // Always keep the pair
                    selectedDice[i] = true;
                } else {
                    // Only keep other dice if we're not trying to complete the three of a kind
                    selectedDice[i] = false; // Reroll non-matching dice to try for another of the pair
                }
            }
            return;
        }

        // Handle the normal case where we have a three of a kind and/or a pair
        for (int i = 0; i < diceValues.length; i++) {
            if (diceValues[i] == threeOfAKind || diceValues[i] == pairValue1 || diceValues[i] == pairValue2) {
                selectedDice[i] = true;
            } else {
                selectedDice[i] = false;
            }
        }
        
        // Log what we're doing
        if (threeOfAKind != -1 && pairValue1 != -1) {
            System.out.println("FULL HOUSE: Already have three " + threeOfAKind + "s and a pair of " + pairValue1 + "s - keeping all");
        } else if (threeOfAKind != -1) {
            System.out.println("FULL HOUSE: Have three " + threeOfAKind + "s - need a pair");
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
        
        // Special case for low-value categories (Aces, Twos, Threes) - be more selective
        if (category.equals("Aces") || category.equals("Twos") || category.equals("Threes")) {
            // Count how many of the target number we have
            int count = 0;
            for (int value : diceValues) {
                if (value == targetNumber) count++;
            }
            
            // Define thresholds based on the category
            int goodCount = (category.equals("Aces")) ? 4 : 
                           (category.equals("Twos")) ? 3 : 3; // Adjust as needed
            
            // If we have just a few of the target number and not on final roll, consider rerolling everything
            if (count < goodCount && rollCount < 3) {
                // Only keep the target numbers if we have enough of them
                for (int i = 0; i < diceValues.length; i++) {
                    selectedDice[i] = diceValues[i] == targetNumber;
                }
                return;
            }
        }
        
        // For other numbers or if we don't satisfy the special conditions
        for (int i = 0; i < diceValues.length; i++) {
            selectedDice[i] = (diceValues[i] == targetNumber);
        }
    }

    // Helper methods
    private int findMostCommonValue(int[] diceValues) {
        int[] counts = getCounts(diceValues);
        int maxCount = 0;
        int mostCommon = 1;

        // First specifically check for 4 of a kind
        for (int i = 1; i <= 6; i++) {
            if (counts[i] >= 4) {
                System.out.println("STRATEGY: Found 4 " + i + "s - prioritizing");
                return i; // Immediately return this value
            }
        }

        // If no 4 of a kind, proceed normally
        for (int i = 1; i <= 6; i++) {
            if (counts[i] > maxCount) {
                maxCount = counts[i];
                mostCommon = i;
            } else if (counts[i] == maxCount && i > mostCommon) {
                // In case of a tie, prefer higher values
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
        // First, identify if we have a sequence
        boolean[] hasValue = new boolean[7]; // 0-6, ignore 0
        for (int v : diceValues) {
            hasValue[v] = true;
        }
        
        // Count consecutive values
        int bestSequenceStart = 0;
        int bestSequenceLength = 0;
        int currentStart = 0;
        int currentLength = 0;
        
        for (int i = 1; i <= 6; i++) {
            if (hasValue[i]) {
                if (currentLength == 0) {
                    currentStart = i;
                }
                currentLength++;
                
                // Update best sequence if this one is better
                if (currentLength > bestSequenceLength) {
                    bestSequenceLength = currentLength;
                    bestSequenceStart = currentStart;
                }
            } else {
                currentLength = 0;
            }
        }
        
        // If the die's value is within the best sequence, keep it
        if (bestSequenceLength >= 3) { // Only consider sequences of at least 3
            return value >= bestSequenceStart && value < bestSequenceStart + bestSequenceLength;
        }
        
        // If no good sequence, prefer higher values
        return value >= 4;
    }

    private boolean isPartOfSmallestSequence(int[] diceValues, int value) {
        // Use the improved largest sequence method
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

    public String getCurrentStrategy() {
        return currentStrategy;
    }

    public void resetDiceSelections() {
        // Clear the selected dice array
        Arrays.fill(selectedDice, false);
        // Reset the current strategy
        currentStrategy = null;
    }

    public boolean shouldRollAgain() {
        // First check if we're on the final roll
        if (rollCount >= 3) {
            return false;
        }
        
        // ULTRA SPECIFIC FIX: If we have exactly [2,2,3,4,5], ALWAYS reroll
        boolean hasExact22345 = false;
        int twoCount = 0;
        int threeCount = 0;
        int fourCount = 0; 
        int fiveCount = 0;
        
        // We need access to the dice values, which are not directly available here
        // But we can infer them from the selected dice if our strategy was correctly set
        if (currentStrategy != null && currentStrategy.contains("Large Straight")) {
            // Look for selection pattern where we keep 2,3,4,5 and reroll the second 2
            boolean hasSelectedOne2 = false;
            boolean hasSelected3 = false;
            boolean hasSelected4 = false;
            boolean hasSelected5 = false;
            int selectedCount = 0;
            
            for (boolean isSelected : selectedDice) {
                if (isSelected) selectedCount++;
            }
            
            // If we're keeping exactly 4 dice, it might be the 2,3,4,5 pattern
            if (selectedCount == 4) {
                System.out.println("SHOULD ROLL: Strategy is for Large Straight and keeping 4 dice - ALWAYS rolling again");
                return true;
            }
        }
        
        // If we have no strategy, we should roll again
        if (currentStrategy == null) {
            return true;
        }
        
        // If we have a strategy but no dice selected, we should roll again
        boolean hasSelectedDice = false;
        for (boolean isSelected : selectedDice) {
            if (isSelected) {
                hasSelectedDice = true;
                break;
            }
        }
        if (!hasSelectedDice) return true;

        // Count how many dice are being kept
        int keptDiceCount = 0;
        for (boolean isSelected : selectedDice) {
            if (isSelected) keptDiceCount++;
        }

        // For different strategies, we need different numbers of matching dice
        if (currentStrategy.contains("Yahtzee")) {
            return keptDiceCount < 5;
        } else if (currentStrategy.contains("Four of a Kind")) {
            return keptDiceCount < 4;
        } else if (currentStrategy.contains("Three of a Kind")) {
            return keptDiceCount < 3;
        } else if (currentStrategy.contains("Full House")) {
            return keptDiceCount < 5;
        } else if (currentStrategy.contains("Large Straight") || currentStrategy.contains("Small Straight")) {
            // For straights, keep rolling if we have fewer than 4 dice selected
            // This is important to get the missing dice for a good straight
            return keptDiceCount < 5; // CHANGED from 4 to 5 to ensure we always roll if not complete
        } else {
            // For number categories like Aces, Twos, etc.
            if (currentStrategy.contains("Aces") && keptDiceCount <= 2) {
                // Special case: If we're pursuing Aces but have only 1-2 of them, consider rerolling
                return true;
            }
            return keptDiceCount < 3;
        }
    }

    public String selectCategory(int[] diceValues) {
        Map<String, Integer> scores = new HashMap<>();
        List<String> availableCategories = getAvailableCategories();
        
        // For debugging
        System.out.println("Roll count: " + rollCount);
        System.out.println("Dice values: " + Arrays.toString(diceValues));
        
        // ULTRA SPECIFIC FIX: Check for [2,2,3,4,5] pattern and never score on first roll
        boolean hasExact22345Pattern = false;
        int twoCount = 0;
        int threeCount = 0;
        int fourCount = 0;
        int fiveCount = 0;
        
        // Count occurrences
        for (int val : diceValues) {
            if (val == 2) twoCount++;
            else if (val == 3) threeCount++;
            else if (val == 4) fourCount++;
            else if (val == 5) fiveCount++;
        }
        
        if (twoCount == 2 && threeCount == 1 && fourCount == 1 && fiveCount == 1) {
            hasExact22345Pattern = true;
            System.out.println("EXTREME FIX: Found exact [2,2,3,4,5] pattern in selectCategory!");
            
            // If we're not on the last roll and Large Straight is available, roll again
            if (rollCount < 3 && availableCategories.contains("Large Straight")) {
                System.out.println("EXTREME FIX: Rolling again to try for Large Straight!");
                return null; // Signal to roll again
            }
        }
        
        // Calculate raw scores for all available categories
        Map<String, Integer> rawScores = new HashMap<>();
        for (String category : availableCategories) {
            int score = Helper.calculateScore(category, diceValues);
            rawScores.put(category, score);
            if (score > 0) {
                scores.put(category, score);
                // For debugging
                System.out.println("Category: " + category + ", Score: " + score);
            }
        }

        // CRITICAL FIX: ALWAYS prioritize Large Straight and Small Straight if available with optimal scores
        if (rawScores.containsKey("Large Straight") && rawScores.get("Large Straight") == 40) {
            System.out.println("CRITICAL FIX: Detected perfect Large Straight (40 points). Selecting it automatically.");
            lastCategory = "Large Straight";
            lastScore = 40;
            super.fillCategory("Large Straight", 40);
            return "Large Straight";
        }
        
        // NEVER score Small Straight automatically on first roll if we have [2,2,3,4,5]
        if (rawScores.containsKey("Small Straight") && rawScores.get("Small Straight") == 30) {
            // Check if we have [2,2,3,4,5] pattern
            if (hasExact22345Pattern && rollCount < 3 && availableCategories.contains("Large Straight")) {
                System.out.println("BLOCKING Small Straight scoring with [2,2,3,4,5] to pursue Large Straight");
                return null; // Roll again
            }
            
            System.out.println("CRITICAL FIX: Detected perfect Small Straight (30 points). Selecting it automatically.");
            lastCategory = "Small Straight";
            lastScore = 30;
            super.fillCategory("Small Straight", 30);
            return "Small Straight";
        }
        
        // BUGFIX: Apply Three of a Kind fix FIRST
        String threeKindFixedCategory = ThreeOfAKindFixer.fixCategorySelection(diceValues, rawScores, rollCount);
        if (threeKindFixedCategory != null) {
            int score = rawScores.get(threeKindFixedCategory);
            lastCategory = threeKindFixedCategory;
            lastScore = score;
            super.fillCategory(threeKindFixedCategory, score);
            System.out.println("FIXED: Forcing selection of " + threeKindFixedCategory + " with score " + score);
            return threeKindFixedCategory;
        }
        
        // DIRECT BUGFIX: Check for Four of a Kind vs Three of a Kind with same score
        // Reuse the counts array that will be initialized later
        boolean hasFourOfKind = false;
        // Calculate counts if needed for our check
        int[] diceCounts = getCounts(diceValues);
        for (int i = 1; i <= 6; i++) {
            if (diceCounts[i] >= 4) {
                hasFourOfKind = true;
                break;
            }
        }
        
        if (hasFourOfKind && rollCount == 3 && 
            rawScores.containsKey("Four of a Kind") && rawScores.containsKey("Three of a Kind")) {
            int fourScore = rawScores.get("Four of a Kind");
            int threeScore = rawScores.get("Three of a Kind");
            
            // Critical fix: If scores are equal or Four of a Kind is higher, always choose Four of a Kind
            if (fourScore >= threeScore) {
                System.out.println("CRITICAL BUGFIX: Forcing Four of a Kind (" + fourScore + 
                                  ") over Three of a Kind (" + threeScore + ")");
                lastCategory = "Four of a Kind";
                lastScore = fourScore;
                super.fillCategory("Four of a Kind", fourScore);
                return "Four of a Kind";
            }
        }
        
        // BUGFIX: Apply Four of a Kind fix next
        String fixedCategory = FourOfAKindFixer.fixCategorySelection(diceValues, rawScores, this);
        if (fixedCategory != null) {
            int score = rawScores.get(fixedCategory);
            lastCategory = fixedCategory;
            lastScore = score;
            super.fillCategory(fixedCategory, score);
            System.out.println("BUG FIX: Forcing selection of " + fixedCategory + " with score " + score);
            return fixedCategory;
        }
        
        // Special case: Check for Yahtzee first
        boolean hasYahtzee = isYahtzee(diceValues);
        if (hasYahtzee && availableCategories.contains("Yahtzee")) {
            lastCategory = "Yahtzee";
            lastScore = 50; // Yahtzee is worth 50 points
            super.fillCategory("Yahtzee", 50);
            return "Yahtzee";
        }
        
        // STRAIGHT DETECTION FIX: If we have 3 consecutive numbers and it's not the final roll,
        // we want to reroll the remaining dice to try for a straight
        if (rollCount < 3) {
            boolean[] hasValue = new boolean[7]; // 0-6, ignore 0
            for (int v : diceValues) {
                hasValue[v] = true;
            }
            
            // CRITICAL FIX: Special case for patterns close to Large Straight
            boolean hasOne = hasValue[1];
            boolean hasTwo = hasValue[2];
            boolean hasThree = hasValue[3];
            boolean hasFour = hasValue[4];
            boolean hasFive = hasValue[5];
            boolean hasSix = hasValue[6];
            
            boolean has1345 = hasOne && hasThree && hasFour && hasFive;
            boolean has2345 = hasTwo && hasThree && hasFour && hasFive;
            
            // If we have a pattern that's one die away from Large Straight, roll again
            if ((has1345 || has2345) && availableCategories.contains("Large Straight")) {
                System.out.println("CRITICAL FIX: Found [1,3,4,5] or [2,3,4,5] pattern - rolling again for Large Straight");
                return null; // Signal to roll again instead of scoring in Small Straight
            }
            
            // BUGFIX: Special case for [2,2,3,4,5] - need 1 or 6 for Large Straight
            // Count occurrences of each value
            int[] counts = getCounts(diceValues);
            
            // Check if we have 2,3,4,5 with duplicate 2
            if (hasTwo && hasThree && hasFour && hasFive && counts[2] >= 2 &&
                availableCategories.contains("Large Straight") && availableCategories.contains("Small Straight")) {
                
                System.out.println("STRATEGIC FIX: Found [2,2,3,4,5] pattern - rolling again for Large Straight instead of scoring Small Straight");
                System.out.println("STRATEGIC FIX: Need 1 or 6 to complete Large Straight");
                
                // Always roll again on the first 2 rolls to try for Large Straight
                if (rollCount < 3) {
                    return null; // Signal to roll again instead of scoring in Small Straight
                }
            }
            
            // General check for 2,3,4,5 or 1,2,3,4 or 3,4,5,6 patterns (one away from Large Straight)
            boolean potentialLargeStraight = 
                (hasTwo && hasThree && hasFour && hasFive) || // 2,3,4,5 - need 1 or 6
                (hasOne && hasTwo && hasThree && hasFour) || // 1,2,3,4 - need 5
                (hasThree && hasFour && hasFive && hasSix);  // 3,4,5,6 - need 2
                
            if (potentialLargeStraight && availableCategories.contains("Large Straight") && 
                availableCategories.contains("Small Straight") && rollCount < 3) {
                System.out.println("STRATEGIC FIX: Found pattern one die away from Large Straight - rolling again");
                return null; // Signal to roll again instead of scoring in Small Straight
            }
            
            int maxConsecutive = 0;
            int currentConsecutive = 0;
            for (int i = 1; i <= 6; i++) {
                if (hasValue[i]) {
                    currentConsecutive++;
                    maxConsecutive = Math.max(maxConsecutive, currentConsecutive);
                } else {
                    currentConsecutive = 0;
                }
            }
            
            // If we have 3 or more consecutive values and Small/Large Straight is available, 
            // don't score yet, try for straight
            if (maxConsecutive >= 3) {
                boolean straightAvailable = availableCategories.contains("Small Straight") || 
                                         availableCategories.contains("Large Straight");
                if (straightAvailable) {
                    System.out.println("REROLL FIX: Found " + maxConsecutive + 
                                     " consecutive values - rerolling for straight");
                    return null; // Signal to roll again
                }
            }
        }
        
        // ADDITIONAL CHECK: Prioritize high-value combinations
        if (rawScores.containsKey("Large Straight") && rawScores.get("Large Straight") > 0) {
            System.out.println("HIGH VALUE FIX: Selecting Large Straight with " + rawScores.get("Large Straight") + " points");
            lastCategory = "Large Straight";
            lastScore = rawScores.get("Large Straight");
            super.fillCategory("Large Straight", lastScore);
            return "Large Straight";
        }
        
        // STRATEGIC FIX: Don't immediately score Small Straight if we have a potential for Large Straight
        if (rawScores.containsKey("Small Straight") && rawScores.get("Small Straight") > 0) {
            boolean potentialForLargeStraight = false;
            
            // Recheck for patterns that are close to Large Straight
            boolean[] hasValue = new boolean[7]; // 0-6, ignore 0
            for (int v : diceValues) {
                hasValue[v] = true;
            }
            boolean hasOne = hasValue[1];
            boolean hasTwo = hasValue[2];
            boolean hasThree = hasValue[3];
            boolean hasFour = hasValue[4];
            boolean hasFive = hasValue[5];
            boolean hasSix = hasValue[6];
            
            boolean has2345 = hasTwo && hasThree && hasFour && hasFive; // Need 1 or 6
            boolean has1234 = hasOne && hasTwo && hasThree && hasFour; // Need 5
            boolean has3456 = hasThree && hasFour && hasFive && hasSix; // Need 2
            
            potentialForLargeStraight = has2345 || has1234 || has3456;
            
            // Only score Small Straight if:
            // 1. Large Straight is not available, OR
            // 2. We don't have a potential for Large Straight, OR
            // 3. It's the final roll (rollCount == 3)
            if (!availableCategories.contains("Large Straight") || !potentialForLargeStraight || rollCount == 3) {
                System.out.println("HIGH VALUE FIX: Selecting Small Straight with " + rawScores.get("Small Straight") + " points");
                lastCategory = "Small Straight";
                lastScore = rawScores.get("Small Straight");
                super.fillCategory("Small Straight", lastScore);
                return "Small Straight";
            } else {
                System.out.println("STRATEGIC FIX: Avoiding Small Straight scoring to pursue Large Straight potential");
                return null; // Signal to roll again
            }
        }
        
        if (rawScores.containsKey("Full House") && rawScores.get("Full House") == 25) {
            System.out.println("HIGH VALUE FIX: Selecting Full House with 25 points");
            lastCategory = "Full House";
            lastScore = 25;
            super.fillCategory("Full House", 25);
            return "Full House";
        }
        
        // If we're on our last roll and we have multiple categories available,
        // just pick the one with the highest raw score rather than using expected values
        if (rollCount == 3 && !rawScores.isEmpty()) {
            String bestCategory = null;
            int bestScore = -1;
            
            // Check if Aces has a low score that should be avoided
            boolean hasAces = rawScores.containsKey("Aces");
            int acesScore = hasAces ? rawScores.get("Aces") : 0;
            boolean acesIsLow = acesScore <= 2; // Aces has 2 or fewer points
            
            // Force prioritize Four of a Kind if it's available and has a score > 0
            if (rawScores.containsKey("Four of a Kind") && rawScores.get("Four of a Kind") > 0) {
                System.out.println("PRIORITY: Choosing Four of a Kind with score " + rawScores.get("Four of a Kind"));
                bestCategory = "Four of a Kind";
                bestScore = rawScores.get("Four of a Kind");
            } 
            // Avoid Aces with low score if we have better options
            else if (hasAces && acesIsLow && rawScores.size() > 1) {
                System.out.println("AVOIDING low Aces score: " + acesScore);
                // Find the best score other than Aces
                for (Map.Entry<String, Integer> entry : rawScores.entrySet()) {
                    if (!entry.getKey().equals("Aces") && entry.getValue() > bestScore) {
                        bestScore = entry.getValue();
                        bestCategory = entry.getKey();
                    }
                }
                
                // If we couldn't find anything better, still use Aces
                if (bestCategory == null) {
                    bestCategory = "Aces";
                    bestScore = acesScore;
                }
            } 
            else {
                // Simply pick the category with the highest point value
                for (Map.Entry<String, Integer> entry : rawScores.entrySet()) {
                    System.out.println("Considering category: " + entry.getKey() + " with score: " + entry.getValue());
                    if (entry.getValue() > bestScore) {
                        bestScore = entry.getValue();
                        bestCategory = entry.getKey();
                        System.out.println("  New best: " + bestCategory + " with score: " + bestScore);
                    }
                }
            }
            
            if (bestCategory != null) {
                System.out.println("FINAL SELECTION: " + bestCategory + " with score: " + bestScore);
                lastCategory = bestCategory;
                lastScore = bestScore;
                super.fillCategory(bestCategory, bestScore);
                return bestCategory;
            }
        }
        
        // Normal category selection logic for multiple available categories with strategic considerations
        // This part only runs if we're not on the final roll
        if (!scores.isEmpty() && rollCount < 3) {
            // If we could potentially improve our score and still have rolls left, consider rolling again
            if (rollCount < 3) {
                int[] counts = getCounts(diceValues);
                int maxCount = 0;
                for (int i = 1; i <= 6; i++) {
                    maxCount = Math.max(maxCount, counts[i]);
                }
                
                // If we have Three of a Kind, always try for better unless it's the last roll
                if (maxCount >= 3 && availableCategories.contains("Yahtzee")) {
                    // We have a good chance for Yahtzee or Four of a Kind
                    return null; // Signal we want to roll again
                }
            }
            
            String bestCategory = null;
            int bestScore = -1;
            for (Map.Entry<String, Integer> entry : scores.entrySet()) {
                if (entry.getValue() > bestScore) {
                    bestScore = entry.getValue();
                    bestCategory = entry.getKey();
                }
            }

            if (bestCategory != null) {
                lastCategory = bestCategory;
                lastScore = bestScore;
                super.fillCategory(bestCategory, bestScore);
                return bestCategory;
            }
        }

        // If no scoring categories available and we can roll again
        if (rollCount < 3) {
            return null;  // Signal we want to roll again
        }

        // If we're on our last roll and have no scoring categories, pick the first available
        // (Avoid Aces if possible when we have a low score and other categories are available)
        if (availableCategories.contains("Aces") && 
            rawScores.containsKey("Aces") && 
            rawScores.get("Aces") <= 2 && 
            availableCategories.size() > 1) {
            
            // Try to find a category other than Aces
            for (String category : availableCategories) {
                if (!category.equals("Aces")) {
                    System.out.println("FINAL ROLL: Avoiding Aces with low score, using " + category + " instead");
                    lastCategory = category;
                    lastScore = rawScores.getOrDefault(category, 0);
                    super.fillCategory(category, lastScore);
                    return category;
                }
            }
        }
        
        String firstCategory = availableCategories.get(0);
        lastCategory = firstCategory;
        lastScore = 0;
        super.fillCategory(firstCategory, 0);
        return firstCategory;
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

    public void setRollCount(int count) {
        this.rollCount = count;
    }

    public int getRollCount() {
        return rollCount;
    }

    // Helper method to check if we have a Yahtzee
    private boolean isYahtzee(int[] diceValues) {
        if (diceValues == null || diceValues.length < 5) {
            return false;
        }
        int firstValue = diceValues[0];
        for (int value : diceValues) {
            if (value != firstValue) {
                return false;
            }
        }
        return true;
    }
}