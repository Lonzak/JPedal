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
 * Glyf.java
 * ---------------
 */
package org.jpedal.fonts.tt;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Glyf extends Table {

	private static final long serialVersionUID = -1163555956748847206L;

	/** holds mappings for drawing the glpyhs */
	private Map charStrings = new HashMap();

	private int glyfCount = 0;

	/** holds list of empty glyphs */
	private Map emptyCharStrings = new HashMap();
	private byte[] glyphTable;

	public Glyf(FontFile2 currentFontFile, int glyphCount, int[] glyphIndexStart) {

		// save so we can access
		this.glyfCount = glyphCount;

		// move to start and check exists
		int startPointer = currentFontFile.selectTable(FontFile2.LOCA);

		// read table
		if (startPointer != 0) {

			// read each gyf
			for (int i = 0; i < glyphCount; i++) {

				// just store in lookup table or flag as zero length
				if ((glyphIndexStart[i] == glyphIndexStart[i + 1])) {
					this.charStrings.put(i, -1);
					this.emptyCharStrings.put(i, "x");
				}
				else {
					this.charStrings.put(i, glyphIndexStart[i]);
				}
			}

			// read the actual glyph data
			this.glyphTable = currentFontFile.getTableBytes(FontFile2.GLYF);

		}
	}

	public boolean isPresent(int glyph) {

		Integer key = glyph;

		Object value = this.charStrings.get(key);

		return value != null && this.emptyCharStrings.get(key) == null;
	}

	public int getCharString(int glyph) {

		Object value = this.charStrings.get(glyph);

		if (value == null) return glyph;
		else return (Integer) value;
	}

	public byte[] getTableData() {
		return this.glyphTable;
	}

	public int getGlypfCount() {
		return this.glyfCount;
	}

	/** assume identify and build data needed for our OTF converter */
	public Map buildCharStringTable(int enc) {

		Map returnStrings = new HashMap();

		for (Object key : this.charStrings.keySet()) {
			if (!this.emptyCharStrings.containsKey(key)) {
				returnStrings.put(key, key);
			}
		}
		return returnStrings;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Glyf [");
		if (charStrings != null) {
			builder.append("charStrings=");
			builder.append(charStrings);
			builder.append(", ");
		}
		builder.append("glyfCount=");
		builder.append(glyfCount);
		builder.append(", ");
		if (emptyCharStrings != null) {
			builder.append("emptyCharStrings=");
			builder.append(emptyCharStrings);
			builder.append(", ");
		}
		if (glyphTable != null) {
			builder.append("glyphTable=");
			builder.append(new String(glyphTable));
		}
		builder.append("]");
		return builder.toString();
	}
}