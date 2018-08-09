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
 * TextLines.java
 * ---------------
 */
package org.jpedal.text;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;

import org.jpedal.objects.PdfData;
import org.jpedal.utils.repositories.Vector_Rectangle;

public class TextLines {

	/** stores area of arrays in which text should be highlighted */
	private Map lineAreas = new HashMap();
	private Map lineWritingMode = new HashMap();

	/** Highlight Areas stored here */
	public Map areas = new HashMap();

	/**
	 * Highlights a section of lines that form a paragraph and returns the area that encloses all highlight areas
	 * 
	 * @return Rectangle that contains all areas highlighted
	 */
	public Rectangle setFoundParagraph(int x, int y, int page) {
		Rectangle[] lines = this.getLineAreas(page);
		if (lines != null) {
			Rectangle point = new Rectangle(x, y, 1, 1);
			Rectangle current = new Rectangle(0, 0, 0, 0);
			boolean lineFound = false;
			int selectedLine = 0;

			for (int i = 0; i != lines.length; i++) {
				if (lines[i].intersects(point)) {
					selectedLine = i;
					lineFound = true;
					break;
				}
			}

			if (lineFound) {
				double left = lines[selectedLine].x;
				double cx = lines[selectedLine].getCenterX();
				double right = lines[selectedLine].x + lines[selectedLine].width;
				double cy = lines[selectedLine].getCenterY();
				int h = lines[selectedLine].height;

				current.x = lines[selectedLine].x;
				current.y = lines[selectedLine].y;
				current.width = lines[selectedLine].width;
				current.height = lines[selectedLine].height;

				boolean foundTop = true;
				boolean foundBottom = true;
				Vector_Rectangle selected = new Vector_Rectangle(0);
				selected.addElement(lines[selectedLine]);

				while (foundTop) {
					foundTop = false;
					for (int i = 0; i != lines.length; i++) {
						if (lines[i].contains(left, cy + h) || lines[i].contains(cx, cy + h) || lines[i].contains(right, cy + h)) {
							selected.addElement(lines[i]);
							foundTop = true;
							cy = lines[i].getCenterY();
							h = lines[i].height;

							if (current.x > lines[i].x) {
								current.width = (current.x + current.width) - lines[i].x;
								current.x = lines[i].x;
							}
							if ((current.x + current.width) < (lines[i].x + lines[i].width)) current.width = (lines[i].x + lines[i].width)
									- current.x;
							if (current.y > lines[i].y) {
								current.height = (current.y + current.height) - lines[i].y;
								current.y = lines[i].y;
							}
							if ((current.y + current.height) < (lines[i].y + lines[i].height)) {
								current.height = (lines[i].y + lines[i].height) - current.y;
							}

							break;
						}
					}
				}

				// Return to selected item else we have duplicate highlights
				left = lines[selectedLine].x;
				cx = lines[selectedLine].getCenterX();
				right = lines[selectedLine].x + lines[selectedLine].width;
				cy = lines[selectedLine].getCenterY();
				h = lines[selectedLine].height;

				while (foundBottom) {
					foundBottom = false;
					for (int i = 0; i != lines.length; i++) {
						if (lines[i].contains(left, cy - h) || lines[i].contains(cx, cy - h) || lines[i].contains(right, cy - h)) {
							selected.addElement(lines[i]);
							foundBottom = true;
							cy = lines[i].getCenterY();
							h = lines[i].height;

							if (current.x > lines[i].x) {
								current.width = (current.x + current.width) - lines[i].x;
								current.x = lines[i].x;
							}
							if ((current.x + current.width) < (lines[i].x + lines[i].width)) current.width = (lines[i].x + lines[i].width)
									- current.x;
							if (current.y > lines[i].y) {
								current.height = (current.y + current.height) - lines[i].y;
								current.y = lines[i].y;
							}
							if ((current.y + current.height) < (lines[i].y + lines[i].height)) {
								current.height = (lines[i].y + lines[i].height) - current.y;
							}

							break;
						}
					}
				}
				selected.trim();
				addHighlights(selected.get(), true, page);
				return current;
			}
			return null;
		}
		return null;
	}

	public void addToLineAreas(Rectangle area, int writingMode, int page) {
		boolean addNew = true;

		if (this.lineAreas == null) { // If null, create array

			// Set area
			this.lineAreas = new HashMap();
			this.lineAreas.put(page, new Rectangle[] { area });

			// Set writing direction
			this.lineWritingMode = new HashMap();
			this.lineWritingMode.put(page, new int[] { writingMode });

		}
		else {
			Rectangle[] lastAreas = ((Rectangle[]) this.lineAreas.get(page));
			int[] lastWritingMode = ((int[]) this.lineWritingMode.get(page));

			// Check for objects close to or intersecting each other
			if (area != null) { // Ensure actual area is selected
				if (lastAreas != null) {
					for (int i = 0; i != lastAreas.length; i++) {
						int lwm = lastWritingMode[i];
						int cwm = writingMode;
						int cx = area.x;
						int cy = area.y;
						int cw = area.width;
						int ch = area.height;
						// int cm = cy+(ch/2);

						int lx = lastAreas[i].x;
						int ly = lastAreas[i].y;
						int lw = lastAreas[i].width;
						int lh = lastAreas[i].height;
						// int lm = ly+(lh/2);

						int currentBaseLine;
						int lastBaseLine;
						float heightMod = 5f;
						float widthMod = 1.1f;

						switch (writingMode) {
							case PdfData.HORIZONTAL_LEFT_TO_RIGHT:

								if (lwm == cwm && ((ly > (cy - (ch / heightMod))) && (ly < (cy + (ch / heightMod)))) && // Ensure this is actually the
																														// same line and are about the
																														// same size
										(((lh < ch + (ch / heightMod) && lh > ch - (ch / heightMod))) && // Check text is the same height
										(((lx > (cx + cw - (ch * widthMod))) && (lx < (cx + cw + (ch * widthMod)))) || // Check for object at end of
																														// this object
												((lx + lw > (cx - (ch * widthMod))) && (lx + lw < (cx + (ch * widthMod)))) || // Check for object at
																																// start of this
																																// object
										lastAreas[i].intersects(area)))// Check to see if it intersects at all
								) {
									addNew = false;

									// No need to reset the writing mode as already set
									lastAreas[i] = mergePartLines(lastAreas[i], area);
								}
								break;
							case PdfData.HORIZONTAL_RIGHT_TO_LEFT:

								lx = lastAreas[i].x;
								ly = lastAreas[i].y;
								lw = lastAreas[i].width;
								lh = lastAreas[i].height;
								cx = area.x;
								cy = area.y;
								cw = area.width;
								ch = area.height;

								if (lwm == cwm && ((ly > (cy - 5)) && (ly < (cy + 5)) && lh <= (ch + (ch / 5)) && lh >= (ch - (ch / 5))) && // Ensure
																																			// this is
																																			// actually
																																			// the
																																			// same
																																			// line
																																			// and are
																																			// about
																																			// the
																																			// same
																																			// size
										(((lx > (cx + cw - (ch * 0.6))) && (lx < (cx + cw + (ch * 0.6)))) || // Check for object at end of this object
												((lx + lw > (cx - (ch * 0.6))) && (lx + lw < (cx + (ch * 0.6)))) || // Check for object at start of
																													// this object
										lastAreas[i].intersects(area))// Check to see if it intersects at all
								) {
									addNew = false;

									// No need to reset the writing mode as already set
									lastAreas[i] = mergePartLines(lastAreas[i], area);
								}
								break;
							case PdfData.VERTICAL_TOP_TO_BOTTOM:

								lx = lastAreas[i].y;
								ly = lastAreas[i].x;
								lw = lastAreas[i].height;
								lh = lastAreas[i].width;
								cx = area.y;
								cy = area.x;
								cw = area.height;
								ch = area.width;

								if (lwm == cwm && ((ly > (cy - 5)) && (ly < (cy + 5)) && lh <= (ch + (ch / 5)) && lh >= (ch - (ch / 5))) && // Ensure
																																			// this is
																																			// actually
																																			// the
																																			// same
																																			// line
																																			// and are
																																			// about
																																			// the
																																			// same
																																			// size
										(((lx > (cx + cw - (ch * 0.6))) && (lx < (cx + cw + (ch * 0.6)))) || // Check for object at end of this object
												((lx + lw > (cx - (ch * 0.6))) && (lx + lw < (cx + (ch * 0.6)))) || // Check for object at start of
																													// this object
										lastAreas[i].intersects(area))// Check to see if it intersects at all
								) {
									addNew = false;

									// No need to reset the writing mode as already set
									lastAreas[i] = mergePartLines(lastAreas[i], area);
								}

								break;

							case PdfData.VERTICAL_BOTTOM_TO_TOP:

								// Calculate the coord value at the bottom of the text
								currentBaseLine = cx + cw;
								lastBaseLine = lx + lw;

								if (lwm == cwm // Check the current writing mode
										&& (currentBaseLine >= (lastBaseLine - (lw / 3))) && (currentBaseLine <= (lastBaseLine + (lw / 3))) // Check
																																			// is same
																																			// line
										&& // Only check left or right if the same line is shared
										(( // Check for text on either side
										((ly + (lh + (lw * 0.6)) > cy) && (ly + (lh - (lw * 0.6)) < cy))// Check for text to left of current area
										|| ((ly + (lw * 0.6) > (cy + ch)) && (ly - (lw * 0.6) < (cy + ch)))// Check for text to right of current area
										) || area.intersects(lastAreas[i]))) {
									addNew = false;

									// No need to reset the writing mode as already set
									lastAreas[i] = mergePartLines(lastAreas[i], area);
								}

								break;

						}

					}
				}
				else {
					addNew = true;
				}

				// If no object near enough to merge, start a new area
				if (addNew) {

					Rectangle[] lineAreas;
					int[] lineWritingMode;

					if (lastAreas != null) {
						lineAreas = new Rectangle[lastAreas.length + 1];
						for (int i = 0; i != lastAreas.length; i++) {
							lineAreas[i] = lastAreas[i];
						}
						lineAreas[lineAreas.length - 1] = area;

						lineWritingMode = new int[lastWritingMode.length + 1];
						for (int i = 0; i != lastWritingMode.length; i++) {
							lineWritingMode[i] = lastWritingMode[i];
						}
						lineWritingMode[lineWritingMode.length - 1] = writingMode;

					}
					else {
						lineAreas = new Rectangle[1];
						lineAreas[0] = area;

						lineWritingMode = new int[1];
						lineWritingMode[0] = writingMode;
					}

					// Set area
					this.lineAreas.put(page, lineAreas);

					// Set writing direction
					this.lineWritingMode.put(page, lineWritingMode);
				}

			}
		}
	}

	/**
	 * remove zone on page for text areas if present
	 */
	public void removeFoundTextArea(Rectangle rectArea, int page) {

		// clearHighlights();
		if (rectArea == null || this.areas == null) return;

		Integer p = page;
		Rectangle[] areas = ((Rectangle[]) this.areas.get(p));
		if (areas != null) {
			int size = areas.length;
			for (int i = 0; i < size; i++) {
				if (areas[i] != null
						&& (areas[i].contains(rectArea) || (areas[i].x == rectArea.x && areas[i].y == rectArea.y && areas[i].width == rectArea.width && areas[i].height == rectArea.height))) {
					areas[i] = null;
					i = size;
				}
			}
			this.areas.put(p, areas);
		}
		// currentManager.addDirtyRegion(this,0,0,x_size,y_size);
	}

	/**
	 * remove highlight zones on page for text areas on single pages null value will totally reset
	 */
	public void removeFoundTextAreas(Rectangle[] rectArea, int page) {

		// clearHighlights();

		if (rectArea == null) {
			this.areas = null;
		}
		else {
			for (Rectangle aRectArea : rectArea) {
				removeFoundTextArea(aRectArea, page);
			}
			boolean allNull = true;
			Integer p = page;
			Rectangle[] areas = ((Rectangle[]) this.areas.get(p));
			if (areas != null) {
				for (int ii = 0; ii < areas.length; ii++) {
					if (areas[ii] != null) {
						allNull = false;
						ii = areas.length;
					}
				}
				if (allNull) {
					areas = null;
					this.areas.put(p, areas);
				}
			}
		}
	}

	/**
	 * Clear all highlights that are being displayed
	 */
	public void clearHighlights() {
		this.areas = null;
		// PdfHighlights.clearAllHighlights(this);
	}

	/**
	 * Method to highlight text on page.
	 * 
	 * If areaSelect = true then the Rectangle array will be highlgihted on screen unmodified. areaSelect should be true if being when used with
	 * values returned from the search as these areas are already corrected and modified for display.
	 * 
	 * If areaSelect = false then all lines between the top left point and bottom right point will be selected including two partial lines the top
	 * line starting from the top left point of the rectangle and the bottom line ending at the bottom right point of the rectangle.
	 * 
	 * @param highlights
	 *            :: The Array of rectangles that you wish to have highlighted
	 * @param areaSelect
	 *            :: The flag that will either select text as line between points if false or characters within an area if true.
	 */
	public void addHighlights(Rectangle[] highlights, boolean areaSelect, int page) {

		if (highlights != null) { // If null do nothing to clear use the clear method

			if (!areaSelect) {
				// Ensure highlighting takes place
				// boolean nothingToHighlight = false;

				for (int j = 0; j != highlights.length; j++) {
					if (highlights[j] != null) {

						// Ensure that the points are adjusted so that they are within line area if that is sent as rectangle
						Point startPoint = new Point(highlights[j].x + 1, highlights[j].y + 1);
						Point endPoint = new Point(highlights[j].x + highlights[j].width - 1, highlights[j].y + highlights[j].height - 1);
						// both null flushes areas

						if (this.areas == null) {
							// this.areas=new Rectangle[1];
							// This is the first highlight, ensure it highlights something
							this.areas = new HashMap();

						}

						Rectangle[] lines = this.getLineAreas(page);
						int[] writingMode = this.getLineWritingMode(page);

						int start = -1;
						int finish = -1;
						boolean backward = false;
						// Find the first selected line and the last selected line.
						if (lines != null) {
							for (int i = 0; i != lines.length; i++) {

								if (lines[i].contains(startPoint)) start = i;

								if (lines[i].contains(endPoint)) finish = i;

								if (start != -1 && finish != -1) {
									break;
								}

							}

							if (start > finish) {
								int temp = start;
								start = finish;
								finish = temp;
								backward = true;
							}

							if (start == finish) {
								if (startPoint.x > endPoint.x) {
									Point temp = startPoint;
									startPoint = endPoint;
									endPoint = temp;
								}
							}

							if (start != -1 && finish != -1) {
								// Fill in all the lines between
								Integer p = page;
								Rectangle[] areas = new Rectangle[finish - start + 1];

								System.arraycopy(lines, start + 0, areas, 0, finish - start + 1);

								if (areas.length > 0) {
									int top = 0;
									int bottom = areas.length - 1;

									if (areas[top] != null && areas[bottom] != null) {

										switch (writingMode[start]) {
											case PdfData.HORIZONTAL_LEFT_TO_RIGHT:
												// if going backwards
												if (backward) {
													if ((endPoint.x - 15) <= areas[top].x) {
														// Do nothing to areas as we want to pick up the start of a line
													}
													else {
														areas[top].width = areas[top].width - (endPoint.x - areas[top].x);
														areas[top].x = endPoint.x;
													}

												}
												else {
													if ((startPoint.x - 15) <= areas[top].x) {
														// Do nothing to areas as we want to pick up the start of a line
													}
													else {
														areas[top].width = areas[top].width - (startPoint.x - areas[top].x);
														areas[top].x = startPoint.x;
													}

												}
												break;
											case PdfData.HORIZONTAL_RIGHT_TO_LEFT:
												break;
											case PdfData.VERTICAL_TOP_TO_BOTTOM:
												if (backward) {
													if ((endPoint.y - 15) <= areas[top].y) {
														// Do nothing to areas as we want to pick up the start of a line
													}
													else {
														areas[top].height = areas[top].height - (endPoint.y - areas[top].y);
														areas[top].y = endPoint.y;
													}

												}
												else {
													if ((startPoint.y - 15) <= areas[top].y) {
														// Do nothing to areas as we want to pick up the start of a line
													}
													else {
														areas[top].height = areas[top].height - (startPoint.y - areas[top].y);
														areas[top].y = startPoint.y;
													}

												}
												break;
											case PdfData.VERTICAL_BOTTOM_TO_TOP:
												if (backward) {
													if ((endPoint.y - 15) <= areas[top].y) {
														// Do nothing to areas as we want to pick up the start of a line
													}
													else {
														areas[top].height = areas[top].height - (endPoint.y - areas[top].y);
														areas[top].y = endPoint.y;
													}

												}
												else {
													if ((startPoint.y - 15) <= areas[top].y) {
														// Do nothing to areas as we want to pick up the start of a line
													}
													else {
														areas[top].height = areas[top].height - (startPoint.y - areas[top].y);
														areas[top].y = startPoint.y;
													}

												}
												break;
										}

										switch (writingMode[finish]) {
											case PdfData.HORIZONTAL_LEFT_TO_RIGHT:
												// if going backwards
												if (backward) {
													if ((startPoint.x + 15) >= areas[bottom].x + areas[bottom].width) {
														// Do nothing to areas as we want to pick up the end of a line
													}
													else {
														areas[bottom].width = startPoint.x - areas[bottom].x;
													}

												}
												else {
													if ((endPoint.x + 15) >= areas[bottom].x + areas[bottom].width) {
														// Do nothing to areas as we want to pick up the end of a line
													}
													else areas[bottom].width = endPoint.x - areas[bottom].x;
												}
												break;
											case PdfData.HORIZONTAL_RIGHT_TO_LEFT:
												break;
											case PdfData.VERTICAL_TOP_TO_BOTTOM:
												// if going backwards
												if (backward) {
													if ((startPoint.y + 15) >= areas[bottom].y + areas[bottom].height) {
														// Do nothing to areas as we want to pick up the end of a line
													}
													else {
														areas[bottom].height = startPoint.y - areas[bottom].y;
													}

												}
												else {
													if ((endPoint.y + 15) >= areas[bottom].y + areas[bottom].height) {
														// Do nothing to areas as we want to pick up the end of a line
													}
													else areas[bottom].height = endPoint.y - areas[bottom].y;
												}
												break;
											case PdfData.VERTICAL_BOTTOM_TO_TOP:
												// if going backwards
												if (backward) {
													if ((startPoint.y + 15) >= areas[bottom].y + areas[bottom].height) {
														// Do nothing to areas as we want to pick up the end of a line
													}
													else {
														areas[bottom].height = startPoint.y - areas[bottom].y;
													}

												}
												else {
													if ((endPoint.y + 15) >= areas[bottom].y + areas[bottom].height) {
														// Do nothing to areas as we want to pick up the end of a line
													}
													else areas[bottom].height = endPoint.y - areas[bottom].y;
												}
												break;
										}
									}
								}
								this.areas.put(p, areas);
							}
							// else {
							// //This is the first highlight and nothing was selected
							// if(nothingToHighlight){
							// System.out.println("Area == null");
							// //Prevent text extraction on nothing
							// this.areas = null;
							// }
							// }
						}
					}
				}
			}
			else {
				// if inset add in difference transparently
				for (int v = 0; v != highlights.length; v++) {
					if (highlights[v] != null) {
						if (highlights[v].width < 0) {
							highlights[v].width = -highlights[v].width;
							highlights[v].x -= highlights[v].width;
						}

						if (highlights[v].height < 0) {
							highlights[v].height = -highlights[v].height;
							highlights[v].y -= highlights[v].height;
						}

						if (this.areas != null) {
							Integer p = page;
							Rectangle[] areas = ((Rectangle[]) this.areas.get(p));
							if (areas != null) {
								boolean matchFound = false;

								// see if already added
								int size = areas.length;
								for (int i = 0; i < size; i++) {
									if (areas[i] != null) {
										// If area has been added before please ignore
										if (areas[i] != null
												&& (areas[i].x == highlights[v].x && areas[i].y == highlights[v].y
														&& areas[i].width == highlights[v].width && areas[i].height == highlights[v].height)) {
											matchFound = true;
											i = size;
										}
									}
								}

								if (!matchFound) {
									int newSize = areas.length + 1;
									Rectangle[] newAreas = new Rectangle[newSize];
									for (int i = 0; i < areas.length; i++) {
										if (areas[i] != null) newAreas[i + 1] = new Rectangle(areas[i].x, areas[i].y, areas[i].width, areas[i].height);
									}
									areas = newAreas;

									areas[0] = highlights[v];
								}
								this.areas.put(p, areas);
							}
							else {
								this.areas.put(p, highlights);
							}
						}
						else {
							this.areas = new HashMap();
							Integer p = page;
							Rectangle[] areas = new Rectangle[1];
							areas[0] = highlights[v];
							this.areas.put(p, areas);
						}
					}
				}
			}
		}
	}

	public Rectangle[] getHighlightedAreas(int page) {

		if (this.areas == null) return null;
		else {
			Integer p = page;
			Rectangle[] areas = ((Rectangle[]) this.areas.get(p));
			if (areas != null) {
				int count = areas.length;

				Rectangle[] returnValue = new Rectangle[count];

				for (int ii = 0; ii < count; ii++) {
					if (areas[ii] == null) returnValue[ii] = null;
					else returnValue[ii] = new Rectangle(areas[ii].x, areas[ii].y, areas[ii].width, areas[ii].height);
				}
				return returnValue;
			}
			else {
				return null;
			}
		}
	}

	public void setLineAreas(Map la) {
		this.lineAreas = la;
	}

	public void setLineWritingMode(Map lineOrientation) {
		this.lineWritingMode = lineOrientation;
	}

	public Rectangle[] getLineAreas(int page) {

		if (this.lineAreas == null) return null;
		else {
			Rectangle[] lineAreas = ((Rectangle[]) this.lineAreas.get(page));

			if (lineAreas == null) return null;

			int count = lineAreas.length;

			Rectangle[] returnValue = new Rectangle[count];

			for (int ii = 0; ii < count; ii++) {
				if (lineAreas[ii] == null) returnValue[ii] = null;
				else returnValue[ii] = new Rectangle(lineAreas[ii].x, lineAreas[ii].y, lineAreas[ii].width, lineAreas[ii].height);
			}

			return returnValue;
		}
	}

	public int[] getLineWritingMode(int page) {

		if (this.lineWritingMode == null) return null;
		else {
			int[] lineWritingMode = ((int[]) this.lineWritingMode.get(page));

			if (lineWritingMode == null) return null;

			int count = lineWritingMode.length;

			int[] returnValue = new int[count];

			System.arraycopy(lineWritingMode, 0, returnValue, 0, count);

			return returnValue;
		}
	}

	private static Rectangle mergePartLines(Rectangle lastArea, Rectangle area) {
		/**
		 * Check coords from both areas and merge them to make a single larger area containing contents of both
		 */
		int x1 = area.x;
		int x2 = area.x + area.width;
		int y1 = area.y;
		int y2 = area.y + area.height;
		int lx1 = lastArea.x;
		int lx2 = lastArea.x + lastArea.width;
		int ly1 = lastArea.y;
		int ly2 = lastArea.y + lastArea.height;

		// Ensure the highest and lowest values are selected
		if (x1 < lx1) area.x = x1;
		else area.x = lx1;

		if (y1 < ly1) area.y = y1;
		else area.y = ly1;

		if (y2 > ly2) area.height = y2 - area.y;
		else area.height = ly2 - area.y;

		if (x2 > lx2) area.width = x2 - area.x;
		else area.width = lx2 - area.x;

		return area;
	}
}
