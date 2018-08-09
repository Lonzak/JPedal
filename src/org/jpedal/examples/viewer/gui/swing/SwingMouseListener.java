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
 * SwingMouseListener.java
 * ---------------
 */

package org.jpedal.examples.viewer.gui.swing;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;

import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;

import org.jpedal.Display;
import org.jpedal.PdfDecoder;
import org.jpedal.examples.viewer.Commands;
import org.jpedal.examples.viewer.MouseMode;
import org.jpedal.examples.viewer.Values;
import org.jpedal.examples.viewer.gui.SwingGUI;
import org.jpedal.examples.viewer.gui.generic.GUIMouseHandler;
import org.jpedal.external.Options;
import org.jpedal.io.PdfObjectReader;
import org.jpedal.render.DynamicVectorRenderer;

public class SwingMouseListener implements GUIMouseHandler, MouseListener, MouseMotionListener, MouseWheelListener {

	private PdfDecoder decode_pdf;
	private SwingGUI currentGUI;
	private Values commonValues;
	private Commands currentCommands;

	SwingMouseSelector selectionFunctions;
	SwingMousePanMode panningFunctions;
	SwingMousePageTurn pageTurnFunctions;

	// Custom mouse function
	private static SwingMouseFunctionality customMouseFunctions;

	private boolean scrollPageChanging = false;

	/** current cursor position */
	private int cx, cy;

	/** tells user if we enter a link */
	private String message = "";

	/**
	 * tracks mouse operation mode currently selected
	 */
	private MouseMode mouseMode = new MouseMode();

	private AutoScrollThread scrollThread = new AutoScrollThread();

	public SwingMouseListener(PdfDecoder decode_pdf, SwingGUI currentGUI, Values commonValues, Commands currentCommands) {

		this.decode_pdf = decode_pdf;
		this.currentGUI = currentGUI;
		this.commonValues = commonValues;
		this.currentCommands = currentCommands;
		this.mouseMode = currentCommands.getMouseMode();

		this.selectionFunctions = new SwingMouseSelector(decode_pdf, currentGUI, commonValues, currentCommands);
		this.panningFunctions = new SwingMousePanMode(decode_pdf);
		this.pageTurnFunctions = new SwingMousePageTurn(decode_pdf, currentGUI, commonValues, currentCommands);

		if (SwingUtilities.isEventDispatchThread()) {
			this.scrollThread.init();
		}
		else {
			final Runnable doPaintComponent = new Runnable() {
				@Override
				public void run() {
					SwingMouseListener.this.scrollThread.init();
				}
			};
			SwingUtilities.invokeLater(doPaintComponent);
		}

		decode_pdf.addExternalHandler(this, Options.SwingMouseHandler);
	}

	@Override
	public void setupExtractor() {
		System.out.println("Set up extractor called");
		this.decode_pdf.addMouseMotionListener(this);
		this.decode_pdf.addMouseListener(this);
	}

	@Override
	public void setupMouse() {
		/**
		 * track and display screen co-ordinates and support links
		 */
		this.decode_pdf.addMouseMotionListener(this);
		this.decode_pdf.addMouseListener(this);
		this.decode_pdf.addMouseWheelListener(this);

		// set cursor
		this.decode_pdf.setDefaultCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	@Override
	public void updateRectangle() {
		switch (this.mouseMode.getMouseMode()) {

			case MouseMode.MOUSE_MODE_TEXT_SELECT:
				this.selectionFunctions.updateRectangle();
				break;

			case MouseMode.MOUSE_MODE_PANNING:
				break;

		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		switch (this.mouseMode.getMouseMode()) {

			case MouseMode.MOUSE_MODE_TEXT_SELECT:
				this.selectionFunctions.mouseClicked(e);
				break;

			case MouseMode.MOUSE_MODE_PANNING:
				// Does Nothing so ignore
				break;

		}

		if (this.currentCommands.getPages().getBoolean(Display.BoolValue.TURNOVER_ON) && this.decode_pdf.getDisplayView() == Display.FACING) {
			this.pageTurnFunctions.mouseClicked(e);
		}

		if (customMouseFunctions != null) {
			customMouseFunctions.mouseClicked(e);
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		switch (this.mouseMode.getMouseMode()) {

			case MouseMode.MOUSE_MODE_TEXT_SELECT:
				// Text selection does nothing here
				// selectionFunctions.mouseEntered(e);
				break;

			case MouseMode.MOUSE_MODE_PANNING:
				// Does Nothing so ignore
				break;

		}

		if (this.currentCommands.getPages().getBoolean(Display.BoolValue.TURNOVER_ON) && this.decode_pdf.getDisplayView() == Display.FACING) {
			this.pageTurnFunctions.mouseEntered(e);
		}

		if (customMouseFunctions != null) {
			customMouseFunctions.mouseEntered(e);
		}
	}

	@Override
	public void mouseExited(MouseEvent e) {

		// Ensure mouse coords don't display when mouse not over PDF
		int[] flag = new int[] { SwingGUI.CURSOR, 0 };
		this.currentGUI.setMultibox(flag);

		// If mouse leaves viewer, stop scrolling
		this.scrollThread.setAutoScroll(false, 0, 0, 0);

		switch (this.mouseMode.getMouseMode()) {

			case MouseMode.MOUSE_MODE_TEXT_SELECT:
				this.selectionFunctions.mouseExited(e);
				break;

			case MouseMode.MOUSE_MODE_PANNING:
				// Does Nothing so ignore
				break;

		}

		if (this.currentCommands.getPages().getBoolean(Display.BoolValue.TURNOVER_ON) && this.decode_pdf.getDisplayView() == Display.FACING) {
			this.pageTurnFunctions.mouseExited(e);
		}

		if (customMouseFunctions != null) {
			customMouseFunctions.mouseExited(e);
		}
	}

	private double middleDragStartX, middleDragStartY, xVelocity, yVelocity;
	private javax.swing.Timer middleDragTimer;

	@Override
	public void mousePressed(MouseEvent e) {
		switch (this.mouseMode.getMouseMode()) {

			case MouseMode.MOUSE_MODE_TEXT_SELECT:
				this.selectionFunctions.mousePressed(e);
				break;

			case MouseMode.MOUSE_MODE_PANNING:
				this.panningFunctions.mousePressed(e);
				break;

		}

		// Start dragging
		if (e.getButton() == MouseEvent.BUTTON2) {
			this.middleDragStartX = e.getX() - this.decode_pdf.getVisibleRect().getX();
			this.middleDragStartY = e.getY() - this.decode_pdf.getVisibleRect().getY();
			this.decode_pdf.setCursor(this.currentGUI.getCursor(SwingGUI.PAN_CURSOR));

			// set up timer to refresh display
			if (this.middleDragTimer == null) this.middleDragTimer = new javax.swing.Timer(100, new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {

					Rectangle r = SwingMouseListener.this.decode_pdf.getVisibleRect();
					r.translate((int) SwingMouseListener.this.xVelocity, (int) SwingMouseListener.this.yVelocity);
					if (SwingMouseListener.this.xVelocity < -2) {
						if (SwingMouseListener.this.yVelocity < -2) SwingMouseListener.this.decode_pdf.setCursor(SwingMouseListener.this.currentGUI
								.getCursor(SwingGUI.PAN_CURSORTL));
						else
							if (SwingMouseListener.this.yVelocity > 2) SwingMouseListener.this.decode_pdf
									.setCursor(SwingMouseListener.this.currentGUI.getCursor(SwingGUI.PAN_CURSORBL));
							else SwingMouseListener.this.decode_pdf.setCursor(SwingMouseListener.this.currentGUI.getCursor(SwingGUI.PAN_CURSORL));
					}
					else
						if (SwingMouseListener.this.xVelocity > 2) {
							if (SwingMouseListener.this.yVelocity < -2) SwingMouseListener.this.decode_pdf
									.setCursor(SwingMouseListener.this.currentGUI.getCursor(SwingGUI.PAN_CURSORTR));
							else
								if (SwingMouseListener.this.yVelocity > 2) SwingMouseListener.this.decode_pdf
										.setCursor(SwingMouseListener.this.currentGUI.getCursor(SwingGUI.PAN_CURSORBR));
								else SwingMouseListener.this.decode_pdf.setCursor(SwingMouseListener.this.currentGUI.getCursor(SwingGUI.PAN_CURSORR));
						}
						else {
							if (SwingMouseListener.this.yVelocity < -2) SwingMouseListener.this.decode_pdf
									.setCursor(SwingMouseListener.this.currentGUI.getCursor(SwingGUI.PAN_CURSORT));
							else
								if (SwingMouseListener.this.yVelocity > 2) SwingMouseListener.this.decode_pdf
										.setCursor(SwingMouseListener.this.currentGUI.getCursor(SwingGUI.PAN_CURSORB));
								else SwingMouseListener.this.decode_pdf.setCursor(SwingMouseListener.this.currentGUI.getCursor(SwingGUI.PAN_CURSOR));
						}
					SwingMouseListener.this.decode_pdf.scrollRectToVisible(r);

				}
			});
			this.middleDragTimer.start();
		}

		if (!SwingUtilities.isMiddleMouseButton(e) && this.currentCommands.getPages().getBoolean(Display.BoolValue.TURNOVER_ON)
				&& this.decode_pdf.getDisplayView() == Display.FACING) {
			this.pageTurnFunctions.mousePressed(e);
		}

		if (customMouseFunctions != null) {
			customMouseFunctions.mousePressed(e);
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		switch (this.mouseMode.getMouseMode()) {

			case MouseMode.MOUSE_MODE_TEXT_SELECT:
				this.selectionFunctions.mouseReleased(e);
				break;

			case MouseMode.MOUSE_MODE_PANNING:
				this.panningFunctions.mouseReleased(e);
				break;

		}

		// stop middle click panning
		if (e.getButton() == MouseEvent.BUTTON2) {
			this.xVelocity = 0;
			this.yVelocity = 0;
			this.decode_pdf.setCursor(this.currentGUI.getCursor(SwingGUI.DEFAULT_CURSOR));
			this.middleDragTimer.stop();
			this.decode_pdf.repaint();
		}

		if (!SwingUtilities.isMiddleMouseButton(e) && this.currentCommands.getPages().getBoolean(Display.BoolValue.TURNOVER_ON)
				&& this.decode_pdf.getDisplayView() == Display.FACING) {
			this.pageTurnFunctions.mouseReleased(e);
		}

		if (customMouseFunctions != null) {
			customMouseFunctions.mouseReleased(e);
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		scrollAndUpdateCoords(e);

		switch (this.mouseMode.getMouseMode()) {

			case MouseMode.MOUSE_MODE_TEXT_SELECT:
				this.selectionFunctions.mouseDragged(e);
				break;

			case MouseMode.MOUSE_MODE_PANNING:
				this.panningFunctions.mouseDragged(e);
				break;

		}

		if (SwingUtilities.isMiddleMouseButton(e)) {
			// middle drag - update velocity
			this.xVelocity = ((e.getX() - this.decode_pdf.getVisibleRect().getX()) - this.middleDragStartX) / 4;
			this.yVelocity = ((e.getY() - this.decode_pdf.getVisibleRect().getY()) - this.middleDragStartY) / 4;
		}

		if (!SwingUtilities.isMiddleMouseButton(e) && this.currentCommands.getPages().getBoolean(Display.BoolValue.TURNOVER_ON)
				&& this.decode_pdf.getDisplayView() == Display.FACING) {
			this.pageTurnFunctions.mouseDragged(e);
		}

		if (customMouseFunctions != null) {
			customMouseFunctions.mouseDragged(e);
		}
	}

	public static void setCustomMouseFunctions(SwingMouseFunctionality cmf) {
		customMouseFunctions = cmf;
	}

	@Override
	public void mouseMoved(MouseEvent e) {

		int page = this.commonValues.getCurrentPage();

		Point p = this.selectionFunctions.getCoordsOnPage(e.getX(), e.getY(), page);
		int x = (int) p.getX();
		int y = (int) p.getY();
		updateCoords(x, y, e.isShiftDown());

		/*
		 * Mouse mode specific code
		 */
		switch (this.mouseMode.getMouseMode()) {

			case MouseMode.MOUSE_MODE_TEXT_SELECT:
				int[] values = this.selectionFunctions.updateXY(e.getX(), e.getY());
				x = values[0];
				y = values[1];
				if (!this.currentCommands.extractingAsImage) getObjectUnderneath(x, y);

				this.selectionFunctions.mouseMoved(e);
				break;

			case MouseMode.MOUSE_MODE_PANNING:
				// Does Nothing so ignore
				break;

		}

		if (this.currentCommands.getPages().getBoolean(Display.BoolValue.TURNOVER_ON) && this.decode_pdf.getDisplayView() == Display.FACING) {
			this.pageTurnFunctions.mouseMoved(e);
		}

		if (customMouseFunctions != null) {
			customMouseFunctions.mouseMoved(e);
		}
	}

	private void getObjectUnderneath(int x, int y) {
		if (this.decode_pdf.getDisplayView() == Display.SINGLE_PAGE) {
			int type = this.decode_pdf.getDynamicRenderer().getObjectUnderneath(x, y);
			switch (type) {
				case -1:
					this.decode_pdf.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					break;
				case DynamicVectorRenderer.TEXT:
					this.decode_pdf.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
					break;
				case DynamicVectorRenderer.IMAGE:
					this.decode_pdf.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
					break;
				case DynamicVectorRenderer.TRUETYPE:
					this.decode_pdf.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
					break;
				case DynamicVectorRenderer.TYPE1C:
					this.decode_pdf.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
					break;
				case DynamicVectorRenderer.TYPE3:
					this.decode_pdf.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
					break;
			}
		}
	}

	class AutoScrollThread implements Runnable {

		Thread scroll;
		boolean autoScroll = false;
		int x = 0;
		int y = 0;
		int interval = 0;

		public AutoScrollThread() {
			this.scroll = new Thread(this);
		}

		public void setAutoScroll(boolean autoScroll, int x, int y, int interval) {
			this.autoScroll = autoScroll;
			this.x = SwingMouseListener.this.currentGUI.AdjustForAlignment(x);
			this.y = y;
			this.interval = interval;
		}

		public void init() {
			this.scroll.start();
		}

		int usedX, usedY;

		@Override
		public void run() {

			while (Thread.currentThread().equals(this.scroll)) {

				// New autoscroll code allow for diagonal scrolling from corner of viewer

				// @kieran - you will see if you move the mouse to right or bottom of page, repaint gets repeatedly called
				// we need to add 2 test to ensure only redrawn if on page (you need to covert x and y back to PDF and
				// check fit in width and height - see code in this class
				// if(autoScroll && usedX!=x && usedY!=y && x>0 && y>0){
				if (this.autoScroll) {
					final Rectangle visible_test = new Rectangle(this.x - this.interval, this.y - this.interval, this.interval * 2, this.interval * 2);
					final Rectangle currentScreen = SwingMouseListener.this.decode_pdf.getVisibleRect();

					if (!currentScreen.contains(visible_test)) {

						if (SwingUtilities.isEventDispatchThread()) {
							SwingMouseListener.this.decode_pdf.scrollRectToVisible(visible_test);
						}
						else {
							final Runnable doPaintComponent = new Runnable() {
								@Override
								public void run() {
									SwingMouseListener.this.decode_pdf.scrollRectToVisible(visible_test);
								}
							};
							SwingUtilities.invokeLater(doPaintComponent);
						}

						// Check values modified by (interval*2) as visible rect changed by interval
						if (this.x - (this.interval * 2) < SwingMouseListener.this.decode_pdf.getVisibleRect().x) this.x = this.x - this.interval;
						else
							if ((this.x + (this.interval * 2)) > (SwingMouseListener.this.decode_pdf.getVisibleRect().x + SwingMouseListener.this.decode_pdf
									.getVisibleRect().width)) this.x = this.x + this.interval;

						if (this.y - (this.interval * 2) < SwingMouseListener.this.decode_pdf.getVisibleRect().y) this.y = this.y - this.interval;
						else
							if ((this.y + (this.interval * 2)) > (SwingMouseListener.this.decode_pdf.getVisibleRect().y + SwingMouseListener.this.decode_pdf
									.getVisibleRect().height)) this.y = this.y + this.interval;

						// thrashes box if constantly called

						// System.out.println("redraw on scroll");
						// decode_pdf.repaint();
					}

					this.usedX = this.x;
					this.usedY = this.y;

				}

				// Delay to check for mouse leaving scroll edge)
				try {
					Thread.sleep(250);
				}
				catch (InterruptedException e) {
					break;
				}
			}
		}
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent event) {

		switch (this.decode_pdf.getDisplayView()) {
			case Display.PAGEFLOW:
				break;

			case Display.FACING:
				if (this.currentCommands.getPages().getBoolean(Display.BoolValue.TURNOVER_ON)) this.pageTurnFunctions.mouseWheelMoved(event);
				break;
			case Display.SINGLE_PAGE:

				if (this.currentGUI.getProperties().getValue("allowScrollwheelZoom").toLowerCase().equals("true")
						&& (event.isMetaDown() || event.isControlDown())) {
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
										SwingMouseListener.this.decode_pdf.scrollRectToVisible(new Rectangle(
												(int) ((x * SwingMouseListener.this.decode_pdf.getWidth()) - (r.getWidth() / 2)),
												(int) ((y * SwingMouseListener.this.decode_pdf.getHeight()) - (r.getHeight() / 2)),
												(int) SwingMouseListener.this.decode_pdf.getVisibleRect().getWidth(),
												(int) SwingMouseListener.this.decode_pdf.getVisibleRect().getHeight()));
										SwingMouseListener.this.decode_pdf.repaint();
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

					// if(t2!=null)
					// t2.cancel();
					//
					// t2 = new java.util.Timer();

					final JScrollBar scroll = this.currentGUI.getVerticalScrollBar();

					// Ensure scrollToPage is set to the correct value as this can change
					// if scaling increases page size to larger than the display area
					this.scrollToPage = scroll.getValue();

					if ((scroll.getValue() >= scroll.getMaximum() - scroll.getHeight() || scroll.getHeight() == 0) && event.getUnitsToScroll() > 0
							&& this.scrollToPage <= this.decode_pdf.getPageCount()) {

						if (this.scrollPageChanging) return;

						this.scrollPageChanging = true;

						// change page
						// currentCommands.executeCommand(Commands.FORWARDPAGE, null);
						if (this.scrollToPage < this.decode_pdf.getPageCount()) this.scrollToPage++;

						// TimerTask listener = new PageListener();
						// t2.schedule(listener, 500);

						// update scrollbar so at top of page
						if (SwingUtilities.isEventDispatchThread()) {
							scroll.setValue(this.scrollToPage);
						}
						else {
							try {
								SwingUtilities.invokeAndWait(new Runnable() {
									@Override
									public void run() {
										scroll.setValue(SwingMouseListener.this.scrollToPage);
									}
								});
							}
							catch (Exception e) {
								SwingUtilities.invokeLater(new Runnable() {
									@Override
									public void run() {
										scroll.setValue(SwingMouseListener.this.scrollToPage);
									}
								});
							}
						}

						this.scrollPageChanging = false;

					}
					else
						if (scroll.getValue() >= scroll.getMinimum() && event.getUnitsToScroll() < 0 && this.scrollToPage >= 1) {

							if (this.scrollPageChanging) return;

							this.scrollPageChanging = true;

							// change page
							// currentCommands.executeCommand(Commands.BACKPAGE, null);
							if (this.scrollToPage >= 1) this.scrollToPage--;

							// TimerTask listener = new PageListener();
							// t2.schedule(listener, 500);

							// update scrollbar so at bottom of page
							if (SwingUtilities.isEventDispatchThread()) {
								scroll.setValue(this.scrollToPage);
							}
							else {
								try {
									SwingUtilities.invokeAndWait(new Runnable() {
										@Override
										public void run() {
											scroll.setValue(SwingMouseListener.this.scrollToPage);
										}
									});
								}
								catch (Exception e) {
									SwingUtilities.invokeLater(new Runnable() {
										@Override
										public void run() {
											scroll.setValue(SwingMouseListener.this.scrollToPage);
										}
									});
								}
							}

							this.scrollPageChanging = false;

						}
				}

				// Don't break here so that continuous modes and single mode can use the following
			default:

				// scroll
				Area rect = new Area(this.decode_pdf.getVisibleRect());
				AffineTransform transform = new AffineTransform();
				transform.translate(0, event.getUnitsToScroll() * this.decode_pdf.getScrollInterval());
				rect = rect.createTransformedArea(transform);
				this.decode_pdf.scrollRectToVisible(rect.getBounds());

				break;
		}
		// //Not used in text selection or panning modes
		// if (decode_pdf.turnoverOn && decode_pdf.getDisplayView()==Display.FACING){
		// pageTurnFunctions.mouseWheelMoved(event);
		// }
	}

	// java.util.Timer t2 = null;
	int scrollToPage = -1;

	// class PageListener extends TimerTask {
	//
	// public void run() {
	// if(scrollToPage!=-1){
	// currentCommands.executeCommand(Commands.GOTO, new Object[]{(""+scrollToPage)});
	// scrollToPage = -1;
	// }
	// }
	// }

	/**
	 * scroll to visible Rectangle and update Coords box on screen
	 */
	protected void scrollAndUpdateCoords(MouseEvent event) {
		// scroll if user hits side
		int interval = this.decode_pdf.getScrollInterval();
		Rectangle visible_test = new Rectangle(this.currentGUI.AdjustForAlignment(event.getX()), event.getY(), interval, interval);
		if ((this.currentGUI.allowScrolling()) && (!this.decode_pdf.getVisibleRect().contains(visible_test))) this.decode_pdf
				.scrollRectToVisible(visible_test);

		int page = this.commonValues.getCurrentPage();

		Point p = this.selectionFunctions.getCoordsOnPage(event.getX(), event.getY(), page);
		int x = (int) p.getX();
		int y = (int) p.getY();
		updateCoords(x, y, event.isShiftDown());
	}

	/**
	 * update current page co-ordinates on screen
	 */
	public void updateCoords(/* MouseEvent event */int x, int y, boolean isShiftDown) {

		this.cx = x;
		this.cy = y;

		if (this.decode_pdf.getDisplayView() != Display.SINGLE_PAGE) {

			if (SwingMouseSelector.activateMultipageHighlight) {

				// @kieran remove when facing is working correctly
				if (this.decode_pdf.getDisplayView() == Display.FACING) {
					this.cx = 0;
					this.cy = 0;
				}
			}
			else {
				this.cx = 0;
				this.cy = 0;
			}
		}

		if ((Values.isProcessing()) | (this.commonValues.getSelectedFile() == null)) this.currentGUI.setCoordText("  X: " + " Y: " + ' ' + ' '); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		else this.currentGUI.setCoordText("  X: " + this.cx + " Y: " + this.cy + ' ' + ' ' + this.message); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	public int[] getCursorLocation() {
		return new int[] { this.cx, this.cy };
	}

	public void checkLinks(boolean mouseClicked, PdfObjectReader pdfObjectReader) {
		// int[] pos = updateXY(event.getX(), event.getY());
		this.pageTurnFunctions.checkLinks(mouseClicked, pdfObjectReader, this.cx, this.cy);
	}

	public void updateCordsFromFormComponent(MouseEvent e) {
		JComponent component = (JComponent) e.getSource();

		int x = component.getX() + e.getX();
		int y = component.getY() + e.getY();
		Point p = this.selectionFunctions.getCoordsOnPage(x, y, this.commonValues.getCurrentPage());
		x = (int) p.getX();
		y = (int) p.getY();

		updateCoords(x, y, e.isShiftDown());
	}

	public boolean getPageTurnAnimating() {
		return this.pageTurnFunctions.getPageTurnAnimating();
	}

	public void setPageTurnAnimating(boolean a) {
		this.pageTurnFunctions.setPageTurnAnimating(a);
	}
}
