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
 * GenericFontMapper.java
 * ---------------
 */
package org.jpedal.render.output;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.jpedal.fonts.StandardFonts;
import org.jpedal.utils.LogWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.org.apache.xml.internal.serializer.OutputPropertiesFactory;

/**
 * control translation of fonts for display
 */
public class GenericFontMapper implements org.jpedal.render.output.FontMapper {

	private static final boolean STORE_UNIQUE_FONTS = false;
	private static final boolean DEBUG = false;

	private static boolean createXMLTemplate = false;

	private static final String DEFAULT_FONT = "DEFAULT_FONT";

	// weight
	String style = "normal";
	String weight = "normal";
	String family = null;

	private String fontID;

	private int fontMode = DEFAULT_ON_UNMAPPED;

	private boolean isFontEmbedded = false;

	private static Set<String> fontsInPdf = new HashSet<String>();
	private static Map<String, String> userXMLTemplate = null;

	// list of font mappings to substitute fonts
	public static Map<String, String> fontMappings = new HashMap<String, String>();
	public static Map<String, Integer> fontSizeAdjustments = new HashMap<String, Integer>();
	private static Map<String, String> fontStyle = new HashMap<String, String>();
	private static Map<String, String> fontWeight = new HashMap<String, String>();

	private static String defaultFonts = "/org/jpedal/render/output/defaultFontMap.xml";

	// original name in PDF
	private String rawFont;

	// <link><a name="fonts" />

	// setup font substitutions once
	static {

		// Mappings reference: http://www.ampsoft.net/webdesign-l/WindowsMacFonts.html
		String arialType = "Arial, Helvetica, sans-serif";
		String arialBlackType = "'Arial Black', Gadget, sans-serif";
		String comicSansType = "'Comic Sans MS', Textile, cursive";
		String courierNewType = "'Courier New', Courier, monospace";
		String georgiaType = "Georgia, 'Times New Roman', Times, serif";
		String impactType = "Impact, Charcoal, sans-serif";
		String lucidaConsoleType = "'Lucida Console', Monaco, monospace";
		String lucidaSansType = "'Lucida Sans Unicode', 'Lucida Grande', sans-serif";
		String palatinoType = "'Palatino Linotype', 'Book Antiqua', Palatino, serif";
		String tahomaType = "Tahoma, Geneva, sans-serif";
		String romanType = "'Times New Roman', Times, serif";
		String trebuchetType = "'Trebuchet MS', Helvetica, sans-serif";
		String verdanaType = "Verdana, Geneva, sans-serif";
		String symbolType = "Symbol";
		String webdingsType = "Webdings";
		String wingdingsType = "Wingdings, 'Zapf Dingbats'";
		String msSansSerifType = "'MS Sans Serif', Geneva, sans-serif";
		String msSerifType = "'MS Serif', 'New York', serif";
		String helveticaType = "Helvetica, Arial, sans-serif";

		if (fontMappings.keySet().isEmpty()) {
			// Default fonts
			fontMappings.put("Arial", arialType);
			fontMappings.put("ArialMT", arialType);
			fontMappings.put("ArialBlack", arialBlackType);
			fontMappings.put("ComicSansMS", comicSansType);
			fontMappings.put("CourierNew", courierNewType);
			fontMappings.put("Georgia", georgiaType);
			fontMappings.put("Impact", impactType);
			fontMappings.put("LucidaConsole", lucidaConsoleType);
			fontMappings.put("LucidaSansUnicode", lucidaSansType);
			fontMappings.put("PalatinoLinotype", palatinoType);
			fontMappings.put("Tahoma", tahomaType);
			fontMappings.put("TimesNewRoman", romanType);
			fontMappings.put("Times", romanType);
			fontMappings.put("Trebuchet", trebuchetType);
			fontMappings.put("Verdana", verdanaType);
			fontMappings.put("Symbol", symbolType);
			fontMappings.put("Webdings", webdingsType);
			fontMappings.put("Wingdings", wingdingsType);
			fontMappings.put("MSSansSerif", msSansSerifType);
			fontMappings.put("MSSerif", msSerifType);
			fontMappings.put("Helvetica", helveticaType);
			fontMappings.put("ZapfDingbats", wingdingsType);

			fontMappings.put(DEFAULT_FONT, romanType);
			fontSizeAdjustments.put(DEFAULT_FONT, -1); // Err on the side of caution for default fonts.
		}

		// Load default fonts if no mappings loaded
		try {
			InputStream defaultFile = GenericFontMapper.class.getResourceAsStream(defaultFonts);
			if (defaultFile != null) loadCustomFontMappings(defaultFile);
		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}
	}

	public GenericFontMapper(String rawFont) {
		init(rawFont);

		this.rawFont = rawFont;
	}

	public GenericFontMapper(String rawFont, int fontMode, boolean isFontEmbedded) {

		this.fontMode = fontMode;
		this.isFontEmbedded = isFontEmbedded;
		this.rawFont = rawFont;

		init(rawFont);
	}

	private void init(String rawFont) {

		if (this.fontMode == EMBED_ALL || this.fontMode == EMBED_ALL_EXCEPT_BASE_FAMILIES) { // Arial-Bold or Arial,bold need splitting into family
																								// and weight

			this.fontID = rawFont;

			// limit to nonn-emebedded of Standard (ie TNR, Arial)
			if (!this.isFontEmbedded || StandardFonts.isStandardFont(rawFont, true)) {

				int ptr = rawFont.indexOf(',');
				if (ptr == -1) {
					ptr = rawFont.indexOf('-');
				}

				if (ptr == -1) {

					// Split at the last number
					for (int i = (rawFont.length() - 1); i >= 0; i--) {
						int pt = rawFont.codePointAt(i);
						if (pt >= 0x30 && pt <= 0x39) {
							if (i < (rawFont.length() - 1)) {
								ptr = i - 1;
							}
							break;
						}
					}
				}

				if (ptr > 0) {
					findAttributes(rawFont);
				}
			}

			// Font exists as it is in mappings
		}
		else
			if (!directMapFont(rawFont)) {
				String fontLessAttributes = findAttributes(rawFont);

				// Does the font name minus attributes exist in mappings?
				if (!mapFont(fontLessAttributes)) {

					// If there isnt a similiar one use the default.
					if (!hasSimiliarMapping(fontLessAttributes)) {
						switch (this.fontMode) {
							case DEFAULT_ON_UNMAPPED:
								this.fontID = DEFAULT_FONT;
								break;

							case FontMapper.FAIL_ON_UNMAPPED:
								throw new RuntimeException("Font " + rawFont + " not mapped");
						}
					}
				}
			}

		if (createXMLTemplate) {
			userXMLTemplate.put(rawFont, this.fontID);
		}
	}

	/**
	 * Strip out and set font attributes returning the font name
	 * 
	 * @param rawFont
	 * @return String contains the name of the font
	 */
	private String findAttributes(String rawFont) {
		String result = rawFont;

		int ptr = rawFont.indexOf(',');

		if (ptr == -1) {
			ptr = rawFont.indexOf('-');
		}

		if (ptr == -1) {

			// Split at the last number
			for (int i = (rawFont.length() - 1); i >= 0; i--) {
				int pt = rawFont.codePointAt(i);
				if (pt >= 0x30 && pt <= 0x39) {
					if (i < (rawFont.length() - 1)) {
						ptr = i - 1;
					}
					break;
				}
			}
		}

		if (ptr != -1) {
			String fontAttributes = rawFont.substring(ptr + 1, rawFont.length()).toLowerCase();
			result = rawFont.substring(0, ptr);
			this.family = result; // font less any -, or number

			boolean isFontExists = false;

			for (String k : fontMappings.keySet()) {
				if (k.startsWith(this.family)) {
					isFontExists = true;
				}
			}

			if (isFontExists || !this.isFontEmbedded) {

				if (fontAttributes.contains("heavy")) {
					this.weight = "900";
				}
				else
					if (fontAttributes.endsWith("black")) {
						this.weight = "bolder";
					}
					else
						if (fontAttributes.contains("light")) {
							this.weight = "lighter";
						}
						else
							if (fontAttributes.contains("condensed")) {
								this.weight = "100";
							}
							else
								if (fontAttributes.contains("bold")) {
									this.weight = "bold";
								}

				/**
				 * and style
				 */
				if (fontAttributes.equals("it") || fontAttributes.contains("italic") || fontAttributes.contains("kursiv")
						|| fontAttributes.contains("oblique")) {
					this.style = "italic";
				}

			}

		}

		return result;
	}

	/**
	 * See if font is in mappings and set the font ID. Return false if its not.
	 * 
	 * @param s
	 *            String to check
	 * @return true if it maps
	 */
	private boolean mapFont(String s) {
		if (fontMappings.get(s) != null) {
			this.fontID = s;
			return true;
		}
		return false;
	}

	/**
	 * Find out if there is a direct mapping to the given font
	 */
	private boolean directMapFont(String s) {
		boolean result = mapFont(s);

		if (!result) {
			return false;
		}

		if (fontStyle.containsKey(s)) {
			this.style = fontStyle.get(s);
		}

		if (fontWeight.containsKey(s)) {
			this.weight = fontWeight.get(s);
		}

		return true;
	}

	/**
	 * Search mappings for a one that sounds close.
	 * 
	 * @param fontName
	 */
	private boolean hasSimiliarMapping(String fontName) {
		Set<String> keySet = fontMappings.keySet();
		Set<String> candidates = new HashSet<String>();

		for (String key : keySet) {
			String lcKey = key.toLowerCase();
			String lcFont = fontName.toLowerCase();

			if (lcKey.equals(lcFont)) {
				this.fontID = key;
				return true;
			}

			if (lcKey.contains(lcFont) || lcFont.contains(lcKey)) {
				candidates.add(key);
			}
		}

		if (!candidates.isEmpty()) {
			String result[] = new String[candidates.size()];
			result = candidates.toArray(result);
			this.fontID = result[0];

			// @TODO Just get the shortest one for the time being.
			if (candidates.size() > 1) {
				for (int i = 1; i < result.length; i++) {
					if (result[i].length() < this.fontID.length()) {
						this.fontID = result[i];
					}
				}
			}
			return true;
		}

		return false;
	}

	@Override
	public String getFont(boolean includeFamily) {
		String result = fontMappings.get(this.fontID);

		if (result == null && this.family != null && includeFamily) {
			result = fontMappings.get(this.family);
		}

		if (this.fontMode == EMBED_ALL && result == null) { // just pass through
			return this.rawFont;
		}
		else
			if (this.isFontEmbedded && this.fontMode == EMBED_ALL_EXCEPT_BASE_FAMILIES && result == null) {
				this.rawFont = this.rawFont.replaceAll("[.,*#]", "-");
				return this.rawFont;
			}
			else {

				// if not actually embedded and no map use raw
				if (result == null && (this.fontMode == EMBED_ALL || this.fontMode == EMBED_ALL_EXCEPT_BASE_FAMILIES)) {
					this.rawFont = this.rawFont.replaceAll("[.,*#]", "-");
					return this.rawFont;
				}
				else return (result == null) ? "" : result;
			}
	}

	@Override
	public String getStyle() {
		return this.style;
	}

	@Override
	public String getWeight() {
		return this.weight;
	}

	@Override
	public int getFontSizeAdjustment() {
		int result = fontSizeAdjustments.get(this.fontID) != null ? fontSizeAdjustments.get(this.fontID) : 0;
		return result;
	}

	/**
	 * Allow user to enter custom font mappings xml file.
	 * 
	 */
	public static void loadCustomFontMappings(InputStream xmlFile) {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(xmlFile);
			doc.getDocumentElement().normalize();

			NodeList nList = doc.getElementsByTagName("font");

			for (int i = 0; i < nList.getLength(); i++) {
				Node nNode = nList.item(i);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;

					String rawFont = getTagValue("rawFont", eElement);
					if (rawFont.length() == 0) {
						throw new RuntimeException("A font element with no name?");
					}

					String tempSize = getTagValue("sizeAdjust", eElement);
					int sizeAdjust = tempSize.length() == 0 ? 0 : Integer.valueOf(tempSize);

					String mapping = getTagValue("mappedTo", eElement);
					String style = getTagValue("style", eElement);
					String weight = getTagValue("weight", eElement);

					if (mapping.length() > 0) {
						fontMappings.put(rawFont, mapping);
					}

					if (sizeAdjust != 0) {
						fontSizeAdjustments.put(rawFont, sizeAdjust);
					}

					if (style.length() > 0) {
						fontStyle.put(rawFont, style);
					}

					if (weight.length() > 0) {
						fontWeight.put(rawFont, weight);
					}
				}
			}

		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}
	}

	/**
	 * Retrieve a value from a tagged element
	 */
	private static String getTagValue(String tag, Element eElement) {
		Node nullCheck = eElement.getElementsByTagName(tag).item(0);

		if (nullCheck == null) {
			return "";
		}

		NodeList nlList = nullCheck.getChildNodes();
		Node nValue = nlList.item(0);
		return nValue != null ? nValue.getNodeValue() : "";
	}

	/**
	 * Output a list of all the fonts found so far.
	 */
	public static void showFoundFonts() {
		for (String rawFont : fontsInPdf) {
			System.out.println(rawFont);
		}
	}

	public static void setXMLTemplate(boolean b) {
		createXMLTemplate = b;

		if (createXMLTemplate) {
			userXMLTemplate = new HashMap<String, String>();
		}
		else {
			userXMLTemplate = null;
		}
	}

	public static void createXMLTemplate(String fileName) {
		if (userXMLTemplate == null) {
			throw new AssertionError("XML Template is null.");
		}

		try {

			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document document = documentBuilder.newDocument();
			Element rootElement = document.createElement("fontMappings");
			document.appendChild(rootElement);

			for (String entry : userXMLTemplate.keySet()) {
				Element fontRoot = document.createElement("font");
				rootElement.appendChild(fontRoot);

				Element e = document.createElement("rawFont");
				fontRoot.appendChild(e);
				Node n = document.createTextNode(entry);
				e.appendChild(n);

				e = document.createElement("sizeAdjust");
				fontRoot.appendChild(e);
				Integer size = fontSizeAdjustments.get(userXMLTemplate.get(entry));
				n = document.createTextNode(size != null ? size.toString() : "");
				e.appendChild(n);

				e = document.createElement("mappedTo");
				fontRoot.appendChild(e);
				String mapping = fontMappings.get(userXMLTemplate.get(entry));
				n = document.createTextNode(mapping != null ? mapping : "");
				e.appendChild(n);

				e = document.createElement("style");
				fontRoot.appendChild(e);
				String style = fontStyle.get(userXMLTemplate.get(entry));
				n = document.createTextNode(style != null ? style : "");
				e.appendChild(n);

				e = document.createElement("weight");
				fontRoot.appendChild(e);
				String weight = fontWeight.get(userXMLTemplate.get(entry));
				n = document.createTextNode(weight != null ? weight : "");
				e.appendChild(n);
			}

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputPropertiesFactory.S_KEY_INDENT_AMOUNT, "4");
			DOMSource source = new DOMSource(document);
			StreamResult result = new StreamResult(new File(fileName));
			transformer.transform(source, result);

		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}
	}

	public static void main(String argv[]) {
	}

}
