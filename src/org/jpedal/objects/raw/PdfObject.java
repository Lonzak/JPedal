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
 * PdfObject.java
 * ---------------
 */
package org.jpedal.objects.raw;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.jpedal.fonts.StandardFonts;
import org.jpedal.io.ObjectStore;
import org.jpedal.io.PdfFileReader;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.StringUtils;

/**
 * holds actual data for PDF file to process
 */
public class PdfObject implements Cloneable {

	protected boolean isIndexed, maybeIndirect = false, isFullyResolved = true, isDataExternal = false;

	private boolean streamMayBeCorrupt;

	/**
	 * states
	 */
	public static final int DECODED = 0;
	public static final int UNDECODED_REF = 1;
	public static final int UNDECODED_DIRECT = 2;

	private int status = DECODED;

	byte[] unresolvedData = null;

	// hold Other dictionary values
	Map otherValues = new HashMap();

	protected int pageNumber = -1;

	int PDFkeyInt = -1;

	// our type which may not be same as /Type
	int objType = PdfDictionary.Unknown;

	// key of object
	private int id = -1;

	protected int colorspace = PdfDictionary.Unknown, subtype = PdfDictionary.Unknown, type = PdfDictionary.Unknown;

	private int BitsPerComponent = -1, BitsPerCoordinate = -1, BitsPerFlag = -1, Count = 0, FormType = -1, Length = -1, Length1 = -1, Length2 = -1,
			Length3 = -1, Rotate = -1; // -1 shows unset

	private float[] ArtBox, BBox, BleedBox, CropBox, Decode, Domain, Matrix, MediaBox, Range, TrimBox;

	protected PdfObject ColorSpace = null, DecodeParms = null, Encoding = null, Function = null, Resources = null, Shading = null, SMask = null;

	private boolean ignoreRecursion = false, ignoreStream = false;

	// used by font code
	protected boolean isZapfDingbats = false, isSymbol = false;

	private boolean isCompressedStream = false;

	protected int generalType = PdfDictionary.Unknown; // some Dictionaries can be a general type (ie /ToUnicode /Identity-H)

	private String generalTypeAsString = null; // some values (ie CMAP can have unknown settings)

	// flag to show if we want parents (generally NO as will scan all up tree every time to root)
	protected boolean includeParent = false;

	private String Creator = null, Parent = null, Name = null, S = null, Title = null;
	private byte[] rawCreator, rawParent, rawName = null, rawS, rawTitle = null;
	public static boolean debug = false;

	protected String ref = null;
	int intRef, gen;

	protected boolean hasStream = false;

	public byte[] stream = null;
	private byte[] DecodedStream = null;

	// use for caching
	private long startStreamOnDisk = -1;
	private PdfFileReader objReader = null;
	private String cacheName = null;

	private byte[][] Filter = null, TR = null;

	private byte[][] keys;

	private byte[][] values;

	private Object[] DecodeParmsAsArray;

	private PdfObject[] objs;

	// used by /Other
	protected Object currentKey = null;

	// used to track AP
	protected int parentType = -1;
	private boolean isInCompressedStream;

	/** used to give the number of a new XFA reference ie. 1 0 X (XFA internal form) */
	private static int newXFAFormID = 1;

	/** set this PdfObject up as an internal object and define its reference */
	protected void setInternalReference() {
		// if this is an internal object generate the next key
		this.ref = (newXFAFormID++) + " 0 X";
	}

	protected PdfObject() {
	}

	public PdfObject(byte[] bytes) {}

	public PdfObject(int intRef, int gen) {
		setRef(intRef, gen);
	}

	public void setRef(int intRef, int gen) {
		this.intRef = intRef;
		this.gen = gen;

		// force reset as may have changed
		this.ref = null;
	}

	/**
	 * 
	 * @return name of file with cached data on disk or null
	 */
	public String getCacheName(PdfFileReader objReader) {

		if (isCached()) {
			this.cacheName = null;
			this.getCachedStreamFile(objReader);
		}
		return this.cacheName;
	}

	public void setRef(String ref) {

		this.ref = ref;
	}

	public PdfObject(String ref) {
		this.ref = ref;

		// int ptr=ref.indexOf(" ");
		// if(ptr>0)
		// intRef=PdfReader.parseInt(0, ptr, StringUtils.toBytes(ref));
	}

	public PdfObject(int type) {
		this.generalType = type;
	}

	protected static boolean[] deepCopy(boolean[] input) {

		if (input == null) return null;

		int count = input.length;

		boolean[] deepCopy = new boolean[count];
		System.arraycopy(input, 0, deepCopy, 0, count);

		return deepCopy;
	}

	public int getStatus() {
		return this.status;
	}

	public byte[] getUnresolvedData() {
		return this.unresolvedData;
	}

	public int getPDFkeyInt() {
		return this.PDFkeyInt;
	}

	public void setUnresolvedData(byte[] unresolvedData, int PDFkeyInt) {
		this.unresolvedData = unresolvedData;
		this.PDFkeyInt = PDFkeyInt;
		/**
		 * int len=unresolvedData.length;
		 * 
		 * //if ref get first value as int if(unresolvedData[len-1]=='R'){
		 * 
		 * int ptr=0, ii=0; while(ii<len){
		 * 
		 * ii++;
		 * 
		 * if(unresolvedData[ii]==' '){ ptr=ii; break; }
		 * 
		 * } if(ptr>0) intRef=PdfReader.parseInt(0, ptr, unresolvedData);
		 * 
		 * }/
		 **/
	}

	public void setStatus(int status) {
		this.status = status;
		this.unresolvedData = null;
	}

	protected static float[] deepCopy(float[] input) {

		if (input == null) return null;

		int count = input.length;

		float[] deepCopy = new float[count];
		System.arraycopy(input, 0, deepCopy, 0, count);

		return deepCopy;
	}

	protected static double[] deepCopy(double[] input) {

		if (input == null) return null;

		int count = input.length;

		double[] deepCopy = new double[count];
		System.arraycopy(input, 0, deepCopy, 0, count);

		return deepCopy;
	}

	protected static int[] deepCopy(int[] input) {

		if (input == null) return null;

		int count = input.length;

		int[] deepCopy = new int[count];
		System.arraycopy(input, 0, deepCopy, 0, count);

		return deepCopy;
	}

	protected static byte[][] deepCopy(byte[][] input) {

		if (input == null) return null;

		int count = input.length;

		byte[][] deepCopy = new byte[count][];
		System.arraycopy(input, 0, deepCopy, 0, count);

		return deepCopy;
	}

	public PdfObject getDictionary(int id) {

		switch (id) {

			case PdfDictionary.ColorSpace:
				return this.ColorSpace;

			case PdfDictionary.DecodeParms:
				return this.DecodeParms;

			case PdfDictionary.Function:
				return this.Function;

			case PdfDictionary.Resources:
				return this.Resources;

			case PdfDictionary.Shading:
				return this.Shading;

			case PdfDictionary.SMask:
				return this.SMask;

			default:

				return null;
		}
	}

	public int getGeneralType(int id) {

		// special case
		if (id == PdfDictionary.Encoding && this.isZapfDingbats) // note this is Enc object so local
		return StandardFonts.ZAPF;
		else
			if (id == PdfDictionary.Encoding && this.isSymbol) // note this is Enc object so local
			return StandardFonts.SYMBOL;
			else
				if (id == PdfDictionary.Type) return this.objType;
				else return this.generalType;
	}

	public String getGeneralStringValue() {
		return this.generalTypeAsString;
	}

	public void setGeneralStringValue(String generalTypeAsString) {
		this.generalTypeAsString = generalTypeAsString;
	}

	public void setIntNumber(int id, int value) {

		switch (id) {

			case PdfDictionary.BitsPerComponent:
				this.BitsPerComponent = value;
				break;

			case PdfDictionary.BitsPerCoordinate:
				this.BitsPerCoordinate = value;
				break;

			case PdfDictionary.BitsPerFlag:
				this.BitsPerFlag = value;
				break;

			case PdfDictionary.Count:
				this.Count = value;
				break;

			case PdfDictionary.FormType:
				this.FormType = value;
				break;

			case PdfDictionary.Length:
				this.Length = value;
				break;

			case PdfDictionary.Length1:
				this.Length1 = value;
				break;

			case PdfDictionary.Length2:
				this.Length2 = value;
				break;

			case PdfDictionary.Length3:
				this.Length3 = value;
				break;

			case PdfDictionary.Rotate:
				this.Rotate = value;
				break;

			default:

		}
	}

	public void setFloatNumber(int id, float value) {

		switch (id) {

		// case PdfDictionary.BitsPerComponent:
		// BitsPerComponent=value;
		// break;

			default:

		}
	}

	public int getInt(int id) {

		switch (id) {

			case PdfDictionary.BitsPerComponent:
				return this.BitsPerComponent;

			case PdfDictionary.BitsPerCoordinate:
				return this.BitsPerCoordinate;

			case PdfDictionary.BitsPerFlag:
				return this.BitsPerFlag;

			case PdfDictionary.Count:
				return this.Count;

			case PdfDictionary.FormType:
				return this.FormType;

			case PdfDictionary.Length:
				return this.Length;

			case PdfDictionary.Length1:
				return this.Length1;

			case PdfDictionary.Length2:
				return this.Length2;

			case PdfDictionary.Length3:
				return this.Length3;

			case PdfDictionary.Rotate:
				return this.Rotate;

			default:

				return PdfDictionary.Unknown;
		}
	}

	public float getFloatNumber(int id) {

		switch (id) {

		// case PdfDictionary.BitsPerComponent:
		// return BitsPerComponent;

			default:

				return PdfDictionary.Unknown;
		}
	}

	public boolean getBoolean(int id) {

		switch (id) {

			default:

		}

		return false;
	}

	public void setBoolean(int id, boolean value) {

		switch (id) {

			default:

		}
	}

	public void setDictionary(int id, PdfObject value) {

		value.id = id;

		switch (id) {

			case PdfDictionary.ColorSpace:
				this.ColorSpace = value;
				break;

			case PdfDictionary.DecodeParms:
				this.DecodeParms = value;
				break;

			case PdfDictionary.Function:
				this.Function = value;
				break;

			case PdfDictionary.Resources:
				this.Resources = value;
				break;

			case PdfDictionary.Shading:
				this.Shading = value;
				break;

			case PdfDictionary.SMask:
				this.SMask = value;
				break;

			default:

				setOtherValues(id, value);

		}
	}

	/**
	 * some values stored in a MAP for AP or Structurede Content
	 */
	protected void setOtherValues(int id, PdfObject value) {

		if (this.objType == PdfDictionary.Form || this.objType == PdfDictionary.MCID || this.currentKey != null) {

			// if(1==1)
			// throw new RuntimeException("xx="+currentKey+" id="+id);

			this.otherValues.put(this.currentKey, value);
			this.currentKey = null;
		}
	}

	public void setID(int id) {

		this.id = id;
	}

	public int getID() {
		return this.id;
	}

	/**
	 * only used internally for some forms - please do not use
	 * 
	 */
	public int getParentID() {
		return this.parentType;
	}

	public void setParentID(int parentType) {
		this.parentType = parentType;
	}

	/**
	 * flag set for embedded data
	 */
	public boolean hasStream() {
		return this.hasStream;
	}

	// public int setConstant(int pdfKeyType, int keyStart, int keyLength, byte[] raw) {
	//
	//
	// return PdfDictionary.Unknown;
	// }

	public int setConstant(int pdfKeyType, int keyStart, int keyLength, byte[] raw) {

		int PDFvalue = PdfDictionary.Unknown;

		int id = 0, x = 0, next;

		try {

			// convert token to unique key which we can lookup

			for (int i2 = keyLength - 1; i2 > -1; i2--) {

				next = raw[keyStart + i2];

				next = next - 48;

				id = id + ((next) << x);

				x = x + 8;
			}

			switch (id) {

			// case PdfDictionary.Image:
			// PDFvalue =PdfDictionary.Image;
			// break;
			//
			// case PdfDictionary.Form:
			// PDFvalue =PdfDictionary.Form;
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
					// }
					// }else

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

		return id;
	}

	public int getParameterConstant(int key) {
		int def = PdfDictionary.Unknown;

		switch (key) {

			case PdfDictionary.ColorSpace:
				return this.colorspace;

			case PdfDictionary.Subtype:
				return this.subtype;

			case PdfDictionary.Type:
				return this.type;

		}

		return def;
	}

	/**
	 * common values shared between types
	 */
	public int setConstant(int pdfKeyType, int id) {
		int PDFvalue = id;

		/**
		 * map non-standard
		 */
		switch (id) {

			case PdfDictionary.FontDescriptor:
				PDFvalue = PdfDictionary.Font;
				break;

		}

		switch (pdfKeyType) {

			case PdfDictionary.ColorSpace:
				this.colorspace = PDFvalue;
				break;

			case PdfDictionary.Subtype:
				this.subtype = PDFvalue;
				break;

			case PdfDictionary.Type:

				// @speed if is temp hack as picks up types on some subobjects
				// if(type==PdfDictionary.Unknown)
				this.type = PDFvalue;

				break;
		}

		return PDFvalue;
	}

	public float[] getFloatArray(int id) {

		float[] array = null;
		switch (id) {

			case PdfDictionary.ArtBox:
				return deepCopy(this.ArtBox);

			case PdfDictionary.BBox:
				return deepCopy(this.BBox);

			case PdfDictionary.BleedBox:
				return deepCopy(this.BleedBox);

			case PdfDictionary.CropBox:
				return deepCopy(this.CropBox);

			case PdfDictionary.Decode:
				return deepCopy(this.Decode);

			case PdfDictionary.Domain:
				return deepCopy(this.Domain);

			case PdfDictionary.Matrix:
				return deepCopy(this.Matrix);

			case PdfDictionary.MediaBox:
				return deepCopy(this.MediaBox);

			case PdfDictionary.Range:
				return deepCopy(this.Range);

			case PdfDictionary.TrimBox:
				return deepCopy(this.TrimBox);

			default:

		}

		return deepCopy(array);
	}

	public byte[][] getKeyArray(int id) {

		switch (id) {

			default:

		}

		return null;
	}

	public double[] getDoubleArray(int id) {

		double[] array = null;
		switch (id) {

			default:

		}

		return deepCopy(array);
	}

	public boolean[] getBooleanArray(int id) {

		boolean[] array = null;
		switch (id) {

			default:

		}

		return deepCopy(array);
	}

	public int[] getIntArray(int id) {

		int[] array = null;
		switch (id) {

			default:

		}

		return deepCopy(array);
	}

	public void setFloatArray(int id, float[] value) {

		switch (id) {

			case PdfDictionary.ArtBox:
				this.ArtBox = value;
				break;

			case PdfDictionary.BBox:
				this.BBox = value;
				break;

			case PdfDictionary.BleedBox:
				this.BleedBox = value;
				break;

			case PdfDictionary.CropBox:
				this.CropBox = value;
				break;

			case PdfDictionary.Decode:
				this.Decode = ignoreIdentity(value);
				break;

			case PdfDictionary.Domain:
				this.Domain = value;
				break;

			case PdfDictionary.Matrix:
				this.Matrix = value;
				break;

			case PdfDictionary.MediaBox:
				this.MediaBox = value;
				break;

			case PdfDictionary.Range:
				this.Range = value;
				break;

			case PdfDictionary.TrimBox:
				this.TrimBox = value;
				break;

			default:

		}
	}

	/** ignore identity value which makes no change */
	private static float[] ignoreIdentity(float[] value) {

		boolean isIdentity = true;
		if (value != null) {

			int count = value.length;
			for (int aa = 0; aa < count; aa = aa + 2) {
				if (value[aa] == 0f && value[aa + 1] == 1f) {
					// okay
				}
				else {
					isIdentity = false;
					aa = count;
				}
			}
		}

		if (isIdentity) return null;
		else return value;
	}

	public void setIntArray(int id, int[] value) {

		switch (id) {

			default:

		}
	}

	public void setBooleanArray(int id, boolean[] value) {

		switch (id) {

			default:

		}
	}

	public void setDoubleArray(int id, double[] value) {

		switch (id) {

			default:

		}
	}

	public void setMixedArray(int id, byte[][] value) {

		switch (id) {

			case PdfDictionary.Filter:

				this.Filter = value;
				break;

			default:

		}
	}

	public String getStringValue(int id, int mode) {

		byte[] data = null;

		// get data
		switch (id) {

			case PdfDictionary.Name:

				data = this.rawName;
				break;

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
				throw new RuntimeException("Value not defined in getName(int,mode)");
		}
	}

	// return as constant we can check
	public int getNameAsConstant(int id) {
		// return PdfDictionary.generateChecksum(0,raw.length,raw);
		return PdfDictionary.Unknown;
	}

	public String getName(int id) {

		String str = null;
		switch (id) {

			case PdfDictionary.Name:

				// setup first time
				if (this.Name == null && this.rawName != null) this.Name = new String(this.rawName);

				return this.Name;

				// case PdfDictionary.Parent:
				//
				// //setup first time
				// if(Filter==null && rawParent!=null)
				// Parent=new String(rawParent);
				//
				// return Parent;

			case PdfDictionary.S:

				// setup first time
				if (this.S == null && this.rawS != null) this.S = new String(this.rawS);

				return this.S;

			default:

		}

		return str;
	}

	public String getStringKey(int id) {

		String str = null;
		switch (id) {

			case PdfDictionary.Parent:

				// setup first time
				if (this.Parent == null && this.rawParent != null) this.Parent = new String(this.rawParent);

				return this.Parent;

			default:

		}

		return str;
	}

	public String getTextStreamValue(int id) {

		String str = null;
		switch (id) {

			case PdfDictionary.Creator:

				// setup first time
				if (this.Creator == null && this.rawCreator != null) this.Creator = StringUtils.getTextString(this.rawCreator, false);

				return this.Creator;

				// can also be stream in OCProperties
			case PdfDictionary.Name:

				// setup first time
				if (this.Name == null && this.rawName != null) this.Name = StringUtils.getTextString(this.rawName, false);

				return this.Name;

			case PdfDictionary.Title:

				// setup first time
				if (this.Title == null && this.rawTitle != null) this.Title = StringUtils.getTextString(this.rawTitle, false);

				return this.Title;

				// case PdfDictionary.Filter:
				//
				// //setup first time
				// if(Filter==null && rawFilter!=null)
				// Filter=PdfReader.getTextString(rawFilter);
				//
				// return Filter;

			default:

		}

		return str;
	}

	public void setName(int id, byte[] value) {

		switch (id) {

			case PdfDictionary.Name:
				this.rawName = value;
				break;

			case PdfDictionary.S:
				this.rawS = value;
				break;

			case PdfDictionary.Parent:

				// gets into endless loop if any obj so use sparingly
				if (this.includeParent) this.rawParent = value;

				break;

			default:

				if (this.objType == PdfDictionary.MCID) {

					// if(1==1)
					// throw new RuntimeException("xx="+currentKey+" id="+id);
					this.otherValues.put(this.currentKey, value);
					// System.out.println("id="+id+" "+value+" "+type+" "+objType+" "+this+" "+otherValues);
				}
				else {

				}
		}
	}

	public void setName(Object id, String value) {

		this.otherValues.put(id, value);
		// System.out.println("id="+id+" "+value+" "+type+" "+objType+" "+this+" "+otherValues);
	}

	public void setStringKey(int id, byte[] value) {

		switch (id) {

			case PdfDictionary.Parent:
				this.rawParent = value;
				break;

			default:

		}
	}

	public void setTextStreamValue(int id, byte[] value) {

		switch (id) {

			case PdfDictionary.Creator:
				this.rawCreator = value;
				break;

			case PdfDictionary.Name:
				this.rawName = value;
				break;

			case PdfDictionary.Title:
				this.rawTitle = value;
				break;

			default:

		}
	}

	public byte[] getDecodedStream() {

		if (isCached()) {
			byte[] cached = null;

			try {

				File f = new File(getCachedStreamFile(this.objReader));
				BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
				cached = new byte[(int) f.length()];

				// System.out.println(cached.length+" "+DecodedStream.length);
				bis.read(cached);
				bis.close();

				// System.out.println(new String(cached));
			}
			catch (Exception e) {
				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
			}

			return cached;
		}
		else

		return this.DecodedStream;
	}

	/**
	 * public byte[] getStream() {
	 * 
	 * if(DecodedStream==null) return null;
	 * 
	 * //make a a DEEP copy so we cant alter int len=DecodedStream.length; byte[] copy=new byte[len]; System.arraycopy(DecodedStream, 0, copy, 0,
	 * len);
	 * 
	 * return copy; }/
	 **/

	public void setStream(byte[] stream) {
		this.stream = stream;

		if (this.getObjectType() == PdfDictionary.ColorSpace) this.hasStream = true;
	}

	public void setDecodedStream(byte[] stream) {
		this.DecodedStream = stream;
	}

	public String getObjectRefAsString() {

		if (this.ref == null) this.ref = this.intRef + " " + this.gen + " R";

		return this.ref;
	}

	public int getObjectRefID() {

		// initialise if not set
		if (this.intRef == 0 && this.ref != null && !this.ref.contains("[")) {
			int ptr = this.ref.indexOf(' ');
			// System.out.println(ref.substring(0,ptr)+"<<ref="+ref+"<");

			if (ptr > 0) {
				try {
					this.intRef = Integer.parseInt(this.ref.substring(0, ptr));
				}
				catch (Exception e) {
					// tell user and log
					if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
				}

				int ptr2 = this.ref.indexOf('R', ptr);

				if (ptr2 > 0) {
					// System.out.println(ref.substring(ptr+1,ptr2-1)+"<<gen="+ref+"<");
					try {
						this.gen = Integer.parseInt(this.ref.substring(ptr + 1, ptr2 - 1));
					}
					catch (Exception e) {
						// tell user and log
						if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
					}
				}
			}
		}

		return this.intRef;
	}

	public int getObjectRefGeneration() {
		return this.gen;
	}

	public PdfArrayIterator getMixedArray(int id) {

		switch (id) {

			case PdfDictionary.Filter:
				return new PdfArrayIterator(this.Filter);

			default:

				return null;
		}
	}

	public void setDictionaryPairs(byte[][] keys, byte[][] values, PdfObject[] objs) {

		this.keys = keys;
		this.values = values;
		this.objs = objs;
	}

	public PdfKeyPairsIterator getKeyPairsIterator() {
		return new PdfKeyPairsIterator(this.keys, this.values, this.objs);
	}

	public void setKeyArray(int id, byte[][] keyValues) {

		switch (id) {

			default:

		}
	}

	public void setStringArray(int id, byte[][] keyValues) {

		switch (id) {

			case PdfDictionary.TR:
				this.TR = keyValues;
				break;

			default:

		}
	}

	public byte[][] getStringArray(int id) {

		switch (id) {

			case PdfDictionary.TR:
				return deepCopy(this.TR);

			default:

		}

		return null;
	}

	public Object[] getObjectArray(int id) {

		switch (id) {

			case PdfDictionary.DecodeParms:
				return (this.DecodeParmsAsArray);

			default:

		}

		return null;
	}

	public void setObjectArray(int id, Object[] objectValues) {

		switch (id) {

			case PdfDictionary.DecodeParms:

				this.DecodeParmsAsArray = objectValues;
				break;

			default:

		}
	}

	public PdfObject duplicate() {
		PdfObject copy = new PdfObject();

		return copy;
	}

	@Override
	public Object clone() {
		Object o = null;
		try {
			o = super.clone();
		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}

		return o;
	}

	public boolean decompressStreamWhenRead() {
		return false;
	}

	public int getObjectType() {
		return this.objType;
	}

	public byte[] getStringValueAsByte(int id) {
		return null;
	}

	public boolean isCompressedStream() {
		return this.isCompressedStream;
	}

	public void setCompressedStream(boolean isCompressedStream) {
		this.isCompressedStream = isCompressedStream;
	}

	/** do not cascade down whole tree */
	public boolean ignoreRecursion() {
		return this.ignoreRecursion;
	}

	/** do not cascade down whole tree */
	public void ignoreRecursion(boolean ignoreRecursion) {
		this.ignoreRecursion = ignoreRecursion;
	}

	public byte[] getTextStreamValueAsByte(int id) {
		return null;
	}

	public byte[][] getByteArray(int id) {
		// TODO Auto-generated method stub
		return null;
	}

	public void setTextStreamValue(int id2, String value) {
		// TODO Auto-generated method stub
	}

	/**
	 * used in Forms code where keys can be page numbers
	 * 
	 */
	public Map getOtherDictionaries() {
		return this.otherValues;
	}

	public void setCurrentKey(Object key) {
		this.currentKey = key;
	}

	// convenience method to return array as String
	public String toString(float[] floatArray, boolean appendPageNumber) {

		if (floatArray == null) return null;

		StringBuilder value = new StringBuilder();

		if (appendPageNumber) {
			value.append(this.pageNumber);
			value.append(' ');
		}

		for (float aFloatArray : floatArray) {
			value.append(aFloatArray);
			value.append(' ');
		}

		return value.toString();
	}

	/**
	 * @return the page this field is associated to
	 */
	public int getPageNumber() {
		return this.pageNumber;
	}

	/**
	 * set the page number for this form
	 */
	public void setPageNumber(int number) {

		this.pageNumber = number;
	}

	/**
	 * set the page number for this form
	 */
	public void setPageNumber(Object field) {
		if (field instanceof String) {
			try {
				this.pageNumber = Integer.parseInt((String) field);
			}
			catch (NumberFormatException e) {
				this.pageNumber = 1;
			}
		}
		else {
			LogWriter.writeFormLog("{FormObject.setPageNumber} pagenumber being set to UNKNOWN type", false);
		}
	}

	public void setCache(long offset, PdfFileReader objReader) {
		this.startStreamOnDisk = offset;
		this.objReader = objReader;
	}

	public boolean isCached() {
		return this.startStreamOnDisk != -1;
	}

	public String getCachedStreamFile(PdfFileReader objReader) {

		File tmpFile = null;

		if (this.startStreamOnDisk != -1) { // cached so we need to read it

			try {

				tmpFile = File.createTempFile("jpedal-", ".bin", new File(ObjectStore.temp_dir));
				tmpFile.deleteOnExit();

				// put raw data on disk
				objReader.spoolStreamDataToDisk(tmpFile, this.startStreamOnDisk);

				// set name for access
				this.cacheName = tmpFile.getAbsolutePath();

				// System.out.println("cached file size="+tmpFile.length()+" "+this.getObjectRefAsString());
			}
			catch (Exception e) {
				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
			}
			finally {
				// remove at end
				if (tmpFile != null) tmpFile.deleteOnExit();
			}
		}

		// decrypt and decompress
		if (getObjectType() != PdfDictionary.XObject) {
			objReader.readStream(this, true, true, false, getObjectType() == PdfDictionary.Metadata, this.isCompressedStream, this.cacheName);
		}

		return this.cacheName;
	}

	public boolean isColorSpaceIndexed() {
		return this.isIndexed;
	}

	public void setInCompressedStream(boolean isInCompressedStream) {
	}

	public boolean isInCompressedStream() {
		return this.isInCompressedStream;
	}

	// use in colorpsace to get ref correct
	public void maybeIndirect(boolean b) {
		this.maybeIndirect = b;
	}

	// use in colorpsace to get ref correct
	public boolean maybeIndirect() {
		return this.maybeIndirect;
	}

	public boolean isFullyResolved() {
		return this.isFullyResolved;
	}

	public void setFullyResolved(boolean isFullyResolved) {
		this.isFullyResolved = isFullyResolved;
	}

	public boolean isDataExternal() {
		return this.isDataExternal;
	}

	public void isDataExternal(boolean isDataExternal) {
		this.isDataExternal = isDataExternal;
	}

	public boolean ignoreStream() {
		return this.ignoreStream;
	}

	public void ignoreStream(boolean ignoreStream) {

		this.ignoreStream = ignoreStream;
	}

	public void setStreamMayBeCorrupt(boolean streamMayBeCorrupt) {
		this.streamMayBeCorrupt = streamMayBeCorrupt;
	}

	public boolean streamMayBeCorrupt() {
		return this.streamMayBeCorrupt;
	}

	/**
	 * move across and reset in pdfObject
	 * 
	 * @param pdfObject
	 */
	public void moveCacheValues(PdfObject pdfObject) {

		this.startStreamOnDisk = pdfObject.startStreamOnDisk;
		pdfObject.startStreamOnDisk = -1;

		this.cacheName = pdfObject.cacheName;
		pdfObject.cacheName = null;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PdfObject [");

		if (this.rawName != null) {
			builder.append("rawName=");
			builder.append(new String(this.rawName));
			builder.append(", ");
		}
		builder.append("isIndexed=");
		builder.append(this.isIndexed);
		builder.append(", maybeIndirect=");
		builder.append(this.maybeIndirect);
		builder.append(", isFullyResolved=");
		builder.append(this.isFullyResolved);
		builder.append(", isDataExternal=");
		builder.append(this.isDataExternal);
		builder.append(", streamMayBeCorrupt=");
		builder.append(this.streamMayBeCorrupt);
		builder.append(", status=");
		builder.append(this.status);
		builder.append(", ");
		if (this.unresolvedData != null) {
			builder.append("unresolvedData=");
			builder.append(new String(this.unresolvedData));
			builder.append(", ");
		}
		if (this.otherValues != null) {
			builder.append("otherValues=");
			builder.append(this.otherValues);
			builder.append(", ");
		}
		builder.append("pageNumber=");
		builder.append(this.pageNumber);
		builder.append(", PDFkeyInt=");
		builder.append(this.PDFkeyInt);
		builder.append(", objType=");
		builder.append(this.objType);
		builder.append(", id=");
		builder.append(this.id);
		builder.append(", colorspace=");
		builder.append(this.colorspace);
		builder.append(", subtype=");
		builder.append(this.subtype);
		builder.append(", type=");
		builder.append(this.type);
		builder.append(", BitsPerComponent=");
		builder.append(this.BitsPerComponent);
		builder.append(", BitsPerCoordinate=");
		builder.append(this.BitsPerCoordinate);
		builder.append(", BitsPerFlag=");
		builder.append(this.BitsPerFlag);
		builder.append(", Count=");
		builder.append(this.Count);
		builder.append(", FormType=");
		builder.append(this.FormType);
		builder.append(", Length=");
		builder.append(this.Length);
		builder.append(", Length1=");
		builder.append(this.Length1);
		builder.append(", Length2=");
		builder.append(this.Length2);
		builder.append(", Length3=");
		builder.append(this.Length3);
		builder.append(", Rotate=");
		builder.append(this.Rotate);
		builder.append(", ");
		if (this.ArtBox != null) {
			builder.append("ArtBox=");
			builder.append(Arrays.toString(this.ArtBox));
			builder.append(", ");
		}
		if (this.BBox != null) {
			builder.append("BBox=");
			builder.append(Arrays.toString(this.BBox));
			builder.append(", ");
		}
		if (this.BleedBox != null) {
			builder.append("BleedBox=");
			builder.append(Arrays.toString(this.BleedBox));
			builder.append(", ");
		}
		if (this.CropBox != null) {
			builder.append("CropBox=");
			builder.append(Arrays.toString(this.CropBox));
			builder.append(", ");
		}
		if (this.Decode != null) {
			builder.append("Decode=");
			builder.append(Arrays.toString(this.Decode));
			builder.append(", ");
		}
		if (this.Domain != null) {
			builder.append("Domain=");
			builder.append(Arrays.toString(this.Domain));
			builder.append(", ");
		}
		if (this.Matrix != null) {
			builder.append("Matrix=");
			builder.append(Arrays.toString(this.Matrix));
			builder.append(", ");
		}
		if (this.MediaBox != null) {
			builder.append("MediaBox=");
			builder.append(Arrays.toString(this.MediaBox));
			builder.append(", ");
		}
		if (this.Range != null) {
			builder.append("Range=");
			builder.append(Arrays.toString(this.Range));
			builder.append(", ");
		}
		if (this.TrimBox != null) {
			builder.append("TrimBox=");
			builder.append(Arrays.toString(this.TrimBox));
			builder.append(", ");
		}
		if (this.ColorSpace != null) {
			builder.append("ColorSpace=");
			builder.append(this.ColorSpace);
			builder.append(", ");
		}
		if (this.DecodeParms != null) {
			builder.append("DecodeParms=");
			builder.append(this.DecodeParms);
			builder.append(", ");
		}
		if (this.Encoding != null) {
			builder.append("Encoding=");
			builder.append(this.Encoding);
			builder.append(", ");
		}
		if (this.Function != null) {
			builder.append("Function=");
			builder.append(this.Function);
			builder.append(", ");
		}
		if (this.Resources != null) {
			builder.append("Resources=");
			builder.append(this.Resources);
			builder.append(", ");
		}
		if (this.Shading != null) {
			builder.append("Shading=");
			builder.append(this.Shading);
			builder.append(", ");
		}
		if (this.SMask != null) {
			builder.append("SMask=");
			builder.append(this.SMask);
			builder.append(", ");
		}
		builder.append("ignoreRecursion=");
		builder.append(this.ignoreRecursion);
		builder.append(", ignoreStream=");
		builder.append(this.ignoreStream);
		builder.append(", isZapfDingbats=");
		builder.append(this.isZapfDingbats);
		builder.append(", isSymbol=");
		builder.append(this.isSymbol);
		builder.append(", isCompressedStream=");
		builder.append(this.isCompressedStream);
		builder.append(", generalType=");
		builder.append(this.generalType);
		builder.append(", ");
		if (this.generalTypeAsString != null) {
			builder.append("generalTypeAsString=");
			builder.append(this.generalTypeAsString);
			builder.append(", ");
		}
		builder.append("includeParent=");
		builder.append(this.includeParent);
		builder.append(", ");
		if (this.Creator != null) {
			builder.append("Creator=");
			builder.append(this.Creator);
			builder.append(", ");
		}
		if (this.Parent != null) {
			builder.append("Parent=");
			builder.append(this.Parent);
			builder.append(", ");
		}
		if (this.Name != null) {
			builder.append("Name=");
			builder.append(this.Name);
			builder.append(", ");
		}
		if (this.S != null) {
			builder.append("S=");
			builder.append(this.S);
			builder.append(", ");
		}
		if (this.Title != null) {
			builder.append("Title=");
			builder.append(this.Title);
			builder.append(", ");
		}
		if (this.rawCreator != null) {
			builder.append("rawCreator=");
			builder.append(new String(this.rawCreator));
			builder.append(", ");
		}
		if (this.rawParent != null) {
			builder.append("rawParent=");
			builder.append(new String(this.rawParent));
			builder.append(", ");
		}
		if (this.rawS != null) {
			builder.append("rawS=");
			builder.append(new String(this.rawS));
			builder.append(", ");
		}
		if (this.rawTitle != null) {
			builder.append("rawTitle=");
			builder.append(new String(this.rawTitle));
			builder.append(", ");
		}
		if (this.ref != null) {
			builder.append("ref=");
			builder.append(this.ref);
			builder.append(", ");
		}
		builder.append("intRef=");
		builder.append(this.intRef);
		builder.append(", gen=");
		builder.append(this.gen);
		builder.append(", hasStream=");
		builder.append(this.hasStream);
		builder.append(", ");
		if (this.stream != null) {
			builder.append("stream=");
			builder.append(new String(this.stream));
			builder.append(", ");
		}
		if (this.DecodedStream != null) {
			builder.append("DecodedStream=");
			builder.append(new String(this.DecodedStream));
			builder.append(", ");
		}
		builder.append("startStreamOnDisk=");
		builder.append(this.startStreamOnDisk);
		builder.append(", ");
		if (this.objReader != null) {
			builder.append("objReader=");
			builder.append(this.objReader);
			builder.append(", ");
		}
		if (this.cacheName != null) {
			builder.append("cacheName=");
			builder.append(this.cacheName);
			builder.append(", ");
		}
		if (this.Filter != null) {
			builder.append("Filter=");
			builder.append(Arrays.toString(this.Filter));
			builder.append(", ");
		}
		if (this.TR != null) {
			builder.append("TR=");
			builder.append(Arrays.toString(this.TR));
			builder.append(", ");
		}
		if (this.keys != null) {
			builder.append("keys=");
			builder.append(Arrays.toString(this.keys));
			builder.append(", ");
		}
		if (this.values != null) {
			builder.append("values=");
			builder.append(Arrays.toString(this.values));
			builder.append(", ");
		}
		if (this.DecodeParmsAsArray != null) {
			builder.append("DecodeParmsAsArray=");
			builder.append(Arrays.toString(this.DecodeParmsAsArray));
			builder.append(", ");
		}
		if (this.objs != null) {
			builder.append("objs=");
			builder.append(Arrays.toString(this.objs));
			builder.append(", ");
		}
		if (this.currentKey != null) {
			builder.append("currentKey=");
			builder.append(this.currentKey);
			builder.append(", ");
		}
		builder.append("parentType=");
		builder.append(this.parentType);
		builder.append(", isInCompressedStream=");
		builder.append(this.isInCompressedStream);
		builder.append("]");
		return builder.toString();
	}
}