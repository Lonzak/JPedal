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
 * SwingMousePageTurn.java
 * ---------------
 */

package org.jpedal.examples.viewer.gui.swing;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.jpedal.Display;
import org.jpedal.PdfDecoder;
import org.jpedal.examples.viewer.Commands;
import org.jpedal.examples.viewer.Values;
import org.jpedal.examples.viewer.gui.GUI;
import org.jpedal.examples.viewer.gui.SwingGUI;
import org.jpedal.external.Options;
import org.jpedal.io.PdfObjectReader;

public class SwingMousePageTurn implements SwingMouseFunctionality {

	private PdfDecoder decode_pdf;
	private SwingGUI currentGUI;
	private Values commonValues;
	private Commands currentCommands;

	private long lastPress;

	/** allow turning page to be drawn */
	private boolean drawingTurnover = false;

	/** show turning page when hovering over corner */
	private boolean previewTurnover = false;

	/** whether page turn is currently animating */
	private boolean pageTurnAnimating = false;

	/** current page being hovered over in pageFlow */
	private int pageFlowCurrentPage;

	/** middle drag panning values */
	private double middleDragStartX, middleDragStartY, xVelocity, yVelocity;
	private Timer middleDragTimer;

	long timeOfLastPageChange;

	boolean altIsDown = false;

	public SwingMousePageTurn(PdfDecoder decode_pdf, SwingGUI currentGUI, Values commonValues, Commands currentCommands) {

		this.decode_pdf = decode_pdf;
		this.currentGUI = currentGUI;
		this.commonValues = commonValues;
		this.currentCommands = currentCommands;

		// SwingMouseSelection sms = new SwingMouseSelection(decode_pdf, commonValues, this);
		// sms.setupMouse();
		// decode_pdf.setMouseMode(PdfDecoder.MOUSE_MODE_TEXT_SELECT);
		//
		// decode_pdf.addExternalHandler(this, Options.SwingMouseHandler);
	}

	public void updateRectangle() {
		// TODO Auto-generated method stub
	}

	@Override
	public void mouseClicked(MouseEvent event) {

		if (this.decode_pdf.getDisplayView() == Display.SINGLE_PAGE && event.getButton() == MouseEvent.BUTTON1
				&& this.decode_pdf.getExternalHandler(Options.UniqueAnnotationHandler) != null) {
			int[] pos = updateXY(event.getX(), event.getY());
			checkLinks(true, this.decode_pdf.getIO(), pos[0], pos[1]);
		}
	}

	@Override
	public void mouseEntered(MouseEvent event) {
		// TODO Auto-generated method stub
	}

	@Override
	public void mouseExited(MouseEvent event) {
		// Do nothing
	}

	@Override
	public void mousePressed(MouseEvent event) {

		// Activate turnover if pressed while preview on
		if (this.previewTurnover && this.currentCommands.getPages().getBoolean(Display.BoolValue.TURNOVER_ON)
				&& this.decode_pdf.getDisplayView() == Display.FACING && event.getButton() == MouseEvent.BUTTON1) {
			this.drawingTurnover = true;
			// set cursor
			this.decode_pdf.setCursor(this.currentGUI.getCursor(SwingGUI.GRABBING_CURSOR));
			this.lastPress = System.currentTimeMillis();
		}

		// Start dragging
		if (event.getButton() == MouseEvent.BUTTON2) {
			this.middleDragStartX = event.getX() - this.decode_pdf.getVisibleRect().getX();
			this.middleDragStartY = event.getY() - this.decode_pdf.getVisibleRect().getY();
			this.decode_pdf.setCursor(this.currentGUI.getCursor(SwingGUI.PAN_CURSOR));

			// set up timer to refresh display
			if (this.middleDragTimer == null) this.middleDragTimer = new Timer(100, new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					Rectangle r = SwingMousePageTurn.this.decode_pdf.getVisibleRect();
					r.translate((int) SwingMousePageTurn.this.xVelocity, (int) SwingMousePageTurn.this.yVelocity);
					if (SwingMousePageTurn.this.xVelocity < -2) {
						if (SwingMousePageTurn.this.yVelocity < -2) SwingMousePageTurn.this.decode_pdf.setCursor(SwingMousePageTurn.this.currentGUI
								.getCursor(SwingGUI.PAN_CURSORTL));
						else
							if (SwingMousePageTurn.this.yVelocity > 2) SwingMousePageTurn.this.decode_pdf
									.setCursor(SwingMousePageTurn.this.currentGUI.getCursor(SwingGUI.PAN_CURSORBL));
							else SwingMousePageTurn.this.decode_pdf.setCursor(SwingMousePageTurn.this.currentGUI.getCursor(SwingGUI.PAN_CURSORL));
					}
					else
						if (SwingMousePageTurn.this.xVelocity > 2) {
							if (SwingMousePageTurn.this.yVelocity < -2) SwingMousePageTurn.this.decode_pdf
									.setCursor(SwingMousePageTurn.this.currentGUI.getCursor(SwingGUI.PAN_CURSORTR));
							else
								if (SwingMousePageTurn.this.yVelocity > 2) SwingMousePageTurn.this.decode_pdf
										.setCursor(SwingMousePageTurn.this.currentGUI.getCursor(SwingGUI.PAN_CURSORBR));
								else SwingMousePageTurn.this.decode_pdf.setCursor(SwingMousePageTurn.this.currentGUI.getCursor(SwingGUI.PAN_CURSORR));
						}
						else {
							if (SwingMousePageTurn.this.yVelocity < -2) SwingMousePageTurn.this.decode_pdf
									.setCursor(SwingMousePageTurn.this.currentGUI.getCursor(SwingGUI.PAN_CURSORT));
							else
								if (SwingMousePageTurn.this.yVelocity > 2) SwingMousePageTurn.this.decode_pdf
										.setCursor(SwingMousePageTurn.this.currentGUI.getCursor(SwingGUI.PAN_CURSORB));
								else SwingMousePageTurn.this.decode_pdf.setCursor(SwingMousePageTurn.this.currentGUI.getCursor(SwingGUI.PAN_CURSOR));
						}
					SwingMousePageTurn.this.decode_pdf.scrollRectToVisible(r);

				}
			});
			this.middleDragTimer.start();
		}
	}

	@Override
	public void mouseReleased(MouseEvent event) {

		// Stop drawing turnover
		if (this.currentCommands.getPages().getBoolean(Display.BoolValue.TURNOVER_ON) && this.decode_pdf.getDisplayView() == Display.FACING) {
			this.drawingTurnover = false;

			boolean dragLeft = this.currentGUI.getDragLeft();
			boolean dragTop = this.currentGUI.getDragTop();

			if (this.lastPress + 200 > System.currentTimeMillis()) {
				if (dragLeft) this.currentCommands.executeCommand(Commands.BACKPAGE, null);
				else this.currentCommands.executeCommand(Commands.FORWARDPAGE, null);
				this.previewTurnover = false;
				this.decode_pdf.setCursor(this.currentGUI.getCursor(SwingGUI.DEFAULT_CURSOR));
			}
			else {
				// Trigger fall
				Point corner = new Point();
				corner.y = this.decode_pdf.getInsetH();
				if (!dragTop) corner.y += ((this.decode_pdf.getPdfPageData().getCropBoxHeight(1) * this.decode_pdf.getScaling()));

				if (dragLeft) corner.x = (int) ((this.decode_pdf.getVisibleRect().getWidth() / 2) - (this.decode_pdf.getPdfPageData()
						.getCropBoxWidth(1) * this.decode_pdf.getScaling()));
				else corner.x = (int) ((this.decode_pdf.getVisibleRect().getWidth() / 2) + (this.decode_pdf.getPdfPageData().getCropBoxWidth(1) * this.decode_pdf
						.getScaling()));

				// MouseMotionListener[] listeners = decode_pdf.getMouseMotionListeners();
				// if (mover==null) {
				// for (int i=0; i< listeners.length; i++) {
				// if (listeners[i] instanceof mouse_mover) {
				// mover = ((mouse_mover)listeners[i]);
				// }
				// }
				// }
				// mover.testFall(corner,event.getPoint(),dragLeft);

				testFall(corner, event.getPoint(), dragLeft);
			}
		}

		// stop middle click panning
		if (event.getButton() == MouseEvent.BUTTON2) {
			this.xVelocity = 0;
			this.yVelocity = 0;
			this.decode_pdf.setCursor(this.currentGUI.getCursor(SwingGUI.DEFAULT_CURSOR));
			this.middleDragTimer.stop();
			this.decode_pdf.repaint();
		}
	}

	@Override
	public void mouseDragged(MouseEvent event) {
		if (SwingUtilities.isLeftMouseButton(event)) {
			this.altIsDown = event.isAltDown();
			// dragged = true;
			// int[] values = updateXY(event);
			// commonValues.m_x2=values[0];
			// commonValues.m_y2=values[1];

			// if(commonValues.isPDF())
			// generateNewCursorBox();

			if (this.decode_pdf.getExternalHandler(Options.UniqueAnnotationHandler) != null) {
				int[] pos = updateXY(event.getX(), event.getY());
				checkLinks(true, this.decode_pdf.getIO(), pos[0], pos[1]);
			}

			// update mouse coords for turnover
			if (this.currentCommands.getPages().getBoolean(Display.BoolValue.TURNOVER_ON) && (this.drawingTurnover || this.previewTurnover)
					&& this.decode_pdf.getDisplayView() == Display.FACING) {
				this.decode_pdf.setCursor(this.currentGUI.getCursor(SwingGUI.GRABBING_CURSOR));

				// update coords
				if (this.currentGUI.getDragLeft()) {
					if (this.currentGUI.getDragTop()) this.decode_pdf.setUserOffsets(event.getX(), event.getY(),
							org.jpedal.external.OffsetOptions.INTERNAL_DRAG_CURSOR_TOP_LEFT);
					else this.decode_pdf.setUserOffsets(event.getX(), event.getY(),
							org.jpedal.external.OffsetOptions.INTERNAL_DRAG_CURSOR_BOTTOM_LEFT);
				}
				else {
					if (this.currentGUI.getDragTop()) this.decode_pdf.setUserOffsets(event.getX(), event.getY(),
							org.jpedal.external.OffsetOptions.INTERNAL_DRAG_CURSOR_TOP_RIGHT);
					else this.decode_pdf.setUserOffsets(event.getX(), event.getY(),
							org.jpedal.external.OffsetOptions.INTERNAL_DRAG_CURSOR_BOTTOM_RIGHT);
				}
			}

		}
		else
			if (SwingUtilities.isMiddleMouseButton(event)) {
				// middle drag - update velocity
				this.xVelocity = ((event.getX() - this.decode_pdf.getVisibleRect().getX()) - this.middleDragStartX) / 4;
				this.yVelocity = ((event.getY() - this.decode_pdf.getVisibleRect().getY()) - this.middleDragStartY) / 4;
			}
	}

	@Override
	public void mouseMoved(MouseEvent event) {

		if (this.decode_pdf.getDisplayView() == Display.FACING && this.currentCommands.getPages().getBoolean(Display.BoolValue.TURNOVER_ON)
				&& ((SwingGUI) this.decode_pdf.getExternalHandler(Options.SwingContainer)).getPageTurnScalingAppropriate()
				&& !this.decode_pdf.getPdfPageData().hasMultipleSizes() && !this.pageTurnAnimating) {
			// show preview turnover

			// get width and height of page
			float pageH = (this.decode_pdf.getPdfPageData().getCropBoxHeight(1) * this.decode_pdf.getScaling()) - 1;
			float pageW = (this.decode_pdf.getPdfPageData().getCropBoxWidth(1) * this.decode_pdf.getScaling()) - 1;

			if ((this.decode_pdf.getPdfPageData().getRotation(1) + this.currentGUI.getRotation()) % 180 == 90) {
				float temp = pageH;
				pageH = pageW + 1;
				pageW = temp;
			}

			final Point corner = new Point();

			// right turnover
			if (this.commonValues.getCurrentPage() + 1 < this.commonValues.getPageCount()) {
				corner.x = (int) ((this.decode_pdf.getVisibleRect().getWidth() / 2) + pageW);
				corner.y = (int) (this.decode_pdf.getInsetH() + pageH);

				final Point cursor = event.getPoint();

				if (cursor.x > corner.x - 30 && cursor.x <= corner.x
						&& ((cursor.y > corner.y - 30 && cursor.y <= corner.y) || (cursor.y >= corner.y - pageH && cursor.y < corner.y - pageH + 30))) {
					// if close enough display preview turnover

					// set cursor
					this.decode_pdf.setCursor(this.currentGUI.getCursor(SwingGUI.GRAB_CURSOR));

					this.previewTurnover = true;
					if (cursor.y >= corner.y - pageH && cursor.y < corner.y - pageH + 30) {
						corner.y = (int) (corner.y - pageH);
						this.decode_pdf.setUserOffsets((int) cursor.getX(), (int) cursor.getY(),
								org.jpedal.external.OffsetOptions.INTERNAL_DRAG_CURSOR_TOP_RIGHT);
					}
					else this.decode_pdf.setUserOffsets((int) cursor.getX(), (int) cursor.getY(),
							org.jpedal.external.OffsetOptions.INTERNAL_DRAG_CURSOR_BOTTOM_RIGHT);

				}
				else {
					if (this.currentGUI.getDragTop()) corner.y = (int) (corner.y - pageH);
					testFall(corner, cursor, false);
				}
			}

			// left turnover
			if (this.commonValues.getCurrentPage() != 1) {
				corner.x = (int) ((this.decode_pdf.getVisibleRect().getWidth() / 2) - pageW);
				corner.y = (int) (this.decode_pdf.getInsetH() + pageH);

				final Point cursor = event.getPoint();

				if (cursor.x < corner.x + 30 && cursor.x >= corner.x
						&& ((cursor.y > corner.y - 30 && cursor.y <= corner.y) || (cursor.y >= corner.y - pageH && cursor.y < corner.y - pageH + 30))) {
					// if close enough display preview turnover
					// System.out.println("drawing left live "+decode_pdf.drawLeft);
					// set cursor
					this.decode_pdf.setCursor(this.currentGUI.getCursor(SwingGUI.GRAB_CURSOR));

					this.previewTurnover = true;
					if (cursor.y >= corner.y - pageH && cursor.y < corner.y - pageH + 30) {
						corner.y = (int) (corner.y - pageH);
						this.decode_pdf.setUserOffsets((int) cursor.getX(), (int) cursor.getY(),
								org.jpedal.external.OffsetOptions.INTERNAL_DRAG_CURSOR_TOP_LEFT);
					}
					else this.decode_pdf.setUserOffsets((int) cursor.getX(), (int) cursor.getY(),
							org.jpedal.external.OffsetOptions.INTERNAL_DRAG_CURSOR_BOTTOM_LEFT);

				}
				else {
					if (this.currentGUI.getDragTop()) corner.y = (int) (corner.y - pageH);
					testFall(corner, cursor, true);
				}
			}

		}

		// <start-adobe>
		// Update cursor position if over page in single mode
		if (this.currentGUI.useNewLayout) {
			int[] flag = new int[2];
			flag[0] = SwingGUI.CURSOR;

			if (this.decode_pdf.getDisplayView() == Display.SINGLE_PAGE || SwingMouseSelector.activateMultipageHighlight) {
				// get raw w and h
				int rawW, rawH;
				if (this.currentGUI.getRotation() % 180 == 90) {
					rawW = this.decode_pdf.getPdfPageData().getCropBoxHeight(1);
					rawH = this.decode_pdf.getPdfPageData().getCropBoxWidth(1);
				}
				else {
					rawW = this.decode_pdf.getPdfPageData().getCropBoxWidth(1);
					rawH = this.decode_pdf.getPdfPageData().getCropBoxHeight(1);
				}

				Point p = event.getPoint();
				int x = (int) p.getX();
				int y = (int) p.getY();

				float scaling = this.decode_pdf.getScaling();

				double pageHeight = scaling * rawH;
				double pageWidth = scaling * rawW;
				int yStart = this.decode_pdf.getInsetH();

				// move so relative to center
				double left = (this.decode_pdf.getWidth() / 2) - (pageWidth / 2);

				if (x >= left && x <= left + pageWidth && y >= yStart && y <= yStart + pageHeight)
				// set displayed
				flag[1] = 1;
				else
				// set not displayed
				flag[1] = 0;

			}
			else {
				// set not displayed
				flag[1] = 0;
			}
			this.currentGUI.setMultibox(flag);
		}
		// <end-adobe>

		if (this.decode_pdf.getExternalHandler(Options.UniqueAnnotationHandler) != null) {
			int[] pos = updateXY(event.getX(), event.getY());
			checkLinks(false, this.decode_pdf.getIO(), pos[0], pos[1]);
		}
	}

	public void mouseWheelMoved(MouseWheelEvent event) {
		if (this.decode_pdf.getDisplayView() == Display.PAGEFLOW) return;

		if (this.currentGUI.getProperties().getValue("allowScrollwheelZoom").toLowerCase().equals("true") && event.isControlDown()) {
			// zoom
			int scaling = this.currentGUI.getSelectedComboIndex(Commands.SCALING);
			if (scaling != -1) {
				scaling = (int) this.decode_pdf.getDPIFactory().removeScaling(this.decode_pdf.getScaling() * 100);
			}
			else {
				String numberValue = (String) this.currentGUI.getSelectedComboItem(Commands.SCALING);
				try {
					scaling = (int) Float.parseFloat(numberValue);
				}
				catch (Exception e) {
					scaling = -1;
					// its got characters in it so get first valid number string
					int length = numberValue.length();
					int ii = 0;
					while (ii < length) {
						char c = numberValue.charAt(ii);
						if (((c >= '0') && (c <= '9')) | (c == '.')) ii++;
						else break;
					}

					if (ii > 0) numberValue = numberValue.substring(0, ii);

					// try again if we reset above
					if (scaling == -1) {
						try {
							scaling = (int) Float.parseFloat(numberValue);
						}
						catch (Exception e1) {
							scaling = -1;
						}
					}
				}
			}

			float value = event.getWheelRotation();

			if (scaling != 1 || value < 0) {
				if (value < 0) {
					value = 1.25f;
				}
				else {
					value = 0.8f;
				}
				if (!(scaling + value < 0)) {
					float currentScaling = (scaling * value);

					// kieran - is this one of yours?
					//
					if (((int) currentScaling) == (scaling)) currentScaling = scaling + 1;
					else currentScaling = ((int) currentScaling);

					if (currentScaling < 1) currentScaling = 1;

					if (currentScaling > 1000) currentScaling = 1000;

					// store mouse location
					final Rectangle r = this.decode_pdf.getVisibleRect();
					final double x = event.getX() / this.decode_pdf.getBounds().getWidth();
					final double y = event.getY() / this.decode_pdf.getBounds().getHeight();

					// update scaling
					this.currentGUI.snapScalingToDefaults(currentScaling);

					// center on mouse location
					Thread t = new Thread() {
						@Override
						public void run() {
							try {
								SwingMousePageTurn.this.decode_pdf.scrollRectToVisible(new Rectangle((int) ((x * SwingMousePageTurn.this.decode_pdf
										.getWidth()) - (r.getWidth() / 2)), (int) ((y * SwingMousePageTurn.this.decode_pdf.getHeight()) - (r
										.getHeight() / 2)), (int) SwingMousePageTurn.this.decode_pdf.getVisibleRect().getWidth(),
										(int) SwingMousePageTurn.this.decode_pdf.getVisibleRect().getHeight()));
								SwingMousePageTurn.this.decode_pdf.repaint();
							}
							catch (Exception e) {
								e.printStackTrace();
							}
						}
					};
					t.start();
					SwingUtilities.invokeLater(t);
				}
			}
		}
		else {

			final JScrollBar scroll = ((JScrollPane) this.decode_pdf.getParent().getParent()).getVerticalScrollBar();
			if ((scroll.getValue() >= scroll.getMaximum() - scroll.getHeight() || scroll.getHeight() == 0) && event.getUnitsToScroll() > 0
					&& this.timeOfLastPageChange + 700 < System.currentTimeMillis()
					&& this.currentGUI.getPageNumber() < this.decode_pdf.getPageCount()) {

				// change page
				this.timeOfLastPageChange = System.currentTimeMillis();
				this.currentCommands.executeCommand(Commands.FORWARDPAGE, null);

				// update scrollbar so at top of page
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						scroll.setValue(scroll.getMinimum());
					}
				});

			}
			else
				if (scroll.getValue() == scroll.getMinimum() && event.getUnitsToScroll() < 0
						&& this.timeOfLastPageChange + 700 < System.currentTimeMillis() && this.currentGUI.getPageNumber() > 1) {

					// change page
					this.timeOfLastPageChange = System.currentTimeMillis();
					this.currentCommands.executeCommand(Commands.BACKPAGE, null);

					// update scrollbar so at bottom of page
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							scroll.setValue(scroll.getMaximum());
						}
					});

				}
				else {
					// scroll
					Area rect = new Area(this.decode_pdf.getVisibleRect());
					AffineTransform transform = new AffineTransform();
					transform.translate(0, event.getUnitsToScroll() * this.decode_pdf.getScrollInterval());
					rect = rect.createTransformedArea(transform);
					this.decode_pdf.scrollRectToVisible(rect.getBounds());
				}
		}
	}

	/**
	 * checks the link areas on the page and allow user to save file
	 **/
	public void checkLinks(boolean mouseClicked, PdfObjectReader pdfObjectReader, int x, int y) {
	}

	/**
	 * get raw co-ords and convert to correct scaled units
	 * 
	 * @return int[] of size 2, [0]=new x value, [1] = new y value
	 */
	protected int[] updateXY(int originalX, int originalY) {

		float scaling = this.currentGUI.getScaling();
		int inset = GUI.getPDFDisplayInset();
		int rotation = this.currentGUI.getRotation();

		// get co-ordinates of top point of outine rectangle
		int x = (int) (((this.currentGUI.AdjustForAlignment(originalX)) - inset) / scaling);
		int y = (int) ((originalY - inset) / scaling);

		// undo any viewport scaling
		if (this.commonValues.maxViewY != 0) { // will not be zero if viewport in play
			x = (int) (((x - (this.commonValues.dx * scaling)) / this.commonValues.viewportScale));
			y = (int) ((this.currentGUI.mediaH - ((this.currentGUI.mediaH - (y / scaling) - this.commonValues.dy) / this.commonValues.viewportScale)) * scaling);
		}

		int[] ret = new int[2];
		if (rotation == 90) {
			ret[1] = x + this.currentGUI.cropY;
			ret[0] = y + this.currentGUI.cropX;
		}
		else
			if ((rotation == 180)) {
				ret[0] = this.currentGUI.mediaW - (x + this.currentGUI.mediaW - this.currentGUI.cropW - this.currentGUI.cropX);
				ret[1] = y + this.currentGUI.cropY;
			}
			else
				if ((rotation == 270)) {
					ret[1] = this.currentGUI.mediaH - (x + this.currentGUI.mediaH - this.currentGUI.cropH - this.currentGUI.cropY);
					ret[0] = this.currentGUI.mediaW - (y + this.currentGUI.mediaW - this.currentGUI.cropW - this.currentGUI.cropX);
				}
				else {
					ret[0] = x + this.currentGUI.cropX;
					ret[1] = this.currentGUI.mediaH - (y + this.currentGUI.mediaH - this.currentGUI.cropH - this.currentGUI.cropY);
				}
		return ret;
	}

	public void testFall(final Point corner, final Point cursor, boolean testLeft) {
		if (!this.previewTurnover) return;

		float width = (this.decode_pdf.getPdfPageData().getCropBoxWidth(1) * this.decode_pdf.getScaling()) - 1;

		if ((this.decode_pdf.getPdfPageData().getRotation(1) + this.currentGUI.getRotation()) % 180 == 90) {
			width = this.decode_pdf.getPdfPageData().getCropBoxHeight(1) * this.decode_pdf.getScaling();
		}

		final float pageW = width;

		if (!testLeft) {
			if (!this.currentGUI.getDragLeft()) {
				// reset cursor
				this.decode_pdf.setCursor(Cursor.getDefaultCursor());

				// If previously displaying turnover, animate to corner
				Thread animation = new Thread() {
					@Override
					public void run() {

						corner.x = (int) ((SwingMousePageTurn.this.decode_pdf.getVisibleRect().getWidth() / 2) + pageW);
						// work out if page change needed
						boolean fallBack = true;
						if (cursor.x < corner.x - pageW) {
							corner.x = (int) (corner.x - (2 * pageW));
							fallBack = false;
						}

						// Fall animation
						int velocity = 1;

						// ensure cursor is not outside expected range
						if (fallBack && cursor.x >= corner.x) cursor.x = corner.x - 1;
						if (!fallBack && cursor.x <= corner.x) cursor.x = corner.x + 1;
						if (!SwingMousePageTurn.this.currentGUI.getDragTop() && cursor.y >= corner.y) cursor.y = corner.y - 1;
						if (SwingMousePageTurn.this.currentGUI.getDragTop() && cursor.y <= corner.y) cursor.y = corner.y + 1;

						// Calculate distance required
						double distX = (corner.x - cursor.x);
						double distY = (corner.y - cursor.y);

						// Loop through animation
						while ((fallBack && cursor.getX() <= corner.getX()) || (!fallBack && cursor.getX() >= corner.getX())
								|| (!SwingMousePageTurn.this.currentGUI.getDragTop() && cursor.getY() <= corner.getY())
								|| (SwingMousePageTurn.this.currentGUI.getDragTop() && cursor.getY() >= corner.getY())) {

							// amount to move this time
							double xMove = velocity * distX * 0.002;
							double yMove = velocity * distY * 0.002;

							// make sure always moves at least 1 pixel in each direction
							if (Math.abs(xMove) < 1) xMove = xMove / Math.abs(xMove);
							if (Math.abs(yMove) < 1) yMove = yMove / Math.abs(yMove);

							cursor.setLocation(cursor.getX() + xMove, cursor.getY() + yMove);
							if (SwingMousePageTurn.this.currentGUI.getDragTop()) SwingMousePageTurn.this.decode_pdf.setUserOffsets(
									(int) cursor.getX(), (int) cursor.getY(), org.jpedal.external.OffsetOptions.INTERNAL_DRAG_CURSOR_TOP_RIGHT);
							else SwingMousePageTurn.this.decode_pdf.setUserOffsets((int) cursor.getX(), (int) cursor.getY(),
									org.jpedal.external.OffsetOptions.INTERNAL_DRAG_CURSOR_BOTTOM_RIGHT);

							// Double speed til moving 32/frame
							if (velocity < 32) velocity = velocity * 2;

							// sleep til next frame
							try {
								Thread.sleep(50);
							}
							catch (Exception e) {
								e.printStackTrace();
							}

						}

						if (!fallBack) {
							// calculate page to turn to
							int forwardPage = SwingMousePageTurn.this.commonValues.getCurrentPage() + 1;
							if (SwingMousePageTurn.this.currentCommands.getPages().getBoolean(Display.BoolValue.SEPARATE_COVER)
									&& forwardPage % 2 == 1) forwardPage++;
							else
								if (!SwingMousePageTurn.this.currentCommands.getPages().getBoolean(Display.BoolValue.SEPARATE_COVER)
										&& forwardPage % 2 == 0) forwardPage++;

							// change page
							SwingMousePageTurn.this.commonValues.setCurrentPage(forwardPage);
							SwingMousePageTurn.this.currentGUI.setPageNumber();
							SwingMousePageTurn.this.decode_pdf.setPageParameters(SwingMousePageTurn.this.currentGUI.getScaling(),
									SwingMousePageTurn.this.commonValues.getCurrentPage());
							SwingMousePageTurn.this.currentGUI.decodePage(false);
						}

						// hide turnover
						SwingMousePageTurn.this.decode_pdf.setUserOffsets(0, 0, org.jpedal.external.OffsetOptions.INTERNAL_DRAG_BLANK);
						setPageTurnAnimating(false);
					}
				};

				setPageTurnAnimating(true);
				animation.start();
				this.previewTurnover = false;
			}
		}
		else {
			if (this.previewTurnover && this.currentGUI.getDragLeft()) {
				// reset cursor
				this.decode_pdf.setCursor(Cursor.getDefaultCursor());

				// If previously displaying turnover, animate to corner
				Thread animation = new Thread() {
					@Override
					public void run() {

						corner.x = (int) ((SwingMousePageTurn.this.decode_pdf.getVisibleRect().getWidth() / 2) - pageW);
						// work out if page change needed
						boolean fallBack = true;
						if (cursor.x > corner.x + pageW) {
							corner.x = (int) (corner.x + (2 * pageW));
							fallBack = false;
						}

						// Fall animation
						int velocity = 1;

						// ensure cursor is not outside expected range
						if (!fallBack && cursor.x >= corner.x) cursor.x = corner.x - 1;
						if (fallBack && cursor.x <= corner.x) cursor.x = corner.x + 1;
						if (!SwingMousePageTurn.this.currentGUI.getDragTop() && cursor.y >= corner.y) cursor.y = corner.y - 1;
						if (SwingMousePageTurn.this.currentGUI.getDragTop() && cursor.y <= corner.y) cursor.y = corner.y + 1;

						// Calculate distance required
						double distX = (corner.x - cursor.x);
						double distY = (corner.y - cursor.y);

						// Loop through animation
						while ((!fallBack && cursor.getX() <= corner.getX()) || (fallBack && cursor.getX() >= corner.getX())
								|| (!SwingMousePageTurn.this.currentGUI.getDragTop() && cursor.getY() <= corner.getY())
								|| (SwingMousePageTurn.this.currentGUI.getDragTop() && cursor.getY() >= corner.getY())) {

							// amount to move this time
							double xMove = velocity * distX * 0.002;
							double yMove = velocity * distY * 0.002;

							// make sure always moves at least 1 pixel in each direction
							if (Math.abs(xMove) < 1) xMove = xMove / Math.abs(xMove);
							if (Math.abs(yMove) < 1) yMove = yMove / Math.abs(yMove);

							cursor.setLocation(cursor.getX() + xMove, cursor.getY() + yMove);
							if (SwingMousePageTurn.this.currentGUI.getDragTop()) SwingMousePageTurn.this.decode_pdf.setUserOffsets(
									(int) cursor.getX(), (int) cursor.getY(), org.jpedal.external.OffsetOptions.INTERNAL_DRAG_CURSOR_TOP_LEFT);
							else SwingMousePageTurn.this.decode_pdf.setUserOffsets((int) cursor.getX(), (int) cursor.getY(),
									org.jpedal.external.OffsetOptions.INTERNAL_DRAG_CURSOR_BOTTOM_LEFT);

							// Double speed til moving 32/frame
							if (velocity < 32) velocity = velocity * 2;

							// sleep til next frame
							try {
								Thread.sleep(50);
							}
							catch (Exception e) {
								e.printStackTrace();
							}

						}

						if (!fallBack) {
							// calculate page to turn to
							int backPage = SwingMousePageTurn.this.commonValues.getCurrentPage() - 2;
							if (backPage == 0) backPage = 1;

							// change page
							SwingMousePageTurn.this.commonValues.setCurrentPage(backPage);
							SwingMousePageTurn.this.currentGUI.setPageNumber();
							SwingMousePageTurn.this.decode_pdf.setPageParameters(SwingMousePageTurn.this.currentGUI.getScaling(),
									SwingMousePageTurn.this.commonValues.getCurrentPage());
							SwingMousePageTurn.this.currentGUI.decodePage(false);
						}

						// hide turnover
						SwingMousePageTurn.this.decode_pdf.setUserOffsets(0, 0, org.jpedal.external.OffsetOptions.INTERNAL_DRAG_BLANK);
						setPageTurnAnimating(false);
					}
				};

				setPageTurnAnimating(true);
				animation.start();
				this.previewTurnover = false;
			}
		}
	}

	public void setPageTurnAnimating(boolean a) {
		this.pageTurnAnimating = a;

		// disable buttons during animation
		if (a == true) {
			this.currentGUI.forward.setEnabled(false);
			this.currentGUI.back.setEnabled(false);
			this.currentGUI.fforward.setEnabled(false);
			this.currentGUI.fback.setEnabled(false);
			this.currentGUI.end.setEnabled(false);
			this.currentGUI.first.setEnabled(false);
		}
		else {
			this.currentGUI.hideRedundentNavButtons();
		}
	}

	public boolean getPageTurnAnimating() {
		return this.pageTurnAnimating;
	}
}
