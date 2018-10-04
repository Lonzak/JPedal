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
 * T3Glyph.java
 * ---------------
 */
package org.jpedal.fonts.glyph;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.jpedal.color.PdfPaint;
import org.jpedal.render.T3Display;
import org.jpedal.render.T3Renderer;
import org.jpedal.utils.LogWriter;

/**
 * <p>
 * defines the current shape which is created by command stream
 * </p>
 * <p>
 * <b>This class is NOT part of the API</b>
 * </p>
 * . Shapes can be drawn onto pdf or used as a clip on other image/shape/text. Shape is built up by storing commands and then turning these commands
 * into a shape. Has to be done this way as Winding rule is not necessarily declared at start.
 */
public class T3Glyph implements PdfGlyph {

	private boolean lockColours = false;

	T3Renderer glyphDisplay;

	/** actual offset of glyph */
	private int maxWidth, maxHeight;

	String stringName = "";

	float glyphScale = 1f;

	public T3Glyph() {}

	@Override
	public String getGlyphName() {
		return this.stringName;
	}

	public void setStringName(String stringName) {
		this.stringName = stringName;
	}

	/**
	 * create the glyph as a wrapper around the DynamicVectorRenderer
	 */
	public T3Glyph(T3Renderer glyphDisplay, int x, int y, boolean lockColours, String pKey) {
		this.glyphDisplay = glyphDisplay;
		this.maxWidth = x;
		this.maxHeight = y;
		this.lockColours = lockColours;
		this.stringName = pKey;
	}

	// used by Type3 if need to adjust scaling due to size
	public void setScaling(float glyphScaling) {
		this.glyphScale = glyphScaling;
	}

	/**
	 * draw the t3 glyph
	 */
	@Override
	public Area getShape() {
		return null;
	}

	/**
	 * draw the t3 glyph
	 */
	@Override
	public void render(int type, Graphics2D g2, float scaling, boolean isFormGlyph) {

		this.glyphDisplay.setScalingValues(0, 0, scaling);

		// preseve old scaling to set back, in case others are using.
		float OLDglyphScale = this.glyphScale;

		if (isFormGlyph) {
			// scale the glyph to include the defined scaling
			this.glyphScale = scaling * this.glyphScale;
		}

		// factor in if not correct size
		AffineTransform aff = null;
		if (this.glyphScale != 1f) {
			aff = g2.getTransform();
			g2.scale(this.glyphScale, this.glyphScale);
		}

		this.glyphDisplay.setG2(g2);
		this.glyphDisplay.paint(null, null, null);

		// undo
		if (aff != null) g2.setTransform(aff);

		// set back the old glyphscale value, in case others are using.
		this.glyphScale = OLDglyphScale;
	}

	/**
	 * Returns the max width
	 */
	@Override
	public float getmaxWidth() {
		if (this.maxWidth == 0 && this.glyphScale < 1f) return 1f / this.glyphScale;
		else return this.maxWidth;
	}

	/**
	 * Returns the max height
	 */
	@Override
	public int getmaxHeight() {
		return this.maxHeight;
	}

	/**
	 * set colors for display
	 */
	@Override
	public void setT3Colors(PdfPaint strokeColor, PdfPaint nonstrokeColor, boolean lockColours) {

		this.glyphDisplay.lockColors(strokeColor, nonstrokeColor, lockColours);
	}

	/**
	 * flag if use internal colours or text colour
	 */
	@Override
	public boolean ignoreColors() {
		return this.lockColours;
	}

	/*
	 * Makes debugging easier
	 */
	@Override
	public int getID() {
		return this.id;
	}

	int id = 0;

	/*
	 * Makes debugging easier
	 */
	@Override
	public void setID(int id) {
		this.id = id;
	}

	/**
	 * method to serialize all the paths in this object. This method is needed because GeneralPath does not implement Serializable so we need to
	 * serialize it ourself. The correct usage is to first serialize this object, cached_current_path is marked as transient so it will not be
	 * serilized, this method should then be called, so the paths are serialized directly after the main object in the same ObjectOutput.
	 * 
	 * NOT PART OF API and subject to change (DO NOT USE)
	 * 
	 * @param os
	 *            - ObjectOutput to write to
	 * @throws IOException
	 */
	public void writePathsToStream(ObjectOutput os) throws IOException {

		// convert to bytes
		byte[] dvr = this.glyphDisplay.serializeToByteArray(null);

		// int size=dvr.length;

		os.writeObject(dvr);
		os.writeInt(this.maxWidth);
		os.writeInt(this.maxHeight);
		os.writeBoolean(this.lockColours);
	}

	/** recreate T3 glyph from serialized data */
	public T3Glyph(ObjectInput os) {

		try {
			byte[] dvr = (byte[]) os.readObject();

			this.glyphDisplay = new T3Display(dvr, null);

			this.maxWidth = os.readInt();
			this.maxHeight = os.readInt();
			this.lockColours = os.readBoolean();

		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}
	}

	public void flushArea() {
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

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("T3Glyph [lockColours=");
		builder.append(this.lockColours);
		builder.append(", ");
		if (this.glyphDisplay != null) {
			builder.append("glyphDisplay=");
			builder.append(this.glyphDisplay.getClass().getSimpleName());
			builder.append(", ");
		}
		builder.append("maxWidth=");
		builder.append(this.maxWidth);
		builder.append(", maxHeight=");
		builder.append(this.maxHeight);
		builder.append(", ");
		if (this.stringName != null) {
			builder.append("stringName=");
			builder.append(this.stringName);
			builder.append(", ");
		}
		builder.append("glyphScale=");
		builder.append(this.glyphScale);
		builder.append(", id=");
		builder.append(this.id);
		builder.append("]");
		return builder.toString();
	}
	
	
}
