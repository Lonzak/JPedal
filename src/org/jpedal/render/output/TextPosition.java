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
 * TextPosition.java
 * ---------------
 */
package org.jpedal.render.output;

public class TextPosition {

	float[] rawAffine;
	double[] coords;

	public TextPosition(double[] coords, float[] rawAffine) {
		this.coords = coords;
		this.rawAffine = rawAffine;
	}

	public double[] getCoords() {
		return this.coords;
	}

	public float[] getRawAffine() {
		return new float[] { this.rawAffine[0], this.rawAffine[1], this.rawAffine[2], this.rawAffine[3], this.rawAffine[4], this.rawAffine[5] };
	}

	public float[][] getTrm() {
		return new float[][] { { this.rawAffine[0], this.rawAffine[1], 0 }, { this.rawAffine[2], this.rawAffine[3], 0 },
				{ this.rawAffine[4], this.rawAffine[5], 1 } };
	}

}
