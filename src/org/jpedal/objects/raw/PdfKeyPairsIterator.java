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
 * PdfKeyPairsIterator.java
 * ---------------
 */
package org.jpedal.objects.raw;

import org.jpedal.utils.NumberUtils;

/**
 * allow fast access to data from PDF object
 * 
 */
public class PdfKeyPairsIterator {

	private byte[][] keys, values;

	private PdfObject[] objs;

	int maxCount, current = 0;

	public PdfKeyPairsIterator(byte[][] keys, byte[][] values, PdfObject[] objs) {

		this.keys = keys;
		this.values = values;
		this.objs = objs;

		if (keys != null) this.maxCount = keys.length;

		this.current = 0;
	}

	/**
	 * number of PAIRS (or keys)
	 * 
	 * @return
	 */
	public int getTokenCount() {
		return this.maxCount;
	}

	/**
	 * roll onto next key and value
	 * 
	 * @return
	 */
	public void nextPair() {

		if (this.current < this.maxCount) this.current++;
		else throw new RuntimeException("No keys left in PdfKeyPairsIterator");
	}

	/**
	 * next key
	 * 
	 * @return
	 */
	public String getNextKeyAsString() {

		// System.out.println((char)keys[current-1][0]+"<length="+keys[current-1].length);
		/**
		 * if(convertNumberToString){ //decide if number and convert to value int length=keys[current].length; boolean isNotNumber=false; for(int
		 * ii=0;ii<length;ii++){ int nextChar=keys[current][ii]; if(nextChar>='0' && nextChar<='9'){
		 * 
		 * }else{ isNotNumber=true; ii=length; } }
		 * 
		 * if(isNotNumber){ return new String(keys[current]); }else{ int key=PdfReader.parseInt(0,length,keys[current]);
		 * 
		 * //generate if needed and cache if(IntsAsChars[key]==null){ char[] stringChar=new char[1]; stringChar[0]=(char)key; IntsAsChars[key]=new
		 * String(stringChar); }
		 * 
		 * return IntsAsChars[key]; } }else
		 */
		return new String(this.keys[this.current]);
	}

	/**
	 * used by CharProcs to return number or number of key (ie /12 or /A)
	 * 
	 * @return
	 */
	public int getNextKeyAsNumber() {

		// System.out.println((char)keys[current-1][0]+"<length="+keys[current-1].length);

		int length = this.keys[this.current].length;
		boolean isNumber = isNextKeyANumber();

		if (!isNumber) {
			if (this.keys[this.current].length != 1) {
				if (1 == 1) throw new RuntimeException("Unexpected value in getNextKeyAsNumber >" + new String(this.keys[this.current]) + '<');
			}
			else return (this.keys[this.current][0] & 255);
		}
		else return NumberUtils.parseInt(0, length, this.keys[this.current]);

		return -1;
	}

	public boolean isNextKeyANumber() {

		int length = this.keys[this.current].length;

		boolean isNumber = true;

		for (int ii = 0; ii < length; ii++) {
			int nextChar = this.keys[this.current][ii];

			// System.out.println(nextChar);
			if (nextChar >= '0' && nextChar <= '9') {

			}
			else {
				isNumber = false;
				ii = length;
			}
		}
		return isNumber;
	}

	public boolean hasMorePairs() {
		return this.current < this.maxCount;
	}

	public String getNextValueAsString() {
		if (this.values[this.current] == null) return null;
		else return new String(this.values[this.current]);
	}

	/**
	 * return value as PdfObject or null
	 * 
	 * @return
	 */
	public PdfObject getNextValueAsDictionary() {
		return this.objs[this.current];
	}
}