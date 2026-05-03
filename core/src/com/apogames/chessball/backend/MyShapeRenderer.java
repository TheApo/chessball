/*
 * Copyright (c) 2005-2017 Dirk Aporius <dirk.aporius@gmail.com>
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.apogames.chessball.backend;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ShortArray;

/**
 * The type My shape renderer.
 */
public class MyShapeRenderer extends ShapeRenderer {

	/**
	 * Draws an arc using {@link ShapeType#Line} or {@link ShapeType#Filled}.
	 */
	public void arc(float x, float y, float radius, float start, float degrees) {
		int segments = (int) (6 * (float) Math.cbrt(radius) * (degrees / 360.0f));

		if (segments <= 0)
			throw new IllegalArgumentException("segments must be > 0.");
		float theta = (2 * MathUtils.PI * (degrees / 360.0f)) / segments;
		float cos = MathUtils.cos(theta);
		float sin = MathUtils.sin(theta);
		float cx = radius * MathUtils.cos(start * MathUtils.degreesToRadians);
		float cy = radius * MathUtils.sin(start * MathUtils.degreesToRadians);

		for (int i = 0; i < segments; i++) {
			getRenderer().color(this.getColor());
			getRenderer().vertex(x + cx, y + cy, 0);
			float temp = cx;
			cx = cos * cx - sin * cy;
			cy = sin * temp + cos * cy;
			getRenderer().color(this.getColor());
			getRenderer().vertex(x + cx, y + cy, 0);
		}
	}

    /**
     * Rounded rect line.
     *
     * @param x      the x
     * @param y      the y
     * @param width  the width
     * @param height the height
     * @param radius the radius
     */
    public void roundedRectLine(float x, float y, float width, float height, float radius) {
		super.line(x + radius, y, x + width - 1 * radius, y);
		super.line(x + radius, y + height, x + width - 1 * radius, y + height);
		super.line(x, y + radius, x, y + height - radius);
		super.line(x + width, y + radius, x + width, y + height - radius);

		this.arc(x + radius, y + radius, radius, 180f, 90f);
		this.arc(x + width - radius, y + radius, radius, 270f, 90f);
		this.arc(x + width - radius, y + height - radius, radius, 0f, 90f);
		this.arc(x + radius, y + height - radius, radius, 90f, 90f);
	}

    /**
     * Rounded rect.
     *
     * @param x      the x
     * @param y      the y
     * @param width  the width
     * @param height the height
     * @param radius the radius
     */
    public void roundedRect(float x, float y, float width, float height, float radius) {
		super.rect(x + radius, y + radius, width - 2 * radius, height - 2 * radius);

		super.rect(x + radius, y, width - 2 * radius, radius);
		super.rect(x + width - radius, y + radius, radius, height - 2 * radius);
		super.rect(x + radius, y + height - radius, width - 2 * radius, radius);
		super.rect(x, y + radius, radius, height - 2 * radius);

		super.arc(x + radius, y + radius, radius, 180f, 90f);
		super.arc(x + width - radius, y + radius, radius, 270f, 90f);
		super.arc(x + width - radius, y + height - radius, radius, 0f, 90f);
		super.arc(x + radius, y + height - radius, radius, 90f, 90f);
	}

	/**
	 * Draws a thick circle outline (annulus) using filled triangles.
	 * Must be called inside a {@link ShapeType#Filled} block. Works identically
	 * on TeaVM / WebGL where {@code glLineWidth} is unreliable.
	 *
	 * @param cx        center x
	 * @param cy        center y
	 * @param radius    stroke centerline radius
	 * @param thickness total stroke thickness (inner to outer edge)
	 */
	public void drawThickCircleOutline(float cx, float cy, float radius, float thickness) {
		float inner = radius - thickness / 2f;
		float outer = radius + thickness / 2f;
		if (inner < 0f) inner = 0f;

		int segments = Math.max(24, (int) (12 * (float) Math.cbrt(radius)));
		float theta = MathUtils.PI2 / segments;
		float cos = MathUtils.cos(theta);
		float sin = MathUtils.sin(theta);

		float px = 1f;
		float py = 0f;
		for (int i = 0; i < segments; i++) {
			float nx = cos * px - sin * py;
			float ny = sin * px + cos * py;

			float ix1 = cx + px * inner;
			float iy1 = cy + py * inner;
			float ox1 = cx + px * outer;
			float oy1 = cy + py * outer;
			float ix2 = cx + nx * inner;
			float iy2 = cy + ny * inner;
			float ox2 = cx + nx * outer;
			float oy2 = cy + ny * outer;

			super.triangle(ix1, iy1, ox1, oy1, ox2, oy2);
			super.triangle(ix1, iy1, ox2, oy2, ix2, iy2);

			px = nx;
			py = ny;
		}
	}

	/**
	 * Draws a thick rectangular outline centered on the edges of the given rect.
	 * Must be called inside a {@link ShapeType#Filled} block. Uses four filled
	 * rects, so it renders identically on TeaVM / WebGL.
	 *
	 * @param x         rect left
	 * @param y         rect bottom
	 * @param width     rect width
	 * @param height    rect height
	 * @param thickness stroke thickness (centered on the rect edges)
	 */
	public void drawThickRectOutline(float x, float y, float width, float height, float thickness) {
		float half = thickness / 2f;
		float ox = x - half;
		float oy = y - half;
		float ow = width + thickness;
		float oh = height + thickness;

		super.rect(ox, oy, ow, thickness);
		super.rect(ox, oy + oh - thickness, ow, thickness);
		super.rect(ox, oy + thickness, thickness, oh - 2f * thickness);
		super.rect(ox + ow - thickness, oy + thickness, thickness, oh - 2f * thickness);
	}

	EarClippingTriangulator ear = new EarClippingTriangulator();

    /**
     * Fillpolygon.
     *
     * @param vertices the vertices
     */
    public void fillpolygon(float[] vertices)
	{
        ShortArray arrRes = ear.computeTriangles(vertices);

        for (int i = 0; i < arrRes.size - 2; i = i + 3)
        {
            float x1 = vertices[arrRes.get(i) * 2];
            float y1 = vertices[(arrRes.get(i) * 2) + 1];

            float x2 = vertices[(arrRes.get(i + 1)) * 2];
            float y2 = vertices[(arrRes.get(i + 1) * 2) + 1];

            float x3 = vertices[arrRes.get(i + 2) * 2];
            float y3 = vertices[(arrRes.get(i + 2) * 2) + 1];

            this.triangle(x1, y1, x2, y2, x3, y3);
        }
	}
}
