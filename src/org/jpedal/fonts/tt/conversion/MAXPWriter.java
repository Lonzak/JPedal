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
 * MAXPWriter.java
 * ---------------
 */
package org.jpedal.fonts.tt.conversion;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.jpedal.fonts.glyph.PdfJavaGlyphs;
import org.jpedal.fonts.tt.Maxp;

public class MAXPWriter extends Maxp implements FontTableWriter {

	private static final long serialVersionUID = 7571895583034441888L;
	int glyphCount = 0;

	/**
	 * used to turn Ps into OTF
	 * 
	 * @param glyphs
	 */
	public MAXPWriter(PdfJavaGlyphs glyphs) {

		this.glyphCount = glyphs.getGlyphCount();
	}

	@Override
	public byte[] writeTable() throws IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		bos.write(FontWriter.setNextUint32(20480)); // revision 5000 hex
		bos.write(FontWriter.setNextUint16(this.glyphCount)); // number of glyphs

		bos.flush();
		bos.close();

		return bos.toByteArray();
	}

	@Override
	public int getIntValue(int key) {
		return 0;
	}
}
