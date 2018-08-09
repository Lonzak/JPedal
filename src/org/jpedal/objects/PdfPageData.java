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
 * PdfPageData.java
 * ---------------
 */
package org.jpedal.objects;

import java.io.Serializable;

import org.jpedal.utils.repositories.Vector_Int;
import org.jpedal.utils.repositories.Vector_Object;

/**
 * store data relating to page sizes set in PDF (MediaBox, CropBox, rotation)
 */
public class PdfPageData implements Serializable {

	private static final long serialVersionUID = -8258244553460848813L;

	private boolean valuesSet = false;

	private int lastPage = -1;

	private int pagesRead = -1;

	private int pageCount = 1; // number of pages

	private float[] defaultMediaBox = null;

	/** any rotation on page (defined in degress) */
	private int rotation = 0;

	/** max media string for page */
	private Vector_Object mediaBoxes = new Vector_Object(500);
	private Vector_Object cropBoxes = new Vector_Object(500);

	private Vector_Int rotations = null;

	/** current x and y read from page info */
	private float cropBoxX = -99999, cropBoxY = -1, cropBoxW = -1, cropBoxH = -1;

	/** current x and y read from page info */
	private float mediaBoxX = -1, mediaBoxY, mediaBoxW, mediaBoxH;

	/** whether the document has varying page sizes and rotation */
	private boolean hasMultipleSizes = false, hasMultipleSizesSet = false;

	/** string representation of crop box */
	private float scalingValue = 1f;

	private float[] mediaBox, cropBox;

	/** string representation of media box */
	private int defaultrotation;
	private float defaultcropBoxX, defaultcropBoxY, defaultcropBoxW, defaultcropBoxH;
	private float defaultmediaBoxX, defaultmediaBoxY, defaultmediaBoxW, defaultmediaBoxH;

	public PdfPageData() {}

	/**
	 * make sure a value set for crop and media box (used internally to trap 'odd' settings and insure setup correctly)
	 */
	public void checkSizeSet(int pageNumber) {

		// use default
		if (this.mediaBox == null) this.mediaBox = this.defaultMediaBox;

		// value we keep
		if (this.cropBox != null
				&& (this.cropBox[0] != this.mediaBox[0] || this.cropBox[1] != this.mediaBox[1] || this.cropBox[2] != this.mediaBox[2] || this.cropBox[3] != this.mediaBox[3])) {

			this.mediaBoxes.setElementAt(this.mediaBox, pageNumber);

			if (this.cropBox[0] >= this.mediaBox[0] && this.cropBox[1] >= this.mediaBox[1]
					&& (this.cropBox[2] - this.cropBox[0]) <= (this.mediaBox[2] - this.mediaBox[0])
					&& (this.cropBox[3] - this.cropBox[1]) <= (this.mediaBox[3] - this.mediaBox[1])) this.cropBoxes.setElementAt(this.cropBox,
					pageNumber);

		}
		else
			if (this.mediaBox != null
					&& (this.defaultMediaBox[0] != this.mediaBox[0] || this.defaultMediaBox[1] != this.mediaBox[1]
							|| this.defaultMediaBox[2] != this.mediaBox[2] || this.defaultMediaBox[3] != this.mediaBox[3])) // if matches default
																															// don't save
			this.mediaBoxes.setElementAt(this.mediaBox, pageNumber);

		// track which pages actually read
		if (this.pagesRead < pageNumber) this.pagesRead = pageNumber;

		this.lastPage = -1;
		this.mediaBox = null;
		this.cropBox = null;
	}

	/**
	 * return height of mediaBox
	 */
	final public int getMediaBoxHeight(int pageNumber) {

		// check values correctly set
		setSizeForPage(pageNumber);

		return (int) this.mediaBoxH;
	}

	/**
	 * return mediaBox y value
	 */
	final public int getMediaBoxY(int pageNumber) {

		// check values correctly set
		setSizeForPage(pageNumber);

		return (int) this.mediaBoxY;
	}

	/**
	 * return mediaBox x value
	 */
	final public int getMediaBoxX(int pageNumber) {

		// check values correctly set
		setSizeForPage(pageNumber);

		return (int) this.mediaBoxX;
	}

	/**
	 * set string with raw values and assign values to crop and media size
	 */
	public void setMediaBox(float[] mediaBox) {

		this.mediaBox = mediaBox;
		this.cropBox = null;

		if (this.defaultMediaBox == null) this.defaultMediaBox = mediaBox;
	}

	/**
	 * set crop with values and align with media box
	 */
	public void setCropBox(float[] cropBox) {

		this.cropBox = cropBox;

		// If mediaBox is set and crop box leaves this area
		// we should limit the cropBox by the mediaBox
		boolean testAlteredCrop = true;
		if (testAlteredCrop && (this.mediaBox != null && !(this.mediaBox.length < 4))) {
			if (cropBox[0] < this.mediaBox[0]) cropBox[0] = this.mediaBox[0];

			if (cropBox[1] < this.mediaBox[1]) cropBox[1] = this.mediaBox[1];

			if (cropBox[2] > this.mediaBox[2]) cropBox[2] = this.mediaBox[2];

			if (cropBox[3] > this.mediaBox[3]) cropBox[3] = this.mediaBox[3];
		}
	}

	public int setPageRotation(int value, int pageNumber) {

		int raw_rotation = value;

		// convert negative
		if (raw_rotation < 0) raw_rotation = 360 + raw_rotation;

		// only create if we need and set value
		if (raw_rotation != 0 || this.rotations != null) {
			if (this.rotations == null) {
				if (pageNumber < 2000) this.rotations = new Vector_Int(2000);
				else this.rotations = new Vector_Int(pageNumber * 2);
			}

			this.rotations.setElementAt(raw_rotation, pageNumber);

		}
		return raw_rotation;
	}

	/**
	 * return width of media box
	 */
	final public int getMediaBoxWidth(int pageNumber) {

		// check values correctly set
		setSizeForPage(pageNumber);

		return (int) this.mediaBoxW;
	}

	/**
	 * return mediaBox string found in PDF file
	 */
	public String getMediaValue(int currentPage) {

		StringBuilder returnValue = new StringBuilder();

		float[] mediaBox = this.defaultMediaBox;

		if (this.mediaBoxes != null) mediaBox = (float[]) this.mediaBoxes.elementAt(currentPage);

		if (mediaBox != null) {

			for (int j = 0; j < 4; j++) {
				returnValue.append(mediaBox[j]);
				returnValue.append(' ');
			}
		}

		return returnValue.toString();
	}

	/**
	 * return cropBox string found in PDF file
	 */
	public String getCropValue(int currentPage) {

		float[] cropBox = null;

		// use default
		if (this.cropBoxes != null) cropBox = (float[]) this.cropBoxes.elementAt(currentPage);
		else
			if (cropBox != null) cropBox = (float[]) this.mediaBoxes.elementAt(currentPage);

		if (cropBox == null) cropBox = this.defaultMediaBox;

		StringBuilder returnValue = new StringBuilder();

		for (int j = 0; j < 4; j++) {
			returnValue.append(cropBox[j]);
			returnValue.append(' ');
		}

		return returnValue.toString();
	}

	/**
	 * return Scaled x value for cropBox
	 */
	public int getScaledCropBoxX(int pageNumber) {

		// check values correctly set
		setSizeForPage(pageNumber);

		return roundFloat(this.cropBoxX * this.scalingValue);
	}

	/**
	 * return Scaled cropBox width
	 */
	public int getScaledCropBoxWidth(int pageNumber) {

		// check values correctly set
		setSizeForPage(pageNumber);

		return roundFloat(this.cropBoxW * this.scalingValue);
	}

	/**
	 * return Scaled y value for cropox
	 */
	public int getScaledCropBoxY(int pageNumber) {

		// check values correctly set
		setSizeForPage(pageNumber);

		return roundFloat(this.cropBoxY * this.scalingValue);
	}

	/**
	 * return Scaled cropBox height
	 */
	public int getScaledCropBoxHeight(int pageNumber) {

		// check values correctly set
		setSizeForPage(pageNumber);

		return roundFloat(this.cropBoxH * this.scalingValue);
	}

	/**
	 * return x value for cropBox
	 */
	public int getCropBoxX(int pageNumber) {

		// check values correctly set
		setSizeForPage(pageNumber);

		return (int) this.cropBoxX;
	}

	/**
	 * return x value for cropBox
	 */
	public float getCropBoxX2D(int pageNumber) {

		// check values correctly set
		setSizeForPage(pageNumber);

		return this.cropBoxX;
	}

	/**
	 * return cropBox width
	 */
	public int getCropBoxWidth(int pageNumber) {

		// check values correctly set
		setSizeForPage(pageNumber);

		return (int) this.cropBoxW;
	}

	/**
	 * return cropBox width
	 */
	public float getCropBoxWidth2D(int pageNumber) {

		// check values correctly set
		setSizeForPage(pageNumber);

		return this.cropBoxW;
	}

	/**
	 * return y value for cropox
	 */
	public int getCropBoxY(int pageNumber) {

		// check values correctly set
		setSizeForPage(pageNumber);

		return (int) this.cropBoxY;
	}

	/**
	 * return y value for cropox
	 */
	public float getCropBoxY2D(int pageNumber) {

		// check values correctly set
		setSizeForPage(pageNumber);

		return this.cropBoxY;
	}

	/**
	 * return cropBox height
	 */
	public int getCropBoxHeight(int pageNumber) {

		// check values correctly set
		setSizeForPage(pageNumber);

		return (int) this.cropBoxH;
	}

	/**
	 * return cropBox height
	 */
	public float getCropBoxHeight2D(int pageNumber) {

		// check values correctly set
		setSizeForPage(pageNumber);

		return this.cropBoxH;
	}

	/** see if current figures generated for this page and setup if not */
	private synchronized void setSizeForPage(int pageNumber) {

		if (pageNumber == this.lastPage) return;

		if (pageNumber > this.pageCount) this.pageCount = pageNumber;

		/** calculate values if first call for this page */
		if (pageNumber > this.pagesRead) {

			// set values if no value
			this.mediaBoxX = 0;
			this.mediaBoxY = 0;
			this.mediaBoxW = 0;
			this.mediaBoxH = 0;

			// set values if no value
			this.cropBoxX = 0;
			this.cropBoxY = 0;
			this.cropBoxW = 0;
			this.cropBoxH = 0;

			this.lastPage = pageNumber;

		}
		else
			if (pageNumber > 0 && this.lastPage != pageNumber) {

				this.lastPage = pageNumber;

				boolean usingDefault = false;

				float[] cropBox = (float[]) this.cropBoxes.elementAt(pageNumber);
				float[] mediaBox = (float[]) this.mediaBoxes.elementAt(pageNumber);
				if (mediaBox == null && this.defaultMediaBox != null) {
					mediaBox = this.defaultMediaBox;
					usingDefault = true;
				}

				// set rotation
				if (this.rotations != null) this.rotation = this.rotations.elementAt(pageNumber);
				else this.rotation = this.defaultrotation;

				while (this.rotation >= 360)
					this.rotation = this.rotation - 360;

				if (this.valuesSet && usingDefault) {

					this.cropBoxX = this.defaultcropBoxX;
					this.mediaBoxX = this.defaultmediaBoxX;
					this.cropBoxY = this.defaultcropBoxY;
					this.mediaBoxY = this.defaultmediaBoxY;
					this.cropBoxW = this.defaultcropBoxW;
					this.mediaBoxW = this.defaultmediaBoxW;
					this.cropBoxH = this.defaultcropBoxH;
					this.mediaBoxH = this.defaultmediaBoxH;

				}
				else {

					/**
					 * set mediaBox, cropBox and default if none
					 */

					// set values if no value
					this.mediaBoxX = 0;
					this.mediaBoxY = 0;
					this.mediaBoxW = 800;
					this.mediaBoxH = 800;

					if (mediaBox != null) {
						this.mediaBoxX = mediaBox[0];
						this.mediaBoxY = mediaBox[1];
						this.mediaBoxW = mediaBox[2] - this.mediaBoxX;
						this.mediaBoxH = mediaBox[3] - this.mediaBoxY;

						if (this.mediaBoxY > 0 && this.mediaBoxH == -this.mediaBoxY) {
							this.mediaBoxH = -this.mediaBoxH;
							this.mediaBoxY = 0;
						}
					}

					/**
					 * set crop
					 */
					if (cropBox != null) {

						this.cropBoxX = cropBox[0];
						this.cropBoxY = cropBox[1];
						this.cropBoxW = cropBox[2];
						this.cropBoxH = cropBox[3];

						if (this.cropBoxX > this.cropBoxW) {
							float temp = this.cropBoxX;
							this.cropBoxX = this.cropBoxW;
							this.cropBoxW = temp;
						}
						if (this.cropBoxY > this.cropBoxH) {
							float temp = this.cropBoxY;
							this.cropBoxY = this.cropBoxH;
							this.cropBoxH = temp;
						}

						this.cropBoxW = this.cropBoxW - this.cropBoxX;
						this.cropBoxH = this.cropBoxH - this.cropBoxY;

						if (this.cropBoxY > 0 && this.cropBoxH == -this.cropBoxY) {
							this.cropBoxH = -this.cropBoxH;
							this.cropBoxY = 0;
						}

					}
					else {
						this.cropBoxX = this.mediaBoxX;
						this.cropBoxY = this.mediaBoxY;
						this.cropBoxW = this.mediaBoxW;
						this.cropBoxH = this.mediaBoxH;
					}
				}

				// fix for odd file with negative height
				if (this.cropBoxH < 0) {
					this.cropBoxY = this.cropBoxY + this.cropBoxH;
					this.cropBoxH = -this.cropBoxH;
				}
				if (this.cropBoxW < 0) {
					this.cropBoxX = this.cropBoxX + this.cropBoxW;
					this.cropBoxW = -this.cropBoxW;
				}

				if (usingDefault && !this.valuesSet) {

					this.defaultrotation = this.rotation;
					this.defaultcropBoxX = this.cropBoxX;
					this.defaultmediaBoxX = this.mediaBoxX;
					this.defaultcropBoxY = this.cropBoxY;
					this.defaultmediaBoxY = this.mediaBoxY;
					this.defaultcropBoxW = this.cropBoxW;
					this.defaultmediaBoxW = this.mediaBoxW;
					this.defaultcropBoxH = this.cropBoxH;
					this.defaultmediaBoxH = this.mediaBoxH;

					this.valuesSet = true;
				}
			}
	}

	/**
	 * Get the scaling value currently being used
	 */
	public float getScalingValue() {
		return this.scalingValue;
	}

	/**
	 * Scaling value to apply to all values
	 */
	public void setScalingValue(float scalingValue) {
		this.scalingValue = scalingValue;
	}

	private static int roundFloat(float origValue) {
		int roundedValue = (int) origValue;

		boolean useCustomRounding = true;
		if (useCustomRounding) {
			float frac = origValue - roundedValue;
			if (frac > 0.3) roundedValue = roundedValue + 1;
		}
		return roundedValue;
	}

	/**
	 * get page count
	 */
	final public int getPageCount() {
		return this.pageCount;
	}

	/** return rotation value (for outside class) */
	final public int getRotation(int pageNumber) {

		// check values correctly set
		setSizeForPage(pageNumber);

		return this.rotation;
	}

	/**
	 * return Scaled height of mediaBox
	 */
	final public int getScaledMediaBoxHeight(int pageNumber) {

		// check values correctly set
		setSizeForPage(pageNumber);

		return roundFloat(this.mediaBoxH * this.scalingValue);
	}

	/**
	 * return Scaled width of media box
	 */
	final public int getScaledMediaBoxWidth(int pageNumber) {

		// check values correctly set
		setSizeForPage(pageNumber);

		return roundFloat(this.mediaBoxW * this.scalingValue);
	}

	/**
	 * return Scaled mediaBox x value
	 */
	final public int getScaledMediaBoxX(int pageNumber) {

		// check values correctly set
		setSizeForPage(pageNumber);

		return roundFloat(this.mediaBoxX * this.scalingValue);
	}

	/**
	 * return Scaled mediaBox y value
	 */
	final public int getScaledMediaBoxY(int pageNumber) {

		// check values correctly set
		setSizeForPage(pageNumber);

		return roundFloat(this.mediaBoxY * this.scalingValue);
	}

	public boolean hasMultipleSizes() {
		// return if already calculated
		if (this.hasMultipleSizesSet) {
			return this.hasMultipleSizes;
		}

		// scan all pages and if we find one different, disable page turn
		int pageCount = this.pageCount;
		int pageW = getCropBoxWidth(1);
		int pageH = getCropBoxHeight(1);
		int pageR = getRotation(1);

		if (pageCount > 1) {
			for (int jj = 2; jj < pageCount + 1; jj++) {

				if (pageW != getCropBoxWidth(jj) || pageH != getCropBoxHeight(jj) || pageR != getRotation(jj)) {
					jj = pageCount;
					this.hasMultipleSizes = true;
				}
			}
		}
		this.hasMultipleSizesSet = true;
		return this.hasMultipleSizes;
	}

}
