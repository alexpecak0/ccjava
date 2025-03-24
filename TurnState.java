package com.example.ccjava;

public enum TurnState {
    INITIAL_TOSS,          // For determining first player with dice roll
    FIRST_ROLL,           // First roll of 5 dice
    SELECT_DICE_FIRST,    // Select dice after first roll
    SECOND_ROLL,          // Second roll of non-selected dice
    SELECT_DICE_SECOND,   // Select dice after second roll
    THIRD_ROLL,           // Final roll of non-selected dice
    SELECT_CATEGORY,      // Select category to fill
    TURN_END             // End of turn
}

/*
The turn flow according to rubric:

1. INITIAL_TOSS
   - Each player rolls one die
   - Higher number goes first
   - Repeat if tie

2. FIRST_ROLL
   - Roll all 5 dice
   - Must announce:
     * Available categories
     * Categories pursuing
     * Explanation (computer only)

3. SELECT_DICE_FIRST
   - Select dice to keep/reroll
   - Must announce:
     * Which dice are set aside
     * Whether to roll again or stand

4. SECOND_ROLL
   - Roll non-selected dice
   - Must announce:
     * Available categories
     * Categories pursuing
     * Explanation (computer only)

5. SELECT_DICE_SECOND
   - Select dice to keep/reroll
   - Must announce:
     * Which dice are set aside
     * Whether to roll again or stand

6. THIRD_ROLL
   - Final roll of non-selected dice
   - Cannot roll dice set aside in first or second roll

7. SELECT_CATEGORY
   - Must select category to fill
   - Update scorecard with:
     * Name
     * Points
     * Round number

8. TURN_END
   - Switch to other player
   - If last category filled, end game
*/