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
 * Loca.java
 * ---------------
 */
package org.jpedal.fonts.tt;

import org.jpedal.utils.LogWriter;

public class Loca extends Table {

	private static final long serialVersionUID = -3586162676690304168L;

	/** points to location of glyph programs */
	int[] glyphIndexStart;

	boolean isCorrupted = false;

	public int format;
	public int glyphCount;
	public int glyfTableLength;

	public Loca(FontFile2 currentFontFile, int glyphCount, int format) {

		// handles super call in LocaWriter
		if (currentFontFile == null) return;

		this.format = format;
		this.glyphCount = glyphCount;
		this.glyfTableLength = currentFontFile.getOffset(FontFile2.GLYF);

		// LogWriter.writeMethod("{readLocaTable}", 0);

		// move to start and check exists
		int startPointer = currentFontFile.selectTable(FontFile2.LOCA);

		int i;

		int locaLength = currentFontFile.getOffset(FontFile2.LOCA);

		this.glyphIndexStart = new int[glyphCount + 1];

		// read 'head' table
		if (startPointer != 0) {

			this.glyphIndexStart[0] = 0;
			// long version
			if(format==1 || format==256){
				if (LogWriter.isOutput() && (locaLength / 4) != (glyphCount + 1)) LogWriter.writeLog("Incorrect length");

				for (i = 0; i < glyphCount; i++)
					this.glyphIndexStart[i] = currentFontFile.getNextUint32();

			}
			else { // short
				//Lonzak: Commented buggy code which caused bold text to not show up in pdf2image 
//				if ((locaLength / 2) != (glyphCount + 1)) {
//
//					if (LogWriter.isOutput()) LogWriter.writeLog("Incorrect length");
//
//					this.isCorrupted = true;
//				}
//				else {
					for (i = 0; i < glyphCount; i++)
						this.glyphIndexStart[i] = (currentFontFile.getNextUint16() * 2);

				}
			//}

			this.glyphIndexStart[glyphCount] = this.glyfTableLength;
		}
	}

	public int[] getIndices() {
		return this.glyphIndexStart;
	}

	public boolean isCorrupted() {
		return this.isCorrupted;
	}

	public int getFormat() {
		return this.format;
	}

	public int getGlyphCount() {
		return this.glyphCount;
	}

	public int getGlyfTableLength() {
		return this.glyfTableLength;
	}
}
