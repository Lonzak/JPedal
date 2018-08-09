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
 * HheaWriter.java
 * ---------------
 */
package org.jpedal.fonts.tt.conversion;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.jpedal.fonts.glyph.PdfJavaGlyphs;
import org.jpedal.fonts.tt.Hhea;

public class HheaWriter extends Hhea implements FontTableWriter {

	private static final long serialVersionUID = -7014279475817569381L;
	private int glyphCount;
	private double xMaxExtent;
	private double minRightSideBearing;
	private double minLeftSideBearing;
	private double advanceWidthMax;
	private double lowestDescender;
	private double highestAscender;

	public HheaWriter(PdfJavaGlyphs glyphs, double xMaxExtent, double minRightSideBearing, double minLeftSideBearing, double advanceWidthMax,
			double lowestDescender, double highestAscender) {
		this.glyphCount = glyphs.getGlyphCount();
		this.xMaxExtent = xMaxExtent;
		this.minRightSideBearing = minRightSideBearing;
		this.minLeftSideBearing = minLeftSideBearing;
		this.advanceWidthMax = advanceWidthMax;
		this.lowestDescender = lowestDescender;
		this.highestAscender = highestAscender;
	}

	@Override
	public byte[] writeTable() throws IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		bos.write(FontWriter.setNextUint32(65536)); // version 65536

		// Designer specified values
		bos.write(FontWriter.setFWord((int) this.highestAscender)); // ascender
		bos.write(FontWriter.setFWord((int) this.lowestDescender)); // descender
		bos.write(FontWriter.setFWord(0)); // lineGap 0

		// Calculated values
		bos.write(FontWriter.setUFWord((int) this.advanceWidthMax)); // advanceWidthMax
		bos.write(FontWriter.setFWord((int) this.minLeftSideBearing)); // minLeftSideBearing
		bos.write(FontWriter.setFWord((int) this.minRightSideBearing)); // minRightSideBearing
		bos.write(FontWriter.setFWord((int) this.xMaxExtent)); // xMaxExtent

		// Italicise caret?
		bos.write(FontWriter.setNextInt16(1)); // caretSlopeRise 1
		bos.write(FontWriter.setNextInt16(0)); // caretSlopeRun 0
		bos.write(FontWriter.setFWord(0)); // caretOffset 0

		// reserved values
		for (int i = 0; i < 4; i++)
			bos.write(FontWriter.setNextUint16(0)); // 0

		bos.write(FontWriter.setNextInt16(0));// metricDataFormat
		bos.write(FontWriter.setNextUint16(this.glyphCount)); // count

		bos.flush();
		bos.close();

		return bos.toByteArray();
	}

	@Override
	public int getIntValue(int key) {
		return 0;
	}
}
