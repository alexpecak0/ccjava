package com.example.ccjava;

import java.util.Random;

public class Dice {
    private int[] values;
    private boolean[] held;
    private Random random;
    private boolean manualMode;

    public Dice() {
        // Initialize with 5 dice
        values = new int[Constants.DICE_COUNT];
        held = new boolean[Constants.DICE_COUNT];
        random = new Random();
        manualMode = false;

        // Initialize all dice to value 1
        for (int i = 0; i < Constants.DICE_COUNT; i++) {
            values[i] = 1;
            held[i] = false;
        }
    }

    public int rollSingleDie() {
        return random.nextInt(Constants.DIE_MAX_VALUE) + 1;
    }

    public void rollAll() {
        // Always roll all 5 dice
        for (int i = 0; i < Constants.DICE_COUNT; i++) {
            values[i] = random.nextInt(6) + 1;
            held[i] = false;
        }
    }
    
    public void roll(boolean[] diceToKeep) {
        // If we were in manual mode, clear the flag but CONTINUE to roll
        if (manualMode) {
            manualMode = false;
            // Don't return - continue processing to roll dice
        }
        
        // Roll dice that are NOT selected (not held)
        // In the game, selectedDice represents dice to KEEP (not reroll)
        for (int i = 0; i < Constants.DICE_COUNT; i++) {
            if (!diceToKeep[i]) {  // If dice is NOT selected to keep, then roll it
                values[i] = random.nextInt(Constants.DIE_MAX_VALUE) + 1;
            }
            // Update held state to match selection
            held[i] = diceToKeep[i];
        }
    }

    public boolean setManualValues(int[] newValues) {
        if (newValues.length != Constants.DICE_COUNT) {
            return false;
        }

        // Validate all values first
        for (int value : newValues) {
            if (value < 1 || value > Constants.DIE_MAX_VALUE) {
                return false;
            }
        }

        // All values valid, now set them
        for (int i = 0; i < Constants.DICE_COUNT; i++) {
            values[i] = newValues[i];
        }
        manualMode = true;
        return true;
    }

    public int[] getValues() {
        return values.clone();
    }

    public boolean[] getHeld() {
        return held.clone();
    }

    public int[] getHeldValues() {
        int count = 0;
        for (boolean isHeld : held) {
            if (isHeld) count++;
        }

        int[] heldValues = new int[count];
        int index = 0;
        for (int i = 0; i < Constants.DICE_COUNT; i++) {
            if (held[i]) {
                heldValues[index++] = values[i];
            }
        }
        return heldValues;
    }

    public void setHeld(int index, boolean isHeld) {
        if (index >= 0 && index < Constants.DICE_COUNT) {
            held[index] = isHeld;
        }
    }

    public void reset() {
        for (int i = 0; i < Constants.DICE_COUNT; i++) {
            values[i] = 1;
            held[i] = false;
        }
        manualMode = false;
    }

    public int countValue(int value) {
        int count = 0;
        for (int v : values) {
            if (v == value) count++;
        }
        return count;
    }

    public int sum() {
        int total = 0;
        for (int value : values) {
            total += value;
        }
        return total;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Dice values: ");
        for (int i = 0; i < Constants.DICE_COUNT; i++) {
            sb.append(values[i]);
            if (held[i]) sb.append("*");
            sb.append(" ");
        }
        return sb.toString();
    }
}