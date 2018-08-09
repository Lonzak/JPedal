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
 * UnrendererGlyph.java
 * ---------------
 */
package org.jpedal.fonts.glyph;

import java.awt.Graphics2D;
import java.awt.geom.Area;
import java.io.Serializable;

import org.jpedal.color.PdfPaint;

/**
 * holds data so we can draw glyph on first appearance
 * 
 */
public class UnrendererGlyph implements PdfGlyph, Serializable {

	private static final long serialVersionUID = -6766992419597676198L;
	public float x, y;
	public int rawInt;
	public float currentWidth;

	public UnrendererGlyph(float x, float y, int rawInt, float currentWidth) {

		this.x = x;
		this.y = y;
		this.rawInt = rawInt;
		this.currentWidth = currentWidth;
	}

	@Override
	public int getID() {
		return -1;
	}

	@Override
	public void setID(int id) {
		// To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void render(int text_fill_type, Graphics2D g2, float scaling, boolean isFormGlyph) {
		// To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public float getmaxWidth() {
		return 0; // To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public int getmaxHeight() {
		return 0; // To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void setT3Colors(PdfPaint strokeColor, PdfPaint nonstrokeColor, boolean lockColours) {
		// To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean ignoreColors() {
		return false; // To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Area getShape() {
		return null; // To change body of implemented methods use File | Settings | File Templates.
	}

	// used by Type3 fonts
	@Override
	public String getGlyphName() {
		return ""; // To change body of implemented methods use File | Settings | File Templates.
	}

	public void setDisplacement(short rawlsb, float width) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setWidth(float width) {
		// TODO Auto-generated method stub
	}

	@Override
	public int getFontBB(int type) {
		return 0; // To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void setStrokedOnly(boolean b) {
		// To change body of implemented methods use File | Settings | File Templates.
	}

	// use by TT to handle broken TT fonts
	@Override
	public boolean containsBrokenData() {
		return false;
	}
}