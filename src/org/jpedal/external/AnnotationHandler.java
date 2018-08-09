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
 * AnnotationHandler.java
 * ---------------
 */
package org.jpedal.external;

import java.util.Map;

import org.jpedal.PdfDecoder;
import org.jpedal.io.PdfObjectReader;

public interface AnnotationHandler {

	// <start-adobe><start-thin>
	/** called when each page created */
	public void handleAnnotations(PdfDecoder decode_pdf, Map objs, int p);

	/** called when mouse moves so you can react to context */
	public void checkLinks(Map objs, boolean mouseClicked, PdfObjectReader pdfObjectReader, int x, int y,
			org.jpedal.examples.viewer.gui.SwingGUI currentGUI, org.jpedal.examples.viewer.Values commonValues);

	// <end-thin><end-adobe>
}
