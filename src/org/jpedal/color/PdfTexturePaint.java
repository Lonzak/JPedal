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
 * PdfTexturePaint.java
 * ---------------
 */
package org.jpedal.color;

import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;

public class PdfTexturePaint extends TexturePaint implements PdfPaint {
	public PdfTexturePaint(BufferedImage txtr, Rectangle2D anchor) {
		super(txtr, anchor);
	}

	@Override
	public PaintContext createContext(ColorModel cm, Rectangle db, Rectangle2D ub, AffineTransform xform, RenderingHints hints) {
		return super.createContext(cm, db, ub, xform, hints);
	}

	@Override
	public void setScaling(double cropX, double cropH, float scaling, float textX, float textY) {}

	@Override
	public boolean isPattern() {
		return false;
	}

	@Override
	public int getRGB() {
		return 0;
	}

	@Override
	public void setRenderingType(int createHtml) {
		// added for HTML conversion
	}

}
