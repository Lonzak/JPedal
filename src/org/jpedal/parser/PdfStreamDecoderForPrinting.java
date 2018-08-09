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
 * PdfStreamDecoderForPrinting.java
 * ---------------
 */

package org.jpedal.parser;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

import org.jpedal.PdfDecoder;
import org.jpedal.external.ColorHandler;
import org.jpedal.external.CustomPrintHintingHandler;
import org.jpedal.external.Options;
import org.jpedal.io.ObjectStore;
import org.jpedal.io.PdfObjectReader;
import org.jpedal.objects.layers.PdfLayerList;
import org.jpedal.render.SwingDisplay;

public class PdfStreamDecoderForPrinting extends PdfStreamDecoder {

	public PdfStreamDecoderForPrinting(PdfObjectReader currentPdfFile, boolean b, PdfLayerList layers) {
		super(currentPdfFile, b, layers);

		this.isPrinting = true;
	}

	public void print(Graphics2D g2, AffineTransform scaling, int currentPrintPage, Rectangle userAnnot,
			CustomPrintHintingHandler customPrintHintingHandler, PdfDecoder pdf) {

		if (customPrintHintingHandler != null) {
			this.current.stopG2HintSetting(true);
			customPrintHintingHandler.preprint(g2, pdf);
		}

		this.current.setPrintPage(currentPrintPage);

		this.current.setCustomColorHandler((ColorHandler) pdf.getExternalHandler(Options.ColorHandler));

		this.current.setG2(g2);
		this.current.paint(null, scaling, userAnnot);
	}

	@Override
	public void setObjectValue(int key, Object obj) {

		if (key == ValueTypes.ObjectStore) {
			this.objectStoreStreamRef = (ObjectStore) obj;

			this.current = new SwingDisplay(this.pageNum, this.objectStoreStreamRef, true);
			this.current.setHiResImageForDisplayMode(this.useHiResImageForDisplay);

			if (this.customImageHandler != null && this.current != null) this.current.setCustomImageHandler(this.customImageHandler);
		}
		else {
			super.setObjectValue(key, obj);
		}
	}
}
