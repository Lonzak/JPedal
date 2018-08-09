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
 * ShowIfEmbeddedFontsUsed.java
 * ---------------
 */

/**
 * This example opens a pdf file to see if fonts are embedded
 *
 */
package org.jpedal.examples;

import java.io.File;

import org.jpedal.PdfDecoder;

public class ShowIfEmbeddedFontsUsed {

	/** user dir in which program can write */
	private String user_dir = System.getProperty("user.dir");

	/** sample file which can be setup - substitute your own. */
	private static String test_file = "/mnt/win_d/sample.pdf";

	// not to be used
	// private ShowIfEmbeddedFontsUsed() {}

	// ////////////////////////////////////////////////////////////////////////
	/** example method to open a file and return the number of pages */
	public ShowIfEmbeddedFontsUsed(String file_name) {
		String separator = System.getProperty("file.separator");

		// check output dir has separator
		if (this.user_dir.endsWith(separator) == false) this.user_dir = this.user_dir + separator;

		/**
		 * set up PdfDecoder object telling it whether to display messages and where to find its lookup tables
		 */
		PdfDecoder decode_pdf;

		// PdfDecoder returns a PdfException if there is a problem
		try {
			decode_pdf = new PdfDecoder(false); // false as no GUI display needed

			/**
			 * open the file (and read metadata including pages in file)
			 */
			System.out.println("Opening file :" + file_name);
			decode_pdf.openPdfFile(file_name);

			/** see if file contains embedded fonts */
			System.out.println("File contains embedded fonts=" + decode_pdf.PDFContainsEmbeddedFonts());

			/** close the pdf file */
			decode_pdf.closePdfFile();

		}
		catch (Exception e) {
			System.err.println("2.Exception " + e + " in pdf code");

		}
	}

	// ////////////////////////////////////////////////////////////////////////
	/**
	 * main routine which checks for any files passed and runs the demo
	 */
	public static void main(String[] args) {
		System.out.println("Simple demo to see if file contains embedded fonts");

		// set to default
		String file_name = test_file;

		// check user has passed us a filename and use default if none
		if (args.length != 1) System.out
				.println("Please pass the file name and any path (ie \"C:/sample.pdf\" ) as a command line value - use double quotes if it includes spaces");
		else {
			file_name = args[0];
			System.out.println("File :" + file_name);
		}

		// check file exists
		File pdf_file = new File(file_name);

		// if file exists, open and get number of pages
		if (pdf_file.exists() == false) System.out.println("File " + file_name + " not found");
		else {
			new ShowIfEmbeddedFontsUsed(file_name);
		}
	}
}
