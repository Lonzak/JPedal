/**
 * ===========================================
 * Java Pdf Extraction Decoding Access Library
 * ===========================================
 *
 * Project Info:  http://www.jpedal.org
 * (C) Copyright 1997-2011, IDRsolutions and Contributors.
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
  * Flate.java
  * ---------------
 */
package org.jpedal.io.filter;

import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.utils.LogWriter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.Inflater;

/**
 * flate
 */
public class Flate extends BaseFilter implements PdfFilter{

    //default values
    private int predictor = 1,colors = 1,bitsPerComponent = 8,columns = 1;

    private boolean hasError=false;

    public Flate(PdfObject decodeParms) {

        super(decodeParms);

        if(decodeParms!=null){

            int newBitsPerComponent = decodeParms.getInt(PdfDictionary.BitsPerComponent);
            if(newBitsPerComponent!=-1)
                bitsPerComponent=newBitsPerComponent;

            int newColors = decodeParms.getInt(PdfDictionary.Colors);
            if(newColors!=-1)
                colors=newColors;

            int columnsSet = decodeParms.getInt(PdfDictionary.Columns);
            if(columnsSet!=-1)
                columns=columnsSet;

            predictor = decodeParms.getInt(PdfDictionary.Predictor);
        }

    }

    /**
     * flate decode - use a byte array stream to decompress data in memory
     *
     */
    public byte[] decode(byte[] data) throws Exception {


        int bufSize = 512000;
        ByteArrayOutputStream bos=null;
        boolean failed=true;

        int orgSize=data.length;

        /**
         * decompress byte[]
         */
        if (data != null) {

            while(failed==true){ //sometimes partly valid so loop to try this

                // create a inflater and initialize it Inflater inf=new Inflater();
                Inflater inf = new Inflater();
                inf.setInput(data);

                int size = data.length;
                bos = new ByteArrayOutputStream(size);

                if (size < bufSize)
                    bufSize = size;

                byte[] buf = new byte[bufSize];
                //int debug = 20;
                int count;
                try{
                    while (!inf.finished()) {

                        count = inf.inflate(buf);
                        bos.write(buf, 0, count);

                        if (inf.getRemaining() == 0)
                            break;
                    }

                    failed=false;
                }catch(Exception ee){
                	
                	if(LogWriter.isOutput())
                		LogWriter.writeLog("Exception in Flate "+ee);

                    failed=true;

                    hasError=true;

                    //retrun on small streams
                    if(data.length==orgSize && data.length>10000){
                        failed=false;
                    }else if(data.length>10){
                        byte[] newData=new byte[data.length-1];
                        System.arraycopy(data,0,newData,0,data.length-1);
                        data=newData;
                    }else
                        failed=false;
                }
            }


            data = bos.toByteArray();

            return applyPredictor(predictor, data, colors, bitsPerComponent, columns);

        }

        return data;

    }

    public void decode(BufferedInputStream bis, BufferedOutputStream streamCache, String cacheName, Map cachedObjects) throws Exception{

        this.bis=bis;
        this.streamCache=streamCache;
        this.cachedObjects=cachedObjects;

        /**
         * decompress cached object
         */
        if (bis != null) {

            InputStream inf=null;

            try {

                // create a inflater and initialize it Inflater inf=new
                // Inflater();
                // if((predictor==1) || (predictor==10) )
                inf = new java.util.zip.InflaterInputStream(bis);
                // else
                // inf=new DecodePredictor(null, predictor,params,new
                // java.util.zip.InflaterInputStream(bis));

                while (true) {

                    int b = inf.read();
                    if ((inf.available() == 0) || (b == -1))
                        break;

                    streamCache.write(b);

                }

                if (predictor != 1 && predictor != 10) {
                    streamCache.flush();
                    streamCache.close();
                    if (cacheName != null)
                        setupCachedObjectForDecoding(cacheName);
                }

            } catch (Exception e) {
                e.printStackTrace();
                
                if(LogWriter.isOutput())
                	LogWriter.writeLog("Exception " + e + " accessing Flate filter ");
            }finally {
                if(inf!=null){
                    inf.close();
                }
            }
        }

        applyPredictor(predictor, null, colors, bitsPerComponent, columns);
    }

    public boolean hasError(){
        return hasError;
    }

}
