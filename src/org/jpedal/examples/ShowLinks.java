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
 * ShowLinks.java
 * ---------------
 */

/**
 * This example opens a pdf file and gets the document links
 */
package org.jpedal.examples;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.jpedal.PdfDecoder;
import org.jpedal.objects.raw.FormObject;
import org.jpedal.objects.raw.PdfArrayIterator;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;

public class ShowLinks {

	/** flag to add links to image of page as well if set */
	private boolean includeImages = true;

	/** example method to open a file and return the links */
	public ShowLinks(String file_name) {

		BufferedImage img = null;

		PdfDecoder decodePdf;

		if (this.includeImages) {
			decodePdf = new PdfDecoder(true);
		}
		else {
			decodePdf = new PdfDecoder(false);
		}

		try {
			decodePdf.openPdfFile(file_name);

			/**
			 * form code here
			 */
			// new list we can parse
			for (int ii = 1; ii < decodePdf.getPageCount() + 1; ii++) {

				// the list of Annots from the file
				PdfArrayIterator annotListForPage = decodePdf.getFormRenderer().getAnnotsOnPage(ii);

				if (annotListForPage != null && annotListForPage.getTokenCount() > 0) { // can have empty lists

					// get image if needed and save
					if (this.includeImages) {
						img = decodePdf.getPageAsImage(ii);

					}

					while (annotListForPage.hasMoreTokens()) {

						// get ID of annot which has already been decoded and get actual object
						String annotKey = annotListForPage.getNextValueAsString(true);

						Object[] rawObj = decodePdf.getFormRenderer().getCompData().getRawForm(annotKey);
						for (Object aRawObj : rawObj) {
							if (aRawObj != null) {
								// each PDF annot object - extract data from it
								FormObject annotObj = (FormObject) aRawObj;

								int subtype = annotObj.getParameterConstant(PdfDictionary.Subtype);

								if (subtype == PdfDictionary.Link) {

									// PDF co-ords
									System.out.println("link object");
									float[] coords = annotObj.getFloatArray(PdfDictionary.Rect);
									System.out.println("PDF Rect= " + coords[0] + ' ' + coords[1] + ' ' + coords[2] + ' ' + coords[3]);

									// convert to Javaspace rectangle by subtracting page Crop Height
									int pageH = decodePdf.getPdfPageData().getCropBoxHeight(ii);
									float x = coords[0];

									float w = coords[2] - coords[0];
									float h = coords[3] - coords[1];
									float y = pageH - coords[1] - h; // note we remove h from y
									System.out.println("Javaspace Rect x=" + x + " y=" + y + " w=" + w + " h=" + h);

									// draw on image as example
									// get image if needed and save
									if (this.includeImages) {

										// as an example draw onto page
										Graphics2D g2 = (Graphics2D) img.getGraphics();
										g2.setPaint(Color.RED);
										g2.drawRect((int) x, (int) y, (int) w, (int) h);

									}

									// text in A subobject
									PdfObject aData = annotObj.getDictionary(PdfDictionary.A);
									if (aData != null && aData.getNameAsConstant(PdfDictionary.S) == PdfDictionary.URI) {
										String text = aData.getTextStreamValue(PdfDictionary.URI); // +"ZZ"; deliberately broken first to test
																									// checking
										System.out.println("text=" + text);
									}
								}
							}
						}
					}
				}

				// get image if needed and save
				if (this.includeImages) {

					ImageIO.write(img, "PNG", new File("image-" + ii + ".png"));
				}
			}

			/** close the pdf file */
			decodePdf.closePdfFile();

		}
		catch (Exception e) {
			e.printStackTrace();

		}
	}

	// ////////////////////////////////////////////////////////////////////////
	/**
	 * main routine which checks for any files passed and runs the sample code
	 */
	public static void main(String[] args) {
		System.out.println("Simple demo to extract pdf file links if any");

		// set to default
		String file_name;

		// check user has passed us a filename
		if (args.length != 1) {
			System.out.println("No filename given or  wrong number of values");
		}
		else {
			file_name = args[0];
			System.out.println("File :" + file_name);

			// check file exists
			File pdf_file = new File(file_name);

			// if file exists, open and show links
			if (pdf_file.exists() == false) {
				System.out.println("File " + file_name + " not found");
			}
			else {
				new ShowLinks(file_name);
			}
		}

		System.exit(1);
	}
}
