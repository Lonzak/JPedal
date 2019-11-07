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
 * PaperSizes.java
 * ---------------
 */
package org.jpedal.examples.viewer.paper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;

public class PaperSizes {

	Map<String, MarginPaper> paperDefinitions = new HashMap<String, MarginPaper>();
	ArrayList<String> paperList = new ArrayList<String>();

	private static final double mmToSubInch = 72 / 25.4;

	Map<String, String> paperNames = new HashMap<String, String>();

	/** default for paper selection */
	private int defaultPageIndex = 0;
	private String defaultSize;

	private PrintService printService;

	public PaperSizes(PrintService printService) {
		this.defaultSize = null;
		populateNameMap();
		addCustomPaperSizes();
		setPrintService(printService);
	}

	public PaperSizes(String defaultSize) {
		this.defaultSize = defaultSize;
		populateNameMap();
		addCustomPaperSizes();
	}

	public String[] getAvailablePaperSizes() {
		Object[] objs = this.paperList.toArray();
		String[] names = new String[objs.length];
		for (int i = 0; i < objs.length; i++) {
			names[i] = (String) objs[i];
		}
		return names;
	}

	/** return selected Paper */
	public MarginPaper getSelectedPaper(Object id) {
		return this.paperDefinitions.get(id);
	}

	/**
	 * method to setup specific Paper sizes - add your own here to extend list
	 */
	private static void addCustomPaperSizes() {

		// String printDescription;
		// MarginPaper paper;
		// defintion for each Paper - must match

		/**
		 * //A4 (border) printDescription="A4"; paper = new Paper(); paper.setSize(595, 842); paper.setImageableArea(43, 43, 509, 756);
		 * paperDefinitions.put(printDescription,paper); paperList.add(printDescription); /
		 **/
		/**
		 * //A4 (borderless) printDescription="A4 (borderless)"; paper = new Paper(); paper.setSize(595, 842); paper.setImageableArea(0, 0, 595, 842);
		 * paperDefinitions.put(printDescription,paper); paperList.add(printDescription); /
		 **/

		/**
		 * //A5 printDescription="A5"; paper = new Paper(); paper.setSize(420, 595); paper.setImageableArea(43,43,334,509);
		 * paperDefinitions.put(printDescription,paper); paperList.add(printDescription);
		 * 
		 * //Added for Adobe printDescription="US Letter (8.5 x 11)"; paper = new Paper(); paper.setSize(612, 792);
		 * paper.setImageableArea(43,43,526,706); paperDefinitions.put(printDescription,paper); paperList.add(printDescription); /
		 **/
		/**
		 * //custom printDescription="Custom 2.9cm x 8.9cm"; int customW=(int) (29*2.83); int customH=(int) (89*2.83); //2.83 is scaling factor to
		 * convert mm to pixels paper = new Paper(); paper.setSize(customW, customH); paper.setImageableArea(0,0,customW,customH); //MUST BE SET ALSO
		 * paperDefinitions.put(printDescription,paper); paperList.add(printDescription);
		 */

		/**
		 * kept in but commented out for general usage //architectural D (1728x2592) printDescription="Architectural D"; paper = new Paper();
		 * paper.setSize(1728, 2592); paper.setImageableArea(25,25,1703,2567); paperDefinitions.put(printDescription,paper);
		 * paperList.add(printDescription); /
		 **/

		// Add your own here
	}

	/**
	 * Checks flags and locale and chooses a default paper size
	 */
	private void setDefault() {

		if (this.paperList == null) return;

		// set default value
		this.defaultPageIndex = -1;

		// check JVM flag
		String paperSizeFlag = System.getProperty("org.jpedal.printPaperSize");
		if (paperSizeFlag != null) {
			for (int i = 0; i < this.paperList.size(); i++) {
				if (this.paperList.get(i).equals(paperSizeFlag)) {
					this.defaultPageIndex = i;
				}
			}
		}

		// Check properties file value (passed in)
		if (this.defaultPageIndex == -1 && this.defaultSize != null && this.defaultSize.length() > 0) {
			for (int i = 0; i < this.paperList.size(); i++) {
				if (this.defaultSize.equals(this.paperList.get(i))) this.defaultPageIndex = i;
			}
		}

		// if no default specified check location and choose
		if (this.defaultPageIndex == -1) {
			this.defaultSize = "A4";

			// Check for US countries
			final String[] letterSizeDefaults = new String[] { "US", "CA", "MX", "CO", "VE", "AR", "CL", "PH" };
			final String country = Locale.getDefault().getCountry();
			for (String letterSizeDefault : letterSizeDefaults) {
				if (country.equals(letterSizeDefault)) {
					this.defaultSize = "North American Letter";
				}
			}

			// Get index
			for (int j = 0; j < this.paperList.size(); j++) {
				if (this.defaultSize.equals(this.paperList.get(j))) this.defaultPageIndex = j;
			}

			// Make sure not negative
			if (this.defaultPageIndex == -1) this.defaultPageIndex = 0;
		}
	}

	/**
	 * Sets the print service and checks which page sizes are available
	 * 
	 * @param p
	 *            print service
	 */
	public synchronized void setPrintService(PrintService p) {
		this.printService = p;
		this.paperDefinitions = new HashMap<String, MarginPaper>();
		this.paperList = new ArrayList<String>();

		checkAndAddSize(MediaSizeName.ISO_A4);
		checkAndAddSize(MediaSizeName.NA_LETTER);
		checkAndAddSize(MediaSizeName.ISO_A0);
		checkAndAddSize(MediaSizeName.ISO_A1);
		checkAndAddSize(MediaSizeName.ISO_A2);
		checkAndAddSize(MediaSizeName.ISO_A3);
		checkAndAddSize(MediaSizeName.ISO_A5);
		checkAndAddSize(MediaSizeName.ISO_A6);
		checkAndAddSize(MediaSizeName.ISO_A7);
		checkAndAddSize(MediaSizeName.ISO_A8);
		checkAndAddSize(MediaSizeName.ISO_A9);
		checkAndAddSize(MediaSizeName.ISO_A10);
		checkAndAddSize(MediaSizeName.ISO_B0);
		checkAndAddSize(MediaSizeName.ISO_B1);
		checkAndAddSize(MediaSizeName.ISO_B2);
		checkAndAddSize(MediaSizeName.ISO_B3);
		checkAndAddSize(MediaSizeName.ISO_B4);
		checkAndAddSize(MediaSizeName.ISO_B5);
		checkAndAddSize(MediaSizeName.ISO_B6);
		checkAndAddSize(MediaSizeName.ISO_B7);
		checkAndAddSize(MediaSizeName.ISO_B8);
		checkAndAddSize(MediaSizeName.ISO_B9);
		checkAndAddSize(MediaSizeName.ISO_B10);
		checkAndAddSize(MediaSizeName.JIS_B0);
		checkAndAddSize(MediaSizeName.JIS_B1);
		checkAndAddSize(MediaSizeName.JIS_B2);
		checkAndAddSize(MediaSizeName.JIS_B3);
		checkAndAddSize(MediaSizeName.JIS_B4);
		checkAndAddSize(MediaSizeName.JIS_B5);
		checkAndAddSize(MediaSizeName.JIS_B6);
		checkAndAddSize(MediaSizeName.JIS_B7);
		checkAndAddSize(MediaSizeName.JIS_B8);
		checkAndAddSize(MediaSizeName.JIS_B9);
		checkAndAddSize(MediaSizeName.JIS_B10);
		checkAndAddSize(MediaSizeName.ISO_C0);
		checkAndAddSize(MediaSizeName.ISO_C1);
		checkAndAddSize(MediaSizeName.ISO_C2);
		checkAndAddSize(MediaSizeName.ISO_C3);
		checkAndAddSize(MediaSizeName.ISO_C4);
		checkAndAddSize(MediaSizeName.ISO_C5);
		checkAndAddSize(MediaSizeName.ISO_C6);
		checkAndAddSize(MediaSizeName.NA_LEGAL);
		checkAndAddSize(MediaSizeName.EXECUTIVE);
		checkAndAddSize(MediaSizeName.LEDGER);
		checkAndAddSize(MediaSizeName.TABLOID);
		checkAndAddSize(MediaSizeName.INVOICE);
		checkAndAddSize(MediaSizeName.FOLIO);
		checkAndAddSize(MediaSizeName.QUARTO);
		checkAndAddSize(MediaSizeName.JAPANESE_POSTCARD);
		checkAndAddSize(MediaSizeName.JAPANESE_DOUBLE_POSTCARD);
		checkAndAddSize(MediaSizeName.A);
		checkAndAddSize(MediaSizeName.B);
		checkAndAddSize(MediaSizeName.C);
		checkAndAddSize(MediaSizeName.D);
		checkAndAddSize(MediaSizeName.E);
		checkAndAddSize(MediaSizeName.ISO_DESIGNATED_LONG);
		checkAndAddSize(MediaSizeName.ITALY_ENVELOPE);
		checkAndAddSize(MediaSizeName.MONARCH_ENVELOPE);
		checkAndAddSize(MediaSizeName.PERSONAL_ENVELOPE);
		checkAndAddSize(MediaSizeName.NA_NUMBER_9_ENVELOPE);
		checkAndAddSize(MediaSizeName.NA_NUMBER_10_ENVELOPE);
		checkAndAddSize(MediaSizeName.NA_NUMBER_11_ENVELOPE);
		checkAndAddSize(MediaSizeName.NA_NUMBER_12_ENVELOPE);
		checkAndAddSize(MediaSizeName.NA_NUMBER_14_ENVELOPE);
		checkAndAddSize(MediaSizeName.NA_6X9_ENVELOPE);
		checkAndAddSize(MediaSizeName.NA_7X9_ENVELOPE);
		checkAndAddSize(MediaSizeName.NA_9X11_ENVELOPE);
		checkAndAddSize(MediaSizeName.NA_9X12_ENVELOPE);
		checkAndAddSize(MediaSizeName.NA_10X13_ENVELOPE);
		checkAndAddSize(MediaSizeName.NA_10X14_ENVELOPE);
		checkAndAddSize(MediaSizeName.NA_10X15_ENVELOPE);
		checkAndAddSize(MediaSizeName.NA_5X7);
		checkAndAddSize(MediaSizeName.NA_8X10);

		addCustomPaperSizes();

		setDefault();
	}

	public String[] getPaperSizes() {
		String[] result = new String[] { this.paperNames.get(MediaSizeName.ISO_A4.toString()),
				this.paperNames.get(MediaSizeName.NA_LETTER.toString()), this.paperNames.get(MediaSizeName.ISO_A0.toString()),
				this.paperNames.get(MediaSizeName.ISO_A1.toString()), this.paperNames.get(MediaSizeName.ISO_A2.toString()),
				this.paperNames.get(MediaSizeName.ISO_A3.toString()), this.paperNames.get(MediaSizeName.ISO_A5.toString()),
				this.paperNames.get(MediaSizeName.ISO_A6.toString()), this.paperNames.get(MediaSizeName.ISO_A7.toString()),
				this.paperNames.get(MediaSizeName.ISO_A8.toString()), this.paperNames.get(MediaSizeName.ISO_A9.toString()),
				this.paperNames.get(MediaSizeName.ISO_A10.toString()), this.paperNames.get(MediaSizeName.ISO_B0.toString()),
				this.paperNames.get(MediaSizeName.ISO_B1.toString()), this.paperNames.get(MediaSizeName.ISO_B2.toString()),
				this.paperNames.get(MediaSizeName.ISO_B3.toString()), this.paperNames.get(MediaSizeName.ISO_B4.toString()),
				this.paperNames.get(MediaSizeName.ISO_B5.toString()), this.paperNames.get(MediaSizeName.ISO_B6.toString()),
				this.paperNames.get(MediaSizeName.ISO_B7.toString()), this.paperNames.get(MediaSizeName.ISO_B8.toString()),
				this.paperNames.get(MediaSizeName.ISO_B9.toString()), this.paperNames.get(MediaSizeName.ISO_B10.toString()),
				this.paperNames.get(MediaSizeName.JIS_B0.toString()), this.paperNames.get(MediaSizeName.JIS_B1.toString()),
				this.paperNames.get(MediaSizeName.JIS_B2.toString()), this.paperNames.get(MediaSizeName.JIS_B3.toString()),
				this.paperNames.get(MediaSizeName.JIS_B4.toString()), this.paperNames.get(MediaSizeName.JIS_B5.toString()),
				this.paperNames.get(MediaSizeName.JIS_B6.toString()), this.paperNames.get(MediaSizeName.JIS_B7.toString()),
				this.paperNames.get(MediaSizeName.JIS_B8.toString()), this.paperNames.get(MediaSizeName.JIS_B9.toString()),
				this.paperNames.get(MediaSizeName.JIS_B10.toString()), this.paperNames.get(MediaSizeName.ISO_C0.toString()),
				this.paperNames.get(MediaSizeName.ISO_C1.toString()), this.paperNames.get(MediaSizeName.ISO_C2.toString()),
				this.paperNames.get(MediaSizeName.ISO_C3.toString()), this.paperNames.get(MediaSizeName.ISO_C4.toString()),
				this.paperNames.get(MediaSizeName.ISO_C5.toString()), this.paperNames.get(MediaSizeName.ISO_C6.toString()),
				this.paperNames.get(MediaSizeName.NA_LEGAL.toString()), this.paperNames.get(MediaSizeName.EXECUTIVE.toString()),
				this.paperNames.get(MediaSizeName.LEDGER.toString()), this.paperNames.get(MediaSizeName.TABLOID.toString()),
				this.paperNames.get(MediaSizeName.INVOICE.toString()), this.paperNames.get(MediaSizeName.FOLIO.toString()),
				this.paperNames.get(MediaSizeName.QUARTO.toString()), this.paperNames.get(MediaSizeName.JAPANESE_POSTCARD.toString()),
				this.paperNames.get(MediaSizeName.JAPANESE_DOUBLE_POSTCARD.toString()), this.paperNames.get(MediaSizeName.A.toString()),
				this.paperNames.get(MediaSizeName.B.toString()), this.paperNames.get(MediaSizeName.C.toString()),
				this.paperNames.get(MediaSizeName.D.toString()), this.paperNames.get(MediaSizeName.E.toString()),
				this.paperNames.get(MediaSizeName.ISO_DESIGNATED_LONG.toString()), this.paperNames.get(MediaSizeName.ITALY_ENVELOPE.toString()),
				this.paperNames.get(MediaSizeName.MONARCH_ENVELOPE.toString()), this.paperNames.get(MediaSizeName.PERSONAL_ENVELOPE.toString()),
				this.paperNames.get(MediaSizeName.NA_NUMBER_9_ENVELOPE.toString()),
				this.paperNames.get(MediaSizeName.NA_NUMBER_10_ENVELOPE.toString()),
				this.paperNames.get(MediaSizeName.NA_NUMBER_11_ENVELOPE.toString()),
				this.paperNames.get(MediaSizeName.NA_NUMBER_12_ENVELOPE.toString()),
				this.paperNames.get(MediaSizeName.NA_NUMBER_14_ENVELOPE.toString()), this.paperNames.get(MediaSizeName.NA_6X9_ENVELOPE.toString()),
				this.paperNames.get(MediaSizeName.NA_7X9_ENVELOPE.toString()), this.paperNames.get(MediaSizeName.NA_9X11_ENVELOPE.toString()),
				this.paperNames.get(MediaSizeName.NA_9X12_ENVELOPE.toString()), this.paperNames.get(MediaSizeName.NA_10X13_ENVELOPE.toString()),
				this.paperNames.get(MediaSizeName.NA_10X14_ENVELOPE.toString()), this.paperNames.get(MediaSizeName.NA_10X15_ENVELOPE.toString()),
				this.paperNames.get(MediaSizeName.NA_5X7.toString()), this.paperNames.get(MediaSizeName.NA_8X10.toString()) };

		return result;
	}

	/**
	 * Checks whether a paper size is available and adds it to the array
	 * 
	 * @param name
	 *            The MediaSizeName to check
	 */
	private void checkAndAddSize(MediaSizeName name) {

		// Check if available on this printer
		PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
		if (!this.printService.isAttributeValueSupported(name, new DocFlavor.BYTE_ARRAY(DocFlavor.BYTE_ARRAY.PNG.getMimeType()), attributes)) return;

		// Get name and lookup in our name map
		Object o = this.paperNames.get(name.toString());
		String printDescription;
		if (o != null) printDescription = o.toString();
		else printDescription = name.toString();

		// Get paper size
		MediaSize size = MediaSize.getMediaSizeForName(name);
		double pX = size.getX(Size2DSyntax.MM);
		double pY = size.getY(Size2DSyntax.MM);

		// Get printable area
		attributes.add(name);
		MediaPrintableArea[] area = (MediaPrintableArea[]) this.printService.getSupportedAttributeValues(MediaPrintableArea.class, null, attributes);

		if (area.length == 0) return;

		int useArea = 0;
		if (area[useArea] == null) for (int i = 0; i != area.length && area[useArea] == null; i++)
			useArea = i;

		float[] values = area[useArea].getPrintableArea(MediaPrintableArea.MM);

		// Check if very near to pagesize since pagesize is stored less accurately (avoids rounding/negative issues)
		if (values[2] > pX - 0.5 && values[2] < pX + 0.5) values[2] = (float) pX;
		if (values[3] > pY - 0.5 && values[3] < pY + 0.5) values[3] = (float) pY;

		// Check if printer thinks page is other way round - flip pagesize if so
		if (values[2] > pX ^ values[3] > pY) {
			double temp = pX;
			pX = pY;
			pY = temp;
		}

		// Create and store as Paper object
		MarginPaper paper = new MarginPaper();
		paper.setSize(pX * mmToSubInch, pY * mmToSubInch);
		paper.setMinImageableArea(values[0] * mmToSubInch, values[1] * mmToSubInch, values[2] * mmToSubInch, values[3] * mmToSubInch);

		this.paperDefinitions.put(printDescription, paper);
		this.paperList.add(printDescription);
	}

	/**
	 * @return the index of the default paper size
	 */
	public int getDefaultPageIndex() {
		return this.defaultPageIndex;
	}

	/**
	 * Fills the name map from standardised to "pretty" names
	 */
	private void populateNameMap() {
		this.paperNames.put("iso-a0", "A0");
		this.paperNames.put("iso-a1", "A1");
		this.paperNames.put("iso-a2", "A2");
		this.paperNames.put("iso-a3", "A3");
		this.paperNames.put("iso-a4", "A4");
		this.paperNames.put("iso-a5", "A5");
		this.paperNames.put("iso-a6", "A6");
		this.paperNames.put("iso-a7", "A7");
		this.paperNames.put("iso-a8", "A8");
		this.paperNames.put("iso-a9", "A9");
		this.paperNames.put("iso-a10", "A10");
		this.paperNames.put("iso-b0", "B0");
		this.paperNames.put("iso-b1", "B1");
		this.paperNames.put("iso-b2", "B2");
		this.paperNames.put("iso-b3", "B3");
		this.paperNames.put("iso-b4", "B4");
		this.paperNames.put("iso-b5", "B5");
		this.paperNames.put("iso-b6", "B6");
		this.paperNames.put("iso-b7", "B7");
		this.paperNames.put("iso-b8", "B8");
		this.paperNames.put("iso-b9", "B9");
		this.paperNames.put("iso-b10", "B10");
		this.paperNames.put("na-letter", "North American Letter");
		this.paperNames.put("na-legal", "North American Legal");
		this.paperNames.put("na-8x10", "North American 8x10 inch");
		this.paperNames.put("na-5x7", "North American 5x7 inch");
		this.paperNames.put("executive", "Executive");
		this.paperNames.put("folio", "Folio");
		this.paperNames.put("invoice", "Invoice");
		this.paperNames.put("tabloid", "Tabloid");
		this.paperNames.put("ledger", "Ledger");
		this.paperNames.put("quarto", "Quarto");
		this.paperNames.put("iso-c0", "C0");
		this.paperNames.put("iso-c1", "C1");
		this.paperNames.put("iso-c2", "C2");
		this.paperNames.put("iso-c3", "C3");
		this.paperNames.put("iso-c4", "C4");
		this.paperNames.put("iso-c5", "C5");
		this.paperNames.put("iso-c6", "C6");
		this.paperNames.put("iso-designated-long", "ISO Designated Long size");
		this.paperNames.put("na-10x13-envelope", "North American 10x13 inch");
		this.paperNames.put("na-9x12-envelope", "North American 9x12 inch");
		this.paperNames.put("na-number-10-envelope", "North American number 10 business envelope");
		this.paperNames.put("na-7x9-envelope", "North American 7x9 inch envelope");
		this.paperNames.put("na-9x11-envelope", "North American 9x11 inch envelope");
		this.paperNames.put("na-10x14-envelope", "North American 10x14 inch envelope");
		this.paperNames.put("na-number-9-envelope", "North American number 9 business envelope");
		this.paperNames.put("na-6x9-envelope", "North American 6x9 inch envelope");
		this.paperNames.put("na-10x15-envelope", "North American 10x15 inch envelope");
		this.paperNames.put("monarch-envelope", "Monarch envelope");
		this.paperNames.put("jis-b0", "Japanese B0");
		this.paperNames.put("jis-b1", "Japanese B1");
		this.paperNames.put("jis-b2", "Japanese B2");
		this.paperNames.put("jis-b3", "Japanese B3");
		this.paperNames.put("jis-b4", "Japanese B4");
		this.paperNames.put("jis-b5", "Japanese B5");
		this.paperNames.put("jis-b6", "Japanese B6");
		this.paperNames.put("jis-b7", "Japanese B7");
		this.paperNames.put("jis-b8", "Japanese B8");
		this.paperNames.put("jis-b9", "Japanese B9");
		this.paperNames.put("jis-b10", "Japanese B10");
		this.paperNames.put("a", "Engineering ANSI A");
		this.paperNames.put("b", "Engineering ANSI B");
		this.paperNames.put("c", "Engineering ANSI C");
		this.paperNames.put("d", "Engineering ANSI D");
		this.paperNames.put("e", "Engineering ANSI E");
		this.paperNames.put("arch-a", "Architectural A");
		this.paperNames.put("arch-b", "Architectural B");
		this.paperNames.put("arch-c", "Architectural C");
		this.paperNames.put("arch-d", "Architectural D");
		this.paperNames.put("arch-e", "Architectural E");
		this.paperNames.put("japanese-postcard", "Japanese Postcard");
		this.paperNames.put("oufuko-postcard", "Oufuko Postcard");
		this.paperNames.put("italian-envelope", "Italian Envelope");
		this.paperNames.put("personal-envelope", "Personal Envelope");
		this.paperNames.put("na-number-11-envelope", "North American Number 11 Envelope");
		this.paperNames.put("na-number-12-envelope", "North American Number 12 Envelope");
		this.paperNames.put("na-number-14-envelope", "North American Number 14 Envelope");
	}
}
