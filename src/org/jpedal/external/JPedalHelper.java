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
 * JPedalHelper.java
 * ---------------
 */
package org.jpedal.external;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.jpedal.color.PdfPaint;
import org.jpedal.fonts.PdfFont;
import org.jpedal.fonts.glyph.PdfJavaGlyphs;

public interface JPedalHelper {

	// Allow user to replace setFont Method in PdfJavaGlyphs
	public Font setFont(PdfJavaGlyphs pdfJavaGlyphs, String name, int size);

	public Font getJavaFontX(PdfFont pdfFont, int size);

	// allow user to control how color set when rendering (ie to produce grayscale or bw)
	// Look at org.jpedal.external.ColorHandler for a multithreaded solution
	public void setPaint(Graphics2D g2, PdfPaint textFillCol, int pageNumber, boolean isPrinting);

	// allow user to process images before drawn (ie to convert to bw or grayscale)
	// Look at org.jpedal.external.ColorHandler for a multithreaded solution
	public BufferedImage processImage(BufferedImage image, int pageNumber, boolean isPrinting);
}
