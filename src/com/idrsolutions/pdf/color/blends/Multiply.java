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
 * Multiply.java
 * ---------------
 */
package com.idrsolutions.pdf.color.blends;

/**
 *
 */
class Multiply extends BMContext {

    Multiply(final float alpha) {
        super(alpha);
    }
    
    @Override
    int[] blend(final int[] src, final int[] dst){
        
        //System.out.println(src[0]+" "+src[1]+" "+src[2]+" "+src[3]+" <> "+dst[0]+" "+dst[1]+" "+dst[2]+" "+dst[3]);
        int result;
        int[] ndst = new int[4];
        
        if(src[0]==0 && src[1]==0 && src[2]==0 && src[3]<10){ 
            ndst = dst;
            dst[3]=255;
           
            //System.out.println(src[0]+" "+src[1]+" "+src[2]+" "+src[3]+" <> "+dst[0]+" "+dst[1]+" "+dst[2]+" "+dst[3]);
        }else if(dst[0]==0 && dst[1]==0 && dst[2]==0 && dst[3]==0){ //allow for white
            ndst = src;

            ndst[3] = (dst[3] + src[3])/2;
        }else if(dst[0]==255 && dst[1]==255 && dst[2]==255 && dst[3]==255){
            ndst=src;  
           //System.out.println(src[0]+" "+src[1]+" "+src[2]+" "+src[3]+" <> "+dst[0]+" "+dst[1]+" "+dst[2]+" "+dst[3]);
            src[3]=255;
        //}else if(dst[3]==255){
         //   ndst=src;  
         //  System.out.println(src[0]+" "+src[1]+" "+src[2]+" "+src[3]+" <> "+dst[0]+" "+dst[1]+" "+dst[2]+" "+dst[3]);
         //   src[3]=255;    
        }else{
            for(int a=0;a<src.length;a++){
                result=dst[a]*src[a] >> 8;
                if(result<256) {
                    ndst[a] = result;
                }
            }
            
            ndst[3]=dst[3];
        }
        return ndst;
     }

}


