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
 * PdfGroupingAlgorithms.java
 * ---------------
 */
package org.jpedal.grouping;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jpedal.color.GenericColorSpace;
import org.jpedal.exception.PdfException;
import org.jpedal.objects.PdfData;
import org.jpedal.objects.PdfPageData;
import org.jpedal.utils.Fonts;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.Sorts;
import org.jpedal.utils.Strip;
import org.jpedal.utils.repositories.Vector_Float;
import org.jpedal.utils.repositories.Vector_Int;
import org.jpedal.utils.repositories.Vector_Object;
import org.jpedal.utils.repositories.Vector_Rectangle;
import org.jpedal.utils.repositories.Vector_String;

/**
 * Applies heuristics to unstructured PDF text to create content
 */
public class PdfGroupingAlgorithms {

	private boolean includeHTMLtags = false;

	public static final int USER_DEFINED_LIST_ONLY = 0;
	public static final int SURROUND_BY_ANY_PUNCTUATION = 1;

	private static String SystemSeparator = System.getProperty("line.separator");

	// public PdfGroupingAlgorithms() {}

	/** ==============START OF ARRAYS================ */
	/**
	 * content is stored in a set of arrays. We have tried various methods (ie create composite object, etc) and none are entirely satisfactory. The
	 * beauty of this method is speed.
	 */

	/**
	 * flag to show this item has been merged into another and should be ignored. This allows us to repeat operations on live elements without lots of
	 * deleting.
	 */
	private boolean[] isUsed;

	/** co-ords of object (x1,y1 is top left) */
	private float[] f_x1, f_x2, f_y1, f_y2;

	/** track if we removed space from end */
	private boolean[] hadSpace;

	/** hold colour info */
	private String[] f_colorTag;

	/** hold writing mode */
	private int[] writingMode;

	/** hold move type */
	private int[] moveType;

	/** font sizes in pixels */
	private int[] fontSize;

	/** amount of space a space uses in this font/size */
	private float[] spaceWidth;

	/** actual text */
	private StringBuilder[] content;

	/** raw number of text characters */
	private int[] textLength;

	/** ==============END OF ARRAYS================ */

	/**
	 * handle on page data object. We extract data from this into local arrays and return grouped content into object at end. This is done for speed.
	 */
	private PdfData pdf_data;

	PdfPageData pageData;

	/** flag to show if output for table is CSV or XHTML */
	private boolean isXHTML = true;

	/** slot to insert next value - used when we split fragments for table code */
	private int nextSlot;

	/** vertical breaks for table calculation */
	private Vector_Int lineBreaks = new Vector_Int();

	/** holds details as we scan lines for table */
	private Vector_Object lines;

	/** lookup table used to sort into correct order for table */
	private Vector_Int lineY2;

	/**
	 * marker char used in content (we bury location for each char so we can split)
	 */
	private static final String MARKER = PdfData.marker;
	public static char MARKER2 = MARKER.charAt(0);

	/** counters for cols and rows and pointer to final object we merge into */
	private int max_rows = 0, master = 0;

	/** flag to show color info is being extracted */
	private boolean colorExtracted = false;

	/** used to calculate correct order for table lines */
	private int[] line_order;

	/** amount we resize arrays holding content with if no space */
	private final static int increment = 100;

	public static boolean useUnrotatedCoords;

	/** end points if text located */
	private float[] endPoints;

	/** flag to show if tease created on findText */
	private boolean includeTease;

	/** teasers for findtext */
	private String[] teasers;

	private List multipleTermTeasers = new ArrayList();

	private boolean usingMultipleTerms = false;

	private boolean isXMLExtraction = true;

	/*
	 * Variables to allow cross line search results
	 */
	/** Value placed between result areas to show they are part of the same result */
	private int linkedSearchAreas = -101;

	/** create a new instance, passing in raw data */
	public PdfGroupingAlgorithms(PdfData pdf_data, PdfPageData pageData, boolean isXMLExtraction) {
		this.pdf_data = pdf_data;
		this.pageData = pageData;
		this.isXMLExtraction = isXMLExtraction;
		this.colorExtracted = pdf_data.isColorExtracted();
	}

	public static void setSeparator(String sep) {
		SystemSeparator = sep;
	}

	/**
	 * workout if we should use space, CR or no separator when joining lines
	 */
	static final private String getLineDownSeparator(StringBuilder rawLine1, StringBuilder rawLine2, boolean isXMLExtraction) {

		String returnValue = " "; // space is default

		boolean hasUnderline = false;

		/** get 2 lines without any XML or spaces so we can look at last char */
		StringBuilder line1, line2;
		if (isXMLExtraction) {
			line1 = Strip.stripXML(rawLine1, isXMLExtraction);
			line2 = Strip.stripXML(rawLine2, isXMLExtraction);
		}
		else {
			line1 = Strip.trim(rawLine1);
			line2 = Strip.trim(rawLine2);
		}

		/** get lengths and if appropriate perform tests */
		int line1Len = line1.length();
		int line2Len = line2.length();
		// System.out.println(line1Len+" "+line2Len);
		if ((line1Len > 1) && (line2Len > 1)) {

			/** get chars to test */
			char line1Char2 = line1.charAt(line1Len - 1);
			char line1Char1 = line1.charAt(line1Len - 2);
			char line2Char1 = line2.charAt(0);
			char line2Char2 = line2.charAt(1);

			// deal with hyphenation first - ignore unless :- or space-
			String hyphen_values = "";
			if (hyphen_values.indexOf(line1Char2) != -1) {
				returnValue = ""; // default of nothing
				if (line1Char1 == ':') returnValue = "\n";
				if (line1Char2 == ' ') returnValue = " ";

				// paragraph breaks if full stop and next line has ascii char or Capital Letter
			}
			else
				if (((line1Char1 == '.') | (line1Char2 == '.'))
						& (Character.isUpperCase(line2Char1) | (line2Char1 == '&') | Character.isUpperCase(line2Char2) | (line2Char2 == '&'))) {
					if (isXMLExtraction) returnValue = "<p></p>\n";
					else returnValue = "\n";
				}

		}

		// add an underline if appropriate
		if (hasUnderline) {
			if (isXMLExtraction) returnValue = returnValue + "<p></p>\n";
			else returnValue = returnValue + '\n';
		}

		return returnValue;
	}

	/**
	 * remove shadows from text created by double printing of text and drowned items where text inside other text
	 */
	private final void cleanupShadowsAndDrownedObjects(boolean avoidSpaces) {

		// get list of items
		int[] items = getUnusedFragments();
		int count = items.length;
		int c, n;
		String separator;
		float diff;

		// work through objects and eliminate shadows or roll together overlaps
		for (int p = 0; p < count; p++) {

			// master item
			c = items[p];

			// ignore used items
			if (this.isUsed[c] == false) {

				// work out mid point in text
				float midX = (this.f_x1[c] + this.f_x2[c]) / 2;
				float midY = (this.f_y1[c] + this.f_y2[c]) / 2;

				for (int p2 = p + 1; p2 < count; p2++) {

					// item to test against
					n = items[p2];
					if ((this.isUsed[n] == false) && (this.isUsed[c] == false)) {

						float fontDiff = this.fontSize[n] - this.fontSize[c];
						if (fontDiff < 0) fontDiff = -fontDiff;

						diff = (this.f_x2[n] - this.f_x1[n]) - (this.f_x2[c] - this.f_x1[c]);
						if (diff < 0) diff = -diff;

						/** stop spurious matches on overlapping text */
						if (fontDiff == 0 && (midX > this.f_x1[n]) && (midX < this.f_x2[n]) && (diff < 10) && (midY < this.f_y1[n])
								&& (midY > this.f_y2[n])) {

							this.isUsed[n] = true;

							// pick up drowned text items (item inside another)
						}
						else {

							boolean a_in_b = (this.f_x1[n] > this.f_x1[c]) && (this.f_x2[n] < this.f_x2[c]) && (this.f_y1[n] < this.f_y1[c])
									&& (this.f_y2[n] > this.f_y2[c]);
							boolean b_in_a = (this.f_x1[c] > this.f_x1[n]) && (this.f_x2[c] < this.f_x2[n]) && (this.f_y1[c] < this.f_y1[n])
									&& (this.f_y2[c] > this.f_y2[n]);

							// merge together
							if (a_in_b || b_in_a) {
								// get order right - bottom y2 underneath
								if (this.f_y2[c] > this.f_y2[n]) {
									separator = getLineDownSeparator(this.content[c], this.content[n], this.isXMLExtraction);
									if ((avoidSpaces == false) || (separator.indexOf(' ') == -1)) {
										merge(c, n, separator, true);
									}
								}
								else {
									separator = getLineDownSeparator(this.content[n], this.content[c], this.isXMLExtraction);
									if (!avoidSpaces || separator.indexOf(' ') == -1) {
										merge(n, c, separator, true);
									}
								}

								// recalculate as may have changed
								midX = (this.f_x1[c] + this.f_x2[c]) / 2;
								midY = (this.f_y1[c] + this.f_y2[c]) / 2;

							}
						}
					}
				}
			}
		}
	}

	/**
	 * general routine to see if we add a space between 2 text fragments
	 */
	final private String isGapASpace(int c, int l, float actualGap, boolean addMultiplespaceXMLTag, int writingMode) {
		String sep = "";
		float gap;

		// use smaller gap
		float gapA = this.spaceWidth[c] * this.fontSize[c];
		float gapB = this.spaceWidth[l] * this.fontSize[l];

		if (gapA > gapB) gap = gapB;
		else gap = gapA;

		gap = (actualGap / (gap / 1000));

		// Round values to closest full integer as float -> int conversion rounds down
		if (gap > 0.51f && gap < 1) gap = 1;

		int spaceCount = (int) gap;

		if (spaceCount > 0) sep = " ";

		/** add an XML tag to flag multiple spaces */
		if (spaceCount > 1 && addMultiplespaceXMLTag && writingMode == PdfData.HORIZONTAL_LEFT_TO_RIGHT) sep = " <SpaceCount space=\"" + spaceCount
				+ "\" />";

		return sep;
	}

	/**
	 * merge 2 text fragments together and update co-ordinates
	 */
	final private void merge(int m, int c, String separator, boolean moveFont) {

		// update co-ords
		if (this.f_x1[m] > this.f_x1[c]) this.f_x1[m] = this.f_x1[c];
		if (this.f_y1[m] < this.f_y1[c]) this.f_y1[m] = this.f_y1[c];
		if (this.f_x2[m] < this.f_x2[c]) this.f_x2[m] = this.f_x2[c];
		if (this.f_y2[m] > this.f_y2[c]) this.f_y2[m] = this.f_y2[c];

		if (this.isXMLExtraction) {
			String test = Fonts.fe;

			// add color tag if needed and changes
			if (this.colorExtracted) test = Fonts.fe + GenericColorSpace.ce;

			// move </Font> if needed and add separator
			if ((moveFont) && (this.content[m].toString().lastIndexOf(test) != -1)) {
				String master = this.content[m].toString();
				this.content[m] = new StringBuilder(master.substring(0, master.lastIndexOf(test)));
				this.content[m].append(separator);
				this.content[m].append(master.substring(master.lastIndexOf(test)));
			}
			else {
				this.content[m].append(separator);
			}

			// Only map out space if text length is longer than 1
			if (this.textLength[c] > 1 && this.content[m].toString().endsWith(" ")) {
				this.content[m].deleteCharAt(this.content[m].lastIndexOf(" "));
			}
			// use font size of second text (ie at end of merged text)
			this.fontSize[m] = this.fontSize[c];

			// Remove excess / redundent xml tags
			if (this.content[c].indexOf("<color") != -1 && this.content[m].indexOf("<color") != -1) {
				if (this.content[c].toString().startsWith(
						this.content[m].substring(this.content[m].lastIndexOf("<color"),
								this.content[m].indexOf(">", this.content[m].lastIndexOf("<color"))))
						&& this.content[m].lastIndexOf("</color>") + 7 == this.content[m].lastIndexOf(">")) {
					this.content[c].replace(this.content[c].indexOf("<color"), this.content[c].indexOf(">") + 1, "");
					this.content[m].replace(this.content[m].lastIndexOf("</color>"), this.content[m].lastIndexOf("</color>") + 8, "");
				}
			}

			if (this.content[c].indexOf("<font") != -1 && this.content[m].indexOf("<font") != -1) {
				if (this.content[c].toString().startsWith(
						this.content[m].substring(this.content[m].lastIndexOf("<font"),
								this.content[m].indexOf(">", this.content[m].lastIndexOf("<font"))))
						&& this.content[m].lastIndexOf("</font>") + 6 == this.content[m].lastIndexOf(">")) {
					this.content[c].replace(this.content[c].indexOf("<font"), this.content[c].indexOf(">") + 1, "");
					this.content[m].replace(this.content[m].lastIndexOf("</font>"), this.content[m].lastIndexOf("</font>") + 7, "");
				}
			}

			this.content[m] = this.content[m].append(this.content[c]);

			// track length of text less all tokens
			this.textLength[m] = this.textLength[m] + this.textLength[c];

			// set objects to null to flush and log as used
			this.isUsed[c] = true;
			this.content[c] = null;
		}
		else {

			// use font size of second text (ie at end of merged text)
			this.fontSize[m] = this.fontSize[c];

			// add together
			this.content[m] = this.content[m].append(separator).append(this.content[c]);

			// track length of text less all tokens
			this.textLength[m] = this.textLength[m] + this.textLength[c];

			// set objects to null to flush and log as used
			this.isUsed[c] = true;
			this.content[c] = null;
		}
	}

	/**
	 * remove width data we may have buried in data
	 */
	final private void removeEncoding() {

		// get list of items
		int[] items = getUnusedFragments();
		int current;

		// work through objects and eliminate shadows or roll together overlaps
		for (int item : items) {

			// master item
			current = item;

			// ignore used items and remove widths we hid in data
			if (this.isUsed[current] == false) this.content[current] = removeHiddenMarkers(current);
		}
	}

	/**
	 * put raw data into Arrays for quick merging breakup_fragments shows if we break on vertical lines and spaces
	 */
	final private void copyToArrays() {

		this.colorExtracted = this.pdf_data.isColorExtracted();

		int count = this.pdf_data.getRawTextElementCount();

		// local lists for faster access
		this.isUsed = new boolean[count];
		this.fontSize = new int[count];
		this.writingMode = new int[count];
		this.spaceWidth = new float[count];
		this.content = new StringBuilder[count];
		this.textLength = new int[count];

		this.f_x1 = new float[count];
		this.f_colorTag = new String[count];
		this.f_x2 = new float[count];
		this.f_y1 = new float[count];
		this.f_y2 = new float[count];
		this.moveType = new int[count];

		// set values
		for (int i = 0; i < count; i++) {
			this.content[i] = new StringBuilder(this.pdf_data.contents[i]);

			this.fontSize[i] = this.pdf_data.f_end_font_size[i];
			this.writingMode[i] = this.pdf_data.f_writingMode[i];
			this.f_x1[i] = this.pdf_data.f_x1[i];
			this.f_colorTag[i] = this.pdf_data.colorTag[i];
			this.f_x2[i] = this.pdf_data.f_x2[i];
			this.f_y1[i] = this.pdf_data.f_y1[i];
			this.f_y2[i] = this.pdf_data.f_y2[i];
			this.moveType[i] = this.pdf_data.move_command[i];

			this.spaceWidth[i] = this.pdf_data.space_width[i];
			this.textLength[i] = this.pdf_data.text_length[i];
		}
	}

	/**
	 * get list of unused fragments and put in list
	 */
	private int[] getUnusedFragments() {
		int total_fragments = this.isUsed.length;

		// get unused item pointers
		int ii = 0;
		int temp_index[] = new int[total_fragments];
		for (int i = 0; i < total_fragments; i++) {
			if (this.isUsed[i] == false) {
				temp_index[ii] = i;
				ii++;
			}
		}

		// put into correctly sized array
		int[] items = new int[ii];
		System.arraycopy(temp_index, 0, items, 0, ii);
		return items;
	}

	/**
	 * strip the hidden numbers of position we encoded into the data (could be coded to be faster by not using Tokenizer)
	 */
	private StringBuilder removeHiddenMarkers(int c) {

		// make sure has markers and ignore if not
		if (this.content[c].indexOf(MARKER) == -1) return this.content[c];

		// strip the markers
		StringTokenizer tokens = new StringTokenizer(this.content[c].toString(), MARKER, true);
		String temp;
		StringBuilder processedData = new StringBuilder();

		// with a token to make sure cleanup works
		while (tokens.hasMoreTokens()) {

			// strip encoding in data
			temp = tokens.nextToken(); // see if first marker

			if (temp.equals(MARKER)) {
				tokens.nextToken(); // point character starts
				tokens.nextToken(); // second marker
				tokens.nextToken(); // width
				tokens.nextToken(); // third marker

				// put back chars
				processedData = processedData.append(tokens.nextToken());

			}
			else processedData = processedData.append(temp);
		}

		return processedData;
	}

	/**
	 * sets if we include HTML in teasers (do we want this is <b>word</b> or this is word as teaser)
	 * 
	 * @param value
	 */
	public void setIncludeHTML(boolean value) {
		this.includeHTMLtags = value;
	}

	/**
	 * method to show data without encoding
	 */
	public static String removeHiddenMarkers(String contents) {

		// trap null
		if (contents == null) return null;

		// run though the string extracting our markers

		// make sure has markers and ignore if not
		if (!contents.contains(MARKER)) return contents;

		// strip the markers
		StringTokenizer tokens = new StringTokenizer(contents, MARKER, true);
		String temp_token=null;
		StringBuilder processed_data = new StringBuilder();
		boolean pushBackByOne = false;
		
		// with a token to make sure cleanup works
		while (tokens.hasMoreTokens()) {

		    if(!pushBackByOne) {
		        // encoding in data
		        temp_token = tokens.nextToken(); // see if first marker
		    }
		    else {
		        //skip fetching nextToken() since it was fetched in the last round
		        pushBackByOne=false;
		    }
			
			if (MARKER.equals(temp_token)) {
				tokens.nextToken(); // point character starts
				tokens.nextToken(); // second marker
				tokens.nextToken(); // width
				tokens.nextToken(); // third marker

                //Lonzak: There are PDFs which contain \0\0 (should be e.g. \0 \0 or \0c\0...) and then the lexer gets confused
                //thus do a push back
				String next = tokens.nextToken();
				if(next.equals(MARKER)) {
				    pushBackByOne=true;
				}
				else {
				    // put back chars
    				processed_data = processed_data.append(next);
				}
			}
			else {
			    // value
			    processed_data = processed_data.append(temp_token);
			}
		}
		return processed_data.toString();
	}

	/**
	 * Method to try and find vertical lines in close data (not as efficient as it could be)
	 * 
	 * @throws PdfException
	 */
	private void findVerticalLines(float minX, float minY, float maxX, float maxY, int currentWritingMode) throws PdfException {

		// hold counters on all x values
		HashMap xLines = new HashMap();

		// counter on most popular item
		int most_frequent = 0, count = this.pdf_data.getRawTextElementCount();
		float x1, x2, y1, y2;
		String raw;

		for (int i = 0; i < count; i++) {
			float currentX = 0, lastX;
			Integer intX;

			// extract values for data
			raw = this.pdf_data.contents[i];

			/**
			 * set pointers so left to right text
			 */
			if (currentWritingMode == PdfData.HORIZONTAL_LEFT_TO_RIGHT) {
				x1 = this.f_x1[i];
				x2 = this.f_x2[i];
				y1 = this.f_y1[i];
				y2 = this.f_y2[i];
			}
			else
				if (currentWritingMode == PdfData.HORIZONTAL_RIGHT_TO_LEFT) {
					x2 = this.f_x1[i];
					x1 = this.f_x2[i];
					y1 = this.f_y1[i];
					y2 = this.f_y2[i];
				}
				else
					if (currentWritingMode == PdfData.VERTICAL_BOTTOM_TO_TOP) {
						x1 = this.f_y1[i];
						x2 = this.f_y2[i];
						y1 = this.f_x2[i];
						y2 = this.f_x1[i];
					}
					else
						if (currentWritingMode == PdfData.VERTICAL_TOP_TO_BOTTOM) {
							x1 = this.f_y2[i];
							x2 = this.f_y1[i];
							y2 = this.f_x1[i];
							y1 = this.f_x2[i];
						}
						else {
							throw new PdfException("Illegal value " + currentWritingMode + "for currentWritingMode");
						}

			// if in the area, process
			if ((x1 > minX - .5) && (x2 < maxX + .5) && (y2 > minY - .5) && (y1 < maxY + .5)) {

				// run though the string extracting our markers to get x values
				StringTokenizer tokens = new StringTokenizer(raw, MARKER, true);
				String value, lastValue = "";
				Object currentValue;

				while (tokens.hasMoreTokens()) {

					// encoding in data
					value = tokens.nextToken(); // see if first marker
					if (value.equals(MARKER)) {

						value = tokens.nextToken(); // point character starts

						if (value.length() > 0) {

							lastX = currentX;
							currentX = Float.parseFloat(value);
							try {

								// add x to list or increase counter at start
								// or on space
								// add points either side of space
								if (lastValue.length() == 0 || (lastValue.indexOf(' ') != -1)) {

									intX = (int) currentX;
									currentValue = xLines.get(intX);
									if (currentValue == null) {
										xLines.put(intX, 1);
									}
									else {
										int countReached = (Integer) currentValue;
										countReached++;

										if (countReached > most_frequent) most_frequent = countReached;

										xLines.put(intX, countReached);
									}

									// work out the middle
									int middle = (int) (lastX + ((currentX - lastX) / 2));

									if (lastX != 0) {
										intX = middle;
										currentValue = xLines.get(intX);
										if (currentValue == null) {
											xLines.put(intX, 1);
										}
										else {
											int count_reached = (Integer) currentValue;
											count_reached++;

											if (count_reached > most_frequent) most_frequent = count_reached;

											xLines.put(intX, count_reached);
										}
									}
								}

							}
							catch (Exception e) {
								LogWriter.writeLog("Exception " + e + " stripping x values");
							}
						}

						tokens.nextToken(); // second marker
						tokens.nextToken(); // glyph width
						tokens.nextToken(); // third marker
						value = tokens.nextToken(); // put back chars
						lastValue = value;

					}
				}
			}
		}

		// now analyse the data
		Iterator keys = xLines.keySet().iterator();
		int minimum_needed = most_frequent / 2;

		while (keys.hasNext()) {
			Integer current_key = (Integer) keys.next();
			int current_count = (Integer) xLines.get(current_key);

			if (current_count > minimum_needed) this.lineBreaks.addElement(current_key);

		}
	}

	/**
	 * Method splitFragments adds raw frgaments to processed fragments breaking up any with vertical lines through or what looks like tabbed spaces
	 * 
	 * @throws PdfException
	 */
	private void copyToArrays(float minX, float minY, float maxX, float maxY, boolean keepFont, boolean breakOnSpace, boolean findLines,
			String punctuation, boolean isWordlist) throws PdfException {

		final boolean debugSplit = false;

		// initialise local arrays allow for extra space
		int count = this.pdf_data.getRawTextElementCount() + increment;

		this.f_x1 = new float[count];
		this.f_colorTag = new String[count];
		this.hadSpace = new boolean[count];
		this.f_x2 = new float[count];
		this.f_y1 = new float[count];
		this.f_y2 = new float[count];

		this.spaceWidth = new float[count];
		this.content = new StringBuilder[count];
		this.fontSize = new int[count];
		this.textLength = new int[count];
		this.writingMode = new int[count];
		this.isUsed = new boolean[count];
		this.moveType = new int[count];

		// flag to find lines based on orientation of first text item*/
		boolean linesScanned = false;

		// set defaults and calculate dynamic values
		int text_length;
		count = count - increment;
		float last_pt, min, max, pt, x1, x2, y1, y2, linePos, character_spacing;
		String raw, char_width = "", currentColor;
		StringBuilder text = new StringBuilder();

		// work through fragments
		for (int i = 0; i < count; i++) {

			// extract values
			character_spacing = this.pdf_data.f_character_spacing[i];
			raw = this.pdf_data.contents[i];
			x1 = this.pdf_data.f_x1[i];
			currentColor = this.pdf_data.colorTag[i];
			x2 = this.pdf_data.f_x2[i];
			y1 = this.pdf_data.f_y1[i];
			y2 = this.pdf_data.f_y2[i];
			text_length = this.pdf_data.text_length[i];
			int mode = this.pdf_data.f_writingMode[i];
			int moveType = this.pdf_data.move_command[i];

			/**
			 * see if in area
			 */
			boolean accepted = false;

			if (debugSplit) {
				System.out.println("raw data=" + raw);
				System.out.println("text data=" + PdfGroupingAlgorithms.removeHiddenMarkers(raw));
			}

			// if at least partly in the area, process
			if ((mode == PdfData.HORIZONTAL_LEFT_TO_RIGHT || mode == PdfData.HORIZONTAL_RIGHT_TO_LEFT) && y2 > minY && y1 < maxY && x1 < maxX
					&& x2 > minX) {
				accepted = true;
			}
			else
				if ((mode == PdfData.VERTICAL_BOTTOM_TO_TOP || mode == PdfData.VERTICAL_TOP_TO_BOTTOM) && x1 > minX && x2 < maxX && y1 > minY
						&& y2 < maxY) accepted = true;

			if (accepted) {

				/** find lines */
				// look for possible vertical or horizontal lines in the data
				if ((!linesScanned) && (findLines)) {
					findVerticalLines(minX, minY, maxX, maxY, mode);
					linesScanned = true;
				}

				/**
				 * initialise pointers and work out an 'average character space'
				 **/
				if (mode == PdfData.HORIZONTAL_LEFT_TO_RIGHT || mode == PdfData.HORIZONTAL_RIGHT_TO_LEFT) {
					// space = (x2 - x1) / text_length;
					pt = x1;
					last_pt = x1;
					min = minX;
					max = maxX;
				}
				else { // vertical text
						// space = (y1 - y2) / text_length;
					pt = y2;
					last_pt = y2;
					min = minY;
					max = maxY;
				}

				linePos = -1;

				/**
				 * work through text, using embedded markers to work out whether each letter is IN or OUT
				 */
				char[] line = raw.toCharArray();

				int end = line.length;
				int pointer = 0;

				String value, textValue = "", pt_reached;

				// allow for no tokens and return all text fragment
				if (!raw.contains(MARKER)) text = new StringBuilder(raw);

				boolean isFirstValue = true, breakPointset = false;

				/**
				 * work through text, using embedded markers to work out whether each letter is IN or OUT
				 */
				while (pointer < end) {

					// only data between min and y locations
					while (true) {

						/**
						 * read value
						 */

						if (line[pointer] != MARKER2) {
							// find second marker and get width
							int startPointer = pointer;
							while ((pointer < end) && (line[pointer] != MARKER2))
								pointer++;
							value = raw.substring(startPointer, pointer);

						}
						else {// if (value.equals(MARKER)) { // read the next token and its location and width

							// find first marker
							while ((pointer < end) && (line[pointer] != MARKER2))
								pointer++;

							pointer++;

							// find second marker and get width
							int startPointer = pointer;
							while ((pointer < end) && (line[pointer] != MARKER2))
								pointer++;
							pt_reached = raw.substring(startPointer, pointer);
							pointer++;

							// find third marker
							startPointer = pointer;
							while ((pointer < end) && (line[pointer] != MARKER2))
								pointer++;

							char_width = raw.substring(startPointer, pointer);
							pointer++;

							// find next marker
							startPointer = pointer;
							while ((pointer < end) && (line[pointer] != MARKER2))
								pointer++;

							value = raw.substring(startPointer, pointer);

							textValue = value; // keep value with no spaces

							if (pt_reached.length() > 0) { // set point character starts
								last_pt = pt;
								pt = Float.parseFloat(pt_reached);

								if (breakPointset) {
									if (mode == PdfData.HORIZONTAL_LEFT_TO_RIGHT) x1 = pt;
									else
										if (mode == PdfData.HORIZONTAL_RIGHT_TO_LEFT) x2 = pt;
										else
											if (mode == PdfData.VERTICAL_BOTTOM_TO_TOP) y2 = pt;
											else
												if (mode == PdfData.VERTICAL_TOP_TO_BOTTOM) y1 = pt;
									breakPointset = false;
								}
							}

							// add font start if needed
							if ((this.isXMLExtraction) && (last_pt < min) && (pt > min) && (!value.startsWith(Fonts.fb))) value = Fonts
									.getActiveFontTag(raw, "") + value;

						}

						if ((pt > min) & (pt < max)) {
							if (mode == PdfData.HORIZONTAL_LEFT_TO_RIGHT) if ((x1 < min || x1 > max) && pt >= min) x1 = pt;
							else
								if (mode == PdfData.HORIZONTAL_RIGHT_TO_LEFT) if ((x2 > max || x2 < min) && pt <= max) x2 = pt;
								else
									if (mode == PdfData.VERTICAL_BOTTOM_TO_TOP) if ((y2 < min || y2 > max) && pt >= min) y2 = pt;
									else
										if (mode == PdfData.VERTICAL_TOP_TO_BOTTOM) if ((y1 < min || y1 > max) && pt <= min) y1 = pt;
							break;
						}

						value = "";
						textValue = "";

						if (pointer >= end) break;
					}

					/** make sure font not sliced off on first value */
					if ((isFirstValue)) {

						isFirstValue = false;
						if ((this.isXMLExtraction) && (keepFont) && (!value.startsWith(Fonts.fb)) && (!value.startsWith(GenericColorSpace.cb))) // &&(!text.toString().startsWith(Fonts.fb))))
						text.append(Fonts.getActiveFontTag(text.toString(), raw));
					}

					/**
					 * we now have a valid value inside the selected area so perform tests
					 */
					// see if a break occurs
					boolean is_broken = false;
					if (findLines && character_spacing > 0 && text.toString().endsWith(" ")) {
						int counts = this.lineBreaks.size();
						for (int jj = 0; jj < counts; jj++) {
							int test_x = this.lineBreaks.elementAt(jj);
							if ((last_pt < test_x) & (pt > test_x)) {
								jj = counts;
								is_broken = true;
							}
						}
					}

					boolean endsWithPunctuation = checkForPunctuation(textValue, punctuation);

					if (is_broken) { // break on double-spaces or larger

						if (debugSplit) System.out.println("Break 1 is_broken");

						float Nx1 = x1, Nx2 = x2, Ny1 = y1, Ny2 = y2;
						if (mode == PdfData.HORIZONTAL_LEFT_TO_RIGHT) Nx2 = last_pt + Float.parseFloat(char_width);
						else
							if (mode == PdfData.HORIZONTAL_RIGHT_TO_LEFT) Nx1 = last_pt + Float.parseFloat(char_width);
							else
								if (mode == PdfData.VERTICAL_BOTTOM_TO_TOP) Ny1 = last_pt + Float.parseFloat(char_width);
								else
									if (mode == PdfData.VERTICAL_TOP_TO_BOTTOM) Ny2 = last_pt + Float.parseFloat(char_width);

						addFragment(moveType, i, text, Nx1, Nx2, Ny1, Ny2, text_length, keepFont, currentColor, isWordlist);
						text = new StringBuilder(Fonts.getActiveFontTag(text.toString(), raw));
						text.append(value);

						if (mode == PdfData.HORIZONTAL_LEFT_TO_RIGHT) x1 = pt;
						else
							if (mode == PdfData.HORIZONTAL_RIGHT_TO_LEFT) x2 = pt;
							else
								if (mode == PdfData.VERTICAL_BOTTOM_TO_TOP) y2 = pt;
								else
									if (mode == PdfData.VERTICAL_TOP_TO_BOTTOM) y1 = pt;

					}
					else
						if ((endsWithPunctuation) | ((breakOnSpace) && ((textValue.indexOf(' ') != -1) || (value.endsWith(" "))))
								| ((textValue.contains("   ")))) {// break on double-spaces or larger
							if (debugSplit) System.out.println("Break 2 endsWithPunctuation=" + endsWithPunctuation + " textValue=" + textValue + '<'
									+ " value=" + value + '<' + " text=" + text + '<');

							// Remove final bit of the below if to fix issue in case 11542
							if (textValue.length() > 1 && textValue.indexOf(' ') != -1) {// && x1==pt){ //add in space values to start of next shape
								// count the spaces
								int ptr = textValue.indexOf(' ');

								if (ptr > 0) {
									pt = pt + ptr * (Float.parseFloat(char_width) / textValue.length());
								}
								// else
								// pt=pt+Float.parseFloat(char_width);

							}

							if (!endsWithPunctuation) text.append(value.trim());

							if (mode == PdfData.HORIZONTAL_LEFT_TO_RIGHT) {

								if (debugSplit) System.out.println("Add " + x1 + ' ' + pt + " text=" + text + " i=" + i);
								addFragment(moveType, i, text, x1, pt, y1, y2, text_length, keepFont, currentColor, isWordlist);
							}
							else
								if (mode == PdfData.HORIZONTAL_RIGHT_TO_LEFT) {
									if (debugSplit) System.out.println("b");
									addFragment(moveType, i, text, pt, x2, y1, y2, text_length, keepFont, currentColor, isWordlist);
								}
								else
									if (mode == PdfData.VERTICAL_BOTTOM_TO_TOP) {
										if (debugSplit) System.out.println("c");
										addFragment(moveType, i, text, x1, x2, pt, y2, text_length, keepFont, currentColor, isWordlist);
									}
									else
										if (mode == PdfData.VERTICAL_TOP_TO_BOTTOM) {
											if (debugSplit) System.out.println("d");
											addFragment(moveType, i, text, x1, x2, y1, pt, text_length, keepFont, currentColor, isWordlist);
										}

							if (char_width.length() > 0) { // add in space values to start of next shape
								// count the spaces
								int ptr = 0;

								if (textValue.indexOf(' ') != -1) ptr = textValue.indexOf(' ');

								if (isWordlist) {
									int len = textValue.length();
									while (ptr < len && textValue.charAt(ptr) == ' ') {
										ptr++;
									}
								}

								if (ptr > 0) pt = pt + ptr * Float.parseFloat(char_width);
								else pt = pt + Float.parseFloat(char_width);

								if (ptr > 0) breakPointset = true;
								else breakPointset = false;

							}

							// store fact it had a space in case we generate wordlist
							if ((breakOnSpace) & (this.nextSlot > 0)) this.hadSpace[this.nextSlot - 1] = true;

							text = new StringBuilder(Fonts.getActiveFontTag(text.toString(), raw));
							if (mode == PdfData.HORIZONTAL_LEFT_TO_RIGHT) x1 = pt;// + space;
							else
								if (mode == PdfData.HORIZONTAL_RIGHT_TO_LEFT) x2 = pt;// - space;
								else
									if (mode == PdfData.VERTICAL_BOTTOM_TO_TOP) y2 = pt;// + space;
									else
										if (mode == PdfData.VERTICAL_TOP_TO_BOTTOM) y1 = pt;// - space;

						}
						else
							if ((linePos != -1) & (pt > linePos)) {// break on a vertical line

								if (mode == PdfData.HORIZONTAL_LEFT_TO_RIGHT) addFragment(moveType, i, text, x1, linePos, y1, y2, text_length,
										keepFont, currentColor, isWordlist);
								else
									if (mode == PdfData.HORIZONTAL_RIGHT_TO_LEFT) addFragment(moveType, i, text, linePos, x2, y1, y2, text_length,
											keepFont, currentColor, isWordlist);
									else
										if (mode == PdfData.VERTICAL_BOTTOM_TO_TOP) addFragment(moveType, i, text, x1, x2, linePos, y2, text_length,
												keepFont, currentColor, isWordlist);
										else
											if (mode == PdfData.VERTICAL_TOP_TO_BOTTOM) addFragment(moveType, i, text, x1, x2, y1, linePos,
													text_length, keepFont, currentColor, isWordlist);

								text = new StringBuilder(Fonts.getActiveFontTag(text.toString(), raw));
								text.append(value);

								if (mode == PdfData.HORIZONTAL_LEFT_TO_RIGHT) x1 = linePos;
								else
									if (mode == PdfData.HORIZONTAL_RIGHT_TO_LEFT) x2 = linePos;
									else
										if (mode == PdfData.VERTICAL_BOTTOM_TO_TOP) y2 = linePos;
										else
											if (mode == PdfData.VERTICAL_TOP_TO_BOTTOM) y1 = linePos;

								linePos = -1;

							}
							else { // allow for space used as tab
								if ((this.isXMLExtraction) && (value.endsWith(' ' + Fonts.fe))) {
									value = Fonts.fe;
									textValue = "";

									if (mode == PdfData.HORIZONTAL_LEFT_TO_RIGHT) x2 = last_pt;
									else
										if (mode == PdfData.HORIZONTAL_RIGHT_TO_LEFT) x1 = last_pt;
										else
											if (mode == PdfData.VERTICAL_BOTTOM_TO_TOP) y1 = last_pt;
											else
												if (mode == PdfData.VERTICAL_TOP_TO_BOTTOM) y2 = last_pt;
								}
								text.append(value);
							}

				}

				// trap scenario we found if all goes through with no break at end
				if ((keepFont) && (this.isXMLExtraction) && (!text.toString().endsWith(Fonts.fe))
						&& (!text.toString().endsWith(GenericColorSpace.ce))) text.append(Fonts.fe);

				// create new line with what is left and output
				if (mode == PdfData.HORIZONTAL_LEFT_TO_RIGHT || mode == PdfData.HORIZONTAL_RIGHT_TO_LEFT) {
					if (x1 < x2) addFragment(moveType, i, text, x1, x2, y1, y2, text_length, keepFont, currentColor, isWordlist);
				}
				else
					if (mode == PdfData.VERTICAL_BOTTOM_TO_TOP || mode == PdfData.VERTICAL_TOP_TO_BOTTOM) {
						if (y1 > y2) addFragment(moveType, i, text, x1, x2, y1, y2, text_length, keepFont, currentColor, isWordlist);
					}
				text = new StringBuilder();

			}
		}

		// local lists for faster access
		this.isUsed = new boolean[this.nextSlot];
	}

	/**
	 * @param textValue
	 */
	private static boolean checkForPunctuation(String textValue, String punctuation) {

		if (punctuation == null) return false;

		/** see if ends with punctuation */
		boolean endsWithPunctuation = false;
		int textLength = textValue.length();
		int ii = textLength - 1;
		if (textLength > 0) { // strip any spaces and tags in test
			char testChar = textValue.charAt(ii);
			boolean inTag = (testChar == '>');
			while (((inTag) | (testChar == ' ')) & (ii > 0)) {

				if (testChar == '<') inTag = false;

				ii--;
				testChar = textValue.charAt(ii);

				if (testChar == '>') inTag = true;
			}

			// stop matches on &;
			if ((testChar == ';')) {
				// ignore if looks like &xxx;
				endsWithPunctuation = true;
				ii--;
				while (ii > -1) {

					testChar = textValue.charAt(ii);
					if (testChar == '&' || testChar == '#') {
						endsWithPunctuation = false;
						ii = 0;
					}

					if (ii == 0 || testChar == ' ' || !Character.isLetterOrDigit(testChar)) break;

					ii--;
				}
			}
			else
				if (punctuation.indexOf(testChar) != -1) endsWithPunctuation = true;

		}
		return endsWithPunctuation;
	}

	/**
	 * add an object to our new XML list
	 */
	private void addFragment(int moveType, int index, StringBuilder contentss, float x1, float x2, float y1, float y2, int text_len,
			boolean keepFontTokens, String currentColorTag, boolean isWordlist) {

		StringBuilder current_text = contentss;
		String str = current_text.toString();

		// strip <> or ascii equivalents
		if (isWordlist) {
			if (str.contains("&#")) current_text = Strip.stripAmpHash(current_text);

			if ((this.isXMLExtraction) && ((str.contains("&lt;")) || (str.contains("&gt;")))) current_text = Strip.stripXMLArrows(current_text, true);
			else
				if ((!this.isXMLExtraction) && ((str.indexOf('<') != -1) || (str.indexOf('>') != -1))) current_text = Strip.stripArrows(current_text);
		}

		// StringBuilder justText=Strip.stripXML(current_text);

		// ignore blank space objects
		// if (justText.length() == 0) {

		if (getFirstChar(current_text) != -1) {

			// strip tags or pick up missed </font> if ends with space
			if (keepFontTokens == false) {

				// strip fonts if required
				current_text = Strip.stripXML(current_text, this.isXMLExtraction);

			}
			else
				if (this.isXMLExtraction) {

					// no color tag
					if (this.pdf_data.isColorExtracted() && (!current_text.toString().endsWith(GenericColorSpace.ce))) {

						// se
						// if ends </font> add </color>
						// otherwise add </font></color>
						if (!current_text.toString().endsWith(Fonts.fe)) current_text = current_text.append(Fonts.fe);
						current_text = current_text.append(GenericColorSpace.ce);

					}
					else
						if ((!this.pdf_data.isColorExtracted()) && (!current_text.toString().endsWith(Fonts.fe))) current_text = current_text
								.append(Fonts.fe);
				}

			// add to vacant slot or create new slot
			int count = this.f_x1.length;

			if (this.nextSlot < count) {

				this.f_x1[this.nextSlot] = x1;
				this.f_colorTag[this.nextSlot] = currentColorTag;
				this.f_x2[this.nextSlot] = x2;
				this.f_y1[this.nextSlot] = y1;
				this.f_y2[this.nextSlot] = y2;
				this.moveType[this.nextSlot] = moveType;

				this.fontSize[this.nextSlot] = this.pdf_data.f_end_font_size[index];
				this.writingMode[this.nextSlot] = this.pdf_data.f_writingMode[index];
				this.textLength[this.nextSlot] = text_len;

				this.spaceWidth[this.nextSlot] = this.pdf_data.space_width[index];
				this.content[this.nextSlot] = current_text;

				this.nextSlot++;
			}
			else {
				count = count + increment;
				float[] t_x1 = new float[count];
				String[] t_colorTag = new String[count];
				float[] t_x2 = new float[count];
				float[] t_y1 = new float[count];
				float[] t_y2 = new float[count];
				float[] t_spaceWidth = new float[count];

				StringBuilder[] t_content = new StringBuilder[count];

				int[] t_font_size = new int[count];
				int[] t_text_len = new int[count];
				int[] t_writingMode = new int[count];

				int[] t_moveType = new int[count];

				boolean[] t_isUsed = new boolean[count];

				boolean[] t_hadSpace = new boolean[count];

				// copy in existing
				for (int i = 0; i < count - increment; i++) {
					t_x1[i] = this.f_x1[i];
					t_colorTag[i] = this.f_colorTag[i];
					t_x2[i] = this.f_x2[i];
					t_y1[i] = this.f_y1[i];
					t_y2[i] = this.f_y2[i];
					t_hadSpace[i] = this.hadSpace[i];
					t_spaceWidth[i] = this.spaceWidth[i];
					t_content[i] = this.content[i];
					t_font_size[i] = this.fontSize[i];
					t_writingMode[i] = this.writingMode[i];
					t_text_len[i] = this.textLength[i];
					t_isUsed[i] = this.isUsed[i];
					t_moveType[i] = this.moveType[i];
				}

				this.f_x1 = t_x1;
				this.f_colorTag = t_colorTag;
				this.hadSpace = t_hadSpace;
				this.f_x2 = t_x2;
				this.f_y1 = t_y1;
				this.f_y2 = t_y2;
				this.isUsed = t_isUsed;

				this.fontSize = t_font_size;
				this.writingMode = t_writingMode;
				this.textLength = t_text_len;

				this.spaceWidth = t_spaceWidth;

				this.content = t_content;

				this.moveType = t_moveType;

				this.f_x1[this.nextSlot] = x1;
				this.f_colorTag[this.nextSlot] = currentColorTag;
				this.f_x2[this.nextSlot] = x2;
				this.f_y1[this.nextSlot] = y1;
				this.f_y2[this.nextSlot] = y2;

				this.fontSize[this.nextSlot] = this.pdf_data.f_end_font_size[index];
				this.writingMode[this.nextSlot] = this.pdf_data.f_writingMode[index];
				t_text_len[this.nextSlot] = text_len;
				this.content[this.nextSlot] = current_text;

				this.spaceWidth[this.nextSlot] = this.pdf_data.space_width[index];

				this.moveType[this.nextSlot] = moveType;

				this.nextSlot++;

			}

		}
	}

	// ////////////////////////////////////////////////////////////////////
	/**
	 * put rows together into one object with start and end
	 */
	private void mergeTableRows(int border_width) {

		// merge row contents
		String separator = "</tr>\n<tr>";

		if (this.isXHTML == false) separator = "\n";

		this.master = ((Vector_Int) this.lines.elementAt(this.line_order[0])).elementAt(0);

		int item;
		for (int rr = 1; rr < this.max_rows; rr++) {

			item = ((Vector_Int) this.lines.elementAt(this.line_order[rr])).elementAt(0);
			if (this.content[this.master] == null) this.master = item;
			else
				if (this.content[item] != null) merge(this.master, item, separator, false);
		}

		// add start/end marker
		if (this.isXHTML) {
			if (border_width == 0) {
				this.content[this.master].insert(0, "<TABLE>\n<tr>");
				this.content[this.master].append("</tr>\n</TABLE>\n");
			}
			else {
				StringBuilder startTag = new StringBuilder("<TABLE border='");
				startTag.append(String.valueOf(border_width));
				startTag.append("'>\n<tr>");
				startTag.append(this.content[this.master]);
				this.content[this.master] = startTag;
				this.content[this.master].append("</tr>\n</TABLE>\n");
			}
		}
	}

	// ////////////////////////////////////////////////
	/**
	 * get list of unused fragments and put in list and sort in sorted_items
	 */
	final private int[] getsortedUnusedFragments(boolean sortOnX, boolean use_y1) {
		int total_fragments = this.isUsed.length;

		// get unused item pointers
		int ii = 0;
		int sorted_temp_index[] = new int[total_fragments];
		for (int i = 0; i < total_fragments; i++) {
			if (this.isUsed[i] == false) {
				sorted_temp_index[ii] = i;
				ii++;
			}
		}

		int[] unsorted_items = new int[ii];
		int[] sorted_items;
		int[] sorted_temp_x1 = new int[ii];
		int[] sorted_temp_y1 = new int[ii];
		int[] sorted_temp_y2 = new int[ii];

		// put values in array and get x/y for sort
		for (int pointer = 0; pointer < ii; pointer++) {
			int i = sorted_temp_index[pointer];
			unsorted_items[pointer] = i;

			sorted_temp_x1[pointer] = (int) this.f_x1[i];

			// negative values to get sort in 'wrong' order from top of page
			sorted_temp_y1[pointer] = (int) this.f_y1[i];
			sorted_temp_y2[pointer] = (int) this.f_y2[i];

		}

		// sort
		if (sortOnX == false) {
			if (use_y1 == true) sorted_items = Sorts.quicksort(sorted_temp_y1, sorted_temp_x1, unsorted_items);
			else sorted_items = Sorts.quicksort(sorted_temp_y2, sorted_temp_x1, unsorted_items);
		}
		else sorted_items = Sorts.quicksort(sorted_temp_x1, sorted_temp_y1, unsorted_items);

		return sorted_items;
	}

	// ////////////////////////////////////////////////////////////////////
	/**
	 * create rows of data from preassembled indices, adding separators. Each row is built to a temp array and then row created - we don't know how
	 * many columns until the table is built
	 * 
	 * @throws PdfException
	 */
	private void createTableRows(boolean keep_alignment_information, boolean keep_width_information, int currentWritingMode) throws PdfException {

		/**
		 * create local copies of arrays
		 */
		float[] f_x1, f_x2;

		/**
		 * set pointers so left to right text
		 */
		if (currentWritingMode == PdfData.HORIZONTAL_LEFT_TO_RIGHT) {
			f_x1 = this.f_x1;
			f_x2 = this.f_x2;
			// f_y1=this.f_y1;
			// f_y2=this.f_y2;
		}
		else
			if (currentWritingMode == PdfData.HORIZONTAL_RIGHT_TO_LEFT) {
				f_x2 = this.f_x1;
				f_x1 = this.f_x2;
				// f_y1=this.f_y1;
				// f_y2=this.f_y2;
			}
			else
				if (currentWritingMode == PdfData.VERTICAL_BOTTOM_TO_TOP) {
					f_x1 = this.f_y2;
					f_x2 = this.f_y1;
					// f_y1=this.f_x2;
					// f_y2=this.f_x1;
				}
				else
					if (currentWritingMode == PdfData.VERTICAL_TOP_TO_BOTTOM) {
						f_x1 = this.f_y1;
						f_x2 = this.f_y2;
						// f_y2=this.f_x1;
						// f_y1=this.f_x2;

						/**
						 * fiddle x,y co-ords so it works
						 */

						// get max size
						int maxX = 0;
						for (float aF_x1 : f_x1) {
							if (maxX < aF_x1) maxX = (int) aF_x1;
						}

						maxX++; // allow for fp error
						// turn around
						for (int ii = 0; ii < f_x2.length; ii++) {
							f_x1[ii] = maxX - f_x1[ii];
							f_x2[ii] = maxX - f_x2[ii];
						}

					}
					else {
						throw new PdfException("Illegal value " + currentWritingMode + "for currentWritingMode");
					}

		int item, i;// , current_col = -1;

		int itemsInTable = 0, items_added = 0;
		// pointer to current element on each row
		int[] currentItem = new int[this.max_rows];

		Vector_Int[] rowContents = new Vector_Int[this.max_rows];
		Vector_String alignments = new Vector_String(); // text alignment
		Vector_Float widths = new Vector_Float(); // cell widths
		Vector_Float cell_x1 = new Vector_Float(); // cell widths
		String separator = "", empty_cell = "&nbsp;";

		if (this.isXHTML == false) {
			separator = "\",\"";
			empty_cell = "";
		}

		/**
		 * set number of items on each line, column count and populate empty rows
		 */
		int[] itemCount = new int[this.max_rows];
		for (i = 0; i < this.max_rows; i++) {
			itemCount[i] = ((Vector_Int) this.lines.elementAt(i)).size() - 1;

			// total number of items
			itemsInTable = itemsInTable + itemCount[i];

			// reset other values
			currentItem[i] = 0;
			rowContents[i] = new Vector_Int(20);
		}

		// now work through and split any overlapping items until all done
		while (true) {

			// size of column and pointers
			float x1 = 9999, min_x2 = 9999, x2, current_x1, current_x2, c_x1, next_x1 = 9999, c_x2, items_in_column = 0;

			boolean all_done = true; // flag to exit at end
			float total_x1 = 0, total_x2 = 0, left_gap = 0, right_gap;

			String alignment = "center";

			if (items_added < itemsInTable) {

				/**
				 * work out cell x boundaries on basis of objects
				 */
				for (i = 0; i < this.max_rows; i++) { // get width for column
					if (itemCount[i] > currentItem[i]) { // item id

						item = ((Vector_Int) this.lines.elementAt(i)).elementAt(currentItem[i]);
						current_x1 = f_x1[item];
						current_x2 = f_x2[item];

						if (current_x1 < x1) // left margin
						x1 = current_x1;
						if (current_x2 < min_x2) // right margin if appropriate
						min_x2 = current_x2;

					}
				}

				cell_x1.addElement(x1); // save left margin
				x2 = min_x2; // set default right margin

				/**
				 * workout end and next column start by scanning all items
				 */
				for (i = 0; i < this.max_rows; i++) { // slot the next item on each row together work out item
					item = ((Vector_Int) this.lines.elementAt(i)).elementAt(currentItem[i]);
					c_x1 = f_x1[item];
					c_x2 = f_x2[item];

					// max item width of this column
					if ((c_x1 >= x1) & (c_x1 < min_x2) & (c_x2 > x2)) x2 = c_x2;

					if (currentItem[i] < itemCount[i]) { // next left margin

						item = ((Vector_Int) this.lines.elementAt(i)).elementAt(currentItem[i] + 1);
						current_x1 = f_x1[item];
						if ((current_x1 > min_x2) & (current_x1 < next_x1)) next_x1 = current_x1;
					}
				}

				// stop infinite loop case
				if (x1 == x2) break;

				// allow for last column
				if (next_x1 == 9999) next_x1 = x2;

				/**
				 * count items in table and workout raw totals for alignment. Also work out widest x2 in column
				 */
				for (i = 0; i < this.max_rows; i++) { // slot the next item on each row together

					// work out item
					item = ((Vector_Int) this.lines.elementAt(i)).elementAt(currentItem[i]);
					c_x1 = f_x1[item];
					c_x2 = f_x2[item];

					// use items in first column of single colspan
					if ((c_x1 >= x1) & (c_x1 < min_x2) & (c_x2 <= next_x1)) {

						// running totals to calculate alignment
						total_x1 = total_x1 + c_x1;
						total_x2 = total_x2 + c_x2;
						items_in_column++;

					}
				}

				/**
				 * work out gap and include empty space between cols and save
				 */
				if (i == 0) left_gap = x1;
				if (next_x1 == -1) right_gap = 0;
				else right_gap = (int) ((next_x1 - x2) / 2);

				int width = (int) (x2 - x1 + right_gap + left_gap);
				// noinspection UnusedAssignment,UnusedAssignment
				left_gap = right_gap;
				widths.addElement(width);

				/** workout the alignment */
				float x1_diff = (total_x1 / items_in_column) - x1;
				float x2_diff = x2 - (total_x2 / items_in_column);
				if (x1_diff < 1) alignment = "left";
				else
					if (x2_diff < 1) alignment = "right";
				alignments.addElement(alignment);

				for (i = 0; i < this.max_rows; i++) { // slot the next item on each row together
					this.master = ((Vector_Int) this.lines.elementAt(i)).elementAt(0);
					// get next item on line or -1 for no more
					if (itemCount[i] > currentItem[i]) {
						// work out item
						item = ((Vector_Int) this.lines.elementAt(i)).elementAt(currentItem[i]);
						c_x1 = f_x1[item];
						c_x2 = f_x2[item];
						all_done = false;

					}
					else {
						item = -1;
						c_x1 = -1;
						c_x2 = -1;
					}

					if ((item == -1) & (items_added <= itemsInTable)) {
						// all items in table so just filling in gaps
						rowContents[i].addElement(-1);

					}
					else
						if ((c_x1 >= x1) & (c_x1 < x2)) {
							// fits into cell so add in and roll on marker

							rowContents[i].addElement(item);
							currentItem[i]++;

							items_added++;
						}
						else
							if (c_x1 > x2) { // empty cell
								rowContents[i].addElement(-1);
							}
				}
			}
			if (all_done) break;
		}

		// ===================================================================
		/**
		 * now assemble rows
		 */
		for (int row = 0; row < this.max_rows; row++) {
			StringBuilder line_content = new StringBuilder(100);

			int count = rowContents[row].size() - 1;
			this.master = ((Vector_Int) this.lines.elementAt(row)).elementAt(0);

			for (i = 0; i < count; i++) {
				item = rowContents[row].elementAt(i);

				if (this.isXHTML) {

					// get width
					float current_width = widths.elementAt(i);
					String current_alignment = alignments.elementAt(i);
					int test, colspan = 1, pointer = i + 1;

					if (item != -1) {

						// look for colspan
						while (true) {
							test = rowContents[row].elementAt(i + 1);
							if ((test != -1) | (count == i + 1)) break;

							// break if over another col - roll up single value on line
							if ((itemCount[row] > 1) & (cell_x1.elementAt(i + 1) > f_x2[item])) break;

							count--;
							rowContents[row].removeElementAt(i + 1);
							colspan++;

							// update width
							current_width = current_width + widths.elementAt(pointer);
							pointer++;
						}
					}
					line_content.append("<td");

					if (keep_alignment_information) {
						line_content.append(" align='");
						line_content.append(current_alignment);
						line_content.append('\'');
						if (colspan > 1) line_content.append(" colspan='").append(colspan).append('\'');
					}

					if (keep_width_information) line_content.append(" width='").append((int) current_width).append('\'');

					line_content.append(" nowrap>");
					if (item == -1) line_content.append(empty_cell);
					else line_content.append(this.content[item]);
					line_content.append("</td>");

				}
				else { // csv
					if (item == -1) // empty col
					line_content.append("\"\",");
					else { // value
						line_content.append('\"');
						line_content.append(this.content[item]);
						line_content.append("\",");
					}
				}

				// merge to update other values
				if ((item != -1) && (this.master != item)) // merge tracks the shape
				merge(this.master, item, separator, false);

			}
			// substitute our 'hand coded' value
			this.content[this.master] = line_content;

		}
	}

	/**
	 * work through data and create a set of rows and return an object with refs for each line
	 * 
	 * @throws PdfException
	 */
	private void createLinesInTable(int itemCount, int[] items, boolean addSpaceXMLTag, int mode) throws PdfException {

		/**
		 * reverse order if text right to left
		 */
		if (mode == PdfData.HORIZONTAL_RIGHT_TO_LEFT) items = reverse(items);

		/**
		 * create and populate local copies of arrays
		 */
		float[] f_x1, f_x2, f_y1, f_y2;

		// set pointers so always left to right text
		switch (mode) {
			case PdfData.HORIZONTAL_LEFT_TO_RIGHT:
				f_x1 = this.f_x1;
				f_x2 = this.f_x2;
				f_y1 = this.f_y1;
				f_y2 = this.f_y2;
				break;

			case PdfData.HORIZONTAL_RIGHT_TO_LEFT:
				f_x2 = this.f_x1;
				f_x1 = this.f_x2;
				f_y1 = this.f_y1;
				f_y2 = this.f_y2;
				break;

			case PdfData.VERTICAL_BOTTOM_TO_TOP:
				f_x1 = this.f_y1;
				f_x2 = this.f_y2;
				f_y1 = this.f_x2;
				f_y2 = this.f_x1;
				break;

			case PdfData.VERTICAL_TOP_TO_BOTTOM:
				f_x1 = this.f_y2;
				f_x2 = this.f_y1;
				f_y2 = this.f_x1;
				f_y1 = this.f_x2;
				items = this.getsortedUnusedFragments(false, true);
				items = reverse(items);
				break;

			default:
				throw new PdfException("Illegal value " + mode + "for currentWritingMode");
		}

		// holds line we're working on
		Vector_Int current_line;

		for (int j = 0; j < itemCount; j++) { // for all items

			int c = items[j], id = -1, i, last = c;
			float smallest_gap = -1, gap, yMidPt;

			if (!this.isUsed[c] && this.writingMode[c] == mode) {

				// reset pointer and add this element
				current_line = new Vector_Int(20);
				current_line.addElement(c);
				this.lineY2.addElement((int) f_y2[c]);

				// look for items along same line (already sorted into order left to right)
				while (true) { // look for a match
					for (int ii = 0; ii < itemCount; ii++) {

						i = items[ii];

						if (!this.isUsed[i]
								&& i != c
								&& this.writingMode[c] == mode
								&& ((f_x1[i] > f_x1[c] && mode != PdfData.VERTICAL_TOP_TO_BOTTOM) || (f_x1[i] < f_x1[c] && mode == PdfData.VERTICAL_TOP_TO_BOTTOM))) { // see
																																										// if
																																										// on
																																										// right

							gap = (f_x1[i] - f_x2[c]);

							if (mode == PdfData.HORIZONTAL_RIGHT_TO_LEFT || mode == PdfData.VERTICAL_TOP_TO_BOTTOM) gap = -gap;

							// allow for fp error
							if (gap < 0 && gap > -2) gap = 0;

							// make sure on right
							yMidPt = (f_y1[i] + f_y2[i]) / 2;

							// see if line & if only or better fit
							if (yMidPt < f_y1[c] && yMidPt > f_y2[c] && (smallest_gap < 0 || gap < smallest_gap)) {
								smallest_gap = gap;
								id = i;
							}
						}
					}

					if (id == -1) // exit when no more matches
					break;

					// merge in best match if fit found with last or if overlaps by less than half a space,otherwise join
					float t = f_x1[id] - f_x2[last], possSpace = f_x1[id] - f_x2[c];
					float av_char1 = (float) 1.5 * ((f_x2[id] - f_x1[id]) / this.textLength[id]);
					float av_char2 = (float) 1.5 * ((f_x2[last] - f_x1[last]) / this.textLength[last]);

					if ((mode == PdfData.HORIZONTAL_RIGHT_TO_LEFT || mode == PdfData.VERTICAL_TOP_TO_BOTTOM)) {
						possSpace = -possSpace;
						t = -t;
						av_char1 = -av_char1;
						av_char2 = -av_char2;
					}

					if (t < av_char1 && t < av_char2) {
						merge(last, id, isGapASpace(id, last, possSpace, addSpaceXMLTag, mode), true);
					}
					else {
						current_line.addElement(id);
						last = id;
					}

					// flag used and reset variables used
					this.isUsed[id] = true;
					id = -1;
					smallest_gap = 1000000;

				}

				// add line to list
				this.lines.addElement(current_line);
				this.max_rows++;
			}
		}
	}

	/**
	 * 
	 * calls various low level merging routines on merge -
	 * 
	 * isCSV sets if output is XHTML or CSV format -
	 * 
	 * XHTML also has options to include font tags (keepFontInfo), preserve widths (keepWidthInfo), try to preserve alignment (keepAlignmentInfo), and
	 * set a table border width (borderWidth) - AddCustomTags should always be set to false
	 * 
	 * @param x1
	 *            is the x coord of the top left corner
	 * @param y1
	 *            is the y coord of the top left corner
	 * @param x2
	 *            is the x coord of the bottom right corner
	 * @param y2
	 *            is the y coord of the bottom right corner
	 * @param pageNumber
	 *            is the page you wish to extract from
	 * @param isCSV
	 *            is a boolean. If false the output is xhtml if true the text is out as CSV
	 * @param keepFontInfo
	 *            if true and isCSV is false keeps font information in extrated text.
	 * @param keepWidthInfo
	 *            if true and isCSV is false keeps width information in extrated text.
	 * @param keepAlignmentInfo
	 *            if true and isCSV is false keeps alignment information in extrated text.
	 * @param borderWidth
	 *            is the width of the border for xhtml
	 * @return Map containing text found in estimated table cells
	 * @throws PdfException
	 *             If the co-ordinates are not valid
	 */
	public final Map extractTextAsTable(int x1, int y1, int x2, int y2, int pageNumber, boolean isCSV, boolean keepFontInfo, boolean keepWidthInfo,
			boolean keepAlignmentInfo, int borderWidth) throws PdfException {

		// check in correct order and throw exception if not
		int[] v = validateCoordinates(x1, y1, x2, y2);
		x1 = v[0];
		y1 = v[1];
		x2 = v[2];
		y2 = v[3];

		/** return the content as an Element */
		Map table_content = new HashMap();

		LogWriter.writeLog("extracting Text As Table");

		// flag type of table so we can add correct separators
		if (isCSV == true) {
			this.isXHTML = false;
		}
		else {
			this.isXHTML = true;
		}

		// init table variables
		this.lines = new Vector_Object(20);
		this.lineY2 = new Vector_Int(20);
		this.max_rows = 0;

		// init store for data
		copyToArrays(x1, y2, x2, y1, keepFontInfo, false, true, null, false);

		// initial grouping and delete any hidden text
		removeEncoding();

		// eliminate shadows and also merge overlapping text
		cleanupShadowsAndDrownedObjects(false);

		int[] items = this.getsortedUnusedFragments(true, false);
		int item_count = items.length; // number of items

		if (item_count == 0) return table_content;

		/**
		 * check orientation and get preferred. Items not correct will be ignored
		 */
		int writingMode = getWritingMode(items, item_count);

		String message = "Table Merging algorithm being applied " + (item_count) + " items";
		LogWriter.writeLog(message);

		/**
		 * scan all items joining best fit to right of each fragment to build lines
		 */
		if (item_count > 1) {

			// workout the raw lines
			createLinesInTable(item_count, items, this.isXHTML, writingMode);

			/**
			 * generate lookup with lines in correct order (minus used to get correct order down the page)
			 */
			int dx = 1;
			if (writingMode == PdfData.HORIZONTAL_LEFT_TO_RIGHT || writingMode == PdfData.VERTICAL_TOP_TO_BOTTOM) dx = -1;

			this.line_order = new int[this.max_rows];
			int[] line_y = new int[this.max_rows];

			for (int i = 0; i < this.max_rows; i++) {
				line_y[i] = dx * this.lineY2.elementAt(i);
				this.line_order[i] = i;
			}

			this.line_order = Sorts.quicksort(line_y, this.line_order);

			// assemble the rows and columns
			createTableRows(keepAlignmentInfo, keepWidthInfo, writingMode);

			// assemble the rows and columns
			mergeTableRows(borderWidth);

		}

		this.content[this.master] = cleanup(this.content[this.master]);

		String processed_value = this.content[this.master].toString();

		if (processed_value != null) {

			// cleanup data if needed by removing duplicate font tokens
			if (!isCSV) processed_value = Fonts.cleanupTokens(processed_value);

			table_content.put("content", processed_value);
			table_content.put("x1", String.valueOf(x1));
			table_content.put("x2", String.valueOf(x2));
			table_content.put("y1", String.valueOf(y1));
			table_content.put("y2", String.valueOf(y2));
		}

		return table_content;
	}

	/** make sure co-ords valid and throw exception if not */
	private static int[] validateCoordinates(int x1, int y1, int x2, int y2) {
		if ((x1 > x2) | (y1 < y2)) {

			// String errorMessage = "Invalid parameters for text rectangle. ";
			if (x1 > x2) {
				// errorMessage =
				// errorMessage
				// + "x1 value ("
				// + x1
				// + ") must be LESS than x2 ("
				// + x2
				// + "). ";
				int temp = x1;
				x1 = x2;
				x2 = temp;
				LogWriter.writeLog("x1 > x2, coordinates were swapped to validate");
			}

			if (y1 < y2) {
				// errorMessage =
				// errorMessage
				// + "y1 value ("
				// + y1
				// + ") must be MORE than y2 ("
				// + y2
				// + "). ";
				int temp = y1;
				y1 = y2;
				y2 = temp;
				LogWriter.writeLog("y1 < y2, coordinates were swapped to validate");
			}
			// throw new PdfException(errorMessage);
		}
		return new int[] { x1, y1, x2, y2 };
	}

	/**
	 * 
	 * algorithm to place data from within coordinates to a vector of word, word coords (x1,y1,x2,y2)
	 * 
	 * @param x1
	 *            is the x coord of the top left corner
	 * @param y1
	 *            is the y coord of the top left corner
	 * @param x2
	 *            is the x coord of the bottom right corner
	 * @param y2
	 *            is the y coord of the bottom right corner
	 * @param page_number
	 *            is the page you wish to extract from
	 * @param breakFragments
	 *            will divide up text based on white space characters
	 * @param punctuation
	 *            is a string containing all values that should be used to divide up words
	 * @return Vector containing words found and words coordinates (word, x1,y1,x2,y2...)
	 * @throws PdfException
	 *             If the co-ordinates are not valid
	 */
	final public List extractTextAsWordlist(int x1, int y1, int x2, int y2, int page_number, boolean breakFragments, String punctuation)
			throws PdfException {

		/** make sure co-ords valid and throw exception if not */
		int[] v = validateCoordinates(x1, y1, x2, y2);
		x1 = v[0];
		y1 = v[1];
		x2 = v[2];
		y2 = v[3];

		/** extract the raw fragments (Note order or parameters passed) */
		if (breakFragments) copyToArrays(x1, y2, x2, y1, true, true, false, punctuation, true);
		else copyToArrays();

		/** delete any hidden text */
		removeEncoding();

		// eliminate shadows and also merge overlapping text
		cleanupShadowsAndDrownedObjects(true);

		int[] items = getsortedUnusedFragments(true, false);
		int count = items.length;

		/**
		 * if no values return null
		 */
		if (count == 0) {
			LogWriter.writeLog("Less than 1 text item on page");

			return null;
		}

		/**
		 * check orientation and get preferred. Items not correct will be ignored
		 */
		int writingMode = getWritingMode(items, count);

		/**
		 * build set of lines from text
		 */
		createLines(count, items, writingMode, true, false, false);

		/**
		 * alter co-ords to rotated if requested
		 */
		float[] f_x1 = null, f_x2 = null, f_y1 = null, f_y2 = null;

		if (useUnrotatedCoords || writingMode == PdfData.HORIZONTAL_LEFT_TO_RIGHT) {
			f_x1 = this.f_x1;
			f_x2 = this.f_x2;
			f_y1 = this.f_y1;
			f_y2 = this.f_y2;
		}
		else
			if (writingMode == PdfData.HORIZONTAL_RIGHT_TO_LEFT) {
				f_x2 = this.f_x1;
				f_x1 = this.f_x2;
				f_y1 = this.f_y1;
				f_y2 = this.f_y2;
			}
			else
				if (writingMode == PdfData.VERTICAL_BOTTOM_TO_TOP) {
					f_x1 = this.f_y2;
					f_x2 = this.f_y1;
					f_y1 = this.f_x2;
					f_y2 = this.f_x1;

				}
				else
					if (writingMode == PdfData.VERTICAL_TOP_TO_BOTTOM) {
						f_x1 = this.f_y1;
						f_x2 = this.f_y2;
						f_y2 = this.f_x1;
						f_y1 = this.f_x2;
					}

		/** put into a Vector */
		List values = new ArrayList();

		for (int i = 0; i < this.content.length; i++) {
			if (this.content[i] != null) {

				// System.out.println(">>>>>"+content[i]);

				if ((this.colorExtracted) && (this.isXMLExtraction)) {
					if (!this.content[i].toString().toLowerCase().startsWith(GenericColorSpace.cb)) {
						this.content[i].insert(0, this.f_colorTag[this.master]);
					}
					if (!this.content[i].toString().toLowerCase().endsWith(GenericColorSpace.ce)) {
						this.content[i].append(GenericColorSpace.ce);
					}
				}

				if (this.isXMLExtraction) values.add((this.content[i]).toString());
				else values.add(Strip.convertToText((this.content[i]).toString(), this.isXMLExtraction));

				if ((!useUnrotatedCoords) && (writingMode == PdfData.VERTICAL_TOP_TO_BOTTOM)) {
					values.add(String.valueOf(f_x1[i]));
					values.add(String.valueOf(f_y1[i]));
					values.add(String.valueOf(f_x2[i]));
					values.add(String.valueOf(f_y2[i]));
				}
				else
					if ((!useUnrotatedCoords) && (writingMode == PdfData.VERTICAL_BOTTOM_TO_TOP)) {
						values.add(String.valueOf(f_x1[i]));
						values.add(String.valueOf(f_y2[i]));
						values.add(String.valueOf(f_x2[i]));
						values.add(String.valueOf(f_y1[i]));
					}
					else {
						values.add(String.valueOf(f_x1[i]));
						values.add(String.valueOf(f_y1[i]));
						values.add(String.valueOf(f_x2[i]));
						values.add(String.valueOf(f_y2[i]));
					}
			}
		}

		LogWriter.writeLog("Text extraction as wordlist completed");

		return values;
	}

	/**
	 * reset global values
	 */
	private void reset() {

		this.isXHTML = true;
		this.nextSlot = 0;

		this.lineBreaks = new Vector_Int();

		this.max_rows = 0;
		this.master = 0;

		this.colorExtracted = false;
	}

	/**
	 * algorithm to place data from specified coordinates on a page into a String.
	 * 
	 * @param x1
	 *            is the x coord of the top left corner
	 * @param y1
	 *            is the y coord of the top left corner
	 * @param x2
	 *            is the x coord of the bottom right corner
	 * @param y2
	 *            is the y coord of the bottom right corner
	 * @param page_number
	 *            is the page you wish to extract from
	 * @param estimateParagraphs
	 *            will attempt to find paragraphs and add new lines in output if true
	 * @param breakFragments
	 *            will divide up text based on white space characters if true
	 * @return Vector containing words found and words coordinates (word, x1,y1,x2,y2...)
	 * @throws PdfException
	 *             If the co-ordinates are not valid
	 */
	final public String extractTextInRectangle(int x1, int y1, int x2, int y2, int page_number, boolean estimateParagraphs, boolean breakFragments)
			throws PdfException {

		reset();

		if ((breakFragments) && (!this.pdf_data.IsEmbedded())) throw new PdfException(
				"[PDF] Request to breakfragments and width not added. Please add call to init(true) of PdfDecoder to your code.");

		/** make sure co-ords valid and throw exception if not */
		int[] v = validateCoordinates(x1, y1, x2, y2);
		x1 = v[0];
		y1 = v[1];
		x2 = v[2];
		y2 = v[3];

		int master, count;

		/** extract the raw fragments (Note order or parameters passed) */
		if (breakFragments) copyToArrays(x1, y2, x2, y1, (this.isXMLExtraction), false, false, null, false);
		else copyToArrays();

		/**
		 * delete any hidden text
		 */
		removeEncoding();

		/**
		 * eliminate shadows and also merge overlapping text
		 */
		cleanupShadowsAndDrownedObjects(false);

		/** get the fragments as an array */
		int[] items = getsortedUnusedFragments(true, false);
		count = items.length;

		/**
		 * if no values return null
		 */
		if (count == 0) {
			LogWriter.writeLog("Less than 1 text item on page");

			return null;
		}

		/**
		 * check orientation and get preferred. Items not correct will be ignored
		 */
		int writingMode = getWritingMode(items, count);

		/**
		 * build set of lines from text
		 */
		createLines(count, items, writingMode, false, this.isXMLExtraction, false);

		/**
		 * roll lines together
		 */

		master = mergeLinesTogether(writingMode, estimateParagraphs, x1, x2, y1, y2);

		/**
		 * add final deliminators
		 */
		if (this.isXMLExtraction) {
			this.content[master] = new StringBuilder(Fonts.cleanupTokens(this.content[master].toString()));
			this.content[master].insert(0, "<p>");
			this.content[master].append("</p>");
		}

		LogWriter.writeLog("Text extraction completed");

		return cleanup(this.content[master]).toString();
	}

	private StringBuilder cleanup(StringBuilder buffer) {

		if (buffer == null) return buffer;

/**
        if(PdfDecoder.inDemo){
            int icount=buffer.length(),count=0;
            boolean inToken=false;
            for(int i=0;i<icount;i++){
                char c=buffer.charAt(i);
                if(c=='<')
                    inToken=true;
                else if(c=='>')
                    inToken=false;
                else if((c!=' ')&&(!inToken)){
                    count++;
                    if(count>4){
                        count=0;
                        buffer.setCharAt(i,'1');
                    }
                }
            }
		}
		/**/

		// sort out & to &amp;
		if (this.isXMLExtraction) {
			String buf = buffer.toString();

			buf = buf.replaceAll("&#", "XX#");
			buf = buf.replaceAll("&lt", "XXlt");
			buf = buf.replaceAll("&gt", "XXgt");

			buf = buf.replaceAll("&", "&amp;");

			// put back others
			buf = buf.replaceAll("XX#", "&#");
			buf = buf.replaceAll("XXlt", "&lt");
			buf = buf.replaceAll("XXgt", "&gt");

			boolean removeInvalidXMLValues = true;
			if (removeInvalidXMLValues) {

				/**
				 * Restricted Char ::= [#x1-#x8] | [#xB-#xC] | [#xE-#x1F] | [#x7F-#x84] | [#x86-#x9F] [#x1-#x8] | [#x11-#x12] | [#x14-#x31] |
				 * [#x127-#x132] | [#x134-#x159]
				 */

				/** set mappings */
				Map asciiMappings = new HashMap();
				/** [#x1-#x8] */
				for (int i = 1; i <= 8; i++)
					asciiMappings.put("&#" + i + ';', "");

				/** [#x11-#x12] */
				for (int i = 11; i <= 12; i++)
					asciiMappings.put("&#" + i + ';', "");

				/** [#x14-#x31] */
				for (int i = 14; i <= 31; i++)
					asciiMappings.put("&#" + i + ';', "");

				/** [#x127-#x132] */
				// for (int i = 127; i <= 132; i++)
				// asciiMappings.put("&#" + i + ";", "");

				/** [#x134-#x159] */
				// for (int i = 134; i <= 159; i++)
				// asciiMappings.put("&#" + i + ";", "");

				/** substitute illegal XML characters for mapped values */
				for (Object o : asciiMappings.keySet()) {
					String character = (String) o;
					String mappedCharacter = (String) asciiMappings.get(character);

					buf = buf.replace(character, mappedCharacter);
				}
			}
			buffer = new StringBuilder(buf);
		}

		return buffer;
	}

	/**
	 * scan fragments and detect orientation. If multiple, prefer horizontal
	 */
	private int getWritingMode(int[] items, int count) {

		/**
		 * get first value
		 */
		int orientation = this.writingMode[items[0]];

		// exit if first is horizontal
		if (orientation == PdfData.HORIZONTAL_LEFT_TO_RIGHT || orientation == PdfData.HORIZONTAL_RIGHT_TO_LEFT) return orientation;

		/**
		 * scan items looking at orientation - exit if we find horizontal
		 */
		for (int j = 1; j < count; j++) {

			int c = items[j];

			if (!this.isUsed[c]) {

				if (this.writingMode[c] == PdfData.HORIZONTAL_LEFT_TO_RIGHT || this.writingMode[c] == PdfData.HORIZONTAL_RIGHT_TO_LEFT) {
					orientation = this.writingMode[c];
					j = count;
					LogWriter.writeLog("Text of multiple orientations found. Only horizontal text used.");
				}
			}
		}

		return orientation;
	}

	/**
	 * @param estimateParagraphs

	 * @throws PdfException
	 */
	private int mergeLinesTogether(int currentWritingMode, boolean estimateParagraphs, int x1, int x2, int y1, int y2) throws PdfException {

		String separator;

		int[] indices;

		// used for working out alignment
		int middlePage;

		/**
		 * create local copies of
		 */
		float[] f_x1, f_x2, f_y1, f_y2;

		if (currentWritingMode == PdfData.HORIZONTAL_LEFT_TO_RIGHT) {
			f_x1 = this.f_x1;
			f_x2 = this.f_x2;
			f_y1 = this.f_y1;
			f_y2 = this.f_y2;
			indices = getsortedUnusedFragments(false, true);
			middlePage = (x1 + x2) / 2;
		}
		else
			if (currentWritingMode == PdfData.HORIZONTAL_RIGHT_TO_LEFT) {
				f_x2 = this.f_x1;
				f_x1 = this.f_x2;
				f_y1 = this.f_y1;
				f_y2 = this.f_y2;
				indices = getsortedUnusedFragments(false, true);
				middlePage = (x1 + x2) / 2;
			}
			else
				if (currentWritingMode == PdfData.VERTICAL_BOTTOM_TO_TOP) {
					f_x1 = this.f_y1;
					f_x2 = this.f_y2;
					f_y1 = this.f_x2;
					f_y2 = this.f_x1;
					indices = getsortedUnusedFragments(true, true);

					indices = reverse(indices);
					middlePage = (y1 + y2) / 2;

				}
				else
					if (currentWritingMode == PdfData.VERTICAL_TOP_TO_BOTTOM) {
						f_x1 = this.f_y2;
						f_x2 = this.f_y1;
						f_y2 = this.f_x2;
						f_y1 = this.f_x1;
						indices = getsortedUnusedFragments(true, true);
						middlePage = (y1 + y2) / 2;
					}
					else {
						throw new PdfException("Illegal value " + currentWritingMode + "for currentWritingMode");
					}
		int quarter = middlePage / 2;
		int count = indices.length;
		int master = indices[count - 1];

		/**
		 * now loop through all lines merging
		 */
		int ClastChar, MlastChar, CFirstChar;
		final boolean debug = false;

		for (int i = count - 2; i > -1; i--) {

			int child = indices[i];
			separator = "";

			/** add formatting in to retain structure */
			// text to see if lasts ends with . and next starts with capital

			// -1 if no chars
			ClastChar = getLastChar(this.content[child]);
			if (debug) {

				CFirstChar = getFirstChar(this.content[child]);
				MlastChar = getLastChar(this.content[master]);

				StringBuilder child_textX = Strip.stripXML(this.content[child], this.isXMLExtraction);
				String master_textX = Strip.stripXML(this.content[master], this.isXMLExtraction).toString();

			}

			if (ClastChar != -1) {

				addAlignmentFormatting(estimateParagraphs, middlePage, f_x1, f_x2, quarter, child);

				// see if we insert a line break and merge
				String lineSpace = "</p>" + SystemSeparator + "<p>";
				if (this.isXMLExtraction) lineSpace = SystemSeparator;

				float gap = f_y2[master] - f_y1[child];
				float line_height = f_y1[child] - f_y2[child];
				if (currentWritingMode == PdfData.VERTICAL_BOTTOM_TO_TOP) {
					gap = -gap;
					line_height = -line_height;
				}

				if ((gap > line_height) & (line_height > 0)) { // add in line gaps

					while (gap > line_height) {
						separator = separator + lineSpace;
						gap = gap - line_height;
					}

					if (this.isXMLExtraction) separator = separator + "</p>" + SystemSeparator + "<p>";
					else separator = SystemSeparator;

				}
				else
					if (estimateParagraphs == true) {

						CFirstChar = getFirstChar(this.content[child]);
						MlastChar = getLastChar(this.content[master]);

						if ((((MlastChar == '.')) || (((MlastChar == '\"')))) && ((CFirstChar >= 'A') && (CFirstChar <= 'Z'))) {
							if (this.isXMLExtraction) separator = "</p>" + SystemSeparator + "<p>";
							else separator = SystemSeparator;
						}

					}
					else {
						if (this.isXMLExtraction) {
							this.content[child].insert(0, "</p>" + SystemSeparator + "<p>");
						}
						else this.content[master].append(SystemSeparator);
					}

				merge(master, child, separator, false);

			}
		}
		return master;
	}

	private int getFirstChar(StringBuilder buffer) {

		int i = -1;
		boolean inTag = false;
		int count = buffer.length();
		char openChar = ' ';
		int ptr = 0;

		while (ptr < count) {
			char nextChar = buffer.charAt(ptr);

			if ((!inTag) && ((nextChar == '<') || (this.isXMLExtraction && nextChar == '&'))) {
				inTag = true;
				openChar = nextChar;

				// trap & .... &xx; or other spurious
				if ((openChar == '&')) {
					if ((ptr + 1) == count) {
						i = '&';
						ptr = count;
					}
					else {
						char c = buffer.charAt(ptr + 1);

						if ((c != '#') && (c != 'g') && (c != 'l')) {
							i = '&';
							ptr = count;
						}
					}
				}
			}

			if ((!inTag) && (nextChar != ' ')) {
				i = nextChar;
				ptr = count;
			}

			// allow for valid & in stream
			if ((inTag) && (openChar == '&') && (nextChar == ' ')) {
				i = openChar;
				ptr = count;
			}
			else
				if ((inTag) && ((nextChar == '>') || (this.isXMLExtraction && openChar == '&' && nextChar == ';'))) {

					// put back < or >
					if ((nextChar == ';') && (openChar == '&') && (ptr > 2) & (buffer.charAt(ptr - 1) == 't')) {
						if ((buffer.charAt(ptr - 2) == 'l')) {
							i = '<';
							ptr = count;
						}
						else
							if ((buffer.charAt(ptr - 2) == 'g')) {
								i = '>';
								ptr = count;
							}
					}

					inTag = false;
				}

			ptr++;
		}

		return i;
	}

	/** return char as int or -1 if no match */
	private int getLastChar(StringBuilder buffer) {

		int i = -1;
		boolean inTag = false;
		int count = buffer.length();
		int size = count;
		char openChar = ' ';
		count--; // knock 1 off so points to last char

		while (count > -1) {
			char nextChar = buffer.charAt(count);

			// trap &xx;;
			if (inTag && openChar == ';' && nextChar == ';') {
				i = ';';
				count = -1;
			}

			if (!inTag && (nextChar == '>' || (this.isXMLExtraction && nextChar == ';'))) {
				inTag = true;

				// check it is a token and not just > at end
				int lastTokenStart = buffer.lastIndexOf("</"); // find start of this tag if exists
				if (lastTokenStart == -1) { // no tag so ignore
					inTag = false;

				}
				else { // see if real token by looking for invalid chars inside and reject if found
					char charToTest;
					for (int ptr = lastTokenStart; ptr < count; ptr++) {
						charToTest = buffer.charAt(ptr);
						if (charToTest == ' ' || charToTest == '>') {
							inTag = false;
							ptr = count;
						}
					}
				}

				if (inTag) openChar = nextChar;
				else {
					i = nextChar;
					count = -1;
				}
			}

			if (!inTag && nextChar != 32) {
				i = nextChar;
				count = -1;
			}

			if (nextChar == '<' || (this.isXMLExtraction && openChar == ';' && nextChar == '&')) {
				inTag = false;

				// put back < or >
				if ((nextChar == '&') && (count + 3 < size) & (buffer.charAt(count + 2) == 't') && (buffer.charAt(count + 3) == ';')) {
					if ((buffer.charAt(count + 1) == 'l')) {
						i = '<';
						count = -1;
					}
					else
						if ((buffer.charAt(count + 1) == 'g')) {
							i = '>';
							count = -1;
						}
				}
			}

			if (inTag && openChar == ';' && nextChar == ' ') {
				count = -1;
				i = ';';
			}
			count--;
		}

		return i;
	}

	/**
	 * reverse order in matrix so back to front
	 */
	private static int[] reverse(int[] indices) {
		int count = indices.length;
		int[] newIndex = new int[count];
		for (int i = 0; i < count; i++) {
			newIndex[i] = indices[count - i - 1];
		}
		return newIndex;
	}

	/**
	 * used to add LEFT,CENTER,RIGHT tags into XML when extracting text
	 */
	private void addAlignmentFormatting(boolean estimateParagraphs, int middlePage, float[] f_x1, float[] f_x2, int quarter, int child) {
		// put in some alignment
		float left_gap = middlePage - f_x1[child];
		float right_gap = f_x2[child] - middlePage;
		if ((!estimateParagraphs) && (this.isXMLExtraction) && (left_gap > 0) && (right_gap > 0) && (f_x1[child] > quarter)
				&& (f_x1[child] < (middlePage + quarter))) {

			float ratio = left_gap / right_gap;
			if (ratio > 1) ratio = 1 / ratio;

			if (ratio > 0.95) { // add centring if seems centered around middle
				this.content[child] = new StringBuilder(Fonts.cleanupTokens(this.content[child].toString()));
				this.content[child].insert(0, "<center>");
				this.content[child].append("</center>\n");
			}
			else
				if ((right_gap < 10) & (left_gap > 30)) { // add right align
					this.content[child] = new StringBuilder(Fonts.cleanupTokens(this.content[child].toString()));
					this.content[child].insert(0, "<right>");
					this.content[child].append("</right>\n");

				}
		}
	}

	/**
	 * convert fragments into lines of text
	 */
	/**
	 * convert fragments into lines of text
	 */
	private void createLines(int count, int[] items, int mode, boolean breakOnSpace, boolean addMultiplespaceXMLTag, boolean sameLineOnly)
			throws PdfException {

		String separator;

		final boolean debug = false;

		/**
		 * create local copies of arrays
		 */
		float[] f_x1, f_x2, f_y1, f_y2;

		/**
		 * reverse order if text right to left
		 */
		if (mode == PdfData.HORIZONTAL_RIGHT_TO_LEFT || mode == PdfData.VERTICAL_TOP_TO_BOTTOM) items = reverse(items);

		/**
		 * set pointers so left to right text
		 */
		if (mode == PdfData.HORIZONTAL_LEFT_TO_RIGHT) {
			f_x1 = this.f_x1;
			f_x2 = this.f_x2;
			f_y1 = this.f_y1;
			f_y2 = this.f_y2;
		}
		else
			if (mode == PdfData.HORIZONTAL_RIGHT_TO_LEFT) {
				f_x2 = this.f_x1;
				f_x1 = this.f_x2;
				f_y1 = this.f_y1;
				f_y2 = this.f_y2;
			}
			else
				if (mode == PdfData.VERTICAL_BOTTOM_TO_TOP) {
					f_x1 = this.f_y1;
					f_x2 = this.f_y2;
					f_y1 = this.f_x2;
					f_y2 = this.f_x1;
				}
				else
					if (mode == PdfData.VERTICAL_TOP_TO_BOTTOM) {
						f_x1 = this.f_y2;
						f_x2 = this.f_y1;
						f_y2 = this.f_x1;
						f_y1 = this.f_x2;
					}
					else {
						throw new PdfException("Illegal value " + mode + "for currentWritingMode");
					}

		/**
		 * scan items joining best fit to right of each fragment to build lines. This is tedious and processor intensive but necessary as the order
		 * cannot be guaranteed
		 */
		for (int j = 0; j < count; j++) {

			int id = -1, i;
			int c = items[j];

			float smallest_gap = -1, gap, yMidPt;
			if (!this.isUsed[c] && this.writingMode[c] == mode) {

				if (debug) System.out.println("Look for match with " + removeHiddenMarkers(this.content[c].toString()));

				while (true) {
					for (int j2 = 0; j2 < count; j2++) {
						i = items[j2];

						if (this.isUsed[i] == false) {

							// amount of variation in bottom of text
							int baseLineDifference = (int) (f_y2[i] - f_y2[c]);
							if (baseLineDifference < 0) baseLineDifference = -baseLineDifference;

							// amount of variation in bottom of text
							int topLineDifference = (int) (f_y1[i] - f_y1[c]);
							if (topLineDifference < 0) topLineDifference = -topLineDifference;

							// line gap
							int lineGap = (int) (f_x1[i] - f_x2[c]);

							// Check if fragments are closer from the other end
							if (lineGap > (int) (f_x1[c] - f_x2[i])) lineGap = (int) (f_x1[c] - f_x2[i]);

							int fontSizeChange = this.fontSize[c] - this.fontSize[i];
							if (fontSizeChange < 0) fontSizeChange = -fontSizeChange;

							if (debug) System.out.println("Against " + removeHiddenMarkers(this.content[i].toString()));

							if (sameLineOnly && lineGap > this.fontSize[c] && lineGap > 0) { // ignore text in wrong order allowing slight margin for
																								// error
								// allow for multicolumns with gap

								if (debug) System.out.println("case1 lineGap=" + lineGap);
								// //Case removed as it broke one file and had no effect on other files
								// }else if (sameLineOnly && (lineGap > (fontSize[c]*10)|| lineGap > (fontSize[i]*10)) ) { //JUMP IN TEXT SIZE ACROSS
								// COL
								// //ignore
								//
								// if(debug)
								// System.out.println("case2");
							}
							else
								if (sameLineOnly && baseLineDifference > 1 && lineGap > 2 * this.fontSize[c]
										&& (this.fontSize[c] == this.fontSize[i])) { // TEXT SLIGHTLY OFFSET
									// ignore
									if (debug) System.out.println("case3");
								}
								else
									if (sameLineOnly && baseLineDifference > 3) {
										// ignore
										if (debug) System.out.println("case4");
									}
									else
										if (sameLineOnly && fontSizeChange > 2) {
											// ignore
											if (debug) System.out.println("case5");
										}
										else
											if (i != c
													&& ((f_x1[i] > f_x1[c] && mode != PdfData.VERTICAL_TOP_TO_BOTTOM) || f_x1[i] < f_x1[c]
															&& mode == PdfData.VERTICAL_TOP_TO_BOTTOM && this.writingMode[c] == mode
															&& (!(fontSizeChange > 2) || (fontSizeChange > 2 && topLineDifference < 3)))) { // see if
																																			// on
																																			// right

												gap = (f_x1[i] - f_x2[c]);

												if (debug) System.out.println("case6 gap=" + gap);

												if (mode == PdfData.HORIZONTAL_RIGHT_TO_LEFT || mode == PdfData.VERTICAL_TOP_TO_BOTTOM) gap = -gap;

												// allow for fp error
												if ((gap < 0) && (gap > -2)) gap = 0;

												// make sure on right
												yMidPt = (f_y1[i] + f_y2[i]) / 2;

												// see if line & if only or better fit
												if ((yMidPt < f_y1[c]) && (yMidPt > f_y2[c]) && ((smallest_gap < 0) || (gap < smallest_gap))) {
													smallest_gap = gap;
													id = i;
												}
											}
						}
					}

					// merge on next right item or exit when no more matches
					if (id == -1) break;

					float possSpace = f_x1[id] - f_x2[c];
					if (mode == PdfData.HORIZONTAL_RIGHT_TO_LEFT || mode == PdfData.VERTICAL_TOP_TO_BOTTOM) possSpace = -possSpace;
					else
						if (mode == PdfData.VERTICAL_BOTTOM_TO_TOP) possSpace = (f_x2[id] - f_x1[c]);

					// add space if gap between this and last object
					separator = isGapASpace(c, id, possSpace, addMultiplespaceXMLTag, mode);

					/** merge if adjoin */
					if ((breakOnSpace) && (this.hadSpace != null) && ((this.hadSpace[c]) || (separator.startsWith(" ")))) break;

					merge(c, id, separator, true);

					id = -1; // reset
					smallest_gap = 1000000; // and reset the gap

				}
			}
		}
	}

	static class ResultsComparator implements Comparator {
		private int rotation;

		public ResultsComparator(int rotation) {
			this.rotation = rotation;
		}

		@Override
		public int compare(Object o1, Object o2) {
			Rectangle[] ra1;
			Rectangle[] ra2;

			if (o1 instanceof Rectangle[]) {
				ra1 = (Rectangle[]) o1;
			}
			else ra1 = new Rectangle[] { (Rectangle) o1 };

			if (o2 instanceof Rectangle[]) {
				ra2 = (Rectangle[]) o2;
			}
			else ra2 = new Rectangle[] { (Rectangle) o2 };

			for (int i = 0; i != ra1.length; i++)
				for (int j = 0; j != ra2.length; j++) { // do we need this loop?
					Rectangle r1 = ra1[i];
					Rectangle r2 = ra2[j];

					switch (this.rotation) {
						case 0:
							if (r1.y == r2.y) { // the two words on on the same level so pick the one on the left
								if (r1.x > r2.x) return 1;
								else return -1;
							}
							else
								if (r1.y > r2.y) { // the first word is above the second, so pick the first
									return -1;
								}

							return 1;// the second word is above the first, so pick the second

						case 90:
							if (r1.x == r2.x) { // the two words on on the same level so pick the one on the left
								if (r1.y > r2.y) return 1;
								else return -1;
							}
							else
								if (r1.x > r2.x) // the first word is above the second, so pick the first
								return 1;

							return -1; // the second word is above the first, so pick the second

						case 180:
							if (r1.y == r2.y) { // the two words on on the same level so pick the one on the left
								if (r1.x > r2.x) return 1;
								else return -1;
							}
							else
								if (r1.y > r2.y) { // the first word is above the second, so pick the first
									return -1;
								}

							return 1;// the second word is above the first, so pick the second

						case 270:
							if (r1.x == r2.x) { // the two words on on the same level so pick the one on the left
								if (r1.y > r2.y) return 1;
								else return -1;
							}
							else
								if (r1.x < r2.x) // the first word is above the second, so pick the first
								return 1;

							return -1; // the second word is above the first, so pick the second
					}

					// Orginal code kept incase of mistake.
					// if (rotation == 0 || rotation == 180) {
					// if (r1.y == r2.y) { // the two words on on the same level so pick the one on the left
					// if (r1.x > r2.x)
					// return 1;
					// else
					// return -1;
					// } else if (r1.y > r2.y) { // the first word is above the second, so pick the first
					// return -1;
					// }
					//
					// return 1; // the second word is above the first, so pick the second
					// }
					// else { // rotation == 90 or 270
					// if (r1.x == r2.x) { // the two words on on the same level so pick the one on the left
					// if (r1.y > r2.y)
					// return 1;
					// else
					// return -1;
					// } else if (r1.x > r2.x) // the first word is above the second, so pick the first
					// return 1;
					//
					// return -1; // the second word is above the first, so pick the second
					// }
				}
			return -1; // the second word is above the first, so pick the second
		}
	}

	// <link><a name="findMultipleTermsInRectangleWithMatchingTeasers" />
	/**
	 * Algorithm to find multiple text terms in x1,y1,x2,y2 rectangle on <b>page_number</b>, with matching teaser
	 * 
	 * @param x1
	 *            the left x cord
	 * @param y1
	 *            the upper y cord
	 * @param x2
	 *            the right x cord
	 * @param y2
	 *            the lower y cord
	 * @param rotation
	 *            the rotation of the page to be searched
	 * @param page_number
	 *            the page number to search on
	 * @param terms
	 *            the terms to search for
	 * @param searchType
	 *            searchType the search type made up from one or more constants obtained from the SearchType class
	 * @param listener
	 *            an implementation of SearchListener is required, this is to enable searching to be cancelled
	 * @return a SortedMap containing a collection of Rectangle describing the location of found text, mapped to a String which is the matching teaser
	 * @throws PdfException
	 *             If the co-ordinates are not valid
	 */
	public SortedMap findMultipleTermsInRectangleWithMatchingTeasers(int x1, int y1, int x2, int y2, final int rotation, int page_number,
			String[] terms, int searchType, SearchListener listener) throws PdfException {

		this.usingMultipleTerms = true;
		this.multipleTermTeasers.clear();
		this.teasers = null;

		boolean origIncludeTease = this.includeTease;

		this.includeTease = true;

		List highlights = findMultipleTermsInRectangle(x1, y1, x2, y2, page_number, terms, searchType, listener);

		SortedMap highlightsWithTeasers = new TreeMap(new ResultsComparator(rotation));

		for (int i = 0; i < highlights.size(); i++) {

			/* highlights.get(i) is a rectangle or a rectangle[] */
			highlightsWithTeasers.put(highlights.get(i), this.multipleTermTeasers.get(i));
		}

		this.usingMultipleTerms = false;

		this.includeTease = origIncludeTease;

		return highlightsWithTeasers;
	}

	// <link><a name="findMultipleTermsInRectangle" />
	/**
	 * Algorithm to find multiple text terms in x1,y1,x2,y2 rectangle on <b>page_number</b>.
	 * 
	 * @param x1
	 *            the left x cord
	 * @param y1
	 *            the upper y cord
	 * @param x2
	 *            the right x cord
	 * @param y2
	 *            the lower y cord
	 * @param rotation
	 *            the rotation of the page to be searched
	 * @param page_number
	 *            the page number to search on
	 * @param terms
	 *            the terms to search for
	 * @param orderResults
	 *            if true the list that is returned is ordered to return the resulting rectangles in a logical order descending down the page, if
	 *            false, rectangles for multiple terms are grouped together.
	 * @param searchType
	 *            searchType the search type made up from one or more constants obtained from the SearchType class
	 * @param listener
	 *            an implementation of SearchListener is required, this is to enable searching to be cancelled
	 * @return a list of Rectangle describing the location of found text
	 * @throws PdfException
	 *             If the co-ordinates are not valid
	 */
	public List findMultipleTermsInRectangle(int x1, int y1, int x2, int y2, final int rotation, int page_number, String[] terms,
			boolean orderResults, int searchType, SearchListener listener) throws PdfException {

		this.usingMultipleTerms = true;
		this.multipleTermTeasers.clear();
		this.teasers = null;

		List highlights = findMultipleTermsInRectangle(x1, y1, x2, y2, page_number, terms, searchType, listener);

		if (orderResults) {
			Collections.sort(highlights, new ResultsComparator(rotation));
		}

		this.usingMultipleTerms = false;

		return highlights;
	}

	private List findMultipleTermsInRectangle(int x1, int y1, int x2, int y2, int page_number, String[] terms, int searchType, SearchListener listener)
			throws PdfException {

		List list = new ArrayList();

		for (String term : terms) {
			if (listener != null && listener.isCanceled()) {
				// System.out.println("RETURNING EARLY");
				break;
			}

			float[] co_ords;

			co_ords = findText(new Rectangle(x1, y1, x2, y2), page_number, new String[] { term }, searchType);

			if (co_ords != null) {
				int count = co_ords.length;
				for (int ii = 0; ii < count; ii = ii + 5) {

					int wx1 = (int) co_ords[ii];
					int wy1 = (int) co_ords[ii + 1];
					int wx2 = (int) co_ords[ii + 2];
					int wy2 = (int) co_ords[ii + 3];

					Rectangle rectangle = new Rectangle(wx1, wy2, wx2 - wx1, wy1 - wy2);

					int seperator = (int) co_ords[ii + 4];

					if (seperator == this.linkedSearchAreas) {
						Vector_Rectangle vr = new Vector_Rectangle();
						vr.addElement(rectangle);
						while (seperator == this.linkedSearchAreas) {
							ii = ii + 5;
							wx1 = (int) co_ords[ii];
							wy1 = (int) co_ords[ii + 1];
							wx2 = (int) co_ords[ii + 2];
							wy2 = (int) co_ords[ii + 3];
							seperator = (int) co_ords[ii + 4];
							rectangle = new Rectangle(wx1, wy2, wx2 - wx1, wy1 - wy2);
							vr.addElement(rectangle);
						}
						vr.trim();
						list.add(vr.get());
					}
					else {
						list.add(rectangle);
					}
				}
			}
		}
		return list;
	}

	// <link><a name="findTextInRectangle" />
	/**
	 * Method to find text in the specified area allowing for the text to be split across multiple lines.</br>
	 * 
	 * @param searchArea
	 *            = Area on page to search. If null search whole page
	 * @param page_number
	 *            = the current page to search
	 * @param terms
	 *            = the text to search for
	 * @param searchType
	 *            = info on how to search the pdf
	 * @return the coords of the found text in a float[] where the coords are pdf page coords. The origin of the coords is the bottom left hand corner
	 *         (on unrotated page) organised in the following order.</br> [0]=result x1 coord</br> [1]=result y1 coord</br> [2]=result x2 coord</br>
	 *         [3]=result y2 coord</br> [4]=either -101 to show that the next text area is the remainder of this word on another line else any other
	 *         value is ignored.</br>
	 * @throws PdfException
	 */
	final public float[] findText(Rectangle searchArea, int page_number, String[] terms, int searchType) throws PdfException {

		// Failed to supply search terms to do nothing
		if (terms == null) return new float[] {};

		// Flags to control the different search options
		boolean firstOccuranceOnly = false;
		boolean wholeWordsOnly = false;
		boolean foundFirst = false;
		boolean useRegEx = false;

		// Search result and teaser holders
		Vector_Float resultCoords = new Vector_Float(0);
		Vector_String resultTeasers = new Vector_String(0);

		// Extract the text data into local arrays for searching
		copyToArrays();

		// Remove any hidden text on page as should not be found
		cleanupShadowsAndDrownedObjects(false);

		// Get unused text objects and sort them for correct searching
		int[] items = getsortedUnusedFragments(true, false);

		/**
		 * check orientation and get preferred. Items not correct will be ignored
		 */
		int l2r = 0;
		int r2l = 0;
		int t2b = 0;
		int b2t = 0;

		for (int i = 0; i != items.length; i++) {
			switch (this.writingMode[items[i]]) {
				case 0:
					l2r++;
					break;
				case 1:
					r2l++;
					break;
				case 2:
					t2b++;
					break;
				case 3:
					b2t++;
					break;
			}
		}

		int[] unsorted = new int[] { l2r, r2l, t2b, b2t };
		int[] sorted = new int[] { l2r, r2l, t2b, b2t };

		// Set all to -1 so we can tell if it's been set yet
		int[] writingModes = new int[] { -1, -1, -1, -1 };

		Arrays.sort(sorted);

		for (int i = 0; i != unsorted.length; i++) {
			for (int j = 0; j < sorted.length; j++) {
				if (unsorted[i] == sorted[j]) {

					int pos = j - 3;
					if (pos < 0) pos = -pos;

					if (writingModes[pos] == -1) {
						writingModes[pos] = i;
						j = sorted.length;
					}
				}
			}
		}

		for (int u = 0; u != writingModes.length; u++) {

			int writingMode = writingModes[u];

			// if not lines for writing mode, ignore
			if (unsorted[writingMode] != 0) {

				// Merge text fragments into lines as displayed on page
				createLines(items.length, items, writingMode, true, false, true);

				// Bitwise flags for regular expressions engine, options always required
				int options = 0;

				// Turn on case sensitive mode
				if ((searchType & SearchType.CASE_SENSITIVE) != SearchType.CASE_SENSITIVE) {
					options = (options | Pattern.CASE_INSENSITIVE);
				}

				// Only find first occurance of each search term
				if ((searchType & SearchType.FIND_FIRST_OCCURANCE_ONLY) == SearchType.FIND_FIRST_OCCURANCE_ONLY) {
					firstOccuranceOnly = true;
				}

				// Only find whole words, not partial words
				if ((searchType & SearchType.WHOLE_WORDS_ONLY) == SearchType.WHOLE_WORDS_ONLY) {
					wholeWordsOnly = true;
				}

				// Allow search to find split line results
				if ((searchType & SearchType.MUTLI_LINE_RESULTS) == SearchType.MUTLI_LINE_RESULTS) {
					options = (options | Pattern.MULTILINE | Pattern.DOTALL);
				}

				// Allow the use of regular expressions symbols
				if ((searchType & SearchType.USE_REGULAR_EXPRESSIONS) == SearchType.USE_REGULAR_EXPRESSIONS) {
					useRegEx = true;
				}

				/**
				 * create local copies of arrays
				 */
				float[] f_y1 = this.f_y1, f_y2 = this.f_y2;

				/**
				 * swap around x and y so rountine works on all cases
				 */
				boolean valuesSwapped = false;
				if (writingMode == PdfData.HORIZONTAL_LEFT_TO_RIGHT) {
					f_y1 = this.f_y1;
					f_y2 = this.f_y2;
				}
				else
					if (writingMode == PdfData.HORIZONTAL_RIGHT_TO_LEFT) {
						f_y1 = this.f_y1;
						f_y2 = this.f_y2;
					}
					else
						if (writingMode == PdfData.VERTICAL_BOTTOM_TO_TOP) {
							f_y1 = this.f_x2;
							f_y2 = this.f_x1;
							valuesSwapped = true;
						}
						else
							if (writingMode == PdfData.VERTICAL_TOP_TO_BOTTOM) {
								f_y2 = this.f_x1;
								f_y1 = this.f_x2;
								valuesSwapped = true;
							}

				// Portions of text to perform the search on and find teasers
				String[] searchText;
				String[] coordsText;

				// Merge all text into one with \n line separators
				// This will allow checking for multi line split results
				String plain = "";
				String raw = "";
				for (int i = 0; i != this.content.length; i++) {
					if (this.content[i] != null && writingMode == this.writingMode[i]) {

						raw += this.content[i] + "\n";
						plain += this.content[i] + "\n";
					}
				}

				// Remove double spaces, replacing them with single spaces
				raw = removeDuplicateSpaces(raw);
				plain = removeDuplicateSpaces(plain);

				// Strip xml from content and keep coords and text data
				raw = Strip.stripXML(raw, this.isXMLExtraction).toString();

				// Strip xml and coords data from content and keep text data
				plain = removeHiddenMarkers(plain);
				plain = Strip.stripXML(plain, this.isXMLExtraction).toString();

				// Store text in the search and teaser arrays
				searchText = new String[] { plain };
				coordsText = new String[] { raw };

				// Hold starting point data at page rotation
				Point resultStart;

				// Work through the search terms one at a time
				for (int j = 0; j != terms.length; j++) {

					String searchValue = terms[j];

					// Set the default separator between words in a search term
					String sep = " ";

					// Multiline needs space or newline to be recognised as word separators
					if ((searchType & SearchType.MUTLI_LINE_RESULTS) == SearchType.MUTLI_LINE_RESULTS) {
						sep = "[ \\\\n]";
					}

					// if not using reg ex add reg ex literal flags around the text and word separators
					if (!useRegEx) {
						searchValue = "\\Q" + searchValue + "\\E";
						sep = "\\\\E" + sep + "\\\\Q";
					}

					// If word seperator has changed, replace all spaces with modified seperator
					if (!sep.equals(" ")) {
						searchValue = searchValue.replaceAll(" ", sep);
					}

					// Surround search term with word boundry tags to match whole words
					if (wholeWordsOnly) searchValue = "\\b" + searchValue + "\\b";

					// Create pattern to match search term
					Pattern searchTerm = Pattern.compile(searchValue, options);

					// Create pattern to match search term with two words before and after
					Pattern teaserTerm = Pattern.compile("(?:\\S+\\s)?\\S*(?:\\S+\\s)?\\S*" + searchValue + "\\S*(?:\\s\\S+)?\\S*(?:\\s\\S+)?",
							options);

					// Loop through all search text
					for (int i = 0; i != searchText.length; i++) {

						// Get text data and text+coord data
						String plainText = searchText[i];
						String coordText = coordsText[i];

						// So long as text data is not null
						if (plainText != null) {

							// Create two matchers for finding search term and teaser
							Matcher termFinder = searchTerm.matcher(plainText);
							Matcher teaserFinder = teaserTerm.matcher(plainText);
							boolean needToFindTeaser = true;

							// Keep looping till no result is returned
							while (termFinder.find()) {
								resultStart = null;
								// Make note of the text found and index in the text
								String foundTerm = termFinder.group();
								int termStarts = termFinder.start();
								int termEnds = termFinder.end() - 1;

								// If storing teasers
								if (this.includeTease) {

									// Store the term found as a default value
									String teaser = foundTerm;

									if (this.includeHTMLtags) teaser = "<b>" + teaser + "</b>";

									boolean itemFound = false;
									if (needToFindTeaser) {
										itemFound = teaserFinder.find();
									}

									if (itemFound) {
										// Get a teaser if found and set the search term to bold is allowed
										if (teaserFinder.start() < termStarts && teaserFinder.end() > termEnds) {

											// replace default with found teaser
											teaser = teaserFinder.group();

											if (this.includeHTMLtags) {
												// Calculate points to add bold tags
												int teaseStarts = termStarts - teaserFinder.start();
												int teaseEnds = (termEnds - teaserFinder.start()) + 1;

												// Add bold tags
												teaser = teaser.substring(0, teaseStarts) + "<b>" + teaser.substring(teaseStarts, teaseEnds) + "</b>"
														+ teaser.substring(teaseEnds, teaser.length());
											}
											needToFindTeaser = true;
										}
										else {
											needToFindTeaser = false;
										}
									}

									// Store teaser
									resultTeasers.addElement(teaser);
								}

								// Get coords of found text for highlights
								float currentX;
								float width;

								// Track point in text data line (without coord data)
								int pointInLine = -1;

								// Track line on page
								int lineCounter = 0;

								// Skip null values and value not in the correct writing mode to ensure correct result coords
								while (this.content[lineCounter] == null || writingMode != this.writingMode[lineCounter])
									lineCounter++;

								// Flags used to catch if result is split accross lines
								boolean startFound = false;
								boolean endFound = false;

								// Cycle through coord text looking for coords of this result
								// Ignore first value as it is known to be the first marker
								for (int pointer = 1; pointer < coordText.length(); pointer++) {

									// find second marker and get x coord
									int startPointer = pointer;
									while (pointer < coordText.length()) {
										if (coordText.charAt(pointer) == MARKER2) break;
										pointer++;
									}

									// Convert text to float value for x coord
									currentX = Float.parseFloat(coordText.substring(startPointer, pointer));
									pointer++;

									// find third marker and get width
									startPointer = pointer;
									while (pointer < coordText.length()) {
										if (coordText.charAt(pointer) == MARKER2) break;

										pointer++;
									}

									// Convert text to float value for character width
									width = Float.parseFloat(coordText.substring(startPointer, pointer));
									pointer++;

									// find fourth marker and get text (character)
									startPointer = pointer;
									while (pointer < coordText.length()) {
										if (coordText.charAt(pointer) == MARKER2) break;

										pointer++;
									}

									// Store text to check for newline character later
									String text = coordText.substring(startPointer, pointer);
									pointInLine += text.length();

									// Start of term not found yet.
									// Point in line is equal to or greater than start of the term.
									// Store coords and mark start as found.
									if (!startFound && pointInLine >= termStarts) {
										resultStart = new Point((int) currentX, (int) f_y1[lineCounter]);
										startFound = true;
									}

									// End of term not found yet.
									// Point in line is equal to or greater than end of the term.
									// Store coords and mark end as found.
									if (!endFound && pointInLine >= termEnds) {
										if (valuesSwapped) {
											if (writingMode == PdfData.VERTICAL_BOTTOM_TO_TOP) {
												resultCoords.addElement((int) f_y2[lineCounter]);
												resultCoords.addElement((int) currentX + width);
												resultCoords.addElement(resultStart.y);
												resultCoords.addElement(resultStart.x);
												resultCoords.addElement(0.0f);
											}
											else {
												resultCoords.addElement((int) f_y2[lineCounter]);
												resultCoords.addElement(resultStart.x);
												resultCoords.addElement(resultStart.y);
												resultCoords.addElement((int) currentX + width);
												resultCoords.addElement(0.0f);
											}
										}
										else {
											resultCoords.addElement(resultStart.x);
											resultCoords.addElement(resultStart.y);
											resultCoords.addElement(currentX + width);
											resultCoords.addElement(f_y2[lineCounter]);
											resultCoords.addElement(0.0f);
										}

										endFound = true;
									}

									// Using multi line option.
									// Start of term found.
									// End of term not found.
									// New line character found.
									// Set up multi line result.
									if (startFound && !endFound && text.contains("\n")) {
										// Set ends coords
										if (valuesSwapped) {
											if (writingMode == PdfData.VERTICAL_BOTTOM_TO_TOP) {
												resultCoords.addElement((int) f_y2[lineCounter]);
												resultCoords.addElement((int) currentX + width);
												resultCoords.addElement(resultStart.y);
												resultCoords.addElement(resultStart.x);
												resultCoords.addElement(this.linkedSearchAreas); // Mark next result as linked

											}
											else {
												resultCoords.addElement((int) f_y2[lineCounter]);
												resultCoords.addElement(resultStart.x);
												resultCoords.addElement(resultStart.y);
												resultCoords.addElement((int) currentX + width);
												resultCoords.addElement(this.linkedSearchAreas); // Mark next result as linked

											}
										}
										else {
											resultCoords.addElement(resultStart.x);
											resultCoords.addElement(resultStart.y);
											resultCoords.addElement(currentX + width);
											resultCoords.addElement(f_y2[lineCounter]);
											resultCoords.addElement(this.linkedSearchAreas); // Mark next result as linked
										}
										// Set start of term as not found
										startFound = false;

										// Set this point in line as start of next term
										// Guarantees next character is found as
										// start of the next part of the search term
										termStarts = pointInLine;
									}

									// In multiline mode we progress the line number when we find a \n
									// This is to allow the correct calculation of y coords
									if (text.contains("\n")) {
										lineCounter++;

										// If current content pointed at is null or not the correct writing mode, skip value until data is found
										while (lineCounter < this.content.length
												&& (this.content[lineCounter] == null || writingMode != this.writingMode[lineCounter])) {
											lineCounter++;
										}
									}

								}

								// If only finding first occurance,
								// Stop searching this text data for search term.
								if (firstOccuranceOnly) {
									foundFirst = true;
									break;
								}
							}

							// If only finding first occurance and first is found,
							// Stop searching all text data for this search term.
							if (firstOccuranceOnly && foundFirst) {
								break;
							}
						}
					}

				}

				// Remove any trailing empty values
				resultCoords.trim();

				// If including tease values
				if (this.includeTease) {

					// Remove any trailing empty values
					resultTeasers.trim();

					// Store teasers so they can be retrieved by different search methods
					if (this.usingMultipleTerms) {
						// Store all teasers for so they may be returned as a sorted map
						// Only used for one method controled by the above flag
						for (int i = 0; i != resultTeasers.size(); i++)
							this.multipleTermTeasers.add(resultTeasers.elementAt(i));
					}
					else {
						// Store all teasers to be retrieved by getTeaser() method
						this.teasers = resultTeasers.get();
					}
				}
			}
		}
		// Return coord data for search results
		return resultCoords.get();
	}

	private static String removeDuplicateSpaces(String textValue) {

		if (textValue.contains("  ")) {

			textValue = textValue.replace("  ", " ");

		}
		return textValue;
	}

	/** return endpoints from last findtext */
	public float[] getEndPoints() {
		return this.endPoints;
	}

	/**
	 * return text teasers from findtext if generateTeasers() called before find
	 */
	public String[] getTeasers() {
		return this.teasers;
	}

	/**
	 * tell find text to generate teasers as well
	 */
	public void generateTeasers() {

		this.includeTease = true;
	}
}
