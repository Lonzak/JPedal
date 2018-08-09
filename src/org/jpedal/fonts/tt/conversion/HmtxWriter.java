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
 * HmtxWriter.java
 * ---------------
 */
package org.jpedal.fonts.tt.conversion;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.jpedal.fonts.glyph.PdfJavaGlyphs;
import org.jpedal.fonts.tt.Hmtx;

public class HmtxWriter extends Hmtx implements FontTableWriter {

	private static final long serialVersionUID = 514149709527353256L;
	int glyphCount;
	int[] advanceWidth, leftSideBearing;

	public HmtxWriter(PdfJavaGlyphs glyphs, int[] advanceWidth, int[] lsbs) {
		this.glyphCount = glyphs.getGlyphCount();
		this.advanceWidth = advanceWidth;

		if (lsbs == null) this.leftSideBearing = new int[65535];
		else this.leftSideBearing = lsbs;
	}

	@Override
	public byte[] writeTable() throws IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		for (int i = 0; i < this.glyphCount; i++) {
			bos.write(FontWriter.setNextUint16(this.advanceWidth[i]));
			bos.write(FontWriter.setNextInt16(this.leftSideBearing[i]));
		}

		bos.flush();
		bos.close();

		return bos.toByteArray();
	}

	@Override
	public int getIntValue(int key) {
		return 0;
	}
}
