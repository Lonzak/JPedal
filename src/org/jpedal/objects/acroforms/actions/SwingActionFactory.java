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
 * SwingActionFactory.java
 * ---------------
 */
package org.jpedal.objects.acroforms.actions;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import javax.swing.text.JTextComponent;

import org.jpedal.PdfDecoder;
import org.jpedal.SingleDisplay;
import org.jpedal.exception.PdfException;
import org.jpedal.io.PdfObjectReader;
import org.jpedal.objects.acroforms.actions.privateclasses.FieldsHideObject;
import org.jpedal.objects.acroforms.gui.Summary;
import org.jpedal.objects.acroforms.overridingImplementations.FixImageIcon;
import org.jpedal.objects.acroforms.overridingImplementations.ReadOnlyTextIcon;
import org.jpedal.objects.acroforms.rendering.AcroRenderer;
import org.jpedal.objects.acroforms.utils.FormUtils;
import org.jpedal.objects.raw.FormObject;
import org.jpedal.objects.raw.FormStream;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.utils.BrowserLauncher;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.StringUtils;
import org.jpedal.utils.Strip;

public class SwingActionFactory implements ActionFactory {

	AcroRenderer acrorend;

	PdfDecoder decode_pdf = null;

	@Override
	public void showMessageDialog(String s) {
		JOptionPane.showMessageDialog(this.decode_pdf, s);
	}

	/**
	 * pick up key press or return ' '
	 */
	@Override
	public char getKeyPressed(Object raw) {

		try {
			ComponentEvent ex = (ComponentEvent) raw;

			if (ex instanceof KeyEvent) return ((KeyEvent) ex).getKeyChar();
			else return ' ';

		}
		catch (Exception ee) {
			System.out.println("Exception " + ee);
		}

		return ' ';
	}

	/**
	 * shows and hides the appropriate fields as defined within the map defined
	 * 
	 * @param fieldToHide - the field names to which we want to hide
	 */
	@Override
	public void setFieldVisibility(FieldsHideObject fieldToHide) {

		String[] fieldsToHide = fieldToHide.getFieldArray();
		boolean[] whetherToHide = fieldToHide.getHideArray();

		if (fieldsToHide.length != whetherToHide.length) {
			// this will exit internally only and the production version will carry on regardless.
			LogWriter.writeFormLog("{custommouselistener} number of fields and nuber of hides or not the same", FormStream.debugUnimplemented);
			return;
		}

		for (int i = 0; i < fieldsToHide.length; i++) {
			this.acrorend.getCompData().hideComp(fieldsToHide[i], !whetherToHide[i]);
		}
	}

	@Override
	public void print() {}

	// Map of components marked for reseting
	Map resetCalled = new HashMap();

	@Override
	public void reset(String[] aFields) {
		// note which fields are being reset
		if (aFields == null) {
			if (this.resetCalled.get("null") != null) return;
			this.resetCalled.put("null", "1");
		}
		else {
			for (int i = 0; i < aFields.length; i++) {

				// Ignores component is already marked for reset
				if (this.resetCalled.get(aFields[i]) != null) {
					// Remove component from list to reset as already present in resetCalled
					aFields = StringUtils.remove(aFields, i);

					// decrement i otherwise we miss one field out
					i--;
				}
				else {
					// Mark component is being reset
					this.resetCalled.put(aFields[i], "1");
				}
			}

			// If nothing left, ignore
			if (aFields.length == 0) return;
		}

		// Reset all components raw values
		this.acrorend.getCompData().reset(aFields);

		// Reset all components fields
		resetComp(aFields);

		// Reset finished, remove field from map
		if (aFields == null) {
			this.resetCalled.remove("null");
		}
		else {
			for (String aField : aFields) {
				this.resetCalled.remove(aField);
			}
		}
	}

	/** reset all the specified fields or all fields if null is specified */
	private void resetComp(String[] aFields) {

		Component[] allFields;

		// If aFields is null get all components
		if (aFields == null) {
			allFields = (Component[]) this.acrorend.getComponentsByName(null);
		}
		else {
			// Only reset components passed in
			Component[][] comps = new Component[aFields.length][];
			int count = 0;
			for (int i = 0; i < aFields.length; i++) {
				// Get all components with the given name
				comps[i] = (Component[]) this.acrorend.getComponentsByName(aFields[i]);
				count += comps[i].length;
			}

			// Add all components into a single array
			allFields = new Component[count];
			int f = 0;
			for (Component[] comp : comps) {
				for (Component aComp : comp) {
					allFields[f++] = aComp;
				}
			}
		}

		// nothing to do
		if (allFields == null) {
			return;
		}

		for (int i = 0; i < allFields.length; i++) {
			if (allFields[i] != null) {// && defaultValues[i]!=null){
				String name = FormUtils.removeStateToCheck(allFields[i].getName(), false);
				String ref = this.acrorend.getCompData().getnameToRef(name);// or use getIndexFromName and then convetIDtoRef
				String state = FormUtils.removeStateToCheck(allFields[i].getName(), true);

				// Point in defaultValue array
				int index;
				if (aFields == null)
				// we are resetting all the forms so go through in order
				index = i;
				else {
					// we are resetting only defined forms so get the index of the values
					// If handling less than all the fields we need to find the
					// index for this field in the list of all components
					index = this.acrorend.getCompData().getIndexFromName(name);
				}

				FormObject formObject = this.acrorend.getCompData().getFormObject(index);

				String defaultValue = formObject.getTextStreamValue(PdfDictionary.DV);
				if (formObject.getValuesMap(true) != null) defaultValue = (String) formObject.getValuesMap(true).get(
						Strip.checkRemoveLeadingSlach(defaultValue));
				else defaultValue = Strip.checkRemoveLeadingSlach(defaultValue);

				if (allFields[i] instanceof JToggleButton) {
					JToggleButton comp = ((JToggleButton) allFields[i]);
					// on/off
					if (defaultValue == null && comp.isSelected()) {
						comp.setSelected(false);
						// reset pressedimages so that they coinside
						Icon icn = comp.getPressedIcon();
						if (icn != null && icn instanceof FixImageIcon) ((FixImageIcon) icn).swapImage(false);
					}
					else {
						String fieldState = state;

						// Check if at the default selection
						if (fieldState.equals(defaultValue)) {
							// If deafult selection is turned off, turn on
							if (!comp.isSelected()) {
								comp.setSelected(true);
								// reset pressedimages so that they coinside
								Icon icn = comp.getPressedIcon();
								if (icn != null && icn instanceof FixImageIcon) ((FixImageIcon) icn).swapImage(true);

							}
						}
						else
							// If not the deafult selection and turned on, turn it off
							if (comp.isSelected()) {
								comp.setSelected(false);
								// reset pressedimages so that they coinside
								Icon icn = comp.getPressedIcon();
								if (icn != null && icn instanceof FixImageIcon) ((FixImageIcon) icn).swapImage(false);
							}
					}
				}
				else
					if (allFields[i] instanceof JTextComponent) {
						this.acrorend.getCompData().setUnformattedValue(ref, defaultValue);
						this.acrorend.getCompData().setLastValidValue(ref, defaultValue);
						this.acrorend.getCompData().setValue(ref, defaultValue, false, false);

					}
					else
						if (allFields[i] instanceof JComboBox) {
							// on/off
							((JComboBox) allFields[i]).setSelectedItem(defaultValue);
						}
						else
							if (allFields[i] instanceof JList) {
								((JList) allFields[i]).setSelectedValue(defaultValue, true);

							}
							else
								if (allFields[i] instanceof JButton) {
									// trap the new readonly text icons for text fields, and reset any that ask to be
									Icon icn = ((JButton) allFields[index]).getIcon();
									if (icn != null && icn instanceof ReadOnlyTextIcon) {
										((ReadOnlyTextIcon) icn).setText(defaultValue);
									}
								}
				this.acrorend.getCompData().flagLastUsedValue(allFields[i],
						(FormObject) this.acrorend.getFormDataAsObject(this.acrorend.getCompData().convertIDtoRef(i))[0], false);

				allFields[i].repaint();

			}
		}

		// sync all after as we are doing a lot together.
		this.acrorend.getCompData().syncAllValues();
	}

	@Override
	public void setPDF(PdfDecoder decode_pdf, AcroRenderer acrorend) {
		this.decode_pdf = decode_pdf;
		this.acrorend = acrorend;
	}

	@Override
	public void setCursor(int eventType) {

		if (this.decode_pdf == null) {
			// do nothing
		}
		else
			if (eventType == ActionHandler.MOUSEENTERED) {
				if (SingleDisplay.allowChangeCursor) this.decode_pdf.setCursor(new Cursor(Cursor.HAND_CURSOR));
			}
			else
				if (eventType == ActionHandler.MOUSEEXITED) {
					if (SingleDisplay.allowChangeCursor) this.decode_pdf.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				}
	}

	@Override
	public void showSig(PdfObject sigObject) {

		JDialog frame = new JDialog(getParentJFrame(this.decode_pdf), "Signature Properties", true);

		Summary summary = new Summary(frame, sigObject);
		summary.setValues(sigObject.getTextStreamValue(PdfDictionary.Name), sigObject.getTextStreamValue(PdfDictionary.Reason),
				sigObject.getTextStreamValue(PdfDictionary.M), sigObject.getTextStreamValue(PdfDictionary.Location));

		frame.getContentPane().add(summary);
		frame.setSize(550, 220);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	private static JFrame getParentJFrame(Component component) {
		while (true) {
			if (component.getParent() == null) return null;

			if (component.getParent() instanceof JFrame) {
				return (JFrame) component.getParent();
			}
			else {
				component = component.getParent();
			}
		}
	}

	/**
	 * @param listOfFields
	 *            - defines a list of fields to either include or exclude from the submit option, Dependent on the <B>flag</b>, if is null all fields
	 *            are submitted.
	 * @param excludeList
	 *            - if true then the listOfFields defines an exclude list, if false the list is an include list, if listOfFields is null then this
	 *            field is ignored.
	 * @param submitURL
	 *            - the URL to submit to.
	 */
	@Override
	public void submitURL(String[] listOfFields, boolean excludeList, String submitURL) {

		if (submitURL != null) {
			Component[] compsToSubmit = new Component[0];
			String[] includeNameList = new String[0];
			if (listOfFields != null) {
				if (excludeList) {
					// listOfFields defines an exclude list
					try {
						java.util.List tmplist = this.acrorend.getComponentNameList();
						if (tmplist != null) {
							for (String listOfField : listOfFields) {
								tmplist.remove(listOfField);
							}
						}
					}
					catch (PdfException e1) {
						LogWriter.writeFormLog("SwingFormFactory.setupMouseListener() get component name list exception",
								FormStream.debugUnimplemented);
					}
				}
				else {
					// fields is an include list
					includeNameList = listOfFields;
				}

				Component[] compsToAdd, tmp;
				for (int i = 0; i < includeNameList.length; i++) {
					compsToAdd = (Component[]) this.acrorend.getComponentsByName(includeNameList[i]);

					if (compsToAdd != null) {
						tmp = new Component[compsToSubmit.length + compsToAdd.length];
						if (compsToAdd.length > 1) {
							LogWriter.writeFormLog("(internal only) SubmitForm multipul components with same name", FormStream.debugUnimplemented);
						}
						for (int k = 0; i < tmp.length; k++) {
							if (k < compsToSubmit.length) {
								tmp[k] = compsToSubmit[k];
							}
							else
								if (k - compsToSubmit.length < compsToAdd.length) {
									tmp[k] = compsToAdd[k - compsToSubmit.length];
								}
						}
						compsToSubmit = tmp;
					}
				}
			}
			else {
				compsToSubmit = (Component[]) this.acrorend.getComponentsByName(null);
			}

			String text = "";
			for (Component aCompsToSubmit : compsToSubmit) {
				if (aCompsToSubmit instanceof JTextComponent) {
					text += ((JTextComponent) aCompsToSubmit).getText();
				}
				else
					if (aCompsToSubmit instanceof AbstractButton) {
						text += ((AbstractButton) aCompsToSubmit).getText();
					}
					else
						if (aCompsToSubmit != null) {
							LogWriter.writeFormLog("(internal only) SubmitForm field form type not accounted for", FormStream.debugUnimplemented);
						}
			}

			try {
				BrowserLauncher.openURL(submitURL + "?en&q=" + text);
			}
			catch (IOException e) {
				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
			}
		}
	}

	@Override
	public Object getHoverCursor() {
		return new MouseListener() {
			@Override
			public void mouseEntered(MouseEvent e) {
				setCursor(ActionHandler.MOUSEENTERED);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				setCursor(ActionHandler.MOUSEEXITED);
			}

			@Override
			public void mouseClicked(MouseEvent e) {}

			@Override
			public void mousePressed(MouseEvent e) {}

			@Override
			public void mouseReleased(MouseEvent e) {}
		};
	}

	@Override
	public void popup(Object raw, FormObject formObj, PdfObjectReader currentPdfFile) {
		if (((MouseEvent) raw).getClickCount() == 2) {
			/**/

			this.acrorend.getCompData().popup(formObj, currentPdfFile);

			// move focus so that the button does not flash
			((JButton) ((MouseEvent) raw).getSource()).setFocusable(false);
		}
	}

	@Override
	public Object getChangingDownIconListener(Object downOff, Object downOn, int rotation) {
		return new SwingDownIconListener();
	}
}
