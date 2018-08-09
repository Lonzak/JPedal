package org.jpedal.render.output;

import java.awt.BasicStroke;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jpedal.objects.GraphicsState;
import org.jpedal.objects.PdfPageData;
import org.jpedal.parser.Cmd;

public class OutputShape
{
	private static final boolean ENABLE_CROPPING = true;

    //track if shape not closed in FXML
    protected boolean shapeIsOpen=false;

    boolean hasMoveTo=false;

	protected int shapeCount=0;

    double minXcoord=999999, minYcoord=999999;

	protected List	pathCommands;//genetics not supported in java ME
	protected int    			currentColor;
	protected Rectangle		cropBox;
	private Point2D			midPoint;

	private double[]		lastVisiblePoint;
	private double[]		lastInvisiblePoint;
	private double[] 		previousPoint;
	private boolean 		isPathSegmentVisible;
	protected double[]		entryPoint;

	//used to display multiple pages where moved down page
	private int yOffset=0;

    boolean includeClip=false;

	private double[] exitPoint;

	//For rectangle larger than cropBox
	private List	        largeBox;
	private boolean			isLargeBox;
	private boolean			largeBoxSideAlternation; //Flag to check that lines are alternating between horizontal and vertical.

	static int debug = 0;

	Rectangle clipBox=null;

	/**number of decimal places on shapes*/
	private int dpCount=0;

	private int pageRotation=0;

	//applied to the whole page. Default= 1
	protected float scaling;

	private boolean debugPath=false;

	//flag to show if we need to convert Fill to Stroke to appear in HTML
	protected boolean isSinglePixelFill;
	


	int pathCommand;

	private float[][] ctm;

	private Shape currentShape;
	private int minX,minY;

    //Cmd.F or Cmd.S or Cmd.B for type of shape
    protected int cmd=-1;

    int adjustY=0;
    
    // fix for pixel perfect lines
    static final boolean lyndonNewCode = false;
    float currentLineWidth;
    double xValues[];
    double yValues[];
    int drawCommandNum = 0;
    boolean modifyX[];
    boolean modifyY[];
    
    public OutputShape(int cmd, int yOffset,float scaling, Shape currentShape, GraphicsState gs, AffineTransform scalingTransform,
			Point2D midPoint, Rectangle cropBox, int currentColor, int dpCount, int pageRotation, PdfPageData pageData, int pageNumber, boolean includeClip)
	{

        //used by HTML to flag if we clip with clip or in our code
        this.includeClip=includeClip;

        //adjust for rounding
        if(currentShape.getBounds2D().getHeight()>0.6f && currentShape.getBounds2D().getHeight()<1.0f && currentShape.getBounds2D().getWidth()>1f){
            
           double diff=currentShape.getBounds2D().getMinY()-currentShape.getBounds().y;
           if(diff>0.7){
               adjustY=1;//
           }
        }
        
		this.cmd=cmd;

        if(cmd==Cmd.Tj){
            if(cropBox!=null){
                minX=(int)cropBox.getMinX();
                minY=(int)cropBox.getMinY();
            }
        }else{
		    minX=pageData.getCropBoxX(pageNumber);
		    minY=pageData.getCropBoxY(pageNumber);
        }

		if(debugPath)
			System.out.println("raw shape="+currentShape.getBounds()+" minx="+minX+ ' ' +minY);

		this.yOffset=yOffset;

		//get any Clip (only rectangle outline)
		Area clip=gs.getClippingShape();
		if(clip!=null){
			clipBox=clip.getBounds();
		}else
			clipBox=null;

		//adjust for offsets
		if((minX!=0 || minY!=0) && clipBox!=null && cropBox!=null){
			clipBox.translate(-minX,-minY);
		}

		this.currentShape = currentShape;
		this.ctm = gs.CTM;
		this.scaling = scaling;
		this.currentColor = currentColor;
		this.cropBox = cropBox;
		this.dpCount = dpCount;
		this.midPoint = midPoint;
		
		//flag if w or h is 1 so we can sub fill for stroke
		this.isSinglePixelFill=currentShape.getBounds().width<2 || currentShape.getBounds().height<2;

		this.pageRotation=pageRotation;

		isPathSegmentVisible = true;
		lastVisiblePoint = new double[2];
		lastInvisiblePoint = new double[2];
		previousPoint = new double[2];
		exitPoint = new double[2];
		entryPoint = new double[2];

		pathCommands = new ArrayList();
		largeBox = new ArrayList();
		isLargeBox = true; //Prove wrong

	}

	protected void generateShapeFromG2Data(GraphicsState gs, AffineTransform scalingTransform, Rectangle cropBox) {
		currentLineWidth = gs.getLineWidth() * scaling; // fix for pixel lines
		PathIterator it = currentShape.getPathIterator(scalingTransform);

		// first part of fix for case 12558 - bike.pdf moveTo as opposed to lineTo
		int totalSeg = 0;
		while(!it.isDone()) {
			totalSeg ++;
			it.next();
		}
		
		if(lyndonNewCode) {
		    xValues = new double[totalSeg];
		    yValues = new double[totalSeg];
		    modifyX = new boolean[totalSeg];
		    modifyY = new boolean[totalSeg];
		    it = currentShape.getPathIterator(scalingTransform);
		    drawCommandNum = 0;
		    while(!it.isDone()) {
			double[] coords = {0,0,0,0,0,0};
			pathCommand = it.currentSegment(coords);
			xValues[drawCommandNum] = coords[0];
			yValues[drawCommandNum] = coords[1];
			System.out.println(xValues[drawCommandNum] + ", " + yValues[drawCommandNum]);
			if(drawCommandNum <= 0) {
			    modifyX[drawCommandNum] = false;
			    modifyY[drawCommandNum] = false;
			}
			else {
			    // checks for pixel perfect lines
			    if(xValues[drawCommandNum] == xValues[drawCommandNum-1]) {
				// this and the previous x are the same
				modifyY[drawCommandNum] = true;
			    }
			    else {
				modifyY[drawCommandNum] = false;
			    }
			    if(yValues[drawCommandNum] == yValues[drawCommandNum-1]) {
				// this and the previous x are the same
				modifyX[drawCommandNum] = true;
			    }
			    else {
				modifyX[drawCommandNum] = false;
			    }
			}
			drawCommandNum ++;
			it.next();
		    }
		    drawCommandNum = 0;
		}
		
		it = currentShape.getPathIterator(scalingTransform);

        //debugPath=cmd==Cmd.n && currentShape.getBounds().x==0 && currentShape.getBounds().y==44;

        shapeIsOpen= true;

        beginShape();

		boolean firstDrawCommand = true && ENABLE_CROPPING;

		if(debugPath){
			System.out.println("About to generate commands for shape with bounds" +currentShape.getBounds());
			System.out.println("------------------------------------------------" );
			System.out.println("crop bounds=" +cropBox.getBounds());
			System.out.println("shape bounds=" +currentShape.getBounds());
			System.out.println("minX=" +minX+" minY="+minY);
			if(clipBox!=null)
				System.out.println("clip bounds=" +clipBox.getBounds());
		}

		int segCount=0;

        double[] lastPt;

		while(!it.isDone()) {
			double[] coords = {0,0,0,0,0,0};
			boolean lastCommandMoveTo = false;

			pathCommand = it.currentSegment(coords);
			segCount++;
			
			if(cropBox!=null){
                for(int ii=0;ii<3;ii++){
                        coords[ii*2]=coords[ii*2]-minX;
                        coords[(ii*2)+1]=coords[(ii*2)+1]-minY;
                }
            }

			// second part of fix for case 12558 - bike.pdf moveTo as opposed to lineTo
			if(segCount >= totalSeg && pathCommand == PathIterator.SEG_MOVETO) {
				BasicStroke stroke = (BasicStroke) gs.getStroke();
				// colour and width specific for bike.pdf
				if(stroke.getLineWidth() >= 10.f && gs.getStrokeColor().getRGB() == -1) {
					pathCommand = PathIterator.SEG_LINETO;
					coords[1] += stroke.getLineWidth() / 2; // may be specific to bike.pdf
					lastCommandMoveTo = true;
				}
				
				// Can potentially remove the final moveTo altogether as it is not required
				//it.next();
				//continue;
			}
			
			if(debugPath) {
				System.out.println("\n=======Get pathCommand segment "+(int)coords[0]+ ' ' +(int)coords[1]+ ' ' +(int)coords[2]+ ' ' +(int)coords[3]+ ' ' +(int)coords[4]+ ' ' +(int)coords[5]);
				if(lastCommandMoveTo)
					System.out.println("Last pathCommand changed from moveTo to lineTo");
			}
				
			boolean isCropBoxDrawn=false;
			if(cmd!=Cmd.n)
        	   isCropBoxDrawn=checkLargeBox(coords, pathCommand);
			
			//isCropBoxDrawn=false;
			//avoid duplicate lineto if box drawn (ie sample_pdfs_html/general/test22.pdf page 1
           if(isCropBoxDrawn){
        	   it.next();
        	   continue;
           }
           
			if(firstDrawCommand) {  //see if starts visible
				int dx=(int)coords[getCoordOffset(pathCommand)];
				int dy=(int)coords[getCoordOffset(pathCommand) + 1];

				//use CropBox or clip (whichever is smaller)
				Rectangle cropBoxtoTest=cropBox;

				if(clipBox!=null){
					cropBoxtoTest=clipBox;
				}

				//avoid rounding error in comparison
				if(cropBoxtoTest!=null && cropBoxtoTest.width>1){
					if(dx==cropBoxtoTest.x)
						dx=dx+1;
					else if(dx==(cropBoxtoTest.x + cropBoxtoTest.width))
						dx=dx-1;
				}

				isPathSegmentVisible = cropBoxtoTest==null || cropBoxtoTest.contains(dx, dy);
				firstDrawCommand = false;

				if(debugPath)
					System.out.println("isPathSegmentVisible="+isPathSegmentVisible);

			}

            if (pathCommand == PathIterator.SEG_CUBICTO) {

				boolean isPointVisible = testDrawLimits(coords, PathIterator.SEG_CUBICTO);
				if(debugPath)
					System.out.println("PathIterator.SEG_CUBICTO isPointVisible="+isPointVisible);

				if(isPointVisible && isPathSegmentVisible)
					bezierCurveTo(coords);

				//flag incase next item is a line
				isPathSegmentVisible = isPointVisible;

			}else if (pathCommand == PathIterator.SEG_LINETO) {
                
                lastPt=new double[]{previousPoint[0],previousPoint[1]};

				boolean isPointVisible = testDrawLimits(coords, PathIterator.SEG_LINETO);

				if(debugPath)
					System.out.println("PathIterator.SEG_LINETO isPointVisible="+isPointVisible+" isPathSegmentVisible="+isPathSegmentVisible+ ' ' +(int)coords[0]+ ' ' +(int)coords[1]);

				if(isPointVisible && isPathSegmentVisible) {

					if(debugPath)
						System.out.println("pdf.lineTo(" + coordsToStringParam(coords, 2)+ ')');

					lineTo(coords);

				}else if(isPointVisible != isPathSegmentVisible) {

					if(!isPathSegmentVisible) {

						if(gs.getFillType() != GraphicsState.FILL) {
							moveTo(entryPoint);
                            hasMoveTo=true;
                            
							if(debugPath)
								System.out.println("pdf.moveTo(" + coordsToStringParam(coords, 2));
						}else {
                            //if it has not entered box yet, work out which corner to start at
                            if(!hasMoveTo && !cropBox.contains(lastPt[0], lastPt[1])){

                                double cx=cropBox.x+cropBox.width;
                                double cy=cropBox.y+cropBox.height;

                                if(cx>lastInvisiblePoint[0])
                                    cx=cropBox.x;

                                if(lastInvisiblePoint[1]<cropBox.y)
                                    cy=cropBox.y;
                                else if(cy<lastInvisiblePoint[1])
                                    cy=cropBox.y;

                                double[] cornerEntryPt=new double[]{cx,cy};
                                hasMoveTo=true;
                                moveTo(cornerEntryPt);

                                if(debugPath)
                                    System.out.println("pdf.moveTo(" + coordsToStringParam(cornerEntryPt, 2));
                                
                            }

                            lineTo(entryPoint);
						}

                        lineTo(coords);

						if(debugPath)
							System.out.println("pdf.lineTo(" + coordsToStringParam(coords, 2)+");");

						isPathSegmentVisible = true;
					}else {

						lineTo(exitPoint);

						isPathSegmentVisible = false;

						if(debugPath)
							System.out.println("pdf.lineTo(" + coordsToStringParam(exitPoint, 2)+");");
					}
				}
			}else if (pathCommand == PathIterator.SEG_QUADTO) {

				if(debugPath)
					System.out.println("PathIterator.SEG_QUADTO");

				if(testDrawLimits(coords, PathIterator.SEG_QUADTO)) {

					if(debugPath)
						System.out.println("pdf.quadraticCurveTo(" + coordsToStringParam(coords, 4));


					quadraticCurveTo(coords);
					isPathSegmentVisible = true;

				}else {
					isPathSegmentVisible = false;
				}
			}else if (pathCommand == PathIterator.SEG_MOVETO) {
				if(debugPath)
					System.out.println("PathIterator.SEG_MOVETO");
				if(testDrawLimits(coords, PathIterator.SEG_MOVETO)) {
					isPathSegmentVisible = true;
					if(debugPath)
						System.out.println("pdf.moveTo(" + coordsToStringParam(coords, 2) + ");");
                    hasMoveTo=true;
					moveTo(coords);

				}else {
					isPathSegmentVisible = false;
				}
			} else if (pathCommand == PathIterator.SEG_CLOSE) {
				if(debugPath)
					System.out.println("pdf.closePath();");
				closePath();
			}
			if(lyndonNewCode) {drawCommandNum ++;}
			it.next();
		}

		if(pathCommands.size()==1) {
			//No commands made it through
			pathCommands.clear();
		}else {

			if(cmd== Cmd.Tj || (!isPathSegmentVisible && gs.getFillType() != GraphicsState.FILL)) {

                shapeIsOpen=false;

                finishShape();

				if(debugPath)
					System.out.println("pdf.closePath();");
			}
			applyGraphicsStateToPath(gs);
		}

		//ignore rect if totally outside clip
		if(cmd== Cmd.S && segCount==6 && clipBox!=null && currentShape.getBounds().width>10 && currentShape.getBounds().height>10 && clipBox.x>=currentShape.getBounds().x && clipBox.x+clipBox.width<=currentShape.getBounds().x +currentShape.getBounds().width
				&& clipBox.y>=currentShape.getBounds().y  && clipBox.y+clipBox.height<=currentShape.getBounds().y +currentShape.getBounds().height){
			this.pathCommands.clear();
			if(debugPath)
				System.out.println("shape removed");
		}
		
		//only does something on FXML
		checkShapeClosed();
	}

	public void checkShapeClosed() {
    	//only does something on FXML which overrides this
	}

	//empty version
	protected void moveTo(double[] coords) {}

	//empty version 
	protected void quadraticCurveTo(double[] coords) {}

	//empty version 
	protected void lineTo(double[] coords) {}
	
	//empty version
	protected void closePath() {}

	//empty version 
	protected void finishShape() {}

	//empty version 
	protected void beginShape() {}

	//empty version 
	protected void bezierCurveTo(double[] coords) {}

	/**
	 * trim if needed
	 * @param i
	 * @return
	 *
	 * (note duplicate in HTMLDisplay)
	 */
	private String setPrecision(double i, boolean isX) {
		
		if(lyndonNewCode) {
			String value = "";
			
			int num;
			if(isX) {
			    if(!modifyX[drawCommandNum]){
				num = (int) i;
				value += num + ".5";
			    }
			    else {
				num = (int) (i + .5);
				value += num;
			    }
				
			}
			else {
			    if(!modifyY[drawCommandNum]){
				num = (int) i;
				value += num + ".5";
			    }
			    else {
				num = (int) (i + .5);
				value += num;
			    }
			}
			
			//track corner so we can reuse if needed for shading
			if(isX && i<minXcoord)
				minXcoord=i;
			else if(!isX && i<minYcoord)
				minYcoord=i;
			
			return value;
		}

		String value=String.valueOf(i);

        //track corner so we can reuse if needed for shading
        if(isX && i<minXcoord)
            minXcoord=i;
        else if(!isX && i<minYcoord)
            minYcoord=i;
		
		int ptr=value.indexOf('.');
		int len=value.length();
		int decimals=len-ptr-1;

		if(ptr>0 && decimals> this.dpCount){
			if(dpCount==0)
				value=value.substring(0,ptr+dpCount);
			else
				value=value.substring(0,ptr+dpCount+1);

		}

			return value;
	}
	


	/**
	 * Extracts information out of graphics state to use in HTML - empty version
	 */
	protected void applyGraphicsStateToPath(GraphicsState gs){}

	/**
	 * Extract line cap attribute
	 */
	protected static String determineLineCap(BasicStroke stroke)
	{
		//attribute DOMString lineCap; // "butt", "round", "square" (default "butt")
		String attribute;

		switch(stroke.getEndCap()) {
		case(BasicStroke.CAP_ROUND):
			attribute = "round";
		break;
		case(BasicStroke.CAP_SQUARE):
			attribute = "square";
		break;
		default:
			attribute = "butt";
			break;
		}
		return attribute;
	}

	/**
	 * Extract line join attribute
	 */
	protected static String determineLineJoin(BasicStroke stroke)
	{
		//attribute DOMString lineJoin; // "round", "bevel", "miter" (default "miter")
		String attribute;
		switch(stroke.getLineJoin()) {
		case(BasicStroke.JOIN_ROUND):
			attribute = "round";
		break;
		case(BasicStroke.JOIN_BEVEL):
			attribute = "bevel";
		break;
		default:
			attribute = "miter";
			break;
		}
		return attribute;
	}

	/**
	 * @return true if coordinates lie with visible area
	 */
	private boolean testDrawLimits(double[] coords, int pathCommand)
	{
		
        //html uses clipping so we do not alter shapes
        if(includeClip)
           return true;

		if(debugPath)
			System.out.println("testDrawLimits coords[0] + coords[1]=" + (int)coords[0] + ' ' + (int)coords[1]);

		if(!ENABLE_CROPPING) {
			return true;
		}

		int offset = getCoordOffset(pathCommand);
		
		//assume not visible by default
		boolean isCurrentPointVisible;

		if(debugPath)
			System.out.println("crop="+this.cropBox+" clip="+clipBox);

		//use CropBox or clip (whichever is smaller)
		Rectangle cropBox=this.cropBox;
		
		if(clipBox!=null){
			cropBox=cropBox.intersection(clipBox);

            if(debugPath)
                System.out.println("merged crop="+cropBox);
		}

		/**
		 * turn co-ords into a Point
		 * (the 1s are to allow for rounding errors)
		 */
		double x=coords[offset];
		if(x>cropBox.x+1)
			x=x-1;
		double y=coords[offset+1];
		if(y>cropBox.y+1)
			y=y-1;
		double[] point = {x,y};
		
		if(cropBox.contains(point[0], point[1])) {
			lastVisiblePoint = point;
			isCurrentPointVisible = true;

			if(debugPath)
				System.out.println("Point visible in cropBox or clip");

		}
		else { //this point outside visible area
			lastInvisiblePoint = point;
			isCurrentPointVisible = false;
			
			if(debugPath)
				System.out.println("Point invisible "+(int)point[0]+ ' ' +(int)point[1]+" crop="+cropBox.getBounds());
		}

		if(!isCurrentPointVisible && isPathSegmentVisible) {

			if(debugPath)
				System.out.println("Case1 this point "+(int)x+ ',' +(int)y+" invisible and isPathSegmentVisible");

			findSwitchPoint(point, true);
		}
		else if(isCurrentPointVisible && !isPathSegmentVisible) {

			if(debugPath)
				System.out.println("Case2 this point "+(int)x+ ',' +(int)y+" visible and isPathSegment invisible");

			findSwitchPoint(point, false);

		}else{

			if(debugPath)
				System.out.println("Case3 NOT COVERED isCurrentPointVisible="+isCurrentPointVisible+" isPathSegmentVisible"+isPathSegmentVisible);
		}


		//Check whether this point and last point cross crop box.
		if(!isCurrentPointVisible && (!cropBox.contains(previousPoint[0], previousPoint[1])) && pathCommand == PathIterator.SEG_LINETO) {

			if(debugPath)
				System.out.println("checkTraversalPoints");

			checkTraversalPoints(point, previousPoint, cropBox);

		}

		previousPoint = point;

		return isCurrentPointVisible;
	}

	/**
	 * Figure out where to draw a line if the given points do not lie within the cropbox
	 * but should draw a line because they pass over it.
	 */
	private void checkTraversalPoints(double[] startPoint, double[] endPoint, Rectangle cropBox)
	{
		boolean xTraversal = (endPoint[0] < cropBox.x && startPoint[0] > (cropBox.x + cropBox.width)) || (startPoint[0] < cropBox.x && endPoint[0] > (cropBox.x + cropBox.width));
		boolean yTraversal = (endPoint[1] < cropBox.y && startPoint[1] > (cropBox.y + cropBox.height)) || (startPoint[1] < cropBox.y && endPoint[1] > (cropBox.y + cropBox.height));
		boolean completeCropBoxMiss = isCompleteCropBoxMiss(startPoint, endPoint,cropBox);

        if(debugPath){

            System.out.println("checkTraversalPoints xtrav="+xTraversal+" yTrav="+yTraversal+" completeCropBoxMiss="+completeCropBoxMiss+ ' ' +(endPoint[0] < cropBox.x && startPoint[0] > (cropBox.x + cropBox.width)));
			System.out.println("start="+(int)startPoint[0]+ ' ' +(int)startPoint[1]+"  end="+(int)endPoint[0]+ ' ' +(int)endPoint[1]);
			//System.out.println("cropBox="+cropBox.x+" "+cropBox.y+" "+cropBox.width+" "+cropBox.height);
		}

		if(!xTraversal && !yTraversal) {
			return;
		}

		//double xSide = startPoint[0] - endPoint[0];
		//double ySide = startPoint[1] - endPoint[1];

        //System.out.println((int)startPoint[0]+" "+(int)startPoint[1]+" end="+(int)endPoint[0]+" "+(int)endPoint[1]+" "+cropBox);

        //work out gradient of line
        double m=(endPoint[1]-startPoint[1])/(endPoint[0]-startPoint[0]);
        double rawM=m;

        //allow for vertical line
        if(Math.abs(endPoint[0]-startPoint[0])<1)
            m=0;

        if(m<0.001)
            m=0;

        //and workout y
        if(m==0){

            if(rawM<0 && endPoint[0]<0 && (endPoint[0]<startPoint[0]) ){
                exitPoint=calcCrossoverPoint(endPoint, cropBox, m, startPoint);
                entryPoint=calcCrossoverPoint(startPoint, cropBox, m, endPoint);
            }else{
                entryPoint=calcCrossoverPoint(endPoint, cropBox, m, startPoint);
                exitPoint=calcCrossoverPoint(startPoint, cropBox, m, endPoint);
            }
        }else{
            exitPoint=calcCrossoverPoint(endPoint, cropBox, m, startPoint);
            entryPoint=calcCrossoverPoint(startPoint, cropBox, m, endPoint);
        }


        if(debugPath){
            System.out.println("entry="+entryPoint[0]+ ' ' +entryPoint[1]+" exit="+exitPoint[0]+ ' ' +exitPoint[1]);
            System.out.println("XXpdf.lineTo(" + coordsToStringParam(entryPoint, 2)+");");
            System.out.println("XXpdf.lineTo(" + coordsToStringParam(exitPoint, 2)+"); hasMoveTo="+hasMoveTo);
        }

        //if it has not entered box yet, work out which corner to start at (see awjune p13)
        if(!hasMoveTo && endPoint[0]<0 && startPoint[0]>entryPoint[0] && entryPoint[0]>exitPoint[0]){

            double cx=cropBox.x+cropBox.width;
            double cy=cropBox.y+cropBox.height;
            if(cx>lastVisiblePoint[0])
                cx=cropBox.x;
            if(cy<lastVisiblePoint[1])
                cy=cropBox.y;

            double[] cornerEntryPt=new double[]{cx,cy};
            hasMoveTo=true;
            moveTo(cornerEntryPt);

            if(debugPath)
                System.out.println("pdf.moveTo(" + coordsToStringParam(cornerEntryPt, 2));
            lineTo(exitPoint);
            lineTo(entryPoint);
        }else{
            lineTo(entryPoint);
            lineTo(exitPoint);
        }
	}

    private static double[] calcCrossoverPoint(double[] pt, Rectangle cropBox, double m, double[] otherPt) {

        double x1=0,y1=0;

        /**
         * case 1/2 and  -above top of of cropBox or below
         */
        //(use cropBox vertical line as Y and check if in bounds to make sure not a side line)
        if(pt[1]<cropBox.y || pt[1]>cropBox.y+cropBox.height){

            //choose y to match
            if(pt[1]<cropBox.y)
                y1=cropBox.y;
            else
                y1=cropBox.y+cropBox.height;

            if(m==0)
                x1=pt[0];
            else
                x1=((y1- pt[1])/m)+ pt[0];

            //check it crosses top line and not one of sides
            if(x1<cropBox.x || x1> cropBox.x+cropBox.width){ //actually it crosses as side line
                //choose left or right side
                if(x1<cropBox.x)
                    x1=cropBox.x;
                else
                    x1=cropBox.x+cropBox.width;

                //and workout y
                y1=(m*(x1- pt[0]))+ pt[1];
            }

            /**
             * case 3/4 and  -to left or right of cropBox
             */
            //(use cropBox horizontal line as X and check if in bounds to make sure not a vertical)
        }else if(pt[0]<cropBox.x || pt[0]>cropBox.x+cropBox.width){

            //choose left or right side
            if(pt[0]<cropBox.x)
                x1=cropBox.x;
            else
                x1=cropBox.x+cropBox.width;

            y1=(m*(x1- pt[0]))+ pt[1];

            //check it crosses side line and not one of verticals
            if(y1<cropBox.y || y1> cropBox.y+cropBox.height){ //actually it crosses as side line
                //choose y to match
                if(pt[1]<cropBox.y)
                    y1=cropBox.y;
                else
                    y1=cropBox.y+cropBox.height;

                x1=((y1- pt[1])/m)+ pt[0];

            }
        }

        return new double[]{x1, y1};
    }

    /**
	 * Figure out where the line disappears or reappears off the cropbox boundary.
	 * @param point The current point
	 * @param exit true if you wish to find an exit point (one that marks a disappearance)
	 */
	private void findSwitchPoint(double[] point, boolean exit){

		double[] lastPoint;
		double[] switchPoint = new double[2];

		lastPoint = exit ? lastVisiblePoint : lastInvisiblePoint;

		if(debugPath){
			if(exit)
				System.out.println("Find point of exit lastPoint="+(int)lastPoint[0]+ ' ' +(int)lastPoint[1]+" current="+(int)point[0]+ ' ' +(int)point[1]);
			else
				System.out.println("Find point of entry lastPoint="+(int)lastPoint[0]+ ' ' +(int)lastPoint[1]+" current="+(int)point[0]+ ' ' +(int)point[1]);
		}

		if(!exit) {
			double[] tmp = point;
			point = lastPoint;
			lastPoint = tmp;
		}

		double xSide = point[0] - lastPoint[0];
		double ySide = point[1] - lastPoint[1];

		//To indicate whether a coordinate has been found
		boolean xFound = false;
		boolean yFound = false;

		if(clipBox!=null && point[0] >= clipBox.width + clipBox.x) {
			switchPoint[0] = clipBox.width + clipBox.x;
			xFound = true;
		}else if(point[0] >= cropBox.width + cropBox.x) {
			switchPoint[0] = cropBox.width + cropBox.x;
			xFound = true;
		}else if(clipBox!=null && point[0] < clipBox.x) {
			switchPoint[0] = clipBox.x;
			xFound = true;
		}else if(point[0] < cropBox.x) {
			switchPoint[0] = cropBox.x;
			xFound = true;
		}

		if(clipBox!=null && point[1] > clipBox.height + clipBox.y) {
			switchPoint[1] = clipBox.height + clipBox.y;
			yFound = true;
		}else if(point[1] > cropBox.height + cropBox.y) {
			switchPoint[1] = cropBox.height + cropBox.y;
			yFound = true;
		}else if(clipBox!=null && point[1] < clipBox.y) {
			switchPoint[1] = clipBox.y;
			yFound = true;
		}else if(point[1] < cropBox.y) {
			switchPoint[1] = cropBox.y;
			yFound = true;
		}

		if(yFound) {
			if(xSide == 0) {
				switchPoint[0] = point[0];
			}else {
				double tan = xSide / ySide;
				if(ySide==0)
					tan=1;
				double distanceToCropY = switchPoint[1] - point[1];
				switchPoint[0] = point[0] + (tan * distanceToCropY);
			}
		}
		if(xFound) {
			if(ySide == 0) {
				switchPoint[1] = point[1];
			}else {
				double tan = ySide / xSide;
				double distanceToCropX = switchPoint[0] - point[0];
				switchPoint[1] = point[1] + (tan * distanceToCropX);
			}
		}

		if(exit) {
			exitPoint = switchPoint;
			if(debugPath)
				System.out.println("returns exit="+(int)switchPoint[0]+ ' ' +(int)switchPoint[1]);

		}
		else {
			entryPoint = switchPoint;
			if(debugPath)
				System.out.println("returns entry="+(int)switchPoint[0]+ ' ' +(int)switchPoint[1]);
		}
	}

	/**
	 * Add the coords to the large box list if it might possibly be part of a rectangle.
	 * @param coords
	 * @param pathCommand
	 */
	private boolean checkLargeBox(double[] coords, int pathCommand)
	{

		boolean isDrawn=false;
		
		if(debugPath)
			System.out.println("check large "+(int)coords[0]+ ' ' +(int)coords[1]);

		if(!isLargeBox && (pathCommand != PathIterator.SEG_LINETO || pathCommand != PathIterator.SEG_MOVETO)) {
			return false;
		}

		double px=coords[getCoordOffset(pathCommand)],py= coords[getCoordOffset(pathCommand) + 1];
		double[] adjustedCords=this.correctCoords(new double[]{px,py});
		px=adjustedCords[0];
		py=adjustedCords[1];

		Point point = new Point((int)px, (int)py);

		if(largeBox.isEmpty()) {
			largeBox.add(point);
		}
		else {
			Point2D last = (Point)largeBox.get(largeBox.size() - 1);
			double xSide = last.getX() - point.getX();
			double ySide = last.getY() - point.getY();

			//First time we can compare so check here and ignore below.
			if(largeBox.size() == 1) {

				if(ySide!=0)//allow for div by zero
					largeBoxSideAlternation = xSide / ySide != 0;
				else
					largeBoxSideAlternation=true;
			}

			//If this point and the last point do not form a horizontal or vertical line its not part of a large rectangular shape.
			if(xSide / ySide == 0 || ySide / xSide == 0) {

				boolean currentSide = xSide / ySide == 0;

				//Ignore if its part of a continous line.  The continous line could be going back on it self but currently not accounted for.
				if(largeBox.size() >1 || (currentSide != largeBoxSideAlternation)) {
					largeBox.add(point);
					largeBoxSideAlternation = xSide / ySide == 0;
				}
			}
			else if(!point.equals(largeBox.get(largeBox.size() - 1))) {
				isLargeBox = false;
				return false;
			}

			//Check if we have enough point to see if it is larger than the whole page.
			if(largeBox.size() >= 4 && isLargerThanCropBox()) {
				drawCropBox();
				isDrawn=true; 
			}
		}
		
		return isDrawn;
	}

	/**
	 * return true if the coordinates in this path specify a box larger than the cropbox.
	 */
	private boolean isLargerThanCropBox()
	{
		if(!isLargeBox || cropBox==null) {
			return false;
		}

		Point2D x = (Point)largeBox.get(largeBox.size() - 4);
		Point2D y = (Point)largeBox.get(largeBox.size() - 3);
		Point2D z = (Point)largeBox.get(largeBox.size() - 2);
		Point2D w = (Point)largeBox.get(largeBox.size() - 1);

		int shortestSide = cropBox.width < cropBox.height ? cropBox.width : cropBox.height;
		//Can not cover the page if this is true.
		if(x.distance(y) < shortestSide || y.distance(z) < shortestSide || z.distance(w) < shortestSide) {
			return false;
		}

		int outsideCount = 0;

		if (!cropBox.contains(x.getX(), x.getY())) outsideCount++;
		if (!cropBox.contains(y.getX(), y.getY())) outsideCount++;
		if (!cropBox.contains(z.getX(), z.getY())) outsideCount++;
		if (!cropBox.contains(w.getX(), w.getY())) outsideCount++;

		if(outsideCount <= 2) {
			return false;
		}

		Set points = new HashSet();
		points.add(x);
		points.add(y);
		points.add(z);
		points.add(w);

		outsideCount = 0;

		//Test that points lie in correct areas to justify a box covering the page.
		for(int hOffset = -1; hOffset <= 1; hOffset++) {
			for(int wOffset = -1; wOffset <= 1; wOffset++) {
				if(hOffset == 0 && wOffset==0) { //Would mean the test rectangle is same as cropbox so ignore
					continue;
				}
				Rectangle outside = new Rectangle(cropBox);
				outside.translate(wOffset * cropBox.width, hOffset * cropBox.height);
				if(doesPointSetCollide(points, outside)) outsideCount++;
			}
		}

		return outsideCount >= 3;
	}

	/**
	 * return true if any of the given points are contained in the given rectangle
	 */
	private static boolean doesPointSetCollide(Set points, Rectangle rect)
	{
		//converted so java ME compilies
        for (Object point : points) {
            Point2D pt = (Point2D) point;
            if (rect.contains(pt)) {
                return true;
            }
        }
		return false;
	}

	protected void drawCropBox(){}

	private double getClosestCropEdgeX(double x)
	{
		return x < cropBox.x + (cropBox.width / 2) ? cropBox.x : cropBox.x + cropBox.width;
	}

	private double getClosestCropEdgeY(double y)
	{
		return y < cropBox.y + (cropBox.height / 2) ? cropBox.y : cropBox.y + cropBox.height;
	}

	/**
	 * Convert from PDF coords to java coords.
	 */
	private double[] correctCoords(double[] coords)
	{

        if(cropBox!=null){
            int offset;

            switch(pathCommand) {
            case(PathIterator.SEG_CUBICTO):
                offset = 4;
            break;
            case(PathIterator.SEG_QUADTO):
                offset = 2;
            break;
            default:
                offset = 0;
                break;
            }

            //ensure fits
            if(offset>coords.length)
                offset=coords.length-2;

            for(int i = 0; i < offset + 2; i+=2) {

                //adjust for rounding error
                coords[i+1]=coords[i+1]+adjustY;

                coords[i] = coords[i] - midPoint.getX()+minX;
                coords[i] += cropBox.width / 2;

                coords[i+1] = coords[i+1] - midPoint.getY()+minY;
                coords[i+1] = 0 - coords[i+1];
                coords[i+1] += cropBox.height / 2;

            }
        }

		return coords;
	}

	/**
	 * Return the index of the start coordinate
	 */
	private static int getCoordOffset(int pathCommand)
	{
		int offset;

		switch(pathCommand) {
		case(PathIterator.SEG_CUBICTO):
			offset = 4;
		break;
		case(PathIterator.SEG_QUADTO):
			offset = 2;
		break;
		default:
			offset = 0;
			break;
		}
		return offset;
	}

	/**
	 * Tests whether the line between the two given points crosses over the crop box.
	 * @return true if it misses completely.
	 */
	private static boolean isCompleteCropBoxMiss(double[] start, double[] end, Rectangle cropBox)
	{
		int xLimMin = cropBox.x;
		int xLimMax = xLimMin + cropBox.width;
		int yLimMin = cropBox.y;
		int yLimMax = xLimMax + cropBox.height;

		return ((start[0] < xLimMin && end[0] < xLimMin) || (start[0] > xLimMax && end[0] > xLimMax)) &&
				((start[1] < yLimMin && end[1] < yLimMin) || (start[1] > yLimMax && end[1] > yLimMax));
	}

	/**
	 * Coverts an array of numbers to a String for JavaScript parameters.
	 * Removes cropbox offset.
	 *
	 * @param coords Numbers to change
	 * @param count Use up to count doubles from coords array
	 * @return String Bracketed stringified version of coords
	 * (note numbers rounded to nearest int to keep down filesize)
	 */
	protected String coordsToStringParam(double[] coords, int count)
	{
		//make copy factoring in size
		int size=coords.length;
		double[] copy=new double[size];
		System.arraycopy(coords, 0, copy, 0, size);

		coords=correctCoords(copy);

		return convertCoords(coords, count);

	}

    protected String convertCoords(double[] coords, int count) {

        String result = "";

        int cropBoxW=0,cropBoxH=0;
        if(cropBox!=null){
            cropBoxW=cropBox.width;
            cropBoxH=cropBox.height;
        }

        switch(pageRotation){
            case 90:

                //for each set of coordinates, set value
                for(int i = 0; i<count; i=i+2) {
                    if(i!=0) {
                        result += ",";
                    }

                    if (ctm[0][0] == 0 && ctm[1][1] == 0 && ctm[0][1]>0 &&  (ctm[1][0] < 0||Math.abs(ctm[1][0]) < 0.5)) {//adjust to take account negative shape offset
                        result += setPrecision(((cropBoxH-coords[i+1])*scaling)+yOffset,false);
                        result += ",";
                        result += setPrecision(coords[i]*scaling,(i & 1) !=1);
                    } else {

                        result += setPrecision(((cropBoxH-coords[i+1])*scaling)+yOffset,false);

                        result += ",";
                        result += setPrecision(coords[i]*scaling, (i & 1) !=1);//- (i%2 == 1 ? cropBox.x : cropBox.y) );
                    }
                }
                break;

            case 180:
            {

                //convert x and y values to output coords from PDF
                for(int i = 0; i<count; i=i+2) {

                    //handle x values
                    if(i!=0) {
                        result += ",";
                    }
                    result += setPrecision((cropBoxW-coords[i])*scaling,true);//- (i%2 == 1 ? cropBox.x : cropBox.y) );

                    //handle y values
                    if(i+1 != 0) {
                        result += ",";
                    }
                    //System.out.println(cropBox.height+" "+coords[i+1]+" "+yOffset);
                    result += setPrecision(((cropBoxH-coords[i+1])*scaling)+yOffset,false);//- (i%2 == 1 ? cropBox.x : cropBox.y) );
                }
            }
            break;

            case 270:
            {
                //for each set of coordinates, set value
                for(int i = 0; i<count; i=i+2) {
                    if(i!=0) {
                        result += ",";
                    }

                    if (ctm[0][0] == 0 && ctm[1][1] == 0 && ctm[0][1]>0 &&  (ctm[1][0] < 0||Math.abs(ctm[1][0]) < 0.5)) {//adjust to take account negative shape offset
                        result += setPrecision(((cropBoxH-coords[i+1])*scaling)+yOffset,false);
                        result += ",";
                        result += setPrecision(coords[i]*scaling,(i & 1) !=1);

                    } else {

                        result += setPrecision(((coords[i+1])*scaling)+yOffset,false);
                        //result += setPrecision((cropBox.height-coords[i+1]-pageData.getCropBoxY(pageNumber))*scaling);

                        result += ",";
                        result += setPrecision((cropBoxW-coords[i])*scaling, (i & 1) !=1);//- (i%2 == 1 ? cropBox.x : cropBox.y) );
                        //    result += setPrecision((coords[i]-(pageData.getCropBoxX(pageNumber)/2))*scaling);//- (i%2 == 1 ? cropBox.x : cropBox.y) );
                    }
                }
            }
            break;

            default:
            {

                //convert x and y values to output coords from PDF
                for(int i = 0; i<count; i=i+2) {

                    //handle x values
                    if(i!=0) {
                        result += ",";
                    }
                    result += setPrecision(coords[i]*scaling,true);//- (i%2 == 1 ? cropBox.x : cropBox.y) );

                    //handle y values
                    if(i+1 != 0) {
                        result += ",";
                    }
                    result += setPrecision((coords[i+1]*scaling)+yOffset,false);//- (i%2 == 1 ? cropBox.x : cropBox.y) );
                }
            }
        }
        return result;
    }

	/**
	 * Formats an int to CSS rgb(r,g,b) string
	 *
	 */
	public static String rgbToCSSColor(int raw)
	{
		int r = (raw>>16) & 255;
		int g = (raw>>8) & 255;
		int b = raw & 255;

		return "rgb(" + r + ',' + g + ',' + b + ')';
	}

    public String getContent()
    {
        int size=pathCommands.size();

        StringBuilder result = new StringBuilder(size*15);

        for(int i = 0; i < size; i++) {

            //@TODO Hack to backspace out tab so as to not break test.
            if(i != 0) {
                result.append('\t');
            }
            result.append(pathCommands.get(i));
            if(i != (size - 1)) {
                result.append('\n');
            }
        }

        return result.toString();
    }

	public boolean isEmpty()
	{
		return pathCommands.isEmpty();
	}

	public int getShapeColor()
	{
		return currentColor;
	}

    public double getMinXcoord() {
        return minXcoord;
    }

    public double getMinYcoord() {
        return minYcoord;
    }

    /**
     * Emulates PDF style lines.<br />
     * In a PDF viewer if a line is touching a neighbouring pixel 
     * that pixel is filled regardless.
     * @param lineWidth
     * @return an integer representing the PDF emulated width
     */
    protected int emulatePdfLineWidth(float lineWidth) {
    	if(lineWidth < 1 && lineWidth > 0) return 1;
    	
    	else if(lineWidth > (int) lineWidth) {
    		return (int) lineWidth+1;
    	}
    	
    	else {
    		return (int) lineWidth;
    	}
    }
}
