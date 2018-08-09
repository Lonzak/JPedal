/*
 * ===========================================
 * Java Pdf Extraction Decoding Access Library
 * ===========================================
 *
 * Project Info:  http://www.idrsolutions.com
 * Help section for developers at http://www.idrsolutions.com/java-pdf-library-support/
 *
 * (C) Copyright 1997-2013, IDRsolutions and Contributors.
 *
 * 	This file is part of JPedal
 *
     This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA


 *
 * ---------------
 * JpedalTexturePaint.java
 * ---------------
 */
package org.jpedal.color;

import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;

import org.jpedal.render.DynamicVectorRenderer;

public class JpedalTexturePaint implements Paint, PdfPaint {

	BufferedImage img;

	/** copy of raw tile if rotated */
	DynamicVectorRenderer glyphDisplay = null;

	TexturePaint rotatedPaint;

	// private boolean isRotated=false;

	private float[][] matrix;

	private float YStep;

	private float dx;

	private float dy;

	private AffineTransform imageScale;

	private float XStep;

	private float xx;

	private float yy;

	// private boolean isUpsideDown;

	public JpedalTexturePaint(BufferedImage txtr, Rectangle2D anchor, boolean isRotated, DynamicVectorRenderer glyphDisplay) {

		// this.isRotated=isRotated;

		System.out.println("isRotated = " + isRotated);

		if (isRotated) this.glyphDisplay = glyphDisplay;
		else this.img = txtr;

		System.out.println("glyphDisplay @ con = " + glyphDisplay);
	}

	@Override
	public PaintContext createContext(ColorModel cm, Rectangle db, Rectangle2D ub, AffineTransform xform, RenderingHints hints) {

		// create each rotated as single huge panel to fit gap as workaround to java

		float startX = 0, startY;

		// workout required size
		int w = (int) (ub.getWidth());
		int h = (int) (ub.getHeight());

		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = image.createGraphics();

		AffineTransform defaultAf2 = g2.getTransform();

		float offX, offY;

		float rotatedWidth = (this.XStep * this.matrix[0][0]) - (this.YStep * this.matrix[1][0]);

		float rotatedHeight = -(this.YStep * this.matrix[1][1]) - (this.XStep * this.matrix[0][1]);
		float shapeW = ub.getBounds().width;
		float shapeH = ub.getBounds().height;
		// int shapeCountW=(int)((shapeW/rotatedWidth));
		int shapeCountH = (int) ((shapeH / rotatedHeight));

		if (shapeCountH > 1) {

			offX = (shapeW - (rotatedHeight * (shapeCountH)));// -19;
			offY = 5 - (shapeH - (rotatedWidth * shapeCountH));// -32;

		}
		else
			if (rotatedHeight > shapeW) {
				offX = rotatedHeight - shapeW;// 5;
				offY = shapeH - rotatedWidth;// 20;
			}
			else {
				offX = (shapeH - rotatedHeight);// 28;
				offY = (shapeW - rotatedWidth);// -5;
			}

		// if tile is smaller than Xstep,Ystep, tesselate to fill
		float y;
		for (y = 0; y < h + this.YStep + this.dy; y = y + this.dy) {

			startY = -this.yy - this.yy;

			for (float x = -this.dx; x < w + this.XStep + this.dx; x = x + this.dx) {

				// if(isUpsideDown)
				// g2.translate(x+startX,-(y+startY));
				// else
				g2.translate(offX + x + startX, offY + y + startY);

				System.out.println("glyphDisplay = " + this.glyphDisplay);

				this.glyphDisplay.setG2(g2);
				this.glyphDisplay.paint(null, this.imageScale, null);
				g2.setTransform(defaultAf2);

				startY = startY + this.yy;

			}
			startX = startX - this.xx;

		}

		Rectangle rect = ub.getBounds();
		this.rotatedPaint = new TexturePaint(image, new Rectangle(rect.x, rect.y, rect.width, rect.height));

		System.out.println("hints = " + hints);

		return this.rotatedPaint.createContext(cm, db, ub, xform, hints);
	}

	@Override
	public void setScaling(double cropX, double cropH, float scaling, float textX, float textY) {
	}

	@Override
	public boolean isPattern() {
		return false;
	}

	@Override
	public void setPattern(int dummy) {
	}

	@Override
	public int getRGB() {
		return 0;
	}

	@Override
	public void setRenderingType(int createHtml) {
		// added for HTML conversion
	}

	public void setValues(float[][] matrix, float XStep, float YStep, float dx, float dy, AffineTransform imageScale, boolean isUpsideDown) {

		this.matrix = matrix;
		this.XStep = XStep;
		this.YStep = YStep;
		this.dx = dx;
		this.dy = dy;

		this.imageScale = imageScale;
		// this.isUpsideDown=isUpsideDown;

		if (matrix[0][0] != 0 && matrix[1][1] != 0) {
			this.xx = XStep * matrix[0][1];
			this.yy = YStep * matrix[1][0];

		}

		// System.out.println("Poss Xs="+(XStep*matrix[0][1])+" "+(XStep*matrix[0][0]));

		// System.out.println("values XStep="+XStep+" YStep="+YStep+" dx="+dx+" dy="+dy+
		// " xx="+xx+" yy="+yy);
	}

	@Override
	public int getTransparency() {
		return 0; // To change body of implemented methods use File | Settings | File Templates.
	}
}
