/*
 * ===========================================
 * Java Pdf Extraction Decoding Access Library
 * ===========================================
 *
 * Project Info:  http://www.idrsolutions.com
 * Help section for developers at http://www.idrsolutions.com/java-pdf-library-support/
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
 * Screen.java
 * ---------------
 */

package com.idrsolutions.pdf.color.blends;

/**
 * Screen blendmode
 */
public class Screen extends BMContext{

    public Screen(final float alpha) {
        super(alpha);
    }

    @Override
    int[] blend(final int[] src, final int[] dst) {
        
        int result;
        
        if(dst[0] == 255 && dst[1] == 255 && dst[2] == 255) {
            // Screening with black returns the original color
            if(src[3] == 0){ // Allow for opacity
                dst[3] = 0;
            }else{
                System.arraycopy(src, 0, dst, 0, dst.length);
            }
        }else{
            for(int a=0;a<src.length - 1;a++){
                result = 255 - ((255-src[a])*(255-dst[a]) >> 8);
                if(result<256) {
                    dst[a] = result;
                }
            }
            
            dst[3] = Math.min(255, src[3] + dst[3]);
        }
        return dst;
    }
    
}
