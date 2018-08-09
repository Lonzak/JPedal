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
 * Vector_Shape.java
 * ---------------
 */
package org.jpedal.utils.repositories;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.jpedal.io.PathSerializer;

/**
 * Provides the functionality/convenience of a Vector for ints
 * 
 * Much faster because not synchronized and no cast Does not double in size each time
 */
public class Vector_Shape implements Serializable {

	private static final long serialVersionUID = 3229698525407301496L;
	// how much we resize each time - will be doubled up to 160
	int increment_size = 1000;
	protected int current_item = 0;

	// current max size
	int max_size = 250;

	// holds the data
	private Area[] items = new Area[this.max_size];

	// //////////////////////////////////

	// default size
	public Vector_Shape() {
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
	public Vector_Shape(int number) {
		this.max_size = number;
		this.items = new Area[this.max_size];
	}

	/**
	 * extract underlying data
	 */
	final public Area[] get() {
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
			this.items[this.current_item - 1] = new Area();
		}
		else this.items[0] = new Area();
		// reduce counter
		this.current_item--;
	}

	// //////////////////////////////////
	/**
	 * does nothing
	 */
	static final public boolean contains(Shape value) {
		return false;
	}

	// /////////////////////////////////
	/**
	 * replace underlying data
	 */
	final public void set(Area[] new_items) {
		this.items = new_items;
	}

	// /////////////////////////////////
	/**
	 * clear the array
	 */
	final public void clear() {
		this.checkPoint = -1;
		// items = null;
		// holds the data
		// items = new Area[max_size];
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
	 * remove element at
	 */
	final public Area elementAt(int id) {
		if (id >= this.max_size) return null;
		else return this.items[id];
	}

	// /////////////////////////////////
	/**
	 * add an item
	 */
	final public void addElement(Area value) {
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
	final public void setElementAt(Area new_name, int id) {
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
			Area[] temp = this.items;
			this.items = new Area[this.max_size];
			System.arraycopy(temp, 0, this.items, 0, old_size);

			// increase size increase for next time
			this.increment_size = incrementSize(this.increment_size);
		}
	}

	/**
	 * writes out the shapes in this collection to the ByteArrayOutputStream
	 * 
	 * NOT PART OF API and subject to change (DO NOT USE)
	 * 
	 * @param bos
	 *            - the ByteArrayOutputStream to write out to
	 * @throws IOException
	 */
	public void writeToStream(ByteArrayOutputStream bos) throws IOException {

		ObjectOutput os = new ObjectOutputStream(bos);

		/** size of array as first item */
		os.writeObject(this.max_size);

		/** iterate through the array, and write out each Area individualy */
		for (int i = 0; i < this.max_size; i++) {
			Area nextObj = this.items[i];

			if (nextObj == null) os.writeObject(null);
			else {
				PathIterator pathIterator = nextObj.getPathIterator(new AffineTransform());
				PathSerializer.serializePath(os, pathIterator);
			}
		}
	}

	/**
	 * restore the shapes from the input stream into this collections
	 * 
	 * NOT PART OF API and subject to change (DO NOT USE)
	 * 
	 * @param bis
	 *            - ByteArrayInputStream to read from
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void restoreFromStream(ByteArrayInputStream bis) throws IOException, ClassNotFoundException {
		ObjectInput os = new ObjectInputStream(bis);

		/** the number of elements in this collection */
		int size = (Integer) os.readObject();

		this.max_size = size;

		this.items = new Area[size];

		/**
		 * iterate through each item in the stream and store each object in the collection
		 */
		for (int i = 0; i < size; i++) {
			GeneralPath path = PathSerializer.deserializePath(os);

			if (path == null) this.items[i] = null;
			else this.items[i] = new Area(path);
		}
	}

	public void trim() {

		Area[] newItems = new Area[this.current_item];

		System.arraycopy(this.items, 0, newItems, 0, this.current_item);

		this.items = newItems;
		this.max_size = this.current_item;
	}

	/** reset pointer used in add to remove items above */
	public void setSize(int currentItem) {
		this.current_item = currentItem;
	}
}
