/*
 * ===========================================
 * Java Pdf Extraction Decoding Access Library
 * ===========================================
 *
 * Project Info:  http://www.idrsolutions.com
 * Help section for developers at http://www.idrsolutions.com/java-pdf-library-support/
 *
 * (C) Copyright 1997-2014, IDRsolutions and Contributors.
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
 * RadialContext.java
 * ---------------
 */
package com.idrsolutions.pdf.color.shading;

import java.awt.Color;
import java.awt.PaintContext;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;


import org.jpedal.color.GenericColorSpace;
import org.jpedal.function.PDFFunction;

public class RadialContext implements PaintContext {
	
	GenericColorSpace shadingColorSpace;
	
	private float scaling=1f;
	
	private final boolean[] isExtended;
	
	private final float x0,x1,y0,y1,r0,r1,t0;
	private float t1=1.0f;
	
	private final PDFFunction[] function;
	
	private float[] cx,cy,cr,crSquared;
	private Color[] circleColor;
	
	private int xstart,ystart,circleCount;

    //private boolean circlesInitialised=false;
	//flag to allow caching on calculation
	private int lastMaxSize=-1;
	
	private final int pageHeight;

	private final int minX;

	private final float[] background;

	private final boolean colorsReversed;

    private final boolean isPrinting;

    private final int offX,offY;

    public RadialContext(final boolean isPrinting, final int offX, final int offY, final int minX, final int pHeight, final float scaling, final boolean[] isExtended,
                         final float[] domain, final float[] coords, final GenericColorSpace shadingColorSpace,
                         final boolean colorsReversed, final float[] background, final PDFFunction[] function){

        //@printIssue not currently used
        this.offX=offX;
        this.offY=offY;

        this.isPrinting=isPrinting;

        this.pageHeight=pHeight;
		this.isExtended=isExtended;
		this.t0=domain[0];
		this.t1=domain[1];
		this.minX = minX;
		this.background = background;
		this.colorsReversed=colorsReversed;
		
		x0=coords[0];
		x1=coords[3];
		r0=coords[2];
		y0=coords[1];
		y1=coords[4];
		r1=coords[5];
		this.shadingColorSpace=shadingColorSpace;
		this.function = function;
		this.scaling=scaling;

    }
    
    @Override
    public void dispose() {

    }

	
	@Override
    public ColorModel getColorModel() { return ColorModel.getRGBdefault(); }
	
	/**
	 * setup the raster with the colors
	 * */
	@Override
    public Raster getRaster(final int xstart, final int ystart, final int w, final int h) {

		this.xstart=xstart;
		this.ystart=ystart;
        
		//setup circles with max size of raster
        double dist0 = Math.sqrt(((x0-xstart)*(x0-xstart))+((y0-ystart)*(y0-ystart)));
        double temp = Math.sqrt(((x0-(xstart+w))*(x0-(xstart+w)))+((y0-ystart)*(y0-ystart)));
        if (temp > dist0) {
            dist0 = temp;
        }
        temp = Math.sqrt(((x0-xstart)*(x0-xstart))+((y0-(ystart+h))*(y0-(ystart+h))));
        if (temp > dist0) {
            dist0 = temp;
        }
        temp = Math.sqrt(((x0-(xstart+w))*(x0-(xstart+w)))+((y0-(ystart+h))*(y0-(ystart+h))));
        if (temp > dist0) {
            dist0 = temp;
        }

        double dist1 = Math.sqrt(((x1-xstart)*(x1-xstart))+((y1-ystart)*(y1-ystart)));
        temp = Math.sqrt(((x1-(xstart+w))*(x1-(xstart+w)))+((y1-ystart)*(y1-ystart)));
        if (temp > dist1) {
            dist1 = temp;
        }
        temp = Math.sqrt(((x1-xstart)*(x1-xstart))+((y1-(ystart+h))*(y1-(ystart+h))));
        if (temp > dist1) {
            dist1 = temp;
        }
        temp = Math.sqrt(((x1-(xstart+w))*(x1-(xstart+w)))+((y1-(ystart+h))*(y1-(ystart+h))));
        if (temp > dist1) {
            dist1 = temp;
        }

        int val = (int)dist0;
        if (dist1 > val) {
            val = (int) dist1;
        }

        val += 20;

		//if(!circlesInitialised){
			//circlesInitialised=true;
			initialiseCircles(val);
		//}
		
		//sets up the array of pixel values
		//WritableRaster raster =getColorModel().createCompatibleWritableRaster(w, h);

        //create buffer to hold all this data
		final int[] data = new int[w * h * 4];

        if(background!=null){
			//y co-ordinates
			for (int y = 0; y < h; y++) {
				
				//x co-ordinates			
				for (int x = 0; x < w; x++) {

					shadingColorSpace.setColor(background,shadingColorSpace.getColorComponentCount());
					final Color c=(Color) shadingColorSpace.getColor();
			
					//set color for the pixel with values
					final int base = (y * w + x) * 4;
					data[base] = c.getRed();
					data[base + 1] = c.getGreen();
					data[base + 2] = c.getBlue();
					data[base + 3] = 255;
					
				}
			}
		}
		
		//y co-ordinates
		for (int y = 0; y < h; y++) {
			
			//x co-ordinates			
			for (int x = 0; x < w; x++) {
				
				final int i=calculateColor(x,y);

				if(i>=0){
					setColor(w, data, y, x, i);
				}
			}
		}
		
		//set values
		//we have to get the raster this way for java me to use the raster as me does not have createCompatableWritableRaster
		final WritableRaster raster = new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB).getRaster();
		raster.setPixels(0, 0, w, h, data);
		
		return raster;
	}
	private void setColor(final int w, final int[] data, final int y, final int x, final int i) {
		
		final int cr;
		final int cg;
		final int cb;

		final Color c=this.circleColor[i];
		cr=c.getRed();
		cg=c.getGreen();
		cb=c.getBlue();
		
		//set color for the pixel with values
		final int base = (y * w + x) * 4;
		data[base] = cr;
		data[base + 1] = cg;
		data[base + 2] = cb;
		data[base + 3] = 255;//(int)(col.getAlpha());
		
	}
	float pdfX;
    float pdfY;
	/**workout rgb color*/
	private int calculateColor(final float x, final float y) {

		/**
		 *take x and y and pass through conversion with domain values - this gives us xx
		 */
        if(isPrinting){

            pdfX=scaling*(((x+xstart))+minX);
            pdfY=scaling*(pageHeight-((y+ystart)));

        }else{
            pdfX=scaling*(x+xstart+minX);
            pdfY=scaling*(pageHeight-(y+ystart));
        }

        final float[] xy = PixelFactory.convertPhysicalToPDF(isPrinting, x, y, offX, offY, scaling, xstart, ystart, minX, pageHeight);
        pdfX = xy[0];
        pdfY = xy[1];

		//number of circles
		int j=-1; //not in
		float rSquared;

		/** draw the circles */
		for (int i = circleCount; i > 0; i--) { // draw all the circles
			rSquared = ((pdfX - cx[i]) * (pdfX - cx[i])) + ((pdfY - cy[i]) * (pdfY - cy[i]));
			if (((rSquared <= crSquared[i]) && (rSquared >= crSquared[i - 1]))
					|| ((rSquared >= crSquared[i]) && (rSquared <= crSquared[i - 1]))) {
				j = i;
				
				break;
			}
		}
		
		/** fill the gaps between the circles */
		if (cr[0] < cr[1]) { // shade from small circle to big
			if (j == -1) {
				for (int i = 0; i < circleCount; i++) {
					rSquared = ((pdfX - cx[i]) * (pdfX - cx[i])) + ((pdfY - cy[i]) * (pdfY - cy[i]));
					final float nextRSquared = ((pdfX - cx[i+1]) * (pdfX - cx[i+1])) + ((pdfY - cy[i+1]) * (pdfY - cy[i+1]));
					
					if ((rSquared > crSquared[i]) && (nextRSquared < crSquared[i+1])){
						j = i;
						break;
					}
				}
			}
		} 
		else {
			for (int i = circleCount; i > 0; i--) {
				rSquared = ((pdfX - cx[i]) * (pdfX - cx[i])) + ((pdfY - cy[i]) * (pdfY - cy[i]));
				final float nextRSquared = ((pdfX - cx[i-1]) * (pdfX - cx[i-1])) + ((pdfY - cy[i-1]) * (pdfY - cy[i-1]));
				
				if ((rSquared > crSquared[i]) && (nextRSquared < crSquared[i-1])){
					j = i;
					break;
				}
			}
		}
		
		return j;
	}
	
	/**work out sets of circles and colors for each*/
	private void initialiseCircles(final int maxSize) {

		//do not reinitialise if next shading smaller as previous values will do
		if(maxSize<lastMaxSize && lastMaxSize>0) {
            return;
        }
		lastMaxSize=maxSize;
		
		circleCount=100;
		
		//do not have more circles than pixels
		if(maxSize<100) {
            circleCount = maxSize;
        }

		circleCount++;
		//create lookups
		cx=new float[circleCount];
		cy=new float[circleCount];
		cr=new float[circleCount];
		crSquared=new float[circleCount];
		circleColor=new Color[circleCount];
		circleCount--;
		
		final float td=(t1-t0);
		final float xd=(x1-x0);
		final float yd=(y1-y0);
		final float rd=(r1-r0);
		
		//see if 1 contained in 0
//		boolean c0Inc1=(((x0-r0)>(x1-r1))&&((x0+r0)<(x1+r1))&&
//		((y0-r0)>(y1-r1))&&((y0+r0)<(y1+r1)));
//
//		boolean c1Inc0=(((x1-r1)>(x0-r0))&&((x1+r1)<(x0+r0))&&
//				((y1-r1)>(y0-r0))&&((y1+r1)<(y0+r0)));

        // setup values (from largest first so we get the biggest circle at the
		// start
		int i = 0;
		float s,t;
		while (true) {
			t = (t1 - t0) * i / circleCount;
			s = (t - t0) / td;

			cx[i] = x0 + (s * xd);
			cy[i] = y0 + (s * yd);
			cr[i] = r0 + (s * rd);

			crSquared[i] = cr[i] * cr[i]; // square it
			
			if(colorsReversed) {
                circleColor[i] = calculateColor(1 - t);
            } else {
                circleColor[i] = calculateColor(t);
            }
			
			if (i == circleCount) {
                break;
            }

			i++;
		}
		
		if (isExtended[0]) {
			i = 0;
			t = (t1 - t0) * i / circleCount;
			s = (t - t0) / td;
			
			if (cr[0] < cr[1]) { // goes from small circle to large
				while ((r0 + (s * rd)) > 0) { // while radius is greater than 0
					t = (t1 - t0) * -i / circleCount;
					s = (t - t0) / td;

					i++;
				}
			}

			else { // goes from large circle to small
                while ((r0 + (s * rd)) < maxSize) { // until radius encoumpases entire box
					t = (t1 - t0) * -i / circleCount;
					s = (t - t0) / td;

					i++;
				}
			}

			if(i != 0){
				final float[] ex = new float[i];
                final float[] ey = new float[i];
                final float[] er = new float[i];
                final float[] erSquared = new float[i];
				final Color[] ecircleColor = new Color[i];

				i--;
				int count = 0;
				while (i >= 0) {
					t = (t1 - t0) * -i / circleCount;
					s = (t - t0) / td;

					ex[count] = x0 + (s * xd);
					ey[count] = y0 + (s * yd);
					er[count] = r0 + (s * rd);

					erSquared[count] = er[count] * er[count]; // square it
					ecircleColor[count] = circleColor[0];

					count++;
					i--;
				}

				cx = concat(ex, cx);
				cy = concat(ey, cy);
				cr = concat(er, cr);

				crSquared = concat(erSquared, crSquared);
				circleColor = concat(ecircleColor, circleColor);

				circleCount = cx.length - 1;

			}
		}
		
		if(isExtended[1]) {
			i=circleCount + 1;
			if (cr[0] > cr[1]) {
				while ((r0 + (s * rd)) > 0) {
					t = (t1 - t0) * i / circleCount;
					s = (t - t0) / td;

					i++;
				}
			}

			else {
				while ((r0 + (s * rd)) < maxSize) {
					t = (t1 - t0) * i / circleCount;
					s = (t - t0) / td;

					i++;
				}
			}

			final float[] ex = new float[i - (circleCount+1)];
            final float[] ey = new float[i - (circleCount+1)];
            final float[] er = new float[i - (circleCount+1)];
            final float[] erSquared = new float[i - (circleCount+1)];
			final Color[] ecircleColor = new Color[i - (circleCount+1)];

			i--;
			int count = i-(circleCount+1);
			
			while (i > circleCount) {
				t = (t1 - t0) * i / circleCount;
				s = (t - t0) / td;

				ex[count] = x0 + (s * xd);
				ey[count] = y0 + (s * yd);
				er[count] = r0 + (s * rd);

				erSquared[count] = er[count] * er[count]; // square it
				ecircleColor[count] = circleColor[circleCount];

				count--;
				i--;
			}

			cx = concat(cx, ex);
			cy = concat(cy, ey);
			cr = concat(cr, er);

			crSquared = concat(crSquared, erSquared);
			circleColor = concat(circleColor, ecircleColor);

			circleCount = cx.length - 1;

		}
	}
	
	/**workout rgb color*/
	private Color calculateColor(final float val) {

        final float[] colValues = ShadingFactory.applyFunctions(function,new float[]{val});

        /**
		 * this value is converted to a color
		 */
		shadingColorSpace.setColor(colValues,colValues.length);

        return (Color) shadingColorSpace.getColor();

	}

    private static float[] concat(final float[] A, final float[] B) {
		final float[] C= new float[A.length+B.length];
		System.arraycopy(A, 0, C, 0, A.length);
		System.arraycopy(B, 0, C, A.length, B.length);
		
		return C;
	}

	private static Color[] concat(final Color[] A, final Color[] B) {
		final Color[] C= new Color[A.length+B.length];
		System.arraycopy(A, 0, C, 0, A.length);
		System.arraycopy(B, 0, C, A.length, B.length);
		
		return C;
	}	
}
