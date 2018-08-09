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
 * Hmtx.java
 * ---------------
 */
package org.jpedal.fonts.tt;

import org.jpedal.utils.LogWriter;

public class Hmtx extends Table {

	private static final long serialVersionUID = 79254234215177405L;
	private int[] hMetrics;
	private short[] leftSideBearing;
	private float scaling = 1f / 1000f;

	public Hmtx(FontFile2 currentFontFile, int glyphCount, int metricsCount, int maxAdvance) {

		this.scaling = maxAdvance;

		if (metricsCount < 0) metricsCount = -metricsCount;

		// move to start and check exists
		int startPointer = currentFontFile.selectTable(FontFile2.HMTX);

		int lsbCount = glyphCount - metricsCount;

		// System.out.println("start="+Integer.toHexString(startPointer)+" lsbCount="+lsbCount+" glyphCount="+glyphCount+" metricsCount="+metricsCount);

		this.hMetrics = new int[glyphCount];
		this.leftSideBearing = new short[glyphCount];

		int currentMetric = 0;

		// read 'head' table
		if (startPointer == 0) {
			if (LogWriter.isOutput()) LogWriter.writeLog("No Htmx table found");
		}
		else
			if (lsbCount < 0) {
				if (LogWriter.isOutput()) LogWriter.writeLog("Invalid Htmx table found");
			}
			else {
				int i;
				for (i = 0; i < metricsCount; i++) {
					currentMetric = currentFontFile.getNextUint16();
					this.hMetrics[i] = currentMetric;
					this.leftSideBearing[i] = currentFontFile.getNextInt16();
					// System.out.println(i+"="+hMetrics[i]+" "+leftSideBearing[i]);
				}

				// workout actual number of values in table
				int tableLength = currentFontFile.getTableSize(FontFile2.HMTX);
				int lsbBytes = tableLength - (i * 4); // each entry above used 4 bytes
				lsbCount = (lsbBytes / 2); // each entry contains 2 bytes

				// read additional lsb entries
				for (int j = i; j < lsbCount; j++) {

					this.hMetrics[j] = currentMetric;
					this.leftSideBearing[j] = currentFontFile.getFWord();
					// System.out.println((j)+" "+leftSideBearing[j]);
				}
			}
	}

	public Hmtx() {}

	// used by OTF code for aligning CFF font data
	public short getRAWLSB(int i) {
		if (this.leftSideBearing == null || i >= this.leftSideBearing.length) return 0;
		else return this.leftSideBearing[i];
	}

	public short getLeftSideBearing(int i) {
		if (i < this.hMetrics.length) {
			return (short) (this.hMetrics[i] & 0xffff);
		}
		else
			if (this.leftSideBearing == null) {
				return 0;
			}
			else {
				try {
					return this.leftSideBearing[i - this.hMetrics.length];
				}
				catch (Exception e) {
					return 0;
				}
			}
	}

	public float getAdvanceWidth(int i) {
		/**
		 * if (i < hMetrics.length) { return hMetrics[i] >> 16; } else { return hMetrics[hMetrics.length - 1] >> 16; }
		 */
		return ((this.hMetrics[i] - getLeftSideBearing(i)) / this.scaling);
	}

	public float getWidth(int i) {
		/**
		 * if (i < hMetrics.length) { return hMetrics[i] >> 16; } else { return hMetrics[hMetrics.length - 1] >> 16; }
		 */
		float w = this.hMetrics[i];

		return ((w) / this.scaling);
	}

	public float getUnscaledWidth(int i) {
		return this.hMetrics[i];
	}
}
