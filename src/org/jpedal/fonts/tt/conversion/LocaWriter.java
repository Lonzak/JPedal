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
 * LocaWriter.java
 * ---------------
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jpedal.fonts.tt.conversion;

import java.io.IOException;

import org.jpedal.fonts.PdfFont;
import org.jpedal.fonts.glyph.PdfJavaGlyphs;
import org.jpedal.fonts.tt.FontFile2;
import org.jpedal.fonts.tt.Loca;

/**
 * 
 * @author markee
 */
class LocaWriter extends Loca implements FontTableWriter {

	private static final long serialVersionUID = -5724043377910366940L;
	String fontName;
	private PdfFont originalFont;
	FontFile2 orginTTTables;

	LocaWriter(String name, PdfFont pdfFont, PdfFont originalFont, PdfJavaGlyphs glyphs, String[] glyphList) {
		super(null, 0, 0);

		this.originalFont = originalFont;
	}

	@Override
	public byte[] writeTable() throws IOException {

		Loca origTable = (Loca) this.originalFont.getGlyphData().getTable(FontFile2.LOCA);

		/**
		 * work out length
		 */
		this.format = origTable.getFormat();
		this.glyphCount = origTable.getGlyphCount();
		this.glyfTableLength = origTable.getGlyfTableLength();

		return null;
	}

	@Override
	public int getIntValue(int key) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

}
