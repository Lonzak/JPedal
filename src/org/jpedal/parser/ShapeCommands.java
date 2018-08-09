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
 * ShapeCommands.java
 * ---------------
 */

package org.jpedal.parser;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.geom.Area;

import org.jpedal.color.ColorSpaces;
import org.jpedal.io.PdfArray;
import org.jpedal.objects.GraphicsState;
import org.jpedal.objects.PdfPageData;
import org.jpedal.objects.PdfShape;
import org.jpedal.render.DynamicVectorRenderer;

public class ShapeCommands {

	static Shape B(boolean isStar, boolean isLowerCase, GraphicsState gs, int formLevel, PdfShape currentDrawShape, LayerDecoder layerDecoder,
			boolean renderPage, DynamicVectorRenderer current) {

		Shape currentShape = null;

		if (layerDecoder.isLayerVisible()) {
			// set Winding rule
			if (isStar) currentDrawShape.setEVENODDWindingRule();
			else currentDrawShape.setNONZEROWindingRule();

			// close for s command
			if (isLowerCase) currentDrawShape.closeShape();

			currentShape = currentDrawShape.generateShapeFromPath(gs.CTM, gs.getLineWidth(), Cmd.B, current.getType());

			// hack which fixes blocky text on Customers3/demo_3.pdf
			if (currentShape != null && currentShape.getBounds2D().getWidth() < 1 && currentShape.getBounds2D().getHeight() < 1) {
				return null;
			}

			if (!isLowerCase && formLevel > 0 && currentShape != null && gs.getClippingShape() != null
					&& gs.nonstrokeColorSpace.getID() == ColorSpaces.DeviceCMYK && gs.nonstrokeColorSpace.getColor().getRGB() == -1) {

				// System.out.println(currentShape.getPathIterator(null).)
				Area a = gs.getClippingShape();
				a.subtract(new Area(currentShape));
				currentShape = a;

			}

			// save for later
			if (renderPage && currentShape != null) {

				gs.setStrokeColor(gs.strokeColorSpace.getColor());
				gs.setNonstrokeColor(gs.nonstrokeColorSpace.getColor());

				if (gs.nonstrokeColorSpace.getColor().getRGB() == -16777216 && (gs.getAlpha(GraphicsState.STROKE) == 0)) {
					gs.setFillType(GraphicsState.STROKE);
				}
				else gs.setFillType(GraphicsState.FILLSTROKE);

				current.drawShape(currentShape, gs, Cmd.B);

			}
		}
		// always reset flag
		currentDrawShape.setClip(false);

		currentDrawShape.resetPath(); // flush all path ops stored

		return currentShape;
	}

	static void D(CommandParser parser, GraphicsState gs) {

		String values = ""; // used to combine values

		// and the dash array
		int items = parser.getOperandCount();

		if (items == 1) values = parser.generateOpAsString(0, false);
		else {
			// concat values
			// StringBuilder list = new StringBuilder(15);
			for (int i = items - 1; i > -1; i--) {
				values += (parser.generateOpAsString(i, false));
				values += (' ');
			}
			// values=list.toString();
		}

		// allow for default
		if ((values.equals("[ ] 0 ")) || (values.equals("[]0")) || (values.equals("[] 0 "))) {
			gs.setDashPhase(0);
			gs.setDashArray(new float[0]);
		}
		else {

			// get dash pattern
			int pointer = values.indexOf(']');

			String dash = values.substring(0, pointer);
			int phase = (int) Float.parseFloat(values.substring(pointer + 1, values.length()).trim());

			// put into dash array
			float[] dash_array = PdfArray.convertToFloatArray(dash);

			for (int aa = 0; aa < dash_array.length; aa++) {
				// System.out.println(aa+" "+dash_array[aa]);

				if (dash_array[aa] < 0.001) dash_array[aa] = 0;
			}
			// put array into global value
			gs.setDashArray(dash_array);

			// last value is phase
			gs.setDashPhase(phase);

		}
	}

	// private void I() {
	// if (currentToken.equals("i")) {
	// int value =
	// (int) Float.parseFloat((String) operand.elementAt(0));

	// set value
	// currentGraphicsState.setFlatness(value);
	// }
	// }

	static void J(boolean isLowerCase, int value, GraphicsState gs) {

		int style = 0;
		if (!isLowerCase) {

			// map join style
			if (value == 0) style = BasicStroke.JOIN_MITER;
			if (value == 1) style = BasicStroke.JOIN_ROUND;
			if (value == 2) style = BasicStroke.JOIN_BEVEL;

			// set value
			gs.setJoinStyle(style);
		}
		else {
			// map cap style
			if (value == 0) style = BasicStroke.CAP_BUTT;
			if (value == 1) style = BasicStroke.CAP_ROUND;
			if (value == 2) style = BasicStroke.CAP_SQUARE;

			// set value
			gs.setCapStyle(style);
		}
	}

	static void N(PdfShape currentDrawShape, GraphicsState gs, int formLevel, Shape defaultClip, boolean renderPage, DynamicVectorRenderer current,
			PdfPageData pageData, int pageNum) {

		if (currentDrawShape.isClip()) {

			// create clipped shape
			currentDrawShape.closeShape();

			Shape s = currentDrawShape.generateShapeFromPath(gs.CTM, 0, Cmd.n, current.getType());

			// ignore huge shapes which will crash Java
			if (currentDrawShape.getComplexClipCount() < 5) {
				if (currentDrawShape.getSegmentCount() > 5000) {
					s = s.getBounds();
				}
			}
			else {
				if (currentDrawShape.getSegmentCount() > 2500) {
					s = s.getBounds();
				}
			}

			Area newClip = new Area(s);
			gs.updateClip(newClip);

			if (formLevel == 0) gs.checkWholePageClip(pageData.getMediaBoxHeight(pageNum) + pageData.getMediaBoxY(pageNum));

			// always reset flag
			currentDrawShape.setClip(false);

			// System.out.println(">>"+renderPage+" "+gs+" "+defaultClip);
			// save for later
			if (renderPage) current.drawClip(gs, defaultClip, false);

		}

		currentDrawShape.resetPath(); // flush all path ops stored
	}

	static Shape S(boolean isLowerCase, LayerDecoder layerDecoder, GraphicsState gs, PdfShape currentDrawShape, DynamicVectorRenderer current,
			boolean renderPage) {

		Shape currentShape = null;
		if (layerDecoder.isLayerVisible()) {

			// close for s command
			if (isLowerCase) currentDrawShape.closeShape();

			currentShape = currentDrawShape.generateShapeFromPath(gs.CTM, gs.getLineWidth(), Cmd.S, current.getType());

			if (currentShape != null) { // allow for the odd combination of crop with zero size
				Area crop = gs.getClippingShape();

				if (crop != null && (crop.getBounds().getWidth() == 0 || crop.getBounds().getHeight() == 0)) currentShape = null;
			}

			if (currentShape != null) { // allow for the odd combination of f then S

				if (currentShape.getBounds().getWidth() <= 1) // && currentGraphicsState.getLineWidth()<=1.0f){
				currentShape = currentShape.getBounds2D();

				// save for later
				if (renderPage) {

					gs.setStrokeColor(gs.strokeColorSpace.getColor());
					gs.setNonstrokeColor(gs.nonstrokeColorSpace.getColor());
					gs.setFillType(GraphicsState.STROKE);

					current.drawShape(currentShape, gs, Cmd.S);

				}
			}
		}

		// always reset flag
		currentDrawShape.setClip(false);
		currentDrawShape.resetPath(); // flush all path ops stored

		return currentShape;
	}
}
