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
 * PatchContext.java
 * ---------------
 */
package com.idrsolutions.pdf.color.shading;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.BufferedImage;

import java.util.ArrayList;

import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.color.GenericColorSpace;
import org.jpedal.examples.handlers.DefaultImageHelper;
import org.jpedal.external.ImageHelper;
import org.jpedal.utils.LogWriter;

public class PatchContext implements PaintContext {

    private double scaling=1f;
    
    private ImageHelper images = new DefaultImageHelper();

    private ArrayList patches;

    private  int offX, offY,pHeight;
    private final int type;

    private final float[][] CTM;
    float[][] matrix;
    boolean debugSaveOut;

    private Rectangle deviceBounds;
    // Cache the pattern bounds to improve performance
    private Rectangle2D patternBounds;
    // If the shading is passed from the SH command
    private final boolean isFromSH;
    
    public PatchContext(final PdfObject shadingObj, final float[] domain, final float[][] CTM, final GenericColorSpace shadingColorSpace,
                        final float[][] matrix, final int type){
        patches = new ArrayList();
        this.CTM = CTM;
        this.matrix = matrix;
        this.type = type;
        
        
        /**
         * If the context is initialised from the SH command, CTM and matrix are
         * the same and therefore should share the same reference.
         */
        //noinspection ArrayEquality
        isFromSH = CTM == matrix;
        

        //specific Tensor values
        final int BitsPerComponent=shadingObj.getInt(PdfDictionary.BitsPerComponent);
        final int BitsPerFlag=shadingObj.getInt(PdfDictionary.BitsPerFlag);
        final int BitsPerCoordinate=shadingObj.getInt(PdfDictionary.BitsPerCoordinate);

        final int compCount=shadingColorSpace.getColorComponentCount();

        //Decode the coordinate data
        final byte[] bytes = shadingObj.getDecodedStream();
        final ShadingDatastreamReader c = new ShadingDatastreamReader(bytes,BitsPerCoordinate,BitsPerComponent,compCount,BitsPerFlag,shadingColorSpace,type);
        patches = c.getPatches();
    }
    
    @Override
    public void dispose() {}

    public void setValues(final int pHeight, final float scaling, final int offX, final int offY) {
        this.offX = offX;
        this.offY = offY;
        this.pHeight = pHeight;
        this.scaling = scaling;
    }

    @Override
    public ColorModel getColorModel() { return ColorModel.getRGBdefault(); }

    /**
     * setup the raster with the colors
     * */
    @Override
    public Raster getRaster(int xstart, int ystart, final int w, final int h) {
        double dh, dw, dx, dy;
        final GeneralPath bounds;
        
        if(pHeight == 0){
            final BufferedImage result = new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
            return result.getData();
        }

        /**
         * We have to handle shadings passed from the SH command differently
         * as the CTM and the matrix passed through in the constructor are the both
         * just the CTM. 
         */
        if(isFromSH){
            final Rectangle2D patternRect = getPatternBounds();
            
            if(patternRect == null){
                return new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB).getData();
            }
            
            bounds = new GeneralPath(patternRect);

            dw = deviceBounds.width / bounds.getBounds2D().getWidth();
            dh = deviceBounds.height / bounds.getBounds2D().getHeight();
            
            dx = -((bounds.getBounds2D().getX() * dw) + (xstart - deviceBounds.getX()));
            dy = -((bounds.getBounds2D().getY() * dh) + (ystart - deviceBounds.getY()));
        }else{
            //check if CTM is set and use other matrix if not
            boolean flip = false;
            if (CTM[0][0]==1 && CTM[1][1]==1) {
                dx = matrix[2][0];
                dy = matrix[2][1];
                dw = matrix[0][0];
                dh = -matrix[1][1];

            } else {
                dx = CTM[2][0];
                dy = CTM[2][1];
                dw = CTM[0][0];
                dh = -CTM[1][1];

                if (dh < 0) {
                    flip = true;
                }
            }
            if (Math.abs(dh) < 1) {
                dh *= 255f;
            }
            if (Math.abs(dw) < 1) {
                dw *= 255f;
            }

            if (type == ShadedPaint.TENSOR) {
                if(CTM[0][0]!=1) {
                    dw = 1.1*matrix[0][0]/CTM[0][0];
                    dh = 1.1*matrix[1][1]/CTM[1][1];
                } else {
                    dx /= 2;
                    dy /= 2;
                    dw = matrix[0][0]*1.05;
                    dh = matrix[1][1]*1.05;
                }
            }

            if (flip) {
                dy -= dh;
                dh = - dh;
            }

            //convert to screen coords
            dx /= scaling;
            dy = pHeight - (dy / scaling);
            dh /= scaling;
            dw /= scaling;

            //include offset in screen raster coords
            xstart -= offX;
            ystart -= offY;

            //Generate shade coords version of raster bounds
            final float shadeCoordsLeft=(float) ((xstart-dx)/dw);
            final float shadeCoordsTop=(float) ((ystart-dy)/dh);
            final float shadeCoordsRight=(float) (shadeCoordsLeft+(w/dw));
            final float shadeCoordsBottom=(float) (shadeCoordsTop+(h/dh));
            bounds = new GeneralPath();
            bounds.moveTo(shadeCoordsLeft,  shadeCoordsTop);
            bounds.lineTo(shadeCoordsRight, shadeCoordsTop);
            bounds.lineTo(shadeCoordsRight, shadeCoordsBottom);
            bounds.lineTo(shadeCoordsLeft,  shadeCoordsBottom);
            bounds.lineTo(shadeCoordsLeft,  shadeCoordsTop);
            
            dx -= xstart;
            dy -= ystart;
        }
        
        //Create image and set transform
        final BufferedImage result = new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g2 = ((Graphics2D)result.getGraphics());
        final AffineTransform trans = new AffineTransform();
        trans.translate(dx,dy);
        trans.scale(dw, dh);
        g2.setTransform(trans);
               
        //draw background if more than one patch
        final boolean drawBack = patches.size() != 1;

        //Draw patches onto raster
        for (final Object patche1 : patches) {
            final CoonsPatch p = (CoonsPatch) patche1;
            //check it won't draw offscreen
            if (p.getBounds2D().intersects(bounds.getBounds2D())) {
                p.printOntoG2(g2, bounds.getBounds2D(), drawBack);
            }
        }
        
        if (debugSaveOut) {
            final BufferedImage out = new BufferedImage(500,500,BufferedImage.TYPE_INT_ARGB);
            final Graphics2D og2 = ((Graphics2D)out.getGraphics());
            final AffineTransform otrans = new AffineTransform();
            otrans.scale(500,500);
            og2.setTransform(otrans);

            //Draw patches onto raster
            for (final Object patche : patches) {
                final CoonsPatch p = (CoonsPatch) patche;
                //check it won't draw offscreen
                p.printOntoG2(og2, bounds.getBounds2D(), drawBack);
            }

            try {
                this.images.write(out, "png","C:/users/sam/desktop/new/"+this+".png");
            } catch (final Exception e) {
                //tell user and log
                if(LogWriter.isOutput()) {
                    LogWriter.writeLog("Exception: " + e.getMessage());
                }
            }
        }

        return result.getData();
    }
    /**
     * Used when Tensor is passed in via SH command
     * @param db Bounding rectangle of the shade in device space
     */
    public void setRect(final Rectangle db){
        deviceBounds = db;
    }
    
    /**
     * Get the bounds of the shading in Pattern space. 
     * Used to scale the pattern to device space.
     */
    private Rectangle2D getPatternBounds(){
        if(patternBounds != null) {
            return patternBounds;
        }
        
        double minX, minY, maxX, maxY;
        if(patches.size() < 1) {
            return null;
        }
        
        // Set values to compare against
        final CoonsPatch firstPatch = (CoonsPatch)patches.get(0);
        Rectangle2D firstRect = firstPatch.getBounds2D();
        minX = firstRect.getMinX();
        minY = firstRect.getMinY();
        maxX = firstRect.getMaxX();
        maxY = firstRect.getMaxY();
        
        for(int i = 1; i < patches.size(); i++){
            final CoonsPatch p = (CoonsPatch)patches.get(i);
            Rectangle2D pRect = p.getBounds2D();
            if(pRect.getMinX() < minX){
                minX = pRect.getMinX();
            }
            if(pRect.getMinY() < minY){
                minY = pRect.getMinY();
            }
            if(pRect.getMaxX() > maxX){
                maxX = pRect.getMaxX();
            }
            if(pRect.getMaxY() > maxY){
                maxY = pRect.getMaxY();
            }
        }
        
        final GeneralPath bounds = new GeneralPath();
        bounds.moveTo(minX, minY);
        bounds.lineTo(maxX, minY);
        bounds.lineTo(maxX, maxY);
        bounds.lineTo(minX, maxY);
        
        patternBounds = bounds.getBounds2D();
        
        return bounds.getBounds2D();
    }

    
}