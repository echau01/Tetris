package ui;

import exceptions.CorruptedFileException;
import model.Game;
import model.Scoreboard;
import model.ScoreboardEntry;
import model.pieces.*;
import persistence.ScoreboardEntryFileReader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

// Represents a Tetris application
public class TetrisApplication {
    // The file path of the file containing all saved scoreboard entries.
    private static final String SCOREBOARD_ENTRIES_FILE_PATH = "./data/scoreboardEntries.txt";

    private Scanner scanner;
    private Random random;
    private boolean running;
    private Scoreboard tempScoreboard;
    private Game game;

    // This field is true if the game has ended and the user has added
    // their score to the scoreboard.
    private boolean userAddedScoreToScoreboard;

    // This field is true if the game has just ended (i.e. the user has just topped
    // out and is seeing the game over commands for the first time after the game),
    // false otherwise.
    private boolean gameJustEnded;

    // Code for the initialization of the class and the reading in and
    // processing of input is based on the TellerApp class in the TellerApp repository.
    // https://github.students.cs.ubc.ca/CPSC210/TellerApp/blob/master/src/main/ca/ubc/cpsc210/bank/ui/TellerApp.java

    // EFFECTS: makes and starts a new tetris application
    public TetrisApplication() {
        start();
    }

    // MODIFIES: this
    // EFFECTS: starts the application
    private void start() {
        initFields();

        while (running) {
            if (!gameJustEnded) {
                printBoard();
                if (!game.isGameOver()) {
                    System.out.println("Score: " + game.getScore());
                    System.out.println("Lines cleared: " + game.getLinesCleared());
                    printNextPiece();
                } else {
                    System.out.println("Game over!");
                    gameJustEnded = true;
                    System.out.println("Final score: " + game.getScore());
                    System.out.println("Final lines cleared: " + game.getLinesCleared());
                }
            }
            displayCommands();
            handleUserInput();
        }

        // I got the idea to close the scanner from the module C7 lecture lab.
        // See the end of the processCommands method at:
        // https://github.students.cs.ubc.ca/CPSC210/CourseRecommender/blob/master/src/main/ca/ubc/cpsc210/courseRecommender/CourseRecommenderApp.java
        scanner.close();
    }

    // EFFECTS: initializes all the fields of this object to their default values
    private void initFields() {
        scanner = new Scanner(System.in);
        random = new Random();
        running = true;
        tempScoreboard = new Scoreboard();
        game = new Game(random.nextInt());
        userAddedScoreToScoreboard = false;
        gameJustEnded = false;
    }

    // EFFECTS: prints a list of commands the user can currently perform
    private void displayCommands() {
        System.out.println("Type one of the following commands:");
        if (game.isGameOver()) {
            System.out.println("replay -> start a new game");
            if (!userAddedScoreToScoreboard) {
                System.out.println("add -> add your score to a temporary scoreboard.");
            }
            System.out.println("view-temp-scores -> view temporary scoreboard");
            System.out.println("save-temp-scores -> permanently save all entries on the temporary scoreboard to file");
            System.out.println("view-saved-scores -> view all permanently-saved scoreboard entries");
        } else {
            System.out.println("left -> move piece left one column");
            System.out.println("right -> move piece right one column");
            System.out.println("rotate -> rotate piece 90 degrees clockwise");
            System.out.println("down -> move piece down one row");
            System.out.println("drop -> drop piece as far down as possible");
            System.out.println("n -> do nothing; let game advance on its own");
        }
        System.out.println("q-save -> quit and save temporary scoreboard to file");
        System.out.println("q-no-save -> quit without saving temporary scoreboard to file");
    }

    // EFFECTS: handle user input in general
    private void handleUserInput() {
        String input = scanner.next();
        scanner.nextLine();
        if (input.equalsIgnoreCase("q-save")) {
            saveTempScoreboard();
            running = false;
        } else if (input.equalsIgnoreCase("q-no-save")) {
            running = false;
        } else {
            if (game.isGameOver()) {
                handleUserInputGameOver(input);
            } else {
                handleUserInputGameNotOver(input);
            }
        }
    }

    // EFFECTS: prints the tetris board onto the console, with X's marking tile locations
    private void printBoard() {
        for (int i = 0; i < Game.WIDTH + 2; i++) {
            System.out.print("-");
        }
        System.out.println();
        List<ArrayList<Boolean>> board = game.getBoard();
        for (int r = 0; r < Game.HEIGHT; r++) {
            System.out.print("|");
            for (int c = 0; c < Game.WIDTH; c++) {
                if (board.get(r).get(c)) {
                    System.out.print("X");
                } else {
                    System.out.print(" ");
                }
            }
            System.out.println("|");
        }
        for (int i = 0; i < Game.WIDTH + 2; i++) {
            System.out.print("-");
        }
        System.out.println();
    }

    // EFFECTS: handles given user input for when the game is over
    private void handleUserInputGameOver(String input) {
        if (input.equalsIgnoreCase("replay")) {
            game = new Game(random.nextInt());
            userAddedScoreToScoreboard = false;
            gameJustEnded = false;
        } else if (!userAddedScoreToScoreboard && input.equalsIgnoreCase("add")) {
            userAddedScoreToScoreboard = addScoreToTempScoreboard();
        } else if (input.equalsIgnoreCase("view-temp-scores")) {
            printTempScoreboard();
        } else if (input.equalsIgnoreCase("save-temp-scores")) {
            saveTempScoreboard();
        } else if (input.equalsIgnoreCase("view-saved-scores")) {
            printSavedScores();
        } else {
            System.out.println("Command not recognized.\n");
        }
    }

    // EFFECTS: handles given user input for when the game is not over
    private void handleUserInputGameNotOver(String input) {
        if (input.equalsIgnoreCase("left")) {
            game.getActivePiece().moveLeft();
        } else if (input.equalsIgnoreCase("right")) {
            game.getActivePiece().moveRight();
        } else if (input.equalsIgnoreCase("rotate")) {
            game.getActivePiece().rotate();
        } else if (input.equalsIgnoreCase("down")) {
            game.getActivePiece().moveDown();
        } else if (input.equalsIgnoreCase("drop")) {
            boolean movedDown = game.getActivePiece().moveDown();
            while (movedDown) {
                movedDown = game.getActivePiece().moveDown();
            }
            game.update();
        } else if (input.equalsIgnoreCase("n")) {
            game.update();
        } else {
            System.out.println("Command not recognized.\n");
        }
    }

    // MODIFIES: this
    // EFFECTS: guides the user in adding an entry to the temporary scoreboard. Returns true if
    //          score was successfully added, false otherwise.
    private boolean addScoreToTempScoreboard() {
        if (!game.isGameOver()) {
            System.err.println("Error: the game is not over.\n");
            return false;
        }
        System.out.println("Please enter your name:");
        String name = scanner.nextLine();

        ScoreboardEntry entry = new ScoreboardEntry(game.getScore(), name, game.getLinesCleared());
        tempScoreboard.add(entry);

        System.out.println("Your score was successfully added to the scoreboard!\n");
        return true;
    }

    // EFFECTS: prints all entries on the temporary scoreboard sorted from greatest to least
    private void printTempScoreboard() {
        if (tempScoreboard.getSize() == 0) {
            System.out.println("You do not have any scores on the temporary scoreboard.\n");
            return;
        }
        System.out.println("Temporary scoreboard entries:");
        printScoreboard(tempScoreboard);
        System.out.println();
    }

    // EFFECTS: saves all entries on the temporary scoreboard to the file at SCOREBOARD_ENTRIES_FILE_PATH.
    //          Clears the temporary scoreboard.
    private void saveTempScoreboard() {
        // The printed messages are based on the messages found in the saveAccounts() method of
        // the TellerApp class of the TellerApp repository.
        // https://github.students.cs.ubc.ca/CPSC210/TellerApp/blob/master/src/main/ca/ubc/cpsc210/bank/ui/TellerApp.java
        List<ScoreboardEntry> entries = tempScoreboard.getEntries();
        if (entries.size() == 0) {
            System.out.println("There are no scores on the temporary scoreboard to save.\n");
            return;
        }
        try {
            for (ScoreboardEntry entry : entries) {
                entry.appendTo(new File(SCOREBOARD_ENTRIES_FILE_PATH));
            }
            System.out.println("Saved all entries on temporary scoreboard to file "
                    + SCOREBOARD_ENTRIES_FILE_PATH + "\n");
            entries.clear();
        } catch (IOException e) {
            System.err.println("Could not save scoreboard to file " + SCOREBOARD_ENTRIES_FILE_PATH + "\n");
        }
    }

    // EFFECTS: prints all permanently-saved scoreboard entries in the file at SCOREBOARD_ENTRIES_FILE_PATH,
    //          sorted from greatest to least
    private void printSavedScores() {
        // The printed messages are based on the messages found in the saveAccounts() method of
        // the TellerApp class of the TellerApp repository.
        // https://github.students.cs.ubc.ca/CPSC210/TellerApp/blob/master/src/main/ca/ubc/cpsc210/bank/ui/TellerApp.java

        File file = new File(SCOREBOARD_ENTRIES_FILE_PATH);
        try {
            file.createNewFile();
        } catch (IOException e) {
            System.err.println("An error occurred when trying to create file " + SCOREBOARD_ENTRIES_FILE_PATH + "\n");
        }

        try {
            Scoreboard savedScoresAsScoreboard = ScoreboardEntryFileReader.readInScoreboardEntries(file);
            if (savedScoresAsScoreboard.getSize() == 0) {
                System.out.println("You do not have any permanently-saved scores.\n");
                return;
            }
            System.out.println("Permanently-saved scoreboard entries:");
            printScoreboard(savedScoresAsScoreboard);
            System.out.println();
        } catch (CorruptedFileException e) {
            System.err.println("Scoreboard entry file " + SCOREBOARD_ENTRIES_FILE_PATH + " is corrupted.\n");
            System.err.println(e.getMessage());
        } catch (IOException e) {
            System.err.println("Could not read from scoreboard entry file " + SCOREBOARD_ENTRIES_FILE_PATH + "\n");
        }
    }

    // EFFECTS: prints a message telling the user what the next piece is.
    //          Prints an error message if the game has ended -- this method should
    //          not be called in that situation.
    private void printNextPiece() {
        if (game.isGameOver()) {
            System.err.println("Error: the game is over.");
            return;
        }
        Piece nextPiece = game.getNextPiece();
        System.out.println("The next piece is a(n) " + getPieceName(nextPiece) + ".");
    }

    // EFFECTS: returns the name of the given piece in the form "I/J/L/O/S/T/Z piece".
    private String getPieceName(Piece piece) {
        if (piece instanceof IPiece) {
            return "I piece";
        } else if (piece instanceof JPiece) {
            return "J piece";
        } else if (piece instanceof LPiece) {
            return "L piece";
        } else if (piece instanceof OPiece) {
            return "O piece";
        } else if (piece instanceof SPiece) {
            return "S piece";
        } else if (piece instanceof TPiece) {
            return "T piece";
        } else {
            return "Z piece";
        }
    }

    // EFFECTS: prints all entries on the given scoreboard sorted from greatest to least
    private void printScoreboard(Scoreboard scoreboard) {
        List<ScoreboardEntry> sortedEntries = scoreboard.getSortedEntries();
        for (int i = 0; i < sortedEntries.size(); i++) {
            ScoreboardEntry entry = sortedEntries.get(i);
            String output = (i + 1) + ". " + entry.toString();
            System.out.println(output);
        }
    }
}
