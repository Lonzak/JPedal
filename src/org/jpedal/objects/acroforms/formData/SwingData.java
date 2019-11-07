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
 * SwingData.java
 * ---------------
 */
package org.jpedal.objects.acroforms.formData;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.Arrays;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;

import org.jpedal.Display;
import org.jpedal.PdfDecoder;
import org.jpedal.constants.ErrorCodes;
import org.jpedal.exception.PdfException;
import org.jpedal.external.CustomFormPrint;
import org.jpedal.io.PdfObjectReader;
import org.jpedal.objects.acroforms.actions.ActionHandler;
import org.jpedal.objects.acroforms.creation.FormFactory;
import org.jpedal.objects.acroforms.creation.JPedalBorderFactory;
import org.jpedal.objects.acroforms.overridingImplementations.FixImageIcon;
import org.jpedal.objects.acroforms.overridingImplementations.PdfSwingPopup;
import org.jpedal.objects.acroforms.overridingImplementations.ReadOnlyTextIcon;
import org.jpedal.objects.acroforms.utils.FormUtils;
import org.jpedal.objects.raw.FormObject;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.utils.LogWriter;

//import org.jpedal.objects.raw.PageObject;

/**
 * Swing specific implementation of Widget data (all non-Swing variables defined in ComponentData)
 * 
 */
public class SwingData extends ComponentData {

	// used to enable work around for bug in JDK1.6.0_10+
	public static boolean JVMBugRightAlignFix = false;

	CustomFormPrint customFormPrint = null;

	/**
	 * panel components attached to
	 */
	private JPanel panel;

	/** scaling used for readOnly text icons drawn as images */
	public static int readOnlyScaling = -1;

	public SwingData() {}

	public SwingData(int html) {
		super();

		this.formFactoryType = html;
	}

	/**
	 * generic call used for triggering repaint and actions
	 */
	@Override
	public void loseFocus() {}

	/**
	 * valid flag used by Javascript to allow rollback &nbsp
	 * 
	 * @param ref
	 *            - name of the field to change the value of
	 * @param value
	 *            - the value to change it to
	 * @param isValid
	 *            - is a valid value or not
	 * @param isFormatted
	 *            - is formatted properly
	 */
	@Override
	public void setValue(String ref, Object value, boolean isValid, boolean isFormatted) {

		Object checkObj = super.setValue(ref, value, isValid, isFormatted, getValue(ref));

		// System.out.println("SwingData.setValue()"+ref+" set display value="+value);
		// set the display fields value
		if (checkObj != null) setFormValue(value, checkObj);

		// idea to move flagging into here, thus allowing kids to be synced easily, but needs a lot of tweaking to work.
		// flagLastUsedValue(allFields[((Integer) checkObj).intValue()], (FormObject)rawFormData.get(ref), true);
	}

	@Override
	public Object getFormValue(Object checkObj) {

		Object retValue = "";

		if (checkObj != null) {
			int index = (Integer) checkObj;

			Object comp = checkGUIObjectResolved((Integer) checkObj);

			FormObject formObject = (FormObject) this.rawFormData.get(this.convertFormIDtoRef.get(index));
			int type = formObject.getFormType();

			switch (type) {
				case FormFactory.checkboxbutton:
					retValue = ((JCheckBox) comp).isSelected();
					break;

				case FormFactory.combobox:
					retValue = ((JComboBox) comp).getSelectedItem();
					break;

				case FormFactory.list:
					retValue = ((JList) comp).getSelectedValues();
					break;

				case FormFactory.radiobutton:
					retValue = ((JRadioButton) comp).isSelected();
					break;

				case FormFactory.singlelinepassword:

					retValue = ((JTextComponent) comp).getText();
					break;

				case FormFactory.multilinepassword:
					retValue = ((JTextComponent) comp).getText();
					break;

				case FormFactory.multilinetext:
					retValue = ((JTextComponent) comp).getText();
					break;

				case FormFactory.singlelinetext:
					if (comp instanceof JButton) {
						retValue = ((ReadOnlyTextIcon) ((JButton) comp).getIcon()).getText();
					}
					else {
						// retValue = ((JTextComponent) comp).getText();
						retValue = formObject.getTextString();
					}
					break;

				default:
					switch (formObject.getGUIType()) {
						case ComponentData.TEXT_TYPE:
							retValue = formObject.getTextString();
							break;
					}
					break;
			}
		}
		return retValue;
	}

	/** sets the value for the displayed form */
	public void setFormValue(Object value, Object checkObj) {

		if (checkObj != null) {
			int index = (Integer) checkObj;

			Component comp = (Component) checkGUIObjectResolved(index);
			FormObject formObject = (FormObject) this.rawFormData.get(this.convertFormIDtoRef.get(index));
			int type = formObject.getFormType();

			switch (type) {
				case FormFactory.checkboxbutton:
					((JCheckBox) comp).setSelected(Boolean.valueOf((String) value));
					break;
				case FormFactory.combobox:
					((JComboBox) comp).setSelectedItem(value);
					break;
				case FormFactory.list:
					((JList) comp).setSelectedValue(value, false);
					break;
				case FormFactory.radiobutton:
					((JRadioButton) comp).setText((String) value);
					break;
				case FormFactory.singlelinepassword:
				case FormFactory.multilinepassword:
				case FormFactory.multilinetext:
					((JTextComponent) comp).setText((String) value);
					break;
				case FormFactory.singlelinetext:

					checkGUIObjectResolved(index);

					if (comp instanceof JButton) {
						((ReadOnlyTextIcon) ((JButton) comp).getIcon()).setText((String) value);
					}
					else {
						((JTextComponent) comp).setText((String) value);
					}
					break;
				case FormFactory.pushbutton:
				case FormFactory.annotation:
				case FormFactory.signature:
					// do not alter as is read only
					break;
				default:
					break;
			}
		}
	}

	public void debugForms() {}

	public void showForms() {

		if (this.nextFreeField > 0) {
			for (int i = 0; i < this.nextFreeField + 1; i++) {

				FormObject formObject = (FormObject) this.rawFormData.get(this.convertFormIDtoRef.get(i));

				if (formObject == null) {
					continue;
				}

				Component comp = (Component) checkGUIObjectResolved(i);

				int type = formObject.getFormType();

				if (comp != null) {
					if (type == FormFactory.pushbutton || type == FormFactory.annotation || type == FormFactory.signature) {
						comp.setBackground(Color.blue);
					}
					else
						if (type == FormFactory.singlelinepassword || type == FormFactory.singlelinetext || type == FormFactory.multilinetext
								|| type == FormFactory.multilinepassword) {
							comp.setBackground(Color.red);
							if (comp instanceof JButton) comp.setBackground(Color.cyan);
						}
						else {
							comp.setBackground(Color.green);
						}

					comp.setForeground(Color.lightGray);
					comp.setVisible(true);
					comp.setEnabled(true);
					((JComponent) comp).setOpaque(true);
					if (type == FormFactory.pushbutton || type == FormFactory.checkboxbutton || type == FormFactory.annotation
							|| type == FormFactory.signature) {
						((AbstractButton) comp).setIcon(null);
					}
					else
						if (type == FormFactory.combobox) {
							((JComboBox) comp).setEditable(false);
						}
				}
			}
		}
	}

	/**
	 * render component onto G2 for print of image creation
	 * 
	 * @param printcombo
	 *            = tells us to print the raw combobox, and dont do aymore formatting of the combobox, should only be called from this method.
	 */
	private void renderComponent(Graphics2D g2, int currentComp, Component comp, int rotation, boolean printcombo, int indent, boolean isPrinting) {

		if (comp != null) {

			boolean editable = false;
			int page = getPageForFormObject(convertIDtoRef(currentComp));

			if (!printcombo && comp instanceof JComboBox) {

				// if we have the comobobox, adapt so we see what we want to
				// for the combobox we need to print the first item within it otherwise we doent see the contents.
				JComboBox combo = (JComboBox) comp;

				if (combo.isEditable()) {
					editable = true;
					combo.setEditable(false);
				}

				if (combo.getComponentCount() > 0) {
					Object selected = combo.getSelectedItem();
					if (selected != null) {

						JTextField text = new JTextField();

						text.setText(selected.toString());

						text.setBackground(combo.getBackground());
						text.setForeground(combo.getForeground());
						text.setFont(combo.getFont());

						text.setBorder(combo.getBorder());

						renderComponent(g2, currentComp, text, rotation, false, indent, isPrinting);
					}
				}

				// set flag to say this is the combobox.
				// (we dont want to print this, as we have printed it as a textfield )
				printcombo = true;
			}

			if (!printcombo) {

				AffineTransform ax = g2.getTransform();

				// when true works on printing,
				// whnen false works for testrenderer, on most except eva_subjob_quer.pdf
				if (isPrinting) {
					// if we dont have the combobox print it
					scaleComponent(page, 1, rotation, currentComp, comp, false, false, indent);

					// Rectangle rect = comp.getBounds();

					// work out new translate after rotate deduced from FixImageIcon
					AffineTransform at;
					switch (360 - rotation) {
						case 270:
							at = AffineTransform.getRotateInstance((270 * java.lang.Math.PI) / 180, 0, 0);
							g2.translate(comp.getBounds().y + this.cropOtherY[page] - this.insetH,
									this.pageData.getCropBoxHeight(page) - comp.getBounds().x + this.insetW);

							g2.transform(at);
							g2.translate(-this.insetW, 0);

							break;
						case 90:
							at = AffineTransform.getRotateInstance((90 * java.lang.Math.PI) / 180, 0, 0);
							g2.translate(comp.getBounds().y + this.cropOtherY[page] - this.insetH, comp.getBounds().x + this.insetW);

							g2.transform(at);
							g2.translate(0, -this.insetH);
							break;
						case 180:// not tested
							at = AffineTransform.getRotateInstance((180 * java.lang.Math.PI) / 180, 0, 0);
							// translate to x,y of comp before applying rotate.
							g2.translate(comp.getBounds().x - this.insetW, comp.getBounds().y + this.cropOtherY[page]);

							g2.transform(at);
							// g2.translate(-rect.width, -rect.height );
							g2.translate(-this.insetW, -this.insetH);// will prob need this to work

							break;
						default:
							// translate to x,y of comp before applying rotate.
							g2.translate(comp.getBounds().x - this.insetW, comp.getBounds().y + this.cropOtherY[page]);
							break;
					}
				}
				else {// used for testrenderer, images

					// if we dont have the combobox print it
					scaleComponent(page, 1, rotation, currentComp, comp, false, false, indent);

					Rectangle rect = comp.getBounds();

					// translate to x,y of comp before applying rotate.
					g2.translate(rect.x - this.insetW, rect.y + this.cropOtherY[page]);

					// only look at rotate on text fields as other fields should be handled.
					if (getFieldType(comp) == ComponentData.TEXT_TYPE) {
						if (this.pageData.getRotation(page) == 90 || this.pageData.getRotation(page) == 270) {
							comp.setBounds(rect.x, rect.y, rect.height, rect.width);
							rect = comp.getBounds();
						}

						// fix for file eva_subjob_quer.pdf as it has page rotations 90 0 90 0, which makes
						// page 1 and 3 print wrong when using each pages rotation value.
						int rotate = rotation - this.pageData.getRotation(0);
						if (rotate < 0) rotate = 360 + rotate;

						// work out new translate after rotate deduced from FixImageIcon
						AffineTransform at;
						switch (rotate) {
							case 270:
								at = AffineTransform.getRotateInstance((rotate * java.lang.Math.PI) / 180, 0, 0);
								g2.transform(at);
								g2.translate(-rect.width, 0);
								break;
							case 90:// not tested
								at = AffineTransform.getRotateInstance((rotate * java.lang.Math.PI) / 180, 0, 0);
								g2.transform(at);
								g2.translate(0, -rect.height);

								break;
							case 180:// not tested
								at = AffineTransform.getRotateInstance((rotate * java.lang.Math.PI) / 180, 0, 0);
								g2.transform(at);
								g2.translate(-rect.width, -rect.height);

								break;
						}
					}
				}

				/**
				 * fix for bug in Java 1.6.0_10 onwards with right aligned values
				 */
				boolean isPainted = false;

				// hack for a very sepcific issue so rather leave
				// Rog's code intack and take out for ME
				if (JVMBugRightAlignFix && comp instanceof JTextField) {

					JTextField field = new JTextField();
					JTextField source = (JTextField) comp;

					if (source.getHorizontalAlignment() == SwingConstants.RIGHT) {

						field.setFont(source.getFont());
						field.setLocation(source.getLocation());
						field.setSize(source.getSize());
						field.setBorder(source.getBorder());
						field.setHorizontalAlignment(SwingConstants.RIGHT);
						// field.setText(new String(createCharArray(' ', maxLengthForTextOnPage - source.getText().length())) + source.getText());

						// Rog's modified code
						int additionalBlanks = 0;
						int width = g2.getFontMetrics(comp.getFont()).stringWidth(
								new String(createCharArray(' ', this.maxLengthForTextOnPage - source.getText().length())) + source.getText());
						int eightPointWidth = g2.getFontMetrics(comp.getFont().deriveFont(7.0F)).stringWidth(
								new String(createCharArray(' ', this.maxLengthForTextOnPage - source.getText().length())) + source.getText());
						int difference = width - eightPointWidth;
						if (difference > 0) {
							additionalBlanks = difference / g2.getFontMetrics(comp.getFont().deriveFont(7.0F)).stringWidth(" ");
						}
						String originalTest = source.getText();
						int bunchOfSpaces = (this.maxLengthForTextOnPage + additionalBlanks) - source.getText().length();
						field.setText(new String(createCharArray(' ', bunchOfSpaces)) + originalTest);
						width = g2.getFontMetrics(comp.getFont()).stringWidth(field.getText());

						int insets = 0;
						if (field.getBorder() != null) insets = (field.getBorder().getBorderInsets(field).left + field.getBorder().getBorderInsets(
								field).right);
						boolean needsChange = false;
						while (bunchOfSpaces > 0 && width > field.getWidth() - insets) {
							bunchOfSpaces = (this.maxLengthForTextOnPage + additionalBlanks) - source.getText().length();
							String newText = new String(createCharArray(' ', bunchOfSpaces)) + originalTest;
							field.setText(newText);
							additionalBlanks--;
							width = g2.getFontMetrics(comp.getFont().deriveFont(7.0F)).stringWidth(field.getText());
							needsChange = true;
						}

						if (needsChange) {
							additionalBlanks--;
							bunchOfSpaces = (this.maxLengthForTextOnPage + additionalBlanks) - source.getText().length();
							String newText = new String(createCharArray(' ', bunchOfSpaces)) + originalTest;
							field.setText(newText);
						}

						// //
						field.paint(g2);
						isPainted = true;
					}
				}

				if (!isPainted) comp.paint(g2);

				g2.setTransform(ax);
			}

			if (editable /* && comp instanceof JComboBox */) {
				((JComboBox) comp).setEditable(true);
			}
		}
	}

	/**
	 * used by fix above
	 * 
	 * @param c
	 * @param count
	 */
	private static char[] createCharArray(char c, int count) {
		if (count <= 0) return new char[0];
		char[] result = new char[count];
		Arrays.fill(result, 0, result.length, c);
		return result;
	}

	boolean renderFormsWithJPedalFontRenderer = false;

	private Component[] popups = new Component[0];

	int maxLengthForTextOnPage = 0;

	/**
	 * draw the forms onto display for print of image. Note different routine to handle forms also displayed at present
	 */
	@Override
	public void renderFormsOntoG2(Object raw, int pageIndex, float currentScaling, int currentIndent, int currentRotation, Map componentsToIgnore,
			FormFactory formFactory, PdfObjectReader currentPdfFile, int pageHeight) {

		if (this.formsUnordered == null || this.rasterizeForms) return;

		this.componentsToIgnore = componentsToIgnore;

		// only passed in on print so also used as flag
		boolean isPrinting = formFactory != null;

		// fix for issue with display of items in 1.6.0_10+
		if (JVMBugRightAlignFix && isPrinting) {

			this.maxLengthForTextOnPage = 0;

			// get unsorted components and iterate over forms
			for (Object o : this.formsUnordered[pageIndex]) {

				// get ref from list and convert to index.
				Object nextVal = o;
				int currentComp = (Integer) this.refToCompIndex.get(nextVal);

				if (currentComp != -1) {

					Component comp = (Component) checkGUIObjectResolved(currentComp);

					if (comp instanceof JTextField) {
						JTextField text = (JTextField) comp;
						int newLength = text.getText().length();

						if (newLength > this.maxLengthForTextOnPage && text.getHorizontalAlignment() == SwingConstants.RIGHT) {
							this.maxLengthForTextOnPage = newLength;
							// System.out.println(maxLengthForTextOnPage+ " "+text.getText());
						}
					}
				}
			}
		}

		Graphics2D g2 = (Graphics2D) raw;

		AffineTransform defaultAf = g2.getTransform();

		// setup scaling
		AffineTransform aff = g2.getTransform();
		aff.scale(1, -1);
		aff.translate(0, -pageHeight - this.insetH);
		g2.setTransform(aff);

		int currentComp;
		Object rawObj;
		Component comp;

		try {

			/** needs to go onto a panel to be drawn */
			JPanel dummyPanel = new JPanel();

			// get unsorted components and iterate over forms
			for (Object nextVal : this.formsUnordered[pageIndex]) {

				// get ref from list and convert to index.
				rawObj = this.refToCompIndex.get(nextVal);
				if (rawObj == null) continue;

				currentComp = (Integer) rawObj;

				if (currentComp != -1) {

					// is this form allowed to be printed
					boolean[] flags = ((FormObject) this.rawFormData.get(this.convertFormIDtoRef.get(currentComp))).getCharacteristics();
					if (flags[1] || (isPrinting && !flags[2])) {// 1 hidden, 2 print (hense !)
						continue;
					}

					checkGUIObjectResolved(currentComp);
					FormObject formObject = (FormObject) this.rawFormData.get(this.convertFormIDtoRef.get(currentComp));

					comp = (Component) formObject.getGUIComponent();

					if (comp != null && comp.isVisible()) {

						/**
						 * sync kid values if needed
						 */
						syncKidValues(currentComp);

						// wrap JList in ScrollPane to ensure displayed if size set to smaller than list
						// (ie like ComboBox)
						// @note - fixed by moving the selected item to the top of the list.
						// this works for the file acro_func_baseline1.pdf
						// and now works on file fieldertest2.pdf and on formsmixed.pdf
						// but does not render correct in tests i THINK, UNCONFIRMED
						// leaves grayed out boxes in renderer.

						/**
						 * possible problem with rotation files, just test if rotated 90 or 270 and get appropriate height or width, that would
						 * represent the height when viewed at correct orientation
						 */

						Rectangle bounds = formObject.getBoundingRectangle();
						float boundHeight = bounds.height;

						int swingHeight = comp.getPreferredSize().height + 6;

						/**
						 * check if the component is a jlist, and if it is, then is their a selected item, if their is then is the bounding box
						 * smaller than the jlist actual size then and only then do we need to change the way its printed
						 */
						if (this.renderFormsWithJPedalFontRenderer) {

							// get correct key to lookup form data
							String ref = this.convertIDtoRef(currentComp);

							// System.out.println(currentComp+" "+comp.getLocation()+" "+comp);
							Object[] rawForm = this.getRawForm(ref);
							for (Object aRawForm : rawForm) {
								if (aRawForm != null) {
									FormObject form = (FormObject) aRawForm;
									System.out.println(ref + ' ' + form.getTextFont() + ' ' + form.getTextString());
								}
							}
						}
						else
							if (isFormNotPrinted(currentComp)) {}
							else
								if (comp instanceof JList && ((JList) comp).getSelectedIndex() != -1 && boundHeight < swingHeight) {

									JList comp2 = (JList) comp;

									dummyPanel.add(comp);

									// JList tmp = comp2;//((JList)((JScrollPane)comp).getViewport().getComponent(0));
									ListModel model = comp2.getModel();
									Object[] array = new Object[model.getSize()];

									int selectedIndex = comp2.getSelectedIndex();
									int c = 0;
									array[c++] = model.getElementAt(selectedIndex);

									for (int i = 0; i < array.length; i++) {
										if (i != selectedIndex) array[c++] = model.getElementAt(i);
									}

									comp2.setListData(array);
									comp2.setSelectedIndex(0);

									try {
										renderComponent(g2, currentComp, comp2, currentRotation, false, currentIndent, isPrinting);
										dummyPanel.remove(comp2);
									}
									catch (Exception cc) {

									}

								}
								else { // if printing improve quality on AP images

									boolean customPrintoverRide = false;
									if (this.customFormPrint != null) {

										// setup scalings
										scaleComponent(this.currentPage, 1, this.rotation, currentComp, comp, false, false, this.indent);

										// comp.paint(g2);
										customPrintoverRide = this.customFormPrint.print(g2, currentComp, comp, this);
										// g2.setTransform(ax);

									}
									// System.out.println(customFormPrint+" "+currentComp+" "+comp);

									if (!customPrintoverRide) {
										// this is where the cust1/display_error file line went, but it affects costena printing.
										if (comp instanceof AbstractButton) {
											Object obj = ((AbstractButton) comp).getIcon();

											if (obj != null) {
												if (obj instanceof FixImageIcon) {
													((FixImageIcon) (obj)).setPrinting(true, 1);
												}
												else
													if (readOnlyScaling > 0 && obj instanceof ReadOnlyTextIcon) {
														((ReadOnlyTextIcon) (obj)).setPrinting(true, readOnlyScaling);
													}
											}
										}
										dummyPanel.add(comp);

										try {
											renderComponent(g2, currentComp, comp, currentRotation, false, currentIndent, isPrinting);
											dummyPanel.remove(comp);
										}
										catch (Exception cc) {

										}

										if (comp instanceof AbstractButton) {
											Object obj = ((AbstractButton) comp).getIcon();
											if (obj instanceof FixImageIcon) {
												((FixImageIcon) (obj)).setPrinting(false, 1);
											}
											else
												if (obj instanceof ReadOnlyTextIcon) {
													((ReadOnlyTextIcon) (obj)).setPrinting(false, 1);
												}
										}
									}
								}
					}

					currentComp++;

					if (currentComp == this.nextFreeField + 1) break;

				}
			}
		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}

		g2.setTransform(defaultAf);

		// put componenents back
		if (this.currentPage == pageIndex && this.panel != null) {
			// createDisplayComponentsForPage(pageIndex,this.panel,this.displayScaling,this.rotation);
			// panel.invalidate();
			// panel.repaint();

			// forceRedraw=true;
			resetScaledLocation(this.displayScaling, this.rotation, this.indent);

		}
	}

	/**
	 * alter font and size to match scaling. Note we pass in compoent so we can have multiple copies (needed if printing page displayed).
	 */
	private void scaleComponent(int curPage, float scale, int rotate, int id, final Component curComp, boolean redraw, boolean popups, int indent) {

		// if (showMethods)
		// System.out.println("DefaultAcroRenderer.scaleComponent()");

		if (curComp == null) return;

		/**
		 * work out if visible in Layer
		 */
		if (this.layers != null) {

			/**
			 * get matching component
			 */
			// get correct key to lookup form data
			String ref = this.convertIDtoRef(id);

			String layerName = null;
			Object[] rawForm = this.getRawForm(ref);
			if (rawForm[0] != null) {
				layerName = ((FormObject) (rawForm[0])).getLayerName();
			}

			// do not display
			if (layerName != null && this.layers.isLayerName(layerName)) {

				boolean isVisible = this.layers.isVisible(layerName);
				curComp.setVisible(isVisible);
			}
		}
		// ////////////////////////

		float[] box = new float[] { 0, 0, 0, 0 };
		// set to false if popups are already on page, to stop recalculating bounds, so that users can move them
		boolean calcBounds = true;
		if (popups) {
			Rectangle popRect = curComp.getBounds();
			if (popRect.x != 0 && popRect.y != 0) {
				// the popup has prob been set so keep the values.
				calcBounds = false;
			}
			else {
				box = this.popupBounds[id];
			}
		}
		else {
			FormObject formObject = (FormObject) this.rawFormData.get(this.convertFormIDtoRef.get(id));
			Rectangle rect = formObject.getBoundingRectangle();

			box = new float[] { rect.x, rect.y, rect.width + rect.x, rect.height + rect.y };

		}
		int[] bounds = new int[] { 0, 0, 0, 0 };
		if (calcBounds) {
			int cropRotation = rotate;
			bounds = cropComponent(box, curPage, scale, cropRotation, id, redraw);
		}

		if (popups) {
			// we dont change the width and height
			bounds[2] = (int) (this.popupBounds[id][2] - this.popupBounds[id][0]);
			bounds[3] = (int) (this.popupBounds[id][3] - this.popupBounds[id][1]);
		}

		/**
		 * rescale the font size
		 */
		Font resetFont = curComp.getFont();
		if (!popups && resetFont != null) {
			// send in scale, rotation, and curComp as they could be from the print routines,
			// which define these parameters.
			recalcFontSize(scale, rotate, id, curComp);
		}

		// scale border if needed
		if (!popups && curComp instanceof JComponent && ((JComponent) curComp).getBorder() != null) {

			FormObject form = (FormObject) this.rawFormData.get(convertIDtoRef(id));
			if (form != null) ((JComponent) curComp).setBorder((Border) generateBorderfromForm(form, scale));
		}

		// factor in offset if multiple pages displayed
		if (calcBounds && (this.xReached != null)) {
			bounds[0] = bounds[0] + this.xReached[curPage];
			bounds[1] = bounds[1] + this.yReached[curPage];
		}

		int pageWidth;
		if ((this.pageData.getRotation(curPage) + rotate) % 180 == 90) {
			pageWidth = this.pageData.getCropBoxHeight(curPage);
		}
		else {
			pageWidth = this.pageData.getCropBoxWidth(curPage);
		}

		if (this.displayView == Display.CONTINUOUS) {
			double newIndent;
			if (rotate == 0 || rotate == 180) newIndent = (this.widestPageNR - (pageWidth)) / 2;
			else newIndent = (this.widestPageR - (pageWidth)) / 2;

			indent = (int) (indent + (newIndent * scale));
		}

		int totalOffsetX = this.userX + indent + this.insetW;
		int totalOffsetY = this.userY + this.insetH;

		Rectangle boundRect = new Rectangle(totalOffsetX + bounds[0], totalOffsetY + bounds[1], bounds[2], bounds[3]);
		if (popups) {
			if (!calcBounds) {
				// if popup already calculated use current values so users moving is kept
				Rectangle popRect = curComp.getBounds();
				boundRect.x = popRect.x;
				boundRect.y = popRect.y;
			}
			// boundRect is changed within method if needed.
			checkPopupBoundsOnPage(boundRect, this.panel.getVisibleRect());
		}
		curComp.setBounds(boundRect);

		/**
		 * rescale the icons if any
		 */
		if (curComp != null && curComp instanceof AbstractButton) {
			AbstractButton but = ((AbstractButton) curComp);

			Icon curIcon = but.getIcon();

			boolean displaySingle = false;
			if (this.displayView == Display.SINGLE_PAGE || this.displayView == Display.NODISPLAY) {
				displaySingle = true;
			}

			int combinedRotation = rotate;
			if (curIcon instanceof FixImageIcon) ((FixImageIcon) curIcon).setAttributes(curComp.getWidth(), curComp.getHeight(), combinedRotation,
					displaySingle);
			else
				if (curIcon instanceof ReadOnlyTextIcon) ((ReadOnlyTextIcon) curIcon).setAttributes(curComp.getWidth(), curComp.getHeight(),
						combinedRotation, displaySingle);

			curIcon = but.getPressedIcon();
			if (curIcon instanceof FixImageIcon) ((FixImageIcon) curIcon).setAttributes(curComp.getWidth(), curComp.getHeight(), combinedRotation,
					displaySingle);

			curIcon = but.getSelectedIcon();
			if (curIcon instanceof FixImageIcon) ((FixImageIcon) curIcon).setAttributes(curComp.getWidth(), curComp.getHeight(), combinedRotation,
					displaySingle);

			curIcon = but.getRolloverIcon();
			if (curIcon instanceof FixImageIcon) ((FixImageIcon) curIcon).setAttributes(curComp.getWidth(), curComp.getHeight(), combinedRotation,
					displaySingle);

			curIcon = but.getRolloverSelectedIcon();
			if (curIcon instanceof FixImageIcon) ((FixImageIcon) curIcon).setAttributes(curComp.getWidth(), curComp.getHeight(), combinedRotation,
					displaySingle);

		}
	}

	/**
	 * we take in curComp as it could be a JTextField showing the selected value from a JComboBox also the scale and rotation could be from a print
	 * routine and not the same as the global variables
	 */
	private void recalcFontSize(float scale, int rotate, int id, final Component curComp) {

		/**
		 * get form Object
		 */
		FormObject formObject = (FormObject) this.rawFormData.get(this.convertFormIDtoRef.get(id));

		int rawSize = formObject.getTextSize();

		if (rawSize == -1) rawSize = 0;// change -1 to best fit so that text is more visible

		if (rawSize == 0) {// best fit

			// work out best size for bounding box of object
			Rectangle bounds = formObject.getBoundingRectangle();

			int width = bounds.width;
			int height = bounds.height;

			if (rotate == 90 || rotate == 270) {
				int tmp = height;
				height = width;
				width = tmp;
			}

			rawSize = (int) (height * 0.85);

			if (curComp instanceof JTextArea) {
				String text = ((JTextArea) curComp).getText();
				rawSize = calculateFontSize(height, width, true, text);

			}
			else
				if (curComp instanceof JTextField) {
					String text = ((JTextComponent) curComp).getText();
					rawSize = calculateFontSize(height, width, false, text);

				}
				else
					if (curComp instanceof JButton) {
						String text = ((JButton) curComp).getText();
						if (text != null) {
							rawSize = calculateFontSize(height, width, false, text);
						}
					}
					else
						if (curComp instanceof JList) {
							int count = ((JList) curComp).getModel().getSize() + 2;
							rawSize = rawSize / count;
						}
		}

		int size = (int) (rawSize * scale);
		if (size < 1) {
			size = 1;
		}

		Font resetFont = curComp.getFont();
		Font newFont = new Font(resetFont.getFontName(), resetFont.getStyle(), size);

		curComp.setFont(newFont);
	}

	/** returns Border as is swing specific class */
	@Override
	public Object generateBorderfromForm(FormObject form, float scaling) {
		float[] BC = form.getDictionary(PdfDictionary.MK).getFloatArray(PdfDictionary.BC);
		if (BC == null && form.getParameterConstant(PdfDictionary.Subtype) == PdfDictionary.Screen) BC = form.getFloatArray(PdfDictionary.C);

		Border newBorder = JPedalBorderFactory.createBorderStyle(form.getDictionary(PdfDictionary.BS), FormObject.generateColor(BC), Color.white,
				scaling);
		return newBorder;
	}

	private int[] cropComponent(float[] box, int curPage, float s, int r, int i, boolean redraw) {

		// NOTE if needs adding in ULC check SpecialOptions.SINGLE_PAGE
		if (this.displayView != Display.SINGLE_PAGE && this.displayView != Display.NODISPLAY) r = (r + this.pageData.getRotation(curPage)) % 360;

		int cropX = this.pageData.getCropBoxX(curPage);
		int cropY = this.pageData.getCropBoxY(curPage);
		int cropW = this.pageData.getCropBoxWidth(curPage);

		int mediaW = this.pageData.getMediaBoxWidth(curPage);
		int mediaH = this.pageData.getMediaBoxHeight(curPage);

		int cropOtherX = (mediaW - cropW - cropX);

		float x100, y100, w100, h100;
		int x = 0, y = 0, w = 0, h = 0;

		{
			switch (r) {
				case 0:

					x100 = box[0];
					// if we are drawing on screen take off cropX if printing or extracting we dont need to do this.
					if (redraw) x100 -= cropX;

					y100 = mediaH - box[3] - this.cropOtherY[curPage];
					w100 = (box[2] - box[0]);
					h100 = (box[3] - box[1]);

					x = (int) ((x100) * s);
					y = (int) ((y100) * s);
					w = (int) (w100 * s);
					h = (int) (h100 * s);

					break;
				case 90:

					// new hopefully better routine
					x100 = box[1] - cropY;
					y100 = box[0] - cropX;
					w100 = (box[3] - box[1]);
					h100 = (box[2] - box[0]);

					x = (int) ((x100) * s);
					y = (int) ((y100) * s);
					w = (int) (w100 * s);
					h = (int) (h100 * s);

					break;
				case 180:

					// new hopefully better routine
					w100 = box[2] - box[0];
					h100 = box[3] - box[1];
					y100 = box[1] - cropY;
					x100 = mediaW - box[2] - cropOtherX;

					x = (int) ((x100) * s);
					y = (int) ((y100) * s);
					w = (int) (w100 * s);
					h = (int) (h100 * s);

					break;
				case 270:

					// new hopefully improved routine
					w100 = (box[3] - box[1]);
					h100 = (box[2] - box[0]);
					x100 = mediaH - box[3] - this.cropOtherY[curPage];
					y100 = mediaW - box[2] - cropOtherX;

					x = (int) ((x100) * s);
					y = (int) ((y100) * s);
					w = (int) (w100 * s);
					h = (int) (h100 * s);

					break;
			}/**/
		}
		return new int[] { x, y, w, h };
	}

	/**
	 * used to flush/resize data structures on new document/page
	 * 
	 * @param formCount
	 * @param pageCount
	 * @param keepValues
	 */
	@Override
	public boolean resetComponents(int formCount, int pageCount, boolean keepValues) {
		// System.out.println("count="+formCount);

		if (!super.resetComponents(formCount, pageCount, keepValues))
		// if return false then we already have enough forms
		return false;

		if (!keepValues) {
			this.popups = new Component[0];
		}

		return true;
	}

	/**
	 * used to remove all components from display
	 */
	@Override
	public void removeAllComponentsFromScreen() {

		// 01032012 be care if you ever re-enable as big performace hit on Abacus code (see slow.pdf)

		// Iterator formIter = rawFormData.values().iterator();
		// while(formIter.hasNext()){
		// FormObject formObj = (FormObject)formIter.next();
		// pdfDecoder.getFormRenderer().getActionHandler().PI(formObj,PdfDictionary.AA);
		// pdfDecoder.getFormRenderer().getActionHandler().PI(formObj,PdfDictionary.A);
		// pdfDecoder.getFormRenderer().getActionHandler().PC(formObj,PdfDictionary.AA);
		// pdfDecoder.getFormRenderer().getActionHandler().PC(formObj,PdfDictionary.A);
		// }

		if (this.panel != null) {
			if (SwingUtilities.isEventDispatchThread()) this.panel.removeAll();
			else {
				final Runnable doPaintComponent = new Runnable() {
					@Override
					public void run() {
						SwingData.this.panel.removeAll();
					}
				};
				SwingUtilities.invokeLater(doPaintComponent);
			}
		}
	}

	/**
	 * pass in object components drawn onto
	 * 
	 * @param rootComp
	 */
	@Override
	public void setRootDisplayComponent(final Object rootComp) {
		if (SwingUtilities.isEventDispatchThread()) this.panel = (JPanel) rootComp;
		else {
			final Runnable doPaintComponent = new Runnable() {
				@Override
				public void run() {
					SwingData.this.panel = (JPanel) rootComp;
				}
			};
			SwingUtilities.invokeLater(doPaintComponent);
		}
	}

	@Override
	protected Object checkGUIObjectResolved(int formNum) {

		FormObject formObject = (FormObject) this.rawFormData.get(this.convertFormIDtoRef.get(formNum));

		Object comp = null;
		if (formObject != null) {
			comp = formObject.getGUIComponent();
		}
		if (formObject != null && comp == null) {

			if (formObject != null) {
				comp = resolveGUIComponent(formObject, formNum);

				if (comp != null) {
					setGUIComp(formObject, formNum, comp);
				}
			}
		}

		return comp;
	}

	private Component resolveGUIComponent(FormObject formObject, int formNum) {

		if (formObject == null) return null;

		FormFactory formFactory = this.pdfDecoder.getFormRenderer().getFormFactory();

		Integer widgetType; // no value set

		Object retComponent;

		int subtype = formObject.getParameterConstant(PdfDictionary.Subtype);// FT

		// flags used to alter interactivity of all fields
		boolean readOnly, required, noexport;

		boolean[] flags = formObject.getFieldFlags();// Ff
		if (flags != null) {
			// noinspection UnusedAssignment
			readOnly = flags[FormObject.READONLY_ID];
			// noinspection UnusedAssignment
			required = flags[FormObject.REQUIRED_ID];
			// noinspection UnusedAssignment
			noexport = flags[FormObject.NOEXPORT_ID];
		}

		/** setup field */
		if (subtype == PdfDictionary.Btn) {// ----------------------------------- BUTTON ----------------------------------------

			boolean isPushButton = false, isRadio = false;// hasNoToggleToOff = false, radioinUnison = false;
			if (flags != null) {
				isPushButton = flags[FormObject.PUSHBUTTON_ID];
				isRadio = flags[FormObject.RADIO_ID];
			}

			if (isPushButton) {

				widgetType = FormFactory.PUSHBUTTON;
				retComponent = formFactory.pushBut(formObject);

			}
			else
				if (isRadio) {
					widgetType = FormFactory.RADIOBUTTON;
					retComponent = formFactory.radioBut(formObject);
				}
				else {
					widgetType = FormFactory.CHECKBOXBUTTON;
					retComponent = formFactory.checkBoxBut(formObject);
				}

		}
		else {
			if (subtype == PdfDictionary.Tx) { // ----------------------------------------------- TEXT --------------------------------------

				boolean isMultiline = false, hasPassword = false;// doNotScroll = false, richtext = false, fileSelect = false, doNotSpellCheck =
																	// false;
				if (flags != null) {
					isMultiline = flags[FormObject.MULTILINE_ID];
					hasPassword = flags[FormObject.PASSWORD_ID];
				}

				if (isMultiline) {

					if (hasPassword) {

						widgetType = FormFactory.MULTILINEPASSWORD;
						retComponent = formFactory.multiLinePassword(formObject);

					}
					else {

						widgetType = FormFactory.MULTILINETEXT;
						retComponent = formFactory.multiLineText(formObject);

					}
				}
				else {// singleLine

					if (hasPassword) {

						widgetType = FormFactory.SINGLELINEPASSWORD;
						retComponent = formFactory.singleLinePassword(formObject);

					}
					else {

						widgetType = FormFactory.SINGLELINETEXT;
						retComponent = formFactory.singleLineText(formObject);

					}
				}
			}
			else
				if (subtype == PdfDictionary.Ch) {// ----------------------------------------- CHOICE ----------------------------------------------

					boolean isCombo = false;// multiSelect = false, sort = false, isEditable = false, doNotSpellCheck = false, comminOnSelChange =
											// false;
					if (flags != null) {
						isCombo = flags[FormObject.COMBO_ID];
					}

					if (isCombo) {// || (type==XFAFORM && ((XFAFormObject)formObject).choiceShown!=XFAFormObject.CHOICE_ALWAYS)){

						widgetType = FormFactory.COMBOBOX;
						retComponent = formFactory.comboBox(formObject);

					}
					else {// it is a list

						widgetType = FormFactory.LIST;
						retComponent = formFactory.listField(formObject);
					}
				}
				else
					if (subtype == PdfDictionary.Sig) {

						widgetType = FormFactory.SIGNATURE;
						retComponent = formFactory.signature(formObject);

					}
					else {// assume annotation if (formType == ANNOTATION) {

						widgetType = FormFactory.ANNOTATION;
						retComponent = formFactory.annotationButton(formObject);

					}
		}

		// set Component specific values such as Tooltip and mouse listener
		completeField(formObject, formNum, -widgetType, retComponent, this.currentPdfFile);

		formObject.setGUIComponent(retComponent);

		return (Component) retComponent;
	}

	@Override
	public void setGUIComp(FormObject formObject, int formNum, Object rawField) {

		Component retComponent = (Component) rawField;

		final int formPage = formObject.getPageNumber();

		// append state to name so we can retrieve later if needed
		String name2 = formObject.getTextStreamValue(PdfDictionary.T);
		if (name2 != null) {// we have some empty values as well as null
			String stateToCheck = formObject.getNormalOnState();
			if (stateToCheck != null && stateToCheck.length() > 0) name2 = name2 + "-(" + stateToCheck + ')';

			retComponent.setName(name2);
		}

		if (formObject.getParameterConstant(PdfDictionary.Subtype) == PdfDictionary.Popup) {
			// Hide popup for now (this is an icon for the popup window)
			// Will add support for this later
			retComponent.setVisible(false);
		}

		// make visible
		scaleComponent(formPage, this.displayScaling, this.rotation, formNum, retComponent, true, false, this.indent);
	}

	/**
	 * alter location and bounds so form objects show correctly scaled
	 */
	@Override
	public void resetScaledLocation(final float currentScaling, final int currentRotation, final int currentIndent) {

		// we get a spurious call in linux resulting in an exception
		if (this.trackPagesRendered == null) return;

		// only if necessary
		if (this.forceRedraw || currentScaling != this.lastScaling || currentRotation != this.oldRotation || currentIndent != this.oldIndent) {

			this.oldRotation = currentRotation;
			this.lastScaling = currentScaling;
			this.oldIndent = currentIndent;
			this.forceRedraw = false;

			int currentComp;

			// fix rescale issue on Testfeld
			if (this.startPage < this.trackPagesRendered.length) {
				currentComp = this.trackPagesRendered[this.startPage];// startID;
			}
			else {
				currentComp = 0;
			}

			// draw popups on the screen
			if (this.panel != null) {
				if (SwingUtilities.isEventDispatchThread()) {
					for (int i = 0; i < this.popups.length; i++) {
						scaleComponent(this.currentPage, currentScaling, currentRotation, i, this.popups[i], true, true, this.indent);
						this.panel.add(this.popups[i]);
					}
				}
				else {
					final Runnable doPaintComponent = new Runnable() {
						@Override
						public void run() {
							for (int i = 0; i < SwingData.this.popups.length; i++) {
								scaleComponent(SwingData.this.currentPage, currentScaling, currentRotation, i, SwingData.this.popups[i], true, true,
										SwingData.this.indent);
								SwingData.this.panel.add(SwingData.this.popups[i]);
							}

						}
					};
					SwingUtilities.invokeLater(doPaintComponent);
				}
			}

			// reset all locations
			if (this.currentPage > 0 && currentComp != -1 && this.nextFreeField + 1 > currentComp) {

				checkGUIObjectResolved(currentComp);

				FormObject formObject = (FormObject) this.rawFormData.get(this.convertFormIDtoRef.get(currentComp));

				if (formObject != null) {
					Component rawComp = (Component) formObject.getGUIComponent();

					// //Remove duplicate code
					// Component rawComp= null;
					//
					// if(formObject!=null){
					// rawComp= (Component) formObject.getGUIComponent();
					// }
					// just put on page, allowing for no values (last one alsways empty as array 1 too big
					// while(pageMap[currentComp]==currentPage){
					while (formObject != null && currentComp < this.nextFreeField + 1 && currentComp > -1
							&& formObject.getPageNumber() >= this.startPage && formObject.getPageNumber() < this.endPage && rawComp != null) {

						// System.out.println("added"+currentComp);
						// while(currentComp<pageMap.length){//potential fix to help currentRotation
						if (this.panel != null) {// && !(allFields[currentComp] instanceof JList))

							Rectangle bounds = formObject.getBoundingRectangle();

							if (rawComp instanceof JList && bounds.height < rawComp.getPreferredSize().height) {

								JList comp = (JList) rawComp;

								rawComp = wrapComponentInScrollPane(comp);
								formObject.setGUIComponent(comp);

								// ensure visible (do it before we add)
								int index = comp.getSelectedIndex();
								if (index > -1) comp.ensureIndexIsVisible(index);

							}

							if (SwingUtilities.isEventDispatchThread()) {

								this.panel.remove(rawComp);

								scaleComponent(formObject.getPageNumber(), currentScaling, currentRotation, currentComp, rawComp, true, false,
										this.indent);

								this.panel.add(rawComp);
							}
							else {
								final Component finalComp = rawComp;
								final int pageID = formObject.getPageNumber();
								final int id = currentComp;

								final Runnable doPaintComponent = new Runnable() {
									@Override
									public void run() {

										SwingData.this.panel.remove(finalComp);
										scaleComponent(pageID, currentScaling, currentRotation, id, finalComp, true, false, SwingData.this.indent);

										SwingData.this.panel.add(finalComp);
									}
								};
								SwingUtilities.invokeLater(doPaintComponent);
							}
						}

						currentComp++;

						formObject = (FormObject) this.rawFormData.get(this.convertFormIDtoRef.get(currentComp));
						if (formObject != null) {
							rawComp = (Component) formObject.getGUIComponent();
						}
					}
				}
			}
		}
	}

	private static Component wrapComponentInScrollPane(JList comp) {

		JScrollPane scroll = new JScrollPane(comp);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroll.setLocation(comp.getLocation());
		scroll.setPreferredSize(comp.getPreferredSize());
		scroll.setSize(comp.getSize());

		return scroll;
	}

	@Override
	public void displayComponent(int currentComp, FormObject formObject, Object comp, int startPage, int page) {

		if (SwingUtilities.isEventDispatchThread()) {

			scaleComponent(formObject.getPageNumber(), this.displayScaling, this.rotation, currentComp, (Component) comp, true, false, this.indent);

		}
		else {
			final int id = currentComp;
			final int pageID = formObject.getPageNumber();
			final Component comp1 = (Component) comp;

			final Runnable doPaintComponent = new Runnable() {
				@Override
				public void run() {
					scaleComponent(pageID, SwingData.this.displayScaling, SwingData.this.rotation, id, comp1, true, false, SwingData.this.indent);
				}
			};
			SwingUtilities.invokeLater(doPaintComponent);
		}
	}

	/**
	 * called by print and display methods to align all fields linked to the specified field.
	 */
	@Override
	public void syncKidValues(int currentComp) {

		// get the form name for this field
		String ref = this.convertIDtoRef(currentComp);
		FormObject form = (FormObject) this.rawFormData.get(ref);
		String name = form.getTextStreamValue(PdfDictionary.T);

		if (this.LastValueChanged.containsKey(name)) {
			syncFormsByName(name);
			this.LastValueChanged.remove(name);
		}
	}

	/**
	 * ensure all kid values sync across all pages before accessing data - only call if you are using Viewer and want to read component values
	 */
	@Override
	public void syncAllValues() {

		for (Object o : this.LastValueByName.keySet()) {
			syncFormsByName((String) o);
		}
	}

	/**
	 * goes through all forms with the given name, ie a single grouped set of forms and sets the current values to all forms depending on if they are
	 * a button group or if they are just linked text fields
	 */
	private void syncFormsByName(String name) {
		// make sure we only work with values we have setup
		if (!this.LastValueByName.containsKey(name)) return;

		Object lastMapValue = this.LastValueByName.get(name);

		// make sure we have all forms decoded so we can sync them all.
		try {
			this.pdfDecoder.getFormRenderer().getComponentNameList(); // decode all pages
		}
		catch (PdfException e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}

		Object[] forms = getRawForm(name);
		for (Object form1 : forms) {
			if (form1 == null) continue;

			FormObject form = (FormObject) form1;
			Object index = this.refToCompIndex.get(form.getObjectRefAsString());
			if (index == null) continue;

			int currentComp = (Integer) index;

			FormObject formObject = (FormObject) this.rawFormData.get(this.convertFormIDtoRef.get(currentComp));
			int type = formObject.getFormType();

			checkGUIObjectResolved(currentComp);

			Component comp = (Component) formObject.getGUIComponent();

			if (type == FormFactory.radiobutton || type == FormFactory.checkboxbutton) {

				JToggleButton checkComp = ((JToggleButton) comp);
				String currOnState = form.getNormalOnState();
				// we know parents are the same
				if ((currOnState == null && lastMapValue == null) || (currOnState != null && currOnState.equals(lastMapValue))) {
					// onstates and parents are the same this is treated as the same
					// we need to make sure we are selected.
					if (!checkComp.isSelected()) {
						checkComp.setSelected(true);
						Icon icn = checkComp.getPressedIcon();
						if (icn != null && icn instanceof FixImageIcon) ((FixImageIcon) icn).swapImage(true);
					}
				}
				else {
					// we need to make sure we are deselected.
					if (checkComp.isSelected()) {
						checkComp.setSelected(false);
						Icon icn = checkComp.getPressedIcon();
						if (icn != null && icn instanceof FixImageIcon) ((FixImageIcon) icn).swapImage(false);
					}
				}
			}
			else
				if (type == FormFactory.combobox) {

					JComboBox combo = ((JComboBox) comp);
					if (combo.getSelectedItem() == null) {
						if (lastMapValue != null) {
							combo.setSelectedItem(lastMapValue);
						}
					}
					else
						if (!combo.getSelectedItem().equals(lastMapValue)) {
							combo.setSelectedItem(lastMapValue);
						}
				}
				else
					if (type == FormFactory.singlelinepassword || type == FormFactory.singlelinetext || type == FormFactory.multilinepassword
							|| type == FormFactory.multilinetext) {

						String text = null;
						if (comp instanceof JTextComponent) text = ((JTextComponent) comp).getText();
						else
							if (comp instanceof JButton) text = ((ReadOnlyTextIcon) ((JButton) comp).getIcon()).getText();

						if (lastMapValue == null) {
							if (text != null) {
								if (comp instanceof JTextComponent) ((JTextComponent) comp).setText(null);
								else
									if (comp instanceof JButton) ((ReadOnlyTextIcon) ((JButton) comp).getIcon()).setText("");
							}
						}
						else
							if (!lastMapValue.equals(text)) {
								if (comp instanceof JTextComponent) ((JTextComponent) comp).setText(lastMapValue.toString());
								else
									if (comp instanceof JButton) ((ReadOnlyTextIcon) ((JButton) comp).getIcon()).setText(lastMapValue.toString());
							}
					}
		}// END loop round forms by name
	}

	/**
	 * tell user about Javascript validation error
	 * 
	 * @param code
	 * @param args
	 */
	@Override
	public void reportError(int code, Object[] args) {

		if (!PdfDecoder.showErrorMessages) return;

		// tell user
		if (code == ErrorCodes.JSInvalidFormat) {
			JOptionPane.showMessageDialog(this.panel, "The values entered does not match the format of the field [" + args[0] + " ]",
					"Warning: Javascript Window", JOptionPane.INFORMATION_MESSAGE);
		}
		else
			if (code == ErrorCodes.JSInvalidDateFormat) JOptionPane.showMessageDialog(this.panel,
					"Invalid date/time: please ensure that the date/time exists. Field [" + args[0] + " ] should match format " + args[1],
					"Warning: Javascript Window", JOptionPane.INFORMATION_MESSAGE);
			else
				if (code == ErrorCodes.JSInvalidRangeFormat) {

					JOptionPane.showMessageDialog(this.panel, args[1], "Warning: Javascript Window", JOptionPane.INFORMATION_MESSAGE);
				}
				else JOptionPane.showMessageDialog(this.panel, "The values entered does not match the format of the field",
						"Warning: Javascript Window", JOptionPane.INFORMATION_MESSAGE);
	}

	/** repaints the specified form or all forms if null is sent in */
	@Override
	public void invalidate(String name) {

		Object[] forms = getComponentsByName(name);

		if (forms == null) return;

		for (Object form : forms) {
			if (form != null) {
				((Component) form).repaint();
			}
		}
	}

	@Override
	public void storeDisplayValue(String fieldRef) {
		Object compIndex = this.refToCompIndex.get(fieldRef);
		if (compIndex == null) return;

		int index = (Integer) compIndex;
		FormObject form = (FormObject) this.rawFormData.get(fieldRef);
		Component comp = (Component) checkGUIObjectResolved(index);

		FormObject formObject = (FormObject) this.rawFormData.get(this.convertFormIDtoRef.get(index));
		int type = formObject.getFormType();

		switch (type) {

			case FormFactory.combobox:
				form.setSelectedItem((String) ((JComboBox) comp).getSelectedItem());
				break;

			case FormFactory.list:
				form.setTopIndex(((JList) comp).getSelectedIndices());
				break;

			case FormFactory.radiobutton:
				JRadioButton radioBut = ((JRadioButton) comp);
				if (radioBut.isSelected()) {
					form.setChildOnState(FormUtils.removeStateToCheck(radioBut.getName(), true));
				}
				break;

			case FormFactory.checkboxbutton:
				JCheckBox checkBox = ((JCheckBox) comp);
				if (checkBox.isSelected()) {
					form.setCurrentState(FormUtils.removeStateToCheck(checkBox.getName(), true));
				}
				break;

			case FormFactory.singlelinepassword:
				form.setTextValue(((JTextComponent) comp).getText());
				break;

			case FormFactory.multilinepassword:
				form.setTextValue(((JTextComponent) comp).getText());
				break;

			case FormFactory.singlelinetext:
				// these fields have readonlytexticons(JButtons) associated sometimes so allow for.
				if (comp instanceof JTextComponent) { // if JButton read only ignore
					form.setTextValue(((JTextComponent) comp).getText());
				}
				break;

			case FormFactory.multilinetext:
				// these fields have readonlytexticons(JButtons) associated sometimes so allow for.
				if (comp instanceof JTextComponent) { // if JButton read only ignore
					form.setTextValue(((JTextComponent) comp).getText());
				}
				break;
		// }else if(allFieldsType[index]==FormFactory.pushbutton || allFieldsType[index]==FormFactory.annotation ||
		// allFieldsType[index]==FormFactory.signature){
		}
	}

	/** finds the display field of the defined form reference and changes its visibility as needed */
	@Override
	public void setCompVisible(String ref, boolean visible) {
		Integer checkObj = convertRefToID(ref);

		if (checkObj == null) return;

		((Component) checkGUIObjectResolved(checkObj)).setVisible(visible);
	}

	@Override
	public void popup(FormObject formObj, PdfObjectReader currentPdfFile) {
		if (ActionHandler.drawPopups) {
			// popup needs to be stored for each field, as method A() is static,
			// and we need a seperate popup for each field.
			JComponent popup;

			if (formObj.isPopupBuilt()) {
				popup = (JComponent) formObj.getPopupObj();

			}
			else {
				PdfObject popupObj = formObj.getDictionary(PdfDictionary.Popup);
				currentPdfFile.checkResolved(popupObj);

				if (popupObj == null) {
					popupObj = new FormObject();
					((FormObject) popupObj).copyInheritedValuesFromParent(formObj);
					((FormObject) popupObj).setParent(formObj.getObjectRefAsString());
					// dont set the parent object as this is a copy of the same object
				}

				popup = new PdfSwingPopup(formObj, popupObj, this.pageData.getCropBoxWidth(this.currentPage));

				// copy current popup bounds array so we can add new bounds to new index
				float[][] tmpf = new float[this.popupBounds.length + 1][4];
				System.arraycopy(this.popupBounds, 0, tmpf, 0, this.popupBounds.length);

				// get rectangle for new popup
				tmpf[this.popupBounds.length] = popupObj.getFloatArray(PdfDictionary.Rect);
				this.popupBounds = tmpf;

				// copy current popups array so we can add a new one to end
				JComponent[] tmp = new JComponent[this.popups.length + 1];
				System.arraycopy(this.popups, 0, tmp, 0, this.popups.length);

				// add new popup to end of popup array
				tmp[this.popups.length] = popup;
				this.popups = tmp;

				formObj.setPopupBuilt(popup);

				// draw the popup on screen for the first time
				popup.setVisible(popupObj.getBoolean(PdfDictionary.Open));

				// rescale the components to that the popup bounds are scaled to the current display
				this.forceRedraw = true;
				resetScaledLocation(this.displayScaling, this.rotation, this.indent);
				this.panel.repaint();
			}

			if (popup.isVisible()) {
				popup.setVisible(false);
			}
			else {
				popup.setVisible(true);
			}
		}
	}

	/** sets the text color for the specified swing component */
	@Override
	public void setTextColor(String ref, Color textColor) {

		Integer checkObj = convertRefToID(ref);

		if (checkObj == null) return;

		// set the text color
		Object comp = checkGUIObjectResolved(checkObj);
		if (comp != null) {
			((Component) comp).setForeground(textColor);
		}
	}

	@Override
	public void setCustomPrintInterface(CustomFormPrint customFormPrint) {
		this.customFormPrint = customFormPrint;
	}

	/**
	 * you can now send in the formobject and this will return the super form type ie ComponentData.TEXT_TYPE, ComponentData.LIST_TYPE (list, combo)
	 * or ComponentData.BUTTON_TYPE (sign,annot,radio,check,push)
	 */
	@Override
	public int getFieldType(Object comp) {

		if (comp instanceof FormObject) {
			return super.getFieldType(comp);
		}
		else
			if (comp instanceof JTextField || comp instanceof JTextArea || comp instanceof JPasswordField) {
				return TEXT_TYPE;
			}
			else
				if (comp instanceof JRadioButton || comp instanceof JCheckBox || comp instanceof JButton) {
					return BUTTON_TYPE;
				}
				else
					if (comp instanceof JList || comp instanceof JComboBox) {
						return LIST_TYPE;
					}
					else {
						return UNKNOWN_TYPE;
					}
	}

	// store last used value so we can align if kids
	@Override
	public void flagLastUsedValue(Object component, FormObject formObject, boolean sync) {

		// get the component
		Component comp = (Component) component;
		String parent = formObject.getStringKey(PdfDictionary.Parent);
		String name = formObject.getTextStreamValue(PdfDictionary.T);

		// if it has a parent, stor elast value in parent so others can sync to it
		if (parent != null) {// && formObject.isKid()){

			if (comp instanceof JComboBox) {
				this.LastValueByName.put(name, ((JComboBox) comp).getSelectedItem());
				this.LastValueChanged.put(name, null);

			}
			else
				if (comp instanceof JTextComponent) {
					this.LastValueByName.put(name, ((JTextComponent) comp).getText());
					this.LastValueChanged.put(name, null);

				}
				else
					if (comp instanceof JToggleButton) { // NOTE WE STORE REF and not value (which is implicit as its a radio button)
						boolean isSelected = ((JToggleButton) comp).isSelected();

						if (isSelected) {
							this.LastValueByName.put(name, formObject.getNormalOnState());
							this.LastValueChanged.put(name, null);
						}
						else
							if (!isSelected) {
								if (this.LastValueByName.get(formObject.getTextStreamValue(PdfDictionary.T)) != null) {
									// if last value is null we dont need to set last value to null as is already.
									String currOnState = formObject.getNormalOnState();
									// we know parents are the same
									if (this.LastValueByName.get(formObject.getTextStreamValue(PdfDictionary.T)).equals(currOnState)) {
										// onstates and parents are the same this is treated as the same
										if (formObject.getFieldFlags()[FormObject.NOTOGGLETOOFF_ID]) {
											// if this was the last value and we cannot turn off, turn back on
											// dont turn it off.
											((JToggleButton) comp).setSelected(true);
											Icon icn = ((JToggleButton) comp).getPressedIcon();
											if (icn != null && icn instanceof FixImageIcon) ((FixImageIcon) icn).swapImage(true);
										}
										else {
											// last value was this and we can toggle all off, store turned off
											this.LastValueChanged.put(name, null);
										}
									}
									// if last value is not this, we dont save anything as we dont know what is set here
								}
							}
					}
		}
		else {// If No Parent
			if (comp instanceof JToggleButton) { // NOTE WE STORE REF and not value (which is implicit as its a radio button)

				JToggleButton radioComp = ((JToggleButton) comp);
				if (!radioComp.isSelected() && formObject.getFieldFlags()[FormObject.NOTOGGLETOOFF_ID]) {
					// dont turn it off.
					radioComp.setSelected(true);
					Icon icn = radioComp.getPressedIcon();
					if (icn != null && icn instanceof FixImageIcon) ((FixImageIcon) icn).swapImage(true);
				}
				// NOTE if allowed to toggle to off, then it will do through java
			}
		}

		if (sync) {
			// NOTE sync by name as we have it already and using index just gets name by reading a lod of maps.
			syncFormsByName(FormUtils.removeStateToCheck(comp.getName(), false));
		}
	}

	@Override
	public void setCompVisible(Object comp, boolean visible) {
		((Component) comp).setVisible(visible);
	}
}
