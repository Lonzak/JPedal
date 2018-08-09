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
 * TextState.java
 * ---------------
 */
package org.jpedal.objects;

import org.jpedal.utils.StringUtils;

/**
 * holds the current text state
 */
public class TextState {

	/** orientation of text using contstants from PdfData */
	public int writingMode = 0;

	/** last Tm value */
	private float[][] TmAtStart = new float[3][3];

	/** last Tm value */
	private float[][] TmAtStartNoRotation = new float[3][3];

	/** matrix operations for calculating start of text */
	public float[][] Tm = new float[3][3];

	/** used by storypad so we can unrotate text for scaling */
	public float[][] TmNoRotation = new float[3][3];

	private String font_ID = "";

	/** leading setin text */
	private float TL = 0;

	/** gap between chars set by Tc command */
	private float character_spacing = 0;

	/** current Tfs value */
	private float Tfs = 0;

	/** text rise set in stream */
	private float text_rise = 0;

	/** text height - see also Tfs */
	private float th = 1;

	/** gap inserted with spaces - set by Tw */
	private float word_spacing;

	private boolean hasFontChanged = false;

	/**
	 * set Trm values
	 */
	public TextState() {
		this.Tm[0][0] = 1;
		this.Tm[0][1] = 0;
		this.Tm[0][2] = 0;
		this.Tm[1][0] = 0;
		this.Tm[1][1] = 1;
		this.Tm[1][2] = 0;
		this.Tm[2][0] = 0;
		this.Tm[2][1] = 0;
		this.Tm[2][2] = 1;

		this.TmAtStart[0][0] = 1;
		this.TmAtStart[0][1] = 0;
		this.TmAtStart[0][2] = 0;
		this.TmAtStart[1][0] = 0;
		this.TmAtStart[1][1] = 1;
		this.TmAtStart[1][2] = 0;
		this.TmAtStart[2][0] = 0;
		this.TmAtStart[2][1] = 0;
		this.TmAtStart[2][2] = 1;

		this.TmNoRotation[0][0] = 1;
		this.TmNoRotation[0][1] = 0;
		this.TmNoRotation[0][2] = 0;
		this.TmNoRotation[1][0] = 0;
		this.TmNoRotation[1][1] = 1;
		this.TmNoRotation[1][2] = 0;
		this.TmNoRotation[2][0] = 0;
		this.TmNoRotation[2][1] = 0;
		this.TmNoRotation[2][2] = 1;
	}

	/**
	 * get Tm at start of line
	 */
	public float[][] getTMAtLineStart() {
		return this.TmAtStart;
	}

	/**
	 * set Tm at start of line
	 */
	public void setTMAtLineStart() {

		// keep position in case we need
		this.TmAtStart[0][0] = this.Tm[0][0];
		this.TmAtStart[0][1] = this.Tm[0][1];
		this.TmAtStart[0][2] = this.Tm[0][2];
		this.TmAtStart[1][0] = this.Tm[1][0];
		this.TmAtStart[1][1] = this.Tm[1][1];
		this.TmAtStart[1][2] = this.Tm[1][2];
		this.TmAtStart[2][0] = this.Tm[2][0];
		this.TmAtStart[2][1] = this.Tm[2][1];
		this.TmAtStart[2][2] = this.Tm[2][2];
	}

	/**
	 * get Tm at start of line
	 */
	public float[][] getTMAtLineStartNoRotation() {
		return this.TmAtStartNoRotation;
	}

	/**
	 * set Tm at start of line
	 */
	public void setTMAtLineStartNoRotation() {

		// keep position in case we need
		this.TmAtStartNoRotation[0][0] = this.TmNoRotation[0][0];
		this.TmAtStartNoRotation[0][1] = this.TmNoRotation[0][1];
		this.TmAtStartNoRotation[0][2] = this.TmNoRotation[0][2];
		this.TmAtStartNoRotation[1][0] = this.TmNoRotation[1][0];
		this.TmAtStartNoRotation[1][1] = this.TmNoRotation[1][1];
		this.TmAtStartNoRotation[1][2] = this.TmNoRotation[1][2];
		this.TmAtStartNoRotation[2][0] = this.TmNoRotation[2][0];
		this.TmAtStartNoRotation[2][1] = this.TmNoRotation[2][1];
		this.TmAtStartNoRotation[2][2] = this.TmNoRotation[2][2];
	}

	// ////////////////////////////////////////////////////////////////////////
	/**
	 * set Horizontal Scaling
	 */
	final public void setHorizontalScaling(float th) {
		this.th = th;
	}

	// /////////////////////////////////////////////////////////////////////
	/**
	 * get font id
	 */
	final public String getFontID() {
		return this.font_ID;
	}

	// /////////////////////////////////////////////////////////////////////////
	/**
	 * get Text rise
	 */
	final public float getTextRise() {
		return this.text_rise;
	}

	// //////////////////////////////////////////////////////////////////////
	/**
	 * get character spacing
	 */
	final public float getCharacterSpacing() {
		return this.character_spacing;
	}

	// ///////////////////////////////////////////////////////////////////////
	/**
	 * get word spacing
	 */
	final public float getWordSpacing() {
		return this.word_spacing;
	}

	// /////////////////////////////////////////////////////////////////////////
	/**
	 * set font tfs
	 */
	final public void setLeading(float TL) {
		this.TL = TL;
	}

	// ///////////////////////////////////////////////////////////////////////
	/**
	 * get font tfs
	 */
	final public float getTfs() {
		return this.Tfs;
	}

	// ///////////////////////////////////////////////////////////////////////
	/**
	 * get Horizontal Scaling
	 */
	final public float getHorizontalScaling() {
		return this.th;
	}

	/**
	 * set Text rise
	 */
	final public void setTextRise(float text_rise) {
		this.text_rise = text_rise;
	}

	/**
	 * get font tfs
	 */
	final public float getLeading() {
		return this.TL;
	}

	// ///////////////////////////////////////////////////////////////////////
	/**
	 * clone object
	 */
	/*
	 * final public Object clone() { Object o = null; try { o = super.clone(); } catch( Exception e ) { LogWriter.writeLog( "Unable to clone " + e );
	 * } return o; }
	 */
	// ///////////////////////////////////////////////////////////////////////

	@Override
	final public Object clone() {

		TextState ts = new TextState();

		ts.writingMode = this.writingMode;

		if (this.TmAtStart != null) {
			for (int i = 0; i < 3; i++) {
				System.arraycopy(this.TmAtStart[i], 0, ts.TmAtStart[i], 0, 3);
			}
		}

		if (this.TmAtStartNoRotation != null) {
			for (int i = 0; i < 3; i++) {
				System.arraycopy(this.TmAtStartNoRotation[i], 0, ts.TmAtStartNoRotation[i], 0, 3);
			}
		}

		if (this.Tm != null) {
			for (int i = 0; i < 3; i++) {
				System.arraycopy(this.Tm[i], 0, ts.Tm[i], 0, 3);
			}
		}

		if (this.TmNoRotation != null) {
			for (int i = 0; i < 3; i++) {
				System.arraycopy(this.TmNoRotation[i], 0, ts.TmNoRotation[i], 0, 3);
			}
		}

		if (this.font_ID != null) ts.font_ID = new String(StringUtils.toBytes(this.font_ID));

		ts.TL = this.TL;

		ts.character_spacing = this.character_spacing;

		ts.Tfs = this.Tfs;

		ts.text_rise = this.text_rise;

		ts.th = this.th;

		ts.word_spacing = this.word_spacing;

		ts.hasFontChanged = this.hasFontChanged;

		return ts;
	}

	/**
	 * set word spacing
	 */
	final public void setWordSpacing(float word_spacing) {
		this.word_spacing = word_spacing;
	}

	/**
	 * set font ID
	 */
	final public void setFontID(String font_ID) {
		this.font_ID = font_ID;
	}

	/**
	 * set character spacing
	 */
	final public void setCharacterSpacing(float character_spacing) {
		this.character_spacing = character_spacing;
	}

	// ////////////////////////////////////////////////////////////////////////
	/**
	 * set font tfs to default
	 */
	final public void resetTm() {
		this.Tm[0][0] = 1;
		this.Tm[0][1] = 0;
		this.Tm[0][2] = 0;
		this.Tm[1][0] = 0;
		this.Tm[1][1] = 1;
		this.Tm[1][2] = 0;
		this.Tm[2][0] = 0;
		this.Tm[2][1] = 0;
		this.Tm[2][2] = 1;

		this.TmNoRotation[0][0] = 1;
		this.TmNoRotation[0][1] = 0;
		this.TmNoRotation[0][2] = 0;
		this.TmNoRotation[1][0] = 0;
		this.TmNoRotation[1][1] = 1;
		this.TmNoRotation[1][2] = 0;
		this.TmNoRotation[2][0] = 0;
		this.TmNoRotation[2][1] = 0;
		this.TmNoRotation[2][2] = 1;

		// keep position in case we need
		setTMAtLineStart();
	}

	// ////////////////////////////////////////////////////////////////////////
	/**
	 * set font tfs
	 */
	final public void setFontTfs(float Tfs) {
		this.Tfs = Tfs;
	}

	public boolean hasFontChanged() {
		return this.hasFontChanged;
	}

	public void setFontChanged(boolean status) {
		this.hasFontChanged = status;
	}

	public void TF(float Tfs, String fontID) {

		// set global variables to new values
		this.Tfs = Tfs;

		this.font_ID = fontID;

		// flag font has changed
		this.hasFontChanged = true;
	}

}
