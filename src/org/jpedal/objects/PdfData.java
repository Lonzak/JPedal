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
 * PdfData.java
 * ---------------
 */
package org.jpedal.objects;

import org.jpedal.color.GenericColorSpace;
import org.jpedal.utils.Fonts;

/**
 * <p>
 * holds text data for extraction & manipulation
 * </p>
 * <p>
 * Pdf routines create 'raw' text data
 * </p>
 * <p>
 * grouping routines will attempt to intelligently stitch together and leave as 'processed data' in this class
 * </p>
 * <p>
 * <b>NOTE ONLY methods (NOT public variables) are part of API </b>
 * </p>
 * 
 */
public class PdfData {

	/** identify type as text */
	public static final int TEXT = 0;

	/** identify type as image */
	public static final int IMAGE = 1;

	/** test orientation */
	public static final int HORIZONTAL_LEFT_TO_RIGHT = 0;

	public static final int HORIZONTAL_RIGHT_TO_LEFT = 1;

	public static final int VERTICAL_TOP_TO_BOTTOM = 2;

	public static final int VERTICAL_BOTTOM_TO_TOP = 3;

	private int pointer = 0;

	/** flag to show x co-ord has been embedded in content */
	private boolean widthIsEmbedded = false;

	/** local store for max and widthheight of page */
	public float maxY = 0, maxX = 0;

	/** used to hide our encoding values in fragments of data so we can strip out */
	public static final String marker = (String.valueOf(((char) (0))));
	public static final String hiddenMarker = (String.valueOf(((char) (65534))));

	/** initial array size */
	protected int max = 2000;

	/** hold the raw content */
	public String[] contents = new String[this.max];

	/** hold flag on raw content orientation */
	public int[] f_writingMode = new int[this.max];

	/** hold raw content */
	public int[] text_length = new int[this.max];

	/** hold raw content */
	public int[] move_command = new int[this.max];

	/** hold raw content */
	public float[] f_character_spacing = new float[this.max];

	/** hold raw content */
	public int[] f_end_font_size = new int[this.max];

	/** hold raw content */
	public float[] space_width = new float[this.max];

	/** hold raw content */
	public float[] f_x1 = new float[this.max];

	/** hold color content */
	public String[] colorTag = new String[this.max];

	/** hold raw content */
	public float[] f_x2 = new float[this.max];

	/** hold raw content */
	public float[] f_y1 = new float[this.max];

	/** hold raw content */
	public float[] f_y2 = new float[this.max];

	boolean isColorExtracted;

	/** create empty object to hold content */
	public PdfData() {}

	/**
	 * get number of raw objects on page
	 */
	final public int getRawTextElementCount() {
		return this.pointer;
	}

	/**
	 * clear store of objects once written out to reclaim memory. If flag set, sets data to state after page decoded before grouping for reparse
	 */
	final public void flushTextList(boolean reinit) {

		if (!reinit) {

			this.pointer = 0;

			this.max = 2000;

			this.contents = new String[this.max];
			this.f_writingMode = new int[this.max];
			this.text_length = new int[this.max];
			this.move_command = new int[this.max];
			this.f_character_spacing = new float[this.max];
			this.f_end_font_size = new int[this.max];
			this.space_width = new float[this.max];
			this.f_x1 = new float[this.max];
			this.f_x2 = new float[this.max];
			this.f_y1 = new float[this.max];
			this.f_y2 = new float[this.max];

			this.colorTag = new String[this.max];
		}
	}

	/**
	 * store line of raw text for later processing
	 */
	final public void addRawTextElement(float character_spacing, int writingMode, String font_as_string, float current_space, int fontSize, float x1,
			float y1, float x2, float y2, int move_type, StringBuffer processed_line, int current_text_length, String currentColorTag,
			boolean isXMLExtraction) {

		if (processed_line.length() > 0) {

			// add tokens
			if (isXMLExtraction) {
				processed_line.insert(0, font_as_string);
				processed_line.append(Fonts.fe);
			}

			// add color token
			if (this.isColorExtracted) {
				processed_line.insert(0, currentColorTag);
				processed_line.append(GenericColorSpace.ce);
			}

			this.f_writingMode[this.pointer] = writingMode;
			this.text_length[this.pointer] = current_text_length;
			this.move_command[this.pointer] = move_type;
			this.f_character_spacing[this.pointer] = character_spacing;
			this.f_x1[this.pointer] = x1;
			this.colorTag[this.pointer] = currentColorTag;
			this.f_x2[this.pointer] = x2;
			this.f_y1[this.pointer] = y1;
			this.f_y2[this.pointer] = y2;
			this.contents[this.pointer] = processed_line.toString();
			this.f_end_font_size[this.pointer] = fontSize;
			this.space_width[this.pointer] = current_space * 1000;

			this.pointer++;

			// resize pointers
			if (this.pointer == this.max) resizeArrays(0);
		}
	}

	/**
	 * resize arrays to add newItems to end (-1 makes it grow)
	 */
	private void resizeArrays(int newItems) {

		float[] temp_f;
		int[] temp_i;
		String[] temp_s;

		if (newItems < 0) {
			this.max = -newItems;
			this.pointer = this.max;
		}
		else
			if (newItems == 0) {
				if (this.max < 5000) this.max = this.max * 5;
				else
					if (this.max < 10000) this.max = this.max * 2;
					else this.max = this.max + 1000;
			}
			else {
				this.max = this.contents.length + newItems - 1;
				this.pointer = this.contents.length;
			}

		temp_s = this.contents;
		this.contents = new String[this.max];
		System.arraycopy(temp_s, 0, this.contents, 0, this.pointer);

		temp_i = this.f_writingMode;
		this.f_writingMode = new int[this.max];
		this.f_writingMode = new int[this.max];
		System.arraycopy(temp_i, 0, this.f_writingMode, 0, this.pointer);

		temp_s = this.colorTag;
		this.colorTag = new String[this.max];
		System.arraycopy(temp_s, 0, this.colorTag, 0, this.pointer);

		temp_i = this.text_length;
		this.text_length = new int[this.max];
		System.arraycopy(temp_i, 0, this.text_length, 0, this.pointer);

		temp_i = this.move_command;
		this.move_command = new int[this.max];
		System.arraycopy(temp_i, 0, this.move_command, 0, this.pointer);

		temp_f = this.f_character_spacing;
		this.f_character_spacing = new float[this.max];
		System.arraycopy(temp_f, 0, this.f_character_spacing, 0, this.pointer);

		temp_i = this.f_end_font_size;
		this.f_end_font_size = new int[this.max];
		System.arraycopy(temp_i, 0, this.f_end_font_size, 0, this.pointer);

		temp_f = this.space_width;
		this.space_width = new float[this.max];
		System.arraycopy(temp_f, 0, this.space_width, 0, this.pointer);

		temp_f = this.f_x1;
		this.f_x1 = new float[this.max];
		System.arraycopy(temp_f, 0, this.f_x1, 0, this.pointer);

		temp_f = this.f_x2;
		this.f_x2 = new float[this.max];
		System.arraycopy(temp_f, 0, this.f_x2, 0, this.pointer);

		temp_f = this.f_y1;
		this.f_y1 = new float[this.max];
		System.arraycopy(temp_f, 0, this.f_y1, 0, this.pointer);

		temp_f = this.f_y2;
		this.f_y2 = new float[this.max];
		System.arraycopy(temp_f, 0, this.f_y2, 0, this.pointer);
	}

	/**
	 * set flag to show width in text
	 */
	public void widthIsEmbedded() {

		this.widthIsEmbedded = true;
	}

	/**
	 * show if width in text
	 */
	public boolean IsEmbedded() {
		return this.widthIsEmbedded;
	}

	/**
	 * set colour extraction
	 */
	public void enableTextColorDataExtraction() {
		this.isColorExtracted = true;
	}

	/**
	 * flag to show if color extracted in xml
	 */
	public boolean isColorExtracted() {
		return this.isColorExtracted;
	}

	public void dispose() {

		this.contents = null;

		this.f_writingMode = null;

		this.text_length = null;

		this.move_command = null;

		this.f_character_spacing = null;

		this.f_end_font_size = null;

		this.space_width = null;

		this.f_x1 = null;

		this.colorTag = null;

		this.f_x2 = null;

		this.f_y1 = null;

		this.f_y2 = null;
	}
}
