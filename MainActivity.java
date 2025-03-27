package com.example.ccjava;
import java.io.*;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.graphics.Color;
import android.view.Gravity;
import android.graphics.Typeface;
import java.util.HashMap;
import java.util.Map;
import android.util.Log;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    // UI Elements
    private TableLayout scorecardTable;
    private Map<String, TableRow> categoryRows;
    private ImageView[] diceViews;
    private Button rollButton;
    private Button helpButton;
    private Button standButton;
    private Button logButton;
    private Button saveButton;
    private Button loadButton;
    private Button manualInputButton;
    private Button continueButton;
    private TextView scoreTextView;
    private TextView gameFeedbackText;

    // Game State
    private Player humanPlayer;
    private ComputerPlayer computerPlayer;
    private Player currentPlayer;
    private Dice dice;
    private int rollCount;
    private StringBuilder gameLog;
    private int roundNumber;
    private boolean[] selectedDice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showStartupDialog();
    }

    private void showStartupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Welcome to Yahtzee!")
                .setMessage("Choose an option to begin")
                .setPositiveButton("New Game", (dialog, which) -> {
                    setContentView(R.layout.activity_main_grid);
                    initializeGame();
                    setupUI();
                    determineFirstPlayer();
                })
                .setNegativeButton("Load Game", (dialog, which) -> {
                    setContentView(R.layout.activity_main_grid);
                    dice = new Dice();  // Initialize dice
                    diceViews = new ImageView[5];  // Initialize diceViews array
                    humanPlayer = new Player("Human");
                    computerPlayer = new ComputerPlayer();
                    gameLog = new StringBuilder();
                    selectedDice = new boolean[5];
                    setupUI();
                    loadGame();
                })
                .setCancelable(false)
                .show();
    }

    private void initializeGame() {
        dice = new Dice();
        humanPlayer = new Player("Human");
        computerPlayer = new ComputerPlayer();
        Player.resetFilledCategories();
        gameLog = new StringBuilder();
        selectedDice = new boolean[5];
        rollCount = 0;
        roundNumber = 1;
    }

    private void setupUI() {
        // Initialize dice ImageViews
        diceViews = new ImageView[5];
        diceViews[0] = findViewById(R.id.dice1);
        diceViews[1] = findViewById(R.id.dice2);
        diceViews[2] = findViewById(R.id.dice3);
        diceViews[3] = findViewById(R.id.dice4);
        diceViews[4] = findViewById(R.id.dice5);

        // Initialize buttons
        rollButton = findViewById(R.id.rollButton);
        helpButton = findViewById(R.id.helpButton);
        standButton = findViewById(R.id.standButton);
        logButton = findViewById(R.id.logButton);
        saveButton = findViewById(R.id.saveButton);
        loadButton = findViewById(R.id.loadButton);
        manualInputButton = findViewById(R.id.manualInputButton);
        continueButton = findViewById(R.id.continueButton);

        // Initialize text views
        scoreTextView = findViewById(R.id.scoreTextView);
        gameFeedbackText = findViewById(R.id.gameFeedbackText);

        // Initialize scorecard table
        scorecardTable = findViewById(R.id.scorecardTable);
        categoryRows = new HashMap<>();
        initializeScorecard();  // Added this line to initialize the scorecard

        setupClickListeners();
        updateUI();
    }

    private void setupClickListeners() {
        // Dice click listeners
        for (int i = 0; i < diceViews.length; i++) {
            final int diceIndex = i;
            diceViews[i].setOnClickListener(v -> {
                if (currentPlayer == humanPlayer && rollCount > 0 && rollCount < 3) {
                    selectedDice[diceIndex] = !selectedDice[diceIndex];
                    updateDiceSelection();
                }
            });
        }

        // Button click listeners
        rollButton.setOnClickListener(v -> handleRollButton());
        helpButton.setOnClickListener(v -> showHelp());
        standButton.setOnClickListener(v -> handleStandButton());
        logButton.setOnClickListener(v -> showGameLog());
        saveButton.setOnClickListener(v -> saveGame());
        loadButton.setOnClickListener(v -> loadGame());
        manualInputButton.setOnClickListener(v -> showManualDiceInput());
        continueButton.setOnClickListener(v -> handleContinueButton());
    }

    private void handleStandButton() {
        if (currentPlayer != humanPlayer || rollCount == 0) {
            return;  // Only handle stand button for human player after at least one roll
        }

        // End current roll sequence and prompt for category selection
        promptForCategorySelection();

        // Disable roll and stand buttons until next turn
        rollButton.setEnabled(false);
        standButton.setEnabled(false);
        updateUI();
    }

    private void determineFirstPlayer() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Determine First Player");
        builder.setMessage("Each player must roll one die. Highest roll goes first.");
        builder.setPositiveButton("Roll Dice", (dialog, which) -> {
            int humanRoll = new Random().nextInt(6) + 1;
            int computerRoll = new Random().nextInt(6) + 1;

            String message = String.format("Human rolled: %d\nComputer rolled: %d\n\n%s",
                    humanRoll,
                    computerRoll,
                    humanRoll > computerRoll ? "Human plays first!" :
                            computerRoll > humanRoll ? "Computer plays first!" :
                                    "Tie! Rolling again...");

            AlertDialog.Builder resultDialog = new AlertDialog.Builder(this);
            resultDialog.setTitle("Roll Result")
                    .setMessage(message)
                    .setPositiveButton("OK", (resultDlg, resultWhich) -> {
                        if (humanRoll == computerRoll) {
                            determineFirstPlayer();
                        } else {
                            currentPlayer = (humanRoll > computerRoll) ? humanPlayer : computerPlayer;
                            startTurn();
                        }
                    })
                    .show();
        });
        builder.show();
    }

    private void startTurn() {
        rollCount = 0;
        resetDiceSelections();
        if (currentPlayer == computerPlayer) {
            handleComputerTurn();
        } else {
            handleHumanTurn();
        }
        updateUI();
    }

    private void handleHumanTurn() {
        enableHumanTurnUI();
        continueButton.setVisibility(View.VISIBLE);
        continueButton.setText("Start Your Turn");
        showMessage("Click 'Start Your Turn' to begin");
    }

    private void handleRollButton() {
        if (rollCount >= 3) {
            showMessage("Maximum rolls reached. Please select a category.");
            return;
        }

        if (rollCount == 0) {
            resetDiceSelections();
            dice = new Dice();
            dice.rollAll();
        } else {
            dice.roll(selectedDice);
        }

        rollCount++;
        updateDiceImages();
        showAvailableCategories();

        // Add this line:
        standButton.setEnabled(true);

        // Remove automatic category selection after third roll
        if (rollCount == 3) {
            showMessage("Maximum rolls reached. Click 'Stand' to select a category.");
            rollButton.setEnabled(false);
        }
    }

    private void rollDice() {
        // Always ensure we have 5 dice before rolling
        selectedDice = new boolean[Constants.DICE_COUNT];

        if (rollCount == 0) {
            // First roll - roll all dice
            dice.rollAll();
        } else {
            // Subsequent rolls - respect held dice
            dice.roll(selectedDice);
        }

        // Force display update for all dice
        updateDiceImages();
        showAvailableCategories();
    }

    private void handleComputerTurn() {
        disableHumanTurnUI();
        rollCount = 0;
        resetDiceSelections();
        continueButton.setVisibility(View.VISIBLE);
        continueButton.setText("Start Computer Turn");
        showMessage("Click 'Start Computer Turn' to begin Computer's turn");
    }

    private void processComputerNextRoll() {
        if (rollCount < 3 && computerPlayer.shouldRollAgain()) {
            selectedDice = computerPlayer.getSelectedDice();  // Get computer's selections
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Computer Reroll")
                    .setMessage("Choose dice input method for reroll " + (rollCount + 1))
                    .setPositiveButton("Random", (dialog, which) -> {
                        dice.roll(selectedDice);
                        rollCount++;
                        updateDiceImages();
                        String analysis = computerPlayer.analyzeRoll(dice.getValues());
                        showMessage("Computer's Analysis:\n" + analysis);

                        if (rollCount < 3) {
                            new Handler().postDelayed(() -> processComputerNextRoll(), 2000);
                        } else {
                            new Handler().postDelayed(() -> computerSelectCategory(), 1000);
                        }
                    })
                    .setNegativeButton("Manual", (dialog, which) -> {
                        // Store computer's selections before manual input
                        boolean[] keptDice = computerPlayer.getSelectedDice();
                        selectedDice = keptDice;  // Save which dice computer wants to keep
                        manualInputButton.setEnabled(true);
                        showManualDiceInput();
                    })
                    .show();
        } else {
            computerSelectCategory();
        }
    }

    private void computerFirstRoll() {
        dice.rollAll();
        rollCount = 1;
        updateDiceImages();

        String analysis = computerPlayer.analyzeRoll(dice.getValues());
        showMessage("Computer's Analysis:\n" + analysis);

        new Handler().postDelayed(() -> {
            if (computerPlayer.shouldRollAgain()) {
                computerSecondRoll();
            } else {
                computerSelectCategory();
            }
        }, 2000);
    }

    private void computerSecondRoll() {
        // Similar to first roll but for second roll
        dice.roll(computerPlayer.getSelectedDice());
        rollCount = 2;
        updateDiceImages();

        String analysis = computerPlayer.analyzeRoll(dice.getValues());
        showMessage("Computer's Analysis:\n" + analysis);

        new Handler().postDelayed(() -> {
            if (computerPlayer.shouldRollAgain()) {
                computerFinalRoll();
            } else {
                computerSelectCategory();
            }
        }, 2000);
    }

    private void computerFinalRoll() {
        dice.roll(computerPlayer.getSelectedDice());
        rollCount = 3;
        updateDiceImages();

        new Handler().postDelayed(() -> {
            computerSelectCategory();
        }, 1000);
    }

    private void computerSelectCategory() {
        String category = computerPlayer.selectCategory(dice.getValues());

        // If category is null, computer wants to roll again
        if (category == null) {
            if (rollCount < 3) {
                selectedDice = computerPlayer.getSelectedDice();
                showComputerInputDialog();
                return;
            }
            // If we're on our last roll, we have to select a category
            category = computerPlayer.getAvailableCategories().get(0);
        }

        int score = Helper.calculateScore(category, dice.getValues());
        computerPlayer.fillCategory(category, score);
        updateScorecardDisplay(category, score, false, roundNumber);
        showMessage("Computer selects " + category + " for " + score + " points");

        new Handler().postDelayed(() -> {
            endTurn();
        }, 1500);
    }

    private void promptForCategorySelection() {
        List<String> availableCategories = currentPlayer.getAvailableCategories();
        String[] categories = availableCategories.toArray(new String[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Category");
        builder.setItems(categories, (dialog, which) -> {
            String selectedCategory = categories[which];
            int score = Helper.calculateScore(selectedCategory, dice.getValues());
            currentPlayer.fillCategory(selectedCategory, score);
            // Add this line right here, after fillCategory:
            updateScorecardDisplay(selectedCategory, score, currentPlayer == humanPlayer, roundNumber);
            showMessage("Scored " + score + " points in " + selectedCategory);
            endTurn();
        });
        builder.show();
    }

    private void endTurn() {
        updateScoreDisplay();
        logTurn();

        if (isGameOver()) {
            endGame();
        } else {
            switchPlayers();
            startTurn();
        }
    }

    private void switchPlayers() {
        // Simply alternate between players
        currentPlayer = (currentPlayer == humanPlayer) ? computerPlayer : humanPlayer;

        // Only increment round number after both players have gone
        if (currentPlayer == computerPlayer) {  // Start of new round
            roundNumber++;
        }
    }

    private boolean isGameOver() {
        return humanPlayer.allCategoriesFilled() || computerPlayer.allCategoriesFilled();
    }

    private void endGame() {
        int humanScore = humanPlayer.getTotalScore();
        int computerScore = computerPlayer.getTotalScore();
        String winner = humanScore > computerScore ? "Human" :
                humanScore < computerScore ? "Computer" : "Tie";

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Game Over");
        builder.setMessage("Final Scores:\n" +
                "Human: " + humanScore + "\n" +
                "Computer: " + computerScore + "\n\n" +
                (winner.equals("Tie") ? "It's a tie!" : winner + " wins!"));
        builder.setPositiveButton("New Game", (dialog, which) -> {
            initializeGame();
            determineFirstPlayer();
        });
        builder.setNegativeButton("Exit", (dialog, which) -> finish());
        builder.show();
    }

    private void showHelp() {
        if (currentPlayer != humanPlayer) return;

        String helpText = Helper.getAdvice(dice.getValues(),
                selectedDice,
                rollCount,
                humanPlayer.getAvailableCategories());
        showMessage("Help", helpText);
    }

    private void showGameLog() {
        showMessage("Game Log", gameLog.toString());
    }

    private void logTurn() {
        gameLog.append("Round ").append(roundNumber).append(": ")
                .append(currentPlayer == humanPlayer ? "Human" : "Computer")
                .append(" scored ").append(currentPlayer.getLastScore())
                .append(" points in ").append(currentPlayer.getLastCategory())
                .append("\n");
    }

    private void updateUI() {
        updateDiceImages();
        updateScoreDisplay();
        updateButtonStates();
    }

    private void updateDiceImages() {
        int[] values = dice.getValues();
        // Always show and update all 5 dice
        for (int i = 0; i < Constants.DICE_COUNT; i++) {
            diceViews[i].setVisibility(View.VISIBLE);
            int resourceId = getResources().getIdentifier(
                    "dice_" + values[i], "drawable", getPackageName());
            diceViews[i].setImageResource(resourceId);
            diceViews[i].setAlpha(selectedDice[i] ? 0.5f : 1.0f);
        }
    }

    private void updateScoreDisplay() {
        scoreTextView.setText(String.format("Human: %d | Computer: %d",
                humanPlayer.getTotalScore(), computerPlayer.getTotalScore()));
    }

    private void updateButtonStates() {
        boolean isHumanTurn = currentPlayer == humanPlayer;

        // Handle roll and stand buttons
        if (isHumanTurn) {
            if (rollCount == 0) {
                // At start of turn, roll button should be enabled after clicking continue
                rollButton.setEnabled(true);
            } else {
                // After first roll, only enable if under 3 rolls
                rollButton.setEnabled(rollCount < 3);
            }
        } else {
            rollButton.setEnabled(false);
        }

        standButton.setEnabled(isHumanTurn && rollCount > 0);
        helpButton.setEnabled(isHumanTurn);
        manualInputButton.setEnabled(isHumanTurn);

        // Handle continue button visibility
        if (isHumanTurn) {
            // Show continue button only at start of human turn
            if (rollCount == 0 && continueButton.getVisibility() == View.VISIBLE) {
                return;
            }
            continueButton.setVisibility(View.GONE);
        } else if (continueButton.getVisibility() == View.VISIBLE) {
            // Keep continue button visible during computer turn if it's already showing
            return;
        }
        continueButton.setVisibility(View.GONE);
    }

    private void showMessage(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showMessage(String message) {
        gameFeedbackText.setText(message);
    }

    private void enableHumanTurnUI() {
        rollButton.setEnabled(true);
        helpButton.setEnabled(true);
        manualInputButton.setEnabled(true);
    }

    private void disableHumanTurnUI() {
        rollButton.setEnabled(false);
        helpButton.setEnabled(false);
        standButton.setEnabled(false);
    }

    private void resetDiceSelections() {
        for (int i = 0; i < selectedDice.length; i++) {
            selectedDice[i] = false;
        }
        updateDiceSelection();
    }

    private void updateDiceSelection() {
        for (int i = 0; i < diceViews.length; i++) {
            diceViews[i].setAlpha(selectedDice[i] ? 0.5f : 1.0f);
        }
    }

    private void showAvailableCategories() {
        StringBuilder categories = new StringBuilder("Available Categories:\n");
        for (String category : currentPlayer.getAvailableCategories()) {
            int potentialScore = Helper.calculateScore(category, dice.getValues());
            categories.append(category)
                    .append(": ")
                    .append(potentialScore)
                    .append(" points\n");
        }
        gameFeedbackText.setText(categories.toString());
    }

    private void saveGame() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText input = new EditText(this);
        input.setHint("Enter save file name");

        builder.setTitle("Save Game")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String fileName = input.getText().toString();
                    if (!fileName.endsWith(".dat")) {
                        fileName += ".dat";
                    }

                    try {
                        File file = new File(getFilesDir(), fileName);
                        BufferedWriter writer = new BufferedWriter(new FileWriter(file));

                        // Write round number
                        writer.write("Round: " + roundNumber + "\n\n");
                        writer.write("Scorecard:\n");

                        // Write scorecard data
                        for (String category : Constants.CATEGORIES) {
                            int humanScore = humanPlayer.getScore(category);
                            int computerScore = computerPlayer.getScore(category);

                            if (humanPlayer.isCategoryFilled(category)) {
                                writer.write(humanScore + " Human " + roundNumber + "\n");
                            } else if (computerPlayer.isCategoryFilled(category)) {
                                writer.write(computerScore + " Computer " + roundNumber + "\n");
                            } else {
                                writer.write("0\n");
                            }
                        }

                        writer.close();
                        showMessage("Game saved successfully!");

                    } catch (IOException e) {
                        showMessage("Error saving game: " + e.getMessage());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadGame() {
        File[] saves = getFilesDir().listFiles((dir, name) -> name.endsWith(".dat"));
        if (saves == null || saves.length == 0) {
            showMessage("No saved games found");
            return;
        }

        String[] fileNames = new String[saves.length];
        for (int i = 0; i < saves.length; i++) {
            fileNames[i] = saves[i].getName();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Save File")
                .setItems(fileNames, (dialog, which) -> {
                    try {
                        BufferedReader reader = new BufferedReader(new FileReader(saves[which]));

                        // Read round number
                        String roundLine = reader.readLine();
                        roundNumber = Integer.parseInt(roundLine.split(": ")[1]);

                        // Skip empty line and "Scorecard:" line
                        reader.readLine();
                        reader.readLine();

                        // Reset players
                        humanPlayer = new Player("Human");
                        computerPlayer = new ComputerPlayer();
                        Player.resetFilledCategories();
                        gameLog = new StringBuilder();

                        // Read scorecard data and update UI
                        int categoryIndex = 0;
                        for (String category : Constants.CATEGORIES) {
                            String line = reader.readLine();
                            if (line == null) break;

                            String[] parts = line.split(" ");
                            if (parts.length > 1) {
                                int score = Integer.parseInt(parts[0]);
                                int round = Integer.parseInt(parts[2]);
                                if (parts[1].equals("Human")) {
                                    humanPlayer.fillCategory(category, score);
                                    updateScorecardDisplay(category, score, true, round);
                                    gameLog.append("Round ").append(round).append(": Human scored ")  // Add these
                                            .append(score).append(" points in ").append(category)      // three
                                            .append("\n");                                            // lines
                                } else if (parts[1].equals("Computer")) {
                                    computerPlayer.fillCategory(category, score);
                                    updateScorecardDisplay(category, score, false, round);
                                    gameLog.append("Round ").append(round).append(": Computer scored ")  // Add these
                                            .append(score).append(" points in ").append(category)         // three
                                            .append("\n");                                               // lines
                                }
                            }
                            categoryIndex++;
                        }


                        reader.close();
                        updateUI();

                        // Determine first player based on scores
                        int humanScore = humanPlayer.getTotalScore();
                        int computerScore = computerPlayer.getTotalScore();

                        if (computerScore < humanScore) {
                            currentPlayer = computerPlayer;
                            showMessage("Computer plays first with score " + computerScore + " vs Human " + humanScore);
                            handleComputerTurn();
                        } else if (humanScore < computerScore) {
                            currentPlayer = humanPlayer;
                            showMessage("Human plays first with score " + humanScore + " vs Computer " + computerScore);
                            enableHumanTurnUI();
                        } else {
                            determineFirstPlayer();
                        }

                        updateButtonStates();

                    } catch (IOException e) {
                        showMessage("Error loading game: " + e.getMessage());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showManualDiceInput() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.manual_input_dialog, null);

        // Track selected values for each die
        int[] selectedValues = new int[5];

        // Setup click listeners for all dice images
        for (int diePosition = 1; diePosition <= 5; diePosition++) {
            for (int value = 1; value <= 6; value++) {
                final int diePos = diePosition;
                final int dieValue = value;

                int resId = getResources().getIdentifier(
                        "die" + diePosition + "_" + value,
                        "id",
                        getPackageName()
                );

                ImageView dieImage = dialogView.findViewById(resId);
                if (dieImage != null) {
                    dieImage.setOnClickListener(v -> {
                        // Reset opacity for all dice in this row
                        for (int i = 1; i <= 6; i++) {
                            int resetId = getResources().getIdentifier(
                                    "die" + diePos + "_" + i,
                                    "id",
                                    getPackageName()
                            );
                            ImageView resetImage = dialogView.findViewById(resetId);
                            if (resetImage != null) {
                                resetImage.setAlpha(0.5f);
                            }
                        }
                        // Highlight selected die
                        dieImage.setAlpha(1.0f);
                        selectedValues[diePos - 1] = dieValue;
                    });

                    // If die is already set (in a reroll), disable selection
                    if (rollCount > 0 && selectedDice[diePosition - 1]) {
                        dieImage.setEnabled(false);
                        if (dice.getValues()[diePosition - 1] == value) {
                            dieImage.setAlpha(1.0f);
                            selectedValues[diePosition - 1] = value;
                        } else {
                            dieImage.setAlpha(0.2f);
                        }
                    } else {
                        dieImage.setAlpha(0.5f);
                    }
                }
            }
        }

        builder.setView(dialogView)
                .setTitle("Select Dice Values")
                .setPositiveButton("OK", (dialog, which) -> {
                    // Validate all dice have been selected
                    boolean allSelected = true;
                    for (int i = 0; i < 5; i++) {
                        if (selectedValues[i] == 0) {
                            allSelected = false;
                            break;
                        }
                    }

                    if (allSelected) {
                        dice.setManualValues(selectedValues);
                        rollCount++;
                        updateUI();
                        showAvailableCategories();

                        if (currentPlayer == computerPlayer) {
                            String analysis = computerPlayer.analyzeRoll(dice.getValues());
                            showMessage("Computer's Analysis:\n" + analysis);

                            if (rollCount < 3) {
                                continueButton.setText("Continue to Roll " + (rollCount + 1));
                                continueButton.setVisibility(View.VISIBLE);
                            } else {
                                continueButton.setText("Select Category");
                                continueButton.setVisibility(View.VISIBLE);
                            }
                        }
                    } else {
                        showMessage("Please select a value for each die");
                        showManualDiceInput();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleContinueButton() {
        if (currentPlayer == computerPlayer) {
            if (rollCount == 0) {
                showComputerInputDialog();
            } else if (rollCount < 3 && computerPlayer.shouldRollAgain()) {
                selectedDice = computerPlayer.getSelectedDice();
                showComputerInputDialog();
            } else {
                computerSelectCategory();
            }
        } else {
            // Human player turn
            if (rollCount == 0) {
                // First roll
                resetDiceSelections();
                rollButton.setEnabled(true);  // Enable roll button
                helpButton.setEnabled(true);  // Enable help button
                manualInputButton.setEnabled(true);  // Enable manual input
                continueButton.setVisibility(View.GONE);
            }
        }
    }

    private void showComputerInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Computer Turn")
                .setMessage("Choose dice input method for roll " + (rollCount + 1))
                .setPositiveButton("Random", (dialog, which) -> {
                    if (rollCount == 0) {
                        dice = new Dice();
                        dice.rollAll();
                    } else {
                        dice.roll(selectedDice);
                    }
                    rollCount++;
                    updateDiceImages();
                    String analysis = computerPlayer.analyzeRoll(dice.getValues());
                    showMessage("Computer's Analysis:\n" + analysis);

                    if (rollCount < 3 && computerPlayer.shouldRollAgain()) {
                        continueButton.setText("Continue to Roll " + (rollCount + 1));
                        continueButton.setVisibility(View.VISIBLE);
                    } else {
                        continueButton.setText("Select Category");
                        continueButton.setVisibility(View.VISIBLE);
                    }
                })
                .setNegativeButton("Manual", (dialog, which) -> {
                    manualInputButton.setEnabled(true);
                    showManualDiceInput();
                })
                .show();
    }

    private void initializeScorecard() {
        scorecardTable = findViewById(R.id.scorecardTable);
        if (scorecardTable == null) {
            Log.e("MainActivity", "Failed to find scorecardTable!");
            return;
        }
        categoryRows = new HashMap<>();

        // First ensure we have our header row
        TableRow headerRow = (TableRow) scorecardTable.getChildAt(0);
        if (headerRow == null) {
            // Header doesn't exist, create it
            headerRow = new TableRow(this);
            headerRow.setBackgroundColor(Color.parseColor("#CCCCCC"));

            String[] headers = {"Category", "Human", "Computer", "Round"};
            float[] weights = {2f, 1f, 1f, 3.0f};  // Doubled Round column weight

            for (int i = 0; i < headers.length; i++) {
                TextView headerView = new TextView(this);
                headerView.setText(headers[i]);
                headerView.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, weights[i]));
                headerView.setPadding(3, 3, 3, 3);
                headerView.setGravity(Gravity.CENTER);
                headerView.setTypeface(null, Typeface.BOLD);
                headerRow.addView(headerView);
            }
            scorecardTable.addView(headerRow);
        }

        // Create a row for each category
        for (String category : Constants.CATEGORIES) {
            TableRow row = new TableRow(this);

            TextView categoryView = new TextView(this);
            categoryView.setText(category);
            categoryView.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 2f));
            categoryView.setPadding(3, 3, 3, 3);

            TextView humanScoreView = new TextView(this);
            humanScoreView.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
            humanScoreView.setGravity(Gravity.CENTER);
            humanScoreView.setPadding(3, 3, 3, 3);
            humanScoreView.setText("-");

            TextView computerScoreView = new TextView(this);
            computerScoreView.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
            computerScoreView.setGravity(Gravity.CENTER);
            computerScoreView.setPadding(3, 3, 3, 3);
            computerScoreView.setText("-");

            TextView roundView = new TextView(this);
            roundView.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 3.0f));  // Doubled Round column weight
            roundView.setGravity(Gravity.CENTER);
            roundView.setPadding(3, 3, 3, 3);
            roundView.setText("-");

            row.addView(categoryView);
            row.addView(humanScoreView);
            row.addView(computerScoreView);
            row.addView(roundView);

            if (scorecardTable.getChildCount() % 2 == 0) {
                row.setBackgroundColor(Color.parseColor("#F5F5F5"));
            }

            scorecardTable.addView(row);
            categoryRows.put(category, row);
        }

        addTotalScoreRow();
    }

    private void addTotalScoreRow() {
        TableRow totalRow = new TableRow(this);
        totalRow.setBackgroundColor(Color.parseColor("#E0E0E0"));

        TextView totalLabel = new TextView(this);
        totalLabel.setText("TOTAL");
        totalLabel.setTypeface(null, Typeface.BOLD);
        totalLabel.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 2f));
        totalLabel.setPadding(3, 3, 3, 3);

        TextView humanTotal = new TextView(this);
        humanTotal.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
        humanTotal.setGravity(Gravity.CENTER);
        humanTotal.setTypeface(null, Typeface.BOLD);
        humanTotal.setPadding(3, 3, 3, 3);
        humanTotal.setText("0");

        TextView computerTotal = new TextView(this);
        computerTotal.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
        computerTotal.setGravity(Gravity.CENTER);
        computerTotal.setTypeface(null, Typeface.BOLD);
        computerTotal.setPadding(3, 3, 3, 3);
        computerTotal.setText("0");

        TextView empty = new TextView(this);
        empty.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 3.0f));  // Doubled Round column weight

        totalRow.addView(totalLabel);
        totalRow.addView(humanTotal);
        totalRow.addView(computerTotal);
        totalRow.addView(empty);

        scorecardTable.addView(totalRow);
    }

    private void updateScorecardDisplay(String category, int score, boolean isHuman, int round) {
        TableRow row = categoryRows.get(category);
        if (row != null) {
            TextView scoreView = (TextView) row.getChildAt(isHuman ? 1 : 2);
            scoreView.setText(String.valueOf(score));
            TextView roundView = (TextView) row.getChildAt(3);
            roundView.setText(String.valueOf(round));
            updateTotalScores();
        }
    }

    private void updateTotalScores() {
        TableRow totalRow = (TableRow) scorecardTable.getChildAt(scorecardTable.getChildCount() - 1);
        TextView humanTotal = (TextView) totalRow.getChildAt(1);
        TextView computerTotal = (TextView) totalRow.getChildAt(2);

        humanTotal.setText(String.valueOf(humanPlayer.getTotalScore()));
        computerTotal.setText(String.valueOf(computerPlayer.getTotalScore()));
    }
}
