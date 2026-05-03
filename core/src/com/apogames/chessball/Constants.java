package com.apogames.chessball;

import com.badlogic.gdx.graphics.g2d.GlyphLayout;

import java.util.Locale;

public class Constants {

    public static String REGION;

    public static final GlyphLayout glyphLayout = new GlyphLayout();

    public static final String USERLEVELS_GETPHP = "";
    public static final String USERLEVELS_SAVEPHP = "https://www.apo-games.de/chessball/save_solution.php";

    public static final String PROPERTY_NAME = "ChessBall";
    public static final String PROGRAM_NAME = "=== " + PROPERTY_NAME + " ===";
    public static final double VERSION = 0.02;

    public static boolean FPS = false;

    public static final int MAX_SCALE = 1;
    public static final int GAME_WIDTH = 480 * MAX_SCALE;
    public static final int GAME_HEIGHT = 800 * MAX_SCALE;

    public static final int BOARD_COLS = 9;
    public static final int BOARD_ROWS = 15;
    public static final int GOAL_WHITE_Y = 0;
    public static final int GOAL_BLACK_Y = BOARD_ROWS - 1;

    public static final int NEEDED_GOALS_TO_WIN = 3;
    public static final int MAX_PASSES = 3;
    public static final int MAX_MOVES = 1;
    public static final int MAX_PIECE_CHANGE = MAX_MOVES;

    public static final String DEMO_GETPHP  = "https://www.apo-games.de/chessball/get_demo_random.php";
    public static final String DEMO_SAVEPHP = "https://www.apo-games.de/chessball/save_demo.php";

    public static final int TEXT_TIME_IN_MILLISECONDS = 1500;
    public static final int WAIT_UNTIL_MOVE_IN_MILLISECONDS = 500;

    public static float[] COLOR_CLEAR = new float[]{184f / 255f, 224f / 255f, 255f / 255f, 1f};

    public static final float[] COLOR_WHITE = new float[]{255f / 255f, 255f / 255f, 255f / 255f, 1f};
    public static final float[] COLOR_BROWN = new float[]{60f / 255f, 40f / 255f, 30f / 255f, 1f};
    public static final float[] COLOR_SELECT = new float[]{137f / 255f, 120f / 255f, 90f / 255f, 1f};
    public static final float[] COLOR_BLUE_BRIGHT = new float[]{128f / 255f, 128f / 255f, 255f / 255f, 1f};
    public static final float[] COLOR_BLUE = new float[]{0f / 255f, 0f / 255f, 255f / 255f, 1f};
    public static final float[] COLOR_BLUE_DARK = new float[]{10f / 255f, 40f / 255f, 105f / 255f, 1f};
    public static final float[] COLOR_GREEN_BRIGHT = new float[]{128f / 255f, 255f / 255f, 128f / 255f, 1f};
    public static final float[] COLOR_GREEN = new float[]{0f / 255f, 255f / 255f, 0f / 255f, 1f};
    public static final float[] COLOR_RED_LIGHT = new float[]{255f / 255f, 128f / 255f, 128f / 255f, 1f};
    public static final float[] COLOR_RED = new float[]{255f / 255f, 0f / 255f, 0f / 255f, 1f};
    public static final float[] COLOR_RED_DARK = new float[]{90f / 255f, 0f / 255f, 0f / 255f, 1f};
    public static final float[] COLOR_YELLOW = new float[]{255f / 255f, 255f / 255f, 0f / 255f, 1f};
    public static final float[] COLOR_ORANGE = new float[]{242f / 255f, 101f / 255f, 34f / 255f, 1f};
    public static final float[] COLOR_BLACK = new float[]{0f / 255f, 0f / 255f, 0f / 255f, 1f};
    public static final float[] COLOR_GREY = new float[]{99f / 255f, 99f / 255f, 99f / 255f, 1f};
    public static final float[] COLOR_GREY_BRIGHT = new float[]{199f / 255f, 199f / 255f, 199f / 255f, 1f};
    public static final float[] COLOR_GRAY = COLOR_GREY;
    public static final float[] COLOR_GRAY_BRIGHT = COLOR_GREY_BRIGHT;
    public static final float[] COLOR_GRAY_BLUE = new float[]{30f / 255f, 84f / 255f, 139f / 255f, 1f};
    public static final float[] COLOR_PURPLE = new float[]{56f / 255f, 51f / 255f, 98f / 255f, 1f};
    public static final float[] COLOR_PURPLE_MENU = new float[]{205f / 255f, 201f / 255f, 245f / 255f, 1f};
    public static final float[] COLOR_PURPLE_BRIGHT = new float[]{111f / 255f, 51f / 255f, 139f / 255f, 1f};
    public static final float[] COLOR_ROSA = new float[]{231f / 255f, 43f / 255f, 120f / 255f, 1f};

    public static final float[] COLOR_BACKGROUND_BRIGHT = new float[]{126f / 255f, 126f / 255f, 146f / 255f, 1f};
    public static final float[] COLOR_BACKGROUND = new float[]{26f / 255f, 26f / 255f, 46f / 255f, 1f};
    public static final float[] COLOR_BACKGROUND_CARD = new float[]{214f / 255f, 214f / 255f, 214f / 255f, 1f};
    public static final float[] COLOR_BACKGROUND_CARD_2 = new float[]{199f / 255f, 199f / 255f, 199f / 255f, 1f};
    public static final float[] COLOR_CARD_UNDERGROUND = new float[]{229f / 255f, 26f / 255f, 116f / 255f, 1f};
    public static final float[] COLOR_BUTTONS = new float[]{55f / 255f, 44f / 255f, 72f / 255f, 1f};

    public static boolean HELP_TIMER = false;

    /** Debug overlay: render "x,y" on every board cell. Press F2 in-game to toggle. */
    public static boolean SHOW_COORDS = false;

    public static boolean IS_ANDROID = false;
    public static boolean IS_HTML = false;
    public static boolean IS_MOBILE = false;

    public static String round(double zahl, int stellen) {
        double d = (double) ((int) zahl + (Math.round(Math.pow(10, stellen) * (zahl - (int) zahl))) / (Math.pow(10, stellen)));
        String result = String.valueOf(d);
        if (result.indexOf(".") < result.length() - stellen) {
            result = result.substring(0, result.indexOf(".") + stellen + 1);
        } else if (result.indexOf(".") >= result.length() - stellen) {
            result = result + "0";
        }
        return result;
    }

    public static final String STRING_EASY_ENG = "Game";
    public static final String STRING_EASY_GER = "Spiel";
    public static String STRING_EASY = STRING_EASY_GER;

    public static final String STRING_TOKEN_ENG = "The token is";
    public static final String STRING_TOKEN_GER = "Der Token lautet";
    public static String STRING_TOKEN = STRING_TOKEN_GER;

    public static final String STRING_LOVE_TEXT_ENG = "Create a path to deliver all presents";
    public static final String STRING_LOVE_TEXT_GER = "Liefere die Geschenke an die Kinder";
    public static String STRING_LOVE_TEXT = STRING_LOVE_TEXT_GER;

    public static final String STRING_UNDO_TEXT_ENG = "Press u to undo the last step";
    public static final String STRING_UNDO_TEXT_GER = "Druecke u, um den letzten Schritt rueckgaenig zu machen";
    public static String STRING_UNDO_TEXT = STRING_UNDO_TEXT_GER;

    public static final String[][] STRING_TUTORIAL_TEXT_ENG = {{"Move with the cursor keys or the dpad.",
            "Press u to undo your last step.", "Press r to restart the level"},
            {"Every rule counts.", ""},
            {"Often I have to make the rules.", "I have the power."}};
    public static final String[][] STRING_TUTORIAL_TEXT_GER = {{
            "Pfeiltasten oder dpad = Bewegung",
            "Druecke U = Letzter Schritt rueckganig",
            "Druecke R = Level Neustart"},
            {"Jede einzelne neue Regel zaehlt.", ""},
            {"Haeufig muss ich die Regeln erstellen,", "um mein Ziel zu erreichen."}};
    public static String[][] STRING_TUTORIAL_TEXT = STRING_TUTORIAL_TEXT_GER;

    public static final String[] STRING_TUTORIAL_0_MOVE_TEXT_ENG = {
            "Every level has 2 steps",
            "In the first step create a path to the house",
            "but aware the ice is brittle and you can't swim.",
            "After visiting the house the water will transform",
            "to snow. Then deliver the sliding presents to the house."};
    public static final String[] STRING_TUTORIAL_0_MOVE_TEXT_GER = {
            "Jedes Level besteht aus zwei Schritten: Zuerst laufe",
            "einen Weg zum Haus, aber das Eis ist bruechig und",
            "du kannst nicht schwimmen. Nach dem Besuch des Hauses",
            "verwandelt sich dein Weg von Wasser zu Schnee und",
            "du kannst die schlitternden Geschenke ins Haus bringen."};
    public static String[] STRING_TUTORIAL_0_MOVE_TEXT = STRING_TUTORIAL_0_MOVE_TEXT_GER;

    public static final String STRING_TUTORIAL_0_MOVE_TEXT_SINGLE_ENG = "Move with the dpad";
    public static final String STRING_TUTORIAL_0_MOVE_TEXT_SINGLE_GER = "Bewege dich mit dem Steuerkreuz";
    public static String STRING_TUTORIAL_0_MOVE_TEXT_SINGLE = STRING_TUTORIAL_0_MOVE_TEXT_SINGLE_GER;

    public static final String[] STRING_MENU_TEXT_ENG = {"Choose your level", "and let the adventure begin"};
    public static final String[] STRING_MENU_TEXT_GER = {"Waehle ein Level aus", "und lass das Abenteuer beginnen"};
    public static String[] STRING_MENU_TEXT = STRING_MENU_TEXT_GER;

    public static final String[] STRING_ANALYSIS_TEXT_ENG = {"Congratulation", "Oh no ..."};
    public static final String[] STRING_ANALYSIS_TEXT_GER = {"Herzlichen Glueckwunsch", "Oh nein ..."};
    public static String[] STRING_ANALYSIS_TEXT = STRING_ANALYSIS_TEXT_GER;

    public static final String[] STRING_ANALYSIS_NEXT_TEXT_ENG = {"Press play to start the next level", "Please try again"};
    public static final String[] STRING_ANALYSIS_NEXT_TEXT_GER = {"Auf gehts ins naechste Level", "Bitte versuche es erneut"};
    public static String[] STRING_ANALYSIS_NEXT_TEXT = STRING_ANALYSIS_NEXT_TEXT_GER;

    public static final String STRING_OPTIONS_LANGUAGE_ENG = "Language";
    public static final String STRING_OPTIONS_LANGUAGE_GER = "Sprache";
    public static String STRING_OPTIONS_LANGUAGE = STRING_OPTIONS_LANGUAGE_GER;

    // --- End-of-game dialog ----------------------------------------------------------
    public static final String STRING_DIALOG_CONGRATS_ENG = "Congratulations!";
    public static final String STRING_DIALOG_CONGRATS_GER = "Glueckwunsch!";
    public static String STRING_DIALOG_CONGRATS = STRING_DIALOG_CONGRATS_GER;

    public static final String STRING_DIALOG_TOO_BAD_ENG = "Too bad!";
    public static final String STRING_DIALOG_TOO_BAD_GER = "Schade!";
    public static String STRING_DIALOG_TOO_BAD = STRING_DIALOG_TOO_BAD_GER;

    public static final String STRING_DIALOG_WINNER_ENG = "Winner";
    public static final String STRING_DIALOG_WINNER_GER = "Sieger";
    public static String STRING_DIALOG_WINNER = STRING_DIALOG_WINNER_GER;

    public static final String STRING_DIALOG_NEXT_ENG = "Next";
    public static final String STRING_DIALOG_NEXT_GER = "Weiter";
    public static String STRING_DIALOG_NEXT = STRING_DIALOG_NEXT_GER;

    public static final String STRING_DIALOG_BACK_ENG = "Back";
    public static final String STRING_DIALOG_BACK_GER = "Zurueck";
    public static String STRING_DIALOG_BACK = STRING_DIALOG_BACK_GER;

    public static final String STRING_DIALOG_PASSES_ENG = "Passes";
    public static final String STRING_DIALOG_PASSES_GER = "Paesse";
    public static String STRING_DIALOG_PASSES = STRING_DIALOG_PASSES_GER;

    public static final String STRING_DIALOG_MOVES_ENG = "Moves";
    public static final String STRING_DIALOG_MOVES_GER = "Zuege";
    public static String STRING_DIALOG_MOVES = STRING_DIALOG_MOVES_GER;

    public static final String STRING_DIALOG_CAPTURED_ENG = "Captured";
    public static final String STRING_DIALOG_CAPTURED_GER = "Geschlagen";
    public static String STRING_DIALOG_CAPTURED = STRING_DIALOG_CAPTURED_GER;

    public static final String STRING_DIALOG_LOST_ENG = "Lost";
    public static final String STRING_DIALOG_LOST_GER = "Verloren";
    public static String STRING_DIALOG_LOST = STRING_DIALOG_LOST_GER;

    public static final String STRING_DIALOG_WHITE_ENG = "White";
    public static final String STRING_DIALOG_WHITE_GER = "Weiss";
    public static String STRING_DIALOG_WHITE = STRING_DIALOG_WHITE_GER;

    public static final String STRING_DIALOG_BLACK_ENG = "Black";
    public static final String STRING_DIALOG_BLACK_GER = "Schwarz";
    public static String STRING_DIALOG_BLACK = STRING_DIALOG_BLACK_GER;

    public static final String STRING_LANG_PICKER_ENG = "Choose language";
    public static final String STRING_LANG_PICKER_GER = "Sprache waehlen";
    public static String STRING_LANG_PICKER = STRING_LANG_PICKER_GER;

    static {
        REGION = "de";
        try {
            REGION = Locale.getDefault().getLanguage();
        } catch (Exception ex) {
            REGION = "de";
        }
        setLanguage(REGION);
    }

    public static void setLanguage(final String region) {
        REGION = region;
        boolean de = (region != null) && region.equals("de");
        STRING_LOVE_TEXT                   = de ? STRING_LOVE_TEXT_GER                   : STRING_LOVE_TEXT_ENG;
        STRING_TUTORIAL_TEXT               = de ? STRING_TUTORIAL_TEXT_GER               : STRING_TUTORIAL_TEXT_ENG;
        STRING_TUTORIAL_0_MOVE_TEXT        = de ? STRING_TUTORIAL_0_MOVE_TEXT_GER        : STRING_TUTORIAL_0_MOVE_TEXT_ENG;
        STRING_MENU_TEXT                   = de ? STRING_MENU_TEXT_GER                   : STRING_MENU_TEXT_ENG;
        STRING_ANALYSIS_TEXT               = de ? STRING_ANALYSIS_TEXT_GER               : STRING_ANALYSIS_TEXT_ENG;
        STRING_ANALYSIS_NEXT_TEXT          = de ? STRING_ANALYSIS_NEXT_TEXT_GER          : STRING_ANALYSIS_NEXT_TEXT_ENG;
        STRING_OPTIONS_LANGUAGE            = de ? STRING_OPTIONS_LANGUAGE_GER            : STRING_OPTIONS_LANGUAGE_ENG;
        STRING_UNDO_TEXT                   = de ? STRING_UNDO_TEXT_GER                   : STRING_UNDO_TEXT_ENG;
        STRING_TUTORIAL_0_MOVE_TEXT_SINGLE = de ? STRING_TUTORIAL_0_MOVE_TEXT_SINGLE_GER : STRING_TUTORIAL_0_MOVE_TEXT_SINGLE_ENG;
        STRING_EASY                        = de ? STRING_EASY_GER                        : STRING_EASY_ENG;
        STRING_TOKEN                       = de ? STRING_TOKEN_GER                       : STRING_TOKEN_ENG;
        STRING_DIALOG_CONGRATS             = de ? STRING_DIALOG_CONGRATS_GER             : STRING_DIALOG_CONGRATS_ENG;
        STRING_DIALOG_TOO_BAD              = de ? STRING_DIALOG_TOO_BAD_GER              : STRING_DIALOG_TOO_BAD_ENG;
        STRING_DIALOG_WINNER               = de ? STRING_DIALOG_WINNER_GER               : STRING_DIALOG_WINNER_ENG;
        STRING_DIALOG_NEXT                 = de ? STRING_DIALOG_NEXT_GER                 : STRING_DIALOG_NEXT_ENG;
        STRING_DIALOG_BACK                 = de ? STRING_DIALOG_BACK_GER                 : STRING_DIALOG_BACK_ENG;
        STRING_DIALOG_PASSES               = de ? STRING_DIALOG_PASSES_GER               : STRING_DIALOG_PASSES_ENG;
        STRING_DIALOG_MOVES                = de ? STRING_DIALOG_MOVES_GER                : STRING_DIALOG_MOVES_ENG;
        STRING_DIALOG_CAPTURED             = de ? STRING_DIALOG_CAPTURED_GER             : STRING_DIALOG_CAPTURED_ENG;
        STRING_DIALOG_LOST                 = de ? STRING_DIALOG_LOST_GER                 : STRING_DIALOG_LOST_ENG;
        STRING_DIALOG_WHITE                = de ? STRING_DIALOG_WHITE_GER                : STRING_DIALOG_WHITE_ENG;
        STRING_DIALOG_BLACK                = de ? STRING_DIALOG_BLACK_GER                : STRING_DIALOG_BLACK_ENG;
        STRING_LANG_PICKER                 = de ? STRING_LANG_PICKER_GER                 : STRING_LANG_PICKER_ENG;
    }
}
