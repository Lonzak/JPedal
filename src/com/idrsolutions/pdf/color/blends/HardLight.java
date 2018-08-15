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
 * HardLight.java
 * ---------------
 */

package com.idrsolutions.pdf.color.blends;

/**
 *
 * @author Simon
 */
public class HardLight extends BMContext{

    public HardLight(final float alpha) {
        super(alpha);
        }
    
    @Override
    int[] blend(final int[] src,int[] dst){
        final int newAlpha = src[3] + dst[3];
        
        if(dst[0]==255 && dst[1]==255 && dst[2]==255){
            if(src[3] == 0){
                dst = new int[]{255,255,255,0};
            }else{
                dst = src;
            }
        }else if (src[0]!=0 || src[1]!=0 || src[2]!=0 || src[3]!=0){ 
       
            for(int i = 0; i < src.length - 1; i++){
                if(src[i] < 128){ // Mutiply
                    dst[i]=(dst[i])*src[i] >> 7;
                }else{ // Screen
                    dst[i] = 255 - ((255-src[i])*(255-(dst[i])) >> 7);
                }
            }
            dst[3] = Math.min(255, newAlpha);
        }
        return dst;
    }
    
}
