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
 * DoubleStack.java
 * ---------------
 */
package org.jpedal.utils.repositories;

/**
 * provides a stack holding double values (primarily for type1 font renderer). MUCH faster than Suns own stack
 */
public class DoubleStack implements Stack {

	/** default intial size */
	private int size = 100;

	private int nextSlot = 0;

	private double[] elements = new double[this.size];

	public DoubleStack(int size) {
		this.size = size;
		this.elements = new double[size];
	}

	/**
	 * take double value from stack
	 */
	@Override
	public double pop() {
		if (this.nextSlot > 0) {
			this.nextSlot--;
			return this.elements[this.nextSlot];
		}
		else return 0;
	}

	/**
	 * get top double value from stack
	 */
	@Override
	public double peek() {
		if (this.nextSlot > 0) return this.elements[this.nextSlot - 1];
		else return 0;
	}

	/**
	 * add double value to stack
	 */
	@Override
	public void push(double value) {

		// resize if needed*/
		if (this.nextSlot == this.size) {

			if (this.size < 1000) {
				this.size = this.size * 4;
			}
			else this.size = this.size + this.size;

			double[] temp = this.elements;
			this.elements = new double[this.size];

			System.arraycopy(temp, 0, this.elements, 0, temp.length);

		}

		this.elements[this.nextSlot] = value;

		this.nextSlot++;
	}

}
