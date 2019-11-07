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
 * Type1C.java
 * ---------------
 */
package org.jpedal.fonts;

import java.awt.Rectangle;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.jpedal.fonts.glyph.PdfJavaGlyphs;
import org.jpedal.fonts.glyph.T1Glyphs;
import org.jpedal.fonts.objects.FontData;
import org.jpedal.io.ObjectStore;
import org.jpedal.io.PdfObjectReader;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.utils.LogWriter;

/**
 * handlestype1 specifics
 */
public class Type1C extends Type1 {

	private static final long serialVersionUID = -3010162163120599585L;

	static final boolean debugFont = false;

	static final boolean debugDictionary = false;

	int ros = -1, CIDFontVersion = 0, CIDFontRevision = 0, CIDFontType = 0, CIDcount = 0, UIDBase = -1, FDArray = -1, FDSelect = -1;

	final static String[] OneByteCCFDict = { "version", "Notice", "FullName", "FamilyName", "Weight", "FontBBox", "BlueValues", "OtherBlues",
			"FamilyBlues", "FamilyOtherBlues", "StdHW", "StdVW", "Escape", "UniqueID", "XUID", "charset", "Encoding", "CharStrings", "Private",
			"Subrs", "defaultWidthX", "nominalWidthX", "-reserved-", "-reserved-", "-reserved-", "-reserved-", "-reserved-", "-reserved-",
			"shortint", "longint", "BCD", "-reserved-" };

	final static String[] TwoByteCCFDict = { "Copyright", "isFixedPitch", "ItalicAngle", "UnderlinePosition", "UnderlineThickness", "PaintType",
			"CharstringType", "FontMatrix", "StrokeWidth", "BlueScale", "BlueShift", "BlueFuzz", "StemSnapH", "StemSnapV", "ForceBold", "-reserved-",
			"-reserved-", "LanguageGroup", "ExpansionFactor", "initialRandomSeed", "SyntheticBase", "PostScript", "BaseFontName", "BaseFontBlend",
			"-reserved-", "-reserved-", "-reserved-", "-reserved-", "-reserved-", "-reserved-", "ROS", "CIDFontVersion", "CIDFontRevision",
			"CIDFontType", "CIDCount", "UIDBase", "FDArray", "FDSelect", "FontName" };

	// current location in file
	private int top = 0;

	private int charset = 0;

	private int enc = 0;

	private int charstrings = 0;

	private int stringIdx;

	private int stringStart;

	private int stringOffSize;

	private Rectangle BBox = null;

	private boolean hasFontMatrix = false;// hasFontBBox=false,;

	private int[] privateDict = { -1 }, privateDictOffset = { -1 };
	private int currentFD = -1;

	private int[] defaultWidthX = { 0 }, nominalWidthX = { 0 }, fdSelect;

	/** decoding table for Expert */
	private static final int ExpertSubCharset[] = { // 87
			// elements
			0, 1, 231, 232, 235, 236, 237, 238, 13, 14, 15, 99, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248, 27, 28, 249, 250, 251, 253, 254,
			255, 256, 257, 258, 259, 260, 261, 262, 263, 264, 265, 266, 109, 110, 267, 268, 269, 270, 272, 300, 301, 302, 305, 314, 315, 158, 155,
			163, 320, 321, 322, 323, 324, 325, 326, 150, 164, 169, 327, 328, 329, 330, 331, 332, 333, 334, 335, 336, 337, 338, 339, 340, 341, 342,
			343, 344, 345, 346 };

	/** lookup table for names for type 1C glyphs */
	public static final String type1CStdStrings[] = { // 391
			// elements
			".notdef", "space", "exclam", "quotedbl", "numbersign", "dollar", "percent", "ampersand", "quoteright", "parenleft", "parenright",
			"asterisk", "plus", "comma", "hyphen", "period", "slash", "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
			"colon", "semicolon", "less", "equal", "greater", "question", "at", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N",
			"O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "bracketleft", "backslash", "bracketright", "asciicircum", "underscore",
			"quoteleft", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y",
			"z", "braceleft", "bar", "braceright", "asciitilde", "exclamdown", "cent", "sterling", "fraction", "yen", "florin", "section",
			"currency", "quotesingle", "quotedblleft", "guillemotleft", "guilsinglleft", "guilsinglright", "fi", "fl", "endash", "dagger",
			"daggerdbl", "periodcentered", "paragraph", "bullet", "quotesinglbase", "quotedblbase", "quotedblright", "guillemotright", "ellipsis",
			"perthousand", "questiondown", "grave", "acute", "circumflex", "tilde", "macron", "breve", "dotaccent", "dieresis", "ring", "cedilla",
			"hungarumlaut", "ogonek", "caron", "emdash", "AE", "ordfeminine", "Lslash", "Oslash", "OE", "ordmasculine", "ae", "dotlessi", "lslash",
			"oslash", "oe", "germandbls", "onesuperior", "logicalnot", "mu", "trademark", "Eth", "onehalf", "plusminus", "Thorn", "onequarter",
			"divide", "brokenbar", "degree", "thorn", "threequarters", "twosuperior", "registered", "minus", "eth", "multiply", "threesuperior",
			"copyright", "Aacute", "Acircumflex", "Adieresis", "Agrave", "Aring", "Atilde", "Ccedilla", "Eacute", "Ecircumflex", "Edieresis",
			"Egrave", "Iacute", "Icircumflex", "Idieresis", "Igrave", "Ntilde", "Oacute", "Ocircumflex", "Odieresis", "Ograve", "Otilde", "Scaron",
			"Uacute", "Ucircumflex", "Udieresis", "Ugrave", "Yacute", "Ydieresis", "Zcaron", "aacute", "acircumflex", "adieresis", "agrave", "aring",
			"atilde", "ccedilla", "eacute", "ecircumflex", "edieresis", "egrave", "iacute", "icircumflex", "idieresis", "igrave", "ntilde", "oacute",
			"ocircumflex", "odieresis", "ograve", "otilde", "scaron", "uacute", "ucircumflex", "udieresis", "ugrave", "yacute", "ydieresis",
			"zcaron", "exclamsmall", "Hungarumlautsmall", "dollaroldstyle", "dollarsuperior", "ampersandsmall", "Acutesmall", "parenleftsuperior",
			"parenrightsuperior", "twodotenleader", "onedotenleader", "zerooldstyle", "oneoldstyle", "twooldstyle", "threeoldstyle", "fouroldstyle",
			"fiveoldstyle", "sixoldstyle", "sevenoldstyle", "eightoldstyle", "nineoldstyle", "commasuperior", "threequartersemdash",
			"periodsuperior", "questionsmall", "asuperior", "bsuperior", "centsuperior", "dsuperior", "esuperior", "isuperior", "lsuperior",
			"msuperior", "nsuperior", "osuperior", "rsuperior", "ssuperior", "tsuperior", "ff", "ffi", "ffl", "parenleftinferior",
			"parenrightinferior", "Circumflexsmall", "hyphensuperior", "Gravesmall", "Asmall", "Bsmall", "Csmall", "Dsmall", "Esmall", "Fsmall",
			"Gsmall", "Hsmall", "Ismall", "Jsmall", "Ksmall", "Lsmall", "Msmall", "Nsmall", "Osmall", "Psmall", "Qsmall", "Rsmall", "Ssmall",
			"Tsmall", "Usmall", "Vsmall", "Wsmall", "Xsmall", "Ysmall", "Zsmall", "colonmonetary", "onefitted", "rupiah", "Tildesmall",
			"exclamdownsmall", "centoldstyle", "Lslashsmall", "Scaronsmall", "Zcaronsmall", "Dieresissmall", "Brevesmall", "Caronsmall",
			"Dotaccentsmall", "Macronsmall", "figuredash", "hypheninferior", "Ogoneksmall", "Ringsmall", "Cedillasmall", "questiondownsmall",
			"oneeighth", "threeeighths", "fiveeighths", "seveneighths", "onethird", "twothirds", "zerosuperior", "foursuperior", "fivesuperior",
			"sixsuperior", "sevensuperior", "eightsuperior", "ninesuperior", "zeroinferior", "oneinferior", "twoinferior", "threeinferior",
			"fourinferior", "fiveinferior", "sixinferior", "seveninferior", "eightinferior", "nineinferior", "centinferior", "dollarinferior",
			"periodinferior", "commainferior", "Agravesmall", "Aacutesmall", "Acircumflexsmall", "Atildesmall", "Adieresissmall", "Aringsmall",
			"AEsmall", "Ccedillasmall", "Egravesmall", "Eacutesmall", "Ecircumflexsmall", "Edieresissmall", "Igravesmall", "Iacutesmall",
			"Icircumflexsmall", "Idieresissmall", "Ethsmall", "Ntildesmall", "Ogravesmall", "Oacutesmall", "Ocircumflexsmall", "Otildesmall",
			"Odieresissmall", "OEsmall", "Oslashsmall", "Ugravesmall", "Uacutesmall", "Ucircumflexsmall", "Udieresissmall", "Yacutesmall",
			"Thornsmall", "Ydieresissmall", "001.000", "001.001", "001.002", "001.003", "Black", "Bold", "Book", "Light", "Medium", "Regular",
			"Roman", "Semibold" };

	/** Lookup table to map values */
	private static final int ISOAdobeCharset[] = { // 229
			// elements
			0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36,
			37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70,
			71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103,
			104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130,
			131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157,
			158, 159, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 180, 181, 182, 183, 184,
			185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211,
			212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226, 227, 228 };
	/** lookup data to convert Expert values */
	private static final int ExpertCharset[] = { // 166
			// elements
			0, 1, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 13, 14, 15, 99, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248, 27, 28, 249,
			250, 251, 252, 253, 254, 255, 256, 257, 258, 259, 260, 261, 262, 263, 264, 265, 266, 109, 110, 267, 268, 269, 270, 271, 272, 273, 274,
			275, 276, 277, 278, 279, 280, 281, 282, 283, 284, 285, 286, 287, 288, 289, 290, 291, 292, 293, 294, 295, 296, 297, 298, 299, 300, 301,
			302, 303, 304, 305, 306, 307, 308, 309, 310, 311, 312, 313, 314, 315, 316, 317, 318, 158, 155, 163, 319, 320, 321, 322, 323, 324, 325,
			326, 150, 164, 169, 327, 328, 329, 330, 331, 332, 333, 334, 335, 336, 337, 338, 339, 340, 341, 342, 343, 344, 345, 346, 347, 348, 349,
			350, 351, 352, 353, 354, 355, 356, 357, 358, 359, 360, 361, 362, 363, 364, 365, 366, 367, 368, 369, 370, 371, 372, 373, 374, 375, 376,
			377, 378 };

	// one byte operators
	public final static int VERSION = 0;
	public final static int NOTICE = 1;
	public final static int FULLNAME = 2;
	public final static int FAMILYNAME = 3;
	public final static int WEIGHT = 4;
	public final static int FONTBBOX = 5;
	public final static int BLUEVALUES = 6;
	public final static int OTHERBLUES = 7;
	public final static int FAMILYBLUES = 8;
	public final static int FAMILYOTHERBLUES = 9;
	public final static int STDHW = 10;
	public final static int STDVW = 11;
	public final static int ESCAPE = 12;
	public final static int UNIQUEID = 13;
	public final static int XUID = 14;
	public final static int CHARSET = 15;
	public final static int ENCODING = 16;
	public final static int CHARSTRINGS = 17;
	public final static int PRIVATE = 18;
	public final static int SUBRS = 19;
	public final static int DEFAULTWIDTHX = 20;
	public final static int NOMINALWIDTHX = 21;
	public final static int RESERVED = 22;
	public final static int SHORTINT = 28;
	public final static int LONGINT = 29;
	public final static int BCD = 30;

	// two byte operators
	public final static int COPYRIGHT = 3072;
	public final static int ISFIXEDPITCH = 3073;
	public final static int ITALICANGLE = 3074;
	public final static int UNDERLINEPOSITION = 3075;
	public final static int UNDERLINETHICKNESS = 3076;
	public final static int PAINTTYPE = 3077;
	public final static int CHARSTRINGTYPE = 3078;
	public final static int FONTMATRIX = 3079;
	public final static int STROKEWIDTH = 3080;
	public final static int BLUESCALE = 3081;
	public final static int BLUESHIFT = 3082;
	public final static int BLUEFUZZ = 3083;
	public final static int STEMSNAPH = 3084;
	public final static int STEMSNAPV = 3085;
	public final static int FORCEBOLD = 3086;
	public final static int LANGUAGEGROUP = 3089;
	public final static int EXPANSIONFACTOR = 3090;
	public final static int INITIALRANDOMSEED = 3091;
	public final static int SYNTHETICBASE = 3092;
	public final static int POSTSCRIPT = 3093;
	public final static int BASEFONTNAME = 3094;
	public final static int BASEFONTBLEND = 3095;
	public final static int ROS = 3102;
	public final static int CIDFONTVERSION = 3103;
	public final static int CIDFONTREVISION = 3104;
	public final static int CIDFONTTYPE = 3105;
	public final static int CIDCOUNT = 3106;
	public final static int UIDBASE = 3107;
	public final static int FDARRAY = 3108;
	public final static int FDSELECT = 3109;
	public final static int FONTNAME = 3110;

	private int weight = 388;// make it default
	private int[] blueValues = null;
	private int[] otherBlues = null;
	private int[] familyBlues = null;
	private int[] familyOtherBlues = null;
	private int stdHW = -1, stdVW = -1, subrs = -1;

	private byte[] encodingDataBytes = null;
	private String[] stringIndexData = null;
	private int[] charsetGlyphCodes = null;
	private int charsetGlyphFormat = 0;
	private int[] rosArray = new int[3];

	/** needed so CIDFOnt0 can extend */
	public Type1C() {}

	/** get handles onto Reader so we can access the file */
	public Type1C(PdfObjectReader current_pdf_file, String substituteFont) {

		this.glyphs = new T1Glyphs(false);

		init(current_pdf_file);

		this.substituteFont = substituteFont;
	}

	/** read details of any embedded fontFile */
	protected void readEmbeddedFont(PdfObject pdfFontDescriptor) throws Exception {

		if (this.substituteFont != null) {

			byte[] bytes;

			// read details
			BufferedInputStream from;
			// create streams
			ByteArrayOutputStream to = new ByteArrayOutputStream();

			InputStream jarFile = null;

			try {
				if (this.substituteFont.startsWith("jar:") || this.substituteFont.startsWith("http:")) jarFile = this.loader
						.getResourceAsStream(this.substituteFont);
				else jarFile = this.loader.getResourceAsStream("file:///" + this.substituteFont);

			}
			catch (Exception e) {
				if (LogWriter.isOutput()) LogWriter.writeLog("3.Unable to open " + this.substituteFont);
			}
			catch (Error err) {
				if (LogWriter.isOutput()) LogWriter.writeLog("3.Unable to open " + this.substituteFont);
			}

			if (jarFile == null) {
				/**
				 * from=new BufferedInputStream(new FileInputStream(substituteFont));
				 * 
				 * //write byte[] buffer = new byte[65535]; int bytes_read; while ((bytes_read = from.read(buffer)) != -1) to.write(buffer, 0,
				 * bytes_read);
				 * 
				 * to.close(); from.close();
				 * 
				 * /
				 **/

				File file = new File(this.substituteFont);
				InputStream is = new FileInputStream(file);
				long length = file.length();

				if (length > Integer.MAX_VALUE) {
					System.out.println("Sorry! Your given file is too large.");
					return;
				}

				bytes = new byte[(int) length];
				int offset = 0;
				int numRead;
				while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
					offset += numRead;
				}
				if (offset < bytes.length) {
					throw new IOException("Could not completely read file " + file.getName());
				}
				is.close();
				// new BufferedReader
				// (new InputStreamReader(loader.getResourceAsStream("org/jpedal/res/cid/" + encodingName), "Cp1252"));
				/**
				 * FileReader from2=null; try { from2 = new FileReader(substituteFont); //new BufferedReader); //outputStream = new
				 * FileWriter("characteroutput.txt");
				 * 
				 * int c; while ((c = from2.read()) != -1) { to.write(c); } } finally { if (from2 != null) { from2.close(); } if (to != null) {
				 * to.close(); } }/
				 **/
			}
			else {
				from = new BufferedInputStream(jarFile);

				// write
				byte[] buffer = new byte[65535];
				int bytes_read;
				while ((bytes_read = from.read(buffer)) != -1)
					to.write(buffer, 0, bytes_read);

				to.close();
				from.close();

				bytes = to.toByteArray();
			}
			/** load the font */
			try {
				this.isFontSubstituted = true;

				// if (substituteFont.indexOf(".afm") != -1)
				readType1FontFile(bytes);
				// else
				// readType1CFontFile(to.toByteArray(),null);

			}
			catch (Exception e) {

				e.printStackTrace(System.out);

				if (LogWriter.isOutput()) LogWriter.writeLog("[PDF]Substitute font=" + this.substituteFont + "Type 1 exception=" + e);
			}

			// over-ride font remapping if substituted
			if (this.isFontSubstituted && this.glyphs.remapFont) this.glyphs.remapFont = false;

		}
		else
			if (pdfFontDescriptor != null) {

				PdfObject FontFile = pdfFontDescriptor.getDictionary(PdfDictionary.FontFile);

				/** try type 1 first then type 1c/0c */
				if (FontFile != null) {
					try {
						byte[] stream = this.currentPdfFile.readStream(FontFile, true, true, false, false, false,
								FontFile.getCacheName(this.currentPdfFile.getObjectReader()));
						if (stream != null) readType1FontFile(stream);
					}
					catch (Exception e) {
						// tell user and log
						if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
					}
				}
				else {

					PdfObject FontFile3 = pdfFontDescriptor.getDictionary(PdfDictionary.FontFile3);
					if (FontFile3 != null) {
						byte[] stream = this.currentPdfFile.readStream(FontFile3, true, true, false, false, false,
								FontFile3.getCacheName(this.currentPdfFile.getObjectReader()));
						if (stream != null) { // if it fails, null returned
							// check for type1c or ottf
							if (stream.length > 3 && stream[0] == 719 && stream[1] == 84 && stream[2] == 84 && stream[3] == 79) {}
							else // assume all standard cff for moment
							readType1CFontFile(stream, null);
						}
					}
				}
			}
	}

	/** read in a font and its details from the pdf file */
	@Override
	public void createFont(PdfObject pdfObject, String fontID, boolean renderPage, ObjectStore objectStore, Map substitutedFonts) throws Exception {

		this.fontTypes = StandardFonts.TYPE1;

		// generic setup
		init(fontID, renderPage);

		/**
		 * get FontDescriptor object - if present contains metrics on glyphs
		 */
		PdfObject pdfFontDescriptor = pdfObject.getDictionary(PdfDictionary.FontDescriptor);

		// FontBBox and FontMatix
		setBoundsAndMatrix(pdfFontDescriptor);

		setName(pdfObject, fontID);
		setEncoding(pdfObject, pdfFontDescriptor);

		try {
			readEmbeddedFont(pdfFontDescriptor);
		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}

		// setWidths(pdfObject);
		readWidths(pdfObject, true);

		// if(embeddedFontName!=null && is1C() && PdfStreamDecoder.runningStoryPad){
		// embeddedFontName= cleanupFontName(embeddedFontName);
		// this.setBaseFontName(embeddedFontName);
		// this.setFontName(embeddedFontName);
		// }

		// make sure a font set
		if (renderPage) setFont(getBaseFontName(), 1);
	}

	/** Constructor for html fonts */
	public Type1C(byte[] fontDataAsArray, PdfJavaGlyphs glyphs, boolean is1C) throws Exception {

		this.glyphs = glyphs;

		// generate reverse lookup so we can encode CMAP
		this.trackIndices = true;

		// flags we extract all details (used originally by rendering and now by PS2OTF)
		this.renderPage = true;

		if (is1C) readType1CFontFile(fontDataAsArray, null);
		else readType1FontFile(fontDataAsArray);
	}

	/** Constructor for OTF fonts */
	public Type1C(byte[] fontDataAsArray, FontData fontData, PdfJavaGlyphs glyphs) throws Exception {

		this.glyphs = glyphs;

		readType1CFontFile(fontDataAsArray, fontData);
	}

	/** Handle encoding for type1C fonts. Also used for CIDFontType0C */
	final private void readType1CFontFile(byte[] fontDataAsArray, FontData fontDataAsObject) throws Exception {

		if (LogWriter.isOutput()) LogWriter.writeLog("Embedded Type1C font used");

		this.glyphs.setis1C(true);

		boolean isByteArray = (fontDataAsArray != null);

		// debugFont=getBaseFontName().indexOf("LC")!=-1;

		if (debugFont) System.err.println(getBaseFontName());

		int start; // pointers within table
		int size = 2;

		/**
		 * read Header
		 */
		int major, minor;
		if (isByteArray) {
			major = fontDataAsArray[0];
			minor = fontDataAsArray[1];
		}
		else {
			major = fontDataAsObject.getByte(0);
			minor = fontDataAsObject.getByte(1);
		}

		if ((major != 1 || minor != 0) && LogWriter.isOutput()) LogWriter.writeLog("1C  format " + major + ':' + minor + " not fully supported");

		if (debugFont) System.out.println("major=" + major + " minor=" + minor);

		// read header size to workout start of names index
		if (isByteArray) this.top = fontDataAsArray[2];
		else this.top = fontDataAsObject.getByte(2);

		/**
		 * read names index
		 */
		// read name index for the first font
		int count, offsize;
		if (isByteArray) {
			count = getWord(fontDataAsArray, this.top, size);
			offsize = fontDataAsArray[this.top + size];
		}
		else {
			count = getWord(fontDataAsObject, this.top, size);
			offsize = fontDataAsObject.getByte(this.top + size);
		}

		/**
		 * get last offset and use to move to top dict index
		 */
		this.top += (size + 1); // move pointer to start of font names
		start = this.top + (count + 1) * offsize - 1; // move pointer to end of offsets
		if (isByteArray) this.top = start + getWord(fontDataAsArray, this.top + count * offsize, offsize);
		else this.top = start + getWord(fontDataAsObject, this.top + count * offsize, offsize);

		/**
		 * read the dict index
		 */
		if (isByteArray) {
			count = getWord(fontDataAsArray, this.top, size);
			offsize = fontDataAsArray[this.top + size];
		}
		else {
			count = getWord(fontDataAsObject, this.top, size);
			offsize = fontDataAsObject.getByte(this.top + size);
		}

		this.top += (size + 1); // update pointer
		start = this.top + (count + 1) * offsize - 1;

		int dicStart, dicEnd;
		if (isByteArray) {
			dicStart = start + getWord(fontDataAsArray, this.top, offsize);
			dicEnd = start + getWord(fontDataAsArray, this.top + offsize, offsize);
		}
		else {
			dicStart = start + getWord(fontDataAsObject, this.top, offsize);
			dicEnd = start + getWord(fontDataAsObject, this.top + offsize, offsize);
		}

		/**
		 * read string index
		 */
		String[] strings = readStringIndex(fontDataAsArray, fontDataAsObject, start, offsize, count);

		/**
		 * read global subroutines (top set by Strings code)
		 */
		readGlobalSubRoutines(fontDataAsArray, fontDataAsObject);

		/**
		 * decode the dictionary
		 */
		decodeDictionary(fontDataAsArray, fontDataAsObject, dicStart, dicEnd, strings);

		/**
		 * allow for subdictionaries in CID font
		 */
		if (this.FDSelect != -1) {

			if (debugDictionary) System.out.println("=============FDSelect====================" + getBaseFontName());

			// Read FDSelect
			int nextDic = this.FDSelect;

			int format;
			if (isByteArray) {
				format = getWord(fontDataAsArray, nextDic, 1);
			}
			else {
				format = getWord(fontDataAsObject, nextDic, 1);
			}

			int glyphCount;
			if (isByteArray) {
				glyphCount = getWord(fontDataAsArray, this.charstrings, 2);
			}
			else {
				glyphCount = getWord(fontDataAsObject, this.charstrings, 2);
			}

			this.fdSelect = new int[glyphCount];
			if (format == 0) {
				// Format 0 is just an array of which to use for each glyph
				for (int i = 0; i < glyphCount; i++) {
					if (isByteArray) {
						this.fdSelect[i] = getWord(fontDataAsArray, nextDic + 1 + i, 1);
					}
					else {
						this.fdSelect[i] = getWord(fontDataAsObject, nextDic + 1 + i, 1);
					}
				}
			}
			else
				if (format == 3) {
					int nRanges;
					if (isByteArray) {
						nRanges = getWord(fontDataAsArray, nextDic + 1, 2);
					}
					else {
						nRanges = getWord(fontDataAsObject, nextDic + 1, 2);
					}

					int[] rangeStarts = new int[nRanges + 1], fDicts = new int[nRanges];

					// Find ranges of glyphs with their DICT index
					for (int i = 0; i < nRanges; i++) {
						if (isByteArray) {
							rangeStarts[i] = getWord(fontDataAsArray, nextDic + 3 + (3 * i), 2);
							fDicts[i] = getWord(fontDataAsArray, nextDic + 5 + (3 * i), 1);
						}
						else {
							rangeStarts[i] = getWord(fontDataAsObject, nextDic + 3 + (3 * i), 2);
							fDicts[i] = getWord(fontDataAsObject, nextDic + 5 + (3 * i), 1);
						}
					}
					rangeStarts[rangeStarts.length - 1] = glyphCount;

					// Fill fdSelect array
					for (int i = 0; i < nRanges; i++) {
						for (int j = rangeStarts[i]; j < rangeStarts[i + 1]; j++) {
							this.fdSelect[j] = fDicts[i];
						}
					}
				}
			((T1Glyphs) this.glyphs).setFDSelect(this.fdSelect);

			// Read FDArray
			nextDic = this.FDArray;

			if (isByteArray) {
				count = getWord(fontDataAsArray, nextDic, size);
				offsize = fontDataAsArray[nextDic + size];
			}
			else {
				count = getWord(fontDataAsObject, nextDic, size);
				offsize = fontDataAsObject.getByte(nextDic + size);
			}

			nextDic += (size + 1); // update pointer
			start = nextDic + (count + 1) * offsize - 1;

			this.privateDict = new int[count];
			this.privateDictOffset = new int[count];
			this.defaultWidthX = new int[count];
			this.nominalWidthX = new int[count];

			for (int i = 0; i < count; i++) {
				this.currentFD = i;
				this.privateDict[i] = -1;
				this.privateDictOffset[i] = -1;

				if (isByteArray) {
					dicStart = start + getWord(fontDataAsArray, nextDic + (i * offsize), offsize);
					dicEnd = start + getWord(fontDataAsArray, nextDic + ((i + 1) * offsize), offsize);
				}
				else {
					dicStart = start + getWord(fontDataAsObject, nextDic + (i * offsize), offsize);
					dicEnd = start + getWord(fontDataAsObject, nextDic + ((i + 1) * offsize), offsize);
				}

				decodeDictionary(fontDataAsArray, fontDataAsObject, dicStart, dicEnd, strings);

			}
			this.currentFD = -1;
			if (debugDictionary) System.out.println("=================================" + getBaseFontName());

		}

		/**
		 * get number of glyphs from charstrings index
		 */
		this.top = this.charstrings;

		int nGlyphs;

		if (isByteArray) nGlyphs = getWord(fontDataAsArray, this.top, size); // start of glyph index
		else nGlyphs = getWord(fontDataAsObject, this.top, size); // start of glyph index

		this.glyphs.setGlyphCount(nGlyphs);

		if (debugFont) System.out.println("nGlyphs=" + nGlyphs);

		int[] names = readCharset(this.charset, nGlyphs, fontDataAsObject, fontDataAsArray);

		if (debugFont) {
			System.out.println("=======charset===============");
			int count2 = names.length;
			for (int jj = 0; jj < count2; jj++) {
				System.out.println(jj + " " + names[jj]);
			}

			System.out.println("=======Encoding===============");
		}

		/**
		 * set encoding if not set
		 */
		setEncoding(fontDataAsArray, fontDataAsObject, nGlyphs, names);

		/**
		 * read glyph index
		 */
		this.top = this.charstrings;
		readGlyphs(fontDataAsArray, fontDataAsObject, nGlyphs, names);

		/**/
		for (int i = 0; i < this.privateDict.length; i++) {
			this.currentFD = i;
			int dict = this.privateDict[i];
			if (dict != -1) {

				int dictOffset = this.privateDictOffset[i];
				decodeDictionary(fontDataAsArray, fontDataAsObject, dict, dictOffset + dict, strings);

				this.top = dict + dictOffset;

				int len, nSubrs;

				if (isByteArray) len = fontDataAsArray.length;
				else len = fontDataAsObject.length();

				if (this.top + 2 < len) {
					if (isByteArray) nSubrs = getWord(fontDataAsArray, this.top, size);
					else nSubrs = getWord(fontDataAsObject, this.top, size);

					if (nSubrs > 0) readSubrs(fontDataAsArray, fontDataAsObject, nSubrs);
				}
				else
					if (debugFont || debugDictionary) {
						System.out.println("Private subroutine out of range");
					}

			}
		}
		this.currentFD = -1;
		/**/
		/**
		 * set flags to tell software to use these descritpions
		 */
		this.isFontEmbedded = true;

		this.glyphs.setFontEmbedded(true);
	}

	/** pick up encoding from embedded font */
	final private void setEncoding(byte[] fontDataAsArray, FontData fontDataAsObject, int nGlyphs, int[] names) {

		boolean isByteArray = fontDataAsArray != null;

		if (debugFont) System.out.println("Enc=" + this.enc);

		// read encoding (glyph -> code mapping)
		if (this.enc == 0) {
			this.embeddedEnc = StandardFonts.STD;
			if (this.fontEnc == -1) putFontEncoding(StandardFonts.STD);

			if (this.isCID) {
				// store values for lookup on text
				try {
					String name;
					for (int i = 1; i < nGlyphs; ++i) {

						if (names[i] < 391) {
							if (isByteArray) name = getString(fontDataAsArray, names[i], this.stringIdx, this.stringStart, this.stringOffSize);
							else name = getString(fontDataAsObject, names[i], this.stringIdx, this.stringStart, this.stringOffSize);

							putMappedChar(names[i], StandardFonts.getUnicodeName(name));
						}
					}
				}
				catch (Exception e) {
					// tell user and log
					if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
				}

			}
		}
		else
			if (this.enc == 1) {
				this.embeddedEnc = StandardFonts.MACEXPERT;
				if (this.fontEnc == -1) putFontEncoding(StandardFonts.MACEXPERT);
			}
			else { // custom mapping

				if (debugFont) System.out.println("custom mapping");

				this.top = this.enc;
				int encFormat, c;

				if (isByteArray) encFormat = (fontDataAsArray[this.top++] & 0xff);
				else encFormat = (fontDataAsObject.getByte(this.top++) & 0xff);

				String name;

				if ((encFormat & 0x7f) == 0) { // format 0

					int nCodes;

					if (isByteArray) nCodes = 1 + (fontDataAsArray[this.top++] & 0xff);
					else nCodes = 1 + (fontDataAsObject.getByte(this.top++) & 0xff);

					if (nCodes > nGlyphs) nCodes = nGlyphs;
					for (int i = 1; i < nCodes; ++i) {

						if (isByteArray) {
							c = fontDataAsArray[this.top++] & 0xff;
							name = getString(fontDataAsArray, names[i], this.stringIdx, this.stringStart, this.stringOffSize);

						}
						else {
							c = fontDataAsObject.getByte(this.top++) & 0xff;
							name = getString(fontDataAsObject, names[i], this.stringIdx, this.stringStart, this.stringOffSize);
						}

						putChar(c, name);

					}

				}
				else
					if ((encFormat & 0x7f) == 1) { // format 1

						int nRanges;
						if (isByteArray) nRanges = (fontDataAsArray[this.top++] & 0xff);
						else nRanges = (fontDataAsObject.getByte(this.top++) & 0xff);

						int nCodes = 1;
						for (int i = 0; i < nRanges; ++i) {

							int nLeft;

							if (isByteArray) {
								c = (fontDataAsArray[this.top++] & 0xff);
								nLeft = (fontDataAsArray[this.top++] & 0xff);
							}
							else {
								c = (fontDataAsObject.getByte(this.top++) & 0xff);
								nLeft = (fontDataAsObject.getByte(this.top++) & 0xff);
							}

							for (int j = 0; j <= nLeft && nCodes < nGlyphs; ++j) {

								if (isByteArray) name = getString(fontDataAsArray, names[nCodes], this.stringIdx, this.stringStart,
										this.stringOffSize);
								else name = getString(fontDataAsObject, names[nCodes], this.stringIdx, this.stringStart, this.stringOffSize);

								putChar(c, name);

								nCodes++;
								c++;
							}
						}
					}

				if ((encFormat & 0x80) != 0) { // supplimentary encodings

					int nSups;

					if (isByteArray) nSups = (fontDataAsArray[this.top++] & 0xff);
					else nSups = (fontDataAsObject.getByte(this.top++) & 0xff);

					for (int i = 0; i < nSups; ++i) {

						if (isByteArray) c = (fontDataAsArray[this.top++] & 0xff);
						else c = (fontDataAsObject.getByte(this.top++) & 0xff);

						int sid;

						if (isByteArray) sid = getWord(fontDataAsArray, this.top, 2);
						else sid = getWord(fontDataAsObject, this.top, 2);

						this.top += 2;

						if (isByteArray) name = getString(fontDataAsArray, sid, this.stringIdx, this.stringStart, this.stringOffSize);
						else name = getString(fontDataAsObject, sid, this.stringIdx, this.stringStart, this.stringOffSize);

						putChar(c, name);

					}
				}
			}
	}

	// LILYPONDTOOL
	private final void readSubrs(byte[] fontDataAsArray, FontData fontDataAsObject, int nSubrs) throws Exception {

		boolean isByteArray = fontDataAsArray != null;

		int subrOffSize;

		if (isByteArray) subrOffSize = fontDataAsArray[this.top + 2];
		else subrOffSize = fontDataAsObject.getByte(this.top + 2);

		this.top += 3;
		int subrIdx = this.top;
		int subrStart = this.top + (nSubrs + 1) * subrOffSize - 1;

		int nextTablePtr = this.top + nSubrs * subrOffSize;

		if (isByteArray) {
			if (nextTablePtr < fontDataAsArray.length) // allow for table at end of file
			this.top = subrStart + getWord(fontDataAsArray, nextTablePtr, subrOffSize);
			else this.top = fontDataAsArray.length - 1;
		}
		else {
			if (nextTablePtr < fontDataAsArray.length) // allow for table at end of file
			this.top = subrStart + getWord(fontDataAsObject, nextTablePtr, subrOffSize);
			else this.top = fontDataAsObject.length() - 1;
		}

		int[] subrOffset = new int[nSubrs + 2];
		int ii = subrIdx;
		for (int jj = 0; jj < nSubrs + 1; jj++) {

			if (isByteArray) {
				if ((ii + subrOffSize) < fontDataAsArray.length) subrOffset[jj] = subrStart + getWord(fontDataAsArray, ii, subrOffSize);
			}
			else {
				if ((ii + subrOffSize) < fontDataAsObject.length()) subrOffset[jj] = subrStart + getWord(fontDataAsObject, ii, subrOffSize);
			}

			ii += subrOffSize;
		}
		subrOffset[nSubrs + 1] = this.top;

		this.glyphs.setLocalBias(calculateSubroutineBias(nSubrs));

		// read the glyphs and store
		int current = subrOffset[0];

		for (int jj = 1; jj < nSubrs + 1; jj++) {

			// skip if out of bounds
			if (current == 0 || subrOffset[jj] > fontDataAsArray.length || subrOffset[jj] < 0 || subrOffset[jj] == 0) continue;

			ByteArrayOutputStream nextSubr = new ByteArrayOutputStream();

			for (int c = current; c < subrOffset[jj]; c++) {
				if (!isByteArray && c < fontDataAsObject.length()) nextSubr.write(fontDataAsObject.getByte(c));

			}

			if (isByteArray) {

				int length = subrOffset[jj] - current;

				if (length > 0) {
					byte[] nextSub = new byte[length];

					System.arraycopy(fontDataAsArray, current, nextSub, 0, length);

					this.glyphs.setCharString("subrs" + (jj - 1), nextSub, jj);
				}
			}
			else {
				nextSubr.close();

				this.glyphs.setCharString("subrs" + (jj - 1), nextSubr.toByteArray(), jj);
			}
			current = subrOffset[jj];

		}
	}

	private final void readGlyphs(byte[] fontDataAsArray, FontData fontDataAsObject, int nGlyphs, int[] names) throws Exception {

		boolean isByteArray = fontDataAsArray != null;

		int glyphOffSize;

		if (isByteArray) glyphOffSize = fontDataAsArray[this.top + 2];
		else glyphOffSize = fontDataAsObject.getByte(this.top + 2);

		this.top += 3;
		int glyphIdx = this.top;
		int glyphStart = this.top + (nGlyphs + 1) * glyphOffSize - 1;

		if (isByteArray) this.top = glyphStart + getWord(fontDataAsArray, this.top + nGlyphs * glyphOffSize, glyphOffSize);
		else this.top = glyphStart + getWord(fontDataAsObject, this.top + nGlyphs * glyphOffSize, glyphOffSize);

		int[] glyphoffset = new int[nGlyphs + 2];

		int ii = glyphIdx;

		// read the offsets
		for (int jj = 0; jj < nGlyphs + 1; jj++) {

			if (isByteArray) glyphoffset[jj] = glyphStart + getWord(fontDataAsArray, ii, glyphOffSize);
			else glyphoffset[jj] = glyphStart + getWord(fontDataAsObject, ii, glyphOffSize);

			ii = ii + glyphOffSize;

		}

		glyphoffset[nGlyphs + 1] = this.top;

		// read the glyphs and store
		int current = glyphoffset[0];
		String glyphName;
		byte[] nextGlyph;
		for (int jj = 1; jj < nGlyphs + 1; jj++) {

			nextGlyph = new byte[glyphoffset[jj] - current]; // read name of glyph

			// get data for the glyph
			for (int c = current; c < glyphoffset[jj]; c++) {

				if (isByteArray) nextGlyph[c - current] = fontDataAsArray[c];
				else nextGlyph[c - current] = fontDataAsObject.getByte(c);
			}

			if (this.isCID) {
				glyphName = String.valueOf(names[jj - 1]);
			}
			else {

				if (isByteArray) glyphName = getString(fontDataAsArray, names[jj - 1], this.stringIdx, this.stringStart, this.stringOffSize);
				else glyphName = getString(fontDataAsObject, names[jj - 1], this.stringIdx, this.stringStart, this.stringOffSize);
			}
			if (debugFont) System.out.println("glyph= " + glyphName + " start=" + current + " length=" + glyphoffset[jj] + " isCID=" + this.isCID);

			this.glyphs.setCharString(glyphName, nextGlyph, jj);

			current = glyphoffset[jj];

			if (this.trackIndices) {
				this.glyphs.setIndexForCharString(jj, glyphName);

			}
		}
	}

	private static final int calculateSubroutineBias(int subroutineCount) {
		int bias;
		if (subroutineCount < 1240) {
			bias = 107;
		}
		else
			if (subroutineCount < 33900) {
				bias = 1131;
			}
			else {
				bias = 32768;
			}
		return bias;
	}

	private final void readGlobalSubRoutines(byte[] fontDataAsArray, FontData fontDataAsObject) throws Exception {

		boolean isByteArray = (fontDataAsArray != null);

		int subOffSize, count;

		if (isByteArray) {
			subOffSize = (fontDataAsArray[this.top + 2] & 0xff);
			count = getWord(fontDataAsArray, this.top, 2);
		}
		else {
			subOffSize = (fontDataAsObject.getByte(this.top + 2) & 0xff);
			count = getWord(fontDataAsObject, this.top, 2);
		}

		this.top += 3;
		if (count > 0) {

			int idx = this.top;
			int start = this.top + (count + 1) * subOffSize - 1;
			if (isByteArray) this.top = start + getWord(fontDataAsArray, this.top + count * subOffSize, subOffSize);
			else this.top = start + getWord(fontDataAsObject, this.top + count * subOffSize, subOffSize);

			int[] offset = new int[count + 2];

			int ii = idx;

			// read the offsets
			for (int jj = 0; jj < count + 1; jj++) {

				if (isByteArray) offset[jj] = start + getWord(fontDataAsArray, ii, subOffSize);
				else offset[jj] = start + getWord(fontDataAsObject, ii, subOffSize);

				ii = ii + subOffSize;

			}

			offset[count + 1] = this.top;

			this.glyphs.setGlobalBias(calculateSubroutineBias(count));

			// read the subroutines and store
			int current = offset[0];
			for (int jj = 1; jj < count + 1; jj++) {

				ByteArrayOutputStream nextStream = new ByteArrayOutputStream();
				for (int c = current; c < offset[jj]; c++) {
					if (isByteArray) nextStream.write(fontDataAsArray[c]);
					else nextStream.write(fontDataAsObject.getByte(c));
				}
				nextStream.close();

				// store
				this.glyphs.setCharString("global" + (jj - 1), nextStream.toByteArray(), jj);

				// setGlobalSubroutine(new Integer(jj-1+bias),nextStream.toByteArray());
				current = offset[jj];

			}
		}
	}

	private void decodeDictionary(byte[] fontDataAsArray, FontData fontDataAsObject, int dicStart, int dicEnd, String[] strings) {

		boolean fdReset = false;

		if (debugDictionary) System.out.println("=============Read dictionary====================" + getBaseFontName());

		boolean isByteArray = fontDataAsArray != null;

		int p = dicStart, nextVal, key;
		int i = 0;
		double[] op = new double[48]; // current operand in dictionary

		while (p < dicEnd) {

			if (isByteArray) nextVal = fontDataAsArray[p] & 0xFF;
			else nextVal = fontDataAsObject.getByte(p) & 0xFF;

			if (nextVal <= 27 || nextVal == 31) { // operator

				key = nextVal;

				p++;

				if (debugDictionary && key != 12) System.out.println(key + " (1) " + OneByteCCFDict[key]);

				if (key == 0x0c) { // handle escaped keys

					if (isByteArray) key = fontDataAsArray[p] & 0xFF;
					else key = fontDataAsObject.getByte(p) & 0xFF;

					if (debugDictionary) System.out.println(key + " (2) " + TwoByteCCFDict[key]);

					p++;

					if (key != 36 && key != 37 && key != 7 && this.FDSelect != -1) {
						if (debugDictionary) {
							System.out.println("Ignored as part of FDArray ");

							for (int ii = 0; ii < 6; ii++)
								System.out.println(op[ii]);
						}
					}
					else
						if (key == 2) { // italic

							this.italicAngle = (int) op[0];
							if (debugDictionary) System.out.println("Italic=" + op[0]);

						}
						else
							if (key == 7) { // fontMatrix
								if (!this.hasFontMatrix) System.arraycopy(op, 0, this.FontMatrix, 0, 6);

								if (debugDictionary) {
									for (int ii = 0; ii < 6; ii++)
										System.out.println(ii + "=" + op[ii] + ' ' + this);
								}

								this.hasFontMatrix = true;
							}
							else
								if (key == 6) {
									if (op[0] > 0) {
										this.blueValues = new int[6];
										for (int z = 0; z < this.blueValues.length; z++) {
											this.blueValues[z] = (int) op[z];
										}
									}
								}
								else
									if (key == 7) {
										if (op[0] > 0) {
											this.otherBlues = new int[6];
											for (int z = 0; z < this.otherBlues.length; z++) {
												this.otherBlues[z] = (int) op[z];
											}
										}

									}
									else
										if (key == 8) {
											this.familyBlues = new int[6];
											for (int z = 0; z < this.familyBlues.length; z++) {
												this.familyBlues[z] = (int) op[z];
											}

										}
										else
											if (key == 9) {
												if (op[0] > 0) {
													this.familyOtherBlues = new int[6];
													for (int z = 0; z < this.familyOtherBlues.length; z++) {
														this.familyOtherBlues[z] = (int) op[z];
													}
												}
											}
											else
												if (key == 10) {
													this.stdHW = (int) op[0];
												}
												else
													if (key == 11) {
														this.stdVW = (int) op[0];
													}

													else
														if (key == 30) { // ROS
															this.ros = (int) op[0];
															this.isCID = true;
															if (debugDictionary) System.out.println(op[0]);
														}
														else
															if (key == 31) { // CIDFontVersion
																this.CIDFontVersion = (int) op[0];
																if (debugDictionary) System.out.println(op[0]);
															}
															else
																if (key == 32) { // CIDFontRevision
																	this.CIDFontRevision = (int) op[0];
																	if (debugDictionary) System.out.println(op[0]);
																}
																else
																	if (key == 33) { // CIDFontType
																		this.CIDFontType = (int) op[0];
																		if (debugDictionary) System.out.println(op[0]);
																	}
																	else
																		if (key == 34) { // CIDcount
																			this.CIDcount = (int) op[0];
																			if (debugDictionary) System.out.println(op[0]);
																		}
																		else
																			if (key == 35) { // UIDBase
																				this.UIDBase = (int) op[0];
																				if (debugDictionary) System.out.println(op[0]);
																			}
																			else
																				if (key == 36) { // FDArray
																					this.FDArray = (int) op[0];
																					if (debugDictionary) System.out.println(op[0]);

																				}
																				else
																					if (key == 37) { // FDSelect
																						this.FDSelect = (int) op[0];

																						fdReset = true;

																						if (debugDictionary) System.out.println(op[0]);
																					}
																					else
																						if (key == 0) { // copyright

																							int id = (int) op[0];
																							if (id > 390) id = id - 390;
																							this.copyright = strings[id];
																							if (debugDictionary) System.out.println("copyright= "
																									+ this.copyright);
																						}
																						else
																							if (key == 21) { // Postscript

																								// postscriptFontName=strings[id];
																								if (debugDictionary) {
																									int id = (int) op[0];
																									if (id > 390) id = id - 390;

																									System.out.println("Postscript= " + strings[id]);
																									System.out.println(TwoByteCCFDict[key] + ' '
																											+ op[0]);
																								}
																							}
																							else
																								if (key == 22) { // BaseFontname

																									// baseFontName=strings[id];
																									if (debugDictionary) {

																										int id = (int) op[0];
																										if (id > 390) id = id - 390;

																										System.out.println("BaseFontname= "
																												+ this.embeddedFontName);
																										System.out.println(TwoByteCCFDict[key] + ' '
																												+ op[0]);
																									}
																								}
																								else
																									if (key == 38) { // fullname

																										// fullname=strings[id];
																										if (debugDictionary) {

																											int id = (int) op[0];
																											if (id > 390) id = id - 390;

																											System.out.println("fullname= "
																													+ strings[id]);
																											System.out.println(TwoByteCCFDict[key]
																													+ ' ' + op[0]);
																										}

																									}
																									else
																										if (debugDictionary) System.out
																												.println(op[0]);

				}
				else {

					if (key == 2) { // fullname

						int id = (int) op[0];
						if (id > 390) id = id - 390;
						this.embeddedFontName = strings[id];
						if (debugDictionary) {
							System.out.println("name= " + this.embeddedFontName);
							System.out.println(OneByteCCFDict[key] + ' ' + op[0]);
						}

					}
					else
						if (key == 3) { // familyname

							// embeddedFamilyName=strings[id];
							if (debugDictionary) {

								int id = (int) op[0];
								if (id > 390) id = id - 390;

								System.out.println("FamilyName= " + this.embeddedFamilyName);
								System.out.println(OneByteCCFDict[key] + ' ' + op[0]);
							}

						}
						else
							if (key == 5) { // fontBBox
								if (debugDictionary) {
									for (int ii = 0; ii < 4; ii++)
										System.out.println(op[ii]);
								}
								for (int ii = 0; ii < 4; ii++) {
									// System.out.println(" "+ii+" "+op[ii]);
									this.FontBBox[ii] = (float) op[ii];
								}

								// hasFontBBox=true;
							}
							else
								if (key == 0x0f) { // charset
									this.charset = (int) op[0];

									if (debugDictionary) System.out.println(op[0]);

								}
								else
									if (key == 0x10) { // encoding
										this.enc = (int) op[0];

										if (debugDictionary) System.out.println(op[0]);

									}
									else
										if (key == 0x11) { // charstrings
											this.charstrings = (int) op[0];

											if (debugDictionary) System.out.println(op[0]);

											// System.out.println("charStrings="+charstrings);
										}
										else
											if (key == 18 && this.glyphs.is1C()) { // readPrivate
												int dictNo = this.currentFD;
												if (dictNo == -1) {
													dictNo = 0;
												}

												this.privateDict[dictNo] = (int) op[1];
												this.privateDictOffset[dictNo] = (int) op[0];

												if (debugDictionary) System.out.println("privateDict=" + op[0] + " Offset=" + op[1]);

											}
											else
												if (key == 20) { // defaultWidthX
													int dictNo = this.currentFD;
													if (dictNo == -1) {
														dictNo = 0;
													}

													this.defaultWidthX[dictNo] = (int) op[0];
													if (this.glyphs instanceof T1Glyphs) ((T1Glyphs) this.glyphs).setWidthValues(this.defaultWidthX,
															this.nominalWidthX);

													if (debugDictionary) System.out.println("defaultWidthX=" + op[0]);

												}
												else
													if (key == 21) { // nominalWidthX
														int dictNo = this.currentFD;
														if (dictNo == -1) {
															dictNo = 0;
														}

														this.nominalWidthX[dictNo] = (int) op[0];
														if (this.glyphs instanceof T1Glyphs) ((T1Glyphs) this.glyphs).setWidthValues(
																this.defaultWidthX, this.nominalWidthX);

														if (debugDictionary) System.out.println("nominalWidthX=" + op[0]);

													}
													else
														if (debugDictionary) {

															// System.out.println(p+" "+key+" "+T1CcharCodes1Byte[key]+" <<<"+op);

															System.out.println("Other value " + key);
															/**
															 * if(op <type1CStdStrings.length) System.out.println(type1CStdStrings[(int)op]); else
															 * if((op-390) <strings.length) System.out.println("interesting key:"+key);
															 */
														}
					// System.out.println(p+" "+key+" "+raw1ByteValues[key]+" <<<"+op);
				}

				i = 0;

			}
			else {

				if (isByteArray) p = this.glyphs.getNumber(fontDataAsArray, p, op, i, false);
				else p = this.glyphs.getNumber(fontDataAsObject, p, op, i, false);

				i++;
			}
		}

		if (debugDictionary) System.out.println("=================================" + getBaseFontName());

		// reset
		if (!fdReset) this.FDSelect = -1;
	}

	private String[] readStringIndex(byte[] fontDataAsArray, FontData fontDataAsObject, int start, int offsize, int count) {

		int nStrings;

		boolean isByteArray = (fontDataAsArray != null);

		if (isByteArray) {
			this.top = start + getWord(fontDataAsArray, this.top + count * offsize, offsize);
			// start of string index
			nStrings = getWord(fontDataAsArray, this.top, 2);
			this.stringOffSize = fontDataAsArray[this.top + 2];
		}
		else {
			this.top = start + getWord(fontDataAsObject, this.top + count * offsize, offsize);
			// start of string index
			nStrings = getWord(fontDataAsObject, this.top, 2);
			this.stringOffSize = fontDataAsObject.getByte(this.top + 2);
		}

		this.top += 3;
		this.stringIdx = this.top;
		this.stringStart = this.top + (nStrings + 1) * this.stringOffSize - 1;

		if (isByteArray) this.top = this.stringStart + getWord(fontDataAsArray, this.top + nStrings * this.stringOffSize, this.stringOffSize);
		else this.top = this.stringStart + getWord(fontDataAsObject, this.top + nStrings * this.stringOffSize, this.stringOffSize);

		int[] offsets = new int[nStrings + 2];
		String[] strings = new String[nStrings + 2];

		int ii = this.stringIdx;
		// read the offsets
		for (int jj = 0; jj < nStrings + 1; jj++) {

			if (isByteArray) offsets[jj] = getWord(fontDataAsArray, ii, this.stringOffSize); // content[ii] & 0xff;
			else offsets[jj] = getWord(fontDataAsObject, ii, this.stringOffSize); // content[ii] & 0xff;
			// getWord(content,ii,stringOffSize);
			ii = ii + this.stringOffSize;

		}

		offsets[nStrings + 1] = this.top - this.stringStart;

		// read the strings
		int current = 0;
		StringBuilder nextString;
		for (int jj = 0; jj < nStrings + 1; jj++) {

			nextString = new StringBuilder(offsets[jj] - current);
			for (int c = current; c < offsets[jj]; c++) {
				if (isByteArray) nextString.append((char) fontDataAsArray[this.stringStart + c]);
				else nextString.append((char) fontDataAsObject.getByte(this.stringStart + c));
			}

			if (debugFont) System.out.println("String " + jj + " =" + nextString);

			strings[jj] = nextString.toString();
			current = offsets[jj];

		}
		return strings;
	}

	/** Utility method used during processing of type1C files */
	static final private String getString(FontData fontDataAsObject, int sid, int idx, int start, int offsize) {

		int len;
		String result;

		if (sid < 391) result = type1CStdStrings[sid];
		else {
			sid -= 391;
			int idx0 = start + getWord(fontDataAsObject, idx + sid * offsize, offsize);
			int idxPtr1 = start + getWord(fontDataAsObject, idx + (sid + 1) * offsize, offsize);
			// System.out.println(sid+" "+idx0+" "+idxPtr1);
			if ((len = idxPtr1 - idx0) > 255) len = 255;

			result = new String(fontDataAsObject.getBytes(idx0, len));
		}
		return result;
	}

	/** Utility method used during processing of type1C files */
	static final private String getString(byte[] fontDataAsArray, int sid, int idx, int start, int offsize) {

		int len;
		String result;

		if (sid < 391) result = type1CStdStrings[sid];
		else {
			sid -= 391;
			int idx0 = start + getWord(fontDataAsArray, idx + sid * offsize, offsize);
			int idxPtr1 = start + getWord(fontDataAsArray, idx + (sid + 1) * offsize, offsize);
			// System.out.println(sid+" "+idx0+" "+idxPtr1);
			if ((len = idxPtr1 - idx0) > 255) len = 255;

			result = new String(fontDataAsArray, idx0, len);

		}
		return result;
	}

	/** get standard charset or extract from type 1C font */
	static final private int[] readCharset(int charset, int nGlyphs, FontData fontDataAsObject, byte[] fontDataAsArray) {

		boolean isByteArray = fontDataAsArray != null;

		int glyphNames[];
		int i, j;

		if (debugFont) System.out.println("charset=" + charset);

		/**
		 * //handle CIDS first if(isCID){ glyphNames = new int[nGlyphs]; glyphNames[0] = 0;
		 * 
		 * for (i = 1; i < nGlyphs; ++i) { glyphNames[i] = i;//getWord(fontData, top, 2); //top += 2; }
		 * 
		 * 
		 * // read appropriate non-CID charset }else
		 */
		if (charset == 0) glyphNames = ISOAdobeCharset;
		else
			if (charset == 1) glyphNames = ExpertCharset;
			else
				if (charset == 2) glyphNames = ExpertSubCharset;
				else {
					glyphNames = new int[nGlyphs + 1];
					glyphNames[0] = 0;
					int top = charset;

					int charsetFormat;

					if (isByteArray) charsetFormat = fontDataAsArray[top++] & 0xff;
					else charsetFormat = fontDataAsObject.getByte(top++) & 0xff;

					if (debugFont) System.out.println("charsetFormat=" + charsetFormat);

					if (charsetFormat == 0) {
						for (i = 1; i < nGlyphs; ++i) {
							if (isByteArray) glyphNames[i] = getWord(fontDataAsArray, top, 2);
							else glyphNames[i] = getWord(fontDataAsObject, top, 2);

							top += 2;
						}

					}
					else
						if (charsetFormat == 1) {

							i = 1;

							int c, nLeft;
							while (i < nGlyphs) {

								if (isByteArray) c = getWord(fontDataAsArray, top, 2);
								else c = getWord(fontDataAsObject, top, 2);
								top += 2;
								if (isByteArray) nLeft = fontDataAsArray[top++] & 0xff;
								else nLeft = fontDataAsObject.getByte(top++) & 0xff;

								for (j = 0; j <= nLeft; ++j)
									glyphNames[i++] = c++;

							}
						}
						else
							if (charsetFormat == 2) {
								i = 1;

								int c, nLeft;

								while (i < nGlyphs) {
									if (isByteArray) c = getWord(fontDataAsArray, top, 2);
									else c = getWord(fontDataAsObject, top, 2);

									top += 2;

									if (isByteArray) nLeft = getWord(fontDataAsArray, top, 2);
									else nLeft = getWord(fontDataAsObject, top, 2);

									top += 2;
									for (j = 0; j <= nLeft; ++j)
										glyphNames[i++] = c++;
								}
							}
				}

		return glyphNames;
	}

	/** Utility method used during processing of type1C files */
	static final private int getWord(FontData fontDataAsObject, int index, int size) {
		int result = 0;
		for (int i = 0; i < size; i++) {
			result = (result << 8) + (fontDataAsObject.getByte(index + i) & 0xff);

		}
		return result;
	}

	/** Utility method used during processing of type1C files */
	static final private int getWord(byte[] fontDataAsArray, int index, int size) {
		int result = 0;
		for (int i = 0; i < size; i++) {
			result = (result << 8) + (fontDataAsArray[index + i] & 0xff);

		}
		return result;
	}

	/**
	 * @return bounding box to highlight
	 */
	@Override
	public Rectangle getBoundingBox() {

		if (this.BBox == null) {
			if (this.isFontEmbedded) this.BBox = new Rectangle((int) this.FontBBox[0], (int) this.FontBBox[1],
					(int) (this.FontBBox[2] - this.FontBBox[0]), (int) (this.FontBBox[3] - this.FontBBox[1])); // To change body of created methods
																												// use File | Settings | File
																												// Templates.
			else this.BBox = super.getBoundingBox();
		}

		return this.BBox;
	}

	/**
	 * @return The Font Dictionary select array
	 */
	public int[] getFDSelect() {
		return this.fdSelect;
	}

	public int[] getRosArray() {
		return this.rosArray;
	}

	public Object getKeyValue(int key) {

		switch (key) {
			case WEIGHT:
				return this.weight;
			case ITALICANGLE:
				return this.italicAngle;
			case FONTMATRIX:
				return this.FontMatrix;
			case FONTBBOX:
				return this.FontBBox;
			case ENCODING:
				return this.enc;

			case DEFAULTWIDTHX:
				return this.defaultWidthX[0];
			case NOMINALWIDTHX:
				return this.nominalWidthX[0];
			case BLUEVALUES:
				return this.blueValues;
			case OTHERBLUES:
				return this.otherBlues;
			case FAMILYBLUES:
				return this.familyBlues;
			case FAMILYOTHERBLUES:
				return this.familyOtherBlues;
			case STDHW:
				return this.stdHW;
			case STDVW:
				return this.stdVW;
			case SUBRS:
				return this.subrs;

			case ROS:
				return this.ros;
			case CIDCOUNT:
				return this.CIDcount;
			case CIDFONTREVISION:
				return this.CIDFontRevision;
			case CIDFONTVERSION:
				return this.CIDFontVersion;
			case CIDFONTTYPE:
				return this.CIDFontType;
			case FDARRAY:
				return this.FDArray;
			case FDSELECT:
				return this.FDSelect;

			default:
				throw new RuntimeException("Key is unknown or value is not yet assigned " + key);
		}
	}

}
