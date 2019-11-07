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
 * TrueType.java
 * ---------------
 */
package org.jpedal.fonts;

import java.awt.Rectangle;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.jpedal.exception.PdfFontException;
import org.jpedal.external.ExternalHandlers;
import org.jpedal.fonts.glyph.PdfJavaGlyphs;
import org.jpedal.fonts.objects.FontData;
import org.jpedal.fonts.tt.TTGlyphs;
import org.jpedal.io.ObjectStore;
import org.jpedal.io.PdfObjectReader;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.utils.LogWriter;

/**
 * handles truetype specifics
 * */
public class TrueType extends PdfFont {

	private static final long serialVersionUID = -7908948398561222878L;
	private boolean subfontAlreadyLoaded;
	private Map fontsLoaded;

	private Rectangle BBox = null;

	TrueType() {
	}

	private void readFontData(byte[] fontDataAsArray, FontData fontData) {

		if (this.subfontAlreadyLoaded) {
			this.glyphs = (PdfJavaGlyphs) this.fontsLoaded.get(this.substituteFont + '_' + this.glyphs.getBaseFontName() + ' '
					+ fontDataAsArray.length);

			this.fontTypes = this.glyphs.getType();
		}
		else {

			if (!this.isCIDFont) {
				if (fontDataAsArray != null) {
					this.fontsLoaded.put(this.substituteFont + '_' + this.glyphs.getBaseFontName() + ' ' + fontDataAsArray.length, this.glyphs);
				}
			}

			this.fontTypes = this.glyphs.readEmbeddedFont(this.TTstreamisCID, fontDataAsArray, fontData);
		}
		// does not see to be accurate on all PDFs tested
		// this.FontBBox=glyphs.getFontBoundingBox();
	}

	/**
	 * allows us to substitute a font to use for display
	 * 
	 * @throws PdfFontException
	 */
	protected void substituteFontUsed(String substituteFontFile) throws PdfFontException {

		InputStream from = null;

		// process the font data
		try {

			// try in jar first
			from = this.loader.getResourceAsStream("org/jpedal/res/fonts/" + substituteFontFile);

			// try as straight file
			if (from == null) from = new FileInputStream(substituteFontFile);

		}
		catch (Exception e) {
			System.err.println("Exception " + e + " reading " + substituteFontFile + " Check cid  jar installed");
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " reading " + substituteFontFile + " Check cid  jar installed");

			if (ExternalHandlers.throwMissingCIDError && e.getMessage().contains("kochi")) throw new Error(e);
		}

		if (from == null) throw new PdfFontException("Unable to load font " + substituteFontFile);

		try {

			// create streams
			ByteArrayOutputStream to = new ByteArrayOutputStream();

			// write
			byte[] buffer = new byte[65535];
			int bytes_read;
			while ((bytes_read = from.read(buffer)) != -1)
				to.write(buffer, 0, bytes_read);

			to.close();
			from.close();

			FontData fontData = null;// new FontData(to.toByteArray());

			readFontData(to.toByteArray(), fontData);

			this.glyphs.setEncodingToUse(this.hasEncoding, this.getFontEncoding(false), true, this.isCIDFont);

			this.isFontEmbedded = true;

		}
		catch (Exception e) {
			System.err.println("Exception " + e + " reading " + substituteFontFile + " Check cid  jar installed");

			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " reading " + substituteFontFile + " Check cid  jar installed");

			if (ExternalHandlers.throwMissingCIDError && e.getMessage().contains("kochi")) throw new Error(e);
		}
	}

	public TrueType(byte[] rawFontData, PdfJavaGlyphs glyphs) {

		this.fontsLoaded = new HashMap();

		init(null);

		// this.substituteFont=substituteFont;

		// this.rawFontData=rawFontData;
	}

	/** entry point when using generic renderer */
	public TrueType(String substituteFont) {

		this.glyphs = new TTGlyphs();

		this.fontsLoaded = new HashMap();

		init(null);

		this.substituteFont = substituteFont;
	}

	/** get handles onto Reader so we can access the file */
	public TrueType(PdfObjectReader current_pdf_file, String substituteFont) {

		this.glyphs = new TTGlyphs();

		init(current_pdf_file);
		this.substituteFont = substituteFont;
	}

	/** read in a font and its details from the pdf file */
	@Override
	public void createFont(PdfObject pdfObject, String fontID, boolean renderPage, ObjectStore objectStore, Map substitutedFonts) throws Exception {

		this.fontTypes = StandardFonts.TRUETYPE;

		this.fontsLoaded = substitutedFonts;

		// generic setup
		init(fontID, renderPage);

		/**
		 * get FontDescriptor object - if present contains metrics on glyphs
		 */
		PdfObject pdfFontDescriptor = pdfObject.getDictionary(PdfDictionary.FontDescriptor);

		setBoundsAndMatrix(pdfFontDescriptor);

		setName(pdfObject, fontID);
		setEncoding(pdfObject, pdfFontDescriptor);

		if (renderPage) {

			if (pdfFontDescriptor != null && this.substituteFont == null) {

				byte[] stream = null;
				PdfObject FontFile2 = pdfFontDescriptor.getDictionary(PdfDictionary.FontFile2);

				// allow for wrong types used (Acrobat does not care so neither do we)
				if (FontFile2 == null) {
					FontFile2 = pdfFontDescriptor.getDictionary(PdfDictionary.FontFile);

					if (FontFile2 == null) {
						FontFile2 = pdfFontDescriptor.getDictionary(PdfDictionary.FontFile3);
					}
				}

				if (FontFile2 != null) {
					stream = this.currentPdfFile.readStream(FontFile2, true, true, false, false, false,
							FontFile2.getCacheName(this.currentPdfFile.getObjectReader()));
				}

				if (stream != null) {
					readEmbeddedFont(stream, null, this.hasEncoding, false);
				}
			}

			if (!this.isFontEmbedded && this.substituteFont != null) {

				// over-ride font remapping if substituted
				if (this.glyphs.remapFont) this.glyphs.remapFont = false;

				this.subfontAlreadyLoaded = !this.isCIDFont
						&& this.fontsLoaded.containsKey(this.substituteFont + '_' + this.glyphs.getBaseFontName());

				File fontFile;
				FontData fontData = null;
				int objSize = 0;

				/**
				 * see if we cache or read
				 */
				if (!this.subfontAlreadyLoaded) {
					fontFile = new File(this.substituteFont);

					objSize = (int) fontFile.length();
				}

				if (FontData.maxSizeAllowedInMemory >= 0 && objSize > FontData.maxSizeAllowedInMemory) {

					if (!this.subfontAlreadyLoaded) fontData = new FontData(this.substituteFont);

					readEmbeddedFont(null, fontData, false, true);
				}
				else
					if (this.subfontAlreadyLoaded) {
						readEmbeddedFont(null, null, false, true);
					}
					else {

						// read details
						BufferedInputStream from;

						InputStream jarFile = null;
						try {
							if (this.substituteFont.startsWith("jar:") || this.substituteFont.startsWith("http:")) jarFile = this.loader
									.getResourceAsStream(this.substituteFont);
							else jarFile = this.loader.getResourceAsStream("file:///" + this.substituteFont);

						}
						catch (Exception e) {
							if (LogWriter.isOutput()) LogWriter.writeLog("1.Unable to open " + this.substituteFont);
						}
						catch (Error err) {
							if (LogWriter.isOutput()) LogWriter.writeLog("1.Unable to open " + this.substituteFont);
						}

						if (jarFile == null) from = new BufferedInputStream(new FileInputStream(this.substituteFont));
						else from = new BufferedInputStream(jarFile);

						// create streams
						ByteArrayOutputStream to = new ByteArrayOutputStream();

						// write
						byte[] buffer = new byte[65535];
						int bytes_read;
						while ((bytes_read = from.read(buffer)) != -1)
							to.write(buffer, 0, bytes_read);

						to.close();
						from.close();

						readEmbeddedFont(to.toByteArray(), null, false, true);
					}

				this.isFontSubstituted = true;

			}
		}

		readWidths(pdfObject, true);

		// make sure a font set
		if (renderPage) setFont(this.glyphs.fontName, 1);

		this.glyphs.setDiffValues(this.diffTable);
	}

	/** read in a font and its details for generic usage */
	@Override
	public void createFont(String fontName) throws Exception {

		this.fontTypes = StandardFonts.TRUETYPE;

		setBaseFontName(fontName);

		/**
		 * see if we cache or read
		 */
		File fontFile = new File(this.substituteFont);

		int objSize = (int) fontFile.length();

		if (FontData.maxSizeAllowedInMemory >= 0 && objSize > FontData.maxSizeAllowedInMemory) {
			FontData fontData = new FontData(this.substituteFont);

			readEmbeddedFont(null, fontData, false, true);
		}
		else {
			// read details
			BufferedInputStream from;

			InputStream jarFile = null;
			try {
				if (this.substituteFont.startsWith("jar:") || this.substituteFont.startsWith("http:")) jarFile = this.loader
						.getResourceAsStream(this.substituteFont);
				else jarFile = this.loader.getResourceAsStream("file:///" + this.substituteFont);

			}
			catch (Exception e) {
				if (LogWriter.isOutput()) LogWriter.writeLog("2.Unable to open " + this.substituteFont);
			}
			catch (Error err) {
				if (LogWriter.isOutput()) LogWriter.writeLog("2.Unable to open " + this.substituteFont);
			}

			if (jarFile == null) from = new BufferedInputStream(new FileInputStream(this.substituteFont));
			else from = new BufferedInputStream(jarFile);

			// create streams
			ByteArrayOutputStream to = new ByteArrayOutputStream();

			// write
			byte[] buffer = new byte[65535];
			int bytes_read;
			while ((bytes_read = from.read(buffer)) != -1)
				to.write(buffer, 0, bytes_read);

			to.close();
			from.close();

			readEmbeddedFont(to.toByteArray(), null, false, true);
		}

		this.isFontSubstituted = true;

		this.glyphs.setDiffValues(this.diffTable);
	}

	/**
	 * read truetype font data and also install font onto System so we can use
	 */
	final protected void readEmbeddedFont(byte[] fontDataAsArray, FontData fontDataAsObject, boolean hasEncoding, boolean isSubstituted) {

		// process the font data
		try {

			if (LogWriter.isOutput()) LogWriter.writeLog("Embedded TrueType font used");

			readFontData(fontDataAsArray, fontDataAsObject);

			this.isFontEmbedded = true;

			this.glyphs.setFontEmbedded(true);

			this.glyphs.setEncodingToUse(hasEncoding, this.getFontEncoding(false), isSubstituted, this.TTstreamisCID);

		}
		catch (Exception e) {

			this.isFontEmbedded = false;

			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " processing TrueType font");
		}
	}

	/**
	 * @return bounding box to highlight
	 */
	@Override
	public Rectangle getBoundingBox() {

		if (this.BBox == null) {
			if (this.isFontEmbedded && !this.isFontSubstituted) this.BBox = new Rectangle((int) this.FontBBox[0], (int) this.FontBBox[1],
					(int) (this.FontBBox[2] - this.FontBBox[0]), (int) (this.FontBBox[3] - this.FontBBox[1])); // To change body of created methods
																												// use File | Settings | File
																												// Templates.
			else this.BBox = super.getBoundingBox();
		}

		return this.BBox;
	}
}
