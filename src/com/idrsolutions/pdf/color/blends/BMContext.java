/*
 * ===========================================
 * Java Pdf Extraction Decoding Access Library
 * ===========================================
 *
 * Project Info:  http://www.idrsolutions.com
 *
 * List of all example and a link to zip at http://www.idrsolutions.com/java-code-examples-for-pdf-files/
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
 *
 * ---------------
 * BMContext.java
 * ---------------
 */
package com.idrsolutions.pdf.color.blends;

import java.awt.CompositeContext;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 *
 */
abstract class BMContext implements CompositeContext {

    float alpha;
    
    BMContext(final float alpha) {
        this.alpha=alpha;
    }

    @Override
    public void dispose() {
        
    }

    @Override
    //based on code showing how to do this
    //http://www.curious-creature.org/2006/09/20/new-blendings-modes-for-java2d/ 
     public void compose(final Raster src, final Raster dstIn, final WritableRaster dstOut) {
            
         if (src.getSampleModel().getDataType() != DataBuffer.TYPE_INT ||
                dstIn.getSampleModel().getDataType() != DataBuffer.TYPE_INT ||
                dstOut.getSampleModel().getDataType() != DataBuffer.TYPE_INT) {
                throw new IllegalStateException(
                        "Source and destination must store pixels as INT.");
            }

            final int width = Math.min(src.getWidth(), dstIn.getWidth());
            final int height = Math.min(src.getHeight(), dstIn.getHeight());

            final int[] srcPixel = new int[4];
            final int[] dstPixel = new int[4];
            final int[] srcPixels = new int[width];
            final int[] dstPixels = new int[width];

            for (int y = 0; y < height; y++) {
                src.getDataElements(0, y, width, 1, srcPixels);
                dstIn.getDataElements(0, y, width, 1, dstPixels);
                for (int x = 0; x < width; x++) {
                    // pixels are stored as INT_ARGB
                    // our arrays are [R, G, B, A]
                    int pixel = srcPixels[x];
                    srcPixel[0] = (pixel >> 16) & 0xFF;
                    srcPixel[1] = (pixel >>  8) & 0xFF;
                    srcPixel[2] = (pixel      ) & 0xFF;
                    srcPixel[3] = (pixel >> 24) & 0xFF;

                    pixel = dstPixels[x];
                    dstPixel[0] = (pixel >> 16) & 0xFF;
                    dstPixel[1] = (pixel >>  8) & 0xFF;
                    dstPixel[2] = (pixel      ) & 0xFF;
                    dstPixel[3] = (pixel >> 24) & 0xFF;

                    final int[] result =blend(srcPixel, dstPixel);
                    alpha = result[3]/255f;

                    // mixes the result with the opacity
                    dstPixels[x] = ((int) (dstPixel[3] + (result[3] - dstPixel[3]) * alpha) & 0xFF) << 24 |
                                   ((int) (dstPixel[0] + (result[0] - dstPixel[0]) * alpha) & 0xFF) << 16 |
                                   ((int) (dstPixel[1] + (result[1] - dstPixel[1]) * alpha) & 0xFF) <<  8 |
                                    (int) (dstPixel[2] + (result[2] - dstPixel[2]) * alpha) & 0xFF;
                }
                dstOut.setDataElements(0, y, width, 1, dstPixels);
            }
        }
     
     int[] blend(final int[] srcPixel, final int[] dstPixel){
         
         if(1==1) {
             throw new UnsupportedOperationException("Not supported yet.");
         }
         
         return new int[4];
     }

}


