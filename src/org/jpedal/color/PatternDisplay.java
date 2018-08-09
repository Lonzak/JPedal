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
 * PatternDisplay.java
 * ---------------
 */

package org.jpedal.color;

import java.awt.image.BufferedImage;

import org.jpedal.io.ObjectStore;
import org.jpedal.objects.GraphicsState;
import org.jpedal.render.DynamicVectorRenderer;
import org.jpedal.render.T3Display;
import org.jpedal.render.T3Renderer;

public class PatternDisplay extends T3Display implements T3Renderer {

	private BufferedImage lastImg;

	private int imageCount = 0;

	public PatternDisplay(int i, boolean b, int j, ObjectStore localStore) {
		super(i, b, j, localStore);

		this.type = DynamicVectorRenderer.CREATE_PATTERN;
	}

	/* save image in array to draw */
	@Override
	public int drawImage(int pageNumber, BufferedImage image, GraphicsState currentGraphicsState, boolean alreadyCached, String name,
			int optionsApplied, int previousUse) {

		this.lastImg = image;

		this.imageCount++;

		return super.drawImage(pageNumber, image, currentGraphicsState, alreadyCached, name, optionsApplied, previousUse);
	}

	@Override
	public BufferedImage getSingleImagePattern() {
		if (this.imageCount != 1) return null;
		else return this.lastImg;
	}
}
