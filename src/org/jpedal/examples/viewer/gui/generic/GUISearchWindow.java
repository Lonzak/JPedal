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
 * GUISearchWindow.java
 * ---------------
 */
package org.jpedal.examples.viewer.gui.generic;

import java.awt.Component;
import java.util.Map;

import org.jpedal.PdfDecoder;
import org.jpedal.examples.viewer.Values;
import org.jpedal.examples.viewer.gui.swing.SearchList;

/** abstract level of search window */
public interface GUISearchWindow {

	// Varible added to allow multiple search style to be implemented
	int style = 0;

	void find(PdfDecoder decode_pdf, Values values);

	void findWithoutWindow(PdfDecoder decode_pdf, Values values, int searchType, boolean listOfTerms, boolean singlePageOnly, String searchValue);

	void grabFocusInInput();

	boolean isSearchVisible();

	void init(final PdfDecoder dec, final Values values);

	void removeSearchWindow(boolean justHide);

	void resetSearchWindow();

	SearchList getResults();

	SearchList getResults(int page);

	Map getTextRectangles();

	Component getContentPanel();

	int getStyle();

	void setStyle(int i);

	boolean isSearching();

	public int getFirstPageWithResults();

	public void setWholeWords(boolean wholeWords);

	public void setCaseSensitive(boolean caseSensitive);

	public void setMultiLine(boolean multiLine);

}
