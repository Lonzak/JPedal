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
 * Decoder.java
 * ---------------
 */

package org.jpedal.parser;

import org.jpedal.fonts.PdfFont;
import org.jpedal.objects.TextState;

public interface Decoder {

	Object getObjectValue(int textAreas);

	void setReturnText(boolean returnText);

	void reset();

	void setParameters(boolean pageContent, boolean renderPage, int renderMode, int extractionMode, boolean printing);

	void setFont(PdfFont currentFontData);

	int processToken(TextState currentTextState, int commandID, int startCommand, int dataPointer);

	boolean isTTHintingRequired();

	void setIntValue(int key, int value);

	void setBooleanValue(int renderDirectly, boolean value);

	void setTextState(TextState currentTextState);
}
