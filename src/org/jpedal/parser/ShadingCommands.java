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
 * ShadingCommands.java
 * ---------------
 */
package org.jpedal.parser;

import java.awt.Rectangle;
import java.awt.Shape;
import java.util.Map;

import org.jpedal.color.ColorspaceFactory;
import org.jpedal.color.GenericColorSpace;
import org.jpedal.color.PdfPaint;
import org.jpedal.io.PdfObjectReader;
import org.jpedal.objects.GraphicsState;
import org.jpedal.objects.PdfPageData;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.render.DynamicVectorRenderer;
import org.jpedal.utils.LogWriter;

public class ShadingCommands {

	public static void sh(String shadingObject, PdfObjectCache cache, GraphicsState gs, boolean isPrinting, Map shadingColorspacesObjects,
			int pageNum, PdfObjectReader currentPdfFile, PdfPageData pageData, DynamicVectorRenderer current) {

		PdfObject Shading = (PdfObject) cache.localShadings.get(shadingObject);
		if (Shading == null) {
			Shading = (PdfObject) cache.globalShadings.get(shadingObject);
		}

		// workout shape
		Shape shadeShape = null;
		/**
		 * if(gs.CTM!=null){ int x=(int)gs.CTM[2][0]; int y=(int)gs.CTM[2][1]; int w=(int)gs.CTM[0][0]; if(w==0){ w=(int)gs.CTM[1][0]; } if(w<0) w=-w;
		 * 
		 * int h=(int)gs.CTM[1][1]; if(h==0) h=(int)gs.CTM[0][1]; if(h<0) h=-h; shadeShape=new Rectangle(x,y,w,h); }/
		 **/
		if (shadeShape == null) shadeShape = gs.getClippingShape();

		if (shadeShape == null && gs.CTM != null && gs.CTM[0][1] > 0 && gs.CTM[0][0] == 0 && gs.CTM[1][1] == 0) { // special case

			double x = gs.CTM[2][0];
			double y = gs.CTM[2][1];
			double w = gs.CTM[0][0];
			if (w == 0) w = gs.CTM[0][1];
			if (w < 0) w = -w;
			double h = gs.CTM[1][1];
			if (h == 0) h = gs.CTM[1][0];
			if (h < 0) h = -h;

			// don't understand but works on example I have!
			if (gs.CTM[1][0] < 0) {
				x = x + gs.CTM[1][0];
				x = -x;
				w = gs.CTM[2][0] - x;
			}
			shadeShape = new Rectangle.Double(x, y, w, h);
		}

		/**
		 * corner case for odd rotated shading
		 */
		if (shadeShape == null && gs.CTM[0][1] < 0 && gs.CTM[1][0] < 0) {
			double x = -gs.CTM[0][1];
			double y = gs.CTM[2][1] + gs.CTM[1][0];
			double w = gs.CTM[2][0] - x;
			double h = -gs.CTM[1][0];

			shadeShape = new Rectangle.Double(x, y, w, h);
			// System.out.println(">>"+tokenNumber+" "+shadingObject+" "+gs.getClippingShape());
		}

		/**
		 * corner case for odd rotated shading
		 */
		if (shadeShape == null && gs.CTM[0][0] > 0 && gs.CTM[1][1] < 0) {
			double x = gs.CTM[2][0];
			double h = gs.CTM[1][1];
			double y = gs.CTM[2][1];
			double w = gs.CTM[0][0];

			shadeShape = new Rectangle.Double(x, y, w, h);
		}

		/**
		 * corner case for odd rotated shading
		 */
		if (shadeShape == null && gs.CTM[0][0] < 0 && gs.CTM[1][1] > 0) {
			double x = gs.CTM[2][0];
			double h = gs.CTM[1][1];
			double y = gs.CTM[2][1];
			double w = gs.CTM[0][0];

			shadeShape = new Rectangle.Double(x, y, w, h);
			// System.out.println(">>"+shadeShape.getBounds());
		}

		if (shadeShape == null) shadeShape = new Rectangle(pageData.getMediaBoxX(pageNum), pageData.getMediaBoxY(pageNum),
				pageData.getMediaBoxWidth(pageNum), pageData.getMediaBoxHeight(pageNum));

		/**
		 * generate the appropriate shading and then colour in the current clip with it
		 */
		try {

			/**
			 * workout colorspace
			 **/
			PdfObject ColorSpace = Shading.getDictionary(PdfDictionary.ColorSpace);

			GenericColorSpace newColorSpace = ColorspaceFactory.getColorSpaceInstance(currentPdfFile, ColorSpace, shadingColorspacesObjects);

			newColorSpace.setPrinting(isPrinting);

			/** setup shading object */

			PdfPaint shading = null;

			if (shading != null) {
				/**
				 * shade the current clip
				 */
				gs.setFillType(GraphicsState.FILL);
				gs.setNonstrokeColor(shading);

				// track colorspace use
				cache.put(PdfObjectCache.ColorspacesUsed, newColorSpace.getID(), "x");

				current.drawShape(shadeShape, gs, Cmd.F);
			}
		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}
	}
}
