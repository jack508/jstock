/*
 * JStock - Free Stock Market Software
 * Copyright (C) 2009 Yan Cheng Cheok <yccheok@yahoo.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.yccheok.jstock.gui;

import javax.swing.*;
import java.awt.event.*;
import javax.swing.plaf.ComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import org.yccheok.jstock.engine.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author yccheok
 */
public class AutoCompleteJComboBox extends JComboBox {
    
    /** Creates a new instance of AutoCompleteJComboBox */
    public AutoCompleteJComboBox() {
        this.stockCodeAndSymbolDatabase = null;
        
        this.setEditable(true);
        
        keyAdapter = this.getEditorComponentKeyAdapter();
        
        this.getEditor().getEditorComponent().addKeyListener(keyAdapter);
    }
    
    public void setStockCodeAndSymbolDatabase(StockCodeAndSymbolDatabase stockCodeAndSymbolDatabase) {
        this.stockCodeAndSymbolDatabase = stockCodeAndSymbolDatabase;
        
        KeyListener[] listeners = this.getEditor().getEditorComponent().getKeyListeners();
        
        for(KeyListener listener : listeners) {
            if(listener == keyAdapter) {
                return;
            }
        }
        
        // Bug in Java 6. Most probably this listener had been removed during look n feel updating, reassign!
        this.getEditor().getEditorComponent().addKeyListener(keyAdapter);
        log.info("Reassign key adapter to combo box");        
    }
    
    // We should make this powerful combo box shared amoing different classes.
    private KeyAdapter getEditorComponentKeyAdapter() {
        
        return new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (!e.isActionKey()) {
                    String string = AutoCompleteJComboBox.this.getEditor().getItem().toString();
                    
                    AutoCompleteJComboBox.this.hidePopup();                                        
                    
                    if (KeyEvent.VK_ENTER == e.getKeyCode()) {
                        if (AutoCompleteJComboBox.this.getItemCount() > 0) {
                            int index = AutoCompleteJComboBox.this.getSelectedIndex();
                            if(index == -1) {
                                lastEnteredString = (String)AutoCompleteJComboBox.this.getItemAt(0);
                                AutoCompleteJComboBox.this.getEditor().setItem(lastEnteredString);
                            }
                            else {
                                lastEnteredString = (String)AutoCompleteJComboBox.this.getItemAt(index);
                                AutoCompleteJComboBox.this.getEditor().setItem(lastEnteredString);
                            }
                        }
                        else {
                            final Object object = AutoCompleteJComboBox.this.getEditor().getItem();
                            
                            if (object instanceof String) {
                                lastEnteredString = (String)object;
                            }
                            else {
                                lastEnteredString = "";
                            }
                            
                            AutoCompleteJComboBox.this.getEditor().setItem(null);
                        }
                        
                        javax.swing.SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                AutoCompleteJComboBox.this.removeAllItems();
                            }
                        });
                                                                        
                        return;
                    }   /* if(KeyEvent.VK_ENTER == e.getKeyCode()) */                                               
                    
                    AutoCompleteJComboBox.this.removeAllItems();
                    
                    if (string.length() > 0) {
                        if (AutoCompleteJComboBox.this.stockCodeAndSymbolDatabase != null) {
                            java.util.List<Code> codes = codes = stockCodeAndSymbolDatabase.searchStockCodes(string);

                            boolean shouldShowPopup = false;

                            // Here is our user friendly rule.
                            // (1) User will first like to search for their prefer stock by code. Hence, we only list
                            // out stock code to them. No more, no less.
                            // (2) If we cannot find any stock based on user given stock code, we will search by using
                            // stock symbol.
                            // (3) Do not search using both code and symbol at the same time. There are too much information,
                            // which will make user unhappy.
                            for(Code c : codes) {
                                AutoCompleteJComboBox.this.addItem(c.toString());
                                shouldShowPopup = true;
                            }

                            if (shouldShowPopup) {
                                AutoCompleteJComboBox.this.showPopup();
                            }
                            else {
                                java.util.List<Symbol> symbols = stockCodeAndSymbolDatabase.searchStockSymbols(string);

                                for(Symbol s : symbols) {
                                    AutoCompleteJComboBox.this.addItem(s.toString());
                                    shouldShowPopup = true;
                                }

                                if (shouldShowPopup) {
                                    AutoCompleteJComboBox.this.showPopup();
                                }   // if (shouldShowPopup)
                            }   // if (shouldShowPopup)
                        }   // if (AutoCompleteJComboBox.this.stockCodeAndSymbolDatabase != null)
                    }
                    
                    AutoCompleteJComboBox.this.getEditor().setItem(string);
                    
                    /* When we are in windows look n feel, the text will always be selected. We do not want that. */
                    JTextField jTextField = (JTextField)AutoCompleteJComboBox.this.getEditor().getEditorComponent();
                    jTextField.setSelectionStart(jTextField.getText().length());
                    jTextField.setSelectionEnd(jTextField.getText().length());
                    jTextField.setCaretPosition(jTextField.getText().length());
                    
                }   /* if(!e.isActionKey()) */
            }   /* public void keyReleased(KeyEvent e) */
        };
    }    
    
    public String getLastEnteredString() {
        return this.lastEnteredString;
    }

    @Override
    public void setUI(ComboBoxUI ui)
    {
        if(ui != null)
        {
            // Let's try our own customized UI.
            Class c = ui.getClass();
            final String myClass = "org.yccheok.jstock.gui.AutoCompleteJComboBox$My" + c.getSimpleName();

            try {
                ComboBoxUI myUI = (ComboBoxUI) Class.forName(myClass).newInstance();
                super.setUI(myUI);
                return;
            } catch (ClassNotFoundException ex) {
                log.error(null, ex);
            } catch (InstantiationException ex) {
                log.error(null, ex);
            } catch (IllegalAccessException ex) {
                log.error(null, ex);
            }
        }

        // Either null, or we fail to use our own customized UI.
        // Fall back to default.
        super.setUI(ui);
    }

    // This is a non-portable method to make combo box horizontal scroll bar.
    // Whenever there is a new look-n-feel, we need to manually provide the ComboBoxUI.
    // Any idea on how to make this portable?
    //
    protected static class MyWindowsComboBoxUI extends com.sun.java.swing.plaf.windows.WindowsComboBoxUI
    {
        @Override
        protected ComboPopup createPopup()
        {
            return new MyComboPopup(comboBox);
        }
    }
    
    protected static class MyMotifComboBoxUI extends com.sun.java.swing.plaf.motif.MotifComboBoxUI
    {
        @Override
        protected ComboPopup createPopup()
        {
            return new MyComboPopup(comboBox);
        }
    }
    
    protected static class MyMetalComboBoxUI extends javax.swing.plaf.metal.MetalComboBoxUI
    {
        @Override
        protected ComboPopup createPopup()
        {
            return new MyComboPopup(comboBox);
        }
    }
    
    private static class MyComboPopup extends BasicComboPopup
    {
        public MyComboPopup(JComboBox combo)
        {
            super(combo);
        }

        @Override
		public JScrollPane createScroller()
		{
            return new JScrollPane(list,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		}
    }
    
    private StockCodeAndSymbolDatabase stockCodeAndSymbolDatabase;
    private KeyAdapter keyAdapter;
    private volatile String lastEnteredString = "";
    
    private static final Log log = LogFactory.getLog(AutoCompleteJComboBox.class);
}
