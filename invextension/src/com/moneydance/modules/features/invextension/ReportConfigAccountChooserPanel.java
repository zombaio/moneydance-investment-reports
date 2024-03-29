/*
 * ReportConfigAccountChooserPanel.java
 * Copyright (c) 2014, Dale K. Furrow
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.moneydance.modules.features.invextension;

import com.moneydance.apps.md.controller.io.FileUtils;
import com.moneydance.apps.md.model.Account;
import com.moneydance.apps.md.model.RootAccount;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.TreeSet;

/**
 * Field chooser panel to control the order and identity of fields to be inclouded
 * in a given report
 */
public class ReportConfigAccountChooserPanel extends JPanel {

    private static final long serialVersionUID = -8990699863699414946L;
    private RootAccount root;
    private ReportControlPanel reportControlPanel;
    //JLists
    private DefaultListModel<Account> availableAccountsListModel = new DefaultListModel<>();
    private JList<Account> availableAccountsList = new JList<>(availableAccountsListModel);
    private JScrollPane availableAccountPane = new JScrollPane(availableAccountsList);
    private DefaultListModel<Account> includedAccountsListModel = new DefaultListModel<>();
    private JList<Account> includedAccountsList = new JList<>(includedAccountsListModel);
    private JScrollPane includedAccountsPane = new JScrollPane(includedAccountsList);
    JCheckBox removeHideOnHomepageAccountsBox = new JCheckBox("<HTML>Remove Accounts<br>" +
            "if Set to<br>'Hide on Home Page'</HTML>", false);

    //buttons
    private JButton removeButton = new JButton("<<-Remove Accounts");
    private JButton resetButton = new JButton("Reset");

    //listeners


    public ReportConfigAccountChooserPanel() throws NoSuchFieldException, IllegalAccessException {
        initComponents();
    }

    public ReportConfigAccountChooserPanel(ReportControlPanel reportControlPanel) throws NoSuchFieldException, IllegalAccessException {
        this.reportControlPanel = reportControlPanel;
        initComponents();

    }

    @SuppressWarnings("unused")
    public static void main(String[] args) throws Exception {
        Class<? extends TotalReport> reportClass = TotalSnapshotReport.class;
        ReportConfig reportConfig = ReportConfig.getStandardReportConfig(reportClass);
        ReportConfigAccountChooserPanel testPanel = new ReportConfigAccountChooserPanel();
        String testFileStr1 = "E:\\\\RECORDS\\moneydance\\\\Test\\\\20141014test.moneydance\\\\root.mdinternal";
        String testFileStr2 = "E:\\\\RECORDS\\moneydance\\\\Test\\\\TestSave.moneydance\\\\root.mdinternal";
        File mdFile = new File(testFileStr2);
        testPanel.root = FileUtils.readAccountsFromFile(mdFile, null);
        testPanel.populateBothAccountLists(reportConfig);
        ReportControlPanel.TestFrame frame = new ReportControlPanel.TestFrame(testPanel);
    }

    private void initComponents() throws NoSuchFieldException, IllegalAccessException {
        //subPanels
        JPanel availableAccountsPanel = new JPanel();
        JPanel accountControlPanel = new JPanel();
        JPanel accountsIncludedPanel = new JPanel();

        String[] titles = {"Available Accounts", "Add/Remove", "Accounts Included"};
        JPanel[] panels = {availableAccountsPanel, accountControlPanel, accountsIncludedPanel};
        for (int i = 0; i < panels.length; i++) {
            TitledBorder titledBorder = BorderFactory.createTitledBorder(titles[i]);
            Border emptyBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
            titledBorder.setTitleColor(new Color(100, 100, 100));
            panels[i].setBorder(BorderFactory.createCompoundBorder(titledBorder, emptyBorder));
        }


        availableAccountsPanel.add(availableAccountPane);
        accountsIncludedPanel.add(includedAccountsPane);
        //button panel
        removeHideOnHomepageAccountsBox.setBorderPainted(true);
        accountControlPanel.setLayout(new GridLayout(3, 1));
        accountControlPanel.add(removeButton);
        accountControlPanel.add(resetButton);
        accountControlPanel.add(removeHideOnHomepageAccountsBox);


        GridBagConstraints c = new GridBagConstraints();
        this.setLayout(new GridBagLayout());
        c.anchor = GridBagConstraints.NORTH;
        c.gridx = 0;
        c.gridy = 0;
        this.add(availableAccountsPanel, c);
        c.gridx++;
        c.anchor = GridBagConstraints.CENTER;
        this.add(accountControlPanel, c);
        c.gridx++;
        c.anchor = GridBagConstraints.NORTH;
        this.add(accountsIncludedPanel, c);

        //selection model
        includedAccountsList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

        //listeners
        removeButton.addActionListener(new RemoveAccountsListener());
        resetButton.addActionListener(new ResetListener());
        removeHideOnHomepageAccountsBox.addActionListener(new RemoveHideOnHomePageAccountListener());

        // renderers
        availableAccountsList.setCellRenderer(new AccountCellRenderer());
        includedAccountsList.setCellRenderer(new AccountCellRenderer());

    }

    public void setHideOnHomePageAccountsRemoved(){
        boolean hideOnHomePageAccountsRemoved = true;
        HashSet<Account> hideOnHomePageAccounts = new HashSet<>();
        for(int i=0;i<availableAccountsListModel.getSize(); i++){
            Account account = availableAccountsListModel.getElementAt(i);
            if(account.getHideOnHomePage()) hideOnHomePageAccounts.add(account);
        }
        for(int i=0;i<includedAccountsListModel.getSize(); i++){
            Account account = includedAccountsListModel.getElementAt(i);
            if(hideOnHomePageAccounts.contains(account)){
                hideOnHomePageAccountsRemoved = false;
                break;
            }
        }
        removeHideOnHomepageAccountsBox.setSelected(hideOnHomePageAccountsRemoved);
    }

    public void removeHideOnHomePageAccounts(){
        HashSet<Account> accountsToRemove = new HashSet<>();
        for(int i=0;i<includedAccountsListModel.getSize(); i++){
            Account account = includedAccountsListModel.getElementAt(i);
            if(account.getHideOnHomePage()) accountsToRemove.add(account);
        }
        for(Account account : accountsToRemove){
            includedAccountsListModel.removeElement(account);
        }
        if(this.reportControlPanel != null) updateReportConfig();
    }

    public void populateBothAccountLists(ReportConfig reportConfig)
            throws Exception {
        availableAccountsListModel.removeAllElements();
        includedAccountsListModel.removeAllElements();
        populateAvailableAccountsList();
        populateIncludedAccountsList(reportConfig);
        Dimension dimension = reportControlPanel.getRelatedDimension(availableAccountPane);
        availableAccountPane.setPreferredSize(dimension);
        includedAccountsPane.setPreferredSize(dimension);
        setHideOnHomePageAccountsRemoved();
    }

    private void populateAvailableAccountsList() throws Exception {
        if(root == null) root = reportControlPanel.getRoot();
        if(root != null){
            TreeSet<Account> investmentAccountSet = BulkSecInfo.getSelectedSubAccounts
                    (root, Account.ACCOUNT_TYPE_INVESTMENT);
            int i = 0;
            for (Account investmentAccount : investmentAccountSet) {
                availableAccountsListModel.add(i, investmentAccount);
                i++;
            }
        } else {
            throw new Exception("Error on loading available account list");

        }

    }

    public void populateIncludedAccountsList(ReportConfig reportConfig) throws NoSuchFieldException,
            IllegalAccessException {
        HashSet<Integer> excludedAccountsSet = reportConfig.getExcludedAccountNums();
        int includedAccountCount = 0;
        for (int i = 0; i < availableAccountsListModel.getSize(); i++){
            Account availableAccount = availableAccountsListModel.getElementAt(i);
            if(!excludedAccountsSet.contains(availableAccount.getAccountNum())){
                includedAccountsListModel.add(includedAccountCount, availableAccount);
                includedAccountCount ++;
            }
        }
    }

    public void updateReportConfig() {
        HashSet<Integer> excludedAccountNums = new HashSet<>();
        HashSet<Account> excludedAccounts = getExcludedAccountSet();
        for (Account account : excludedAccounts){
            excludedAccountNums.add(account.getAccountNum());
        }
        reportControlPanel.getReportConfig().setExcludedAccountNums(excludedAccountNums);
    }

    public LinkedHashSet<Account> getExcludedAccountSet() {
        LinkedHashSet<Account> includedAccountSet = new LinkedHashSet<>();
        for (int i = 0; i < includedAccountsListModel.size(); i++) {
            includedAccountSet.add(includedAccountsListModel.get(i));
        }
        LinkedHashSet<Account> totalAccountSet = new LinkedHashSet<>();
        for (int i = 0; i < availableAccountsListModel.size(); i++) {
            totalAccountSet.add(availableAccountsListModel.get(i));
        }

        totalAccountSet.removeAll(includedAccountSet);
        return totalAccountSet;
    }

    class RemoveAccountsListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {

            int[] indices = includedAccountsList.getSelectedIndices();
            removeAccountRange(indices);
        }
    }

    class RemoveHideOnHomePageAccountListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if(e.getSource() == removeHideOnHomepageAccountsBox){
                if(removeHideOnHomepageAccountsBox.isSelected()){
                    removeHideOnHomePageAccounts();
                } else {
                    refillIncludedAccounts();
                }
            }
        }
    }

    private void removeAccountRange(int[] indices) {
        if (indices.length > 0) {
            includedAccountsListModel.removeRange(indices[0], indices[indices.length - 1]);
            int sizeRemaining = includedAccountsListModel.getSize();

            if (sizeRemaining == 0) { //Nobody's left, disable firing.
                refillIncludedAccounts();
                JOptionPane.showMessageDialog(this,
                        "Must include at least one account!");
            } else { //Select an index.
                int index = indices[indices.length - 1];
                if (index == includedAccountsListModel.getSize()) {
                    //removed item in last position
                    index -= indices.length;
                }
                includedAccountsList.setSelectedIndex(index);
                includedAccountsList.ensureIndexIsVisible(index);
            }
        }
        if(this.reportControlPanel != null) updateReportConfig();
    }

    class ResetListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            refillIncludedAccounts();
        }
    }

    private void refillIncludedAccounts() {
        includedAccountsListModel.removeAllElements();
        for (int i = 0; i < availableAccountsListModel.size(); i++) {
            Account account = availableAccountsListModel.get(i);
            includedAccountsListModel.addElement(account);
        }
        if(this.reportControlPanel != null) updateReportConfig();
    }


    class AccountCellRenderer extends JLabel implements ListCellRenderer<Account> {
        private static final long serialVersionUID = 7586072864239449518L;
        private final Color HIGHLIGHT_COLOR = new Color(0, 0, 128);

        public AccountCellRenderer() {
            setOpaque(true);
        }

        public Component getListCellRendererComponent(JList<? extends Account> list, Account value,
            int index, boolean isSelected, boolean cellHasFocus) {
            String displayText = value.getAccountName().trim() + " (id: " + value.getAccountNum() + ")";
            setText(displayText);
            if (isSelected) {
                setBackground(HIGHLIGHT_COLOR);
                setForeground(Color.white);
            } else {
                setBackground(Color.white);
                setForeground(Color.black);
            }
            return this;
        }
    }


}









