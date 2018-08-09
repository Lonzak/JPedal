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
 * NameLookup.java
 * ---------------
 */
package org.jpedal.io;

import java.util.HashMap;

import org.jpedal.objects.Javascript;
import org.jpedal.objects.raw.NamesObject;
import org.jpedal.objects.raw.PdfArrayIterator;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;

/**
 * convert names to refs
 */
public class NameLookup extends HashMap {

	private static final long serialVersionUID = 1604162937228903817L;
	private final PdfFileReader objectReader;

	public NameLookup(PdfFileReader objectReader) {

		this.objectReader = objectReader;
	}

	/**
	 * read any names
	 */
	public void readNames(PdfObject nameObject, Javascript javascript, boolean isKid) {

		ObjectDecoder objectDecoder = new ObjectDecoder(this.objectReader);
		objectDecoder.checkResolved(nameObject);

		/**
		 * loop to read required values into lookup
		 */
		final int[] nameLists = new int[] { PdfDictionary.Dests, PdfDictionary.JavaScript };
		int count = nameLists.length;
		if (isKid) count = 1;

		PdfObject pdfObj;
		PdfArrayIterator namesArray;

		String name, value;

		for (int ii = 0; ii < count; ii++) {

			if (isKid) pdfObj = nameObject;
			else pdfObj = nameObject.getDictionary(nameLists[ii]);

			if (pdfObj == null) continue;

			// any kids
			byte[][] kidList = pdfObj.getKeyArray(PdfDictionary.Kids);
			if (kidList != null) {
				int kidCount = kidList.length;

				/** allow for empty value and put next pages in the queue */
				if (kidCount > 0) {

					for (byte[] aKidList : kidList) {

						String nextValue = new String(aKidList);

						PdfObject nextObject = new NamesObject(nextValue);
						nextObject.ignoreRecursion(false);

						this.objectReader.readObject(nextObject);

						readNames(nextObject, javascript, true);
					}
				}
			}

			// get any names object
			namesArray = pdfObj.getMixedArray(PdfDictionary.Names);

			// read all the values
			if (namesArray != null && namesArray.getTokenCount() > 0) {
				while (namesArray.hasMoreTokens()) {
					name = namesArray.getNextValueAsString(true);

					// fix for baseline_screens/customers-June2011/Bundy_vs_F_Kruger_Sons_Bundy_v_F_Kruger!~!2200.pdf
					// as code assumes paired values and not in this file (List a list)
					if (!namesArray.hasMoreTokens()) continue;

					value = namesArray.getNextValueAsString(true);

					// if Javascript, get full value and store, otherwise just get name
					if (nameLists[ii] == PdfDictionary.JavaScript) {

						/**/
					}
					else // just store
					this.put(name, value);
				}
			}
		}
	}

}
