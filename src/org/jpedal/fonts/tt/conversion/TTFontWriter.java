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
 * TTFontWriter.java
 * ---------------
 */
package org.jpedal.fonts.tt.conversion;

import org.jpedal.fonts.tt.FontFile2;
import org.jpedal.fonts.tt.Maxp;
import org.jpedal.utils.LogWriter;

public class TTFontWriter extends FontWriter {

	private static final long serialVersionUID = -6569687309537259643L;

	byte[] rawFontData;

	byte[] cmap = null;

	public TTFontWriter(byte[] rawFontData) {

		super(rawFontData);

		// get number of glyphs
		Maxp currentMaxp = new Maxp(new FontFile2(rawFontData));
		this.glyphCount = currentMaxp.getGlyphCount();

		this.rawFontData = rawFontData;
	}

	@Override
	void readTables() {

		this.cmap = null;
		TTFontWriter fontData = new TTFontWriter(this.rawFontData);
		org.jpedal.fonts.tt.conversion.CMAPWriter orginalCMAP = new org.jpedal.fonts.tt.conversion.CMAPWriter(fontData,
				fontData.selectTable(FontFile2.CMAP), null);

		try {
			this.cmap = orginalCMAP.writeTable();
		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}
	}

	@Override
	public byte[] getTableBytes(int tableID) {

		if (tableID == CMAP) {

			return this.cmap;

		}
		else return super.getTableBytes(tableID);
	}
}
