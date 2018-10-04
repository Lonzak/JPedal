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
 * T3Display.java
 * ---------------
 */
package org.jpedal.render;

import java.awt.Color;
import java.util.Map;

import org.jpedal.color.PdfColor;
import org.jpedal.color.PdfPaint;
import org.jpedal.io.ObjectStore;

public class T3Display extends SwingDisplay implements T3Renderer {

	public T3Display(int pageNumber, ObjectStore newObjectRef, boolean isPrinting) {
		super(pageNumber, newObjectRef, isPrinting);
	}

	/** create instance and set flag to show if we draw white background */
	public T3Display(int pageNumber, boolean addBackground, int defaultSize, ObjectStore newObjectRef) {

		this.pageNumber = pageNumber;
		this.objectStoreRef = newObjectRef;
		this.addBackground = addBackground;

		setupArrays(defaultSize);
	}

	public T3Display(byte[] dvr, Map map) {
		super(dvr, map);
	}

	/**
	 * used internally - please do not use
	 */
	@Override
	public void setOptimisedRotation(boolean value) {
		this.optimisedTurnCode = value;
	}

	/**
	 * use by type3 fonts to differentiate images in local store
	 */
	@Override
	public void setType3Glyph(String pKey) {
		this.rawKey = pKey;

		this.isType3Font = true;
	}

	/**
	 * used by type 3 glyphs to set colour
	 */
	@Override
	public void lockColors(PdfPaint strokePaint, PdfPaint nonstrokePaint, boolean lockColour) {

		this.colorsLocked = lockColour;
		Color strokeColor = Color.white, nonstrokeColor = Color.white;

		if (strokePaint != null && !strokePaint.isPattern()) strokeColor = (Color) strokePaint;
		this.strokeCol = new PdfColor(strokeColor.getRed(), strokeColor.getGreen(), strokeColor.getBlue());

		if (!nonstrokePaint.isPattern()) nonstrokeColor = (Color) nonstrokePaint;
		this.fillCol = new PdfColor(nonstrokeColor.getRed(), nonstrokeColor.getGreen(), nonstrokeColor.getBlue());
	}

	@Override
	public String toString() {
		return "T3Display: " + super.toString();
	}
	
}
