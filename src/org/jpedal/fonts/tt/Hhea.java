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
 * Hhea.java
 * ---------------
 */
package org.jpedal.fonts.tt;

public class Hhea extends Table {

	private static final long serialVersionUID = 4341523780624603429L;
	public static final int VERSION = 0;
	public static final int ASCENDER = 1;
	public static final int DESCENDER = 2;
	public static final int LINEGAP = 3;
	public static final int ADVANCEWIDTHMAX = 4;
	public static final int MINIMUMLEFTSIDEBEARING = 5;
	public static final int MINIMUMRIGHTSIDEBEARING = 6;
	public static final int XMAXEXTENT = 7;
	public static final int CARETSLOPERISING = 8;
	public static final int CARETSLOPERUN = 9;
	public static final int CARETOFFSET = 10;
	public static final int METRICDATAFORMAT = 11;
	public static final int NUMBEROFMETRICS = 12;

	private int version = 65536;
	private int ascender = 1;
	private int descender = -1;
	private int lineGap = 0;
	private int advancedWidthMax = 0;
	private int minimumLeftSideBearing = 0;
	private int minimumRightSideBearing = 0;
	private int xMaxExtent = 0;
	private int caretSlopeRise = 0;
	private int caretSlopeRun = 0;
	private int caretOffset = 0;
	private int metricDataFormat = 0;
	private int numberOfHMetrics = 0;

	public Hhea(FontFile2 currentFontFile) {

		// move to start and check exists
		int startPointer = currentFontFile.selectTable(FontFile2.HHEA);

		// read 'head' table
		if (startPointer != 0) {

			this.version = currentFontFile.getNextUint32(); // version 65536
			this.ascender = currentFontFile.getFWord();// ascender 1972
			this.descender = currentFontFile.getFWord();// descender -483
			this.lineGap = currentFontFile.getFWord();// lineGap 0
			this.advancedWidthMax = currentFontFile.readUFWord();// advanceWidthMax 2513
			this.minimumLeftSideBearing = currentFontFile.getFWord();// minLeftSideBearing -342
			this.minimumRightSideBearing = currentFontFile.getFWord();// minRightSideBearing -340
			this.xMaxExtent = currentFontFile.getFWord();// xMaxExtent 2454
			this.caretSlopeRise = currentFontFile.getNextInt16();// caretSlopeRise 1
			this.caretSlopeRun = currentFontFile.getNextInt16();// caretSlopeRun 0
			this.caretOffset = currentFontFile.getFWord();// caretOffset 0

			// reserved values
			for (int i = 0; i < 4; i++)
				currentFontFile.getNextUint16(); // 0

			this.metricDataFormat = currentFontFile.getNextInt16();// metricDataFormat
			this.numberOfHMetrics = currentFontFile.getNextUint16(); // 261

		}
	}

	public Hhea() {}

	public int getNumberOfHMetrics() {
		return this.numberOfHMetrics;
	}

	/**
	 * @param key value (required)
	 * @return  the value of given int number of variable
	 */
	public int getIntValue(int key) {
		switch (key) {
			case VERSION:
				return this.version;
			case ASCENDER:
				return this.ascender;
			case DESCENDER:
				return this.descender;
			case LINEGAP:
				return this.lineGap;
			case ADVANCEWIDTHMAX:
				return this.advancedWidthMax;
			case MINIMUMLEFTSIDEBEARING:
				return this.minimumLeftSideBearing;
			case MINIMUMRIGHTSIDEBEARING:
				return this.minimumRightSideBearing;
			case XMAXEXTENT:
				return this.xMaxExtent;
			case CARETSLOPERISING:
				return this.caretSlopeRise;
			case CARETSLOPERUN:
				return this.caretSlopeRun;
			case CARETOFFSET:
				return this.caretOffset;
			case METRICDATAFORMAT:
				return this.metricDataFormat;
			case NUMBEROFMETRICS:
				return this.numberOfHMetrics;
			default:
				return 0;
		}
	}

}
