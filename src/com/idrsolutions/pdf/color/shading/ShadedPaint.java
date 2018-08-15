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
 * ShadedPaint.java
 * ---------------
 */
package com.idrsolutions.pdf.color.shading;

import org.jpedal.color.GenericColorSpace;
import org.jpedal.color.PdfPaint;
import org.jpedal.function.FunctionFactory;
import org.jpedal.function.PDFFunction;
import org.jpedal.io.ObjectDecoder;
import org.jpedal.io.PdfObjectReader;
import org.jpedal.objects.raw.FunctionObject;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.parser.DecoderOptions;
import org.jpedal.render.DynamicVectorRenderer;
import org.jpedal.utils.Matrix;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.io.Serializable;
import java.util.HashMap;

/**
 * template for all shading operations
 */
public class ShadedPaint implements PdfPaint,Paint, Serializable {

	public static final int FUNCTION = 1;
	public static final int AXIAL = 2;
	public static final int RADIAL = 3;
	public static final int FREEFORM = 4;
	public static final int LATTICEFORM = 5;
	public static final int COONS = 6;
	public static final int TENSOR = 7;

	private static final boolean debug=false;

    /**used to show whether we are rendering PDF, turning into HTMl, etc */
    private int renderingType= DynamicVectorRenderer.CREATE_PATTERN;

    protected PDFFunction[] function;

	//protected int pageHeight;

	/**colorspace to use for shading*/
	protected GenericColorSpace shadingColorSpace;

    private PdfObject Shading;

	/**optional bounding box*/
	//protected float[] BBox=null;

	protected float[] coords;

	/**optional flag*/
	//protected boolean AntiAlias=false;

	/**type used - see values in ShadingFactory*/
	protected int shadingType;

	protected float[] domain={0.0f,1.0f};

	private int type;

	private boolean[] isExtended=new boolean[2];
	private boolean colorsReversed;
	private float scaling;
	private int cropX;
	
	private int textX,textY;
	private int cropH;
	private float[] background;
	private boolean isPrinting;

    private HashMap patchShades;

    float[][] matrix;

    private float[][] CTM;
    
	/**read general values*/
	public ShadedPaint(final PdfObject Shading, final boolean isPrinting, final GenericColorSpace shadingColorSpace,
                       final PdfObjectReader currentPdfFile, final float[][] matrix, final boolean colorsReversed, final float[][] CTM){

        this.isPrinting=isPrinting;
		this.colorsReversed=colorsReversed;
		this.type=Shading.getInt(PdfDictionary.ShadingType);
		//this.pageHeight=pageHeight;
        this.CTM = CTM;
        this.matrix=matrix;
        
		init(Shading, shadingColorSpace, currentPdfFile, matrix);

	}
    
    
	public void init(final PdfObject Shading, final GenericColorSpace shadingColorSpace, final PdfObjectReader currentPdfFile, final float[][] matrix){

		/**
		 * read axial specific values not read in generic
		 */
        final boolean[] extension=Shading.getBooleanArray(PdfDictionary.Extend);
        if(extension!=null) {
            isExtended = extension;
        }

        /**
		 * get colorspace
		 */
		this.shadingColorSpace=shadingColorSpace;
        this.Shading=Shading;

		/**
		 * read standard shading values
		 */
		shadingType=Shading.getInt(PdfDictionary.ShadingType);

		background=Shading.getFloatArray(PdfDictionary.Background);

		//BBox=Shading.getFloatArray(PdfDictionary.BBox);

        //AntiAlias=Shading.getBoolean(PdfDictionary.AntiAlias);

        /**
		 * these values appear in several types of shading but not all
		 */
        final PdfObject functionObj=Shading.getDictionary(PdfDictionary.Function);
        final byte[][] keys=Shading.getKeyArray(PdfDictionary.Function);

        /** setup the translation function */
		if(functionObj!=null){
            function=new PDFFunction[1];
			function[0] = FunctionFactory.getFunction(functionObj, currentPdfFile);
        }else if(keys!=null){

            int functionCount=0;
            if(keys!=null) {
                functionCount = keys.length;
            }

            PdfObject functionsObj;
            if(keys!=null){

                final PdfObject[] subFunction=new PdfObject[functionCount];

                String id;
                for(int i=0;i<functionCount;i++){

                    id=new String(keys[i]);

                    if(id.startsWith("<<")){
                        functionsObj=new FunctionObject(1);
                        final ObjectDecoder objectDecoder=new ObjectDecoder(currentPdfFile.getObjectReader());
                        objectDecoder.readDictionaryAsObject(functionsObj,0,keys[i]);
                    }else{
                        functionsObj=new FunctionObject(id);
                        currentPdfFile.readObject(functionsObj);
                    }
                    subFunction[i]=functionsObj;
                }

                function = new PDFFunction[subFunction.length];

                /**
                 * get values for sub stream Function
                 */
                for (int i1 =0,imax= subFunction.length; i1 <imax; i1++){
                    function[i1] = FunctionFactory.getFunction(subFunction[i1], currentPdfFile);
                }
            }
        }

        final float[] newDomain=Shading.getFloatArray(PdfDictionary.Domain);
        if(newDomain!=null) {
            domain = newDomain;
        }

        final float[] Coords=Shading.getFloatArray(PdfDictionary.Coords);
        if(Coords!=null){

            final int len=Coords.length;
            coords=new float[len];
            System.arraycopy(Coords,0,coords,0,len);

            if (matrix != null) {

				if(debug) {
                    Matrix.show(matrix);
                }

		float a = matrix[0][0];
                float b = matrix[0][1];
                final float c = matrix[1][0];
                final float d = matrix[1][1];
                float tx = matrix[2][0];
                float ty = matrix[2][1];

                final float x;
                final float y;
                float x1;
                final float y1;
                
                if(type==AXIAL){ //axial
					x = coords[0]; y = coords[1]; x1 = coords[2]; y1 = coords[3];
					coords[0] = (a * x) + (c * y) + tx;
					coords[1] = (b * x) + (d * y) + ty;
					coords[2] = (a * x1) + (c * y1) + tx;
					coords[3] = (b * x1) + (d * y1) + ty;

					if(debug){
						System.out.println(coords[0]+" "+coords[1]);
						System.out.println(coords[2]+" "+coords[3]);
						System.out.println("=============================");
					}

				}else if(type==RADIAL){  //radial
                                        
                                        if(a<0.1 && a>0 && ty>1024){ //special case case:18893
                                            a = 1.0f;
                                            b = -1.0f;
                                            ty = 1.0f;
                                        }
					//x,y
					x = coords[0]; y = coords[1]; x1 = coords[3]; y1 = coords[4];
					coords[0] = (a * x) + (c * y) + tx;
					coords[1] = (b * x) + (d * y) + ty;
					coords[3] = (a * x1) + (c * y1) + tx;
					coords[4] = (b * x1) + (d * y1) + ty;

					/**/
					//r0
					x1 = coords[2];
					float mx = (a * x1);
					float my = (b * x1);

					coords[2] = (float) Math.sqrt((mx * mx) + (my * my));

					//r1
					x1 = coords[5];
					mx = (a * x1);
					my = (b * x1);			

					coords[5] = (float) Math.sqrt((mx * mx) + (my * my));
					/**/

					if(d<0){
						final float tmp = coords[5];
						coords[5] = coords[2];
						coords[2] = tmp;
						colorsReversed=true;
					}

					if(debug){
						System.out.println("x0 = "+coords[0]+" y0 = "+coords[1]+" r0 = "+coords[2]);
						System.out.println("x1 = "+coords[3]+" y1 = "+coords[4]+" r1 = "+coords[5]);
						System.out.println("=============================");
					}
				}
			}else if(type==AXIAL && DecoderOptions.isRunningOnMac){
				if(coords[1]>coords[3]) {
                    colorsReversed = true;
                }
			}
		}
	}

	@Override
    public PaintContext createContext(final ColorModel cm, final Rectangle db, final Rectangle2D ub,
			final AffineTransform xform, final RenderingHints hints) {
        
		PaintContext pt=null;

		//float printScale=1f;

		//@printIssue - creates the paintContext which converts physical pixels into PDF co-ords and
		//sets colour accordingly. The original code works on screen but not print
		final int offX;
        final int offY;

        if(!isPrinting){
			offX=(int) (xform.getTranslateX()+cropX-(textX*scaling));
			offY=(int) (xform.getTranslateY()-cropH+(textY*scaling));
			
		}else{
			offX=(int) xform.getTranslateX();
			offY=(int) xform.getTranslateY();
			scaling=(float)xform.getScaleY();
		}
        
        
		switch(type){
		case FUNCTION :

            pt= new FunctionContext(cropH,(float)(1f/xform.getScaleX()), shadingColorSpace,colorsReversed, function);

			break;
		case AXIAL :

            pt= new AxialContext(xform,renderingType,isPrinting,offX,offY,cropX,cropH,1f/scaling,isExtended,domain,coords, shadingColorSpace,colorsReversed,background, function);
			
			break;
		case RADIAL :
			
			pt= new RadialContext(isPrinting,offX,offY,cropX,cropH,1f/scaling,isExtended,domain,coords, shadingColorSpace,colorsReversed,background, function);

			break;
		case FREEFORM :
                        pt=new FreeFormContext(shadingColorSpace,null, Shading,matrix,cropH,scaling,offX,offY);
                    	break;
		case LATTICEFORM :
			pt=new LatticeFormContext(shadingColorSpace,null, Shading,matrix,cropH,scaling,offX,offY);
			break;
		case COONS :
            //check store setup and try using cached context
            if (patchShades == null) {
                patchShades = new HashMap();
            }
            pt = (PaintContext)patchShades.get(Shading);
            if (pt == null) {
                pt=new PatchContext(Shading, domain, CTM, shadingColorSpace, matrix, COONS);
                patchShades.put(Shading, pt);
            }
            ((PatchContext)pt).setRect(db);

            //update values
            ((PatchContext)pt).setValues(cropH, 1f/scaling, offX, offY);
            break;
        case TENSOR :
            //check store setup and try using cached context
            if (patchShades == null) {
                patchShades = new HashMap();
            }
            pt = (PaintContext)patchShades.get(Shading);
            if (pt == null) {
                pt=new PatchContext(Shading, domain, CTM, shadingColorSpace, matrix, TENSOR);
                patchShades.put(Shading, pt);
            }
            ((PatchContext)pt).setRect(db);

            //update values
            ((PatchContext)pt).setValues(cropH, 1f/scaling, offX, offY);
			break;

		default:
		break;/**/
		}
		/**/

		return pt;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.Transparency#getTransparency()
	 */
	@Override
    public int getTransparency() {
		return 0;
	}

	@Override
    public void setScaling(final double cropX, final double cropH, final float scaling, final float textX, final float textY){
		this.scaling=scaling;
		this.cropX=(int)cropX;
		this.cropH=(int)cropH;
		this.textX=(int)textX;
		this.textY=(int)textY;
	}

	@Override
    public boolean isPattern() {
		return true;
	}

	@Override
    public int getRGB() {
		return 0;
	}

    /**set type of parsing going on if not PDF
     * - introduced for HTML5 as we render onto image indirectly
     * @param type
     */
    @Override
    public void setRenderingType(final int type) {
        this.renderingType = type;
    }

}
