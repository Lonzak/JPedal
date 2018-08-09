/**
 * ===========================================
 * Java Pdf Extraction Decoding Access Library
 * ===========================================
 *
 * Project Info:  http://www.jpedal.org
 * (C) Copyright 1997-2008, IDRsolutions and Contributors.
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
  * GraphicsState.java
  * ---------------
 */
package org.jpedal.objects;
import java.awt.BasicStroke;
import java.awt.Stroke;
import java.awt.geom.Area;

import org.jpedal.color.DeviceRGBColorSpace;
import org.jpedal.color.GenericColorSpace;
import org.jpedal.color.PdfColor;
import org.jpedal.color.PdfPaint;
import org.jpedal.objects.raw.PdfArrayIterator;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.objects.raw.PdfDictionary;

/**
 * holds the graphics state as stream decoded
 */
public class GraphicsState
{

    //hold image co-ords
    public float x,y;

    TextState currentTextState=new TextState();

    //transparency
    private float strokeAlpha=1.0f;
    private float nonstrokeAlpha=1.0f;

    private float maxStrokeAlpha=1.0f;
    private float maxNonstrokeAlpha=1.0f;

    /** copy used for images*/
    public float[][] lastCTM = new float[3][3];

    //TR value
    private PdfObject TR;

    public PdfObject SMask;

    /**stroke colorspace*/
    public GenericColorSpace strokeColorSpace=new DeviceRGBColorSpace();

    /**nonstroke colorspace*/
    public GenericColorSpace nonstrokeColorSpace=new DeviceRGBColorSpace();

    private boolean hasClipChanged=false;

    //overprinting
    private boolean op=false;
    private boolean OP=false;

    private float OPM=0;

    private PdfPaint nonstrokeColor=new PdfColor(0,0,0);
    private PdfPaint strokeColor=new PdfColor(0,0,0);

    /**holds current clipping shape*/
    private Area current_clipping_shape = null;

    private final boolean debugClip=false;

    /** CTM which is used for plotting (see pdf
     *  spec for explanation*/
    public float[][] CTM = new float[3][3];

    /**dash of lines (phase) for drawing*/
    private int current_line_dash_phase = 0;

    /**used for TR effect*/
    private Area TRmask=null;

    /**fill type for drawing*/
    private int fill_type;

    /**mitre limit for drawing */
    private int mitre_limit = 0;

    /**dash of lines (array) for drawing*/
    private float[] current_line_dash_array=new float[0];

    /**join style of lines for drawing*/
    private int current_line_cap_style = 0;

    /**width of lines for drawing*/
    private float current_line_width = 1;

    /**join of lines for drawing*/
    private int current_line_join_style = 0;

    /**Type of draw to use*/
    private int text_render_type = GraphicsState.FILL;

    /**displacement to allow for negative page displacement*/
    private int minX=0;//%%

    /**displacement to allow for negative page displacement*/
    private int minY=0;//%%

    public static final int STROKE = 1;

    public static final int FILL = 2;

    public static final int FILLSTROKE = 3;

    public static final int INVISIBLE = 4;

    public static final int CLIPTEXT=7;

    private PdfArrayIterator BM;

    public GraphicsState(){
        resetCTM();
    }

    /**
     * initialise the GraphicsState
     * @param minX
     * @param minY
     */
    public GraphicsState(int minX,int minY)
    {
        this.minX=-minX;/*keep%%*/
        this.minY=-minY;/*keep%%*/
        resetCTM();

    }

    public void setMaxAlpha(int type, float value){

        switch(type){

            case STROKE:

                this.maxStrokeAlpha=value;
                break;

            case FILL :
                this.maxNonstrokeAlpha=value;
                break;

        }
    }

    public void setAlpha(int type, float value){

        switch(type){

            case STROKE:

                if(value>maxStrokeAlpha)
                    value=maxStrokeAlpha;
                this.strokeAlpha=value;
                break;

            case FILL :
                if(value>maxNonstrokeAlpha)
                    value=maxNonstrokeAlpha;
                this.nonstrokeAlpha=value;
                break;

        }
    }

    /**use STROKE or FILL
     * @param type
     * @return*/
    public float getAlpha(int type){

	float value=1f;

        switch(type){

            case STROKE:
		if(strokeAlpha>maxStrokeAlpha){
		    value=maxStrokeAlpha;
		}else{
		    value=this.strokeAlpha;
		}
                break;

            case FILL :
		if(nonstrokeAlpha>maxNonstrokeAlpha){
		    value=maxNonstrokeAlpha;
		}else{
		    value=this.nonstrokeAlpha;
		}
                break;

        }

        return value;
    }

    /**use STROKE or FILL
     * @param type
     * @return*/
    public float getAlphaMax(int type){

        float value=1f;

        switch(type){

            case STROKE:
                value=this.maxStrokeAlpha ;
                break;

            case FILL :
                value=this.maxNonstrokeAlpha;
                break;

        }

        return value;
    }

    /**get stroke op*
    public boolean getStrokeOP(){
        return this.OP;
    } /**/

    /**get stroke op
     * @return*/
    public boolean getNonStrokeOP(){
        return this.op;
    }

    /**get stroke op
     * @return*/
    public float getOPM(){
        return this.OPM;
    }

    public PdfObject getTR() {
        return TR;
    }

    //////////////////////////////////////////////////////////////////////////
    /**
     * set text render type
     * @param text_render_type
     */
    final public void setTextRenderType( int text_render_type )
    {
        this.text_render_type = text_render_type;

        TRmask=null;
    }

    //////////////////////////////////////////////////////////////////////////
    /**
     * set text render type
     * @return
     */
    final public int getTextRenderType()
    {
        return text_render_type;
    }

    ///////////////////////////////////////////////////////////////////////////
    /**
     * set mitre limit
     * @param mitre_limit
     */
    final public void setMitreLimit( int mitre_limit )
    {
        this.mitre_limit = mitre_limit;
    }

    /**
     * get line width
     * @return
     */
    final public float getLineWidth()
    {
        return current_line_width;
    }
    //////////////////////////////////////////////////////////////////////////
    /**
     * set fill type
     * @param fill_type
     */
    final public void setFillType( int fill_type )
    {
        this.fill_type = fill_type;
    }
    //////////////////////////////////////////////////////////////////////////
    /**
     * update clip
     * @param current_area
     */
    final public void updateClip( Area current_area )
    {
        //  System.out.println("Update clip "+current_area.getBounds());
        if( current_clipping_shape == null || current_area==null){
            current_clipping_shape = current_area;
            hasClipChanged=true;
        }else{// if(current_clipping_shape.intersects(current_area.getBounds2D().getX(),current_area.getBounds2D().getY(),
            //current_area.getBounds2D().getWidth(),current_area.getBounds2D().getHeight())){
            current_clipping_shape.intersect( current_area );
            /**if( current_clipping_shape.getBounds().getHeight() == 0 ){
             current_clipping_shape = current_area;
             System.out.println("Reset to area");
             }*/
            hasClipChanged=true;
        }

        if(debugClip){
            System.out.println("[checkWholePageClip]");
            if(current_clipping_shape==null){
                System.out.println("Null shape");
            }else{
                System.out.println("Shape bounds= "+current_clipping_shape.getBounds());
            }
        }
    }

    /**
     * add to clip (used for TR 7)
     * @param current_area
     */
    final public void addClip( Area current_area )
    {

        if(TRmask==null)
            TRmask = current_area;
        else{
            TRmask.add(current_area);
        }
    }
    //////////////////////////////////////////////////////////////////////////
    /**
     * set the current stroke to be used - basic solid line or pattern
     * @return
     */
    final public Stroke getStroke()
    {

        //hold the stroke for the path
        Stroke current_stroke;

        //factor in scaling to line width
        float w=current_line_width;
        if(CTM[0][0]!=0)
            w=w*CTM[0][0];
        else if( CTM[0][1]!=0)
            w=w*CTM[0][1];

        if(w<0)
            w=-w;

        //check values all in legal boundaries
        if( mitre_limit < 1 )
            mitre_limit = 1;

        int dashCount=current_line_dash_array.length;
        if( dashCount > 0 ){
            //factor in scaling
            float[] dash=new float[dashCount];
            for(int aa=0;aa<dashCount;aa++){
                if(CTM[0][0]!=0)
                    dash[aa]=current_line_dash_array[aa]*CTM[0][0];
                else
                    dash[aa]=current_line_dash_array[aa]*CTM[0][1];

                if(dash[aa]<0)
                    dash[aa]=-dash[aa];

                //smaller values throw a segmentation fault in JVM (not pretty)
                if(dash[aa]<0.05f){
                    dash[aa]=0.05f;
                }
            }
            current_stroke = new BasicStroke( w, current_line_cap_style, current_line_join_style, mitre_limit, dash, current_line_dash_phase );
        }else{
            current_stroke = new BasicStroke( w, current_line_cap_style, current_line_join_style, mitre_limit );
        }


        return current_stroke;
    }

    //////////////////////////////////////////////////////////////////////////
    /**
     * set line width
     * @param current_line_width
     */
    final public void setLineWidth( float current_line_width )
    {
        this.current_line_width = current_line_width;
    }
    /**
     * get clipping shape
     * @return
     */
    final public Area getClippingShape()
    {
        if(TRmask!=null && current_clipping_shape==null){
            return TRmask;
        }else if(TRmask!=null){

            //    if(TRmask.intersects(current_clipping_shape.getBounds()))
            TRmask.intersect(current_clipping_shape);

            if(debugClip){
                System.out.println("[getClippingShape1]");
                if(current_clipping_shape==null){
                    System.out.println("Null shape");
                }else{
                    System.out.println("Shape bounds= "+current_clipping_shape.getBounds());
                }
            }

            return TRmask;
        }else{

            if(debugClip){
                System.out.println("[getClippingShape2]");
                if(current_clipping_shape==null){
                    System.out.println("Null shape");
                }else{
                    System.out.println("Shape bounds= "+current_clipping_shape.getBounds());
                }
            }

            return current_clipping_shape;
        }
    }

    /**read GS settings and set supported values
     * @param GS*/
    public void setMode(PdfObject GS){

        /**set to defaults*/
        //strokeAlpha=1.0f;
        nonstrokeAlpha=1.0f;
        op=false;
        OP=false;

        if(GS==null)
            return;

        float LW=GS.getFloatNumber(PdfDictionary.LW);

        if(LW!=-1)
            current_line_width=LW;

        /**
         * set transparency
         */
        boolean AIS=GS.getBoolean(PdfDictionary.AIS);
        SMask=GS.getDictionary(PdfDictionary.SMask);

        boolean notMask=(SMask==null || AIS || SMask.getGeneralType(PdfDictionary.SMask)==PdfDictionary.None);

        if(notMask){
            float newCA=GS.getFloatNumber(PdfDictionary.CA), newca=GS.getFloatNumber(PdfDictionary.ca);
            if(newCA!=-1)
                setAlpha(GraphicsState.STROKE,newCA);

            if(newca!=-1)
                setAlpha(GraphicsState.FILL,newca);
        }

        //set overprinting
        OP=GS.getBoolean(PdfDictionary.OP);
        op=GS.getBoolean(PdfDictionary.op);

        float newOPM=GS.getFloatNumber(PdfDictionary.OPM);
        if(newOPM!=-1)
            OPM=newOPM;
        else
            OPM=0;

        TR=GS.getDictionary(PdfDictionary.TR);

        //transferFunction
        if(TR!=null){

            boolean isIdentity=false;

            if(TR.getGeneralType(PdfDictionary.TR)==PdfDictionary.Identity){
                isIdentity=true;
            }else{
                byte[][] maskArray=TR.getKeyArray(PdfDictionary.TR);

                //see if object or colors
                if(maskArray!=null){

                    int count=maskArray.length;
                    if(count>0){

                        isIdentity=true;

                        for (byte[] aMaskArray : maskArray) {
                            int nextID = PdfDictionary.getIntKey(1, aMaskArray.length - 1, aMaskArray);

                            //System.out.println("ii="+ii+" "+nextID+" "+PdfDictionary.Identity+" "+new String(maskArray[ii]));

                            if (nextID != PdfDictionary.Identity) {
                                isIdentity = false;
                                break;
                            }
                        }
                    }
                }
            }

            if(isIdentity)
                TR=null;

        }

        //set BM if present
        BM = GS.getMixedArray(PdfDictionary.BM);

    }


    /**
     * set line join style
     * @param cap_style
     */
    final public void setCapStyle( int cap_style )
    {
        this.current_line_cap_style = cap_style;
    }

    /**
     * set line join style
     * @param join_style
     */
    final public void setJoinStyle( int join_style )
    {
        this.current_line_join_style = join_style;
    }

    /**
     * check whole page clip (if whole page set clip to null)
     * @param max_y
     */
    final public void checkWholePageClip( int max_y )
    {

        if(current_clipping_shape != null && current_clipping_shape.getBounds().getHeight() > max_y+2){ //2 is margin for error needed on some files

            if(current_clipping_shape.getBounds().y>=0){
                current_clipping_shape = null;

                hasClipChanged=true;

                if(debugClip){
                    System.out.println("[checkWholePageClip]");
                    if(current_clipping_shape==null){
                        System.out.println("Null shape");
                    }else{
                        System.out.println("Shape bounds= "+current_clipping_shape.getBounds());
                    }
                }
            }
        }
    }

    /**
     * set dash array
     * @param current_line_dash_array
     */
    final public void setDashArray( float[] current_line_dash_array ){
        this.current_line_dash_array = current_line_dash_array;
    }


    /**
     * custom clone method
     */
    @SuppressWarnings({"CloneDoesntCallSuperClone"})
    final public Object clone(){

        GraphicsState newGS=new GraphicsState();

        newGS.x=x;
        newGS.y=y;

        if(TR!=null)
            newGS.TR=(PdfObject) TR.clone();

        newGS.maxNonstrokeAlpha=maxNonstrokeAlpha;
        newGS.maxStrokeAlpha=maxStrokeAlpha;

        newGS.strokeAlpha = strokeAlpha;
        newGS.nonstrokeAlpha = nonstrokeAlpha;

        newGS.op = op;
        newGS.OP = OP;
        newGS.OPM = OPM;

        newGS.nonstrokeColor = nonstrokeColor;
        newGS.strokeColor = strokeColor;

        if(current_clipping_shape != null)
            newGS.current_clipping_shape = (Area)current_clipping_shape.clone();

        if(CTM != null){
            for(int i=0;i<3;i++){
                System.arraycopy(CTM[i], 0, newGS.CTM[i], 0, 3);
            }
        }

        newGS.hasClipChanged=hasClipChanged;


        newGS.current_line_dash_phase = current_line_dash_phase;

        if(TRmask != null)
            newGS.TRmask = (Area)TRmask.clone();


        newGS.fill_type = fill_type;

        newGS.mitre_limit = mitre_limit;

        if(current_line_dash_array!=null){
            int size=current_line_dash_array.length;
            newGS.current_line_dash_array=new float[size];
            System.arraycopy(current_line_dash_array, 0, newGS.current_line_dash_array, 0, size);
        }

        newGS.current_line_cap_style = current_line_cap_style;

        newGS.current_line_width = current_line_width;

        newGS.current_line_join_style = current_line_join_style;

        newGS.text_render_type = text_render_type;

        newGS.minX = minX;

        newGS.minY = minY;

        return newGS;
    }

    /**
     * reset CTM
     */
    private void resetCTM()
    {
        //init CTM
        CTM[0][0] = (float)1.0;
        CTM[1][0] = (float)0.0;
        CTM[2][0] = minX;
        CTM[0][1] = (float)0.0;
        CTM[1][1] = (float)1.0;
        CTM[2][1] = minY;
        CTM[0][2] = (float)0.0;
        CTM[1][2] = (float)0.0;
        CTM[2][2] = (float)1.0;

    }

    /**
     * set dash phase
     * @param current_line_dash_phase
     */
    final public void setDashPhase( int current_line_dash_phase )
    {
        this.current_line_dash_phase = current_line_dash_phase;
    }

    /**
     * get fill type
     * @return
     */
    final public int getFillType()
    {
        return fill_type;
    }

    /**
     * set clipping shape
     * @param new_clip
     */
    final public void setClippingShape( Area new_clip )
    {
        this.current_clipping_shape = new_clip;

        hasClipChanged=true;

        if(debugClip){
            System.out.println("[setClippingShape]");

            if(current_clipping_shape==null){
                System.out.println("Null shape");
            }else{
                System.out.println("Shape bounds= "+current_clipping_shape.getBounds());
            }
        }

    }
    /**
     * @return Returns the currentNonstrokeColor.
     */
    public PdfPaint getNonstrokeColor() {
        return nonstrokeColor;
    }

    /**
     * @param currentNonstrokeColor The currentNonstrokeColor to set.
     */
    public void setNonstrokeColor(PdfPaint currentNonstrokeColor) {
        this.nonstrokeColor = currentNonstrokeColor;
    }

    /**
     * @return Returns the strokeColor.
     */
    public PdfPaint getStrokeColor() {
        return strokeColor;
    }

    /**
     * @param strokeColor The strokeColor to set.
     */
    public void setStrokeColor(PdfPaint strokeColor) {
        this.strokeColor = strokeColor;
    }

    public PdfArrayIterator getBM() {
        return BM;
    }

    /**
     * tell software if clip has changed and
     * return
     * @return
     */
    public boolean hasClipChanged() {

        boolean flag=hasClipChanged;

        hasClipChanged=false;
        return flag;
    }

    public void setTextState(TextState currentTextState) {
        this.currentTextState=currentTextState;
    }

    public TextState getTextState() {
        return currentTextState;
    }
}
