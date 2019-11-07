/*
 * ===========================================
 * Java Pdf Extraction Decoding Access Library
 * ===========================================
 *
 * Project Info:  http://www.idrsolutions.com
 * Help section for developers at http://www.idrsolutions.com/java-pdf-library-support/
 *
 * (C) Copyright 1997-2013, IDRsolutions and Contributors.
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
 * FormObject.java
 * ---------------
 */
package org.jpedal.objects.raw;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.jpedal.color.DeviceCMYKColorSpace;
import org.jpedal.objects.acroforms.actions.ActionHandler;
import org.jpedal.objects.acroforms.formData.ComponentData;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.ObjectCloneFactory;
import org.jpedal.utils.StringUtils;
//<start-adobe><start-thin>
//<end-thin><end-adobe>

public class FormObject extends PdfObject {

	String nameUsed = "";

	private boolean tested = false;

	private int formType = -1;

	Object guiComp = null;

	int guiType = -1;

	// rotation on page
	private int rawRotation = 0;

	// show if a kid (which is linked to other objects
	private boolean isKid = false;

	// unknown CMAP as String
	private String EOPROPtype, Filter = null, Location = null, M, Reason, SubFilter;
	private byte[] rawEOPROPtype, rawFilter, rawLocation, rawM, rawReason, rawSubFilter;

	/**
	 * the C color for annotations
	 */
	private Color cColor;
	/**
	 * the contents for any text display on the annotation
	 */
	private String contents;
	/**
	 * whether the annotation is being displayed or not by default
	 */
	private boolean show = false;

	private Map OptValues = null; // values from Opt

	private boolean popupBuilt = false;

	private Object popupObj;

	/** 1 form flag indexes for the field flags */
	public static final int READONLY_ID = 1;
	/** 2 form flag indexes for the field flags */
	public static final int REQUIRED_ID = 2;
	/** 3 form flag indexes for the field flags */
	public static final int NOEXPORT_ID = 3;
	/** 13 form flag indexes for the field flags */
	public static final int MULTILINE_ID = 13;
	/** 14 form flag indexes for the field flags */
	public static final int PASSWORD_ID = 14;
	/** 15 form flag indexes for the field flags */
	public static final int NOTOGGLETOOFF_ID = 15;
	/** 16 form flag indexes for the field flags */
	public static final int RADIO_ID = 16;
	/** 17 form flag indexes for the field flags */
	public static final int PUSHBUTTON_ID = 17;
	/** 18 form flag indexes for the field flags */
	public static final int COMBO_ID = 18;
	/** 19 form flag indexes for the field flags */
	public static final int EDIT_ID = 19;
	/** 20 form flag indexes for the field flags */
	public static final int SORT_ID = 20;
	/** 21 form flag indexes for the field flags */
	public static final int FILESELECT_ID = 21;
	/** 22 form flag indexes for the field flags */
	public static final int MULTISELECT_ID = 22;
	/** 23 form flag indexes for the field flags */
	public static final int DONOTSPELLCHECK_ID = 23;
	/** 24 form flag indexes for the field flags */
	public static final int DONOTSCROLL_ID = 24;
	/** 25 form flag indexes for the field flags */
	public static final int COMB_ID = 25;
	/** 26 form flag indexes for the field flags */
	public static final int RICHTEXT_ID = 26;// same as RADIOINUNISON_ID (radio buttons)
	/** 26 form flag indexes for the field flags */
	public static final int RADIOINUNISON_ID = 26;// same as RICHTEXT_ID (text fields)
	/** 27 form flag indexes for the field flags */
	public static final int COMMITONSELCHANGE_ID = 27;

	/*
	 * variables for forms to check with the (Ff) flags field (1<<bit position -1), to get required result
	 */
	final private static int READONLY_BIT = (1);// 1
	final private static int REQUIRED_BIT = (1 << 1);// 2
	final private static int NOEXPORT_BIT = (1 << 2);// 4
	final private static int MULTILINE_BIT = (1 << 12);// 4096;
	final private static int PASSWORD_BIT = (1 << 13);// 8192;
	final private static int NOTOGGLETOOFF_BIT = (1 << 14);// 16384;
	final private static int RADIO_BIT = (1 << 15);// 32768;
	final private static int PUSHBUTTON_BIT = (1 << 16);// 65536;
	final private static int COMBO_BIT = (1 << 17);// 131072;
	final private static int EDIT_BIT = (1 << 18);// 262144;
	final private static int SORT_BIT = (1 << 19);// 524288;
	final private static int FILESELECT_BIT = (1 << 20);// 1048576
	final private static int MULTISELECT_BIT = (1 << 21);// 2097152
	final private static int DONOTSPELLCHECK_BIT = (1 << 22);// 4194304
	final private static int DONOTSCROLL_BIT = (1 << 23);// 8388608
	final private static int COMB_BIT = (1 << 24);// 16777216
	final private static int RADIOINUNISON_BIT = (1 << 25);// 33554432 //same as RICHTEXT_ID
	final private static int RICHTEXT_BIT = (1 << 25);// 33554432 //same as RADIOINUNISON_ID
	final private static int COMMITONSELCHANGE_BIT = (1 << 26);// 67108864

	protected String[] OptString = null;

	protected boolean isXFAObject = false;
	private String parentRef;
	private PdfObject parentPdfObj;
	private String selectedItem;

	private boolean textColorChanged = false;
	private float[] textColor;
	private Font textFont;
	private int textSize = -1;
	private String textString = null, lastTextString = null;

	private boolean appearancesUsed = false;
	private boolean offsetDownIcon = false;
	private boolean noDownIcon = false;
	private boolean invertDownIcon = false;

	private String onState;
	private String currentState;
	private String normalOnState;
	private BufferedImage normalOffImage = null;
	private BufferedImage normalOnImage;
	private BufferedImage rolloverOffImage = null;
	private BufferedImage rolloverOnImage;
	private BufferedImage downOffImage = null;
	private BufferedImage downOnImage;

	// flag used to handle POPUP internally
	public static final int POPUP = 1;

	private String layerName = null;

	private ActionHandler formHandler;

	private boolean[] Farray = null;

	protected Rectangle BBox = null;

	protected float[] C, QuadPoints, RD, Rect;

	protected boolean[] flags = null;

	boolean Open = true, H_Boolean = true; // note default it true but false in Popup!!

	boolean NeedAppearances = false;

	protected int F = -1, Ff = -1, MaxLen = -1, W = -1;

	protected int Q = -1; // default value

	int SigFlags = -1, StructParent = -1;

	protected int TI = -1;

	protected PdfObject A;

	// internal flag used to store status on additional actions when we decode
	private int popupFlag = 0;

	protected PdfObject AA, AP = null, Cdict;

	private PdfObject BI;

	protected PdfObject BS;

	protected PdfObject D, IF;

	protected PdfObject RichMediaContent;

	/**
	 * Filters the MK command and its properties
	 * <p/>
	 * appearance characteristics dictionary (all optional) R rotation on wiget relative to page BC array of numbers, range between 0-1 specifiying
	 * the border color number of array elements defines type of colorspace 0=transparant 1=gray 3=rgb 4=cmyk BG same as BC but specifies wigets
	 * background color
	 * <p/>
	 * buttons only - CA its normal caption text
	 * <p/>
	 * pushbuttons only - RC rollover caption text AC down caption text I formXObject defining its normal icon RI formXObject defining its rollover
	 * icon IX formXObject defining its down icon IF icon fit dictionary, how to fit its icon into its rectangle (if specified must contain all
	 * following) SW when it should be scaled to fit ( default A) A always B when icon is bigger S when icon is smaller N never S type of scaling -
	 * (default P) P keep aspect ratio A ignore aspect ratio (fit exactly to width and hight) A array of 2 numbers specifying its location when scaled
	 * keeping the aspect ratio range between 0.0-1.0, [x y] would be positioned x acress, y up TP positioning of text relative to icon - (integer)
	 * 0=caption only 1=icon only 2=caption below icon 3=caption above icon 4=caption on right of icon 5=caption on left of icon 6=caption overlaid
	 * ontop of icon
	 */
	private PdfObject MK;

	private PdfObject DC, DP, DR, DS, E, Fdict, Fo, FS, JS, K, Nobj, Next, O, PC, PI, PO, Popup, PV, R, Sig, Sound, U, V, Win, WP, WS, X;

	protected int[] ByteRange, I;

	protected byte[] rawAS, rawCert, rawContactInfo, rawContents, rawDstring, rawDA, rawDV, rawFstring, rawJS, rawH, rawN, rawNM, rawPstring, rawRC,
			rawS, rawSubj, rawT, rawTM, rawTU, rawURI, rawV, rawX;

	protected int FT = -1;

	protected String AS, Cert, ContactInfo, Contents, Dstring, DA, DV, Fstring, JSString, H, N, NM, Pstring, RC, S, Subj, T, TM, TU, URI, Vstring;

	private byte[][] Border, DmixedArray, Fields, State, rawXFAasArray;
	protected PdfObject Bl, OC, Off, On, P;

	private PdfObject XFAasStream;

	protected Object[] CO, Opt, Reference;

	protected byte[][] Kids;

	private String htmlName = null;

	public void setHTMLName(String name) {
		this.htmlName = name;
	}

	public String getHTMLName() {
		return this.htmlName;
	}

	public FormObject(String ref) {
		super(ref);
		this.objType = PdfDictionary.Form;
	}

	public FormObject(String ref, boolean flag) {
		super(ref);
		this.objType = PdfDictionary.Form;
		this.includeParent = flag;
	}

	public FormObject(int ref, int gen) {
		super(ref, gen);

		this.objType = PdfDictionary.Form;
	}

	public FormObject(int type) {
		super(type);
		this.objType = PdfDictionary.Form;
	}

	public FormObject() {
		super();
		this.objType = PdfDictionary.Form;
	}

	public FormObject(String ref, ActionHandler inFormHandler, int rot) {

		super(ref);
		this.formHandler = inFormHandler;
		this.objType = PdfDictionary.Form;

		this.rawRotation = rot;
	}

	public FormObject(String ref, ActionHandler inFormHandler) {

		super(ref);
		this.formHandler = inFormHandler;
		this.objType = PdfDictionary.Form;
	}

	public FormObject(String ref, int parentType) {
		super(ref);
		this.objType = PdfDictionary.Form;

		this.parentType = parentType;
	}

	public ActionHandler getHandler() {
		return this.formHandler;
	}

	public void setHandler(ActionHandler inFormHandler) {
		this.formHandler = inFormHandler;
	}

	@Override
	public boolean getBoolean(int id) {

		switch (id) {

			case PdfDictionary.H:
				return this.H_Boolean;

			case PdfDictionary.NeedAppearances:
				return this.NeedAppearances;

			case PdfDictionary.Open:
				return this.Open;

			default:
				return super.getBoolean(id);
		}
	}

	@Override
	public void setBoolean(int id, boolean value) {

		switch (id) {

			case PdfDictionary.H:
				this.H_Boolean = value;
				break;

			case PdfDictionary.NeedAppearances:
				this.NeedAppearances = value;
				break;

			case PdfDictionary.Open:
				this.Open = value;
				break;

			default:
				super.setBoolean(id, value);
		}
	}

	/**
	 * used internally to set status while parsing - should not be called
	 * 
	 * @param popup
	 */
	public void setActionFlag(int popup) {
		this.popupFlag = popup;
	}

	/**
	 * get status found during decode
	 */
	public int getActionFlag() {
		return this.popupFlag;
	}

	/**
	 * public void setFloatNumber(int id,float value){
	 * 
	 * switch(id){
	 * 
	 * case PdfDictionary.R: R=value; break;
	 * 
	 * default:
	 * 
	 * super.setFloatNumber(id,value); } }
	 * 
	 * public float getFloatNumber(int id){
	 * 
	 * switch(id){
	 * 
	 * case PdfDictionary.R: return R;
	 * 
	 * default:
	 * 
	 * return super.getFloatNumber(id); } } /
	 **/

	@Override
	public PdfObject getDictionary(int id) {

		switch (id) {

			case PdfDictionary.A:
				return this.A;

			case PdfDictionary.AA:
				return this.AA;

			case PdfDictionary.AP:

				if (this.AP == null) this.AP = new FormObject();
				return this.AP;

			case PdfDictionary.BI:
				return this.BI;

			case PdfDictionary.Bl:
				return this.Bl;

			case PdfDictionary.BS:
				if (this.BS == null) {
					if (this.parentPdfObj != null) {
						PdfObject BSdic = this.parentPdfObj.getDictionary(PdfDictionary.BS);
						if (BSdic != null) {
							return (PdfObject) BSdic.clone();
						}
					}
					this.BS = new FormObject();
				}
				return this.BS;

			case PdfDictionary.C:
				return this.Cdict;

				// case PdfDictionary.C2:
				// return C2;

			case PdfDictionary.D:
				return this.D;

			case PdfDictionary.DC:
				return this.DC;

			case PdfDictionary.DP:
				return this.DP;

			case PdfDictionary.DR:
				return this.DR;

			case PdfDictionary.DS:
				return this.DS;

			case PdfDictionary.E:
				return this.E;

			case PdfDictionary.F:
				return this.Fdict;

			case PdfDictionary.Fo:
				return this.Fo;

			case PdfDictionary.FS:
				return this.FS;

			case PdfDictionary.JS:
				return this.JS;

				// case PdfDictionary.I:
				// return I;

			case PdfDictionary.IF:
				return this.IF;

			case PdfDictionary.K:
				return this.K;

			case PdfDictionary.MK: // can't return null

				if (this.MK == null) {
					if (this.parentPdfObj != null) {
						PdfObject MKdic = this.parentPdfObj.getDictionary(PdfDictionary.MK);
						if (MKdic != null) {
							return (PdfObject) MKdic.clone();
						}
					}
					this.MK = new MKObject();
				}
				return this.MK;

			case PdfDictionary.N:
				return this.Nobj;

			case PdfDictionary.Next:
				return this.Next;

			case PdfDictionary.O:
				return this.O;

			case PdfDictionary.OC:
				return this.OC;

			case PdfDictionary.Off:

				// System.out.println("Off "+this.getObjectRefAsString()+" "+Off);
				// if(Off==null){
				// System.out.println(otherValues);
				// return (PdfObject) otherValues.get("Off");
				// }else
				return this.Off;

			case PdfDictionary.On:
				// System.out.println("On "+this.getObjectRefAsString()+" "+On);
				// if(On==null){
				// System.out.println(otherValues);
				// return (PdfObject) otherValues.get("On");
				// }else
				return this.On;

			case PdfDictionary.P:
				return this.P;

			case PdfDictionary.PC:
				return this.PC;

			case PdfDictionary.PI:
				return this.PI;

			case PdfDictionary.PO:
				return this.PO;

			case PdfDictionary.Popup:
				return this.Popup;

			case PdfDictionary.PV:
				return this.PV;

			case PdfDictionary.R:
				return this.R;

			case PdfDictionary.RichMediaContent:
				return this.RichMediaContent;

			case PdfDictionary.Sig:
				return this.Sig;

			case PdfDictionary.Sound:
				return this.Sound;

			case PdfDictionary.U:
				return this.U;

			case PdfDictionary.V:
				return this.V;

			case PdfDictionary.Win:
				return this.Win;

			case PdfDictionary.WP:
				return this.WP;

			case PdfDictionary.WS:
				return this.WS;

			case PdfDictionary.X:
				return this.X;

			case PdfDictionary.XFA:
				return this.XFAasStream;

			default:
				return super.getDictionary(id);
		}
	}

	@Override
	public void setIntNumber(int id, int value) {

		switch (id) {

			case PdfDictionary.F:
				this.F = value;
				break;

			case PdfDictionary.Ff:
				this.Ff = value;
				commandFf(this.Ff);
				break;

			case PdfDictionary.Q: // correct alignment converted to Java value

				switch (value) {

					case 0:
						this.Q = SwingConstants.LEFT;
						break;

					case 1:
						this.Q = SwingConstants.CENTER;
						break;

					case 2:
						this.Q = SwingConstants.RIGHT;
						break;

					default:
						this.Q = SwingConstants.LEFT;
						break;
				}

				break;

			case PdfDictionary.MaxLen:
				this.MaxLen = value;
				break;

			case PdfDictionary.Rotate:// store in MK so works for Annot
				if (this.MK == null) this.MK = new MKObject();

				// factor in page rotation
				if (this.rawRotation == 0) this.MK.setIntNumber(PdfDictionary.R, value);
				else {

					int diff = this.rawRotation - value;
					if (diff < 0) diff = 360 + diff;

					// if(diff!=0)
					this.MK.setIntNumber(PdfDictionary.R, diff);

				}

				break;

			case PdfDictionary.SigFlags:
				this.SigFlags = value;
				break;

			case PdfDictionary.StructParent:
				this.StructParent = value;
				break;

			case PdfDictionary.TI:
				this.TI = value;
				break;

			case PdfDictionary.W:
				this.W = value;
				break;

			default:
				super.setIntNumber(id, value);
		}
	}

	@Override
	public int getInt(int id) {

		switch (id) {

			case PdfDictionary.F:
				return this.F;

			case PdfDictionary.Ff:
				return this.Ff;

			case PdfDictionary.MaxLen:
				return this.MaxLen;

			case PdfDictionary.Q:
				return this.Q;

			case PdfDictionary.SigFlags:
				return this.SigFlags;

			case PdfDictionary.StructParent:
				return this.StructParent;

			case PdfDictionary.TI:
				return this.TI;

			case PdfDictionary.W:
				return this.W;

			default:
				return super.getInt(id);
		}
	}

	@Override
	public void setDictionary(int id, PdfObject value) {

		value.setID(id);

		// if in AP array as other value store here
		if (this.currentKey != null) {

			// System.out.println("Other values---- "+id+" "+value+" "+objType);
			setOtherValues(id, value);
			return;
		}

		switch (id) {

			case PdfDictionary.A:
				this.A = value;
				break;

			case PdfDictionary.AA:
				this.AA = value;
				break;

			case PdfDictionary.AP:
				this.AP = value;

				// copy across
				if (this.MK == null && this.AP != null && this.AP.getDictionary(PdfDictionary.N) != null) this.MK = this.AP.getDictionary(
						PdfDictionary.N).getDictionary(PdfDictionary.MK);

				break;

			case PdfDictionary.BI:
				this.BI = value;
				break;

			case PdfDictionary.Bl:
				this.Bl = value;
				break;

			case PdfDictionary.BS:
				this.BS = value;
				break;

			case PdfDictionary.C:
				this.Cdict = value;
				break;

			// case PdfDictionary.C2:
			// C2=value;
			// break;

			case PdfDictionary.D:
				this.D = value;
				break;

			case PdfDictionary.DC:
				this.DC = value;
				break;

			case PdfDictionary.DP:
				this.DP = value;
				break;

			case PdfDictionary.DR:
				this.DR = value;
				break;

			case PdfDictionary.DS:
				this.DS = value;
				break;

			case PdfDictionary.E:
				this.E = value;
				break;

			case PdfDictionary.F:
				this.Fdict = value;
				break;

			case PdfDictionary.Fo:
				this.Fo = value;
				break;

			case PdfDictionary.FS:
				this.FS = value;
				break;

			case PdfDictionary.IF:
				this.IF = value;
				break;

			case PdfDictionary.JS:
				this.JS = value;
				break;

			case PdfDictionary.K:
				this.K = value;
				break;

			// case PdfDictionary.I:
			// I=value;
			// break;

			case PdfDictionary.MK:
				this.MK = value;
				break;

			case PdfDictionary.N:
				this.Nobj = value;
				break;

			case PdfDictionary.Next:
				this.Next = value;
				break;

			case PdfDictionary.O:
				this.O = value;
				break;

			case PdfDictionary.OC:
				this.OC = value;
				break;

			case PdfDictionary.Off:
				this.Off = value;
				break;

			case PdfDictionary.On:
				this.On = value;
				break;

			case PdfDictionary.P:
				this.P = value;
				break;

			case PdfDictionary.PC:
				this.PC = value;
				break;

			case PdfDictionary.PI:
				this.PI = value;
				break;

			case PdfDictionary.PO:
				this.PO = value;
				break;

			case PdfDictionary.Popup:
				this.Popup = value;
				break;

			case PdfDictionary.PV:
				this.PV = value;
				break;

			case PdfDictionary.R:
				this.R = value;
				break;

			case PdfDictionary.RichMediaContent:
				this.RichMediaContent = value;
				break;

			case PdfDictionary.Sig:
				this.Sig = value;
				break;

			case PdfDictionary.Sound:
				this.Sound = value;
				break;

			case PdfDictionary.U:
				this.U = value;
				break;

			case PdfDictionary.V:
				this.V = value;
				break;

			case PdfDictionary.Win:
				this.Win = value;
				break;

			case PdfDictionary.WP:
				this.WP = value;
				break;

			case PdfDictionary.WS:
				this.WS = value;
				break;

			case PdfDictionary.X:
				this.X = value;
				break;

			case PdfDictionary.XFA:
				this.XFAasStream = value;
				break;

			default:
				super.setDictionary(id, value);
		}
	}

	@Override
	public int setConstant(int pdfKeyType, int keyStart, int keyLength, byte[] raw) {

		int PDFvalue = PdfDictionary.Unknown;

		int id = 0, x = 0, next;

		try {

			// convert token to unique key which we can lookup

			for (int i2 = keyLength - 1; i2 > -1; i2--) {

				next = raw[keyStart + i2];

				// System.out.println((char)next);
				next = next - 48;

				id = id + ((next) << x);

				x = x + 8;
			}

			/**
			 * not standard
			 */
			switch (id) {

			// case StandardFonts.CIDTYPE0:
			// PDFvalue =StandardFonts.CIDTYPE0;
			// break;

				default:

					// if(pdfKeyType==PdfDictionary.Encoding){
					// PDFvalue=PdfCIDEncodings.getConstant(id);
					//
					// if(PDFvalue==PdfDictionary.Unknown){
					//
					// byte[] bytes=new byte[keyLength];
					//
					// System.arraycopy(raw,keyStart,bytes,0,keyLength);
					//
					// unknownValue=new String(bytes);
					// }
					//
					// if(debug && PDFvalue==PdfDictionary.Unknown){
					// System.out.println("Value not in PdfCIDEncodings");
					//
					// byte[] bytes=new byte[keyLength];
					//
					// System.arraycopy(raw,keyStart,bytes,0,keyLength);
					// System.out.println("Add to CIDEncodings and as String");
					// System.out.println("key="+new String(bytes)+" "+id+" not implemented in setConstant in PdfFont Object");
					//
					// System.out.println("final public static int CMAP_"+new String(bytes)+"="+id+";");
					//
					// }
					// }else
					PDFvalue = super.setConstant(pdfKeyType, id);

					if (PDFvalue == -1) {

						if (debug) {

							byte[] bytes = new byte[keyLength];

							System.arraycopy(raw, keyStart, bytes, 0, keyLength);
							System.out.println("key=" + new String(bytes) + ' ' + id + " not implemented in setConstant in " + this);

							System.out.println("final public static int " + new String(bytes) + '=' + id + ';');

						}

					}

					break;

			}

		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}

		// System.out.println(pdfKeyType+"="+PDFvalue);
		switch (pdfKeyType) {

			default:
				super.setConstant(pdfKeyType, id);

		}

		return PDFvalue;
	}

	// return as constnt we can check
	@Override
	public int getNameAsConstant(int id) {

		byte[] raw;

		switch (id) {

			case PdfDictionary.FT:
				return this.FT;

			case PdfDictionary.H:
				raw = this.rawH;
				break;

			case PdfDictionary.N:
				raw = this.rawN;
				break;

			case PdfDictionary.S:
				raw = this.rawS;
				break;

			case PdfDictionary.X:
				raw = this.rawX;
				break;

			default:
				return super.getNameAsConstant(id);

		}

		if (raw == null) return super.getNameAsConstant(id);
		else return PdfDictionary.generateChecksum(0, raw.length, raw);
	}

	@Override
	public int getParameterConstant(int key) {

		// System.out.println("Get constant for "+key +" "+this);
		switch (key) {

			case PdfDictionary.Subtype:
				if (this.FT != PdfDictionary.Unknown) return this.FT;
				else return super.getParameterConstant(key);
				//
				// //special cases first
				// if(key==PdfDictionary.BaseEncoding && Encoding!=null && Encoding.isZapfDingbats)
				// return StandardFonts.ZAPF;
				// else if(key==PdfDictionary.BaseEncoding && Encoding!=null && Encoding.isSymbol)
				// return StandardFonts.SYMBOL;
				// else
				// return BaseEncoding;
			default:
				return super.getParameterConstant(key);

		}
	}

	// public void setStream(){
	//
	// hasStream=true;
	// }

	@Override
	public PdfArrayIterator getMixedArray(int id) {

		switch (id) {

			case PdfDictionary.Border:
				return new PdfArrayIterator(this.Border);

			case PdfDictionary.D:
				return new PdfArrayIterator(this.DmixedArray);

			case PdfDictionary.Dest:
				return new PdfArrayIterator(this.DmixedArray);

			case PdfDictionary.Fields:
				return new PdfArrayIterator(this.Fields);

			case PdfDictionary.State:
				return new PdfArrayIterator(this.State);

			case PdfDictionary.XFA:
				return new PdfArrayIterator(this.rawXFAasArray);

			default:
				return super.getMixedArray(id);
		}
	}

	@Override
	public byte[] getTextStreamValueAsByte(int id) {

		switch (id) {

			case PdfDictionary.Cert:
				return this.rawCert;

			case PdfDictionary.ContactInfo:
				return this.rawContactInfo;

			case PdfDictionary.Contents:
				return this.rawContents;

				/**
				 * case PdfDictionary.AC: return rawAC;
				 * 
				 * case PdfDictionary.CA: return rawCA;
				 * 
				 * case PdfDictionary.RC: return rawRC;
				 */
			default:
				return super.getTextStreamValueAsByte(id);

		}
	}

	@Override
	public double[] getDoubleArray(int id) {

		switch (id) {
			default:
				return super.getDoubleArray(id);
		}
	}

	@Override
	public void setDoubleArray(int id, double[] value) {

		switch (id) {

		// case PdfDictionary.FontMatrix:
		// FontMatrix=value;
		// break;

			default:
				super.setDoubleArray(id, value);
		}
	}

	@Override
	public int[] getIntArray(int id) {

		switch (id) {

			case PdfDictionary.I:
				return deepCopy(this.I);

			case PdfDictionary.ByteRange:
				return deepCopy(this.ByteRange);

			default:
				return super.getIntArray(id);
		}
	}

	@Override
	public void setIntArray(int id, int[] value) {

		switch (id) {

			case PdfDictionary.I:
				this.I = value;
				break;

			case PdfDictionary.ByteRange:
				this.ByteRange = value;
				break;

			default:
				super.setIntArray(id, value);
		}
	}

	@Override
	public void setMixedArray(int id, byte[][] value) {

		switch (id) {

			case PdfDictionary.Border:
				this.Border = value;
				break;

			case PdfDictionary.Dest:
				this.DmixedArray = value;
				break;

			case PdfDictionary.Fields:
				this.Fields = value;
				break;

			case PdfDictionary.State:
				this.State = value;
				break;

			case PdfDictionary.XFA:
				this.rawXFAasArray = value;
				break;

			default:
				super.setMixedArray(id, value);
		}
	}

	@Override
	public float[] getFloatArray(int id) {

		switch (id) {

			case PdfDictionary.C:
				return this.C;

			case PdfDictionary.QuadPoints:
				return this.QuadPoints;

			case PdfDictionary.Rect:
				return this.Rect;

			case PdfDictionary.RD:
				return this.RD;

			default:
				return super.getFloatArray(id);

		}
	}

	@Override
	public void setFloatArray(int id, float[] value) {

		switch (id) {

			case PdfDictionary.C:
				this.C = value;
				break;

			case PdfDictionary.QuadPoints:
				this.QuadPoints = value;
				break;

			case PdfDictionary.RD:
				this.RD = value;
				break;

			case PdfDictionary.Rect:
				this.Rect = value;
				break;

			default:
				super.setFloatArray(id, value);
		}
	}

	@Override
	public void setName(int id, byte[] value) {

		switch (id) {

			case PdfDictionary.AS:
				this.rawAS = value;
				break;

			case PdfDictionary.DV:
				this.rawDV = value;
				break;

			case PdfDictionary.Filter:
				this.rawFilter = value;
				break;

			case PdfDictionary.SubFilter:
				this.rawSubFilter = value;
				break;

			case PdfDictionary.FT:
				// setup first time
				this.FT = PdfDictionary.generateChecksum(0, value.length, value);
				break;

			case PdfDictionary.H:
				this.rawH = value;

				// set H flags
				break;

			case PdfDictionary.N:
				this.rawN = value;
				break;

			case PdfDictionary.S:
				this.rawS = value;
				break;

			case PdfDictionary.X:
				this.rawX = value;
				break;

			default:
				super.setName(id, value);

		}
	}

	@Override
	public void setObjectArray(int id, Object[] objectValues) {

		switch (id) {

			case PdfDictionary.CO:
				this.CO = objectValues;
				break;

			case PdfDictionary.Opt:
				this.Opt = objectValues;
				break;

			case PdfDictionary.Reference:
				this.Reference = objectValues;
				break;

			default:
				super.setObjectArray(id, objectValues);
				break;
		}
	}

	@Override
	public Object[] getObjectArray(int id) {

		switch (id) {

			case PdfDictionary.CO:
				return this.CO;

			case PdfDictionary.Opt:
				return this.Opt;

			case PdfDictionary.Reference:
				return this.Reference;

			default:
				return super.getObjectArray(id);
		}
	}

	@Override
	public byte[][] getStringArray(int id) {

		switch (id) {

		// case PdfDictionary.XFA:
		// return deepCopy(rawXFAasArray);

			default:
				return super.getStringArray(id);
		}
	}

	@Override
	public void setStringArray(int id, byte[][] value) {

		switch (id) {

		// case PdfDictionary.XFA:
		// rawXFAasArray=value;

			default:
				super.setStringArray(id, value);
		}
	}

	@Override
	public void setTextStreamValue(int id, byte[] value) {

		switch (id) {

			case PdfDictionary.Cert:
				this.rawCert = value;
				break;

			case PdfDictionary.ContactInfo:
				this.rawContactInfo = value;
				break;

			case PdfDictionary.Contents:
				this.rawContents = value;
				break;

			case PdfDictionary.D:
				this.rawDstring = value;
				break;

			case PdfDictionary.DA:
				this.rawDA = value;
				break;

			case PdfDictionary.DV:
				this.rawDV = value;
				break;

			case PdfDictionary.EOPROPtype:
				this.rawEOPROPtype = value;
				break;

			case PdfDictionary.F:
				this.rawFstring = value;
				break;

			case PdfDictionary.JS:
				this.rawJS = value;
				break;

			case PdfDictionary.Location:
				this.rawLocation = value;
				break;

			case PdfDictionary.M:
				this.rawM = value;
				break;

			case PdfDictionary.P:
				this.rawPstring = value;
				break;

			case PdfDictionary.RC:
				this.rawRC = value;
				break;

			case PdfDictionary.Reason:
				this.rawReason = value;
				break;

			case PdfDictionary.NM:
				this.rawNM = value;
				break;

			case PdfDictionary.Subj:
				this.rawSubj = value;
				break;

			case PdfDictionary.T:
				this.rawT = value;
				this.T = null;
				break;

			case PdfDictionary.TM:
				this.rawTM = value;
				break;

			case PdfDictionary.TU:
				this.rawTU = value;
				break;

			case PdfDictionary.URI:
				this.rawURI = value;
				break;

			case PdfDictionary.V:
				this.rawV = value;
				this.Vstring = null; // can be reset
				break;

			default:
				super.setTextStreamValue(id, value);

		}
	}

	@Override
	public void setTextStreamValue(int id, String value) {

		switch (id) {

			case PdfDictionary.V:
				this.Vstring = value; // can be reset
				break;
			case PdfDictionary.T:
				this.setTextStreamValue(id, StringUtils.toBytes(value));
				break;
			default:
				super.setTextStreamValue(id, value);

		}
	}

	@Override
	public String getName(int id) {

		switch (id) {

			case PdfDictionary.AS:

				// setup first time
				if (this.AS == null && this.rawAS != null) this.AS = new String(this.rawAS);

				return this.AS;

			case PdfDictionary.FT:

				// setup first time
				return null;

			case PdfDictionary.H:

				// setup first time
				if (this.H == null && this.rawH != null) this.H = new String(this.rawH);

				return this.H;

			case PdfDictionary.Filter:

				// setup first time
				if (this.Filter == null && this.rawFilter != null) this.Filter = new String(this.rawFilter);

				return this.Filter;

			case PdfDictionary.SubFilter:

				// setup first time
				if (this.SubFilter == null && this.rawSubFilter != null) this.SubFilter = new String(this.rawSubFilter);

				return this.SubFilter;
			case PdfDictionary.N:

				// setup first time
				if (this.N == null && this.rawN != null) this.N = new String(this.rawN);

				return this.N;

			case PdfDictionary.S:

				// setup first time
				if (this.S == null && this.rawS != null) this.S = new String(this.rawS);

				return this.S;

			case PdfDictionary.X:

				// setup first time
				if (this.rawX != null) return new String(this.rawX);

			default:
				return super.getName(id);

		}
	}

	@Override
	public String getTextStreamValue(int id) {

		switch (id) {

			case PdfDictionary.Cert:

				// setup first time
				if (this.Cert == null && this.rawCert != null) this.Cert = StringUtils.getTextString(this.rawCert, false);

				return this.Cert;

			case PdfDictionary.ContactInfo:

				// setup first time
				if (this.ContactInfo == null && this.rawContactInfo != null) this.ContactInfo = StringUtils.getTextString(this.rawContactInfo, false);

				return this.ContactInfo;

			case PdfDictionary.Contents:

				// setup first time
				if (this.Contents == null && this.rawContents != null) this.Contents = StringUtils.getTextString(this.rawContents, true);

				return this.Contents;

			case PdfDictionary.D:

				// setup first time
				if (this.Dstring == null && this.rawDstring != null) this.Dstring = StringUtils.getTextString(this.rawDstring, false);

				return this.Dstring;

			case PdfDictionary.DA:

				// setup first time
				if (this.DA == null && this.rawDA != null) this.DA = StringUtils.getTextString(this.rawDA, false);

				return this.DA;

			case PdfDictionary.DV:

				// setup first time
				if (this.DV == null && this.rawDV != null) this.DV = StringUtils.getTextString(this.rawDV, true);

				return this.DV;

			case PdfDictionary.EOPROPtype:

				// setup first time
				if (this.EOPROPtype == null && this.rawEOPROPtype != null) this.EOPROPtype = new String(this.rawEOPROPtype);

				return this.EOPROPtype;

			case PdfDictionary.F:

				// setup first time
				if (this.Fstring == null && this.rawFstring != null) this.Fstring = StringUtils.getTextString(this.rawFstring, false);

				return this.Fstring;

			case PdfDictionary.JS:

				// setup first time
				if (this.JSString == null && this.rawJS != null) this.JSString = StringUtils.getTextString(this.rawJS, true);

				return this.JSString;

			case PdfDictionary.NM:

				// setup first time
				if (this.NM == null && this.rawNM != null) this.NM = StringUtils.getTextString(this.rawNM, false);

				return this.NM;

			case PdfDictionary.Location:

				// setup first time
				if (this.Location == null && this.rawLocation != null) this.Location = new String(this.rawLocation);
				return this.Location;

			case PdfDictionary.M:

				// setup first time
				if (this.M == null && this.rawM != null) this.M = new String(this.rawM);
				return this.M;

			case PdfDictionary.P:

				// setup first time
				if (this.Pstring == null && this.rawPstring != null) this.Pstring = StringUtils.getTextString(this.rawPstring, false);

				return this.Pstring;

			case PdfDictionary.RC:

				// setup first time
				if (this.RC == null && this.rawRC != null) this.RC = new String(this.rawRC);
				return this.RC;

			case PdfDictionary.Reason:

				// setup first time
				if (this.Reason == null && this.rawReason != null) this.Reason = new String(this.rawReason);
				return this.Reason;

			case PdfDictionary.Subj:
				// setup first time
				if (this.Subj == null && this.rawSubj != null) this.Subj = StringUtils.getTextString(this.rawSubj, false);

				return this.Subj;

			case PdfDictionary.T:
				// setup first time
				if (this.T == null && this.rawT != null) this.T = StringUtils.getTextString(this.rawT, false);

				if (this.T == null && this.parentPdfObj != null) {
					return this.parentPdfObj.getTextStreamValue(PdfDictionary.T);
				}
				return this.T;

			case PdfDictionary.TM:

				// setup first time
				if (this.TM == null && this.rawTM != null) this.TM = StringUtils.getTextString(this.rawTM, false);

				return this.TM;

			case PdfDictionary.TU:

				// setup first time
				if (this.TU == null && this.rawTU != null) this.TU = StringUtils.getTextString(this.rawTU, false);

				return this.TU;

			case PdfDictionary.URI:

				// setup first time
				if (this.URI == null && this.rawURI != null) this.URI = StringUtils.getTextString(this.rawURI, false);

				return this.URI;

			case PdfDictionary.V:

				// setup first time
				if (this.Vstring == null && this.rawV != null) this.Vstring = StringUtils.getTextString(this.rawV, true);

				return this.Vstring;

			default:
				return super.getTextStreamValue(id);

		}
	}

	/**
	 * unless you need special fucntions, use getStringValue(int id) which is faster
	 */
	@Override
	public String getStringValue(int id, int mode) {

		byte[] data = null;

		// get data
		switch (id) {

		// case PdfDictionary.BaseFont:
		// data=rawBaseFont;
		// break;

		}

		// convert
		switch (mode) {
			case PdfDictionary.STANDARD:

				// setup first time
				if (data != null) return new String(data);
				else return null;

			case PdfDictionary.LOWERCASE:

				// setup first time
				if (data != null) return new String(data);
				else return null;

			case PdfDictionary.REMOVEPOSTSCRIPTPREFIX:

				// setup first time
				if (data != null) {
					int len = data.length;
					if (len > 6 && data[6] == '+') { // lose ABCDEF+ if present
						int length = len - 7;
						byte[] newData = new byte[length];
						System.arraycopy(data, 7, newData, 0, length);
						return new String(newData);
					}
					else return new String(data);
				}
				else return null;

			default:
				throw new RuntimeException("Value not defined in getName(int,mode) in " + this);
		}
	}

	public boolean hasKeyArray(int id) {
		switch (id) {
			case PdfDictionary.Kids:
				if (this.Kids != null && this.Kids.length > 0) {
					return true;
				}
				return false;

			default:
				return false;
		}
	}

	@Override
	public byte[][] getKeyArray(int id) {

		switch (id) {

			case PdfDictionary.Kids:
				return deepCopy(this.Kids);

			default:
				return super.getKeyArray(id);
		}
	}

	@Override
	public void setKeyArray(int id, byte[][] value) {

		switch (id) {

			case PdfDictionary.Kids:
				this.Kids = value;
				break;

			default:
				super.setKeyArray(id, value);
		}
	}

	@Override
	public boolean decompressStreamWhenRead() {
		return true;
	}

	/**
	 * resolve what type of field <b>type</b> specifies and return as String
	 */
	public static String resolveType(int type) {

		if (type == PdfDictionary.Btn) return "Button";
		else
			if (type == PdfDictionary.Ch) return "Choice";
			else
				if (type == PdfDictionary.Tx) return "Text";
				else
					if (type == PdfDictionary.Popup) return "Popup";
					else
						if (type == PdfDictionary.Square) return "Square";
						else
							if (type == PdfDictionary.Text) return "Text Annot";

		return null;
	}

	/**
	 * read and setup the form flags for the Ff entry <b>field</b> is the data to be used to setup the Ff flags
	 */
	protected void commandFf(int flagValue) {
		/**
		 * use formObject.flags to get flags representing field preferences the following are accessed by array address (bit position -1)
		 * 
		 * <b>bit positions</b> all 1=readonly - if set there is no interaction 2=required - if set the field must have a value when
		 * submit-form-action occures 3=noexport - if set the field must not be exported by a submit-form-action
		 * 
		 * Choice fields 18=combo - set its a combobox, else a list box 19=edit - defines a comboBox to be editable 20=sort - defines list to be
		 * sorted alphabetically 22=multiselect - if set more than one items can be selected, else only one 23=donotspellcheck - (only used on
		 * editable combobox) don't spell check 27=commitOnselchange - if set commit the action when selection changed, else commit when user exits
		 * field
		 * 
		 * text fields 13=multiline - uses multipul lines else uses a single line 14=password - a password is intended 21=fileselect -text in field
		 * represents a file pathname to be submitted 23=donotspellcheck - don't spell check 24=donotscroll - once the field is full don't enter
		 * anymore text. 25=comb - (only if maxlen is present, (multiline, password and fileselect are CLEAR)), the text is justified across the field
		 * to MaxLen 26=richtext - use richtext format specified by RV entry in field dictionary
		 * 
		 * button fields 15=notoggletooff - (use in radiobuttons only) if set one button must always be selected 16=radio - if set is a set of radio
		 * buttons 17=pushbutton - if set its a push button if neither 16 nor 17 its a check box 26=radiosinunison - if set all radio buttons with the
		 * same on state are turned on and off in unison (same behaviour as html browsers)
		 */

		// System.out.println("flag value="+flag);

		this.flags = new boolean[32];
		/**/
		this.flags[READONLY_ID] = (flagValue & READONLY_BIT) == READONLY_BIT;
		this.flags[REQUIRED_ID] = (flagValue & REQUIRED_BIT) == REQUIRED_BIT;
		this.flags[NOEXPORT_ID] = (flagValue & NOEXPORT_BIT) == NOEXPORT_BIT;
		this.flags[MULTILINE_ID] = (flagValue & MULTILINE_BIT) == MULTILINE_BIT;
		this.flags[PASSWORD_ID] = (flagValue & PASSWORD_BIT) == PASSWORD_BIT;
		this.flags[NOTOGGLETOOFF_ID] = (flagValue & NOTOGGLETOOFF_BIT) == NOTOGGLETOOFF_BIT;
		this.flags[RADIO_ID] = (flagValue & RADIO_BIT) == RADIO_BIT;
		this.flags[PUSHBUTTON_ID] = (flagValue & PUSHBUTTON_BIT) == PUSHBUTTON_BIT;
		this.flags[COMBO_ID] = (flagValue & COMBO_BIT) == COMBO_BIT;
		this.flags[EDIT_ID] = (flagValue & EDIT_BIT) == EDIT_BIT;
		this.flags[SORT_ID] = (flagValue & SORT_BIT) == SORT_BIT;
		this.flags[FILESELECT_ID] = (flagValue & FILESELECT_BIT) == FILESELECT_BIT;
		this.flags[MULTISELECT_ID] = (flagValue & MULTISELECT_BIT) == MULTISELECT_BIT;
		this.flags[DONOTSPELLCHECK_ID] = (flagValue & DONOTSPELLCHECK_BIT) == DONOTSPELLCHECK_BIT;
		this.flags[DONOTSCROLL_ID] = (flagValue & DONOTSCROLL_BIT) == DONOTSCROLL_BIT;
		this.flags[COMB_ID] = (flagValue & COMB_BIT) == COMB_BIT;
		this.flags[RICHTEXT_ID] = (flagValue & RICHTEXT_BIT) == RICHTEXT_BIT;// same as RADIOINUNISON
		this.flags[RADIOINUNISON_ID] = (flagValue & RADIOINUNISON_BIT) == RADIOINUNISON_BIT;// same as RICHTEXT
		this.flags[COMMITONSELCHANGE_ID] = (flagValue & COMMITONSELCHANGE_BIT) == COMMITONSELCHANGE_BIT;
	}

	/**
	 * takes a value, and turns it into the color it represents e.g. (0.5) represents gray (127,127,127) grey = array length 1, with one value rgb =
	 * array length 3, in the order of red,green,blue cmyk = array length 4, in the reverse order, (ie. k, y, m, c)
	 */
	public static Color generateColor(float[] toks) {
		// 0=transparant
		// 1=gray
		// 3=rgb
		// 4=cmyk

		int i = -1;
		if (toks != null) i = toks.length;

		Color newColor = null;
		if (i == 0) {
			// LogWriter.writeFormLog("{stream} CHECK transparent color",debugUnimplemented);
			newColor = new Color(0, 0, 0, 0);// if num of tokens is 0 transparant, fourth variable my need to be 1

		}
		else
			if (i == 1) {

				float tok0 = toks[0];

				if (tok0 <= 1) {
					newColor = new Color(tok0, tok0, tok0);
				}
				else {
					newColor = new Color((int) tok0, (int) tok0, (int) tok0);
				}

			}
			else
				if (i == 3) {
					if (debug) System.out.println("rgb color=" + toks[0] + ' ' + toks[1] + ' ' + toks[2]);

					float tok0 = toks[0];
					float tok1 = toks[1];
					float tok2 = toks[2];

					if (tok0 <= 1 && tok1 <= 1 && tok2 <= 1) {
						newColor = new Color(tok0, tok1, tok2);
					}
					else {
						newColor = new Color((int) tok0, (int) tok1, (int) tok2);
					}

				}
				else
					if (i == 4) {

						DeviceCMYKColorSpace cs = new DeviceCMYKColorSpace();
						cs.setColor(new float[] { toks[3], toks[2], toks[1], toks[0] }, 4);
						newColor = (Color) cs.getColor();

					}

		return newColor;
	}

	/**
	 * @return true if this formObject represents an XFAObject
	 */
	public boolean isXFAObject() {
		return this.isXFAObject;
	}

	@Override
	public PdfObject duplicate() {
		FormObject newObject = new FormObject();

		newObject.formHandler = this.formHandler;

		// String
		newObject.AS = this.AS;
		newObject.currentState = this.currentState;
		newObject.contents = this.contents;
		newObject.Cert = this.Cert;
		newObject.ContactInfo = this.ContactInfo;
		newObject.contents = this.contents;
		newObject.Contents = this.Contents;
		newObject.currentState = this.currentState;
		newObject.DA = this.DA;
		newObject.Dstring = this.Dstring;
		newObject.DV = this.DV;
		newObject.Filter = this.Filter;
		newObject.Fstring = this.Fstring;
		newObject.H = this.H;
		newObject.JSString = this.JSString;
		newObject.layerName = this.layerName;
		newObject.Location = this.Location;
		newObject.M = this.M;
		newObject.N = this.N;
		newObject.NM = this.NM;
		newObject.normalOnState = this.normalOnState;
		// parentRef DO NOT SET
		newObject.Pstring = this.Pstring;
		newObject.onState = this.onState;
		newObject.ref = this.ref;
		newObject.RC = this.RC;
		newObject.Reason = this.Reason;
		newObject.S = this.S;
		newObject.selectedItem = this.selectedItem;
		newObject.SubFilter = this.SubFilter;
		newObject.Subj = this.Subj;
		newObject.T = this.T;
		newObject.TM = this.TM;
		newObject.TU = this.TU;
		newObject.textString = this.textString;
		newObject.URI = this.URI;
		newObject.Vstring = this.Vstring;

		// int
		newObject.display = this.display;
		newObject.F = this.F;
		newObject.Ff = this.Ff;
		newObject.formType = this.formType;
		newObject.FT = this.FT;
		newObject.MaxLen = this.MaxLen;
		newObject.pageNumber = this.pageNumber;
		newObject.popupFlag = this.popupFlag;
		newObject.Q = this.Q;
		newObject.rawRotation = this.rawRotation;
		newObject.SigFlags = this.SigFlags;
		newObject.StructParent = this.StructParent;
		newObject.textSize = this.textSize;
		newObject.TI = this.TI;
		newObject.W = this.W;

		// boolean
		newObject.appearancesUsed = this.appearancesUsed;
		newObject.offsetDownIcon = this.offsetDownIcon;
		newObject.noDownIcon = this.noDownIcon;
		newObject.invertDownIcon = this.invertDownIcon;
		newObject.show = this.show;
		newObject.H_Boolean = this.H_Boolean;
		newObject.NeedAppearances = this.NeedAppearances;
		newObject.isKid = this.isKid;
		newObject.isXFAObject = this.isXFAObject;
		newObject.Open = this.Open;
		newObject.popupBuilt = this.popupBuilt;

		// Font
		newObject.textFont = this.textFont;

		// Color
		newObject.cColor = this.cColor;

		// Object
		if (this.popupObj == null) {
			newObject.popupObj = null;
		}
		else {
			// not sure how to clone a JComponent or ULCComponent
			newObject.popupObj = this.popupObj;
		}

		// String[]
		newObject.OptString = (this.OptString == null) ? null : this.OptString.clone();

		// boolean[]
		newObject.flags = this.flags == null ? null : this.flags.clone();
		newObject.Farray = this.Farray == null ? null : this.Farray.clone();

		// int[]
		newObject.I = this.I == null ? null : this.I.clone();

		// float[]
		newObject.C = this.C == null ? null : this.C.clone();
		newObject.QuadPoints = this.QuadPoints == null ? null : this.QuadPoints.clone();
		newObject.Rect = this.Rect == null ? null : this.Rect.clone();
		newObject.RD = this.RD == null ? null : this.RD.clone();
		newObject.textColor = this.textColor == null ? null : this.textColor.clone();

		// PdfObject
		newObject.A = this.A.duplicate();
		newObject.AA = this.AA.duplicate();
		newObject.AP = this.AP.duplicate();
		newObject.BS = this.BS.duplicate();
		newObject.BI = this.BI.duplicate();
		newObject.Bl = this.Bl.duplicate();
		newObject.Cdict = this.Cdict.duplicate();
		newObject.D = this.D.duplicate();
		newObject.DC = this.DC.duplicate();
		newObject.DP = this.DP.duplicate();
		newObject.DS = this.DS.duplicate();
		newObject.E = this.E.duplicate();
		newObject.Fdict = this.Fdict.duplicate();
		newObject.Fo = this.Fo.duplicate();
		newObject.FS = this.FS.duplicate();
		newObject.IF = this.IF.duplicate();
		newObject.JS = this.JS.duplicate();
		newObject.K = this.K.duplicate();
		newObject.MK = this.MK.duplicate();
		newObject.Next = this.Next.duplicate();
		newObject.Nobj = this.Nobj.duplicate();
		newObject.O = this.O.duplicate();
		newObject.OC = this.OC.duplicate();
		newObject.Off = this.Off.duplicate();
		newObject.On = this.On.duplicate();
		newObject.P = this.P.duplicate();
		// parentPdfObject Do NOT SET OR WE RECURSE INFINATLY
		newObject.PC = this.PC.duplicate();
		newObject.PI = this.PI.duplicate();
		newObject.PO = this.PO.duplicate();
		newObject.Popup = this.Popup.duplicate();
		newObject.PV = this.PV.duplicate();
		newObject.R = this.R.duplicate();
		newObject.Sig = this.Sig.duplicate();
		newObject.Sound = this.Sound.duplicate();
		newObject.U = this.U.duplicate();
		newObject.V = this.V.duplicate();
		newObject.Win = this.Win.duplicate();
		newObject.WP = this.WP.duplicate();
		newObject.WS = this.WS.duplicate();
		newObject.X = this.X.duplicate();
		newObject.XFAasStream = this.XFAasStream.duplicate();

		// Object[]
		newObject.CO = this.CO == null ? null : this.CO.clone();
		newObject.Opt = this.Opt == null ? null : this.Opt.clone();
		newObject.Reference = this.Reference == null ? null : this.Reference.clone();

		// byte[]
		newObject.rawAS = this.rawAS == null ? null : this.rawAS.clone();
		newObject.rawCert = this.rawCert == null ? null : this.rawCert.clone();
		newObject.rawContactInfo = this.rawContactInfo == null ? null : this.rawContactInfo.clone();
		newObject.rawContents = this.rawContents == null ? null : this.rawContents.clone();
		newObject.rawDA = this.rawDA == null ? null : this.rawDA.clone();
		newObject.rawDstring = this.rawDstring == null ? null : this.rawDstring.clone();
		newObject.rawDV = this.rawDV == null ? null : this.rawDV.clone();
		newObject.rawEOPROPtype = this.rawEOPROPtype == null ? null : this.rawEOPROPtype.clone();

		newObject.rawFilter = this.rawFilter == null ? null : this.rawFilter.clone();
		newObject.rawFstring = this.rawFstring == null ? null : this.rawFstring.clone();
		newObject.rawH = this.rawH == null ? null : this.rawH.clone();
		newObject.rawJS = this.rawJS == null ? null : this.rawJS.clone();
		newObject.rawLocation = this.rawLocation == null ? null : this.rawLocation.clone();
		newObject.rawM = this.rawM == null ? null : this.rawM.clone();
		newObject.rawN = this.rawN == null ? null : this.rawN.clone();
		newObject.rawNM = this.rawNM == null ? null : this.rawNM.clone();
		newObject.rawPstring = this.rawPstring == null ? null : this.rawPstring.clone();
		newObject.rawRC = this.rawRC == null ? null : this.rawRC.clone();
		newObject.rawReason = this.rawReason == null ? null : this.rawReason.clone();
		newObject.rawS = this.rawS == null ? null : this.rawS.clone();
		newObject.rawSubFilter = this.rawSubFilter == null ? null : this.rawSubFilter.clone();
		newObject.rawSubj = this.rawSubj == null ? null : this.rawSubj.clone();
		newObject.rawT = this.rawT == null ? null : this.rawT.clone();
		newObject.rawTM = this.rawTM == null ? null : this.rawTM.clone();
		newObject.rawTU = this.rawTU == null ? null : this.rawTU.clone();
		newObject.rawURI = this.rawURI == null ? null : this.rawURI.clone();
		newObject.rawV = this.rawV == null ? null : this.rawV.clone();
		newObject.rawX = this.rawX == null ? null : this.rawX.clone();

		// byte[][]
		newObject.Border = (this.Border == null ? null : ObjectCloneFactory.cloneDoubleArray(this.Border));
		newObject.DmixedArray = (this.DmixedArray == null ? null : ObjectCloneFactory.cloneDoubleArray(this.DmixedArray));
		newObject.Fields = (this.Fields == null ? null : ObjectCloneFactory.cloneDoubleArray(this.Fields));
		newObject.rawXFAasArray = this.rawXFAasArray == null ? null : ObjectCloneFactory.cloneDoubleArray(this.rawXFAasArray);
		newObject.State = this.State == null ? null : ObjectCloneFactory.cloneDoubleArray(this.State);

		// BUfferedImage
		newObject.normalOffImage = ObjectCloneFactory.deepCopy(this.normalOffImage);
		newObject.normalOnImage = ObjectCloneFactory.deepCopy(this.normalOnImage);
		newObject.rolloverOffImage = ObjectCloneFactory.deepCopy(this.rolloverOffImage);
		newObject.rolloverOnImage = ObjectCloneFactory.deepCopy(this.rolloverOnImage);
		newObject.downOffImage = ObjectCloneFactory.deepCopy(this.downOffImage);
		newObject.downOnImage = ObjectCloneFactory.deepCopy(this.downOnImage);

		// Map
		newObject.OptValues = ObjectCloneFactory.cloneMap(this.OptValues);

		return newObject;
	}

	/** overwrites all the values on this form with any values from the parent */
	public void copyInheritedValuesFromParent(FormObject parentObj) {
		if (parentObj == null) return;

		if (this.pageNumber == -1 && parentObj.pageNumber != -1) this.pageNumber = parentObj.pageNumber;

		this.formHandler = parentObj.formHandler;

		// byte[]
		if (this.rawAS == null) this.rawAS = parentObj.rawAS;
		if (this.rawDA == null) this.rawDA = parentObj.rawDA;
		if (this.rawDV == null) this.rawDV = parentObj.rawDV;
		if (this.rawJS == null) this.rawJS = parentObj.rawJS;
		if (this.rawNM == null) this.rawNM = parentObj.rawNM;
		if (this.rawTM == null) this.rawTM = parentObj.rawTM;
		if (this.rawTU == null) this.rawTU = parentObj.rawTU;
		if (this.rawV == null) this.rawV = parentObj.rawV;

		// before we copy the fieldName make sure the parent values are valid
		if (parentObj.T == null && parentObj.rawT != null) {
			parentObj.T = StringUtils.getTextString(parentObj.rawT, false);
		}
		// copy fieldname, making sure to keep it fully qualified
		if (parentObj.T != null) {
			if (this.T == null && this.rawT != null) this.T = StringUtils.getTextString(this.rawT, false);

			if (this.T != null) {
				// make sure the parent name has not already been added to the name
				if (!this.T.contains(parentObj.T)) {
					this.T = parentObj.T + '.' + this.T;
					this.rawT = StringUtils.toBytes(this.T);
				}// else we should already have the right name
					// NOTE dont just pass parent T and rawT values through as we read them vis getTextStreamValue(T) if needed
			}
		}

		// PdfObject
		if (this.A == null) this.A = parentObj.A;
		if (this.AA == null) this.AA = parentObj.AA;
		if (this.AP == null) this.AP = parentObj.AP;
		if (this.D == null) this.D = parentObj.D;
		if (this.OC == null) this.OC = parentObj.OC;

		// float[]
		if (this.C == null) this.C = (parentObj.C == null) ? null : parentObj.C.clone();

		if (this.QuadPoints == null) this.QuadPoints = (parentObj.QuadPoints == null) ? null : parentObj.QuadPoints.clone();

		if (this.Rect == null) this.Rect = (parentObj.Rect == null) ? null : parentObj.Rect.clone();

		// int
		if (this.F == -1) this.F = parentObj.F;
		if (this.Ff == -1) this.Ff = parentObj.Ff;
		if (this.Q == -1) this.Q = parentObj.Q;
		if (this.MaxLen == -1) this.MaxLen = parentObj.MaxLen;
		if (this.FT == -1) this.FT = parentObj.FT;
		if (this.TI == -1) this.TI = parentObj.TI;

		// boolean[]
		if (this.flags == null) this.flags = (parentObj.flags == null) ? null : parentObj.flags.clone();

		// Object[]
		if (this.Opt == null) this.Opt = (parentObj.Opt == null) ? null : parentObj.Opt.clone();
		if (this.CO == null) this.CO = (parentObj.CO == null) ? null : parentObj.CO.clone();

		// String
		if (this.textString == null) this.textString = parentObj.textString;
		if (this.OptString == null) this.OptString = parentObj.OptString;
		if (this.selectedItem == null) this.selectedItem = parentObj.selectedItem;
	}

	/**
	 * get actual object reg
	 * 
	 * @deprecated use formObject.getObjectRefAsString();
	 */
	@Deprecated
	public String getPDFRef() {
		return getObjectRefAsString();
	}

	/**
	 * @return the alignment (Q)
	 */
	public int getAlignment() {

		if (this.Q == -1) this.Q = SwingConstants.LEFT;

		return this.Q;
	}

	public boolean hasColorChanged() {
		return this.textColorChanged;
	}

	/** rests the color changed flag to false, to say that it has be refreshed on screen */
	public void resetColorChanged() {
		this.textColorChanged = false;
	}

	/**
	 * sets the text color for this form
	 * 
	 */
	public void setTextColor(float[] color) {
		// JS made public so that javascript can access it

		// check if is javascript and convert to our float
		if (color.length > 0 && Float.isNaN(color[0])) {// not-a-number
			float[] tmp = new float[color.length - 1];
			System.arraycopy(color, 1, tmp, 0, color.length - 1);
			color = tmp;
		}

		this.textColor = color;

		// set flag to say that the text color has chnaged so we can update the forms.
		this.textColorChanged = true;
	}

	/**
	 * set the text font for this form
	 */
	public void setTextFont(Font font) {
		this.textFont = font;
	}

	/**
	 * sets the text size for this form
	 */
	public void setTextSize(int size) {
		this.textSize = size;
	}

	/**
	 * sets the child on state, only applicable to radio buttons
	 */
	public void setChildOnState(String curValue) {
		this.onState = curValue;
	}

	/**
	 * sets the current state, only applicable to check boxes
	 */
	public void setCurrentState(String curValue) {
		this.currentState = curValue;
	}

	/**
	 * sets the text value
	 * 
	 * @deprecated : use updateValue(ComponentData.TEXT_TYPE, text,true);
	 */
	@Deprecated
	public void setTextValue(String text) {

		// use empty string so that the raw pdf value does not get recalled.
		if (text == null) text = "";

		updateValue(ComponentData.TEXT_TYPE, text, true);
	}

	/**
	 * sets the selected item only applicable to the choices fields
	 */
	public void setSelectedItem(String curValue) {

		this.selectedItem = curValue;
	}

	/**
	 * sets the field name for this field (used by XFA)
	 */
	public void setFieldName(String field) {

		this.T = null;

		// ensure we sync to low level value
		this.setTextStreamValue(PdfDictionary.T, StringUtils.toBytes(field));
	}

	/**
	 * sets the parent for this field
	 */
	public void setParent(String parent) {
		setParent(parent, null, false);
	}

	/**
	 * sets the parent string for this field and stores the parent PDFObject passed in to be accessed locally and from getParent() BEWARE :- this
	 * method will copy all relevent values from the parent is copyValuesFromParent is true
	 */
	public void setParent(String parent, FormObject parentObj, boolean copyValuesFromParent) {
		if (copyValuesFromParent) {
			// copy all values from the parent here, then they can be overwritten in future.
			copyInheritedValuesFromParent(parentObj);
		}

		this.parentRef = parent;
		if (parentObj != null) {
			this.parentPdfObj = parentObj;
		}
	}

	public PdfObject getParentPdfObj() {
		return this.parentPdfObj;
	}

	/**
	 * gets the parent for this field
	 */
	public String getParentRef() {

		// option to take from file as well
		if (this.parentRef == null && this.includeParent) return getStringKey(PdfDictionary.Parent);
		else return this.parentRef;
	}

	/**
	 * return the characteristic type
	 */
	private static boolean[] calcFarray(int flagValue) {

		if (flagValue == 0) return new boolean[10];

		boolean[] Farray = new boolean[10];

		final int[] pow = { 0, 1, 2, 4, 8, 16, 32, 64, 128, 256, 512 };
		for (int jj = 1; jj < 10; jj++) {
			if (((flagValue & pow[jj]) == pow[jj])) Farray[jj - 1] = true;
		}

		return Farray;
	}

	/**
	 * sets the top index for the choice fields
	 */
	public void setTopIndex(int[] index) {

		if (index == null) this.TI = -1;
		else
			if (index.length > 0) this.TI = index[0];

		this.I = index;
	}

	/**
	 * return the bounding rectangle for this object
	 */
	public Rectangle getBoundingRectangle() {

		float[] coords = getFloatArray(PdfDictionary.Rect);

		if (coords != null) {
			float x1 = coords[0], y1 = coords[1], x2 = coords[2], y2 = coords[3];

			if (x1 > x2) {
				float tmp = x1;
				x1 = x2;
				x2 = tmp;
			}
			if (y1 > y2) {
				float tmp = y1;
				y1 = y2;
				y2 = tmp;
			}

			int ix1 = (int) x1; // truncates to ensure form area fully contained
			int iy1 = (int) y1;

			int ix2 = (int) x2 + ((x2 - (int) x2 > 0) ? 1 : 0); // Always rounds up to ensure form area fully contained
			int iy2 = (int) y2 + ((y2 - (int) y2 > 0) ? 1 : 0);

			this.BBox = new Rectangle(ix1, iy1, ix2 - ix1, iy2 - iy1);
		}
		else this.BBox = new Rectangle(0, 0, 0, 0);

		return this.BBox;
	}

	/**
	 * sets the type this form specifies
	 */
	public void setType(int type, boolean isXFA) {

		if (isXFA) this.FT = type;
	}

	/**
	 * sets the flag <b>pos</b> to value of <b>flag</b>
	 */
	public void setFlag(int pos, boolean flag) {
		// @chris xfa this needs fixing, all references set wrong flag.
		if (this.flags == null) {
			this.flags = new boolean[32];
		}
		this.flags[pos] = flag;
	}

	/**
	 * 
	 * 1=readonly - if set there is no interaction <br>
	 * 2=required - if set the field must have a value when submit-form-action occures <br>
	 * 3=noexport - if set the field must not be exported by a submit-form-action <br>
	 * <br>
	 * Choice fields <br>
	 * 18=combo - set its a combobox, else a list box <br>
	 * 19=edit - defines a comboBox to be editable <br>
	 * 20=sort - defines list to be sorted alphabetically <br>
	 * 22=multiselect - if set more than one items can be selected, else only one <br>
	 * 23=donotspellcheck - (only used on editable combobox) don't spell check <br>
	 * 27=commitOnselchange - if set commit the action when selection changed, else commit when user exits field <br>
	 * <br>
	 * text fields <br>
	 * 13=multiline - uses multipul lines else uses a single line <br>
	 * 14=password - a password is intended <br>
	 * 21=fileselect -text in field represents a file pathname to be submitted <br>
	 * 23=donotspellcheck - don't spell check <br>
	 * 24=donotscroll - once the field is full don't enter anymore text. <br>
	 * 25=comb - (only if maxlen is present, (multiline, password and fileselect are CLEAR)), the text is justified across the field to MaxLen <br>
	 * 26=richtext - use richtext format specified by RV entry in field dictionary <br>
	 * <br>
	 * button fields <br>
	 * 15=notoggletooff - (use in radiobuttons only) if set one button must always be selected <br>
	 * 16=radio - if set is a set of radio buttons <br>
	 * 17=pushbutton - if set its a push button, if neither 16 nor 17 its a check box <br>
	 * 26=radiosinunison - if set all radio buttons with the same on state are turned on and off in unison (same behaviour as html browsers)
	 * 
	 * @return the flags array (Ff in PDF) (indexs are the number listed) * all <br>
	 */
	public boolean[] getFieldFlags() {// otherwise known as Ff flags
		if (this.flags == null) this.flags = new boolean[32];
		return this.flags;
	}

	/**
	 * set the state which is defined as the On state for this form <br>
	 * usually different for each child so that the selected child can be found by the state
	 */
	public void setNormalOnState(String state) {
		this.normalOnState = state;
	}

	/**
	 * @return whether or not appearances are used in this field
	 */
	public boolean isAppearanceUsed() {
		return this.appearancesUsed;
	}

	/**
	 * sets the image <br>
	 * images are one of R rollover, N normal, D down and Off unselected, On selected <br>
	 * for Normal selected the normal on state should also be set
	 */
	public void setAppearanceImage(BufferedImage image, int imageType, int status) {
		if (image == null)
		// if null use opaque image
		image = getOpaqueImage();

		switch (imageType) {
			case PdfDictionary.D:
				if (status == PdfDictionary.On) {
					this.downOnImage = image;
				}
				else
					if (status == PdfDictionary.Off) {
						this.downOffImage = image;
					}
					else throw new RuntimeException("Unknown status use PdfDictionary.On or PdfDictionary.Off");
				break;

			case PdfDictionary.N:
				if (status == PdfDictionary.On) {
					this.normalOnImage = image;
				}
				else
					if (status == PdfDictionary.Off) {
						this.normalOffImage = image;
					}
					else throw new RuntimeException("Unknown status use PdfDictionary.On or PdfDictionary.Off");
				break;

			case PdfDictionary.R:
				if (status == PdfDictionary.On) {
					this.rolloverOnImage = image;
				}
				else
					if (status == PdfDictionary.Off) {
						this.rolloverOffImage = image;
					}
					else throw new RuntimeException("Unknown status use PdfDictionary.On or PdfDictionary.Off");
				break;

			default:
				throw new RuntimeException("Unknown type use PdfDictionary.D, PdfDictionary.N or PdfDictionary.R");
		}

		this.appearancesUsed = true;
	}

	/**
	 * sets the border color
	 */
	public void setBorderColor(String nextField) {

		if (nextField != null) getDictionary(PdfDictionary.MK).setFloatArray(PdfDictionary.BC, generateFloatFromString(nextField));
	}

	/**
	 * sets the background color for this form
	 */
	public void setBackgroundColor(String nextField) {

		if (nextField != null) getDictionary(PdfDictionary.MK).setFloatArray(PdfDictionary.BG, generateFloatFromString(nextField));
	}

	/**
	 * takes a String <b>colorString</b>, and turns it into the color it represents e.g. (0.5) represents gray (127,127,127) cmyk = 4 tokens in the
	 * order c, m, y, k
	 */
	private static float[] generateFloatFromString(String colorString) {
		// 0=transparant
		// 1=gray
		// 3=rgb
		// 4=cmyk
		if (debug) System.out.println("CHECK generateColorFromString=" + colorString);

		StringTokenizer tokens = new StringTokenizer(colorString, "[()] ,");

		float[] toks = new float[tokens.countTokens()];
		int i = 0;
		while (tokens.hasMoreTokens()) {

			String tok = tokens.nextToken();
			if (debug) System.out.println("token" + (i + 1) + '=' + tok + ' ' + colorString);

			toks[i] = Float.parseFloat(tok);
			i++;
		}

		if (i == 0) return null;
		else return toks;
	}

	/**
	 * sets the normal caption for this form
	 */
	public void setNormalCaption(String caption) {

		if (caption != null) {
			getDictionary(PdfDictionary.MK).setTextStreamValue(PdfDictionary.CA, StringUtils.toBytes(caption));
		}
	}

	/**
	 * sets whether there should be a down looking icon
	 */
	protected void setOffsetDownApp() {
		this.offsetDownIcon = true;
	}

	/**
	 * sets whether a down icon should be used
	 */
	protected void setNoDownIcon() {
		this.noDownIcon = true;
	}

	/**
	 * sets whether to invert the normal icon for the down icon
	 */
	protected void setInvertForDownIcon() {
		this.invertDownIcon = true;
	}

	/*
	 * returns to rotation of this field object, currently in stamp annotations only
	 * 
	 * deprecated use formObject.getDictionary(PdfDictionary.MK).getInt(PdfDictionary.R);
	 * 
	 * public int getRotation(){
	 * 
	 * return getDictionary(PdfDictionary.MK).getInt(PdfDictionary.R); }/
	 */

	/**
	 * @return true if has normal of image
	 */
	public boolean hasNormalOff() {
		return this.normalOffImage != null;
	}

	/**
	 * @return true if has rollover off image
	 */
	public boolean hasRolloverOff() {
		return this.rolloverOffImage != null;
	}

	/**
	 * @return true if has down off image
	 */
	public boolean hasDownOff() {
		return this.downOffImage != null;
	}

	/**
	 * @return true if has one or more down images set
	 */
	public boolean hasDownImage() {
		return (this.downOnImage != null || hasDownOff());
	}

	/**
	 * @return true if has a rollover on image
	 */
	public boolean hasRolloverOn() {
		return this.rolloverOnImage != null;
	}

	/**
	 * @return true if has a normal on image
	 */
	public boolean hasNormalOn() {
		return this.normalOnImage != null;
	}

	/**
	 * copy all values from <b>form</b> to <b>this</b> FormObject WARNING overrites current data, so shoudl be called before the rest of the form is
	 * setup.
	 */
	public void overwriteWith(FormObject form) {
		if (form == null) return;

		if (form.parentRef != null) this.parentRef = form.parentRef;
		if (form.flags != null) this.flags = form.flags.clone();

		if (form.I != null) this.I = form.I.clone();
		if (form.selectedItem != null) this.selectedItem = form.selectedItem;

		if (form.ref != null) this.ref = form.ref;
		if (form.textColor != null) this.textColor = form.textColor.clone();
		if (form.textFont != null) this.textFont = form.textFont;
		if (form.textSize != -1) this.textSize = form.textSize;
		if (form.textString != null) this.textString = form.textString;

		if (form.appearancesUsed) this.appearancesUsed = form.appearancesUsed;
		if (form.offsetDownIcon) this.offsetDownIcon = form.offsetDownIcon;
		if (form.noDownIcon) this.noDownIcon = form.noDownIcon;
		if (form.invertDownIcon) this.invertDownIcon = form.invertDownIcon;

		if (form.onState != null) this.onState = form.onState;
		if (form.currentState != null) this.currentState = form.currentState;
		if (form.normalOffImage != null) this.normalOffImage = form.normalOffImage;
		if (form.normalOnImage != null) this.normalOnImage = form.normalOnImage;
		if (form.rolloverOffImage != null) this.rolloverOffImage = form.rolloverOffImage;
		if (form.rolloverOnImage != null) this.rolloverOnImage = form.rolloverOnImage;
		if (form.downOffImage != null) this.downOffImage = form.downOffImage;
		if (form.downOnImage != null) this.downOnImage = form.downOnImage;
		if (form.pageNumber != -1) this.pageNumber = form.pageNumber;

		// annotations
		if (form.cColor != null) this.cColor = form.cColor;
		if (form.contents != null) this.contents = form.contents;
		if (form.show) this.show = form.show;

		// ?? cloning
		this.AA = form.AA;
		this.AP = form.AP;
		this.BS = form.BS;
		this.D = form.D;
		this.OC = form.OC;

		this.C = (form.C == null) ? null : form.C.clone();

		this.QuadPoints = (form.QuadPoints == null) ? null : form.QuadPoints.clone();

		this.F = form.F;
		this.Ff = form.Ff;
		this.CO = (form.CO == null) ? null : form.CO.clone();
		this.Opt = (form.Opt == null) ? null : form.Opt.clone();
		this.Q = form.Q;
		this.MaxLen = form.MaxLen;
		this.FT = form.FT;
		this.rawAS = (form.rawAS == null) ? null : form.rawAS.clone();
		this.rawDA = (form.rawDA == null) ? null : form.rawDA.clone();
		this.rawDV = (form.rawDV == null) ? null : form.rawDV.clone();
		this.rawJS = (form.rawJS == null) ? null : form.rawJS.clone();
		this.rawNM = (form.rawNM == null) ? null : form.rawNM.clone();
		this.rawTM = (form.rawTM == null) ? null : form.rawTM.clone();
		this.rawTU = (form.rawTU == null) ? null : form.rawTU.clone();
		this.rawV = (form.rawV == null) ? null : form.rawV.clone();

		// copy fieldname
		this.T = form.T;
		this.rawT = (form.rawT == null) ? null : form.rawT.clone();

		this.Rect = (form.Rect == null) ? null : form.Rect.clone();

		this.TI = form.TI;

		this.MK = (form.MK == null) ? null : (PdfObject) form.MK.clone();
	}

	public Object getPopupObj() {
		return this.popupObj;
	}

	/**
	 * See also {@link FormObject#getUserName()}
	 * 
	 * @return the full field name for this form
	 * 
	 * @deprecated use formObject.getTextStreamValue(PdfDictionary.T); NO LONGER USED INTERNALLY
	 */
	// public String getFieldName() {
	//
	// //ensure resolved
	// if(T==null)
	// this.getTextStreamValue(PdfDictionary.T);
	//
	// return T;
	// }

	/**
	 * @return the currently selected State of this field at time of opening.
	 */
	public String getCurrentState() {
		return this.currentState;
	}

	/**
	 * @return the on state for this field
	 */
	public String getOnState() {
		return this.onState;
	}

	/**
	 * @return the characteristics for this field. <br>
	 *         bit 1 is index 0 in [] [0] 1 = invisible [1] 2 = hidden - dont display or print [2] 3 = print - print if set, dont if not [3] 4 =
	 *         nozoom [4] 5= norotate [5] 6= noview [6] 7 = read only (ignored by wiget) [7] 8 = locked [8] 9 = togglenoview as on pdf 1.7 this became
	 *         10 bits long [9] 10 = LockedContents
	 */
	public boolean[] getCharacteristics() {

		// lazy initialisation
		if (this.Farray == null) {

			if (this.F == -1) this.Farray = new boolean[10];
			else this.Farray = calcFarray(this.F);

			// System.out.println("F="+F+" "+this.getPDFRef()+" "+characteristic+" display="+display+" "+characteristic[2]);
		}

		return this.Farray;
	}

	/**
	 * @return userName for this field (TU value)
	 * 
	 *         use formObject.getTextStreamValue(PdfDictionary.TU);
	 */
	// public String getUserName() {
	//
	// //ensure resolved g
	// if(TU==null)
	// getTextStreamValue(PdfDictionary.TU);
	//
	// return TU;
	// }

	/**
	 * @return the default text size for this field
	 */
	public int getTextSize() {
		return this.textSize;
	}

	/**
	 * @return the values map for this field, map that references the display value from the export values
	 */
	public Map getValuesMap(boolean keyFirst) {

		if (this.Opt != null && this.OptValues == null) {

			Object[] rawOpt = getObjectArray(PdfDictionary.Opt);

			if (rawOpt != null) {

				String key, value;
				Object[] obj;

				for (Object aRawOpt : rawOpt) {

					if (aRawOpt instanceof Object[]) { // 2 items (see p678 in v1.6 PDF ref)
						obj = (Object[]) aRawOpt;

						if (keyFirst) {
							key = StringUtils.getTextString((byte[]) obj[0], false);
							value = StringUtils.getTextString((byte[]) obj[1], false);
						}
						else {
							key = StringUtils.getTextString((byte[]) obj[1], false);
							value = StringUtils.getTextString((byte[]) obj[0], false);
						}

						if (this.OptValues == null) this.OptValues = new HashMap();

						this.OptValues.put(key, value);

					}
				}
			}
		}

		return this.OptValues;
	}

	/**
	 * @return the default value for this field
	 * 
	 *         use formObject.getTextStreamValue(PdfDictionary.DV);
	 */
	// public String getDefaultValue() {

	// return getTextStreamValue(PdfDictionary.DV);
	// }

	/**
	 * @return the items array list (Opt)
	 */
	public String[] getItemsList() {

		if (this.OptString == null) {
			Object[] rawOpt = getObjectArray(PdfDictionary.Opt);

			if (rawOpt != null) {
				int count = rawOpt.length;
				this.OptString = new String[count];

				for (int ii = 0; ii < count; ii++) {

					if (rawOpt[ii] instanceof Object[]) { // 2 items (see p678 in v1.6 PDF ref)
						Object[] obj = (Object[]) rawOpt[ii];

						this.OptString[ii] = StringUtils.getTextString((byte[]) obj[1], false);

					}
					else
						if (rawOpt[ii] instanceof byte[]) {
							this.OptString[ii] = StringUtils.getTextString((byte[]) rawOpt[ii], false);

						}
						else
							if (rawOpt[ii] != null) {}
				}
			}
		}

		return this.OptString;
	}

	/**
	 * @return the selected Item for this field
	 */
	public String getSelectedItem() {

		if (this.selectedItem == null) this.selectedItem = getTextStreamValue(PdfDictionary.V);

		// if no value set but selection, use that
		if (this.selectedItem == null && this.I != null) {
			String[] items = this.getItemsList();
			int itemSelected = this.I[0];
			if (items != null && itemSelected > -1 && itemSelected < items.length) return items[itemSelected];
			else return null;
		}
		else return this.selectedItem;
	}

	/**
	 * @return the top index, or item that is visible in the combobox or list first.
	 */
	public int[] getTopIndex() {

		if (this.I == null && this.TI != -1) {
			this.I = new int[1];
			this.I[0] = this.TI;
		}

		return this.I;
	}

	/**
	 * @return the text string for this field - if no value set but a default (DV value) set, return that.
	 */
	public String getTextString() {

		if (this.textString == null) this.textString = getTextStreamValue(PdfDictionary.V);

		if (this.textString == null && getTextStreamValue(PdfDictionary.DV) != null) return getTextStreamValue(PdfDictionary.DV);
		else {
			if (this.textString != null) this.textString = this.textString.replaceAll("\r", "\n").trim();
			else this.textString = "";

			return this.textString;
		}
	}

	/**
	 * @return the maximum length of the text in the field
	 * 
	 *         use formObject.getInt(PdfDictionary.MaxLen)
	 */
	// public int getMaxTextLength() {

	// return MaxLen;
	// }

	/**
	 * @return the normal caption for this button, the caption displayed when nothing is interacting with the icon, and at all other times unless a
	 *         down and/or rollover caption is present
	 * 
	 *         use formObject.getDictionary(PdfDictionary.MK).getTextStreamValue(PdfDictionary.CA);
	 */
	// public String getNormalCaption() {

	// return getDictionary(PdfDictionary.MK).getTextStreamValue(PdfDictionary.CA);
	// }

	/**
	 * @return the down caption, caption displayed when the button is down/pressed
	 * 
	 *         use formObject.getDictionary(PdfDictionary.MK).getTextStreamValue(PdfDictionary.AC);
	 */
	// public String getDownCaption() {

	// return getDictionary(PdfDictionary.MK).getTextStreamValue(PdfDictionary.AC);
	// }

	/**
	 * @return the rollover caption, the caption displayed when the user rolls the mouse cursor over the button
	 * 
	 *         deprecated use formObject.getDictionary(PdfDictionary.MK).getTextStreamValue(PdfDictionary.RC);
	 * 
	 *         public String getRolloverCaption() {
	 * 
	 *         return getDictionary(PdfDictionary.MK).getTextStreamValue(PdfDictionary.RC); }/
	 **/

	/**
	 * @return the position of the view of the text in this field
	 * 
	 *         positioning of text relative to icon - (integer) 0=caption only 1=icon only 2=caption below icon 3=caption above icon 4=caption on
	 *         right of icon 5=caption on left of icon 6=caption overlaid ontop of icon
	 */
	public int getTextPosition() {
		return getDictionary(PdfDictionary.MK).getInt(PdfDictionary.TP);
	}

	/**
	 * @return the default state of this field, the state to return to when the field is reset
	 * 
	 *         use formObject.getName(PdfDictionary.AS);
	 */
	// public String getDefaultState() {
	//
	// if(AS==null)
	// this.getName(PdfDictionary.AS);
	//
	// return AS;
	//
	// }

	/**
	 * @return the normal on state for this field
	 */
	public String getNormalOnState() {
		return this.normalOnState;
	}

	/**
	 * @return the normal off image for this field
	 */
	public BufferedImage getNormalOffImage() {
		return this.normalOffImage;
	}

	/**
	 * @return the normal On image for this field
	 */
	public BufferedImage getNormalOnImage() {
		return this.normalOnImage;
	}

	/**
	 * @return if this field has not got a down icon
	 */
	public boolean hasNoDownIcon() {
		return this.noDownIcon;
	}

	/**
	 * @return whether this field has a down icon as an offset of the normal icon
	 */
	public boolean hasOffsetDownIcon() {
		return this.offsetDownIcon;
	}

	/**
	 * @return whether this field has a down icon as an inverted image of the normal icon
	 */
	public boolean hasInvertDownIcon() {
		return this.invertDownIcon;
	}

	/**
	 * @return the down off image for this field
	 */
	public BufferedImage getDownOffImage() {
		return this.downOffImage;
	}

	/**
	 * @return the down on image for this field
	 */
	public BufferedImage getDownOnImage() {
		return this.downOnImage;
	}

	/**
	 * @return the rollover image for this field, the image displayed when the user moves the mouse over the field
	 */
	public BufferedImage getRolloverOffImage() {
		return this.rolloverOffImage;
	}

	/**
	 * @return the rollover on image, the image displayed when the user moves the mouse over the field, when in the on state
	 */
	public BufferedImage getRolloverOnImage() {
		return this.rolloverOnImage;
	}

	/**
	 * @return the text font for this field
	 */
	public Font getTextFont() {
		if (this.textFont == null) {
			this.textFont = new Font("Arial", Font.PLAIN, 8);
		}
		return this.textFont;
	}

	/**
	 * @return the text color for this field
	 */
	public Color getTextColor() {
		return generateColor(this.textColor);
	}

	/**
	 * @return the border color for this field
	 * 
	 * @deprecated use FormObject.generateColor(formObject.getDictionary(PdfDictionary.MK).getFloatArray(PdfDictionary.BC));
	 */
	@Deprecated
	public Color getBorderColor() {
		return generateColor(getDictionary(PdfDictionary.MK).getFloatArray(PdfDictionary.BC));
	}

	/**
	 * @return the border style for this field
	 * 
	 *         deprecated use formObject.getDictionary(pdfDictionary.BS);
	 * 
	 *         public PdfObject getBorder() { return BS; }/
	 **/

	/**
	 * @return the background color for this field deprecated - use
	 *         FormObject.generateColor(formObj.getDictionary(PdfDictionary.MK).getFloatArray(PdfDictionary.BG));
	 * 
	 *         public Color getBackgroundColor() {
	 * 
	 *         return generateColor(getDictionary(PdfDictionary.MK).getFloatArray(PdfDictionary.BG)); } /
	 **/

	/**
	 * return true if the popup component has been built
	 */
	public boolean isPopupBuilt() {
		return this.popupBuilt;
	}

	/**
	 * store the built popup component for use next time and set popupBuilt to true.
	 */
	public void setPopupBuilt(Object popup) {
		if (popup == null) return;

		this.popupObj = popup;
		this.popupBuilt = true;
	}

	public String getLayerName() {

		// lazy initialisation
		if (this.layerName == null) {
			PdfObject OC = this.getDictionary(PdfDictionary.OC);

			if (OC != null) this.layerName = OC.getName(PdfDictionary.Name);
		}

		return this.layerName;
	}

	/** JS stores if any of the form values have changed acessed via hasFormChanged() */
	private boolean formChanged = false;

	/** JS has the form fields changed */
	public boolean hasValueChanged() {
		return this.formChanged;
	}

	/** flags up this forms value as being changed so that it will be updated to the view */
	public void setFormChanged() {
		this.formChanged = true;
	}

	/** JS resets the form changed flag to indicate the values have been updated */
	public void resetFormChanged() {
		this.formChanged = false;
	}

	/**
	 * @return the current value for this field, if text field the text string, if choice field the selected item if button field the normal
	 *        caption
	 */
	public String getValue() {

		int subtype = getParameterConstant(PdfDictionary.Subtype);

		switch (subtype) {
			case PdfDictionary.Tx:
				return getTextString();

			case PdfDictionary.Ch:

				if (this.selectedItem == null) this.selectedItem = getTextStreamValue(PdfDictionary.V);

				return this.selectedItem;

			case PdfDictionary.Btn:

				return getDictionary(PdfDictionary.MK).getTextStreamValue(PdfDictionary.CA);

			case PdfDictionary.Sig:

				return getDictionary(PdfDictionary.MK).getTextStreamValue(PdfDictionary.CA);

			default:// to catch all the annots

				return getDictionary(PdfDictionary.MK).getTextStreamValue(PdfDictionary.CA);
		}
		// NOTE - Do not return empty string if value is null, as affects 0.00 values.
	}

	/**
	 * sets the value of this field dependent on which type of field it is
	 */
	public void setValue(String inVal) {// need to kept as java strings
		boolean preFormChanged = this.formChanged;
		String CA;

		int subtype = getParameterConstant(PdfDictionary.Subtype);

		// if(isReadOnly()){
		// return;
		// }

		switch (subtype) {
			case PdfDictionary.Tx:

				String curVal = getTextStreamValue(PdfDictionary.V);
				// if the current value is the same as the new value, our job is done.
				if (curVal != null && curVal.equals(inVal)) break;

				if (this.textString != null && this.textString.equals(inVal)) {
					break;
				}

				// use empty string so that the raw pdf value does not get recalled.
				if (inVal == null) inVal = "";
				this.textString = inVal;

				this.formChanged = true;
				break;

			case PdfDictionary.Ch:

				if (this.selectedItem == null) this.selectedItem = getTextStreamValue(PdfDictionary.V);

				if (this.selectedItem != null && this.selectedItem.equals(inVal)) {
					break;
				}
				this.selectedItem = inVal;
				this.formChanged = true;
				break;

			case PdfDictionary.Btn:

				CA = getDictionary(PdfDictionary.MK).getTextStreamValue(PdfDictionary.CA);
				if (CA != null && CA.equals(inVal)) {
					break;
				}
				getDictionary(PdfDictionary.MK).setTextStreamValue(PdfDictionary.CA, StringUtils.toBytes(inVal));

				this.formChanged = true;
				break;

			default:

				CA = getDictionary(PdfDictionary.MK).getTextStreamValue(PdfDictionary.CA);
				if (CA != null && CA.equals(inVal)) {
					break;
				}

				getDictionary(PdfDictionary.MK).setTextStreamValue(PdfDictionary.CA, StringUtils.toBytes(inVal));

				this.formChanged = true;

		}

		if (this.formHandler != null && this.formChanged && !preFormChanged) this.formHandler.C(this);
	}

	/**
	 *  defines the thickness of the border when stroking.
	 */
	public void setLineWidth(int lineWidth) {

		if (this.BS == null) this.BS = new FormObject();

		this.BS.setIntNumber(PdfDictionary.W, lineWidth);
	}

	/**
	 * JS Controls whether the field is hidden or visible on screen and in print. Values are: visible 0, hidden 1, noPrint 2, noView 3.
	 * 
	 * GetCharacteristics will add this value into its array when called.
	 */
	public int display = -1;

	/**
	 *  added for backward compatibility or old adobe files.
	 */
	public void setBorderWidth(int width) {
		setLineWidth(width);
	}

	/*
	 * change; p373 js_api Example; changeEx commitKey; fieldFull; keyDown; modifier; name; rc; richChange; richChangeEx; richValue; selEnd; selStart;
	 * shift; source; target; targetName; type; value;
	 */
	/**
	 *  Verifies the current keystroke event before the data is committed. It can be used to check target form field values to verify, for
	 *        example, whether character data was entered instead of numeric data. JavaScript sets this property to true after the last keystroke
	 *        event and before the field is validated.
	 */
	public static boolean willCommit() {
		// CHRIS javascript unimplemented willcommit
		return true;
	}

	/**
	 * JS shows if the display value has changed, if it has we need to check what to and change <br>
	 * call getCharacteristics() to get the new display values and that will reset the display flag
	 */
	public boolean hasDisplayChanged() {
		boolean checkChange = (this.display != -1);
		if (checkChange) return true;
		else return false;
	}

	/**
	 *  added to return this for event.target from javascript
	 */
	public Object getTarget() {
		return this;
	}

	/**
	 *  @return the normal caption associated to this button
	 */
	public String buttonGetCaption() {
		return buttonGetCaption(0);
	}

	/**
	 *  @return the caption associated with this button,
	 * @param nFace
	 *            - 0 normal caption (default) 1 down caption 2 rollover caption
	 */
	public String buttonGetCaption(int nFace) {
		switch (nFace) {
			case 1:
				return getDictionary(PdfDictionary.MK).getTextStreamValue(PdfDictionary.AC);
			case 2:
				return getDictionary(PdfDictionary.MK).getTextStreamValue(PdfDictionary.RC);
			default:
				return getDictionary(PdfDictionary.MK).getTextStreamValue(PdfDictionary.CA);
		}
	}

	/**
	 *  sets this buttons normal caption to <b>cCaption</b>
	 */
	public void buttonSetCaption(String cCaption) {
		buttonSetCaption(cCaption, 0);
	}

	/**
	 *  sets this buttons caption to <b>cCaption</b>, it sets the caption defined by <b>nFace</b>.
	 * @param nFace
	 *            - 0 (default) normal caption 1 down caption 2 rollover caption
	 */
	public void buttonSetCaption(String cCaption, int nFace) {
		switch (nFace) {
			case 1:
				getDictionary(PdfDictionary.MK).setTextStreamValue(PdfDictionary.AC, StringUtils.toBytes(cCaption));
				break;
			case 2:
				getDictionary(PdfDictionary.MK).setTextStreamValue(PdfDictionary.RC, StringUtils.toBytes(cCaption));
				break;
			default:
				getDictionary(PdfDictionary.MK).setTextStreamValue(PdfDictionary.CA, StringUtils.toBytes(cCaption));
		}
	}

	/**
	 * @return the background color for the annotation objects
	 */
	public Object getfillColor() {
		return generateColor(getDictionary(PdfDictionary.MK).getFloatArray(PdfDictionary.BG));
	}

	public boolean isKid() {
		return this.isKid;
	}

	public void setKid(boolean kid) {
		this.isKid = kid;
	}

	public void setFormType(int widgetType) {
		this.formType = widgetType;
	}

	/**
	 * look at FormFactory.LIST etc for full list of types
	 */
	public int getFormType() {
		return this.formType;
	}

	/** @return an Opaque BufferedImage for use when appearance Streams are null */
	public static BufferedImage getOpaqueImage() {
		return new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
	}

	public String getNameUsed() {
		return this.nameUsed;
	}

	public void setNameUsed(String name) {
		this.nameUsed = name;
	}

	public boolean testedForDuplicates() {
		return this.tested;
	}

	public void testedForDuplicates(boolean b) {
		this.tested = b;
	}

	public void setGUIComponent(Object guiComp) {
		this.guiComp = guiComp;
	}

	public Object getGUIComponent() {
		return this.guiComp;
	}

	private boolean isReadOnly() {

		boolean isReadOnly = false;

		boolean[] flags = getFieldFlags();
		boolean[] characteristics = getCharacteristics();

		if (((flags != null) && (flags[FormObject.READONLY_ID])) || (characteristics != null && characteristics[9])// characteristics[9] =
																													// LockedContents
		// p609 PDF ref ver 1-7, characteristics 'locked' flag does allow contents to be edited,
		// but the 'LockedContents' flag stops content being changed.

		) {
			isReadOnly = true;
		}

		return isReadOnly;
	}

	/**
	 * allow us to update value (and sync to GUI version if exists
	 */
	public void updateValue(int key, final Object value, boolean sync) {

		// if(isReadOnly())
		// return;

		switch (key) {
			case ComponentData.TEXT_TYPE:
				this.textString = (String) value;

				// stop updating if value unchanged (and stops our legacy code failing in continuous loop
				if (this.lastTextString != null && this.textString != null && this.textString.equals(this.lastTextString)) {
					sync = false;
				}

				// track last value
				this.lastTextString = this.textString;

				break;

			default:
				throw new RuntimeException("Unknown type " + key);
		}

		/**
		 * update our Swing/ULC component if needed on correct thread
		 */
		if (sync) {

			if (this.guiComp != null) {
				if (this.guiComp instanceof JTextComponent) {
					if (SwingUtilities.isEventDispatchThread()) {
						((JTextComponent) this.guiComp).setText((String) value);
					}
					else {
						final Runnable doPaintComponent = new Runnable() {
							@Override
							public void run() {
								((JTextComponent) FormObject.this.guiComp).setText((String) value);
							}
						};
						SwingUtilities.invokeLater(doPaintComponent);
					}

				}
				else {}
			}
		}
		else {
			// we will cache updates here in JS and then write out at end.
		}
	}

	/**
	 * Identify whether we use a Text component, JList,
	 * 
	 * @param guiType
	 */
	public void setGUIType(int guiType) {
		this.guiType = guiType;
	}

	public int getGUIType() {
		return this.guiType;
	}
}
