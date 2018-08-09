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
 * GUI.java
 * ---------------
 */

package org.jpedal.examples.viewer.gui;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;

import org.jpedal.Display;
import org.jpedal.PdfDecoder;
import org.jpedal.SingleDisplay;
import org.jpedal.examples.viewer.Values;
import org.jpedal.examples.viewer.Viewer;
import org.jpedal.examples.viewer.gui.generic.GUIButton;
import org.jpedal.examples.viewer.gui.generic.GUICombo;
import org.jpedal.examples.viewer.gui.generic.GUIOutline;
import org.jpedal.examples.viewer.gui.generic.GUIThumbnailPanel;
import org.jpedal.examples.viewer.gui.swing.SwingOutline;
import org.jpedal.examples.viewer.utils.PropertiesFile;
import org.jpedal.utils.Messages;

/** any shared GUI code - generic and AWT */
public class GUI {

	/** nav buttons - global so accessible to ContentExtractor */
	public GUIButton first, fback, back, forward, fforward, end;

	public GUIButton singleButton, continuousButton, continuousFacingButton, facingButton, pageFlowButton;

	/**
	 * list for types - assumes present in org/jpedal/examples/viewer/annots* "OTHER" MUST BE FIRST ITEM Try adding Link to the list to see links
	 */
	private String[] annotTypes = { "Other", "Text", "FileAttachment" };

	// private Color[] annotColors={Color.RED,Color.BLUE,Color.BLUE};

	protected boolean hiResPrinting = false;

	// @annot - table of objects we wish to track
	protected Map objs;

	// flag if generated so we setup once for each file
	protected boolean bookmarksGenerated = false;

	public boolean useHiResPrinting() {
		return this.hiResPrinting;
	}

	public void setHiResPrinting(boolean hiResPrinting) {
		this.hiResPrinting = hiResPrinting;
	}

	public String getPropertiesFileLocation() {
		return this.properties.getConfigFile();
	}

	public void setPropertiesFileLocation(String file) {
		this.properties.loadProperties(file);
	}

	public void setProperties(String item, boolean value) {
		this.properties.setValue(item, String.valueOf(value));
	}

	public void setPreferences(int dpi, int search, int border, boolean scroll, int pageMode, boolean updateDefaultValue, int maxNoOfMultiViewers,
			boolean showDownloadWindow, boolean useHiResPrinting) {

		// Set border config value and repaint
		SingleDisplay.CURRENT_BORDER_STYLE = border;
		this.properties.setValue("borderType", String.valueOf(border));

		// Set autoScroll default and add to properties file
		this.allowScrolling = scroll;
		this.properties.setValue("autoScroll", String.valueOf(scroll));

		// Dpi is taken into effect when zoom is called
		this.decode_pdf.getDPIFactory().setDpi(dpi);
		this.properties.setValue("resolution", String.valueOf(dpi));

		// @kieran Ensure valid value if not recognised
		if (pageMode < Display.SINGLE_PAGE || pageMode > Display.CONTINUOUS_FACING) pageMode = Display.SINGLE_PAGE;

		// Default Page Layout
		this.decode_pdf.setPageMode(pageMode);
		this.properties.setValue("pageMode", String.valueOf(pageMode));

		this.decode_pdf.repaint();

		// Set the search window
		String propValue = this.properties.getValue("searchWindowType");
		if (propValue.length() > 0 && !propValue.equals(String.valueOf(search))) {
			if (Viewer.showMessages) JOptionPane.showMessageDialog(null, Messages.getMessage("PageLayoutViewMenu.ResetSearch"));
		}
		this.properties.setValue("searchWindowType", String.valueOf(search));

		this.properties.setValue("automaticupdate", String.valueOf(updateDefaultValue));

		this.commonValues.setMaxMiltiViewers(maxNoOfMultiViewers);
		this.properties.setValue("maxmultiviewers", String.valueOf(maxNoOfMultiViewers));

		this.useDownloadWindow = showDownloadWindow;
		this.properties.setValue("showDownloadWindow", String.valueOf(showDownloadWindow));

		this.hiResPrinting = useHiResPrinting;
		this.properties.setValue("useHiResPrinting", String.valueOf(showDownloadWindow));
	}

	/** handle for internal use */
	protected PdfDecoder decode_pdf;

	/** location for divider with thumbnails turned on */
	protected static final int thumbLocation = 200;

	/** minimum screen width to ensure menu buttons are visible */
	protected static final int minimumScreenWidth = 700;

	/** track pages decoded once already */
	protected HashMap pagesDecoded = new HashMap();

	/** allows user to toggle on/off text/image snapshot */
	public GUIButton snapshotButton;

	public int cropX;

	public int cropW;

	public int cropH;

	/** crop offset if present */
	protected int mediaX, mediaY;

	public int mediaW;

	public int cropY;

	public int mediaH;

	/** Use Download Windom */
	protected boolean useDownloadWindow = true;

	/** show if outlines drawn */
	protected boolean hasOutlinesDrawn = false;

	/** XML structure of bookmarks */
	protected GUIOutline tree = new SwingOutline();

	/** stops autoscrolling at screen edge */
	protected boolean allowScrolling = true;

	/** confirms exit when closing the window */
	protected boolean confirmClose = false;

	/** location for the divider when bookmarks are displayed */
	protected int divLocation = 170;

	/** flag to switch bookmarks on or off */
	protected boolean showOutlines = true;

	/** scaling values as floats to save conversion */
	protected float[] scalingFloatValues = { 1.0f, 1.0f, 1.0f, .25f, .5f, .75f, 1.0f, 1.25f, 1.5f, 2.0f, 2.5f, 5.0f, 7.5f, 10.0f };

	/** page scaling to use 1=100% */
	protected float scaling = 1;

	/** padding so that the pdf is not right at the edge */
	protected static int inset = 25;

	/** store page rotation */
	protected int rotation = 0;

	/** scaling values as floats to save conversion */
	protected String[] rotationValues = { "0", "90", "180", "270" };

	/** scaling factors on the page */
	protected GUICombo rotationBox;

	/** allows user to set quality of images */
	protected GUICombo qualityBox;

	/** scaling factors on the page */
	protected GUICombo scalingBox;

	/** default scaling on the combobox scalingValues */
	protected static int defaultSelection = 0;

	/** title message on top if you want to over-ride JPedal default */
	protected String titleMessage = null;

	protected Values commonValues;

	protected GUIThumbnailPanel thumbnails;

	protected PropertiesFile properties;

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#allowScrolling()
	 */
	public boolean allowScrolling() {
		return this.allowScrolling;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#confirmClose()
	 */
	public boolean confirmClose() {
		return this.confirmClose;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#getAnnotTypes()
	 */
	public String[] getAnnotTypes() {
		return this.annotTypes;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#setAutoScrolling(boolean allowScrolling)
	 */
	public void setAutoScrolling(boolean allowScrolling) {
		this.allowScrolling = allowScrolling;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#toogleAutoScrolling()
	 */
	public void toogleAutoScrolling() {
		this.allowScrolling = !this.allowScrolling;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#getRotation()
	 */
	public int getRotation() {
		return this.rotation;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#getScaling()
	 */
	public float getScaling() {
		return this.scaling;
	}

	public void setScaling(float s) {
		this.scaling = s;
		this.scalingBox.setSelectedIndex((int) this.scaling);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.examples.viewer.gui.swing.GUIFactory#getPDFDisplayInset()
	 */
	public static int getPDFDisplayInset() {
		return inset;
	}

	// <link><a name="handleAnnotations" />
	/**
	 * example code which sets up an individual icon for each annotation to display - only use if you require each annotation to have its own icon
	 * <p>
	 * To use this you ideally need to parse the annotations first -there is a method allowing you to extract just the annotations from the data.
	 */
	public void createUniqueAnnotationIcons() {
	}

	public void setDpi(int dpi) {
		this.decode_pdf.getDPIFactory().setDpi(dpi);
	}

	public boolean isUseDownloadWindow() {
		return this.useDownloadWindow;
	}

	public void setUseDownloadWindow(boolean useDownloadWindow) {
		this.useDownloadWindow = useDownloadWindow;
	}

	public PropertiesFile getProperties() {
		return this.properties;
	}

	public void dispose() {

		this.first = null;
		this.fback = null;
		this.back = null;
		this.forward = null;
		this.fforward = null;
		this.end = null;

		this.singleButton = null;
		this.continuousButton = null;
		this.continuousFacingButton = null;
		this.facingButton = null;
		this.pageFlowButton = null;

		this.annotTypes = null;

		// annotColors=null;

		this.pagesDecoded = null;

		this.snapshotButton = null;

		this.tree = null;
		// protected PdfDecoder decode_pdf;

		this.pagesDecoded = null;

		this.snapshotButton = null;

		this.scalingFloatValues = null;

		this.rotationValues = null;

		this.rotationBox = null;

		this.qualityBox = null;

		this.scalingBox = null;

		this.titleMessage = null;

		// protected Values commonValues;

		// protected GUIThumbnailPanel thumbnails;
	}
}
