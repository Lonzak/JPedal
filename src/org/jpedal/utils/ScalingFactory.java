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
 * ScalingFactory.java
 * ---------------
 */
package org.jpedal.utils;

import java.awt.geom.AffineTransform;

import org.jpedal.objects.PdfPageData;

/**
 * workout Transformation to use on image
 */
public class ScalingFactory {

	public static AffineTransform getScalingForImage(int pageNumber, int rotation, float scaling, PdfPageData pageData) {

		double mediaX = pageData.getMediaBoxX(pageNumber) * scaling;
		double mediaY = pageData.getMediaBoxY(pageNumber) * scaling;
		// double mediaW = pageData.getMediaBoxWidth(pageNumber)*scaling;
		double mediaH = pageData.getMediaBoxHeight(pageNumber) * scaling;

		double crw = pageData.getCropBoxWidth(pageNumber) * scaling;
		double crh = pageData.getCropBoxHeight(pageNumber) * scaling;
		double crx = pageData.getCropBoxX(pageNumber) * scaling;
		double cry = pageData.getCropBoxY(pageNumber) * scaling;

		// create scaling factor to use
		AffineTransform displayScaling = new AffineTransform();

		// ** new x_size y_size declaration *
		int x_size = (int) (crw + (crx - mediaX));
		int y_size = (int) (crh + (cry - mediaY));

		if (rotation == 270) {

			displayScaling.rotate(-Math.PI / 2.0, x_size / 2, y_size / 2);

			double x_change = (displayScaling.getTranslateX());
			double y_change = (displayScaling.getTranslateY());
			displayScaling.translate((y_size - y_change), -x_change);
			displayScaling.translate(0, y_size);
			displayScaling.scale(1, -1);
			displayScaling.translate(-(crx + mediaX), -(mediaH - crh - (cry - mediaY)));

		}
		else
			if (rotation == 180) {

				displayScaling.rotate(Math.PI, x_size / 2, y_size / 2);
				displayScaling.translate(-(crx + mediaX), y_size + (cry + mediaY) - (mediaH - crh - (cry - mediaY)));
				displayScaling.scale(1, -1);

			}
			else
				if (rotation == 90) {

					displayScaling.rotate(Math.PI / 2.0);
					displayScaling.translate(0, (cry + mediaY) - (mediaH - crh - (cry - mediaY)));
					displayScaling.scale(1, -1);

				}
				else {
					displayScaling.translate(0, y_size);
					displayScaling.scale(1, -1);
					displayScaling.translate(0, -(mediaH - crh - (cry - mediaY)));
				}

		displayScaling.scale(scaling, scaling);

		return displayScaling;
	}
}
