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
 * GenericFormFactory.java
 * ---------------
 */

package org.jpedal.objects.acroforms.creation;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ByteLookupTable;
import java.awt.image.LookupOp;
import java.util.EnumSet;

import org.jpedal.PdfDecoder;
import org.jpedal.objects.acroforms.actions.ActionHandler;

public class GenericFormFactory {

	/**
	 * handle on AcroRenderer needed for adding mouse listener
	 */
	protected ActionHandler formsActionHandler;

	/* handle on PdfDecoder to alow us to use it */
	protected PdfDecoder decode_pdf;

	public void setDecoder(PdfDecoder pdfDecoder) {
		this.decode_pdf = pdfDecoder;
	}

	protected BufferedImage invertImage(BufferedImage image) {
		if (image == null) return null;

		BufferedImage ret = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());

		byte reverse[] = new byte[256];
		for (int j = 0; j < 200; j++) {
			reverse[j] = (byte) (256 - j);
		}

		ByteLookupTable blut = new ByteLookupTable(0, reverse);
		LookupOp lop = new LookupOp(blut, null);
		lop.filter(image, ret);

		return ret;
	}

	/**
	 * create a pressed look of the <b>image</b> and return it
	 */
	protected BufferedImage createPressedLook(Image image) {

		if (image == null) return null;

		BufferedImage pressedImage = new BufferedImage(image.getWidth(null) + 2, image.getHeight(null) + 2, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) pressedImage.getGraphics();
		g.drawImage(image, 1, 1, null);
		g.dispose();
		return pressedImage;
	}

	/**
	 * does nothing (overriden by HTML implementation)
	 */
	public void indexAllKids() {
	}

	public void setOptions(EnumSet formSettings) {
		throw new RuntimeException("setOptions(EnumSet formSettings) called in GenericFormFactory - not implemented in " + this);
	}
}
