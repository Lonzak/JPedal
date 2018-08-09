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
 * SearchList.java
 * ---------------
 */
package org.jpedal.examples.viewer.gui.swing;

import java.awt.event.MouseEvent;
import java.util.Map;

import javax.swing.DefaultListModel;

import org.jpedal.utils.Messages;

/** used by search function ro provide page number as tooltip */
public class SearchList extends javax.swing.JList {

	private static final long serialVersionUID = -8008130967853983390L;
	private Map textPages;
	private Map textAreas;
	private String pageStr = "Page";
	private int Length = 0;

	public final static int NO_RESULTS_FOUND = 1;
	public final static int SEARCH_COMPLETE_SUCCESSFULLY = 2;
	public final static int SEARCH_INCOMPLETE = 4;
	public final static int SEARCH_PRODUCED_ERROR = 8;

	private int status = NO_RESULTS_FOUND;

	/**
	 * @deprecated Constructor that will set up the search list designed to be manipulated via the simple viewer user interface.
	 * @param listModel
	 *            :: List of teasers
	 * @param textPages
	 *            :: Map to find the page the result is on
	 */
	@Deprecated
	public SearchList(DefaultListModel listModel, Map textPages) {
		super(listModel);

		this.Length = listModel.capacity();

		this.textPages = textPages;
		this.pageStr = Messages.getMessage("PdfViewerSearch.Page") + ' ';
	}

	/**
	 * Constructor that will set up the search list and store highlight areas internally so the search highlights can be manipulated externally.
	 * 
	 * @param listModel
	 *            :: List of teasers
	 * @param textPages
	 *            :: Map of key to page of result
	 * @param textAreas
	 *            :: Map of key to highlight area
	 */
	public SearchList(DefaultListModel listModel, Map textPages, Map textAreas) {
		super(listModel);

		this.Length = listModel.capacity();

		this.textPages = textPages;
		this.textAreas = textAreas;
		this.pageStr = Messages.getMessage("PdfViewerSearch.Page") + ' ';
	}

	@Override
	public String getToolTipText(MouseEvent event) {

		int index = this.locationToIndex(event.getPoint());

		Object page = this.textPages.get(index);

		if (page != null) return this.pageStr + page;
		else return null;
	}

	public Map getTextPages() {
		return this.textPages;
	}

	public Map textAreas() {
		return this.textAreas;
	}

	/**
	 * Find out the current amount of results found
	 * 
	 * @return the amount of search results found
	 */
	public int getResultCount() {
		return this.textAreas.size();
	}

	/**
	 * Returns the max capcity fo the list
	 * 
	 * @return length of the list
	 */
	public int getLength() {
		return this.Length;
	}

	public void setLength(int length) {
		this.Length = length;
	}

	/**
	 * Get status of results.
	 * 
	 * @return a value corresponding to one of the following public final static int NO_RESULTS_FOUND = -1; public final static int
	 *         SEARCH_COMPLETE_SUCCESSFULLY = 0; public final static int SEARCH_INCOMPLETE = 1; public final static int SEARCH_PRODUCED_ERROR = 2;
	 */
	public int getStatus() {
		return this.status;
	}

	public void setStatus(int status) {
		this.status = status;
	}
}
