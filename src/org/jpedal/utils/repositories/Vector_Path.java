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
 * Vector_Path.java
 * ---------------
 */
package org.jpedal.utils.repositories;

import java.awt.geom.GeneralPath;
import java.io.Serializable;

/**
 * Provides the functionality/convenience of a Vector for ints
 * 
 * Much faster because not synchronized and no cast Does not double in size each time
 */
public class Vector_Path implements Serializable {

	private static final long serialVersionUID = 6175490286103945024L;
	// how much we resize each time - will be doubled up to 160
	int increment_size = 1000;
	protected int current_item = 0;

	// current max size
	int max_size = 250;

	// holds the data
	private GeneralPath[] items = new GeneralPath[this.max_size];

	// //////////////////////////////////

	// default size
	public Vector_Path() {
	}

	protected static int incrementSize(int increment_size) {

		if (increment_size < 8000) increment_size = increment_size * 4;
		else
			if (increment_size < 16000) increment_size = increment_size * 2;
			else increment_size = increment_size + 2000;
		return increment_size;
	}

	// set size
	public Vector_Path(int number) {
		this.max_size = number;
		this.items = new GeneralPath[this.max_size];
	}

	/**
	 * extract underlying data
	 */
	final public GeneralPath[] get() {
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
			this.items[this.current_item - 1] = new GeneralPath();
		}
		else this.items[0] = new GeneralPath();
		// reduce counter
		this.current_item--;
	}

	// //////////////////////////////////
	/**
	 * does nothing
	 * 
	 * static final public boolean contains( Shape value ) { return false; }/
	 **/
	// /////////////////////////////////
	/**
	 * clear the array
	 */
	final public void clear() {
		// items = null;
		// holds the data
		// items = new GeneralPath[max_size];
		if (this.current_item > 0) {
			for (int i = 0; i < this.current_item; i++)
				this.items[i] = null;
		}
		else {
			for (int i = 0; i < this.max_size; i++)
				this.items[i] = null;
		}
		this.current_item = 0;
	}

	// /////////////////////////////////
	/**
	 * replace underlying data
	 */
	final public void set(GeneralPath[] new_items) {
		this.items = new_items;
	}

	// /////////////////////////////////
	/**
	 * remove element at
	 */
	final public GeneralPath elementAt(int id) {
		if (id >= this.max_size) return null;
		else return this.items[id];
	}

	// /////////////////////////////////
	/**
	 * add an item
	 */
	final public void addElement(GeneralPath value) {
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
	final public void setElementAt(GeneralPath new_name, int id) {
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
			GeneralPath[] temp = this.items;
			this.items = new GeneralPath[this.max_size];
			System.arraycopy(temp, 0, this.items, 0, old_size);

			// increase size increase for next time
			this.increment_size = incrementSize(this.increment_size);
		}
	}

	/**
	 * sets the current item
	 * 
	 * NOT PART OF API and subject to change (DO NOT USE)
	 * 
	 * @param current_item
	 */
	public void setCurrent_item(int current_item) {
		this.current_item = current_item;
	}

	public void trim() {

		GeneralPath[] newItems = new GeneralPath[this.current_item];

		System.arraycopy(this.items, 0, newItems, 0, this.current_item);

		this.items = newItems;
		this.max_size = this.current_item;
	}

	/** reset pointer used in add to remove items above */
	public void setSize(int currentItem) {
		this.current_item = currentItem;
	}
}
