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
 * PropertiesFile.java
 * ---------------
 */
package org.jpedal.examples.viewer.utils;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.jpedal.PdfDecoder;
import org.jpedal.gui.ShowGUIMessage;
import org.jpedal.parser.DecoderOptions;
import org.jpedal.utils.LogWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** holds values stored in XML file on disk */
public class PropertiesFile {

	private String separator = System.getProperty("file.separator");
	private String userDir = System.getProperty("user.dir");
	private String configFile = this.userDir + this.separator + ".properties.xml";
	private InputStream configInputStream = null;

	private boolean isTest = false;

	private boolean refactorProperties = false;

	private boolean isReadOnly = false;

	public boolean isReadOnly() {
		return this.isReadOnly;
	}

	private Document doc;

	private int noOfRecentDocs = 6;

	private String[] properties = {

	"showfirsttimepopup", "true", /**/
	"daysLeft", "", "showrhinomessage", "false", "showsaveformsmessage", "true", "showitextmessage", "true", "showddmessage", "true",
			"searchWindowType", "2", "borderType", "1", "useHiResPrinting", "true", "showDownloadWindow", "true", "resolution", "110",
			"allowCursorToChange", "true", "autoScroll", "true", "pageMode", "1", "displaytipsonstartup", "false", "automaticupdate", "true",
			"currentversion", PdfDecoder.version, "showtiffmessage", "true", "maxmultiviewers", "20", "MenuBarMenu", "true", "FileMenu", "true",
			"OpenMenu", "true", "Open", "true", "Openurl", "true", "ENDCHILDREN", "Save", "true", "Resaveasforms", "true", "Find", "true",
			"Documentproperties", "true", "Signpdf", "true", "Print", "true", "Recentdocuments", "true", "Exit", "true", "ENDCHILDREN", "EditMenu",
			"true", "Copy", "true", "Selectall", "true", "Deselectall", "true", "Preferences", "true", "ENDCHILDREN", "ViewMenu", "true", "GotoMenu",
			"true", "Firstpage", "true", "Backpage", "true", "Forwardpage", "true", "Lastpage", "true", "Goto", "true", "Previousdocument", "true",
			"Nextdocument", "true", "ENDCHILDREN", "PagelayoutMenu", "true", "Single", "true", "Continuous", "true", "Facing", "true",
			"Continuousfacing", "true", "PageFlow", "true", "ENDCHILDREN", "separateCover", "true", "textSelect", "true", "panMode", "true",
			"Fullscreen", "true", "ENDCHILDREN", "WindowMenu", "true", "Cascade", "true", "Tile", "true", "ENDCHILDREN", "ExportMenu", "true",
			"PdfMenu", "true", "Oneperpage", "true", "Nup", "true", "Handouts", "true", "ENDCHILDREN", "ContentMenu", "true", "Images", "true",
			"Text", "true", "ENDCHILDREN", "Bitmap", "true", "ENDCHILDREN", "PagetoolsMenu", "true", "Rotatepages", "true", "Deletepages", "true",
			"Addpage", "true", "Addheaderfooter", "true", "Stamptext", "true", "Stampimage", "false", "Crop", "true", "ENDCHILDREN", "HelpMenu",
			"true", "Visitwebsite", "true", "Tipoftheday", "true", "Checkupdates", "true", "About",
			"true",
			"ENDCHILDREN",
			"ENDCHILDREN",
			"ButtonsMenu",
			"true",
			"Openfilebutton",
			"true",
			"Printbutton",
			"true",
			"Searchbutton",
			"true",
			"Propertiesbutton",
			"false",
			// <start-wrap>
			"Aboutbutton", "false",
			/**
			 * //<end-wrap> "Aboutbutton", "true", /
			 **/
			"Snapshotbutton", "true", "CursorButton", "true", "MouseModeButton", "true", "ENDCHILDREN", "DisplayOptionsMenu", "true",
			"Scalingdisplay", "true", "Rotationdisplay", "true", "Imageopdisplay", "false", "Progressdisplay", "true", "Downloadprogressdisplay",
			"true", "ENDCHILDREN", "NavigationBarMenu", "true", "Memorybottom", "true", "Firstbottom", "true", "Back10bottom", "true", "Backbottom",
			"true", "Gotobottom", "true", "Forwardbottom", "true", "Forward10bottom", "true", "Lastbottom", "true", "Singlebottom", "true",
			"Continuousbottom", "true", "Continuousfacingbottom", "true", "Facingbottom", "true", "PageFlowbottom", "true", "ENDCHILDREN",
			"SideTabBarMenu", "true", "Pagetab", "true", "Bookmarkstab", "true", "Layerstab", "true", "Signaturestab", "true", "ENDCHILDREN",
			"ShowMenubar", "true", "ShowButtons", "true",
			"ShowDisplayoptions",
			"true",
			"ShowNavigationbar",
			"true",
			"ShowSidetabbar",
			"true",
			// "ENDCHILDREN",
			"highlightBoxColor", "-16777216", "highlightTextColor", "16750900", "replaceDocumentTextColors", "false", "vfgColor", "0", "vbgColor",
			"16777215", "TextColorThreshold", "255", "replacePdfDisplayBackground", "false", "pdfDisplayBackground", "16777215",
			"changeTextAndLineart", "false", "sbbgColor", "16777215", "highlightComposite", "0.35", "invertHighlights", "false", "openLastDocument",
			"false", "lastDocumentPage", "1", "pageInsets", "25", "sideTabBarCollapseLength",
			"30",
			"consistentTabBar",
			"false",
			"allowRightClick",
			"true",
			"allowScrollwheelZoom",
			"true",
			// <start-wrap>
			/**
			 * //<end--wrap> "readOnly","true", /
			 **/
			// <start-wrap>
			"readOnly",
			"false",
			// <end-wrap>
			"enhancedViewerMode", "true", "enhancedFacingMode", "true", "windowTitle", "", "confirmClose", "false", "iconLocation",
			"/org/jpedal/examples/simpleviewer/res/", "enhancedGUI", "true", "showpageflowmessage", "true", "defaultPrinter", "", "debugPrinter",
			"false", "defaultDPI", "600", "defaultPagesize", "", "printerBlacklist", "", "useHinting", "false", "voice", "kevin16(general domain)",
			"previewOnSingleScroll", "true", "showMouseSelectionBox", "false", "separateCoverOn", "true" };

	public PropertiesFile() {
		// <start-wrap>
		try {
			String jarLoc = getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
			this.userDir = jarLoc.substring(0, jarLoc.lastIndexOf('/'));
			this.configFile = this.userDir + this.separator + ".properties.xml";
			if (DecoderOptions.isRunningOnWindows) {
				if (this.configFile.length() > 1) {
					this.configFile = this.configFile.substring(1);
					this.configFile = this.configFile.replaceAll("\\\\", "/");
				}
			}
		}
		catch (Exception e) {
			this.userDir = System.getProperty("user.dir");
			this.configFile = this.userDir + this.separator + ".properties.xml";
		}
		/**
		 * //<end-wrap> configFile="jar:org/jpedal/examples/simpleviewer/res/Default.xml"; /
		 **/
	}

	public PropertiesFile(String config) {
		this.configFile = config;
	}

	/**
	 * Please use loadProperties()
	 * 
	 * @deprecated
	 */
	@Deprecated
	public void setupProperties() {
		loadProperties();
	}

	public void loadProperties() {

		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			File config = null;

			if (this.configInputStream != null) {
				try {
					this.doc = db.parse(this.configInputStream);
					this.isReadOnly = true;
				}
				catch (Exception e) {
					this.doc = db.newDocument();
				}
			}
			else {
				config = new File(this.configFile);

				if (config.exists() && config.length() > 0) {
					try {
						this.doc = db.parse(config);
					}
					catch (Exception e) {
						this.doc = db.newDocument();
					}
				}
				else this.doc = db.newDocument();
			}
			if (this.configInputStream == null && (config != null && (config.canWrite() || (!config.exists() && !config.canWrite())))
					&& !getValue("readOnly").toLowerCase().equals("true")) {
				this.isReadOnly = false;
				boolean hasAllElements = checkAllElementsPresent();

				// If properties is an old version or we are missing elements
				// add missing / reload properties file
				if (this.refactorProperties || !hasAllElements) {
					// Reset to start of properties file
					this.position = 0;

					// Delete old config file
					config.delete();
					// config.createNewFile();

					Document oldDoc = (Document) this.doc.cloneNode(true);
					this.doc = db.newDocument();
					if (!this.isReadOnly && !getValue("readOnly").toLowerCase().equals("true")) {
						this.isReadOnly = false;
					}
					else {
						this.isReadOnly = true;
					}
					checkAllElementsPresent();

					/**
					 * Move RecentFiles List Over to new properties
					 */
					// New Properties
					NodeList newRecentFiles = this.doc.getElementsByTagName("recentfiles");
					Element newRecentRoot = (Element) newRecentFiles.item(0);

					// Old Properties
					NodeList oldRecentFiles = oldDoc.getElementsByTagName("recentfiles");
					Element oldRecentRoot = (Element) oldRecentFiles.item(0);

					// Get children elements
					NodeList children = oldRecentRoot.getChildNodes();
					for (int i = 0; i != children.getLength(); i++) {
						if (!children.item(i).getNodeName().equals("#text")) {// Ignore this element
							Element e = this.doc.createElement("file");
							e.setAttribute("name", ((Element) children.item(i)).getAttribute("name"));
							newRecentRoot.appendChild(e);
						}
					}

					for (int i = 0; i != this.properties.length; i++) {
						if (!this.properties[i].equals("ENDCHILDREN")) {
							NodeList nl = this.doc.getElementsByTagName(this.properties[i]);
							Element element = (Element) nl.item(0);
							if (element == null) {
								ShowGUIMessage.showGUIMessage("The property " + this.properties[i] + " was either not found in the properties file.",
										"Property not found.");
							}
							else {
								NodeList l = oldDoc.getElementsByTagName(this.properties[i]);
								Element el = (Element) l.item(0);
								if (el != null) element.setAttribute("value", el.getAttribute("value"));
							}
							i++;
						}
					}

					if (!this.isTest) writeDoc();
				}

				// Check for invalid color options (possible mistake in properties file)
				String v1 = getValue("vfgColor");
				String v2 = getValue("vbgColor");
				String v3 = getValue("sbbgColor");

				if (v1.length() > 0 && v1.length() > 0 && v1.length() > 0) {
					int value = Integer.parseInt(v1) + Integer.parseInt(v2) + Integer.parseInt(v3);

					if (value == -3) {
						// 3 null values, replace with default values
						setValue("vfgColor", "");
						setValue("vbgColor", "16777215");
						setValue("sbbgColor", "16777215");
					}
				}
				// //only write out if needed
				// if(!hasAllElements)
				// writeDoc();

			}
			else {
				this.isReadOnly = true;
			}
		}
		catch (Exception e) {
			LogWriter.writeLog("Exception " + e + " generating properties file");
		}

		// <start-wrap>
		/**
		 * //<end-wrap> isReadOnly= true; /
		 **/
	}

	public void removeRecentDocuments() {
		NodeList nl = this.doc.getElementsByTagName("recentfiles");

		if (nl != null && nl.getLength() > 0) {
			NodeList allRecentDocs = ((Element) nl.item(0)).getElementsByTagName("*");

			for (int i = 0; i < allRecentDocs.getLength(); i++) {
				Node item = allRecentDocs.item(i);
				nl.item(0).removeChild(item);
			}
		}
	}

	public String[] getRecentDocuments() {
		String[] recentDocuments;

		try {
			NodeList nl = this.doc.getElementsByTagName("recentfiles");
			List<String> fileNames = new ArrayList<String>();

			if (nl != null && nl.getLength() > 0) {
				NodeList allRecentDocs = ((Element) nl.item(0)).getElementsByTagName("*");

				for (int i = 0; i < allRecentDocs.getLength(); i++) {
					Node item = allRecentDocs.item(i);
					NamedNodeMap attrs = item.getAttributes();
					fileNames.add(attrs.getNamedItem("name").getNodeValue());
				}
			}

			// prune unwanted entries
			while (fileNames.size() > this.noOfRecentDocs) {
				fileNames.remove(0);
			}

			Collections.reverse(fileNames);

			recentDocuments = fileNames.toArray(new String[this.noOfRecentDocs]);
		}
		catch (Exception e) {
			LogWriter.writeLog("Exception " + e + " getting recent documents");
			return null;
		}

		return recentDocuments;
	}

	public void addRecentDocument(String file) {
		try {
			Element recentElement = (Element) this.doc.getElementsByTagName("recentfiles").item(0);

			checkExists(file, recentElement);

			Element elementToAdd = this.doc.createElement("file");
			elementToAdd.setAttribute("name", file);

			recentElement.appendChild(elementToAdd);

			removeOldFiles(recentElement);

			// writeDoc();
		}
		catch (Exception e) {
			LogWriter.writeLog("Exception " + e + " adding recent document to properties file");
		}
	}

	public void setValue(String elementName, String newValue) {

		try {
			NodeList nl = this.doc.getElementsByTagName(elementName);
			Element element = (Element) nl.item(0);
			if (element == null || newValue == null) {
				ShowGUIMessage.showGUIMessage("The property " + elementName + " was either not found in the properties file or the value " + newValue
						+ " was not set.", "Property not found.");
			}
			else {
				element.setAttribute("value", newValue);
			}

			writeDoc();
		}
		catch (Exception e) {
			LogWriter.writeLog("Exception " + e + " setting value in properties file");
		}
	}

	public String getGUIValue(String elementName) {
		String menu = getValue(elementName);
		if (menu.length() == 0) return "false";
		else return menu.toLowerCase();
	}

	public NodeList getChildren(String item) {
		return this.doc.getElementsByTagName(item).item(0).getChildNodes();
	}

	public String getValue(String elementName) {
		NamedNodeMap attrs = null;
		try {
			NodeList nl = this.doc.getElementsByTagName(elementName);
			Element element = (Element) nl.item(0);
			if (element == null) return "";
			attrs = element.getAttributes();

		}
		catch (Exception e) {
			LogWriter.writeLog("Exception " + e + " generating properties file");
			return "";
		}

		return attrs.getNamedItem("value").getNodeValue();
	}

	private void removeOldFiles(Element recentElement) throws Exception {
		NodeList allRecentDocs = recentElement.getElementsByTagName("*");

		while (allRecentDocs.getLength() > this.noOfRecentDocs) {
			recentElement.removeChild(allRecentDocs.item(0));
		}
	}

	private static void checkExists(String file, Element recentElement) throws Exception {
		NodeList allRecentDocs = recentElement.getElementsByTagName("*");

		for (int i = 0; i < allRecentDocs.getLength(); i++) {
			Node item = allRecentDocs.item(i);
			NamedNodeMap attrs = item.getAttributes();
			String value = attrs.getNamedItem("name").getNodeValue();

			if (value.equals(file)) recentElement.removeChild(item);
		}
	}

	//
	public void writeDoc() throws Exception {

		if (!this.isReadOnly && !getValue("readOnly").toLowerCase().equals("true")) {
			InputStream stylesheet = this.getClass().getResourceAsStream("/org/jpedal/examples/viewer/res/xmlstyle.xslt");

			StreamResult str = new StreamResult(this.configFile);
			StreamSource ss = new StreamSource(stylesheet);
			DOMSource dom = new DOMSource(this.doc);
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer(ss);
			transformer.transform(dom, str);

			transformer = null;
			stylesheet.close();
			if (ss != null) ss.getInputStream().close();
			// if(str!=null)
			// str.getOutputStream().close();
			ss = null;
			str = null;
			dom = null;
		}
	}

	public void dispose() {

		this.doc = null;
		this.properties = null;
		this.configFile = null;
	}

	private boolean checkAllElementsPresent() throws Exception {

		// assume true and set to false if wrong
		boolean hasAllElements = true;

		NodeList allElements = this.doc.getElementsByTagName("*");
		List<String> elementsInTree = new ArrayList<String>(allElements.getLength());

		for (int i = 0; i < allElements.getLength(); i++)
			elementsInTree.add(allElements.item(i).getNodeName());

		Element propertiesElement = null;

		if (elementsInTree.contains("properties")) {
			propertiesElement = (Element) this.doc.getElementsByTagName("properties").item(0);
		}
		else {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();

			this.doc = db.newDocument();

			propertiesElement = this.doc.createElement("properties");
			this.doc.appendChild(propertiesElement);

			allElements = this.doc.getElementsByTagName("*");
			elementsInTree = new ArrayList<String>(allElements.getLength());

			for (int i = 0; i < allElements.getLength(); i++)
				elementsInTree.add(allElements.item(i).getNodeName());

			hasAllElements = false;
		}

		if (!elementsInTree.contains("recentfiles")) {
			Element recent = this.doc.createElement("recentfiles");
			propertiesElement.appendChild(recent);

			hasAllElements = false;
		}

		hasAllElements = addProperties(elementsInTree, propertiesElement);

		return hasAllElements;
	}

	// Keep track of position in the properties array
	int position = 0;

	private boolean addMenuElement(List<String> tree, Element menu) {
		boolean hasAllElements = true;

		// System.out.println("MENU == "+properties[position]);

		if (!tree.contains(this.properties[this.position])) {
			Element property = this.doc.createElement(this.properties[this.position]);

			// Increment to property value
			this.position++;

			property.setAttribute("value", this.properties[this.position]);
			menu.appendChild(property);

			// update position in array
			this.position++;

			// Start on children of menu
			addProperties(tree, property);

			hasAllElements = false;
		}
		else { // Increment passed value to next property
			Element property = (Element) this.doc.getElementsByTagName(this.properties[this.position]).item(0);
			this.position++;
			this.position++;
			addProperties(tree, property);
		}

		return hasAllElements;
	}

	private boolean addChildElements(List<String> tree, Element menu) {
		boolean hasAllElements = true;

		// System.out.println("Child == "+properties[position]);
		// Not at the end of the children so keep adding
		if (!this.properties[this.position].equals("ENDCHILDREN")) {
			if (!tree.contains(this.properties[this.position])) {
				// if(properties[i].equals("currentversion")){
				// refactorProperties = true;
				// }

				Element property = this.doc.createElement(this.properties[this.position]);

				// Increment to property value
				this.position++;

				property.setAttribute("value", this.properties[this.position]);
				menu.appendChild(property);

				hasAllElements = false;
			}
			else {

				// Check version number for refactoring and updating
				if (this.properties[this.position].equals("currentversion")) {

					// Get store value for the current version
					NodeList nl = this.doc.getElementsByTagName(this.properties[this.position]);
					Element element = (Element) nl.item(0);

					if (element == null) {
						// Element not found in tree, should never happen
						ShowGUIMessage.showGUIMessage("The property " + this.properties[this.position]
								+ " was either not found in the properties file.", "Property not found.");
					}
					else {

						// Is it running in the IDE
						if (this.properties[this.position + 1].equals("4.92b23")) {
							// Do nothing as we are in the IDE
							// Refactor for testing purposes
							// refactorProperties = true;
							// isTest=true;

						}
						else {// Check versions of released jar

							// Program Version
							float progVersion = Float.parseFloat(PdfDecoder.version.substring(0, 4));

							// Ensure properties is a valid value
							String propVer = "0";
							String version = element.getAttribute("value");
							if (version.length() > 3) propVer = version.substring(0, 4);

							// Properties Version
							float propVersion = Float.parseFloat(propVer);

							// compare version, only update on newer version
							if (progVersion > propVersion) {
								element.setAttribute("value", PdfDecoder.version);
								this.refactorProperties = true;
							}
						}
					}
				}

				// Increment passed value to next property
				this.position++;
			}
		}
		else {
			this.endMenu = true;
		}
		this.position++;
		return hasAllElements;
	}

	private boolean endMenu = false;

	private boolean addProperties(List<String> tree, Element menu) {
		boolean hasAllElements = true;

		while (this.position < this.properties.length) {
			boolean value = true;
			// Add menu to properties
			if (this.properties[this.position].endsWith("Menu")) {
				value = addMenuElement(tree, menu);
			}
			else {
				value = addChildElements(tree, menu);
				if (this.endMenu) {
					this.endMenu = false;
					return hasAllElements;
				}
			}

			if (!value) hasAllElements = false;
		}
		return hasAllElements;
	}

	public int getNoRecentDocumentsToDisplay() {
		return this.noOfRecentDocs;
	}

	public String getConfigFile() {
		return this.configFile;
	}

	/**
	 * Please use loadProperties(String configFile)
	 * 
	 * @param configFile
	 * @deprecated
	 */
	@Deprecated
	public void setConfigFile(String configFile) {
		loadProperties(configFile);
	}

	public void loadProperties(InputStream is) {

		this.configInputStream = is;
		// if(is!=null){
		// InputStreamReader isr = new InputStreamReader(is);
		// BufferedReader br = new BufferedReader(isr);
		// try {
		// File con = File.createTempFile("config", ".xml",new File(ObjectStore.temp_dir));
		// FileOutputStream fos = new FileOutputStream(con);
		// OutputStreamWriter osw=new OutputStreamWriter(fos); // read bytes and thus output bytes
		// while(br.ready()){
		// osw.write(br.read());
		// }
		//
		// //Close input streams and reader
		// is.close();
		// br.close();
		// isr.close();
		//
		// //Flush the output writers
		// osw.flush();
		// fos.flush();
		//
		// //Close the output writers
		// osw.close();
		// fos.close();
		//
		// this.configFile = con.getAbsolutePath();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		// }else{
		// throw new RuntimeException("unable to open resource stream for "+configFile);
		// }

		// <start-wrap>
		/**
		 * //<end-wrap> isReadOnly= true; /
		 **/

		loadProperties();
	}

	public void loadProperties(String configFile) {

		if (configFile.startsWith("jar:")) {
			configFile = configFile.substring(4);

			InputStream is = this.getClass().getResourceAsStream(configFile);

			if (is != null) {
				this.configInputStream = is;
				// InputStreamReader isr = new InputStreamReader(is);
				// BufferedReader br = new BufferedReader(isr);
				// try {
				// File con = File.createTempFile("config", ".xml",new File(ObjectStore.temp_dir));
				// FileOutputStream fos = new FileOutputStream(con);
				// OutputStreamWriter osw=new OutputStreamWriter(fos); //read bytes and thus output bytes
				// while(br.ready()){
				// osw.write(br.read());
				// }
				//
				// //Close input streams and reader
				// is.close();
				// br.close();
				// isr.close();
				//
				// //Flush the output writers
				// osw.flush();
				// fos.flush();
				//
				// //Close the output writers
				// osw.close();
				// fos.close();
				//
				// this.configFile = con.getAbsolutePath();
				// } catch (IOException e) {
				// e.printStackTrace();
				// }
			}
			else {
				throw new RuntimeException("unable to open resource stream for " + configFile);
			}
		}
		else {
			if (this.configInputStream == null) {
				File p = new File(configFile);
				if (p.exists() || (!p.exists() && !p.canWrite())) {
					this.configFile = configFile;
				}
				else {
					throw new RuntimeException();
				}
				if (p.canWrite()) {
					this.isReadOnly = false;
				}
				else {
					this.isReadOnly = true;
				}
			}
		}

		// <start-wrap>
		/**
		 * //<end-wrap> isReadOnly= true; /
		 **/

		loadProperties();
	}
}
