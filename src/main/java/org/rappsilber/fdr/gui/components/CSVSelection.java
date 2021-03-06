/*
 * Copyright 2015 Lutz Fischer <lfischer at staffmail.ed.ac.uk>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rappsilber.fdr.gui.components;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EventObject;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import org.rappsilber.data.csv.CsvParser;
import org.rappsilber.gui.components.JoinedThreadedTextOuput;
import org.rappsilber.data.csv.ColumnAlternatives;

/**
 *
 * @author lfischer
 */
public class CSVSelection extends javax.swing.JPanel {

    private ArrayList<java.awt.event.ActionListener> m_actionlisteners = new ArrayList<ActionListener>();

    public String missingColumn = "!! !MISSING! !!";
    public String optionalColumn = "  OPTIONAL   ";
    public String[] csvColumns = new String[]{missingColumn};
    public String[] csvColumnsOptional = new String[]{optionalColumn};

    private JoinedThreadedTextOuput m_status = null;
    
    
    private class NeededOptionalComboBoxCellEditor extends DefaultCellEditor {

        DefaultCellEditor.EditorDelegate neededDelegate;
        DefaultCellEditor.EditorDelegate optionalDelegate;
        JComponent neededEditorComponent;
        JComponent optionalEditorComponent;

        public NeededOptionalComboBoxCellEditor(final JComboBox needed, final JComboBox optional) {
            super(needed);
            this.neededDelegate = delegate;
            this.neededEditorComponent = needed;

            optionalDelegate = new DefaultCellEditor.EditorDelegate() {
                public void setValue(Object value) {
                    optional.setSelectedItem(value);
                }

                public Object getCellEditorValue() {
                    return optional.getSelectedItem();
                }

                public boolean shouldSelectCell(EventObject anEvent) {
                    if (anEvent instanceof MouseEvent) {
                        MouseEvent e = (MouseEvent) anEvent;
                        return e.getID() != MouseEvent.MOUSE_DRAGGED;
                    }
                    return true;
                }

                public boolean stopCellEditing() {
                    if (optional.isEditable()) {
                        // Commit edited value.
                        optional.actionPerformed(new ActionEvent(this, 0, ""));
                    }
                    return super.stopCellEditing();
                }
            };
            optional.addActionListener(optionalDelegate);
            optionalEditorComponent = optional;

        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected,
                int row, int column) {
            TableModel tm = table.getModel();
            if (Boolean.TRUE.equals(tm.getValueAt(row, 1))) {
                delegate = optionalDelegate;
                editorComponent = optionalEditorComponent;
            } else {
                delegate = neededDelegate;
                editorComponent = neededEditorComponent;
            }
            return super.getTableCellEditorComponent(table, value, isSelected, row, column);
        }
    }
    
    
    /**
     * Creates new form CSVSelection
     */
    public CSVSelection() {
        initComponents();
        fbCsvIn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setUpCsvHeaders();
            }
        });       
        

        ckCSVHasHeader.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setUpCsvHeaders();
            }
        });
                
        
        cbCSVHeaders.setModel(new DefaultComboBoxModel(csvColumns));
        cbCSVHeaderOptional.setModel(new DefaultComboBoxModel(csvColumnsOptional));

        TableColumn columnNamesColumn = tblCSVColumns.getColumnModel().getColumn(2);
        columnNamesColumn.setCellEditor(new NeededOptionalComboBoxCellEditor(cbCSVHeaders, cbCSVHeaderOptional));
        TableModel tm = tblCSVColumns.getModel();
        for (int r = 0; r < tblCSVColumns.getRowCount(); r++) {
            if (Boolean.TRUE.equals(tm.getValueAt(r, 1))) {
                tblCSVColumns.getModel().setValueAt(optionalColumn, r, 2);
            } else {
                tblCSVColumns.getModel().setValueAt(missingColumn, r, 2);
            }
        }
        
    }

    public void setUpCsvHeaders() {
        File f = fbCsvIn.getFile();
        if (f != null && f.canRead()) {
            CsvParser csv;
            try {
                csv = CsvParser.guessCsv(f, 50);
                ColumnAlternatives.setupAlternatives(csv);
                String delimiter = csv.getDelimiter();
                if (delimiter.contentEquals(",")) {
                    delimiter = "Comma";
                } else if (delimiter.contentEquals("\t")) {
                    delimiter = "Tab";
                } else if (delimiter.contentEquals(" ")) {
                    delimiter = "Space";
                }
                cbCSVDElimiter.setSelectedItem(delimiter);
                cbCSVQuote.setSelectedItem(csv.getQuote());
                csv.next();
                csvColumns = new String[csv.getMaxColumns() + 1];
                csvColumnsOptional = new String[csv.getMaxColumns() + 1];
                csvColumns[0] = missingColumn;
                csvColumnsOptional[0] = optionalColumn;

                if (ckCSVHasHeader.isSelected()) {
                    csv.setCurrentLineAsHeader();
                    ColumnAlternatives.levenshteinMatchHeadersALternatives(csv);
                    for (int c = 0; c < csv.getMaxColumns(); c++) {
                        csvColumns[c + 1] = csv.getHeader(c);
                        csvColumnsOptional[c + 1] = csv.getHeader(c);
                    }
                    TableModel tm = tblCSVColumns.getModel();
                    for (int r = 0; r < tm.getRowCount(); r++) {
                        Integer rc = csv.getColumn(tm.getValueAt(r, 0).toString());
                        if (rc != null) {
                            tm.setValueAt(csv.getHeader(rc), r, 2);
                        }
                    }

                } else {
                    for (int c = 0; c < csv.getMaxColumns(); c++) {
                        csvColumns[c + 1] = Integer.toString(c);
                    }
                }

                cbCSVHeaders.setModel(new DefaultComboBoxModel(csvColumns));
                cbCSVHeaderOptional.setModel(new DefaultComboBoxModel(csvColumnsOptional));
                csv.close();

            } catch (FileNotFoundException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                setStatus(ex.toString());
            } catch (IOException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                setStatus(ex.toString());
            }
        }

    }
 
    
    private void setStatus(final String status) {
        if (m_status != null)
            m_status.write(status);
    }
    
    public void setStatusInterface(final JoinedThreadedTextOuput status ) {
        m_status = status;
    }
    
    public File getFile() {
        return fbCsvIn.getFile();
    }
    
    protected void doActionPerformed() {
        ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "file selected",Calendar.getInstance().getTimeInMillis(), 0);
        for (java.awt.event.ActionListener al : m_actionlisteners) 
            al.actionPerformed(null);
    }
    
    public void addActionListener(java.awt.event.ActionListener al) {
        this.m_actionlisteners.add(al);
    }
    

    public void removeActionListener(java.awt.event.ActionListener al) {
        this.m_actionlisteners.remove(al);
    }
    
    public String getDelimiter() {
        return cbCSVDElimiter.getSelectedItem().toString();
    }

    public char getQuote() {
        return cbCSVQuote.getSelectedItem().toString().charAt(0);
    }
    
    public boolean higherIsBetter() {
        return rbCSVHighBetter.isSelected();
    }
    
    public boolean hasHeader() {
        return ckCSVHasHeader.isSelected();
    }
    
    
    public CsvParser getCsv() throws IOException {
        final TableModel tm = tblCSVColumns.getModel();

        CsvParser csv = new CsvParser();
        for (int r = 0; r < tm.getRowCount(); r++) {
            if (tm.getValueAt(r, 2) != missingColumn) {
                csv.setAlternative(tm.getValueAt(r, 2).toString(), tm.getValueAt(r, 0).toString());
            }
        }
        String delimiter = getDelimiter();
        if (delimiter.contentEquals("Comma")) {
            delimiter = ",";
        } else if (delimiter.contentEquals("Tab")) {
            delimiter = "\t";
        } else if (delimiter.contentEquals("Space")) {
            delimiter = " ";
        }
        csv.setDelimiter(delimiter.charAt(0));
        csv.setQuote(getQuote());
        csv.openFile(getFile(), hasHeader());
        return csv;
    }
    
    
    public void setEnable(boolean enabled) {
        super.setEnabled(enabled);
        btnReadCsv.setEnabled(enabled);
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        cbCSVHeaders = new javax.swing.JComboBox();
        cbCSVHeaderOptional = new javax.swing.JComboBox();
        bgScoreDirectionCSV = new javax.swing.ButtonGroup();
        jLabel26 = new javax.swing.JLabel();
        cbCSVQuote = new javax.swing.JComboBox();
        cbCSVDElimiter = new javax.swing.JComboBox();
        rbCSVHighBetter = new javax.swing.JRadioButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblCSVColumns = new javax.swing.JTable();
        ckCSVHasHeader = new javax.swing.JCheckBox();
        fbCsvIn = new org.rappsilber.gui.components.FileBrowser();
        rbCSVLowBetter = new javax.swing.JRadioButton();
        jLabel11 = new javax.swing.JLabel();
        btnReadCsv = new javax.swing.JButton();
        jLabel27 = new javax.swing.JLabel();

        cbCSVHeaders.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        cbCSVHeaderOptional.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel26.setText("Delimiter");

        cbCSVQuote.setEditable(true);
        cbCSVQuote.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "\"", "'" }));
        cbCSVQuote.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbCSVQuoteActionPerformed(evt);
            }
        });

        cbCSVDElimiter.setEditable(true);
        cbCSVDElimiter.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Comma", "TAB", "Space", "|", " " }));
        cbCSVDElimiter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbCSVDElimiterActionPerformed(evt);
            }
        });

        bgScoreDirectionCSV.add(rbCSVHighBetter);
        rbCSVHighBetter.setSelected(true);
        rbCSVHighBetter.setText("High Score better");

        tblCSVColumns.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"run",  new Boolean(true), ""},
                {"scan",  new Boolean(true), null},
                {"psmid",  new Boolean(true), null},
                {"peptide1", null, null},
                {"peptide2", null, null},
                {"peptide length 1",  new Boolean(true), null},
                {"peptide length 2",  new Boolean(true), null},
                {"peptide link 1", null, null},
                {"peptide link 2", null, null},
                {"is decoy 1", null, null},
                {"is decoy 2", null, null},
                {"precursor charge", null, null},
                {"score",  new Boolean(true), null},
                {"score ratio",  new Boolean(true), null},
                {"peptide1 score",  new Boolean(true), null},
                {"peptide2 score",  new Boolean(true), null},
                {"accession1", null, null},
                {"accession2", null, null},
                {"description1",  new Boolean(true), null},
                {"description2",  new Boolean(true), null},
                {"peptide position 1", null, null},
                {"peptide position 2", null, null}
            },
            new String [] {
                "Column", "Optional", "Name in CSV"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Boolean.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblCSVColumns.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_LAST_COLUMN);
        jScrollPane2.setViewportView(tblCSVColumns);

        ckCSVHasHeader.setSelected(true);
        ckCSVHasHeader.setText("hasHeader");

        fbCsvIn.setDescription("Text Files");
        fbCsvIn.setExtensions(new String[] {"csv", "txt", "tsv"});

        bgScoreDirectionCSV.add(rbCSVLowBetter);
        rbCSVLowBetter.setText("Lower score better");

        jLabel11.setText("CSV-File");

        btnReadCsv.setText("Read");
        btnReadCsv.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnReadCsvActionPerformed(evt);
            }
        });

        jLabel27.setText("Quote");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(13, 13, 13)
                        .addComponent(jLabel11))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel26)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(cbCSVDElimiter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel27)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cbCSVQuote, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(ckCSVHasHeader))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(rbCSVLowBetter)
                            .addComponent(rbCSVHighBetter))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnReadCsv, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(12, 12, 12))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(fbCsvIn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(jLabel11))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(fbCsvIn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addComponent(btnReadCsv))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel26)
                        .addComponent(cbCSVDElimiter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel27)
                        .addComponent(cbCSVQuote, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(rbCSVHighBetter)
                        .addGap(3, 3, 3)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(rbCSVLowBetter)
                            .addComponent(ckCSVHasHeader))))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 178, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void cbCSVQuoteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbCSVQuoteActionPerformed
        String quote = cbCSVQuote.getSelectedItem().toString();
        if (quote.length() > 1) {
            JOptionPane.showMessageDialog(this, "Quote \"" + quote + "\" is not supported. \n\nThe quote character has to be a single character", "Unsupoorted quote", JOptionPane.ERROR_MESSAGE);
            cbCSVQuote.requestFocusInWindow();
        } else if (quote.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Sorry we need a quote character", "Unsupoorted quote", JOptionPane.ERROR_MESSAGE);
            cbCSVQuote.setSelectedItem("\"");
            cbCSVQuote.requestFocusInWindow();
        }
    }//GEN-LAST:event_cbCSVQuoteActionPerformed

    private void cbCSVDElimiterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbCSVDElimiterActionPerformed

        String delimiter = cbCSVDElimiter.getSelectedItem().toString();
        if (delimiter.length() > 1) {
            if (!delimiter.matches("(Tab|Comma|Space)")) {
                JOptionPane.showMessageDialog(this, "Delimiter \"" + delimiter + "\" is not supported. \n\nOnly single character delimiter are supported", "Unsupoorted delimiter", JOptionPane.ERROR_MESSAGE);
                cbCSVDElimiter.requestFocusInWindow();
            }
        } else if (delimiter.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Sorry we need a delimiter", "Unsupoorted delimiter", JOptionPane.ERROR_MESSAGE);
            cbCSVDElimiter.setSelectedItem("Comma");
            cbCSVDElimiter.requestFocusInWindow();
        } else {
            if (delimiter.contentEquals(",")) {
                cbCSVDElimiter.setSelectedItem("Comma");
            } else if (delimiter.contentEquals(" ")) {
                cbCSVDElimiter.setSelectedItem("Space");
            } else if (delimiter.contentEquals("\t")) {
                cbCSVDElimiter.setSelectedItem("Tab");
            }
        }
    }//GEN-LAST:event_cbCSVDElimiterActionPerformed

    private void btnReadCsvActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnReadCsvActionPerformed

            boolean scoreSelected = false;
            boolean peptide1ScoreSelected = false;
            boolean peptide2ScoreSelected = false;
            boolean psmidSelected = false;
            boolean runSelected = false;
            boolean scanSelected = false;
        
        
            final TableModel tm = tblCSVColumns.getModel();
            // check, whether we have all the neded columns
            for (int r = 0; r < tm.getRowCount(); r++) {
                if ((!Boolean.TRUE.equals(tm.getValueAt(r, 1))) && tm.getValueAt(r, 2) == missingColumn) {
                    JOptionPane.showMessageDialog(this, "No column for " + tm.getValueAt(r, 0) + " selected", "Missing Column", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (tm.getValueAt(r, 0).toString().contentEquals("Peptide1 Score") && tm.getValueAt(r, 2) != optionalColumn) {
                    peptide1ScoreSelected = true;
                }
                if (tm.getValueAt(r, 0).toString().contentEquals("Peptide2 Score") && tm.getValueAt(r, 2) != optionalColumn) {
                    peptide2ScoreSelected = true;
                }
                if (tm.getValueAt(r, 0).toString().contentEquals("score") && tm.getValueAt(r, 2) != optionalColumn) {
                    scoreSelected = true;
                }
                if (tm.getValueAt(r, 0).toString().contentEquals("run") && tm.getValueAt(r, 2) != optionalColumn) {
                    runSelected = true;
                }
                if (tm.getValueAt(r, 0).toString().contentEquals("scan") && tm.getValueAt(r, 2) != optionalColumn) {
                    scanSelected = true;
                }
                if (tm.getValueAt(r, 0).toString().contentEquals("psmid") && tm.getValueAt(r, 2) != optionalColumn) {
                    psmidSelected = true;
                }
            }

            if (!(scoreSelected || (peptide1ScoreSelected && peptide2ScoreSelected))) {
                JOptionPane.showMessageDialog(this, "At least a PSM-level-score (Score-column) \n"
                        + "or two peptide scores for a PSM is required.", "Missing Column", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (!(psmidSelected || (runSelected && scanSelected))) {
                JOptionPane.showMessageDialog(this, "We need at least a PSM-ID \n"
                        + "or run and Scan to uniqely identify a PSM.", "Missing Column", JOptionPane.WARNING_MESSAGE);
                return;
            }
        
        doActionPerformed();
    }//GEN-LAST:event_btnReadCsvActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup bgScoreDirectionCSV;
    private javax.swing.JButton btnReadCsv;
    private javax.swing.JComboBox cbCSVDElimiter;
    private javax.swing.JComboBox cbCSVHeaderOptional;
    private javax.swing.JComboBox cbCSVHeaders;
    private javax.swing.JComboBox cbCSVQuote;
    private javax.swing.JCheckBox ckCSVHasHeader;
    private org.rappsilber.gui.components.FileBrowser fbCsvIn;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JRadioButton rbCSVHighBetter;
    private javax.swing.JRadioButton rbCSVLowBetter;
    private javax.swing.JTable tblCSVColumns;
    // End of variables declaration//GEN-END:variables
}
