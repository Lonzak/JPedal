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
 * ValueTypes.java
 * ---------------
 */
package org.jpedal.parser;

/**
 * Contains static flags we use for identifying all the object types in parser package
 */
public class ValueTypes {

	public static final int UNSET = 0;
	public static final int PATTERN = 1;
	public static final int FORM = 1;

	final public static int EmbeddedFonts = -1;
	final public static int StructuredContent = -2;
	final public static int StatusBar = -3;
	final public static int PdfLayerList = -4;
	final public static int MarkedContent = -5;
	final public static int ImageHandler = -6;
	final public static int DirectRendering = -7;
	final public static int ObjectStore = -8;
	final public static int Name = -9;
	final public static int PageNum = -10;
	final public static int GenerateGlyphOnRender = -11;
	final public static int StreamType = -12;

	final public static int TextPrint = -15;
	final public static int XFormFlattening = -16;

	final public static int PDFPageData = -18;
	final public static int PDFData = -19;
	final public static int PDFImages = -20;
	final public static int TextAreas = -21;
	final public static int TextDirections = 22;
	final public static int DynamicVectorRenderer = 23;
	final public static int HTMLInvisibleTextHandler = 24;

}
