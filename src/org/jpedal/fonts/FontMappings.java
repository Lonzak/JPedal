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
 * FontMappings.java
 * ---------------
 */
package org.jpedal.fonts;

import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.jpedal.PdfDecoder;
import org.jpedal.exception.PdfFontException;
import org.jpedal.parser.DecoderOptions;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.Strip;

/**
 * Holds Maps which are used to map font names onto actual fonts and files
 */
public class FontMappings {

	/** ensure fonts setup only once */
	public static boolean fontsInitialised = false;

	/**
	 * font to use in preference to Lucida
	 */
	public static String defaultFont = null;

	/**
	 * flag to show if there must be a mapping value (program exits if none found)
	 */
	public static boolean enforceFontSubstitution = false;

	/**
	 * used to remap fonts onto truetype fonts (set internally)
	 */
	public static Map<String, String> fontSubstitutionTable = null;

	/**
	 * hold details of all fonts
	 */
	public static Map fontPropertiesTable = null;

	/**
	 * used to ensure substituted fonts unique
	 */
	public static Map<String, String> fontPossDuplicates = null;

	/**
	 * used to store number for subfonts in TTC
	 */
	public static Map<String, Integer> fontSubstitutionFontID = null;

	/**
	 * used to remap fonts onto truetype fonts (set internally)
	 */
	public static Map<String, String> fontSubstitutionLocation = new HashMap<String, String>();

	/**
	 * used to remap fonts onto truetype fonts (set internally)
	 */
	public static Map<String, String> fontSubstitutionAliasTable = new HashMap<String, String>();

	/** only upload all fonts once */
	private static boolean fontsSet = false;

	final private static String separator = System.getProperty("file.separator");

	/**
	 * determine how font substitution is done
	 */
	private static int fontSubstitutionMode = PdfDecoder.SUBSTITUTE_FONT_USING_FILE_NAME;

	// private static int fontSubstitutionMode=PdfDecoder.SUBSTITUTE_FONT_USING_POSTSCRIPT_NAME;
	// private static int fontSubstitutionMode=PdfDecoder.SUBSTITUTE_FONT_USING_FULL_FONT_NAME;
	// private static int fontSubstitutionMode=PdfDecoder.SUBSTITUTE_FONT_USING_FAMILY_NAME;
	// private static int fontSubstitutionMode=PdfDecoder.SUBSTITUTE_FONT_USING_POSTSCRIPT_NAME_USE_FAMILY_NAME_IF_DUPLICATES;

	private FontMappings() {}

	public static void initFonts() {

		// pick up D options and use settings

		try {
			String fontMaps = System.getProperty("org.jpedal.fontmaps");

			if (fontMaps != null) {
				StringTokenizer fontPaths = new StringTokenizer(fontMaps, ",");

				while (fontPaths.hasMoreTokens()) {

					String fontPath = fontPaths.nextToken();
					StringTokenizer values = new StringTokenizer(fontPath, "=:");

					int count = values.countTokens() - 1;
					String nameInPDF[] = new String[count];
					String key = values.nextToken();
					for (int i = 0; i < count; i++)
						nameInPDF[i] = values.nextToken();

					setSubstitutedFontAliases(key, nameInPDF); //$NON-NLS-1$

				}
			}

		}
		catch (Exception e) {
			if (LogWriter.isOutput()) LogWriter.writeLog("Unable to read org.jpedal.fontmaps " + e.getMessage());
		}

		// pick up D options and use settings

		try {
			String fontDirs = System.getProperty("org.jpedal.fontdirs");
			String failed = null;
			if (fontDirs != null) failed = FontMappings.addFonts(fontDirs, failed);
			if (failed != null && LogWriter.isOutput()) LogWriter.writeLog("Could not find " + failed);
		}
		catch (Exception e) {
			if (LogWriter.isOutput()) LogWriter.writeLog("Unable to read FontDirs " + e.getMessage());
		}
	}

	/**
	 * set mode to use when substituting fonts (default is to use Filename (ie arial.ttf) Options are SUBSTITUTE_* values from PdfDecoder
	 */
	public static void setFontSubstitutionMode(int mode) {
		fontSubstitutionMode = mode;
	}

	/**
	 * set mode to use when substituting fonts (default is to use Filename (ie arial.ttf) Options are SUBSTITUTE_* values from PdfDecoder
	 */
	public static int getFontSubstitutionMode() {
		return fontSubstitutionMode;
	}

	/**
	 * allows a number of fonts to be mapped onto an actual font and provides a way around slightly differing font naming when substituting fonts - So
	 * if arialMT existed on the target machine and the PDF contained arial and helvetica (which you wished to replace with arialmt), you would use
	 * the following code -
	 * <p/>
	 * String[] aliases={"arial","helvetica"}; currentPdfDecoder.setSubstitutedFontAliases("arialmt",aliases); -
	 * <p/>
	 * comparison is case-insensitive and file type/ending should not be included - For use in conjunction with -Dorg.jpedal.fontdirs options which
	 * allows user to pass a set of comma separated directories with Truetype fonts (directories do not need to exist so can be multi-platform
	 * setting)
	 */
	public static void setSubstitutedFontAliases(String fontFileName, String[] aliases) {

		if (aliases != null) {

			String name = fontFileName.toLowerCase(), alias;
			for (String aliase : aliases) {
				alias = aliase.toLowerCase();
				if (!alias.equals(name)) fontSubstitutionAliasTable.put(alias, name);
			}
		}
	}

	/**
	 * takes a comma separated list of font directories and add to substitution
	 */
	public static String addFonts(String fontDirs, String failed) {

		StringTokenizer fontPaths = new StringTokenizer(fontDirs, ",");

		while (fontPaths.hasMoreTokens()) {

			String fontPath = fontPaths.nextToken();

			if (!fontPath.endsWith("/") & !fontPath.endsWith("\\")) fontPath = fontPath + separator;

			// LogWriter.writeLog("Looking in " + fontPath + " for TT fonts");

			addTTDir(fontPath, failed);
		}

		return failed;
	}

	public static void dispose() {

		fontSubstitutionTable = null;
		fontPropertiesTable = null;
		fontPossDuplicates = null;
		fontSubstitutionFontID = null;
		fontSubstitutionLocation = null;
		fontSubstitutionAliasTable = null;
	}

	/**
	 * add a truetype font directory and contents to substitution
	 */
	public static String addTTDir(String fontPath, String failed) {

		if (fontSubstitutionTable == null) {
			fontSubstitutionTable = new HashMap<String, String>();
			fontSubstitutionFontID = new HashMap<String, Integer>();
			fontPossDuplicates = new HashMap<String, String>();
			fontPropertiesTable = new HashMap();
		}

		File currentDir = new File(fontPath);

		if ((currentDir.exists()) && (currentDir.isDirectory())) {

			String[] files = currentDir.list();

			if (files != null) {

				for (String currentFont : files) {
					addFontFile(currentFont, fontPath);

				}
			}
		}
		else {
			if (failed == null) {
				failed = fontPath;
			}
			else {
				failed = failed + ',' + fontPath;
			}
		}

		return failed;
	}

	public static void setFontReplacements() {

		// this is where we setup specific font mapping to use fonts on local machines
		// note different settigns for Win, linux, MAC

		// general
		String[] aliases6 = { /** "AcArial"};//,/ **/
		"acarialunicodems__cn" };// ,"acarial,bold"};
		setSubstitutedFontAliases("adobeheitistd-regular", aliases6);

		// platform settings
		if (DecoderOptions.isRunningOnMac) {

			// Courier (CourierNew) both on Mac and different
			setSubstitutedFontAliases("Courier italic", new String[] { "Courier-Oblique" });
			setSubstitutedFontAliases("Courier bold", new String[] { "Courier-Bold" });
			setSubstitutedFontAliases("Courier bold italic", new String[] { "Courier-BoldOblique" });

			setSubstitutedFontAliases("Courier new italic", new String[] { "CourierNew,italic", "CourierStd-Oblique" });
			setSubstitutedFontAliases("Courier new bold", new String[] { "CourierNew,Bold", "Courier-Bold", "CourierStd-Bold" });
			setSubstitutedFontAliases("Courier new bold italic", new String[] { "CourierNew-BoldOblique", "CourierStd-BoldOblique" });
			setSubstitutedFontAliases("Courier new", new String[] { "CourierNew", "Courier", "CourierStd" });

			// Helvetica (Arial)
			setSubstitutedFontAliases("arial", new String[] { "Helvetica", "arialmt" });
			setSubstitutedFontAliases("arial italic", new String[] { "arial-italic", "arial-italicmt", "Helvetica-Oblique", "Arial,Italic" });
			setSubstitutedFontAliases("arial bold", new String[] { "arial-boldmt,bold", "arial-boldmt", "Helvetica-Bold", "Arial,bold" });
			setSubstitutedFontAliases("arial bold italic", new String[] { "Arial-BoldItalicMT", "Helvetica-BoldOblique" });

			// Arial Narrow - not actually one of fonts but very common so added
			setSubstitutedFontAliases("arial Narrow", new String[] { "ArialNarrow", }); // called ArialNarrow in PDF, needs to be arialn for Windows
			setSubstitutedFontAliases("arial Narrow italic", new String[] { "ArialNarrow-italic" });
			setSubstitutedFontAliases("arial Narrow bold", new String[] { "ArialNarrow-bold", "ArialNarrow,Bold" });
			setSubstitutedFontAliases("arial Narrow bold italic", new String[] { "ArialNarrow-bolditalic" });

			// Times/TimesNewRoman
			setSubstitutedFontAliases("times new roman bold", new String[] { "Times-Bold", "TimesNewRoman,Bold", "TimesNewRomanPS-BoldMT" });
			setSubstitutedFontAliases("times new roman bold italic", new String[] { "Times-BoldItalic", "TimesNewRoman,BoldItalic",
					"TimesNewRomanPS-BoldItalicMT" });
			setSubstitutedFontAliases("times new roman italic", new String[] { "Times-Italic", "TimesNewRoman,Italic", "TimesNewRomanPS-ItalicMT" });
			setSubstitutedFontAliases("times new roman", new String[] { "Times-Roman", "TimesNewRoman", "Times", "TimesNewRomanPSMT" });

			setSubstitutedFontAliases("wingdings", new String[] { "ZapfDingbats", "ZaDb" });

			// default at present for others as well
		}
		else {// if(DecoderOptions.isRunningOnWindows){

			// Courier (CourierNew)
			setSubstitutedFontAliases("Couri", new String[] { "Courier-Oblique", "CourierNew,italic", "CourierStd-Oblique" });
			setSubstitutedFontAliases("Courbd", new String[] { "Courier-Bold", "CourierNew,Bold", "CourierStd-Bold" });
			setSubstitutedFontAliases("Courbi", new String[] { "Courier-BoldOblique", "CourierNew-BoldOblique", "CourierStd-BoldOblique" });
			setSubstitutedFontAliases("Cour", new String[] { "CourierNew", "Courier", "CourierStd", "CourierNewPSMT" });

			// Helvetica (Arial)
			setSubstitutedFontAliases("arial", new String[] { "Helvetica", "arialmt" });
			setSubstitutedFontAliases("ariali", new String[] { "arial-italic", "arial-italicmt", "Helvetica-Oblique", "Arial,Italic" });
			setSubstitutedFontAliases("arialbd", new String[] { "arial-boldmt,bold", "arial-boldmt", "Helvetica-Bold", "Arial,bold", "arial bold" });
			setSubstitutedFontAliases("arialbdi", new String[] { "Arial-BoldItalicMT", "Helvetica-BoldOblique" });

			// Arial Narrow - not actually one of fonts but very common so added
			setSubstitutedFontAliases("arialn", new String[] { "ArialNarrow", }); // called ArialNarrow in PDF, needs to be arialn for Windows
			setSubstitutedFontAliases("arialni", new String[] { "ArialNarrow-italic" });
			setSubstitutedFontAliases("arialnb", new String[] { "ArialNarrow-bold", "ArialNarrow,Bold" });
			setSubstitutedFontAliases("arialnbi", new String[] { "ArialNarrow-bolditalic" });

			// Times/TimesNewRoman
			setSubstitutedFontAliases("timesbd", new String[] { "Times-Bold", "TimesNewRoman,Bold", "TimesNewRomanPS-BoldMT" });
			setSubstitutedFontAliases("timesi", new String[] { "Times-BoldItalic", "TimesNewRoman,BoldItalic" });
			setSubstitutedFontAliases("timesbi", new String[] { "Times-Italic", "TimesNewRoman,Italic" });
			setSubstitutedFontAliases("times", new String[] { "Times-Roman", "TimesNewRoman", "Times", "TimesNewRomanPSMT" });

			setSubstitutedFontAliases("wingdings", new String[] { "ZapfDingbats", "ZaDb" });

		}

		setSubstitutedFontAliases("AdobeSongStd-Light", new String[] { "STSong-Light" });
		// setSubstitutedFontAliases("AdobeSongStd-Light",new String[] {"MHei-Medium"});
		// setSubstitutedFontAliases("AdobeSongStd-Light",new String[] {"MSung-Light"});
		// setSubstitutedFontAliases("AdobeSongStd-Light",new String[] {"HeiseiKakuGo-W5"});
		// setSubstitutedFontAliases("AdobeSongStd-Light",new String[] {"HeiseiMin-W3"});
		// setSubstitutedFontAliases("AdobeSongStd-Light",new String[] {"HYGoThic-Medium"});
		// setSubstitutedFontAliases("AdobeSongStd-Light",new String[] {"HYSMyeongJo-Medium"});

		// set general mappings for non-embedded fonts (assumes names the same) - do first time used
		if (!fontsSet) {

			fontsSet = true;
			setFontDirs(new String[] { "C:/windows/fonts/", "C:/winNT/fonts/", "/Library/Fonts/",
					// "/System/Library/Fonts/", ms 20111013 commented out as breaks forms with Zapf
					"/usr/share/fonts/truetype/msttcorefonts/",
			// "/usr/share/fonts/truetype/",
			// "/windows/D/Windows/Fonts/"
			});

			/** check for any windows fonts lurking in Adobe folders as well */
			if (DecoderOptions.isRunningOnWindows) {
				File adobeFonts = new File("C:\\Program Files\\Adobe\\");

				if (adobeFonts.exists()) {

					String[] subdirs = adobeFonts.list();

					for (String path : subdirs) {
						String adobePath = "C:\\Program Files\\Adobe\\" + path + "\\Resource\\CIDFont";
						File testAdobe = new File(adobePath);

						// add if it exists
						if (testAdobe.exists()) {
							addTTDir(adobePath, "");
						}
					}
				}
			}
		}
	}

	/**
	 * takes a String[] of font directories and adds to substitution - Can just be called for each JVM - Should be called before file opened - this
	 * offers an alternative to the call -DFontDirs - Passing a null value flushes all settings
	 * 
	 * @return String which will be null or list of directories it could not find
	 */
	public static String setFontDirs(String[] fontDirs) {

		String failed = null;

		if (FontMappings.fontSubstitutionTable == null) {
			fontSubstitutionTable = new HashMap<String, String>();
			fontSubstitutionFontID = new HashMap<String, Integer>();
			fontPossDuplicates = new HashMap<String, String>();
			fontPropertiesTable = new HashMap();
		}

		try {
			if (fontDirs == null) { // idiot safety test
				if (LogWriter.isOutput()) LogWriter.writeLog("Null font parameter passed");

				fontSubstitutionAliasTable.clear();
				fontSubstitutionLocation.clear();
				fontSubstitutionTable.clear();
				fontSubstitutionFontID.clear();
				fontPossDuplicates.clear();
				fontPropertiesTable.clear();

				fontsSet = false;
			}
			else {

				for (String fontDir : fontDirs) {

					String fontPath = fontDir;

					// allow for 'wrong' separator
					if (!fontPath.endsWith("/") && !fontPath.endsWith("\\")) fontPath = fontPath + separator;

					failed = addTTDir(fontPath, failed);
				}
			}
		}
		catch (Exception e) {
			if (LogWriter.isOutput()) LogWriter.writeLog("Unable to run setFontDirs " + e.getMessage());
		}

		return failed;
	}

	/**
	 * This routine allows the user to add truetype, type1 or type1C fonts which will be used to disalay the fonts in PDF rendering and substitution
	 * as if the fonts were embedded in the PDF <br>
	 * This is very useful for clients looking to keep down the size of PDFs transmitted and control display quality -
	 * <p/>
	 * Thanks to Peter for the idea/code -
	 * <p/>
	 * How to set it up -
	 * <p/>
	 * JPedal will look for the existence of the directory fontPath (ie com/myCompany/Fonts) -
	 * <p/>
	 * If this exists, Jpedal will look for 3 possible directories (tt,t1c,t1) and make a note of any fonts if these directories exist -
	 * <p/>
	 * When fonts are resolved, this option will be tested first and if a font if found, it will be used to display the font (the effect will be the
	 * same as if the font was embedded) -
	 * <p/>
	 * If the enforceMapping is true, JPedal assumes there must be a match and will throw a PdfFontException -
	 * <p/>
	 * Otherwise Jpedal will look in the java font path for a match or approximate with Lucida -
	 * <p/>
	 * The Format is defined as follows: -
	 * <p/>
	 * fontname = filename
	 * <p/>
	 * Type1/Type1C Font names exclude any prefix so /OEGPNB+FGHeavyItalic is resolved to FGHeavyItalic -
	 * <p/>
	 * Each font have the same name as the font it replaces (so Arial will require a font file such as Arial.ttf) and it must be unique (there cannot
	 * be an Arial font in each sub-directory) -
	 * <p/>
	 * So to use this functionality, place the fonts in a jar or add to the JPedal jar and call this method after instancing PdfDecoder - JPedal will
	 * do the rest
	 * 
	 * @param fontPath
	 *            - root directory for fonts
	 * @param enforceMapping
	 *            - tell JPedal if all fonts should be in this directory
	 * @return flag (true if fonts added)
	 */
	public static boolean addSubstituteFonts(String fontPath, boolean enforceMapping) {

		boolean hasFonts = false;

		InputStream in = null, dir = null;

		try {
			String[] dirs = { "tt", "t1c", "t1" };
			String[] types = { "/TrueType", "/Type1C", "/Type1" };

			// check fontpath ends with separator - we may need to check this.
			// if((!fontPath.endsWith("/"))&(!fontPath.endsWith("\\")))
			// fontPath=fontPath=fontPath+separator;

			enforceFontSubstitution = enforceMapping;

			ClassLoader loader = FontMappings.class.getClass().getClassLoader();

			// see if root dir exists
			dir = loader.getResourceAsStream(fontPath);

			if (LogWriter.isOutput()) LogWriter.writeLog("Looking for root " + fontPath);

			// if it does, look for sub-directories
			if (in != null) {

				if (LogWriter.isOutput()) LogWriter.writeLog("Adding fonts fonts found in  tt,t1c,t1 sub-directories of " + fontPath);

				hasFonts = true;

				for (int i = 0; i < dirs.length; i++) {

					if (!fontPath.endsWith("/")) fontPath = fontPath + '/';

					String path = fontPath + dirs[i] + '/';

					// see if it exists
					in = loader.getResourceAsStream(path);

					// if it does read its contents and store
					if (in != null) {
						System.out.println("Found  " + path + ' ' + in);

						ArrayList<String> fonts;

						try {

							// works with IDE or jar
							if (in instanceof ByteArrayInputStream) fonts = readIndirectValues(in);
							else fonts = getDirectoryMatches(path);

							String value, fontName;

							// now assign the fonts
							for (Object font : fonts) {

								value = (String) font;

								if (value == null) break;

								int pointer = value.indexOf('.');
								if (pointer == -1) fontName = value.toLowerCase();
								else fontName = value.substring(0, pointer).toLowerCase();

								fontSubstitutionTable.put(fontName, types[i]);
								fontSubstitutionLocation.put(fontName, path + value);

							}

						}
						catch (Exception e) {
							if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " reading substitute fonts");

							System.out.println("Exception " + e + " reading substitute fonts");
							// <start-demo>
							// <end-demo>
						}
						finally {
							if (in != null) {
								try {
									in.close();
								}
								catch (IOException e) {
									// tell user and log
									if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
								}
							}
						}
					}

				}
			}
			else
				if (LogWriter.isOutput()) LogWriter.writeLog("No fonts found at " + fontPath);

		}
		catch (Exception e) {
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception adding substitute fonts " + e.getMessage());
		}
		finally { // close streams if open
			if (in != null) {
				try {
					in.close();
				}
				catch (IOException e) {
					// tell user and log
					if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
				}
			}

			if (dir != null) {
				try {
					dir.close();
				}
				catch (IOException e) {
					// tell user and log
					if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
				}
			}
		}

		return hasFonts;
	}

	/**
	 * method to add a single file to the PDF renderer
	 * 
	 * @param currentFont
	 *            - actual font name we use to identify
	 * @param fontPath
	 *            - full path to font file used for this font
	 */
	public static void addFontFile(String currentFont, String fontPath) {

		if (fontSubstitutionTable == null) {
			fontSubstitutionTable = new HashMap<String, String>();
			fontSubstitutionFontID = new HashMap<String, Integer>();
			fontPossDuplicates = new HashMap<String, String>();
			fontPropertiesTable = new HashMap();
		}

		// add separator if needed
		if (fontPath != null && !fontPath.endsWith("/") && !fontPath.endsWith("\\")) fontPath = fontPath + separator;

		String name = currentFont.toLowerCase();

		// decide font type
		int type = StandardFonts.getFontType(name);

		InputStream in = null;

		if (type != StandardFonts.FONT_UNSUPPORTED && new File(fontPath + currentFont).exists()) {
			// see if root dir exists

			boolean failed = false;

			try {
				in = new FileInputStream(fontPath + currentFont);

			}
			catch (Exception e) {
				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
				failed = true;
			}
			catch (Error err) {
				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Error: " + err.getMessage());
				err.printStackTrace(System.out);

				failed = true;
			}

			// if it does, add
			if (!failed) {

				String fontName;

				// name from file
				int pointer = currentFont.indexOf('.');
				if (pointer == -1) fontName = currentFont.toLowerCase();
				else fontName = currentFont.substring(0, pointer).toLowerCase();

				// choose filename or over-ride if OpenType
				if (fontSubstitutionMode == PdfDecoder.SUBSTITUTE_FONT_USING_FILE_NAME || type == StandardFonts.OPENTYPE) {
					if (type == StandardFonts.TYPE1) fontSubstitutionTable.put(fontName, "/Type1");
					else // TT or OTF
					fontSubstitutionTable.put(fontName, "/TrueType");

					fontSubstitutionLocation.put(fontName, fontPath + currentFont);

					// store details under file
					fontPropertiesTable.put(fontName + "_type", type);
					fontPropertiesTable.put(fontName + "_path", fontPath + currentFont);

				}
				else
					if (type == StandardFonts.TRUETYPE_COLLECTION || type == StandardFonts.TRUETYPE) {

						if (fontSubstitutionMode == PdfDecoder.SUBSTITUTE_FONT_USING_POSTSCRIPT_NAME_USE_FAMILY_NAME_IF_DUPLICATES) {

							// get both possible values
							String[] postscriptNames = null;
							try {
								postscriptNames = StandardFonts.readNamesFromFont(type, fontPath + currentFont,
										PdfDecoder.SUBSTITUTE_FONT_USING_POSTSCRIPT_NAME);
							}
							catch (Exception e) {
								// tell user and log
								if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
							}

							String[] familyNames = null;
							try {
								familyNames = StandardFonts.readNamesFromFont(type, fontPath + currentFont,
										PdfDecoder.SUBSTITUTE_FONT_USING_FAMILY_NAME);
							}
							catch (Exception e) {
								// tell user and log
								if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
							}

							int fontCount = 0;
							if (postscriptNames != null) fontCount = postscriptNames.length;

							for (int ii = 0; ii < fontCount; ii++) {

								// allow for null and use font name
								if (postscriptNames[ii] == null) postscriptNames[ii] = Strip.stripAllSpaces(fontName);

								// allow for null and use font name
								if (familyNames[ii] == null) familyNames[ii] = Strip.stripAllSpaces(fontName);

								Object fontSubValue = fontSubstitutionTable.get(postscriptNames[ii]);
								Object possDuplicate = fontPossDuplicates.get(postscriptNames[ii]);
								if (fontSubValue == null && possDuplicate == null) { // first time so store and track

									// System.out.println("store "+postscriptNames[ii]);

									fontSubstitutionTable.put(postscriptNames[ii], "/TrueType");
									fontSubstitutionLocation.put(postscriptNames[ii], fontPath + currentFont);
									fontSubstitutionFontID.put(postscriptNames[ii], ii);

									// and remember in case we need to switch
									fontPossDuplicates.put(postscriptNames[ii], familyNames[ii]);

								}
								else
									if (!familyNames[ii].equals(postscriptNames[ii])) {
										// if no duplicates,add to mappings with POSTSCRIPT and log filename
										// both lists should be in same order and name

										// else save as FAMILY_NAME
										fontSubstitutionTable.put(postscriptNames[ii], "/TrueType");
										fontSubstitutionLocation.put(postscriptNames[ii], fontPath + currentFont);
										fontSubstitutionFontID.put(postscriptNames[ii], ii);

										// store details under file
										fontPropertiesTable.put(postscriptNames[ii] + "_type", type);
										fontPropertiesTable.put(postscriptNames[ii] + "_path", fontPath + currentFont);

										// if second find change first match
										if (!possDuplicate.equals("DONE")) {

											// System.out.println("replace "+postscriptNames[ii]+" "+familyNames[ii]);

											// flag as done
											fontPossDuplicates.put(postscriptNames[ii], "DONE");

											// swap over
											fontSubstitutionTable.remove(postscriptNames[ii]);
											fontSubstitutionTable.put(familyNames[ii], "/TrueType");

											String font = fontSubstitutionLocation.get(postscriptNames[ii]);
											fontSubstitutionLocation.remove(postscriptNames[ii]);
											fontSubstitutionLocation.put(familyNames[ii], font);

											fontSubstitutionFontID.remove(postscriptNames[ii]);
											fontSubstitutionFontID.put(familyNames[ii], ii);

											// store details under file
											fontPropertiesTable.remove(familyNames[ii] + "_path");
											fontPropertiesTable.remove(familyNames[ii] + "_type");

											fontPropertiesTable.put(familyNames[ii] + "_type", type);
											fontPropertiesTable.put(familyNames[ii] + "_path", fontPath + currentFont);

										}
									}
							}

						}
						else { // easy version
								// read 1 or more font mappings from file
							String[] fontNames = null;
							try {
								fontNames = StandardFonts.readNamesFromFont(type, fontPath + currentFont, fontSubstitutionMode);
							}
							catch (Exception e) {
								// tell user and log
								if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
							}

							if (fontNames != null) {
								for (int i = 0; i < fontNames.length; i++) {

									// allow for null and use font name
									if (fontNames[i] == null) fontNames[i] = Strip.stripAllSpaces(fontName);

									fontSubstitutionTable.put(fontNames[i], "/TrueType");
									fontSubstitutionLocation.put(fontNames[i], fontPath + currentFont);
									fontSubstitutionFontID.put(fontNames[i], i);

									// store details under file
									fontPropertiesTable.put(fontNames[i] + "_type", type);
									fontPropertiesTable.put(fontNames[i] + "_path", fontPath + currentFont);
								}
							}
						}
					}
					else
						if (type == StandardFonts.TYPE1) {// || type == StandardFonts.OPENTYPE){ //type1

							// read 1 or more font mappings from file
							String[] fontNames = null;
							try {
								fontNames = StandardFonts.readNamesFromFont(type, fontPath + currentFont, fontSubstitutionMode);
							}
							catch (Exception e) {
								// tell user and log
								if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
							}

							if (fontNames != null) {
								for (int i = 0; i < fontNames.length; i++) {

									// allow for null and use font name
									if (fontNames[i] == null) fontNames[i] = Strip.stripAllSpaces(fontName);

									// System.out.println("font="+fontNames[i]);

									fontSubstitutionTable.put(fontNames[i], "/Type1");
									fontSubstitutionLocation.put(fontNames[i], fontPath + currentFont);
									fontSubstitutionFontID.put(fontNames[i], i);

									// store details under file
									fontPropertiesTable.put(fontNames[i] + "_type", type);
									fontPropertiesTable.put(fontNames[i] + "_path", fontPath + currentFont);

								}
							}
						}
			}
			else
				if (LogWriter.isOutput()) {
					LogWriter.writeLog("No fonts found at " + fontPath);
				}
		}

		// finally close
		if (in != null) {
			try {
				in.close();
			}
			catch (IOException e) {
				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
			}
		}
	}

	private static ArrayList<String> getDirectoryMatches(String sDirectoryName) throws IOException {

		sDirectoryName.replaceAll("\\.", "/");

		URL u = Thread.currentThread().getContextClassLoader().getResource(sDirectoryName);
		ArrayList<String> retValue = new ArrayList<String>(0);
		String s = u.toString();

		System.out.println("scanning " + s);

		if (s.startsWith("jar:") && s.endsWith(sDirectoryName)) {
			int idx = s.lastIndexOf(sDirectoryName);
			s = s.substring(0, idx); // isolate entry name

			System.out.println("entry= " + s);

			URL url = new URL(s);
			// Get the jar file
			JarURLConnection conn = (JarURLConnection) url.openConnection();
			JarFile jar = conn.getJarFile();

			for (Enumeration e = jar.entries(); e.hasMoreElements();) {
				JarEntry entry = (JarEntry) e.nextElement();
				if ((!entry.isDirectory()) & (entry.getName().startsWith(sDirectoryName))) { // this
					// is how you can match
					// to find your fonts.
					// System.out.println("Found a match!");
					String fontName = entry.getName();
					int i = fontName.lastIndexOf('/');
					fontName = fontName.substring(i + 1);
					retValue.add(fontName);
				}
			}
		}
		else {
			// Does not start with "jar:"
			// Dont know - should not happen
			if (LogWriter.isOutput()) LogWriter.writeLog("Path: " + s);
		}
		return retValue;
	}

	/**
	 * read values from the classpath
	 */
	private static ArrayList<String> readIndirectValues(InputStream in) throws IOException {
		ArrayList<String> fonts;
		BufferedReader inpStream = new BufferedReader(new InputStreamReader(in));
		fonts = new ArrayList<String>(0);
		while (true) {
			String nextValue = inpStream.readLine();
			if (nextValue == null) break;

			fonts.add(nextValue);
		}

		inpStream.close();

		return fonts;
	}

	/**
	 * set the font used for default from Java fonts on system - Java fonts are case sensitive, but JPedal resolves this internally, so you could use
	 * Webdings, webdings or webDings for Java font Webdings - checks if it is a valid Java font (otherwise it will default to Lucida anyway)
	 */
	public static void setDefaultDisplayFont(String fontName) throws PdfFontException {

		boolean isFontInstalled = false;

		// get list of fonts and see if installed
		String[] fontList = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();

		int count = fontList.length;

		for (int i = 0; i < count; i++) {
			if (fontList[i].toLowerCase().equals(fontName.toLowerCase())) {
				isFontInstalled = true;
				defaultFont = fontList[i];
				i = count;
			}
		}

		if (!isFontInstalled) throw new PdfFontException("Font " + fontName + " is not available.");
	}
}
