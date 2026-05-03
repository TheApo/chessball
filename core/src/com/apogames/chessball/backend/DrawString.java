package com.apogames.chessball.backend;

/**
 * The enum Draw string.
 */
public enum DrawString {

    /**
     * Begin draw string.
     */
    BEGIN(0),
    /**
     * Middle draw string.
     */
    MIDDLE(0.5f),
    /**
     * End draw string.
     */
    END(1f);
	
	private float difference;
	
	private DrawString(float difference) {
		this.difference = difference;
	}

    /**
     * Gets difference.
     *
     * @return the difference
     */
    public float getDifference() {
		return difference;
	}
}
