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
 * CIDFontType2.java
 * ---------------
 */
package org.jpedal.fonts;

import java.util.Map;

import org.jpedal.fonts.tt.TTGlyphs;
import org.jpedal.io.ObjectStore;
import org.jpedal.io.PdfObjectReader;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.parser.PdfFontFactory;

/**
 * handles truetype specifics
 * */
public class CIDFontType2 extends TrueType {

	private static final long serialVersionUID = 4982210412457721612L;

	/** get handles onto Reader so we can access the file */
	public CIDFontType2(PdfObjectReader currentPdfFile, String substituteFontFile) {

		this.isCIDFont = true;
		this.TTstreamisCID = true;

		this.glyphs = new TTGlyphs();

		init(currentPdfFile);

		this.substituteFontFile = substituteFontFile;
	}

	/** get handles onto Reader so we can access the file */
	public CIDFontType2(PdfObjectReader currentPdfFile, boolean ttflag) {

		this.isCIDFont = true;
		this.TTstreamisCID = ttflag;

		this.glyphs = new TTGlyphs();

		init(currentPdfFile);
	}

	/** read in a font and its details from the pdf file */
	@Override
	public void createFont(PdfObject pdfObject, String fontID, boolean renderPage, ObjectStore objectStore, Map substitutedFonts) throws Exception {

		this.fontTypes = StandardFonts.CIDTYPE2;
		this.fontID = fontID;

		PdfObject Descendent = pdfObject.getDictionary(PdfDictionary.DescendantFonts);
		PdfObject pdfFontDescriptor = Descendent.getDictionary(PdfDictionary.FontDescriptor);

		createCIDFont(pdfObject, Descendent);

		if (pdfFontDescriptor != null) {

			byte[] stream;
			PdfObject FontFile2 = pdfFontDescriptor.getDictionary(PdfDictionary.FontFile2);
			if (FontFile2 != null) {
				stream = this.currentPdfFile.readStream(FontFile2, true, true, false, false, false,
						FontFile2.getCacheName(this.currentPdfFile.getObjectReader()));

				if (stream != null) readEmbeddedFont(stream, null, this.hasEncoding, false);
			}
		}

		// allow for corrupted
		boolean isCorrupt = this.glyphs.isCorrupted();

		if (this.glyphs.isCorrupted()) {

			PdfFontFactory pdfFontFactory = new PdfFontFactory(this.currentPdfFile);
			pdfFontFactory.getFontSub(getBaseFontName());
			this.isFontEmbedded = false;

			this.substituteFontFile = pdfFontFactory.getMapFont();

		}

		// setup and substitute font
		if (renderPage && !this.isFontEmbedded && this.substituteFontFile != null) {
			this.substituteFontUsed(this.substituteFontFile);
			this.isFontSubstituted = true;
			this.isFontEmbedded = true;

			this.glyphs.setFontEmbedded(true);
		}

		// make sure a font set
		if (renderPage) setFont(getBaseFontName(), 1);

		if (!this.isFontEmbedded) selectDefaultFont();

		this.glyphs.setCorrupted(isCorrupt);
	}
}
