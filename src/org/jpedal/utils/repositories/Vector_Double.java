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
 * Vector_Double.java
 * ---------------
 */
package org.jpedal.utils.repositories;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Provides the functionality/convenience of a Vector for Doubles
 * 
 * Much faster because not synchronized and no cast Does not double in size each time
 */
public class Vector_Double implements Serializable {

	private static final long serialVersionUID = 643640502419161703L;
	// how much we resize each time - will be doubled up to 160
	int increment_size = 1000;
	protected int current_item = 0;

	// current max size
	int max_size = 250;

	// holds the data
	private double[] items = new double[this.max_size];

	// default size
	public Vector_Double() {
	}

	private int checkPoint = -1;

	/**
	 * used to store end of PDF components
	 */
	public void resetToCheckpoint() {

		if (this.checkPoint != -1) this.current_item = this.checkPoint;

		this.checkPoint = -1;
	}

	/**
	 * used to rollback array to point
	 */
	public void setCheckpoint() {
		if (this.checkPoint == -1) this.checkPoint = this.current_item;
	}

	protected static int incrementSize(int increment_size) {

		if (increment_size < 8000) increment_size = increment_size * 4;
		else
			if (increment_size < 16000) increment_size = increment_size * 2;
			else increment_size = increment_size + 2000;
		return increment_size;
	}

	// set size
	public Vector_Double(int number) {
		this.max_size = number;
		this.items = new double[this.max_size];
	}

	/**
	 * extract underlying data
	 */
	final public double[] get() {
		return this.items;
	}

	// /////////////////////////////////
	/**
	 * remove element at
	 */
	final public void removeElementAt(int id) {
		if (id >= 0) {
			// copy all items back one to over-write
			System.arraycopy(this.items, id + 1, this.items, id, this.current_item - 1 - id);

			// flush last item
			this.items[this.current_item - 1] = 0;
		}
		else this.items[0] = 0;
		// reduce counter
		this.current_item--;
	}

	// //////////////////////////////////
	/**
	 * see if value present
	 */
	final public boolean contains(int value) {
		boolean flag = false;
		for (int i = 0; i < this.current_item; i++) {
			if (this.items[i] == value) {
				i = this.current_item + 1;
				flag = true;
			}
		}
		return flag;
	}

	// /////////////////////////////////
	/**
	 * add an item
	 */
	final public void addElement(double value) {
		checkSize(this.current_item);
		this.items[this.current_item] = value;
		this.current_item++;
	}

	// /////////////////////////////////
	/**
	 * replace underlying data
	 */
	final public void set(double[] new_items) {
		this.items = new_items;
	}

	// /////////////////////////////////
	/**
	 * remove element at
	 */
	final public double elementAt(int id) {
		if (id >= this.max_size) return 0d;
		else return this.items[id];
	}

	// /////////////////////////////////
	/**
	 * clear the array
	 */
	final public void clear() {
		this.checkPoint = -1;
		// items = null;
		// holds the data
		// items = new double[max_size];
		if (this.current_item > 0) {
			for (int i = 0; i < this.current_item; i++)
				this.items[i] = 0d;
		}
		else {
			for (int i = 0; i < this.max_size; i++)
				this.items[i] = 0d;
		}
		this.current_item = 0;
	}

	// ///////////////////////////////////
	/**
	 * pull item from top as in LIFO stack
	 */
	final public double pull() {

		if (this.current_item > 0) this.current_item--;

		return (this.items[this.current_item]);
	}

	// ///////////////////////////////////
	/**
	 * put item at top as in LIFO stack
	 */
	final public void push(double value) {

		checkSize(this.current_item);
		this.items[this.current_item] = value;

		this.current_item++;
	}

	// /////////////////////////////////
	/**
	 * return the size
	 */
	final public int size() {
		return this.current_item + 1;
	}

	// /////////////////////////////////
	/**
	 * set an element
	 */
	final public void setElementAt(double new_name, int id) {
		if (id >= this.max_size) checkSize(id);

		this.items[id] = new_name;
	}

	// //////////////////////////////////
	/**
	 * check the size of the array and increase if needed
	 */
	final private void checkSize(int i) {
		if (i >= this.max_size) {
			int old_size = this.max_size;
			this.max_size = this.max_size + this.increment_size;

			// allow for it not creating space
			if (this.max_size <= i) this.max_size = i + this.increment_size + 2;
			double[] temp = this.items;
			this.items = new double[this.max_size];
			System.arraycopy(temp, 0, this.items, 0, old_size);

			// increase size increase for next time
			this.increment_size = incrementSize(this.increment_size);
		}
	}

	public void trim() {

		double[] newItems = new double[this.current_item];

		System.arraycopy(this.items, 0, newItems, 0, this.current_item);

		this.items = newItems;
		this.max_size = this.current_item;
	}

	/** reset pointer used in add to remove items above */
	public void setSize(int currentItem) {
		this.current_item = currentItem;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Vector_Double [current_item=");
		builder.append(this.current_item);
		builder.append(", ");
		if (this.items != null && this.size()>0) {
			builder.append("items=");
			builder.append("[");
			for (int i = 0; i < this.size(); i++) {
				builder.append(this.items[i]);
				builder.append(", ");
			}
			builder.append("]]");
		}
		return builder.toString();
	}
}