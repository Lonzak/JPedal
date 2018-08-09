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
 * MarginPaper.java
 * ---------------
 */

package org.jpedal.examples.viewer.paper;

import java.awt.print.Paper;

/**
 * Created by IntelliJ IDEA. User: Sam Date: 02-Jul-2010 Time: 16:41:28 To change this template use File | Settings | File Templates.
 */
public class MarginPaper extends Paper {
	double minX = 0, minY = 0, maxRX = 0, maxBY = 0;

	public void setMinImageableArea(double x, double y, double w, double h) {
		this.minX = x;
		this.minY = y;
		this.maxRX = x + w;
		this.maxBY = y + h;
		super.setImageableArea(this.minX, this.minY, this.maxRX, this.maxBY);
	}

	@Override
	public void setImageableArea(double x, double y, double w, double h) {

		if (x < this.minX) x = this.minX;
		if (y < this.minY) y = this.minY;
		if (x + w > this.maxRX) w = this.maxRX - x;
		if (y + h > this.maxBY) h = this.maxBY - y;

		super.setImageableArea(x, y, w, h);
	}

	public double getMinX() {
		return this.minX;
	}

	public double getMinY() {
		return this.minY;
	}

	public double getMaxRX() {
		return this.maxRX;
	}

	public double getMaxBY() {
		return this.maxBY;
	}
}
