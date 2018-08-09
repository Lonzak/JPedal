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
 * ShowPageSize.java
 * ---------------
 */

/**
 * example written to show pagesize of all pages on system
 */
package org.jpedal.examples;

import org.jpedal.PdfDecoder;
import org.jpedal.objects.PdfPageData;

public class ShowPageSize {

	public ShowPageSize(String file_name) {

		PdfDecoder decode_pdf = new PdfDecoder(false); // false as no display

		try {
			decode_pdf.openPdfFile(file_name);

			/** get page count */
			int pageCount = decode_pdf.getPageCount();
			System.out.println("Page count=" + pageCount);

			// get PageData object
			PdfPageData pageData = decode_pdf.getPdfPageData();
			// show all page sizes
			for (int ii = 0; ii < pageCount; ii++) {

				if (pageData.getRotation(ii) != 0) {
					System.out.println("Has rotation " + pageData.getRotation(ii) + " degrees");
				}

				// pixels
				System.out.print("page (size in pixels) " + ii + " mediaBox=" + pageData.getMediaBoxX(ii) + ' ' + pageData.getMediaBoxY(ii) + ' '
						+ pageData.getMediaBoxWidth(ii) + ' ' + pageData.getMediaBoxHeight(ii) + " CropBox=" + pageData.getCropBoxX(ii) + ' '
						+ pageData.getCropBoxY(ii) + ' ' + pageData.getCropBoxWidth(ii) + ' ' + pageData.getCropBoxHeight(ii));

				// inches
				float factor = 72f; // 72 is the usual screen dpi
				System.out.print(" (size in inches) " + ii + " mediaBox=" + pageData.getMediaBoxX(ii) / factor + ' ' + pageData.getMediaBoxY(ii)
						/ factor + ' ' + pageData.getMediaBoxWidth(ii) / factor + ' ' + pageData.getMediaBoxHeight(ii) / factor + " CropBox="
						+ pageData.getCropBoxX(ii) / factor + ' ' + pageData.getCropBoxY(ii) / factor + pageData.getCropBoxWidth(ii) / factor + ' '
						+ pageData.getCropBoxHeight(ii) / factor);

				// cm
				factor = 72f / 2.54f;
				System.out.print(" (size in cm) " + ii + " mediaBox=" + pageData.getMediaBoxX(ii) / factor + ' ' + pageData.getMediaBoxY(ii) / factor
						+ ' ' + pageData.getMediaBoxWidth(ii) / factor + ' ' + pageData.getMediaBoxHeight(ii) / factor + " CropBox="
						+ pageData.getCropBoxX(ii) / factor + ' ' + pageData.getCropBoxY(ii) / factor + pageData.getCropBoxWidth(ii) / factor + ' '
						+ pageData.getCropBoxHeight(ii) / factor + '\n');

			}

			/** close the pdf file */
			decode_pdf.closePdfFile();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** main method to run the software as standalone application */
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Please pass in file name (including path");
		}
		else {
			new ShowPageSize(args[0]);
		}
	}
}
