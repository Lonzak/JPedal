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
 * SingleDisplay.java
 * ---------------
 */
package org.jpedal;

//<start-adobe>
//<start-thin>
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import javax.swing.border.Border;

import org.jpedal.examples.viewer.gui.generic.GUIThumbnailPanel;
import org.jpedal.external.Options;
import org.jpedal.external.RenderChangeListener;
import org.jpedal.objects.PdfPageData;
import org.jpedal.render.DynamicVectorRenderer;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.repositories.Vector_Int;
//<end-thin>
//<end-adobe>

public class SingleDisplay implements Display {

	// flag to track if page decoded twice
	// private int lastPageDecoded = -1;

	/**
	 * Flag if we should allow cursor to change
	 */
	public static boolean allowChangeCursor = true;

	// Animation enabled (currently just turnover in facing)
	public static boolean default_turnoverOn = true;// can be altered by user
	public boolean turnoverOn = default_turnoverOn;

	// Display the first page separately in Facing mode
	public static boolean default_separateCover = true;// can be altered by user
	public boolean separateCover = default_separateCover;

	/** Holds the x,y,w,h of the current highlighted image, null if none */
	private int[] highlightedImage = null;

	BufferedImage previewImage = null;

	String previewText;

	// used to flag we can decode multiple pages
	boolean isGeneratingOtherPages = false;

	// facing mode drag pages
	BufferedImage[] facingDragCachedImages = new BufferedImage[4];
	BufferedImage facingDragTempLeftImg, facingDragTempRightImg;
	int facingDragTempLeftNo, facingDragTempRightNo;

	protected Map keyToFilename = new HashMap();

	Rectangle userAnnot = null;

	AffineTransform rawAf;
	Shape rawClip;

	DynamicVectorRenderer currentDisplay;

	// flag shows if running
	boolean running = false;

	protected PageOffsets currentOffset;

	/** Normally null - user object to listen to paint events */
	RenderChangeListener customRenderChangeListener;

	/** used for creating the image size of volatileImage */
	protected int volatileWidth, volatileHeight;

	int startViewPage = 0;
	int endViewPage = 0;

	Map pagesDrawn = new HashMap();

	Map cachedPageViews = new WeakHashMap();

	Map currentPageViews = new HashMap();

	boolean screenNeedsRedrawing;

	PdfDecoder pdf;

	protected Vector_Int offsets = new Vector_Int(3);
	protected Map newPages = new HashMap();
	protected int currentXOffset = 0, additionalPageCount = 0;

	/**
	 * any scaling factor being used to convert co-ords into correct values and to alter image size
	 */
	float oldScaling = -1, oldRotation = -1, oldVolatileWidth = -1, oldVolatileHeight = -1;

	/** centering on page */
	int indent = 0;

	/** used to draw pages offset if not in SINGLE_PAGE mode */
	int[] xReached, yReached, pageW, pageH;

	/** Keep a record of cumalative offsets for SINGLE_PAGE mode */
	protected int[] pageOffsetH, pageOffsetW;

	boolean[] isRotated;

	/** optimise page redraws */
	Map accleratedPagesAlreadyDrawn = new HashMap();

	/** flag to switch back to unaccelerate screen if no enough memory for scaling */
	boolean overRideAcceleration = false;

	// to remind me to add feature back
	private boolean message = false;

	/** local copies */
	int displayRotation, displayView = SINGLE_PAGE;
	int lastDisplayRotation = 0;

	int insetW, insetH;

	float scaling;
	float lastScaling;

	int pageNumber;
	int pageCount = 0;

	PdfPageData pageData = null;

	/** flag to optimse calculations */
	// private int lastPageChecked=-1,lastState=-1;

	Graphics2D g2;

	Map areas;

	// stop multiple repaints
	private int lastAreasPainted = -1;

	/** render screen using hardware acceleration */
	boolean useAcceleration = true;

	boolean isInitialised;

	// rectangle onscreen
	int rx = 0, ry = 0, rw = 0, rh = 0;

	/** used to draw demo cross */
	private int crx, cry, crw, crh;

	AffineTransform current2 = null;
	Shape currentClip = null;

	protected Border myBorder;

	public SingleDisplay(int pageNumber, int pageCount, DynamicVectorRenderer currentDisplay) {

		if (pageNumber < 1) pageNumber = 1;

		this.pageNumber = pageNumber;
		this.pageCount = pageCount;
		this.currentDisplay = currentDisplay;
	}

	public SingleDisplay(PdfDecoder pdf) {
		this.pdf = pdf;
	}

	public static int CURRENT_BORDER_STYLE = 1;

	public static void setBorderStyle(int style) {
		CURRENT_BORDER_STYLE = style;
	}

	public static int getBorderStyle() {
		return CURRENT_BORDER_STYLE;
	}

	/**
	 * general method to pass in Objects - only takes RenderChangeListener at present
	 * 
	 * @param type
	 * @param newHandler
	 */
	@Override
	public void setObjectValue(int type, Object newHandler) {

		// set value
		switch (type) {
			case Options.RenderChangeListener:
				this.customRenderChangeListener = (RenderChangeListener) newHandler;
				break;

			default:
				throw new RuntimeException("setObjectValue does not take value " + type);
		}
	}

	/**
	 * used by Storypad to display split spreads not aprt of API
	 */
	@Override
	public void clearAdditionalPages() {

		this.offsets.clear();
		this.newPages.clear();
		this.currentXOffset = 0;
		this.additionalPageCount = 0;
	}

	/**
	 * used by Storypad to display split spreads not aprt of API
	 */
	@Override
	public void addAdditionalPage(DynamicVectorRenderer dynamicRenderer, int pageWidth, int origPageWidth) {

		// store
		this.offsets.addElement(this.currentXOffset + origPageWidth);
		this.newPages.put(this.currentXOffset + origPageWidth, dynamicRenderer);
		this.additionalPageCount++;

		// work out new offset
		this.currentXOffset = this.currentXOffset + pageWidth;

		// force redraw
		this.oldScaling = -this.oldScaling;
		this.refreshDisplay();
	}

	VolatileImage backBuffer = null;

	@Override
	public void dispose() {

		if (this.backBuffer != null) this.backBuffer.flush();

		this.keyToFilename.clear();

		this.currentOffset = null;
		this.areas = null;
		this.pageH = null;
		this.pageW = null;
		this.cachedPageViews = null;
		this.isRotated = null;

		this.backBuffer = null;

		this.accleratedPagesAlreadyDrawn = null;
	}

	protected void createBackBuffer() {

		final boolean debugBackBuffer = false;

		if (debugLayout) System.out.println("createBackBuffer");

		if (debugBackBuffer) System.out.println("Flushing back buffer");

		if (this.backBuffer != null) {
			this.backBuffer.flush();
			this.backBuffer = null;
		}

		int width = 0, height = 0;

		if (debugBackBuffer) System.out.println("Setting height and width");

		if (this.displayView == SINGLE_PAGE) {
			if ((this.displayRotation == 90) | (this.displayRotation == 270)) {
				width = (this.volatileHeight + this.currentXOffset);
				height = this.volatileWidth;
			}
			else {
				width = this.volatileWidth + this.currentXOffset;
				height = this.volatileHeight;
			}
		}
		else
			if (this.currentOffset != null) {

				if (debugBackBuffer) System.out.println("Also setting offset");

				// height for facing pages
				int biggestFacingHeight = 0;

				if ((this.displayView == FACING) && (this.pageW != null)) {
					// get 2 facing page numbers
					int p1, p2;
					if (this.separateCover) {
						p1 = this.pageNumber;
						if ((p1 & 1) == 1) p1--;
						p2 = p1 + 1;
					}
					else {
						p1 = this.pageNumber;
						if ((p1 & 1) == 0) p1--;
						p2 = p1 + 1;
					}

					/*
					 * if((displayRotation==90)|(displayRotation==270)){ biggestFacingHeight=pageH[p1]; if(p2<pageH.length &&
					 * biggestFacingHeight<pageH[p2]) biggestFacingHeight=pageH[p2]; }else{ biggestFacingHeight=pageH[p1]; if(p2<pageH.length &&
					 * biggestFacingHeight<pageH[p2]) biggestFacingHeight=pageH[p2]; }
					 */

					biggestFacingHeight = this.pageH[p1];
					if (p2 < this.pageH.length && biggestFacingHeight < this.pageH[p2]) biggestFacingHeight = this.pageH[p2];
				}

				int gaps = this.currentOffset.gaps;
				int doubleGaps = this.currentOffset.doubleGaps;

				switch (this.displayView) {

					case FACING:

						// Get widths of pages
						int firstW,
						secondW;
						if ((this.displayRotation + this.pdf.getPdfPageData().getRotation(this.pageNumber)) % 180 == 90) firstW = this.pdf
								.getPdfPageData().getCropBoxHeight(this.pageNumber);
						else firstW = this.pdf.getPdfPageData().getCropBoxWidth(this.pageNumber);

						if (this.pageNumber + 1 > this.pageCount || (this.pageNumber == 1 && this.pageCount != 2)) {
							secondW = firstW;
						}
						else {
							if ((this.displayRotation + this.pdf.getPdfPageData().getRotation(this.pageNumber + 1)) % 180 == 90) secondW = this.pdf
									.getPdfPageData().getCropBoxHeight(this.pageNumber + 1);
							else secondW = this.pdf.getPdfPageData().getCropBoxWidth(this.pageNumber + 1);
						}

						// get total width
						int totalW = firstW + secondW;

						width = (int) (totalW * this.scaling) + PageOffsets.pageGap;
						height = (biggestFacingHeight); // NOTE scaled already!
						break;

					case CONTINUOUS:

						if ((this.displayRotation == 90) | (this.displayRotation == 270)) {
							width = (int) (this.currentOffset.biggestHeight * this.scaling);
							height = (int) ((this.currentOffset.totalSingleWidth) * this.scaling) + gaps + this.insetH;
						}
						else {
							width = (int) (this.currentOffset.biggestWidth * this.scaling);
							height = (int) ((this.currentOffset.totalSingleHeight) * this.scaling) + gaps + this.insetH;
						}
						break;

					case CONTINUOUS_FACING:

						if ((this.displayRotation == 90) | (this.displayRotation == 270)) {
							width = (int) ((this.currentOffset.doublePageHeight) * this.scaling) + (this.insetW * 2) + doubleGaps;
							height = (int) ((this.currentOffset.totalDoubleWidth) * this.scaling) + doubleGaps + this.insetH;
						}
						else {
							width = (int) ((this.currentOffset.doublePageWidth) * this.scaling) + (this.insetW * 2);
							height = (int) ((this.currentOffset.totalDoubleHeight) * this.scaling) + doubleGaps + this.insetH;
						}
						break;

				}
			}

		try {
			if (debugBackBuffer) System.out.println("Handle Huge pages if any");
			// avoid huge pages as VERY slow
			if (height > 15000) {
				this.volatileHeight = 0;
				height = 0;
				this.overRideAcceleration = true;
			}
			if (debugBackBuffer) System.out.println("Create back buffer");
			if ((width > 0) && (height > 0)) {
				this.backBuffer = this.pdf.createVolatileImage(width, height);
				this.oldVolatileWidth = this.volatileWidth;
				this.oldVolatileHeight = this.volatileHeight;

				Graphics2D gg = (Graphics2D) this.backBuffer.getGraphics();
				gg.setPaint(this.pdf.getBackground());
				gg.fillRect(0, 0, width, height);
			}

		}
		catch (Error e) { // switch off if not enough memory
			if (debugBackBuffer) System.out.println("Acceleration issue found " + e.getMessage());
			this.overRideAcceleration = true;
			this.backBuffer = null;
		}
		catch (Exception e) {
			if (debugBackBuffer) System.out.println("Issue found in display mode");
			e.printStackTrace();
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " is display mode");
		}

		if (debugBackBuffer) System.out.println("Painting optimised");
		// don't redraw
		this.currentDisplay.setOptimsePainting(true);
	}

	@Override
	public boolean isAccelerated() {

		if (!this.useAcceleration || this.overRideAcceleration) {
			return false;
		}
		else return true;
	}

	@Override
	public void resetCachedValues() {

		// rest page views
		// lastPageChecked = -1;
		// lastState = -1;
	}

	@Override
	public void stopGeneratingPage() {

		// request any processes die
		this.isGeneratingOtherPages = false;

		// wait to die
		while (this.running) {
			// System.out.println("Waiting to die");
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException e) {
				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
			}
		}
	}

	@Override
	public void disableScreen() {
		this.isInitialised = false;

		this.oldScaling = -1;
	}

	boolean testAcceleratedRendering() {

		boolean canDrawAccelerated = false;

		// force redraw if page rescaled
		if ((this.oldScaling != this.scaling) || (this.oldRotation != this.displayRotation) || (this.oldVolatileWidth != this.volatileWidth)
				|| (this.oldVolatileHeight != this.volatileHeight)) {
			this.backBuffer = null;
			this.overRideAcceleration = false;

		}

		if (DynamicVectorRenderer.debugPaint) System.err.println("acceleration called " + this.backBuffer);

		if (!this.overRideAcceleration && (this.backBuffer == null)) {// ||(screenNeedsRedrawing)){

			createBackBuffer();
			this.accleratedPagesAlreadyDrawn.clear();

		}

		if (this.backBuffer != null) {
			do {
				// First, we validate the back buffer
				int valCode = VolatileImage.IMAGE_INCOMPATIBLE;
				if (this.backBuffer != null) valCode = this.backBuffer.validate(this.pdf.getGraphicsConfiguration());

				if (valCode == VolatileImage.IMAGE_RESTORED) {
					// This case is just here for illustration
					// purposes. Since we are
					// recreating the contents of the back buffer
					// every time through this loop, we actually
					// do not need to do anything here to recreate
					// the contents. If our VImage was an image that
					// we were going to be copying _from_, then we
					// would need to restore the contents at this point
				}
				else
					if (valCode == VolatileImage.IMAGE_INCOMPATIBLE) {
						if (!this.overRideAcceleration) createBackBuffer();

					}

				// Now we've handled validation, get on with the rendering
				if ((this.backBuffer != null)) {

					canDrawAccelerated = true;

				}

				// Now we are done; or are we? Check contentsLost() and loop as necessary
			}
			while ((this.backBuffer == null) || (this.backBuffer.contentsLost()));
		}
		return canDrawAccelerated;
	}

	@Override
	public Dimension getPageSize(int displayView) {

		Dimension pageSize = null;

		// height for facing pages
		int biggestFacingHeight = 0;
		int facingWidth = 0;

		if ((displayView == FACING) && (this.pageW != null)) {
			// get 2 facing page numbers
			int p1, p2;
			if (this.separateCover) {
				p1 = this.pageNumber;
				if ((p1 & 1) == 1) p1--;
				p2 = p1 + 1;
			}
			else {
				p1 = this.pageNumber;
				if ((p1 & 1) == 0) p1--;
				p2 = p1 + 1;
			}

			if (p1 == 0) {
				biggestFacingHeight = this.pageH[p2];
				facingWidth = this.pageW[p2] * 2;
			}
			else {
				biggestFacingHeight = this.pageH[p1];
				if (p2 < this.pageH.length) {
					if (biggestFacingHeight < this.pageH[p2]) biggestFacingHeight = this.pageH[p2];

					facingWidth = this.pageW[p1] + this.pageW[p2];
				}
				else {
					facingWidth = this.pageW[p1] * 2;
				}
			}
		}

		int gaps = this.currentOffset.gaps;
		int doubleGaps = this.currentOffset.doubleGaps;

		switch (displayView) {

			case FACING:

				pageSize = new Dimension((facingWidth + this.insetW + this.insetW), (biggestFacingHeight + this.insetH + this.insetH));

				break;

			case CONTINUOUS:

				if ((this.displayRotation == 90) | (this.displayRotation == 270)) pageSize = new Dimension(
						(int) ((this.currentOffset.biggestHeight * this.scaling) + this.insetW + this.insetW),
						(int) ((this.currentOffset.totalSingleWidth * this.scaling) + gaps + this.insetH + this.insetH));
				else pageSize = new Dimension((int) ((this.currentOffset.biggestWidth * this.scaling) + this.insetW + this.insetW),
						(int) ((this.currentOffset.totalSingleHeight * this.scaling) + gaps + this.insetH + this.insetH));

				break;

			case CONTINUOUS_FACING:

				if ((this.displayRotation == 90) | (this.displayRotation == 270)) {
					if (this.pageCount == 2) pageSize = new Dimension(
							(int) ((this.currentOffset.doublePageHeight * this.scaling) + this.insetW + this.insetW),
							(int) ((this.currentOffset.biggestWidth * this.scaling) + gaps + this.insetH + this.insetH));
					else pageSize = new Dimension((int) ((this.currentOffset.doublePageHeight * this.scaling) + this.insetW + this.insetW),
							(int) ((this.currentOffset.totalDoubleWidth * this.scaling) + doubleGaps + this.insetH + this.insetH));
				}
				else {
					if (this.pageCount == 2) pageSize = new Dimension(
							(int) ((this.currentOffset.doublePageWidth * this.scaling) + this.insetW + this.insetW),
							(int) ((this.currentOffset.biggestHeight * this.scaling) + gaps + this.insetH + this.insetH));
					else pageSize = new Dimension((int) ((this.currentOffset.doublePageWidth * this.scaling) + this.insetW + this.insetW),
							(int) ((this.currentOffset.totalDoubleHeight * this.scaling) + doubleGaps + this.insetH + this.insetH));
				}
				break;

		}

		if (debugLayout) System.out.println("pageSize" + pageSize);

		return pageSize;
	}

	public Rectangle getDisplayedRectangle() {

		Rectangle userAnnot = this.pdf.getVisibleRect();

		// get raw rectangle
		/**
		 * if((this.displayRotation==90)||(this.displayRotation==270)){ ry =userAnnot.x; rx =userAnnot.y ; rh =userAnnot.width; rw =userAnnot.height;
		 * }else{
		 */
		this.rx = userAnnot.x;
		this.ry = userAnnot.y;
		this.rw = userAnnot.width;
		this.rh = userAnnot.height;

		// Best way I found to catch if pdf decoder is being used but never displayed
		if (this.pdf.isShowing() && userAnnot.width == 0 || userAnnot.height == 0) {
			this.rx = 0;
			this.ry = 0;
			this.rw = this.pageData.getScaledCropBoxWidth(this.pageNumber);
			this.rh = this.pageData.getScaledCropBoxHeight(this.pageNumber);

			if (this.pageData.getRotation(this.pageNumber) % 180 != 0) {
				this.rh = this.pageData.getScaledCropBoxWidth(this.pageNumber);
				this.rw = this.pageData.getScaledCropBoxHeight(this.pageNumber);
			}
		}

		// System.out.println("R's -> (x,y) " + rx + " " + ry);
		return userAnnot;
	}

	@Override
	public void drawBorder() {

		if (CURRENT_BORDER_STYLE == BORDER_SHOW) {

			if ((this.crw > 0) && (this.crh > 0) && (this.myBorder != null)) this.myBorder.paintBorder(this.pdf, this.g2, this.crx - 1, this.cry - 1,
					this.crw + 1, this.crh + 1);
		}
	}

	void setDisplacementOnG2(Graphics2D gBB) { // multi pages assumes all page 0 and rotates for each
		// translate the graphic to 0,0 on volatileImage java co-ords
		float cX = this.crx / this.scaling, cY = this.cry / this.scaling;
		if (this.displayRotation == 0 || this.displayView != Display.SINGLE_PAGE) gBB.translate(-cX, cY);
		else
			if (this.displayRotation == 90) gBB.translate(-cY, -cX);
			else
				if (this.displayRotation == 180) gBB.translate(cX, -cY);
				else
					if (this.displayRotation == 270) gBB.translate(cY, cX);
	}

	@Override
	public void refreshDisplay() {

		this.screenNeedsRedrawing = true;
		// reset over-ride which may have been enabled

		this.accleratedPagesAlreadyDrawn.clear();

		this.overRideAcceleration = false;
	}

	@Override
	public void flushPageCaches() {
		this.currentPageViews.clear();
		this.cachedPageViews.clear();
	}

	@Override
	public void init(float scaling, int pageCount, int displayRotation, int pageNumber, DynamicVectorRenderer currentDisplay, boolean isInit,
			PdfPageData pageData, int insetW, int insetH) {

		// if(debugLayout)
		// System.out.println("init");

		if (pageNumber < 1) pageNumber = 1;

		this.currentDisplay = currentDisplay;
		this.scaling = scaling;
		this.pageCount = pageCount;
		this.displayRotation = displayRotation;
		this.pageNumber = pageNumber;
		this.pageData = pageData;

		this.insetW = insetW;
		this.insetH = insetH;

		currentDisplay.setInset(insetW, insetH);

		// reset over-ride which may have been enabled
		pageData.setScalingValue(scaling); // ensure aligned
		this.volatileWidth = pageData.getScaledCropBoxWidth(this.pageNumber);
		this.volatileHeight = pageData.getScaledCropBoxHeight(this.pageNumber);

		if (isInit) {
			// lastPageChecked=-1;
			setPageOffsets(this.pageNumber);
			this.isInitialised = true;
		}

		// clear facing drag cache & set not drawing
		if (this.displayView == FACING && (isInit || (this.lastDisplayRotation != displayRotation) || (this.lastScaling != scaling))) {
			for (int i = 0; i < 4; i++) {
				this.facingDragCachedImages[i] = null;
			}

			this.pdf.setUserOffsets(0, 0, org.jpedal.external.OffsetOptions.INTERNAL_DRAG_BLANK);
		}

		this.lastScaling = scaling;
	}

	/**
	 * workout offsets so we can draw pages
	 * */
	@Override
	public void setPageOffsets(int pageNumber) {

		if (debugLayout) System.out.println("setPageOffsets " + pageNumber + ' ' + this.pageCount + " displayView=" + this.displayView + " scaling="
				+ this.scaling);

		if (this.displayView == SINGLE_PAGE) {
			this.xReached = null;
			this.yReached = null;

			// <start-adobe>
			/** pass in values for forms/annots */
			if (this.pdf.getFormRenderer() != null) this.pdf.getFormRenderer().getCompData().setPageDisplacements(this.xReached, this.yReached);

			// <end-adobe>
			return;
		}

		// lastPageChecked=pageNumber;
		// lastState=displayView;

		this.xReached = new int[this.pageCount + 1];
		this.yReached = new int[this.pageCount + 1];
		this.pageW = new int[this.pageCount + 1];
		this.pageH = new int[this.pageCount + 1];
		this.pageOffsetW = new int[this.pageCount + 1];
		this.pageOffsetH = new int[this.pageCount + 1];

		int heightCorrection;
		int displayRotation;

		this.isRotated = new boolean[this.pageCount + 1];
		int gap = PageOffsets.pageGap;// set.pageGap*scaling);

		if (this.turnoverOn && this.pageCount != 2 && !this.pdf.getPdfPageData().hasMultipleSizes() && this.displayView == Display.FACING) gap = 0;

		// Used to help allign first page is page 2 is cropped / rotated
		int LmaxWidth = 0;
		int LmaxHeight = 0;
		int RmaxWidth = 0;
		int RmaxHeight = 0;

		/** work out page sizes - need to do it first as we can look ahead */
		for (int i = 1; i < this.pageCount + 1; i++) {

			/**
			 * get unrotated page sizes
			 */
			this.pageW[i] = this.pageData.getScaledCropBoxWidth(i);
			this.pageH[i] = this.pageData.getScaledCropBoxHeight(i);

			displayRotation = this.pageData.getRotation(i) + this.displayRotation;
			if (displayRotation >= 360) displayRotation = displayRotation - 360;

			// swap if this page rotated and flag
			if ((displayRotation == 90 || displayRotation == 270)) {
				int tmp = this.pageW[i];
				this.pageW[i] = this.pageH[i];
				this.pageH[i] = tmp;

				this.isRotated[i] = true; // flag page as rotated
			}

			if ((i & 1) == 1) {
				if (this.pageW[i] > RmaxWidth) RmaxWidth = this.pageW[i];
				if (this.pageH[i] > RmaxHeight) RmaxHeight = this.pageH[i];
			}
			else {
				if (this.pageW[i] > LmaxWidth) LmaxWidth = this.pageW[i];
				if (this.pageH[i] > LmaxHeight) LmaxHeight = this.pageH[i];
			}
		}

		// loop through all pages and work out positions
		for (int i = 1; i < this.pageCount + 1; i++) {
			heightCorrection = 0;
			if (((this.pageCount == 2) && (this.displayView == FACING || this.displayView == CONTINUOUS_FACING))
					|| (this.displayView == FACING && !this.separateCover)) { // special case
				// if only 2 pages display side by side
				if ((i & 1) == 1) {
					this.xReached[i] = 0;
					this.yReached[i] = 0;
				}
				else {
					this.xReached[i] = this.xReached[i - 1] + this.pageW[i - 1] + gap;
					this.yReached[i] = 0;
					if (!(i == 2 && this.pageData.getRotation(1) == 270)) {
						this.pageOffsetW[2] = (this.pageW[2] - this.pageW[1]) + this.pageOffsetW[1];
						this.pageOffsetH[2] = (this.pageH[2] - this.pageH[1]) + this.pageOffsetH[1];
					}
				}

			}
			else
				if (i == 1) { // first page is special case
					// First page should be on the left so indent
					if (this.displayView == CONTINUOUS) {
						this.xReached[1] = 0;
						this.yReached[1] = 0;
						this.pageOffsetW[1] = 0;
						this.pageOffsetH[1] = 0;
						this.pageOffsetW[0] = gap; // put the gap values in the empty entry in the offset array. A bit bodgy!
						this.pageOffsetH[0] = gap;

					}
					else
						if (this.displayView == CONTINUOUS_FACING) {
							this.pageOffsetW[0] = gap; // put the gap values in the empty entry in the offset array. A bit bodgy!
							this.pageOffsetH[0] = gap;
							this.pageOffsetW[1] = 0;
							this.pageOffsetH[1] = 0;
							this.xReached[1] = LmaxWidth + gap;
							this.yReached[1] = 0;
						}
						else
							if (this.displayView == FACING) {
								this.xReached[1] = this.pageW[1] + gap;
								this.yReached[1] = 0;
							}

				}
				else {
					// Calculate position for all other pages / cases
					if ((this.displayView == CONTINUOUS_FACING)) {

						if (!(i >= 2 && (((this.pageData.getRotation(i) == 270 || this.pageData.getRotation(i) == 90) && (this.pageData
								.getRotation(i - 1) != 270 || this.pageData.getRotation(i - 1) != 90)) || ((this.pageData.getRotation(i - 1) == 270 || this.pageData
								.getRotation(i - 1) == 90) && (this.pageData.getRotation(i) != 270 || this.pageData.getRotation(i) != 90))))) {
							this.pageOffsetW[i] = (this.pageW[i] - this.pageW[i - 1]) + this.pageOffsetW[i - 1];
							this.pageOffsetH[i] = (this.pageH[i] - this.pageH[i - 1]) + this.pageOffsetH[i - 1];
						}

						// Left Pages
						if ((i & 1) == 0) {
							// Last Page rotated so correct height
							if (i < this.pageCount) heightCorrection = (this.pageH[i + 1] - this.pageH[i]) / 2;
							if (heightCorrection < 0) heightCorrection = 0;// -heightCorrection;
							if (i > 3) {
								int temp = (this.pageH[i - 2] - this.pageH[i - 1]) / 2;
								if (temp > 0) heightCorrection = heightCorrection + temp;
							}
							this.yReached[i] = (this.yReached[i - 1] + this.pageH[i - 1] + gap) + heightCorrection;
						}
						else { // Right Pages
								// Last Page rotated so correct height
							heightCorrection = (this.pageH[i - 1] - this.pageH[i]) / 2;
							this.yReached[i] = (this.yReached[i - 1]) + heightCorrection;
						}

						if ((i & 1) == 0) {// Indent Left pages by diff between maxWidth and pageW (will only indent unrotated)
							this.xReached[i] = this.xReached[i] + (LmaxWidth - this.pageW[i]);
						}
						else {// Place Right Pages with a gap (This keeps pages centered)
							this.xReached[i] = this.xReached[i - 1] + this.pageW[i - 1] + gap;
						}

					}
					else
						if (this.displayView == CONTINUOUS) {
							// Place page below last with gap
							this.yReached[i] = (this.yReached[i - 1] + this.pageH[i - 1] + gap);

							if (!(i >= 2 && (((this.pageData.getRotation(i) == 270 || this.pageData.getRotation(i) == 90) && (this.pageData
									.getRotation(i - 1) != 270 || this.pageData.getRotation(i - 1) != 90)) || ((this.pageData.getRotation(i - 1) == 270 || this.pageData
									.getRotation(i - 1) == 90) && (this.pageData.getRotation(i) != 270 || this.pageData.getRotation(i) != 90))))) {
								this.pageOffsetW[i] = (this.pageW[i] - this.pageW[i - 1]) + this.pageOffsetW[i - 1];
								this.pageOffsetH[i] = (this.pageH[i] - this.pageH[i - 1]) + this.pageOffsetH[i - 1];
							}

						}
						else
							if ((this.displayView == FACING)) {
								if ((i & 1) == 1) { // If right page, place on right with gap
									this.xReached[i] = (this.xReached[i - 1] + this.pageW[i - 1] + gap);
									if (this.pageH[i] < this.pageH[i - 1]) // Drop page down to keep pages centred
									this.yReached[i] = this.yReached[i] + (((this.pageH[i - 1] - this.pageH[i]) / 2));
								}
								else { // If left page, indent by diff of max and current page
									this.xReached[i] = 0;
									if (i < this.pageCount) if (this.pageH[i] < this.pageH[i + 1]) // Drop page down to keep pages centered
									this.yReached[i] = this.yReached[i] + ((this.pageH[i + 1] - this.pageH[i]) / 2);
								}
							}
				}
		}

		// <start-adobe>
		if (this.pdf.getFormRenderer() != null) this.pdf.getFormRenderer().getCompData().setPageDisplacements(this.xReached, this.yReached);
		// <end-adobe>
	}

	@Override
	public void decodeOtherPages(int pageNumber, int pageCount) {}

	@Override
	public void completeForm(Graphics2D g2) {
		g2.drawLine(this.crx, this.cry, this.crx + this.crw, this.cry + this.crh);
		g2.drawLine(this.crx, this.crh + this.cry, this.crw + this.crx, this.cry);
	}

	@Override
	public void resetToDefaultClip() {

		if (this.current2 != null) this.g2.setTransform(this.current2);

		// reset transform and clip
		if (this.currentClip != null) this.g2.setClip(this.currentClip);
	}

	@Override
	public void initRenderer(Map areas, Graphics2D g2, Border myBorder, int indent) {

		// if(debugLayout)
		// System.out.println("initRenderer");

		this.rawAf = g2.getTransform();
		this.rawClip = g2.getClip();

		if (areas != null) this.lastAreasPainted = -2;
		this.areas = areas;
		this.g2 = g2;

		this.myBorder = myBorder;

		this.indent = indent;
		this.pagesDrawn.clear();

		setPageSize(this.pageNumber, this.scaling);
	}

	void setPageSize(int pageNumber, float scaling) {

		/**
		 * handle clip - crop box values
		 */
		this.pageData.setScalingValue(scaling); // ensure aligned
		this.topW = this.pageData.getScaledCropBoxWidth(pageNumber);
		this.topH = this.pageData.getScaledCropBoxHeight(pageNumber);
		double mediaH = this.pageData.getScaledMediaBoxHeight(pageNumber);

		this.cropX = this.pageData.getScaledCropBoxX(pageNumber);
		this.cropY = this.pageData.getScaledCropBoxY(pageNumber);
		this.cropW = this.topW;
		this.cropH = this.topH;

		/**
		 * actual clip values - for flipped page
		 */
		if (this.displayView == Display.SINGLE_PAGE) {
			this.crx = (int) (this.insetW + this.cropX);
			this.cry = (int) (this.insetH - this.cropY);
		}
		else {
			this.crx = this.insetW;
			this.cry = this.insetH;
		}
		// cry =(int)(insetH+(mediaH-cropH)-cropY);

		// amount needed to move cropped page into correct position
		// int offsetX=(int) (mediaW-cropW);
		int offsetY = (int) (mediaH - this.cropH);

		if ((this.displayRotation == 90 || (this.displayRotation == 270))) {
			this.crw = (int) (this.cropH);
			this.crh = (int) (this.cropW);

			int tmp = this.crx;
			this.crx = this.cry;
			this.cry = tmp;

			this.crx = this.crx + offsetY;
		}
		else {
			this.crw = (int) (this.cropW);
			this.crh = (int) (this.cropH);

			this.cry = this.cry + offsetY;
		}
		/**/
		this.g2.translate(this.insetW - this.crx, this.insetH - this.cry);

		// save any transform and stroke
		this.current2 = this.g2.getTransform();

		// save global clip and set to our values
		this.currentClip = this.g2.getClip();

		// if(!showCrop)
		this.g2.clip(new Rectangle(this.crx, this.cry, (int) (this.crw + ((this.insetW * this.additionalPageCount) + this.currentXOffset * scaling)),
				this.crh));

		// System.out.println("Set pagesize "+pageNumber+" scaling="+scaling+" dimensions="+crx+" "+cry+" "+crw+" "+crh+" insetW="+insetW+" currentXOffset="+currentXOffset);
	}

	int topW, topH;
	double cropX, cropY, cropW, cropH;

	// <start-adobe><start-thin>
	protected GUIThumbnailPanel thumbnails = null;

	// <end-thin> <end-adobe>

	Rectangle calcVisibleArea(int topY, int topX) {

		Rectangle userAnnot;
		/** get area to draw */
		int x = 0, y = 0, h, w, gap;

		// update coords
		getDisplayedRectangle();

		if ((this.displayRotation != 270) && (this.displayRotation != 180) && (this.rx > this.insetW)) x = (int) ((this.rx - this.insetW) / this.scaling);

		// set defaults
		w = (int) ((this.rw + this.insetW) / this.scaling);
		h = topY;

		if (this.displayRotation == 0 || this.displayView != SINGLE_PAGE) {

			x = 0;
			// No need to stop drawing until view edge actualy cuts off part of page
			if (!((this.rx) < (this.insetW))) x = (int) (this.rx / this.scaling) - (int) (this.insetW / this.scaling);

			h = (int) ((this.rh + this.insetH) / this.scaling);
			w = (int) ((this.rw - this.insetW) / this.scaling) + (int) (this.insetW / this.scaling);

			// Following code does not allow for drawing for
			// the first insetW on the left hand side
			// if((x)<(insetW/scaling))
			// x=(int)(insetW/scaling);

			y = (int) (topY - ((this.ry + this.rh) / this.scaling));

		}
		else
			if (this.displayRotation == 90) {

				y = (int) ((this.rx - this.insetW) / this.scaling);
				h = (int) ((this.rw + this.insetW) / this.scaling);

				if (this.ry > this.insetW) {
					x = (int) ((this.ry - this.insetW) / this.scaling);
				}
				else {
					x = 0;
				}
				w = (int) ((this.rh) / this.scaling);

			}
			else
				if (this.displayRotation == 270) {

					w = (int) ((this.rh + this.insetW) / this.scaling);
					x = topX - ((int) ((this.ry + this.rh) / this.scaling));
					h = (int) ((this.rw + this.insetH) / this.scaling);
					y = topY - ((int) ((this.rx + this.rw) / this.scaling));

					if (x < this.insetH) {
						x = 0;
						w = w + this.insetH;
					}

					if (y < this.insetW) {
						y = 0;
						h = h + this.insetW;
					}

				}
				else
					if (this.displayRotation == 180) {

						h = (int) ((this.rh + this.insetH) / this.scaling) + this.insetH;
						y = ((int) ((this.ry - this.insetH) / this.scaling));
						w = (int) ((this.rw + this.insetW) / this.scaling);
						x = topX - ((int) ((this.rx - this.insetW) / this.scaling));
						x = x - w;

						// Check x or y is not negative
						if (x < 0 || x < this.insetH) {
							x = 0;
							w = w + this.insetH;
						}
						if (y < this.insetW) {
							y = 0;
							h = h + this.insetW;
						}

					}

		// do not use on hardware acceleration
		if (isAccelerated() || (this.scaling >= 2)) {
			int dx1 = this.pageData.getScaledCropBoxX(this.pageNumber);
			int dy1 = this.pageData.getScaledCropBoxY(this.pageNumber);

			// System.err.println("dx1 and dy1 == " + dx1 + " " + dy1);

			gap = 2;

			if (dx1 != 0 || dy1 != 0) {
				userAnnot = new Rectangle(x + (int) (dx1 / this.scaling), y + (int) (dy1 / this.scaling), w, h);
			}
			else {
				userAnnot = new Rectangle(x, y, w + (gap + gap), h + (gap + gap));
			}

		}
		else userAnnot = null;

		if ((this.displayView != Display.SINGLE_PAGE) && (this.message)) {
			this.message = true;
			System.out.println("SingleDisplay fast scrolling");
			userAnnot = null;
		}

		if (debugLayout) System.out.println("userAnnot=" + userAnnot + " scaling=" + this.scaling);

		return userAnnot;
	}

	@Override
	public Rectangle drawPage(AffineTransform viewScaling, AffineTransform displayScaling, int pageUsedForTransform) {

		Rectangle actualBox = null;

		/** add in other elements */
		if ((displayScaling != null) && (this.currentDisplay != null)) {

			// reset on scaling dropping back below 200 so retested below
			if (this.scaling < 2 && this.oldScaling > 2) this.useAcceleration = true;

			// redraw highlights
			if (this.useAcceleration || this.areas != null) {

				int areasPainted = -1;
				if (this.areas != null) areasPainted = this.areas.size();

				if (this.lastAreasPainted == -2 && areasPainted == -1) {}
				else
					if (areasPainted != this.lastAreasPainted) {
						this.screenNeedsRedrawing = true;
						this.lastAreasPainted = areasPainted;
					}
			}

			boolean canDrawAccelerated = false;
			// use hardware acceleration - it sucks on large image so
			// we check scaling as well...
			if ((this.useAcceleration) && (!this.overRideAcceleration) && (this.scaling < 2)) canDrawAccelerated = testAcceleratedRendering();
			else this.useAcceleration = false;

			/**
			 * pass in values as needed for patterns
			 */
			this.currentDisplay.setScalingValues(this.cropX, this.cropH + this.cropY, this.scaling);

			this.g2.transform(displayScaling);

			if (DynamicVectorRenderer.debugPaint) System.err.println("accelerate or redraw");

			if (canDrawAccelerated) {
				// rendering to the back buffer:
				Graphics2D gBB = (Graphics2D) this.backBuffer.getGraphics();

				if (this.screenNeedsRedrawing) {

					// fill background white to prevent memory overRun from previous graphics memory
					this.currentDisplay.setG2(gBB);
					this.currentDisplay.paintBackground(new Rectangle(0, 0, this.backBuffer.getWidth(), this.backBuffer.getHeight()));

					this.currentDisplay.setOptimsePainting(true);

					gBB.setTransform(displayScaling);
					setDisplacementOnG2(gBB);

					this.currentDisplay.setG2(gBB);
					if (this.areas != null) actualBox = this.currentDisplay.paint(((Rectangle[]) this.areas.get(this.pageNumber)), viewScaling,
							this.userAnnot);
					else actualBox = this.currentDisplay.paint(null, viewScaling, this.userAnnot);

					// drawSpreadPages(gBB,viewScaling,userAnnot,false);

					this.screenNeedsRedrawing = false;
				}

				gBB.dispose();

				if (this.backBuffer != null) {

					// draw the buffer
					AffineTransform affBefore = this.g2.getTransform();

					this.g2.setTransform(this.rawAf);
					this.g2.drawImage(this.backBuffer, this.insetW, this.insetH, this.pdf);
					this.g2.setTransform(affBefore);

					/**
					 * draw other page outlines and any decoded pages so visible
					 */

					// 20091124 I don't think is needed and it is faster without

					// actualBox =currentDisplay.paint(g2,areas,viewScaling,userAnnot,true,true);

					// drawSpreadPages(g2,viewScaling,userAnnot,true);
				}
			}
			else {
				if (DynamicVectorRenderer.debugPaint) System.err.println("standard paint called ");

				/**
				 * draw other page outlines and any decoded pages so visible
				 */
				// same rectangle works for any rotation so removed the rotation check
				// System.out.println("a");

				// The page clip already seems to have been taken into account, and occasionally this causes issues at high
				// scaling values, so I've commented it out for now.
				// g2.clip(new Rectangle((int)(cropX/scaling),(int)(cropY/scaling),(int)(topW/scaling),(int)(topH/scaling)));
				this.currentDisplay.setOptimsePainting(false);

				this.currentDisplay.setG2(this.g2);
				if (this.areas != null) actualBox = this.currentDisplay.paint(((Rectangle[]) this.areas.get(this.pageNumber)), viewScaling,
						this.userAnnot);
				else actualBox = this.currentDisplay.paint(null, viewScaling, this.userAnnot);

				// drawSpreadPages(g2,viewScaling,userAnnot,false);
			}

			// track scaling so we can update dependent values
			this.oldScaling = this.scaling;
			this.oldRotation = this.displayRotation;

		}

		return actualBox;
	}

	/**
	 * used internally by multiple pages scaling -1 to ignore, -2 to force reset
	 */
	@Override
	public int getXCordForPage(int page, float scaling) {

		if (scaling == -2 || (scaling != -1f && scaling != this.oldScaling)) {
			this.oldScaling = scaling;
			setPageOffsets(page);
		}
		return getXCordForPage(page);
	}

	/**
	 * used internally by multiple pages scaling -1 to ignore, -2 to force reset
	 */
	@Override
	public int getYCordForPage(int page, float scaling) {

		if (scaling == -2 || (scaling != -1f && scaling != this.oldScaling)) {
			this.oldScaling = scaling;
			setPageOffsets(page);
		}
		return getYCordForPage(page);
	}

	@Override
	public void setup(boolean useAcceleration, PageOffsets currentOffset, PdfDecoder pdf) {

		this.useAcceleration = useAcceleration;
		this.currentOffset = currentOffset;
		this.pdf = pdf;

		this.overRideAcceleration = false;
	}

	@Override
	public int getXCordForPage(int page) {
		if (this.xReached != null) return this.xReached[page] + this.insetW;
		else return this.insetW;
	}

	@Override
	public int getYCordForPage(int page) {
		if (this.yReached != null) return this.yReached[page] + this.insetH;
		else return this.insetH;
	}

	@Override
	public int getStartPage() {
		return this.startViewPage;
	}

	@Override
	public int getEndPage() {
		return this.endViewPage;
	}

	// <start-adobe>
	// <start-thin>
	@Override
	public void setThumbnailPanel(GUIThumbnailPanel thumbnails) {
		this.thumbnails = thumbnails;
	}

	// <end-thin>
	// <end-adobe>

	@Override
	public void setScaling(float scaling) {
		this.scaling = scaling;
		if (this.pageData != null) this.pageData.setScalingValue(scaling);
	}

	@Override
	public void setAcceleration(boolean enable) {
		this.useAcceleration = enable;
	}

	@Override
	public int getWidthForPage(int pageNumber) {
		return this.pageW[pageNumber];
	}

	/**
	 * internal method used by Viewer to provide preview of PDF in Viewer
	 */
	public void setPreviewThumbnail(BufferedImage previewImage, String previewText) {

		this.previewImage = previewImage;
		this.previewText = previewText;
	}

	/**
	 * put a little thumbnail of page on display for user in viewer as he scrolls through
	 * 
	 * @param g2
	 * @param visibleRect
	 */
	@Override
	public void drawPreviewImage(Graphics2D g2, Rectangle visibleRect) {

		if (this.previewImage != null) {
			int iw = this.previewImage.getWidth();
			int ih = this.previewImage.getHeight();

			int textWidth = g2.getFontMetrics().stringWidth(this.previewText);

			int width = iw > textWidth ? iw : textWidth;

			int x = visibleRect.x + visibleRect.width - 40 - width;
			int y = (visibleRect.y + visibleRect.height - 20 - ih) / 2;

			Composite original = g2.getComposite();
			g2.setPaint(Color.BLACK);
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
			g2.fill(new RoundRectangle2D.Double(x - 10, y - 10, width + 20, ih + 35, 10, 10));
			g2.setComposite(original);

			g2.setPaint(Color.WHITE);
			x += (width - iw) / 2;
			g2.drawImage(this.previewImage, x, y, null);

			int xOffset = (iw + 20 - textWidth) / 2;

			g2.drawString(this.previewText, x + xOffset - 10, y + ih + 15);
		}
	}

	@Override
	public void setHighlightedImage(int[] highlightedImage) {
		this.highlightedImage = highlightedImage;
	}

	@Override
	public boolean getBoolean(BoolValue option) {
		switch (option) {
			case SEPARATE_COVER:
				return this.separateCover;
			case TURNOVER_ON:
				return this.turnoverOn;
			default:
				return false;
		}
	}

	@Override
	public void setBoolean(BoolValue option, boolean value) {
		switch (option) {
			case SEPARATE_COVER:
				this.separateCover = value;
				return;
			case TURNOVER_ON:
				this.turnoverOn = value;
				return;
			default:
		}
	}

	@Override
	public float getOldScaling() {
		return this.oldScaling;
	}

	@Override
	public int[] getHighlightedImage() {
		return this.highlightedImage;
	}
}
