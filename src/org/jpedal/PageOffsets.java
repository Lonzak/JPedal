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
 * PageOffsets.java
 * ---------------
 */
package org.jpedal;

import org.jpedal.objects.PdfPageData;

/**
 * holds offsets for all multiple pages
 */
public class PageOffsets {

	/** width of all pages */
	protected int totalSingleWidth = 0, totalDoubleWidth = 0, gaps = 0, doubleGaps = 0;

	/** height of all pages */
	protected int totalSingleHeight = 0, totalDoubleHeight = 0;

	protected int maxW = 0, maxH = 0;

	/** gap between pages */
	protected static final int pageGap = 10;

	/** max widths and heights for facing and continuous pages */
	protected int doublePageWidth = 0, doublePageHeight = 0, biggestWidth = 0, biggestHeight = 0, widestPageNR, widestPageR;

	protected boolean hasRotated;

	public PageOffsets(int pageCount, PdfPageData pageData) {

		/** calulate sizes for continuous and facing page modes */
		int pageH, pageW, rotation;
		int facingW = 0, facingH = 0;
		int greatestW = 0, greatestH = 0;
		this.totalSingleHeight = 0;
		this.totalSingleWidth = 0;
		this.hasRotated = false;

		int widestLeftPage = 0, widestRightPage = 0, highestLeftPage = 0, highestRightPage = 0;

		this.widestPageR = 0;
		this.widestPageNR = 0;

		this.totalDoubleWidth = 0;
		this.totalDoubleHeight = 0;
		this.gaps = 0;
		this.doubleGaps = 0;

		this.biggestWidth = 0;
		this.biggestHeight = 0;

		for (int i = 1; i < pageCount + 1; i++) {

			// get page sizes
			pageW = pageData.getCropBoxWidth(i);
			pageH = pageData.getCropBoxHeight(i);
			rotation = pageData.getRotation(i);

			// swap if this page rotated and flag
			if ((rotation == 90 || rotation == 270)) {
				int tmp = pageW;
				pageW = pageH;
				pageH = tmp;
			}

			if (pageW > this.maxW) this.maxW = pageW;

			if (pageH > this.maxH) this.maxH = pageH;

			this.gaps = this.gaps + pageGap;

			this.totalSingleWidth = this.totalSingleWidth + pageW;
			this.totalSingleHeight = this.totalSingleHeight + pageH;

			// track page sizes
			if ((i & 1) == 1) {// odd
				if (widestRightPage < pageW) widestRightPage = pageW;
				if (highestRightPage < pageH) highestRightPage = pageH;
			}
			else {
				if (widestLeftPage < pageW) widestLeftPage = pageW;
				if (highestLeftPage < pageH) highestLeftPage = pageH;
			}

			if (this.widestPageNR < pageW) this.widestPageNR = pageW;

			if (this.widestPageR < pageH) this.widestPageR = pageH;

			if (pageW > this.biggestWidth) this.biggestWidth = pageW;
			if (pageH > this.biggestHeight) this.biggestHeight = pageH;

			// track widest and highest combination of facing pages
			if ((i & 1) == 1) {

				if (greatestW < pageW) greatestW = pageW;
				if (greatestH < pageH) greatestH = pageH;

				if (i == 1) {// first page special case
					this.totalDoubleWidth = pageW;
					this.totalDoubleHeight = pageH;
				}
				else {
					this.totalDoubleWidth = this.totalDoubleWidth + greatestW;
					this.totalDoubleHeight = this.totalDoubleHeight + greatestH;
				}

				this.doubleGaps = this.doubleGaps + pageGap;

				facingW = pageW;
				facingH = pageH;

			}
			else {

				facingW = facingW + pageW;
				facingH = facingH + pageH;

				greatestW = pageW;
				greatestH = pageH;

				if (i == pageCount) { // allow for even number of pages
					this.totalDoubleWidth = this.totalDoubleWidth + greatestW + pageGap;
					this.totalDoubleHeight = this.totalDoubleHeight + greatestH + pageGap;
				}
			}

			// choose largest (to allow for rotation on specific pages)
			// int max=facingW;
			// if(max<facingH)
			// max=facingH;

		}

		this.doublePageWidth = widestLeftPage + widestRightPage + pageGap;
		this.doublePageHeight = highestLeftPage + highestRightPage + pageGap;

		// subtract pageGap to make sum correct
		this.totalSingleWidth = this.totalSingleWidth - pageGap;
		this.totalSingleHeight = this.totalSingleHeight - pageGap;
	}

	public int getMaxH() {
		return this.maxH;
	}

	public int getMaxW() {
		return this.maxW;
	}

	public int getWidestPageR() {
		return this.widestPageR;
	}

	public int getWidestPageNR() {
		return this.widestPageNR;
	}
}
