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
 * XFAUtils.java
 * ---------------
 */
package com.idrsolutions.pdf.acroforms.xfa;

/**
 * Class provides utils for handling xfa forms
 */
public class XFAUtils {

	/**
	 * Convert the given unit length to a millipoints value
	 * 
	 * @param str input unit value
	 * @return double millipoints
	 */
	public static double convertToMilliPoints(String str) {
		str = str.toLowerCase();
		double retValue = 0;
		retValue = Double.parseDouble(str.substring(0, str.length() - 2));
		if (str != null) {
			if (str.contains("pt")) {
				retValue *= 1000;
			}
			else
				if (str.contains("mm")) {
					retValue *= 2834.64567;
				}
				else
					if (str.contains("cm")) {
						retValue *= 28346.4567;
					}
					else
						if (str.contains("in")) {
							retValue *= 72000;
						}
						else
							if (str.contains("px")) {
								retValue *= retValue * 1000 * (72 / 96); // assumption taken as 96 dpi system
							}
							else {
								System.err.println("xfa value not found for " + str);
							}
		}
		return retValue;
	}

	/**
	 * Convert the given unit length to a 96dpi pixel value
	 * 
	 * @param str input unit value
	 * @return double pixels
	 */
	public static double convertToPixels96(String str) {
		str = str.toLowerCase();
		double retValue = 0;
		retValue = Double.parseDouble(str.substring(0, str.length() - 2));
		if (str != null) {
			if (str.contains("pt")) {
				retValue *= 1.333333333;
			}
			else
				if (str.contains("mm")) {
					retValue *= 3.779527559;
				}
				else
					if (str.contains("cm")) {
						retValue *= 37.795275591;
					}
					else
						if (str.contains("in")) {
							retValue *= 96;
						}
						else
							if (str.contains("px")) {
								retValue = retValue * 1; // assumption taken as 96 dpi system
							}
							else {
								System.err.println("xfa value not found for " + str);
							}
		}
		return retValue;
	}

}
