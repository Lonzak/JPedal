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
 * ItextFunctions.java
 * ---------------
 */
package org.jpedal.examples.viewer.utils;

import org.jpedal.PdfDecoder;
import org.jpedal.examples.viewer.gui.SwingGUI;
import org.jpedal.examples.viewer.gui.popups.AddHeaderFooterToPDFPages;
import org.jpedal.examples.viewer.gui.popups.CropPDFPages;
import org.jpedal.examples.viewer.gui.popups.DeletePDFPages;
import org.jpedal.examples.viewer.gui.popups.EncryptPDFDocument;
import org.jpedal.examples.viewer.gui.popups.ExtractPDFPagesNup;
import org.jpedal.examples.viewer.gui.popups.InsertBlankPDFPage;
import org.jpedal.examples.viewer.gui.popups.RotatePDFPages;
import org.jpedal.examples.viewer.gui.popups.SavePDF;
import org.jpedal.examples.viewer.gui.popups.StampImageToPDFPages;
import org.jpedal.examples.viewer.gui.popups.StampTextToPDFPages;
import org.jpedal.examples.viewer.objects.SignData;
import org.jpedal.objects.PdfPageData;

/** central location to place external code using itext library */
public class ItextFunctions {

	public final static boolean IS_DUMMY = true;

	public final static int ROTATECLOCKWISE = 0;
	public final static int ROTATECOUNTERCLOCKWISE = 1;
	public final static int ROTATE180 = 2;

	public final static int ORDER_ACROSS = 3;
	public final static int ORDER_DOWN = 4;
	public final static int ORDER_STACK = 5;

	public final static int REPEAT_NONE = 6;
	public final static int REPEAT_AUTO = 7;
	public final static int REPEAT_SPECIFIED = 8;

	public static final int NOT_CERTIFIED = -1;// PdfSignatureAppearance.NOT_CERTIFIED;
	public static final int CERTIFIED_NO_CHANGES_ALLOWED = -1;// PdfSignatureAppearance.CERTIFIED_NO_CHANGES_ALLOWED;
	public static final int CERTIFIED_FORM_FILLING = -1;// PdfSignatureAppearance.CERTIFIED_FORM_FILLING;
	public static final int CERTIFIED_FORM_FILLING_AND_ANNOTATIONS = -1;// PdfSignatureAppearance.CERTIFIED_FORM_FILLING_AND_ANNOTATIONS;

	public static final int ALLOW_PRINTING = -1;// PdfWriter.ALLOW_PRINTING;
	public static final int ALLOW_MODIFY_CONTENTS = -1;// PdfWriter.ALLOW_MODIFY_CONTENTS;
	public static final int ALLOW_COPY = -1;// PdfWriter.ALLOW_COPY;
	public static final int ALLOW_MODIFY_ANNOTATIONS = -1;// PdfWriter.ALLOW_MODIFY_ANNOTATIONS;
	public static final int ALLOW_FILL_IN = -1;// PdfWriter.ALLOW_FILL_IN;
	public static final int ALLOW_SCREENREADERS = -1;// PdfWriter.ALLOW_SCREENREADERS;
	public static final int ALLOW_ASSEMBLY = -1;// PdfWriter.ALLOW_ASSEMBLY;
	public static final int ALLOW_DEGRADED_PRINTING = -1;// PdfWriter.ALLOW_DEGRADED_PRINTING;

	public ItextFunctions(SwingGUI currentGUI, String selectedFile, PdfDecoder decode_pdf) {
	}

	// <link><a name="saveform" />
	/** uses itext to save out form data with any changes user has made */
	public static void saveFormsData(String file) {
		throw new java.lang.AssertionError("Itext not on classpath");
	}

	public static void extractPagesToNewPDF(SavePDF current_selection) {
		throw new java.lang.AssertionError("Itext not on classpath");
	}

	public static void nup(int pageCount, PdfPageData currentPageData, ExtractPDFPagesNup extractPage) {
		throw new java.lang.AssertionError("Itext not on classpath");
	}

	public static void handouts(String file) {
		throw new java.lang.AssertionError("Itext not on classpath");
	}

	public static void add(int pageCount, PdfPageData currentPageData, InsertBlankPDFPage addPage) {
		throw new java.lang.AssertionError("Itext not on classpath");
	}

	// @SIGNING
	public static void Sign(SignData signData) {
		throw new java.lang.AssertionError("Itext not on classpath");
	}

	public static void rotate(int pageCount, PdfPageData currentPageData, RotatePDFPages current_selection) {
		throw new java.lang.AssertionError("Itext not on classpath");
	}

	public static void setCrop(int pageCount, PdfPageData currentPageData, CropPDFPages cropPage) {
		throw new java.lang.AssertionError("Itext not on classpath");
	}

	public static void delete(int pageCount, PdfPageData currentPageData, DeletePDFPages deletedPages) {
		throw new java.lang.AssertionError("Itext not on classpath");
	}

	public static void stampImage(int pageCount, PdfPageData currentPageData, final StampImageToPDFPages stampImage) {
		throw new java.lang.AssertionError("Itext not on classpath");
	}

	public static void stampText(int pageCount, PdfPageData currentPageData, final StampTextToPDFPages stampText) {
		throw new java.lang.AssertionError("Itext not on classpath");
	}

	public static void addHeaderFooter(int pageCount, PdfPageData currentPageData, final AddHeaderFooterToPDFPages addHeaderFooter) {
		throw new java.lang.AssertionError("Itext not on classpath");
	}

	public static void encrypt(int pageCount, PdfPageData currentPageData, EncryptPDFDocument encryptPage) {
		throw new java.lang.AssertionError("Itext not on classpath");
	}

	public static String getVersion() {
		throw new java.lang.AssertionError("Itext not on classpath");
	}
}
