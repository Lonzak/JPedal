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
 * Display.java
 * ---------------
 */
package org.jpedal;

//<start-thin><start-adobe>
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.Map;

import javax.swing.border.Border;

import org.jpedal.examples.viewer.gui.generic.GUIThumbnailPanel;
import org.jpedal.objects.PdfPageData;
import org.jpedal.render.DynamicVectorRenderer;

//<end-adobe><end-thin>

public interface Display {

	public static final int BORDER_SHOW = 1;

	public static final int BORDER_HIDE = 0;

	/** x and y axis */
	int X_AXIS = 0;

	int Y_AXIS = 1;

	/** when no display is set */
	int NODISPLAY = 0;

	/** show pages one at a time */
	int SINGLE_PAGE = 1;

	/** show all pages */
	int CONTINUOUS = 2;

	/** show pages two at a time */
	int FACING = 3;

	/** show all pages two at a time */
	int CONTINUOUS_FACING = 4;

	/** PageFlowing mode */
	int PAGEFLOW = 5;

	int DISPLAY_LEFT_ALIGNED = 1;

	int DISPLAY_CENTERED = 2;

	public enum BoolValue {
		TURNOVER_ON, SEPARATE_COVER
	}

	/** flag used in development of layout modes */
	final boolean debugLayout = false;

	Dimension getPageSize(int displayView);

	void initRenderer(Map areas, Graphics2D g2, Border myBorder, int indent);

	void decodeOtherPages(int pageNumber, int pageCount);

	void stopGeneratingPage();

	void refreshDisplay();

	void disableScreen();

	void flushPageCaches();

	void resetCachedValues();

	void init(float scaling, int pageCount, int displayRotation, int pageNumber, DynamicVectorRenderer currentDisplay, boolean isInit,
			PdfPageData pageData, int insetW, int insetH);

	boolean isAccelerated();

	Rectangle drawPage(AffineTransform viewScaling, AffineTransform displayScaling, int pageUsedForTransform);

	void drawBorder();

	void setup(boolean useAcceleration, PageOffsets currentOffset, PdfDecoder pdfDecoder);

	void completeForm(Graphics2D g2);

	void resetToDefaultClip();

	int getYCordForPage(int page);

	int getYCordForPage(int page, float scaling);

	int getStartPage();

	int getEndPage();

	int getXCordForPage(int currentPage, float scaling);

	int getXCordForPage(int currentPage);

	// <start-thin><start-adobe>
	void setThumbnailPanel(GUIThumbnailPanel thumbnails);

	// <end-adobe><end-thin>

	void setScaling(float scaling);

	void setPageOffsets(int page);

	void addAdditionalPage(DynamicVectorRenderer dynamicRenderer, int pageWidth, int origPageWidth);

	void clearAdditionalPages();

	void dispose();

	public void setAcceleration(boolean enable);

	public int getWidthForPage(int pageNumber);

	public void setObjectValue(int type, Object newValue);

	public void drawPreviewImage(Graphics2D g2, Rectangle visibleRect);

	public int[] getHighlightedImage();

	public void setHighlightedImage(int[] i);

	float getOldScaling();

	public boolean getBoolean(BoolValue option);

	public void setBoolean(BoolValue option, boolean value);

}
