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
 * PdfPageFormat.java
 * ---------------
 */

package org.jpedal.utils;

/**
 * Factory methods for PdfPageFormats.  
 * 
 * <p>Encapsulates all PageFormat creation for Acrobat-like results.  Queries the printer
 * in order to find the most suitable PageFormat.</p>
 */

import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;

import org.jpedal.PdfDecoder;
import org.jpedal.objects.PdfPageData;

public class PdfPageFormat {
	private static Set<PageFormat> availablePaper;

	public PdfPageFormat() {
		throw new AssertionError("PdfPageFormat cannot be instanced.  Use factory methods.");
	}

	/**
	 * The purpose of this method is to create a <tt>PageFormat</tt> object that covers the correct printable area of the given device. Use this
	 * method to create paper in order to get Acrobat like results.
	 * 
	 * The size of the <tt>PageFormat</tt> is based on the given <tt>MediaSize</tt>. See
	 * http://download.oracle.com/javase/1.4.2/docs/api/javax/print/attribute/standard/MediaSize.html
	 * 
	 * @param mediaSizeName The desired paper size (ie <tt>MediaSize.ISO.A4</tt>)
	 * @param printingDevice The device to be printed to
	 * @return A formatted <tt>PageFormat</tt> for use with printing PDFs. May return null if there is no available size information.
	 */
	public static final PageFormat createPdfPageFormat(MediaSizeName mediaSizeName, PrintService printingDevice) {
		boolean testFlag = false;
		if (printingDevice == null) {
			printingDevice = PrintServiceLookup.lookupDefaultPrintService();
		}

		if (testFlag && printingDevice == null) System.out.println("Print Device is null");

		PageFormat pdfPageFormat = new PageFormat();

		MediaSize mediaSize = MediaSize.getMediaSizeForName(mediaSizeName);

		if (mediaSize == null) {
			mediaSize = MediaSize.getMediaSizeForName(PdfPageFormat.getDefaultMediaSizeName(printingDevice));
		}

		if (testFlag && mediaSize == null) System.out.println("Media Size is null");

		float xSize = mediaSize.getX(Size2DSyntax.INCH) * 72;
		float ySize = mediaSize.getY(Size2DSyntax.INCH) * 72;

		Paper pdfPaper = new Paper();
		pdfPaper.setSize(xSize, ySize);

		MediaPrintableArea area = (MediaPrintableArea) printingDevice.getDefaultAttributeValue(MediaPrintableArea.class);

		if (testFlag && area == null) System.out.println("Area is null");

		float[] imagable;

		// On my cheap tesco printer this return null, so accounted for that case.
		// if(area != null) {
		// imagable = area.getPrintableArea(Size2DSyntax.INCH);
		// }
		// else {
		PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
		attributes.add(mediaSizeName);
		MediaPrintableArea areas[] = (MediaPrintableArea[]) printingDevice.getSupportedAttributeValues(MediaPrintableArea.class, null, attributes);

		if (testFlag && areas == null) System.out.println("Printable Area array is null");

		int useArea = 0;
		if (areas[useArea] == null) for (int i = 0; i != areas.length && areas[useArea] == null; i++)
			useArea = i;

		imagable = areas[useArea].getPrintableArea(Size2DSyntax.INCH);
		// }

		for (int i = 0; i < 4; i++) {
			imagable[i] *= 72;
		}

		if (imagable[2] + imagable[0] > xSize) {
			imagable[2] = xSize - imagable[0];
		}

		if (imagable[3] + imagable[1] > ySize) {
			imagable[3] = ySize - imagable[1];
		}

		pdfPaper.setImageableArea(imagable[0], imagable[1], imagable[2], imagable[3]);

		pdfPageFormat.setPaper(pdfPaper);

		return pdfPageFormat;
	}

	/**
	 * Find the default size for the given printer. If the printer hasnt got a default it will figure it out based on the locale.
	 * 
	 * @param printingDevice
	 *            The <tt>PrintService</tt> to inspect
	 * @return The default page size suitable for printing PDF files too.
	 */
	public static PageFormat getDefaultPage(PrintService printingDevice) {
		if (printingDevice == null) {
			printingDevice = PrintServiceLookup.lookupDefaultPrintService();
		}

		return createPdfPageFormat(getDefaultMediaSizeName(printingDevice), printingDevice);
	}

	/**
	 * Find an the default MediaSizeName from the <tt>PrintService</tt>. If there isnt a decent default one available get one based on locale.
	 * 
	 * @param printingDevice
	 * @return A valid, default <tt>MediaSizeName</tt>
	 */
	public static MediaSizeName getDefaultMediaSizeName(PrintService printingDevice) {
		MediaSizeName size = (MediaSizeName) printingDevice.getDefaultAttributeValue(MediaSizeName.class);

		if (size == null || MediaSize.getMediaSizeForName(size) == null) {
			Locale locale = Locale.getDefault();

			if (locale.equals(Locale.UK)) {
				size = MediaSizeName.ISO_A4;
			}
			else
				if (locale.equals(Locale.US)) {
					size = MediaSizeName.NA_LETTER;
				}
				else {
					size = MediaSizeName.ISO_A4;
				}
		}

		return size;
	}

	/**
	 * Used to create a set of available <tt>PageFormat</tt> based on what is available on the given device.
	 * 
	 * @param printingDevice
	 *            The <tt>PrintService</tt> to examine
	 */
	public static void findPrinterPapers(PrintService printingDevice) {
		availablePaper = new HashSet<PageFormat>();

		if (printingDevice == null) {
			printingDevice = PrintServiceLookup.lookupDefaultPrintService();
		}

		Media[] res = (Media[]) printingDevice.getSupportedAttributeValues(javax.print.attribute.standard.Media.class,
				DocFlavor.SERVICE_FORMATTED.PAGEABLE, null);
		for (Media m : res) {
			if (m instanceof javax.print.attribute.standard.MediaSizeName) {
				MediaSize size = MediaSize.getMediaSizeForName((MediaSizeName) m);
				if (size != null) { // The sun.print.CustomMediaSizeName returns null for some reason?
					availablePaper.add(createPdfPageFormat((MediaSizeName) m, printingDevice));
				}
			}
		}
	}

	/**
	 * Find the best fitting <tt>PageFormat</tt< for a given page.
	 * 
	 * @param p
	 *            The page to examine by PDF page number.
	 * @param pdfDecoder
	 *            The <tt>PdfDecoder</tt> encapsulating the PdfPage
	 * @return The nearest available <tt>PageFormat</tt> for this <tt>PrintService</tt>
	 */
	public static PageFormat getPageFormat(int p, PdfDecoder pdfDecoder) {
		PdfPageData pageData = pdfDecoder.getPdfPageData();

		float cropW = pageData.getCropBoxWidth(p);
		float cropH = pageData.getCropBoxHeight(p);

		PageFormat result = PdfPageFormat.getAppropriatePageFormat(cropW, cropH);

		return result;
	}

	/**
	 * Examine the Set of <tt>PageFormat</tt>s to find the best fit.
	 * 
	 * @param cropW
	 *            The desired paper width
	 * @param cropH
	 *            The desired paper height
	 */
	private static PageFormat getAppropriatePageFormat(float cropW, float cropH) {
		if (availablePaper == null) {
			findPrinterPapers(null);
		}

		// Start off with null
		PageFormat result = null;

		for (PageFormat pf : availablePaper) {
			result = getClosestPageFormat(result, pf, cropW, cropH);
		}

		if (result == null) { // return the default PageFormat if null. Shouldnt really occur.
			result = PdfPageFormat.getDefaultPage(null);
		}

		return result;
	}

	/**
	 * Compare two <tt>PageFormat</tt> objects to find which is a better fit.
	 * 
	 * @param original
	 *            The current best fit <tt>PageFormat</tt> may be null.
	 * @param newFormat
	 *            The <tt>PageFormat</tt> to compare against
	 * @param cropW
	 *            The width of the PDF page
	 * @param cropH
	 *            The height of the PDF page
	 * @return The best fitting <tt>PageFormat</tt> either the original or a clone of the new best fitting one.
	 */
	private static PageFormat getClosestPageFormat(PageFormat original, PageFormat newFormat, float cropW, float cropH) {
		double newLongSide = newFormat.getHeight() > newFormat.getWidth() ? newFormat.getHeight() : newFormat.getWidth();
		double newShortSide = newFormat.getHeight() < newFormat.getWidth() ? newFormat.getHeight() : newFormat.getWidth();

		double cropLongSide = cropH > cropW ? cropH : cropW;
		double cropShortSide = cropH < cropW ? cropH : cropW;

		if (newShortSide >= cropShortSide && newLongSide >= cropLongSide) {

			if (original == null) { // No others so far so just have this one to start off with
				PageFormat result = (PageFormat) newFormat.clone();
				return result;
			}

			double origLongSide = original.getHeight() > original.getWidth() ? original.getHeight() : original.getWidth();
			double origShortSide = original.getHeight() < original.getWidth() ? original.getHeight() : original.getWidth();

			if ((origLongSide + origShortSide) > (newShortSide + newLongSide)) {
				PageFormat result = (PageFormat) newFormat.clone();
				return result;
			}
		}

		return original;
	}

}
