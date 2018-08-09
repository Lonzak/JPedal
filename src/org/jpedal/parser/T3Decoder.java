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
 * T3Decoder.java
 * ---------------
 */
package org.jpedal.parser;

public class T3Decoder extends BaseDecoder {

	/** max width of type3 font */
	int T3maxWidth;
	int T3maxHeight;

	// public void setParameters(boolean isPageContent, boolean renderPage, int renderMode, int extractionMode, boolean isPrinting) {

	// super.setParameters(isPageContent,renderPage, renderMode, extractionMode);

	// }

	/** shows if t3 glyph uses internal colour or current colour */
	boolean ignoreColors = false;

	private void d1(float urX, float llX, float wX, float urY, float llY, float wY) {

		// flag to show we use text colour or colour in stream
		this.ignoreColors = true;

		/**/
		// not fully implemented
		// float urY = Float.parseFloat(generateOpAsString(0,characterStream));
		// float urX = Float.parseFloat(generateOpAsString(1,characterStream));
		// float llY = Float.parseFloat(generateOpAsString(2,characterStream));
		// float llX = Float.parseFloat(generateOpAsString(3,characterStream));
		// float wY = Float.parseFloat(generateOpAsString(4,characterStream));
		// float wX = Float.parseFloat(generateOpAsString(5,characterStream));

		// this.minX=(int)llX;
		// this.minY=(int)llY;

		// currentGraphicsState = new GraphicsState(0,0);/*remove values on contrutor%%*/

		// setup image to draw on
		// current.init((int)(wX),(int)(urY-llY+1));

		// wH=urY;
		// wW=llX;

		this.T3maxWidth = (int) wX;
		if (wX == 0) this.T3maxWidth = (int) (llX - urX);
		else this.T3maxWidth = (int) wX; // Float.parseFloat(generateOpAsString(5,characterStream));

		this.T3maxHeight = (int) wY;
		if (wY == 0) this.T3maxHeight = (int) (urY - llY);
		else this.T3maxHeight = (int) wY; // Float.parseFloat(generateOpAsString(5,characterStream));
	}

	// //////////////////////////////////////////////////////////////////////
	private void d0(int w, int y) {

		// flag to show we use text colour or colour in stream
		this.ignoreColors = false;

		// float glyphX = Float.parseFloat((String) operand.elementAt(0));
		this.T3maxWidth = w;
		this.T3maxHeight = y;

		// setup image to draw on
		// current.init((int)glyphX,(int)glyphY);
	}

	public void processToken(int commandID) {
		switch (commandID) {

			case Cmd.d0:
				d0((int) this.parser.parseFloat(0), (int) this.parser.parseFloat(1));
				break;

			case Cmd.d1:
				d1(this.parser.parseFloat(1), this.parser.parseFloat(3), this.parser.parseFloat(5), this.parser.parseFloat(0),
						this.parser.parseFloat(2), this.parser.parseFloat(4));
				break;
		}
	}

}
