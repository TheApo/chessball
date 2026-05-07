package com.apogames.chessball.entity;

/**
 * One piece of colored text. Multiple segments are rendered side-by-side by
 * {@link TopBar} when a single title needs portions in different colors —
 * e.g. the black-side player name shown in black to signal who is who.
 */
public class TextSegment {

    private final String text;
    private final float[] color;

    public TextSegment(String text, float[] color) {
        this.text = text == null ? "" : text;
        this.color = color;
    }

    public String getText() { return text; }
    public float[] getColor() { return color; }
}
