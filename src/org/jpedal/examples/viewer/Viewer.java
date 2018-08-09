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
 * Viewer.java
 * ---------------
 */

package org.jpedal.examples.viewer;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.event.DocumentListener;

import org.jpedal.Display;
import org.jpedal.PdfDecoder;
import org.jpedal.examples.viewer.gui.MultiViewTransferHandler;
import org.jpedal.examples.viewer.gui.SingleViewTransferHandler;
import org.jpedal.examples.viewer.gui.SwingGUI;
import org.jpedal.examples.viewer.gui.generic.GUIMouseHandler;
import org.jpedal.examples.viewer.gui.generic.GUISearchWindow;
import org.jpedal.examples.viewer.gui.generic.GUIThumbnailPanel;
import org.jpedal.examples.viewer.gui.popups.TipOfTheDay;
import org.jpedal.examples.viewer.gui.swing.SearchList;
import org.jpedal.examples.viewer.gui.swing.SwingMouseListener;
import org.jpedal.examples.viewer.gui.swing.SwingSearchWindow;
import org.jpedal.examples.viewer.gui.swing.SwingThumbnailPanel;
import org.jpedal.examples.viewer.utils.Printer;
import org.jpedal.examples.viewer.utils.PropertiesFile;
import org.jpedal.exception.PdfException;
import org.jpedal.external.Options;
import org.jpedal.fonts.FontMappings;
import org.jpedal.io.JAIHelper;
import org.jpedal.objects.acroforms.actions.ActionHandler;
import org.jpedal.objects.raw.OutlineObject;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.Messages;
import org.w3c.dom.Node;

/**
 * PDF viewer
 * 
 * If you are compiling, you will need to download all the examples source files from http://www.idrsolutions.com/how-to-view-pdf-files-in-java/
 * 
 * Run directly from jar with java -cp jpedal.jar org/jpedal/examples/viewer/Viewer or java -jar jpedal.jar
 * 
 * Lots of tutorials on how to configure on our website
 * 
 * If you want to implement your own Very simple example at http://www.jpedal.org/gplSrc/org/jpedal/examples/jpaneldemo/JPanelDemo.java.html But we
 * would recommend you look at the full viewer as it is totally configurable and does everything for you.
 * 
 * See also http://www.jpedal.org/javadoc/org/jpedal/constants/JPedalSettings.html for settings to customise
 * 
 * Fully featured GUI viewer and demonstration of JPedal's capabilities
 * 
 * <br>
 * This class provides the framework for the Viewer and calls other classes which provide the following functions:-
 * 
 * <br>
 * Values commonValues - repository for general settings Printer currentPrinter - All printing functions and access methods to see if printing active
 * PdfDecoder decode_pdf - PDF library and panel ThumbnailPanel thumbnails - provides a thumbnail pane down the left side of page - thumbnails can be
 * clicked on to goto page PropertiesFile properties - saved values stored between sessions SwingGUI currentGUI - all Swing GUI functions SearchWindow
 * searchFrame (not GPL) - search Window to search pages and goto references on any page Commands currentCommands - parses and executes all options
 * SwingMouseHandler mouseHandler - handles all mouse and related activity
 */
public class Viewer {

	/** control if messages appear */
	public static boolean showMessages = true;

	/** repository for general settings */
	protected Values commonValues = new Values();

	/** All printing functions and access methods to see if printing active */
	protected Printer currentPrinter = new Printer();

	/** PDF library and panel */
	protected PdfDecoder decode_pdf = new PdfDecoder(true);

	/** encapsulates all thumbnail functionality - just ignore if not required */
	protected GUIThumbnailPanel thumbnails = new SwingThumbnailPanel(this.commonValues, this.decode_pdf);

	/** values saved on file between sessions */
	private PropertiesFile properties = new PropertiesFile();

	/** general GUI functions */
	public SwingGUI currentGUI = new SwingGUI(this.decode_pdf, this.commonValues, this.thumbnails, this.properties);

	/** search window and functionality */
	private GUISearchWindow searchFrame = new SwingSearchWindow(this.currentGUI);

	/** command functions */
	protected Commands currentCommands = new Commands(this.commonValues, this.currentGUI, this.decode_pdf, this.thumbnails, this.properties,
			this.searchFrame, this.currentPrinter);

	/** all mouse actions */
	// protected GUIMouseHandler mouseHandler=new SwingMouseHandler(decode_pdf,currentGUI,commonValues,currentCommands);
	protected GUIMouseHandler mouseHandler = new SwingMouseListener(this.decode_pdf, this.currentGUI, this.commonValues, this.currentCommands);

	/** scaling values which appear onscreen */
	protected String[] scalingValues;

	/** warn user if viewer not setup fully */
	private boolean isSetup;

	// private Object[] restrictedMenus;

	/** Location of Preferences Files */
	public final static String PREFERENCES_DEFAULT = "jar:/org/jpedal/examples/viewer/res/preferences/Default.xml";
	// public final static String PREFERENCES_TABLEZONER = "jar:/org/jpedal/examples/viewer/res/preferences/TableZoner.xml";
	// public final static String PREFERENCES_CONTENTEXTRACTOR = "jar:/org/jpedal/examples/viewer/res/preferences/ContentExtractor.xml";
	public final static String PREFERENCES_NO_GUI = "jar:/org/jpedal/examples/viewer/res/preferences/NoGUI.xml";
	public final static String PREFERENCES_NO_SIDE_BAR = "jar:/org/jpedal/examples/viewer/res/preferences/NoSideTabOrTopButtons.xml";
	public final static String PREFERENCES_OPEN_AND_NAV_ONLY = "jar:/org/jpedal/examples/viewer/res/preferences/OpenAndNavOnly.xml";
	public final static String PREFERENCES_PDFHELP = "jar:/org/jpedal/examples/viewer/res/preferences/PDFHelp.xml";
	public final static String PREFERENCES_BEAN = "jar:/org/jpedal/examples/viewer/res/preferences/Bean.xml";

	/** tell software to exit on close - default is true */
	public static boolean exitOnClose = true;

	/** used internally - please do not use */
	private static String rawFile = "";
	public static String file = "";

	// <start-wrap>
	/**
	 * //<end-wrap>
	 * 
	 * public static String message="${titleMessage}";
	 * 
	 * private static int count=0;
	 * 
	 * // setup and run client with hard-coded file public Viewer() { //enable error messages which are OFF by default
	 * 
	 * PdfDecoder.showErrorMessages=true;
	 * 
	 * properties.loadProperties();
	 * 
	 * //clean up name String[] seps=new String[]{"/","\\"}; for(int ii=0;ii<seps.length;ii++){ int id=rawFile.lastIndexOf(seps[ii]); if(id!=-1)
	 * rawFile=rawFile.substring(id+1,rawFile.length()); }
	 * 
	 * file=rawFile;
	 * 
	 * count++; } /
	 **/

	/**
	 * setup and run client, loading defaultFile on startup (please do not use) - use setupViewer();, openDefaultFile(defaultFile)
	 * 
	 **/
	// <start-wrap>
	/**
	 * //<end-wrap>
	 * 
	 * private String defaultFile=null;
	 * 
	 * public void setupViewer(String defaultFile) {
	 * 
	 * if(count==0) throw new RuntimeException("You cannot use wrapper to open PDFs");
	 * 
	 * this.defaultFile=defaultFile;
	 * 
	 * setupViewer();
	 * 
	 * openDefaultFile();
	 * 
	 * }/
	 **/

	/**
	 * open the file passed in by user on startup (do not call directly)
	 */
	public SwingGUI getSwingGUI() {
		return this.currentGUI;
	}

	/**
	 * 
	 * @param defaultFile
	 *            Allow user to open PDF file to display
	 */
	// <start-wrap>
	public void openDefaultFile(String defaultFile) {
		/**
		 * //<end-wrap> public void openDefaultFile() {
		 * 
		 * String defaultFile=this.defaultFile; if(defaultFile==null){ defaultFile="jar:/org/jpedal/file.pdf";
		 * currentCommands.inputStream=this.getClass().getResourceAsStream("jar:/org/jpedal/file.pdf"); } /
		 **/

		// get any user set dpi
		String hiresFlag = System.getProperty("org.jpedal.hires");
		if (Commands.hires || hiresFlag != null) this.commonValues.setUseHiresImage(true);

		// get any user set dpi
		String memFlag = System.getProperty("org.jpedal.memory");
		if (memFlag != null) this.commonValues.setUseHiresImage(false);

		// reset flag
		if (this.thumbnails.isShownOnscreen()) this.thumbnails.resetToDefault();

		this.commonValues.maxViewY = 0;// ensure reset for any viewport

		/**
		 * open any default file and selected page
		 */
		if (defaultFile != null) {

			// <start-wrap>
			File testExists = new File(defaultFile);
			boolean isURL = false;
			if (defaultFile.startsWith("http:") || defaultFile.startsWith("jar:") || defaultFile.startsWith("file:")) {
				LogWriter.writeLog("Opening http connection");
				isURL = true;
			}

			if ((!isURL) && (!testExists.exists())) {
				this.currentGUI.showMessageDialog(defaultFile + '\n' + Messages.getMessage("PdfViewerdoesNotExist.message"));
			}
			else
				if ((!isURL) && (testExists.isDirectory())) {
					this.currentGUI.showMessageDialog(defaultFile + '\n' + Messages.getMessage("PdfViewerFileIsDirectory.message"));
				}
				else {
					this.commonValues.setFileSize(testExists.length() >> 10);

					// <end-wrap>

					this.commonValues.setSelectedFile(defaultFile);

					this.currentGUI.setViewerTitle(null);

					// <start-wrap>
					/** see if user set Page */
					String page = System.getProperty("org.jpedal.page");
					String bookmark = System.getProperty("org.jpedal.bookmark");
					if (page != null && !isURL) {

						try {
							int pageNum = Integer.parseInt(page);

							if (pageNum < 1) {
								pageNum = -1;
								System.err.println(page + " must be 1 or larger. Opening on page 1");
								LogWriter.writeLog(page + " must be 1 or larger. Opening on page 1");
							}

							if (pageNum != -1) openFile(testExists, pageNum);

						}
						catch (Exception e) {
							System.err.println(page + "is not a valid number for a page number. Opening on page 1");
							LogWriter.writeLog(page + "is not a valid number for a page number. Opening on page 1");
						}
					}
					else
						if (bookmark != null) {
							openFile(testExists, bookmark);
						}
						else {
							// <end-wrap>
							try {
								this.currentCommands.openFile(defaultFile);
							}
							catch (PdfException e) {}
							// <start-wrap>
						}
				}
			// <end-wrap>
		}

		// <start-wrap>
		/**
		 * //<end-wrap> executeCommand(Commands.SINGLE,null); /
		 **/
	}

	/**
	 * 
	 * @param defaultFile
	 *            Allow user to open PDF file to display
	 */
	// <start-wrap>
	public void openDefaultFileAtPage(String defaultFile, int page) {
		/**
		 * //<end-wrap> private void openDefaultFileAtPage(String defaultFile, int page) { /
		 **/

		// get any user set dpi
		String hiresFlag = System.getProperty("org.jpedal.hires");
		if (Commands.hires || hiresFlag != null) this.commonValues.setUseHiresImage(true);

		// get any user set dpi
		String memFlag = System.getProperty("org.jpedal.memory");
		if (memFlag != null) this.commonValues.setUseHiresImage(false);

		// reset flag
		if (this.thumbnails.isShownOnscreen()) this.thumbnails.resetToDefault();

		this.commonValues.maxViewY = 0;// ensure reset for any viewport

		/**
		 * open any default file and selected page
		 */
		if (defaultFile != null) {

			File testExists = new File(defaultFile);
			boolean isURL = false;
			if (defaultFile.startsWith("http:") || defaultFile.startsWith("jar:")) {
				LogWriter.writeLog("Opening http connection");
				isURL = true;
			}

			if ((!isURL) && (!testExists.exists())) {
				this.currentGUI.showMessageDialog(defaultFile + '\n' + Messages.getMessage("PdfViewerdoesNotExist.message"));
			}
			else
				if ((!isURL) && (testExists.isDirectory())) {
					this.currentGUI.showMessageDialog(defaultFile + '\n' + Messages.getMessage("PdfViewerFileIsDirectory.message"));
				}
				else {

					this.commonValues.setSelectedFile(defaultFile);
					// <start-wrap>
					this.commonValues.setFileSize(testExists.length() >> 10);
					// <end-wrap>
					this.currentGUI.setViewerTitle(null);

					openFile(testExists, page);

				}
		}
	}

	// <start-wrap>
	/**
	 * setup and run client
	 */
	public Viewer() {
		// enable error messages which are OFF by default
		PdfDecoder.showErrorMessages = true;

		String prefFile = System.getProperty("org.jpedal.Viewer.Prefs");
		if (prefFile != null) {
			this.properties.loadProperties(prefFile);
		}
		else {
			this.properties.loadProperties();
		}

		// properties.loadProperties();
	}

	// <end-wrap>
	// <start-wXXrap> //here when other files removed

	/**
	 * setup and run client passing in paramter to show if running as applet, webstart or JSP (only applet has any effect at present)
	 */
	public Viewer(int modeOfOperation) {

		// <start-wrap>
		/**
		 * //<end-wrap>
		 * 
		 * //clean up name String[] seps=new String[]{"/","\\"}; for(int ii=0;ii<seps.length;ii++){ int id=rawFile.lastIndexOf(seps[ii]); if(id!=-1)
		 * rawFile=rawFile.substring(id+1,rawFile.length()); }
		 * 
		 * file=rawFile; /
		 **/

		// enable error messages which are OFF by default
		PdfDecoder.showErrorMessages = true;

		String prefFile = System.getProperty("org.jpedal.Viewer.Prefs");
		if (prefFile != null) {
			this.properties.loadProperties(prefFile);
		}
		else {
			this.properties.loadProperties();
		}

		this.commonValues.setModeOfOperation(modeOfOperation);
	}

	/**
	 * setup and run client passing in paramter that points to the preferences file we should use.
	 */
	public Viewer(String prefs) {

		// enable error messages which are OFF by default
		PdfDecoder.showErrorMessages = true;

		// Example preference file can be found here. You will know when it's working
		// String p = "org/jpedal/examples/viewer/res/preferences/NoGUI.xml";

		//
		// // p = p.replaceAll("\\.", "/");
		// URL u = Thread.currentThread().getContextClassLoader().getResource(
		// p);
		// ArrayList retValue = new ArrayList(0);
		// String s = u.toString();
		//
		// System.out.println("scanning " + s);
		//
		// if (s.startsWith("jar:") && s.endsWith(p)) {
		// int idx = s.lastIndexOf(p);
		// s = s.substring(0, idx); // isolate entry name
		//
		// System.out.println("entry= " + s);
		// try{
		// URL url = new URL(s);
		// // Get the jar file
		// JarURLConnection conn = (JarURLConnection) url.openConnection();
		// JarFile jar = conn.getJarFile();
		//
		// for (Enumeration e = jar.entries(); e.hasMoreElements();) {
		// JarEntry entry = (JarEntry) e.nextElement();
		// if ((!entry.isDirectory())
		// & (entry.getName().startsWith(p))) { // this
		// // is how you can match
		// // to find your fonts.
		// // System.out.println("Found a match!");
		// String fontName = entry.getName();
		// int i = fontName.lastIndexOf('/');
		// fontName = fontName.substring(i + 1);
		// retValue.add(fontName);
		// }
		// }
		// }catch(Exception e){
		//
		// }
		// } else {
		// // Does not start with "jar:"
		// // Dont know - should not happen
		// System.out.println(p);
		// System.exit(1);
		// }

		try {
			this.properties.loadProperties(prefs);
		}
		catch (Exception e) {
			System.err.println("Specified Preferrences file not found at " + prefs
					+ ". If this file is within a jar ensure filename has jar: at the begining.");
			System.exit(1);
		}
	}

	/**
	 * setup and run client passing in parameter that points to the preferences file we should use.
	 */
	public Viewer(Container rootContainer, String preferencesPath) {

		// enable error messages which are OFF by default
		PdfDecoder.showErrorMessages = true;

		if (preferencesPath != null && preferencesPath.length() > 0) {
			try {
				this.properties.loadProperties(preferencesPath);
			}
			catch (Exception e) {
				System.err.println("Specified Preferrences file not found at " + preferencesPath
						+ ". If this file is within a jar ensure filename has jar: at the begining.");
				System.exit(1);
			}
		}
		else {
			this.properties.loadProperties();
		}
		setRootContainer(rootContainer);
	}

	/**
	 * Pass a document listener to the page counter to watch for changes to the page number. This value is updated when the page is altered.
	 * 
	 * @param docListener
	 *            :: A document listener to listen for changes to page number. New page number can be found in the insertUpdate method using
	 *            DocumentEvent.getDocument().getText(int offset, int length)
	 */
	public void addPageChangeListener(DocumentListener docListener) {
		if (this.currentGUI != null) this.currentGUI.addPageChangeListener(docListener);
	}

	public void setRootContainer(Container rootContainer) {
		if (rootContainer == null) throw new RuntimeException("Null containers not allowed.");

		Container c = rootContainer;

		if ((rootContainer instanceof JTabbedPane)) {
			JPanel temp = new JPanel(new BorderLayout());
			rootContainer.add(temp);
			c = temp;
		}
		else
			if (rootContainer instanceof JScrollPane) {
				JPanel temp = new JPanel(new BorderLayout());
				((JScrollPane) rootContainer).getViewport().add(temp);
				c = temp;

			}
			else
				if (rootContainer instanceof JSplitPane) {
					throw new RuntimeException(
							"To add the viewer to a split pane please pass through either JSplitPane.getLeftComponent() or JSplitPane.getRightComponent()");
				}

		if (!(rootContainer instanceof JFrame)) {
			c.setLayout(new BorderLayout());
		}

		// Used to prevent infinite scroll issue as a preferred size has been set
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		int width = d.width / 2, height = d.height / 2;
		if (width < 700) width = 700;

		// allow user to alter size
		String customWindowSize = System.getProperty("org.jpedal.startWindowSize");
		if (customWindowSize != null) {

			StringTokenizer values = new StringTokenizer(customWindowSize, "x");

			System.out.println(values.countTokens());
			if (values.countTokens() != 2) throw new RuntimeException("Unable to use value for org.jpedal.startWindowSize=" + customWindowSize
					+ "\nValue should be in format org.jpedal.startWindowSize=200x300");

			try {
				width = Integer.parseInt(values.nextToken().trim());
				height = Integer.parseInt(values.nextToken().trim());

			}
			catch (Exception ee) {
				throw new RuntimeException("Unable to use value for org.jpedal.startWindowSize=" + customWindowSize
						+ "\nValue should be in format org.jpedal.startWindowSize=200x300");
			}
		}

		c.setPreferredSize(new Dimension(width, height));

		this.currentGUI.setFrame(c);

		// setupViewer();
	}

	// <end-wrap>

	/**
	 * Should be called before setupViewer
	 */
	public void loadProperties(String props) {
		this.properties.loadProperties(props);
	}

	/**
	 * Should be called before setupViewer
	 */
	public void loadProperties(InputStream is) {
		this.properties.loadProperties(is);
	}

	/**
	 * initialise and run client (default as Application in own Frame)
	 */
	public void setupViewer() {

		// also allow messages to be suppressed with JVM option
		String flag = System.getProperty("org.jpedal.suppressViewerPopups");
		boolean suppressViewerPopups = false;

		if (flag != null && flag.toLowerCase().equals("true")) suppressViewerPopups = true;

		/**
		 * set search window position here to ensure that gui has correct value
		 */
		String searchType = this.properties.getValue("searchWindowType");
		if (searchType != null && searchType.length() != 0) {
			int type = Integer.parseInt(searchType);
			this.searchFrame.setStyle(type);
		}
		else this.searchFrame.setStyle(SwingSearchWindow.SEARCH_MENU_BAR);

		// Set search frame here
		this.currentGUI.setSearchFrame(this.searchFrame);

		/** switch on thumbnails if flag set */
		String setThumbnail = System.getProperty("org.jpedal.thumbnail");
		if (setThumbnail != null) {
			if (setThumbnail.equals("true")) this.thumbnails.setThumbnailsEnabled(true);
			else
				if (setThumbnail.equals("false")) this.thumbnails.setThumbnailsEnabled(false);
		}
		else // default
		this.thumbnails.setThumbnailsEnabled(true);

		/**
		 * non-GUI initialisation
		 **/

		// allow user to override messages
		// <link><a name="locale" />
		/**
		 * allow user to define country and language settings
		 * 
		 * you will need a file called messages_XX.properties in org.jpedal.international.messages where XX is a valid Locale.
		 * 
		 * You can also choose an alternative Lovation - see sample code below
		 * 
		 * You can manually set Java to use a Locale with this code (also useful to test)
		 * 
		 * Example here is Brazil (note no Locale files present for it)
		 * 
		 * If you make and Locale files, we would be delighted to include them in future versions of the software.
		 * 
		 * java.util.Locale aLocale = new java.util.Locale("br", "BR");
		 * 
		 * java.util.Locale.setDefault(aLocale);
		 */

		String customBundle = System.getProperty("org.jpedal.bundleLocation");
		// customBundle="org.jpedal.international.messages"; //test code

		if (customBundle != null) {

			BufferedReader input_stream = null;
			ClassLoader loader = Messages.class.getClassLoader();
			String fileName = customBundle.replaceAll("\\.", "/") + '_' + java.util.Locale.getDefault().getLanguage() + ".properties";

			// also tests if locale file exists and tell user if not
			try {

				input_stream = new BufferedReader(new InputStreamReader(loader.getResourceAsStream(fileName)));

				input_stream.close();

			}
			catch (Exception ee) {

				java.util.Locale.setDefault(new java.util.Locale("en", "EN"));
				this.currentGUI.showMessageDialog("No locale file " + fileName + " has been defined for this Locale - using English as Default"
						+ "\n Format is path, using '.' as break ie org.jpedal.international.messages");

			}

			ResourceBundle rb = ResourceBundle.getBundle(customBundle);
			// Messages.setBundle(ResourceBundle.getBundle(customBundle));
			init(rb);

		}
		else init(null);

		/**
		 * gui setup, create gui, load properties
		 */
		this.currentGUI.init(this.scalingValues, this.currentCommands, this.currentPrinter);

		// now done on first usage
		// p.createPreferenceWindow(currentGUI);

		this.mouseHandler.setupMouse();

		if (this.searchFrame.getStyle() == SwingSearchWindow.SEARCH_TABBED_PANE) this.currentGUI.searchInTab(this.searchFrame);

		/**
		 * setup window for warning if renderer has problem
		 */
		this.decode_pdf.getDynamicRenderer().setMessageFrame(this.currentGUI.getFrame());

		String propValue = this.properties.getValue("showfirsttimepopup");
		boolean showFirstTimePopup = !suppressViewerPopups && propValue.length() > 0 && propValue.equals("true");

		if (showFirstTimePopup) {
			this.currentGUI.showFirstTimePopup();
			this.properties.setValue("showfirsttimepopup", "false");
		}
		else
			if (!suppressViewerPopups) propValue = this.properties.getValue("showrhinomessage");

		// if(!suppressViewerPopups && properties != null && (propValue.length()>0 && propValue.equals("true"))){
		//
		//
		// currentGUI.showMessageDialog(Messages.getMessage("Beta release Javascript support\n" +
		// "OS version does not contain Javascript support - please look at full version"));
		//
		// /**/
		// properties.setValue("showrhinomessage","false");
		// }

		if (!suppressViewerPopups && JAIHelper.isJAIused()) {
			propValue = this.properties.getValue("showddmessage");
			if (this.properties != null && (propValue.length() > 0 && propValue.equals("true"))) {

				this.currentGUI.showMessageDialog(Messages.getMessage("PdfViewer.JAIWarning") + Messages.getMessage("PdfViewer.JAIWarning1")
						+ Messages.getMessage("PdfViewer.JAIWarning2") + Messages.getMessage("PdfViewer.JAIWarning3")
						+ Messages.getMessage("PdfViewer.JAIWarning4"));

				this.properties.setValue("showddmessage", "false");
			}
		}

		// <start-wrap><start-pdfhelp>
		/**
		 * check for itext and tell user about benefits
		 */
		if (!suppressViewerPopups) {
			propValue = this.properties.getValue("showitextmessage");
			boolean showItextMessage = (propValue.length() > 0 && propValue.equals("true"));

			if (!this.commonValues.isItextOnClasspath() && showItextMessage) {

				// currentGUI.showItextPopup();

				this.properties.setValue("showitextmessage", "false");
			}
		}
		// <end-pdfhelp><end-wrap>

		if (this.currentGUI.isSingle()) {
			TransferHandler singleViewTransferHandler = new SingleViewTransferHandler(this.commonValues, this.thumbnails, this.currentGUI,
					this.currentCommands);
			this.decode_pdf.setTransferHandler(singleViewTransferHandler);
		}
		else {
			TransferHandler multiViewTransferHandler = new MultiViewTransferHandler(this.commonValues, this.thumbnails, this.currentGUI,
					this.currentCommands);
			this.currentGUI.getMultiViewerFrames().setTransferHandler(multiViewTransferHandler);
		}

		// DefaultTransferHandler dth = new DefaultTransferHandler(commonValues, thumbnails, currentGUI, currentCommands);
		// decode_pdf.setTransferHandler(dth);

		boolean wasUpdateAvailable = false;

		propValue = this.properties.getValue("displaytipsonstartup");
		if (!suppressViewerPopups && !wasUpdateAvailable && propValue.length() > 0 && propValue.equals("true")) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					TipOfTheDay tipOfTheDay = new TipOfTheDay(Viewer.this.currentGUI.getFrame(), "/org/jpedal/examples/viewer/res/tips",
							Viewer.this.properties);
					tipOfTheDay.setVisible(true);
				}
			});
		}

		// falg so we can warn user if thewy call executeCommand without it setup
		this.isSetup = true;
	}

	/**
	 * setup the viewer
	 */
	protected void init(ResourceBundle bundle) {

		/**
		 * load correct set of messages
		 */
		if (bundle == null) {

			// load locale file
			try {
				Messages.setBundle(ResourceBundle.getBundle("org.jpedal.international.messages"));
			}
			catch (Exception e) {
				LogWriter.writeLog("Exception " + e + " loading resource bundle.\n"
						+ "Also check you have a file in org.jpedal.international.messages to support Locale=" + java.util.Locale.getDefault());
			}

		}
		else {
			try {
				Messages.setBundle(bundle);
			}
			catch (Exception ee) {
				LogWriter.writeLog("Exception with bundle " + bundle);
				ee.printStackTrace();
			}
		}
		/** setup scaling values which ar displayed for user to choose */
		this.scalingValues = new String[] { Messages.getMessage("PdfViewerScaleWindow.text"), Messages.getMessage("PdfViewerScaleHeight.text"),
				Messages.getMessage("PdfViewerScaleWidth.text"), "25%", "50%", "75%", "100%", "125%", "150%", "200%", "250%", "500%", "750%", "1000%" };

		/**
		 * setup display
		 */
		if (SwingUtilities.isEventDispatchThread()) {

			this.decode_pdf.setDisplayView(Display.SINGLE_PAGE, Display.DISPLAY_CENTERED);

		}
		else {
			final Runnable doPaintComponent = new Runnable() {

				@Override
				public void run() {
					Viewer.this.decode_pdf.setDisplayView(Display.SINGLE_PAGE, Display.DISPLAY_CENTERED);
				}
			};
			SwingUtilities.invokeLater(doPaintComponent);
		}
		// decode_pdf.setDisplayView(Display.SINGLE_PAGE,Display.DISPLAY_CENTERED);

		// pass through GUI for use in multipages and Javascript
		this.decode_pdf.addExternalHandler(this.currentGUI, Options.MultiPageUpdate);

		// used to test ability to replace Javascript with own engine
		// org.jpedal.objects.javascript.ExpressionEngine marksTest=new TestEngine();
		// decode_pdf.addExternalHandler(marksTest, Options.ExpressionEngine);

		/** debugging code to create a log */
		// LogWriter.setupLogFile("v");
		// LogWriter.log_name = "/mnt/shared/log.txt";

		/**/

		// make sure widths in data CRITICAL if we want to split lines correctly!!
		PdfDecoder.init(true);

		// <link><a name="customann" />
		/**
		 * ANNOTATIONS code
		 * 
		 * replace Annotations with your own custom annotations using paint code
		 * 
		 */
		// decode_pdf.setAnnotationsVisible(false); //disable built-in annotations and use custom versions
		// code to create a unique iconset
		// see also <link><a
		// href="http://www.jpedal.org/gplSrc/org/jpedal/examples/viewer/gui/GUI.java.html#handleAnnotations">org.jpedal.examples.viewer.gui.GUI.handleAnnotations()</a>

		// this allows the user to place fonts in the classpath and use these for display, as if embedded
		// decode_pdf.addSubstituteFonts("org/jpedal/res/fonts/", true);

		// set to extract all
		// COMMENT OUT THIS LINE IF USING JUST THE VIEWER
		this.decode_pdf.setExtractionMode(0, 1); // values extraction mode,dpi of images, dpi of page as a factor of 72

		// don't extract text and images (we just want the display)

		/**/
		/**
		 * FONT EXAMPLE CODE showing JPedal's functionality to set values for non-embedded fonts.
		 * 
		 * This allows sophisticated substitution of non-embedded fonts.
		 * 
		 * Most font mapping is done as the fonts are read, so these calls must be made BEFORE the openFile() call.
		 */

		// <link><a name="fontmapping" />
		/**
		 * FONT EXAMPLE - Replace global default for non-embedded fonts.
		 * 
		 * You can replace Lucida as the standard font used for all non-embedded and substituted fonts by using is code. Java fonts are case
		 * sensitive, but JPedal resolves currentGUI.frame, so you could use Webdings, webdings or webDings for Java font Webdings
		 */

		/**
		 * Removed to save time on startup - uncomment if it causes problems try{ //choice of example font to stand-out (useful in checking results to
		 * ensure no font missed. //In general use Helvetica or similar is recommended // decode_pdf.setDefaultDisplayFont("SansSerif");
		 * }catch(PdfFontException e){ //if its not available catch error and show valid list
		 * 
		 * System.out.println(e.getMessage());
		 * 
		 * //get list of fonts you can use String[] fontList =GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		 * System.out.println(Messages.getMessage("PdfViewerFontsFound.message")); System.out.println("=====================\n"); int count =
		 * fontList.length; for (int i = 0; i < count; i++) { Font f=new Font(fontList[i],1,10);
		 * System.out.println(fontList[i]+" ("+Messages.getMessage("PdfViewerFontsPostscript.message")+ '=' +f.getPSName()+ ')');
		 * 
		 * } System.exit(1);
		 * 
		 * }
		 * 
		 * /** IMPORTANT note on fonts for EXAMPLES
		 * 
		 * USEFUL TIP : The Viewer displays a list of fonts used on the current PDF page with the File > Fonts menu option.
		 * 
		 * PDF allows the use of weights for fonts so Arial,Bold is a weight of Arial. This value is not case sensitive so JPedal would regard
		 * arial,bold and aRiaL,BoLd as the same.
		 * 
		 * Java supports a set of Font families internally (which may have weights), while JPedals substitution facility uses physical True Type fonts
		 * so it is resolving each font weight separately. So mapping works differently, depending on which is being used.
		 * 
		 * If you are using a font, which is named as arial,bold you can use either arial,bold or arial (and JPedal will then try to select the bold
		 * weight if a Java font is used).
		 * 
		 * So for a font such as Arial,Bold JPedal will test for an external truetype font substitution (ie arialMT.ttf) mapped to Arial,Bold. BUT if
		 * the substitute font is a Java font an additional test will be made for a match against Arial if there is no match on Arial,Bold.
		 * 
		 * If you want to map all Arial to equivalents to a Java font such as Times New Roman, just map Arial to Times New Roman (only works for
		 * inbuilt java fonts). Note if you map Arial,Bold to a Java font such as Times New Roman, you will get Times New Roman in a bold weight, if
		 * available. You cannot set a weight for the Java font.
		 * 
		 * If you wish to substitute Arial but not Arial,Bold you should explicitly map Arial,Bold to Arial,Bold as well.
		 * 
		 * The reason for the difference is that when using Javas inbuilt fonts JPedal can resolve the Font Family and will try to work out the weight
		 * internally. When substituting Truetype fonts, these only contain ONE weight so JPedal is resolving the Font and any weight as a separate
		 * font . Different weights will require separate files.
		 * 
		 * Open Source version does not support all font capabilities.
		 */

		/**
		 * FONT EXAMPLE - Use fonts placed in jar for substitution (1.4 and above only)
		 * 
		 * This allows users to store fonts in the jar and use these for substitution. Please see javadoc for full description of usage.
		 */
		// decode_pdf.addSubstituteFonts(fontPath,enforceMapping)

		// <link><a name="substitutedfont" />
		/**
		 * FONT EXAMPLE - Use fonts located on machine for substitution
		 * 
		 * This code explains how to use JPedal to substitute fonts which are not embedded using fonts held in any font directory.
		 * 
		 * It works as follows:-
		 * 
		 * If the -Dorg.jpedal.fontdirs="C:/win/fonts/","/mnt/X11/fonts" is set to a comma-separated list of directories, any truetype fonts (with
		 * .ttf file ending) will be logged and added to the substitution table. So arialMT.ttf will be added as arialmt. If arialmt is used in the
		 * PDF but not embedded, JPedal will use this font file to render it.
		 * 
		 * If a command line paramter is not appropriate, the call setFontDirs(String[] fontDirs) will achieve the same.
		 * 
		 * 
		 * If the name is not an exact match (ie you have arialMT which you wish to use to display arial, you can use the method
		 * setSubstitutedFontAliases(String[] name, String[] aliases) to convert it internally - see sample code at bottom of note.
		 * 
		 * The Name is not case-sensitive.
		 * 
		 * Spaces are important so TimesNewRoman and Times New Roman are degarded as 2 fonts.
		 * 
		 * If you have 2 copies of arialMT.ttf in the scanned directories, the last one will be used.
		 * 
		 * If the file was called arialMT,bold.ttf it is resolved as ArialMT,bold only.
		 * 
		 */

		// mappings for non-embedded fonts to use
		FontMappings.setFontReplacements();

		// decode_pdf.setFontDirs(new String[]{"C:/windows/fonts/","C:/winNT/fonts/"});
		/**
		 * FONT EXAMPLE - Use Standard Java fonts for substitution
		 * 
		 * This code tells JPedal to substitute fonts which are not embedded.
		 * 
		 * The Name is not case-sensitive.
		 * 
		 * Spaces are important so TimesNewRoman and Times New Roman are degarded as 2 fonts.
		 * 
		 * If you have 2 copies of arialMT.ttf in the scanned directories, the last one will be used.
		 * 
		 * 
		 * If you wish to use one of Javas fonts for display (for example, Times New Roman is a close match for myCompanyFont in the PDF, you can the
		 * code below
		 * 
		 * String[] aliases={"Times New Roman"};//,"helvetica","arial"}; decode_pdf.setSubstitutedFontAliases("myCompanyFont",aliases);
		 * 
		 * Here is is used to map Javas Times New Roman (and all weights) to TimesNewRoman.
		 * 
		 * This can also be done with the command -org.jpedal.fontmaps="TimesNewRoman=Times New Roman","font2=pdfFont1"
		 */
		// String[] nameInPDF={"TimesNewRoman"};//,"helvetica","arial"};
		// decode_pdf.setSubstitutedFontAliases("Times New Roman",nameInPDF);

		// <link><a name="imageHandler" />
		/**
		 * add in external handlers for code - 2 examples supplied
		 * 
		 * 
		 * //org.jpedal.external.ImageHandler myExampleImageHandler=new org.jpedal.examples.handlers.ExampleImageDecodeHandler();
		 * org.jpedal.external.ImageHandler myExampleImageHandler=new org.jpedal.examples.handlers.ExampleImageDrawOnScreenHandler();
		 * 
		 * decode_pdf.addExternalHandler(myExampleImageHandler, Options.ImageHandler);
		 * 
		 * 
		 * /
		 **/
		// <link><a name="customMessageOutput" />
		/**
		 * divert all message to our custom code
		 * 
		 * 
		 * CustomMessageHandler myExampleCustomMessageHandler =new ExampleCustomMessageHandler();
		 * 
		 * decode_pdf.addExternalHandler(myExampleCustomMessageHandler, Options.CustomMessageOutput);
		 * 
		 * /
		 **/
	}

	/**
	 * private boolean showMenu(String input){ //Check for disabled options
	 * 
	 * if(restrictedMenus!=null) for(int i=0; i!=restrictedMenus.length; i++)
	 * if(((String)restrictedMenus[i]).toLowerCase().equals(input.toLowerCase())) return true; return false; }/
	 **/

	/**
	 * create items on drop down menus
	 */
	protected void createSwingMenu(boolean includeAll) {
		this.currentGUI.createMainMenu(includeAll);
	}

	/** main method to run the software as standalone application */
	public static void main(String[] args) {

		/**
		 * set the look and feel for the GUI components to be the default for the system it is running on
		 */
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) {
			LogWriter.writeLog("Exception " + e + " setting look and feel");
		}

		// Viewer current;
		// String prefFile = System.getProperty("org.jpedal.Viewer.Prefs");
		// if(prefFile != null){
		// current = new Viewer(prefFile);
		// current.setupViewer();
		// }else{
		Viewer current = new Viewer();
		current.setupViewer();
		// }

		// <start-wrap>
		if (args.length > 0) {
			current.openDefaultFile(args[0]);

		}
		else
			if (current.properties.getValue("openLastDocument").toLowerCase().equals("true")) {
				if (current.properties.getRecentDocuments() != null && current.properties.getRecentDocuments().length > 1) {

					int lastPageViewed = Integer.parseInt(current.properties.getValue("lastDocumentPage"));

					if (lastPageViewed < 0) lastPageViewed = 1;

					current.openDefaultFileAtPage(current.properties.getRecentDocuments()[0], lastPageViewed);
				}

			}
		/**
		 * //<end-wrap> current.openDefaultFile(); /
		 **/
	}

	/**
	 * General code to open file at specified boomark - do not call directly
	 * 
	 * @param file
	 *            File the PDF to be decoded
	 * @param bookmark
	 *            - if not present, exception will be thrown
	 */
	private void openFile(File file, String bookmark) {

		try {

			boolean fileCanBeOpened = this.currentCommands.openUpFile(file.getCanonicalPath());

			Object bookmarkPage = null;

			int page = -1;

			// reads tree and populates lookup table
			if (this.decode_pdf.getOutlineAsXML() != null) {
				Node rootNode = this.decode_pdf.getOutlineAsXML().getFirstChild();
				if (rootNode != null) bookmarkPage = this.currentGUI.getBookmark(bookmark);

				if (bookmarkPage != null) page = Integer.parseInt((String) bookmarkPage);
			}

			// it may be a named destination ( ie bookmark=Test1)
			if (bookmarkPage == null) {
				bookmarkPage = this.decode_pdf.getIO().convertNameToRef(bookmark);

				if (bookmarkPage != null) {

					// read the object
					PdfObject namedDest = new OutlineObject((String) bookmarkPage);
					this.decode_pdf.getIO().readObject(namedDest);

					// still needed to init viewer
					if (fileCanBeOpened) this.currentCommands.processPage();

					// and generic open Dest code
					this.decode_pdf.getFormRenderer().getActionHandler().gotoDest(namedDest, ActionHandler.MOUSECLICKED, PdfDictionary.Dest);
				}
			}

			if (bookmarkPage == null) throw new PdfException("Unknown bookmark " + bookmark);

			if (page > -1) {
				this.commonValues.setCurrentPage(page);
				if (fileCanBeOpened) this.currentCommands.processPage();
			}
		}
		catch (Exception e) {
			System.err.println("Exception " + e + " processing file");

			Values.setProcessing(false);
		}
	}

	/**
	 * General code to open file at specified page - do not call directly
	 * 
	 * @param file
	 *            File the PDF to be decoded
	 * @param page
	 *            int page number to show the user
	 */
	private void openFile(File file, int page) {

		try {
			boolean fileCanBeOpened = this.currentCommands.openUpFile(file.getCanonicalPath());

			this.commonValues.setCurrentPage(page);

			if (fileCanBeOpened) this.currentCommands.processPage();
		}
		catch (Exception e) {
			System.err.println("Exception " + e + " processing file");

			Values.setProcessing(false);
		}
	}

	/**
	 * Execute Jpedal functionality from outside of the library using this method. EXAMPLES commandID = Commands.OPENFILE, args =
	 * {"/PDFData/Hand_Test/crbtrader.pdf}" commandID = Commands.OPENFILE, args = {byte[] = {0,1,1,0,1,1,1,0,0,1}, "/PDFData/Hand_Test/crbtrader.pdf}"
	 * commandID = Commands.ROTATION, args = {"90"} commandID = Commands.OPENURL, args = {"http://www.cs.bham.ac.uk/~axj/pub/papers/handy1.pdf"}
	 * 
	 * @param commandID
	 *            :: static int value from Commands to spedify which command is wanted
	 * @param args
	 *            :: arguements for the desired command
	 * 
	 */
	public Object executeCommand(int commandID, Object[] args) {

		/**
		 * far too easy to miss this step (I did!) so warn user
		 */
		if (!this.isSetup) {
			throw new RuntimeException("You must call viewer.setupViewer(); before you call any commands");
		}

		return this.currentCommands.executeCommand(commandID, args);
	}

	public SearchList getSearchResults() {
		return this.currentCommands.getSearchList();
	}

	public boolean isProcessing() {
		return Values.isProcessing();
	}

	public boolean isExecutingCommand() {
		return this.currentCommands.isExecutingCommand();
	}

	/**
	 * Allows external helper classes to be added to JPedal to alter default functionality. <br>
	 * <br>
	 * If Options.FormsActionHandler is the type then the <b>newHandler</b> should be of the form <b>org.jpedal.objects.acroforms.ActionHandler</b> <br>
	 * <br>
	 * If Options.JPedalActionHandler is the type then the <b>newHandler</b> should be of the form <b>Map</b> which contains Command Integers, mapped
	 * onto their respective <b>org.jpedal.examples.viewer.gui.swing.JPedalActionHandler</b> implementations. For example, to create a custom help
	 * action, you would add to your map, Integer(Commands.HELP) -> JPedalActionHandler. For a tutorial on creating custom actions in the Viewer, see
	 * <b>http://www.jpedal.org/support.php</b>
	 * 
	 * @param newHandler
	 * @param type
	 */
	public void addExternalHandler(Object newHandler, int type) {
		this.decode_pdf.addExternalHandler(newHandler, type);
	}

	public void dispose() {

		this.commonValues = null;

		this.currentPrinter = null;

		if (this.thumbnails != null) this.thumbnails.dispose();

		this.thumbnails = null;

		this.properties.dispose();
		this.properties = null;

		if (this.currentGUI != null) this.currentGUI.dispose();

		this.currentGUI = null;

		this.searchFrame = null;

		this.currentCommands = null;

		this.mouseHandler = null;

		this.scalingValues = null;

		// restrictedMenus=null;

		if (this.decode_pdf != null) this.decode_pdf.dispose();

		this.decode_pdf = null;

		Messages.dispose();
	}
}
