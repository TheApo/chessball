package com.apogames.chessball;

import com.apogames.chessball.common.Localization;
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

    public static float[] COLOR_CLEAR = new float[]{62f / 255f, 98f / 255f, 28f / 255f, 1f};

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

    static {
        REGION = "de";
        try {
            REGION = Locale.getDefault().getLanguage();
        } catch (Exception ex) {
            REGION = "de";
        }
    }

    /** Switches the active locale used by {@link Localization}. */
    public static void setLanguage(final String region) {
        REGION = region;
        Locale locale = (region != null && region.equals("de")) ? Locale.GERMAN : Locale.ENGLISH;
        Localization.getInstance().setLocale(locale);
    }
}
