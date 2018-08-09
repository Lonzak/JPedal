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
 * StructuredContentHandler.java
 * ---------------
 */
package org.jpedal.objects.structuredtext;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jpedal.io.PdfObjectReader;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * structured content
 */
public class StructuredContentHandler {

	/** flag to show if we add co-ordinates to merely tagged content */
	private boolean addCoordinates = false;

	/** store entries from BMC */
	private Map markedContentProperties;

	/** handle nested levels of marked content */
	private int markedContentLevel = 0;

	/** stream of marked content */
	private StringBuffer markedContentSequence;

	static final private boolean debug = false;

	private boolean contentExtracted = false;

	private String currentKey;

	private Map keys, values, dictionaries;

	PdfObjectReader currentPdfFile;

	boolean buildDirectly = false;

	Document doc;

	Element root;

	private float x1, y1, x2, y2;

	public StructuredContentHandler(Object markedContent) {

		// build either tree of lookuptable
		if (markedContent instanceof Map) {
			this.buildDirectly = false;
			this.values = (Map) markedContent;
		}
		else {
			this.buildDirectly = true;
			this.doc = (Document) markedContent;
			this.root = this.doc.createElement("TaggedPDF-doc");
			this.doc.appendChild(this.root);
		}

		if (debug) System.out.println("BuildDirectly=" + this.buildDirectly);

		// this.currentPdfFile=currentPdfFile;

		this.markedContentProperties = new HashMap();
		this.markedContentLevel = 0;

		this.markedContentSequence = new StringBuffer();

		this.currentKey = "";

		this.keys = new HashMap();

		this.dictionaries = new HashMap();
	}

	public void MP() {
	}

	public void DP(PdfObject BDCobj) {

		if (debug) {
			System.out.println("DP----------------------------------------------------------" + this.markedContentLevel);

			System.out.println(BDCobj);

			System.out.println("BDCobj=" + BDCobj);

		}
	}

	public void BDC(PdfObject BDCobj) {

		// if start of sequence, reinitialise settings
		if (this.markedContentLevel == 0) this.markedContentSequence = new StringBuffer();

		this.markedContentLevel++;

		// only used in direct mode and breaks non-direct code so remove
		if (this.buildDirectly) BDCobj.setIntNumber(PdfDictionary.MCID, -1);

		int MCID = BDCobj.getInt(PdfDictionary.MCID);

		// save key

		if (MCID != -1) this.keys.put(this.markedContentLevel, String.valueOf(MCID));

		this.dictionaries.put(String.valueOf(this.markedContentLevel), BDCobj);

		if (debug) {
			System.out.println("BDC----------------------------------------------------------" + this.markedContentLevel + " MCID=" + MCID);
			System.out.println("BDCobj=" + BDCobj);
		}
	}

	public void BMC(String op) {

		// stip off /
		if (op.startsWith("/")) op = op.substring(1);

		// if start of sequence, reinitialise settings
		if (this.markedContentLevel == 0) this.markedContentSequence = new StringBuffer();

		this.markedContentProperties.put(this.markedContentLevel, op);

		this.markedContentLevel++;

		if (debug) System.out.println("BMC----------------------------------------------------------level=" + this.markedContentLevel + " raw op="
				+ op);

		// save label and any dictionary
		this.keys.put(this.markedContentLevel, op);

		if (this.buildDirectly) {
			// read any dictionay work out type
			// PdfObject dict=(PdfObject) dictionaries.get(currentKey);
			// boolean isBMC=dict==null;

			// add node with name for BMC
			if (op != null) {
				// System.out.println(op+" "+root.getElementsByTagName(op));
				Element newRoot = (Element) this.root.getElementsByTagName(op).item(0);

				if (newRoot == null) {
					newRoot = this.doc.createElement(op);
					this.root.appendChild(newRoot);
				}
				this.root = newRoot;
			}
		}
	}

	public void EMC() {

		// set flag to show some content
		this.contentExtracted = true;

		/**
		 * add current structure to tree
		 **/
		this.currentKey = (String) this.keys.get(this.markedContentLevel);

		// if no MCID use current level as key
		if (this.currentKey == null) this.currentKey = String.valueOf(this.markedContentLevel);

		if (debug) System.out.println("currentKey=" + this.currentKey + ' ' + this.keys);

		if (this.buildDirectly) {

			PdfObject BDCobj = (PdfObject) this.dictionaries.get(this.currentKey);

			boolean isBMC = (BDCobj == null);

			if (debug) System.out.println(isBMC + " " + this.currentKey + ' ' + BDCobj + " markedContentSequence=" + this.markedContentSequence);

			// any custom tags
			if (BDCobj != null) {
				Map metadata = BDCobj.getOtherDictionaries();
				if (metadata != null) {
					Iterator customValues = metadata.keySet().iterator();
					Object key;
					while (customValues.hasNext()) {
						key = customValues.next();
						this.root.setAttribute(key.toString(), metadata.get(key).toString());

						// if(addCoordinates){
						this.root.setAttribute("x1", String.valueOf((int) this.x1));
						this.root.setAttribute("y1", String.valueOf((int) this.y1));
						this.root.setAttribute("x2", String.valueOf((int) this.x2));
						this.root.setAttribute("y2", String.valueOf((int) this.y2));
						// }
					}
				}
			}

			// add node with name for BMC
			if (isBMC) {
				if (this.currentKey != null) {

					Node child = this.doc.createTextNode(stripEscapeChars(this.markedContentSequence.toString()));

					this.root.appendChild(child);

					if (this.addCoordinates) {
						this.root.setAttribute("x1", String.valueOf((int) this.x1));
						this.root.setAttribute("y1", String.valueOf((int) this.y1));
						this.root.setAttribute("x2", String.valueOf((int) this.x2));
						this.root.setAttribute("y2", String.valueOf((int) this.y2));
					}

					Node oldRoot = this.root.getParentNode();
					if (oldRoot instanceof Element) this.root = (Element) oldRoot;
				}
			}
			else {
				// get root key on dictionary (should only be 1)
				// and create node
				// Iterator keys=dict.keySet().iterator();
				String S = "p";// (String) keys.next();

				// System.out.println("dict="+BDCobj.getObjectRefAsString());

				if (S == null) S = "p";

				Element tag = this.doc.createElement(S);
				this.root.appendChild(tag);

				// now add any attributes
				/**
				 * Map atts=(Map) dict.get(S); if(atts==null) atts=(Map)dict.get(null); Iterator attribKeys=atts.keySet().iterator();
				 * while(attribKeys.hasNext()){ String nextAtt=(String) attribKeys.next();
				 * tag.setAttribute(nextAtt,stripEscapeChars(atts.get(nextAtt))); }
				 */
				if (this.addCoordinates) {
					tag.setAttribute("x1", String.valueOf((int) this.x1));
					tag.setAttribute("y1", String.valueOf((int) this.y1));
					tag.setAttribute("x2", String.valueOf((int) this.x2));
					tag.setAttribute("y2", String.valueOf((int) this.y2));
				}

				// add the text
				Node child = this.doc.createTextNode(this.markedContentSequence.toString());
				tag.appendChild(child);
			}

			// reset
			this.markedContentSequence = new StringBuffer();

		}
		else {

			String ContentSequence = this.markedContentSequence.toString();

			/*
			 * if(ContentSequence.indexOf("&amp;")!= -1){ ContentSequence = ContentSequence.replaceAll("&amp;","&"); }
			 * if(ContentSequence.indexOf("&lt;")!= -1){ ContentSequence = ContentSequence.replaceAll("&lt;","<");
			 * //System.out.print(">>>>>>>>>>>> Temp =="+ContentSequence); } if(ContentSequence.indexOf("&gt;")!= -1){ ContentSequence =
			 * ContentSequence.replaceAll("&gt;",">"); } if(ContentSequence.indexOf("&#")!= -1){ //convert hex numbers to the char value }
			 */

			// System.out.println(currentKey+" "+markedContentSequence);
			if (debug) System.out.println("write out " + this.currentKey + " text=" + this.markedContentSequence + '<');

			PdfObject BDCobj = (PdfObject) (this.dictionaries.get(String.valueOf(this.markedContentLevel)));

			// System.out.println("BDCobj="+BDCobj+" currentKey="+currentKey);

			// reset on MCID tag
			int MCID = -1;
			if (BDCobj != null) MCID = BDCobj.getInt(PdfDictionary.MCID);

			if (MCID != -1) {
				this.values.put(String.valueOf(MCID), ContentSequence);
				// System.out.println(MCID+" "+ContentSequence);
				this.markedContentSequence = new StringBuffer();
			}

			// remove used dictionary
			this.dictionaries.remove(String.valueOf(this.markedContentLevel));

		}

		if (this.markedContentLevel > 0) this.markedContentLevel--;

		if (debug) System.out.println("EMC----------------------------------------------------------" + this.markedContentLevel);
	}

	/** store the actual text in the stream */
	public void setText(StringBuffer current_value, float x1, float y1, float x2, float y2) {

		if (this.markedContentSequence.length() == 0) {
			this.markedContentSequence = current_value;

			// lose space at start
			if (this.markedContentSequence.length() > 0 && this.markedContentSequence.charAt(0) == ' ') this.markedContentSequence.deleteCharAt(0);

		}
		else { // add space to tidy up

			char c = ' ', c2 = ' ';

			if (current_value.length() > 0) c = current_value.charAt(0);

			int len = this.markedContentSequence.length() - 1;
			if (len > 0) c2 = this.markedContentSequence.charAt(len);

			if (c2 != '-' && c != '-' && c != '.') this.markedContentSequence.append(' ');

			// System.out.println("\nbit=>"+current_value+"<");
			// System.out.println("whole=>"+markedContentSequence+"<");

			this.markedContentSequence.append(current_value);

		}

		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
	}

	// delete escape chars such as \( but allow for \\
	private static String stripEscapeChars(Object dict) {
		char c, lastC = ' ';

		StringBuilder str = new StringBuilder((String) dict);
		int length = str.length();
		for (int ii = 0; ii < length; ii++) {
			c = str.charAt(ii);
			if (c == '\\' && lastC != '\\') {
				str.deleteCharAt(ii);
				length--;
			}
			lastC = c;

		}

		return str.toString();
	}

	public boolean hasContent() {
		return this.contentExtracted;
	}
}
