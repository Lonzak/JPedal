/**
 * ===========================================
 * Java Pdf Extraction Decoding Access Library
 * ===========================================
 *
 * Project Info:  http://www.jpedal.org
 *
 * (C) Copyright 2007, IDRsolutions and Contributors.
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

 * FXMLShape.java
 * ---------------
 * (C) Copyright 2011
 * , by IDRsolutions and Contributors.
 *
 *
 * --------------------------
 */
package org.jpedal.render.output.javafx;

import org.jpedal.color.PdfPaint;
import org.jpedal.objects.GraphicsState;
import org.jpedal.objects.PdfPageData;
import org.jpedal.render.ShapeFactory;
import org.jpedal.render.output.OutputDisplay;
import org.jpedal.render.output.OutputShape;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;

public class FXMLShape extends OutputShape implements ShapeFactory {

	private GraphicsState gs;
	
	// FXML supports both winding rules, it's default is non-zero
	private int windingRule = PathIterator.WIND_NON_ZERO;

	public FXMLShape(int cmd, int yOffset,int shapeCount,float scaling, Shape currentShape, GraphicsState gs, AffineTransform scalingTransform, Point2D midPoint, Rectangle cropBox, int currentColor, int dpCount, int pageRotation, PdfPageData pageData, int pageNumber, boolean includeClip) {

		super(cmd, yOffset, scaling, currentShape, gs, scalingTransform, midPoint, cropBox, currentColor, dpCount, pageRotation, pageData, pageNumber, includeClip);

		this.shapeCount=shapeCount;
		this.gs = gs;

		windingRule = currentShape.getPathIterator(scalingTransform).getWindingRule(); // Added winding rule via shape iterator
		
		generateShapeFromG2Data(gs, scalingTransform, cropBox);
	}
	
	public void checkShapeClosed() {
		if(shapeIsOpen){

            shapeIsOpen=false;

            finishShape();
        }
	}


	protected void beginShape() {

		pathCommands.add("\n<!-- begin shape -->");

//		if(shapeCount!=0){
//			pathCommands.add("\t</elements>");
//			pathCommands.add("\t</Path>");
//		}

		String strokeWidth="", strokeLineCap="", strokeLineJoin="", strokeColor="", strokeMiterLimit="", fillColor="", fillRule="";
		boolean setStrokeWidth=false, setStrokeLineCap=false, setStrokeLineJoin=false, setStrokeColor=false, setStrokeMiterLimit=false, setFillColor=false;


		int fillType = gs.getFillType();

		if(fillType==GraphicsState.FILL || fillType==GraphicsState.FILLSTROKE) {
			PdfPaint col = gs.getNonstrokeColor();
			
			if(windingRule == PathIterator.WIND_EVEN_ODD) {
				fillRule = " fillRule=\"even_odd\"";
        	}

			String hexColor = OutputDisplay.hexColor(col.getRGB());

			//            if(isSinglePixelFill){ //special case to make sure appears in javaFx if w or h of shape is 1
			//			pathCommands.add("\tpath_"+shapeCount+".setStroke(Color."+ rgbToCSSColor(col.getRGB()) + ");");
			setStrokeColor=true;
			if(setStrokeColor)
				strokeColor = " stroke="+ '"' +hexColor+ '"';

			//            }else{ //add new fillStyle only if color changed
			//                if(col.getRGB() != currentColor) {                    
			//			pathCommands.add("\tpath_"+shapeCount+".setFill(Color."+ rgbToCSSColor(col.getRGB()) + ");");
			currentColor = col.getRGB();
			setFillColor=true;
			if(setFillColor)
				fillColor = " fill="+ '"' +hexColor+ '"';
			//                }
			//                pathCommands.add("\tpath_"+shapeCount+".setFill(Color."+ rgbToCSSColor(col.getRGB()) + ");");
		}
		//        }

		if(fillType==GraphicsState.STROKE || fillType==GraphicsState.FILLSTROKE) {
			BasicStroke stroke = (BasicStroke) gs.getStroke();

			if(stroke.getLineWidth()!=1) { //attribute double lineWidth; (default 1)
				//				pathCommands.add("\tpath_"+shapeCount+".setStrokeWidth(" + ((double) stroke.getLineWidth()*scaling) + ");");
				setStrokeWidth=true;
				if(setStrokeWidth)
					strokeWidth = " strokeWidth="+ '"' +((double) stroke.getLineWidth()*scaling)+ '"';
			}

			if(stroke.getMiterLimit()!=10) {  //attribute double miterLimit; // (default 10)
				//				pathCommands.add("\tpath_"+shapeCount+".setStrokeMiterLimit(" + ((double) stroke.getLineWidth()*scaling) + ");");
				setStrokeMiterLimit=true;
				if(setStrokeMiterLimit)
					strokeMiterLimit = " strokeMiterLimit="+ '"' +((double) stroke.getLineWidth()*scaling)+ '"';
			}

			//			pathCommands.add("\tpath_"+shapeCount+".setStrokeLineCap(StrokeLineCap." + determineLineCap(stroke) + ");");
			setStrokeLineCap=true;
			if(setStrokeLineCap)
				strokeLineCap = " strokeLineCap="+ '"' + determineLineCap(stroke) + '"';
			//			pathCommands.add("\tpath_"+shapeCount+".setStrokeLineJoin(StrokeLineJoin." + determineLineJoin(stroke) + ");");
			setStrokeLineJoin=true;
			if(setStrokeLineJoin)
				strokeLineJoin = " strokeLineJoin="+ '"' + determineLineJoin(stroke) + '"';

			PdfPaint col = gs.getStrokeColor();

			String hexColor = OutputDisplay.hexColor(col.getRGB());			


			//			pathCommands.add("\tpath_"+shapeCount+".setStroke(Color."+ rgbToCSSColor(col.getRGB()) + ");");
			setStrokeColor=true;
			if(setStrokeColor)
				strokeColor = " stroke="+ '"' +hexColor+ '"';
		}

		pathCommands.add("\t");
		pathCommands.add("<Path id=\"path"+ shapeCount + '"' +  fillRule + strokeColor + fillColor + strokeWidth + strokeLineCap + strokeLineJoin + strokeMiterLimit + '>');
		pathCommands.add("\t<elements>");

	}


	protected void finishShape() {

		//    	pathCommands.add("\t\troot.getChildren().add(path_"+shapeCount+");\n");

		pathCommands.add("\t</elements>");
		pathCommands.add("</Path>");
		pathCommands.add("\n<!-- finish shape -->");
	}

	protected void lineTo(double[] coords) {    
		
		//Get coords size as String  
		String coordinates = coordsToStringParam(coords, 2);

		//Values separated by the , 
		String delimiter = ",";

		//Temp storage for prefPageSize before converting to float
		String[] temp = coordinates.split(delimiter);

		//pathCommands.add("\tshape_"+shapeCount+".add(new LineTo(" + coordsToStringParam(coords, 2) + "));");
		pathCommands.add("\t\t<LineTo x=\""+temp[0] +"\" y=\""+ temp[1] +"\"/>");

	}

	protected void bezierCurveTo(double[] coords) {
		//Get coords size as String  
		String coordinates = coordsToStringParam(coords, 6);

		//Values separated by the , 
		String delimiter = ",";

		//Temp storage for prefPageSize before converting to float
		String[] temp = coordinates.split(delimiter);

		//    	pathCommands.add("\tshape_"+shapeCount+".add(new CubicCurveTo("+coordsToStringParam(coords, 6)+"));");
		pathCommands.add("\t\t<CubicCurveTo controlX1=\""+ temp[0] +"\" controlY1=\""+ temp[1] +"\" controlX2=\""+ temp[2] +"\" controlY2=\""+ temp[3] + "\" x=\""+ temp[4] +"\" y=\"" + temp[5] +"\" />");
		//    	pathCommands.add("");

	}

	protected void quadraticCurveTo(double[] coords) {
		//Get coords size as String  
		String coordinates = coordsToStringParam(coords, 4);

		//Values separated by the , 
		String delimiter = ",";

		//Temp storage for prefPageSize before converting to float
		String[] temp = coordinates.split(delimiter);

		//    	pathCommands.add("\tshape_"+shapeCount+".add(new QuadCurveTo("+coordsToStringParam(coords, 4)+"));");
		pathCommands.add("\t\t<QuadCurveTo controlX=\""+ temp[0] +"\" controlY=\""+ temp[1] +"\" x=\""+ temp[2] +"\" y=\""+ temp[3] +"\" />");

	}

	protected void moveTo(double[] coords) {
		//Get coords size as String  
		String coordinates = coordsToStringParam(coords, 2);

		//Values separated by the , 
		String delimiter = ",";

		//Temp storage for prefPageSize before converting to float
		String[] temp = coordinates.split(delimiter);

		//    	pathCommands.add("\tshape_"+shapeCount+".add(new MoveTo("+coordsToStringParam(coords, 2)+"));");

		pathCommands.add("\t\t<MoveTo x=\""+ temp[0] +"\" y=\""+ temp[1] +"\"/>");

	}
	
	protected void closePath() {
		pathCommands.add("\t\t<ClosePath />");
	}

	protected void drawCropBox(){

		double[] coords = {cropBox.x, cropBox.y};

//		pathCommands.add("\tshape_"+shapeCount+".add(new MoveTo("+convertCoords(coords, 2)+"));");
		pathCommands.add("\t\t<MoveTo x=\""+ coords[0] +"\" y=\""+ coords[1] +"\"/>");
		coords[0] += cropBox.width;
//		pathCommands.add("\tshape_"+shapeCount+".add(new LineTo("+convertCoords(coords, 2)+"));");
		pathCommands.add("\t\t<LineTo x=\""+ coords[0] +"\" y=\""+ coords[1] +"\"/>");
		coords[1] += cropBox.height;
//		pathCommands.add("\tshape_"+shapeCount+".add(new LineTo("+convertCoords(coords, 2)+"));");
		pathCommands.add("\t\t<LineTo x=\""+ coords[0] +"\" y=\""+ coords[1] +"\"/>");
		coords[0] -= cropBox.width;
//		pathCommands.add("\tshape_"+shapeCount+".add(new LineTo("+convertCoords(coords, 2)+"));");
		pathCommands.add("\t\t<LineTo x=\""+ coords[0] +"\" y=\""+ coords[1] +"\"/>");

	}

	protected void applyGraphicsStateToPath(GraphicsState gs){
		//implemented in the beginShape() method
	}


	/**
	 * Extract line cap attribute javaFX implementation
	 */

	protected static String determineLineCap(BasicStroke stroke)//javaFX implementation
	{
		//attribute DOMString lineCap; // "butt", "round", "square" (default "butt")
		String attribute;

		switch(stroke.getEndCap()) {
		case(BasicStroke.CAP_ROUND):
			attribute = "ROUND";
		break;
		case(BasicStroke.CAP_SQUARE):
			attribute = "SQUARE";
		break;
		default:
			attribute = "BUTT";
			break;
		}
		return attribute;
	}

	/**
	 * Extract line join attribute javaFX implementation
	 */
	protected static String determineLineJoin(BasicStroke stroke){//javaFX implementation
		//attribute DOMString lineJoin; // "round", "bevel", "miter" (default "miter")
		String attribute;
		switch(stroke.getLineJoin()) {
		case(BasicStroke.JOIN_ROUND):
			attribute = "ROUND";
		break;
		case(BasicStroke.JOIN_BEVEL):
			attribute = "BEVEL";
		break;
		default:
			attribute = "MITER";
			break;
		}
		return attribute;
	}

	public void setShapeNumber(int shapeCount) {
		this.shapeCount=shapeCount;
		//		System.out.println("shape="+shapeCount);
	}
	
}





