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
 * Values.java
 * ---------------
 */
package org.jpedal.examples.viewer;

/** provides access to values used by multiple classes */
public class Values {

	/** Normal mode (works for webstart, application) */
	public static final int RUNNING_NORMAL = 0;
	public static final int RUNNING_APPLET = 1;
	public static final int RUNNING_WEBSTART = 2;
	public static final int RUNNING_JSP = 3;
	public static final int RUNNING_PLUGIN = 4;

	/** flag to show if itext is available */
	private boolean isItextOnClasspath = false;

	/** flag to show if an encryption library is available */
	private boolean isEncryptOnClasspath = false;

	/** flag to show if file opened is PDF or not */
	private boolean isPDF = true;

	/** flag to show if the file opened is a Tiff with multiple pages or not */
	private boolean isMultiTiff = false;

	/** allow common code to be aware if applet or webstart or JSP */
	private int modeOfOperation = RUNNING_NORMAL;

	/** size of file for display */
	private long size;

	/** directory to load files from */
	private String inputDir = null;

	/** current page number */
	private int currentPage = 1;

	/** name of current file being decoded */
	private String selectedFile = null;

	/** flag to show that form values have been altered by user */
	private boolean formsChanged;

	/** file separator used */
	private String separator = null;

	/** uses hires images for display (uses more memory) */
	private boolean useHiresImage = true;

	public int m_x1, m_y1, m_x2, m_y2;

	/** offsets to viewport if used */
	public int dx, dy = 0;

	/** scaling on viewport if used */
	public double viewportScale = 1;

	/**
	 * height of the viewport. Because everything is draw upside down we need this to get true y value
	 */
	public int maxViewY = 0;

	/**
	 * number of pages in current pdf (inclusive so 2 page doc would have 2 with first page as 1)
	 */
	private int pageCount = 1;
	private int maxNoOfMultiViewers;

	/** boolean lock to stop multiple access */
	public static boolean isProcessing = false;

	/**/

	public Values() {
		try {
			this.isItextOnClasspath = getClass().getResource("/com/itextpdf") != null;
		}
		catch (Exception e) {
			this.isItextOnClasspath = false;
		}
		catch (Error e) {
			this.isItextOnClasspath = false;
		}

		String altSP = System.getProperty("org.jpedal.securityprovider");

		try {
			if (altSP == null) altSP = "/org/bouncycastle/";

			this.isEncryptOnClasspath = getClass().getResource(altSP) != null;
		}
		catch (Exception e) {
			this.isEncryptOnClasspath = false;
		}
		catch (Error e) {
			this.isEncryptOnClasspath = false;
		}
	}

	/**
	 * flag to show isProcessing so Viewer can lock actions while decoding page
	 */
	public static boolean isProcessing() {
		return isProcessing;
	}

	/**
	 * set to show decoding page
	 */
	public static void setProcessing(boolean isProcessing) {
		Values.isProcessing = isProcessing;
	}

	/**
	 * show if itext installed
	 */
	public boolean isItextOnClasspath() {
		return this.isItextOnClasspath;
	}

	public boolean isEncrypOnClasspath() {
		return this.isEncryptOnClasspath;
	}

	/**
	 * show if file is type PDF
	 */
	public boolean isPDF() {
		return this.isPDF;
	}

	/**
	 * set flag to show if file is PDF or other
	 */
	public void setPDF(boolean isPDF) {
		this.isPDF = isPDF;
	}

	/**
	 * get current page number (1 - pageCount)
	 */
	public int getCurrentPage() {
		return this.currentPage;
	}

	/**
	 * set current page number (1 - pageCount)
	 */
	public void setCurrentPage(int currentPage) {
		this.currentPage = currentPage;
	}

	/**
	 * get directory to use as input root
	 */
	public String getInputDir() {

		// <start-wrap>
		if (this.inputDir == null) this.inputDir = System.getProperty("user.dir");
		/**
		 * //<end-wrap> if(inputDir==null) inputDir= ""; /
		 **/

		return this.inputDir;
	}

	/**
	 * set directory to use as input root
	 */
	public void setInputDir(String inputDir) {
		this.inputDir = inputDir;
	}

	/**
	 * get current filename
	 */
	public String getSelectedFile() {
		return this.selectedFile;
	}

	/**
	 * set current filename
	 */
	public void setSelectedFile(String selectedFile) {
		this.selectedFile = selectedFile;
	}

	/**
	 * return if user has edited forms
	 */
	public boolean isFormsChanged() {
		return this.formsChanged;
	}

	/**
	 * set user has edited forms
	 */
	public void setFormsChanged(boolean formsChanged) {
		this.formsChanged = formsChanged;
	}

	/**
	 * get number of pages
	 */
	public int getPageCount() {
		return this.pageCount;
	}

	/**
	 * set number of pages
	 */
	public void setPageCount(int pageCount) {
		this.pageCount = pageCount;
	}

	/**
	 * get current file size in kilobytes
	 */
	public long getFileSize() {
		return this.size;
	}

	/**
	 * set current file size in kilobytes
	 */
	public void setFileSize(long size) {
		this.size = size;
	}

	/**
	 * get platform file separator
	 */
	public String getSeparator() {

		// <start-wrap>
		if (this.separator == null) this.separator = System.getProperty("file.separator");
		/**
		 * //<end-wrap> if(separator==null) separator= "/"; /
		 **/

		return this.separator;
	}

	/**
	 * get modeOfOperation (RUNNING_NORMAL,RUNNING_APPLET,RUNNING_WEBSTART,RUNNING_JSP)
	 */
	public int getModeOfOperation() {
		return this.modeOfOperation;
	}

	/**
	 * set modeOfOperation (RUNNING_NORMAL,RUNNING_APPLET,RUNNING_WEBSTART,RUNNING_JSP)
	 */
	public void setModeOfOperation(int modeOfOperation) {
		this.modeOfOperation = modeOfOperation;
	}

	/**
	 * flag to show if using images as hires
	 */
	public boolean isUseHiresImage() {
		return this.useHiresImage;
	}

	/**
	 * set to show images being used are hires and not downsampled
	 */
	public void setUseHiresImage(boolean useHiresImage) {
		this.useHiresImage = useHiresImage;
	}

	public boolean isMultiTiff() {
		return this.isMultiTiff;
	}

	public void setMultiTiff(boolean isMultiTiff) {
		this.isMultiTiff = isMultiTiff;
	}

	public void setMaxMiltiViewers(int maxNoOfMultiViewers) {
		this.maxNoOfMultiViewers = maxNoOfMultiViewers;
	}

	public int getMaxMiltiViewers() {
		return this.maxNoOfMultiViewers;
	}
}
