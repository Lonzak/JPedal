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
 * SwingPrinter.java
 * ---------------
 */
package org.jpedal.parser;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.util.HashMap;
import java.util.Map;

import javax.print.attribute.SetOfIntegerSyntax;

import org.jpedal.Overlays;
import org.jpedal.exception.PdfException;
import org.jpedal.external.ExternalHandlers;
import org.jpedal.io.ObjectStore;
import org.jpedal.io.PdfObjectReader;
import org.jpedal.objects.PdfPageData;
import org.jpedal.objects.PrinterOptions;
import org.jpedal.render.DynamicVectorRenderer;
import org.jpedal.utils.Messages;

public class SwingPrinter {

	/** content we print onto display */
	private Overlays overlays;

	private int duplexGapOdd = 0, duplexGapEven = 0;

	/** the ObjectStore for this file for printing */
	ObjectStore objectPrintStoreRef = null;

	ExternalHandlers externalHandlers;

	private boolean centerOnScaling = false;

	/**
	 * flag used to show if printing worked
	 */
	public boolean operationSuccessful = true;

	/** page scaling mode to use for printing */
	public int pageScalingMode = PrinterOptions.PAGE_SCALING_REDUCE_TO_PRINTER_MARGINS;

	/**
	 * Any printer errors
	 */
	public String pageErrorMessages = "";

	// print only the visible area of the doc.
	public boolean printOnlyVisible = false;

	public int logicalPageOffset = 0;

	/**
	 * used by Canoo for printing
	 */
	public DynamicVectorRenderer printRender = null;

	/**
	 * last page printed
	 */
	public int lastPrintedPage = -1;

	/**
	 * current print page or -1 if finished
	 */
	public int currentPrintPage = 0;

	/**
	 * used is bespoke version of JPedal - do not use
	 */
	public boolean isCustomPrinting = false;

	PdfObjectReader currentPdfFile;

	/**
	 * holds pageformats
	 */
	public Map pageFormats = new HashMap(100);

	// list of pages in range for quick lookup
	public int[] listOfPages;

	public boolean allowDifferentPrintPageSizes = false;

	/**
	 * actual page range to print
	 */
	public int start = 0;
	public int end = -1;

	public boolean oddPagesOnly = false;
	public boolean evenPagesOnly = false;

	public SetOfIntegerSyntax range;

	public boolean pagesPrintedInReverse = false;
	public boolean stopPrinting = false;

	/** type of printing */
	public boolean isPrintAutoRotateAndCenter = false;

	/** flag to show we use PDF page size */
	public boolean usePDFPaperSize = false;

	/**
	 * printing object
	 */
	public PdfStreamDecoderForPrinting currentPrintDecoder = null;

	public boolean legacyPrintMode = true;
	private float scaling;
	private int insetW, insetH;

	public int getNumberOfPages(int pageCount) {

		// handle 1,2,5-7,12
		if (this.range != null) {
			int rangeCount = 0;
			for (int ii = 1; ii < pageCount + 1; ii++) {
				if (this.range.contains(ii) && (!this.oddPagesOnly || (ii & 1) == 1) && (!this.evenPagesOnly || (ii & 1) == 0)) rangeCount++;
			}
			return rangeCount;
		}

		int count = 1;

		if (this.end != -1) {
			count = this.end - this.start + 1;
			if (count < 0) // allow for reverse order
			count = 2 - count;
		}

		if (this.oddPagesOnly || this.evenPagesOnly) {
			return (count + 1) / 2;
		}
		else {
			return count;
		}
	}

	public PageFormat getPageFormat(int p, PdfPageData pageData, int pageCount) throws IndexOutOfBoundsException {

		Object returnValue;
		int actualPage;

		// remap if in list
		if (this.listOfPages != null && p < this.listOfPages.length) {
			p = this.listOfPages[p];
		}

		if (this.end == -1) actualPage = p + 1;
		else
			if (this.end > this.start) actualPage = this.start + p;
			else actualPage = this.start - p;

		returnValue = this.pageFormats.get(actualPage);

		if (returnValue == null) returnValue = this.pageFormats.get("standard");

		PageFormat pf = new PageFormat();

		this.pageFormats.put("Align-" + actualPage, "normal");

		if (this.usePDFPaperSize) {

			int crw = pageData.getCropBoxWidth(actualPage);
			int crh = pageData.getCropBoxHeight(actualPage);
			int rotation = pageData.getRotation(actualPage);

			if (this.allowDifferentPrintPageSizes) { // keep old mode for Rog
				// swap round if needed
				if (rotation == 90 || rotation == 270) {
					int tmp = crw;
					crw = crh;
					crh = tmp;
				}

				if (crw > crh) {
					int tmp = crw;
					crw = crh;
					crh = tmp;

					if (rotation == 90) this.pageFormats.put("Align-" + actualPage, "inverted");
				}
			}

			createCustomPaper(pf, crw, crh, pageCount, pageData);

		}
		else
			if (returnValue != null) pf = (PageFormat) returnValue;

		if (!this.isPrintAutoRotateAndCenter) {
			pf.setOrientation(PageFormat.PORTRAIT);
		}

		return pf;
	}

	private void createCustomPaper(PageFormat pf, int clipW, int clipH, int pageCount, PdfPageData pageData) {

		Paper customPaper = new Paper();

		// System.out.println("createCustomPaper "+pf+" "+clipW+" "+clipH);
		// Do not change this code, if you have check if it doesn't break
		// the barcode file (ABACUS) !
		// The barcode is to be printed horizontally on the page!

		if (pageCount == 1 || this.allowDifferentPrintPageSizes) {
			customPaper.setSize(clipW, clipH);
			customPaper.setImageableArea(0, 0, clipW, clipH);
		}
		else {

			// Due to the way printing (different sized pages in one go) works in Java we
			// work out the biggest for the printed selection and apply it to all printed pages.
			int paperClipW = 0;
			int paperClipH = 0;

			for (int t = this.start; t <= this.end; t++) {
				if (clipW <= (pageData.getMediaBoxWidth(t) + 1) && clipH <= (pageData.getMediaBoxHeight(t) + 1)) {
					paperClipW = pageData.getMediaBoxWidth(t) + 1;
					paperClipH = pageData.getMediaBoxHeight(t) + 1;
				}
			}

			customPaper.setSize(paperClipW, paperClipH);
			customPaper.setImageableArea(0, 0, clipW, clipH);

		}

		pf.setPaper(customPaper);
	}

	public void setPagePrintRange(SetOfIntegerSyntax range, int pageCount) throws PdfException {

		this.range = range;
		this.start = range.next(0); // find first

		int rangeCount = 0;

		// get number of items
		for (int ii = 0; ii < pageCount; ii++) {
			if (range.contains(ii)) rangeCount++;
		}

		// setup array
		this.listOfPages = new int[rangeCount + 1];

		// find last
		int i = this.start;
		this.end = this.start;
		if (range.contains(2147483647)) // allow for all returning largest int
		this.end = pageCount;
		else {
			while (range.next(i) != -1)
				i++;
			this.end = i;
		}

		// if actually backwards, reverse order
		if (this.start > this.end) {
			int tmp = this.start;
			this.start = this.end;
			this.end = tmp;
		}

		// populate table
		int j = 0;

		for (int ii = this.start; ii < this.end + 1; ii++) {
			if (range.contains(ii) && (!this.oddPagesOnly || (ii & 1) == 1) && (!this.evenPagesOnly || (ii & 1) == 0)) {
				this.listOfPages[j] = ii - this.start;
				j++;
			}
		}

		if ((this.start < 1) || (this.end < 1) || (this.start > pageCount) || (this.end > pageCount)) throw new PdfException(
				Messages.getMessage("PdfViewerPrint.InvalidPageRange") + ' ' + this.start + ' ' + this.end);
	}

	public void setPrintPageMode(int mode) {

		this.oddPagesOnly = (mode & PrinterOptions.ODD_PAGES_ONLY) == PrinterOptions.ODD_PAGES_ONLY;
		this.evenPagesOnly = (mode & PrinterOptions.EVEN_PAGES_ONLY) == PrinterOptions.EVEN_PAGES_ONLY;

		this.pagesPrintedInReverse = (mode & PrinterOptions.PRINT_PAGES_REVERSED) == PrinterOptions.PRINT_PAGES_REVERSED;
	}

	public void putPageFormat(Object key, PageFormat value) {
		this.pageFormats.put(key, value);
	}

	/**
	 * return page currently being printed or -1 if finished
	 */
	public int getCurrentPrintPage() {
		return this.currentPrintPage;
	}

	/**
	 * ask JPedal to stop printing a page
	 */
	final public void stopPrinting() {
		this.stopPrinting = true;
	}

	public void setPagePrintRange(int start, int end, int pageCount) throws PdfException {

		this.start = start;
		this.end = end;

		// all returns huge number not page end range
		if (end == 2147483647) end = pageCount;

		// if actually backwards, reverse order
		if (start > end) {
			int tmp = start;
			start = end;
			end = tmp;
		}
		if ((start < 1) || (end < 1) || (start > pageCount) || (end > pageCount)) throw new PdfException(
				Messages.getMessage("PdfViewerPrint.InvalidPageRange") + ' ' + start + ' ' + end);
	}

	public void useLogicalPrintOffset(int pagesPrinted) {
		this.logicalPageOffset = pagesPrinted;
	}

	public boolean isPageSuccessful() {
		return this.operationSuccessful;
	}

	/**
	 * Return String with all error messages from last printed (useful for debugging)
	 */
	public String getPageFailureMessage() {
		return this.pageErrorMessages;
	}

	/**
	 * set page scaling mode to use - default setting is PAGE_SCALING_REDUCE_TO_PRINTER_MARGINS All values start PAGE_SCALING
	 */
	public void setPrintPageScalingMode(int pageScalingMode) {
		this.pageScalingMode = pageScalingMode;
	}

	/**
	 * turn list into Array
	 * 
	 * @param values
	 */
	private static Map toMap(int[] values) {

		if (values == null || values.length == 0) return null;

		int count = values.length;
		Map newList = new HashMap(count);

		for (int value : values)
			newList.put(value, "x");

		return newList;
	}

	private Rectangle workoutClipping(int displayRotation, Rectangle vr, int print_x_size, int print_y_size) {

		Rectangle cRect;

		double x = vr.getX();
		double y = vr.getY();
		double w = vr.getWidth();
		double h = vr.getHeight();

		/**
		 * g2.clipRect((int)((vr.x-insetW)/this.scaling),(int) ((print_y_size) - ((vr.y+vr.height-insetH)/this.scaling)) ,(int)
		 * (vr.width/this.scaling)-1,(int) (vr.height/this.scaling));
		 */

		int newX = 0;
		int newY = 0;
		int newW = 0;
		int newH = 0;

		if (true) {
			switch (displayRotation) {
				case (0):
					newX = (int) ((vr.x - this.insetW) / this.scaling);
					newY = (int) ((print_y_size) - ((vr.y + vr.height - this.insetH) / this.scaling));
					newW = (int) (vr.width / this.scaling - 1);
					newH = (int) (vr.height / this.scaling);
					break;

				case (90):
					newX = (int) (((y - this.insetH) / this.scaling));
					newY = (int) (((x - this.insetW) / this.scaling));
					newW = (int) (h / this.scaling);
					newH = (int) (w / this.scaling);
					break;

				case (180):
					newY = (int) ((y / this.scaling) - (this.insetH / this.scaling));
					newX = (int) (print_x_size - ((x + w - this.insetW) / this.scaling));
					newW = (int) (w / this.scaling);
					newH = (int) (h / this.scaling);
					break;

				case (270):
					newX = (int) ((print_x_size - (y + h - this.insetH) / this.scaling));
					newY = (int) ((print_y_size - (x + w - this.insetW) / this.scaling));
					newW = (int) (h / this.scaling);
					newH = (int) (w / this.scaling);
					break;
			}
		}
		else {
			switch (displayRotation) {
				case (0):
					newX = (int) ((vr.x - this.insetW) / this.scaling);
					newY = (int) ((print_x_size) - ((vr.y + vr.height - this.insetH) / this.scaling));
					newW = (int) (vr.width / this.scaling - 1);
					newH = (int) (vr.height / this.scaling);
					break;

				case (90):
					newX = (int) (((y - this.insetH) / this.scaling));
					newY = (int) (((x - this.insetW) / this.scaling));
					newW = (int) (h / this.scaling);
					newH = (int) (w / this.scaling);
					break;

				case (180):
					newY = (int) ((y / this.scaling) - (this.insetH / this.scaling));
					newX = (int) (print_y_size - ((x + w - this.insetW) / this.scaling));
					newW = (int) (w / this.scaling);
					newH = (int) (h / this.scaling);
					break;

				case (270):
					newX = (int) ((print_y_size - (y + h - this.insetH) / this.scaling));
					newY = (int) ((print_x_size - (x + w - this.insetW) / this.scaling));
					newW = (int) (h / this.scaling);
					newH = (int) (w / this.scaling);
					break;
			}
		}

		cRect = new Rectangle(newX, newY, newW, newH);

		return cRect;
	}

	/**
	 * store objects to use on a print
	 * 
	 * @param page
	 * @param type
	 * @param colors
	 * @param obj
	 * @throws PdfException
	 */
	public void printAdditionalObjectsOverPage(int page, int[] type, Color[] colors, Object[] obj) throws PdfException {

		if (this.overlays == null) this.overlays = new Overlays();

		this.overlays.printAdditionalObjectsOverPage(page, type, colors, obj);
	}

	/**
	 * store objects to use on a print
	 * 
	 * @param type
	 * @param colors
	 * @param obj
	 * @throws PdfException
	 */
	public void printAdditionalObjectsOverAllPages(int[] type, Color[] colors, Object[] obj) throws PdfException {

		if (this.overlays == null) this.overlays = new Overlays();

		this.overlays.printAdditionalObjectsOverAllPages(type, colors, obj);
	}

	public void clear() {

		if (this.overlays != null) this.overlays.clear();

		if (this.objectPrintStoreRef != null) {
			this.objectPrintStoreRef.flush();
		}
	}

	public void setPrintIndent(int oddPages, int evenPages) {
		this.duplexGapOdd = oddPages;
		this.duplexGapEven = evenPages;
	}

	public void setCenterOnScaling(boolean center) {

		this.centerOnScaling = center;
	}
}
