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
 * Maxp.java
 * ---------------
 */
package org.jpedal.fonts.tt;

public class Maxp extends Table {

	private static final long serialVersionUID = -9050290510979983537L;
	private int numGlyphs = 0, maxPoints = 0, maxContours = 0, maxTwilightPoints = 0, maxStorage = 0;

	public Maxp(FontFile2 currentFontFile) {

		// LogWriter.writeMethod("{readMapxTable}", 0);
		init(currentFontFile);
	}

	public Maxp() {}

	private void init(FontFile2 currentFontFile) {
		// move to start and check exists
		int startPointer = currentFontFile.selectTable(FontFile2.MAXP);

		// read 'head' table
		if (startPointer != 0) {

			currentFontFile.getNextUint32(); // id
			this.numGlyphs = currentFontFile.getNextUint16();
			this.maxPoints = currentFontFile.getNextUint16();
			this.maxContours = currentFontFile.getNextUint16();
			currentFontFile.getNextUint16(); // maxComponentPoints
			currentFontFile.getNextUint16(); // maxComponentContours
			currentFontFile.getNextUint16(); // maxZones
			this.maxTwilightPoints = currentFontFile.getNextUint16();
			this.maxStorage = currentFontFile.getNextUint16();
			currentFontFile.getNextUint16(); // maxFunctionDefs
			currentFontFile.getNextUint16(); // maxInstructionDefs
			currentFontFile.getNextUint16(); // maxStackElements
			currentFontFile.getNextUint16(); // maxSizeOfInstructions
			currentFontFile.getNextUint16(); // maxComponentElements
			currentFontFile.getNextUint16(); // maxComponentDepth
		}
	}

	public int getGlyphCount() {
		return this.numGlyphs;
	}

	public int getMaxPoints() {
		return this.maxPoints;
	}

	public int getMaxTwilightPoints() {
		return this.maxTwilightPoints;
	}

	public int getMaxStorage() {
		return this.maxStorage;
	}

	public int getMaxContours() {
		return this.maxContours;
	}
}
