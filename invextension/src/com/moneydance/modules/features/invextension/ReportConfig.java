/*
 * ReportConfig.java
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


import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Controller for Report Configuration Options
 */
public class ReportConfig {
    Preferences prefs = Prefs.REPORT_CONFIG_PREFS;
    Class<? extends TotalReport> reportClass;
    String reportTypeName;
    String reportName;
    boolean useAverageCostBasis;
    AggregationController aggregationController;
    boolean outputSingle;
    int numFrozenColumns;
    boolean closedPosHidden;
    LinkedList<Integer> viewHeader;
    HashSet<Integer> excludedAccountNums;
    DateRange dateRange;
    boolean isDefaultConfig = false;
    FrameInfo frameInfo;

    public ReportConfig() {
        this.reportClass = null;
        this.reportTypeName = "select any report";
        this.reportName = "select any report";
        this.useAverageCostBasis = true;
        this.aggregationController = null;
        this.outputSingle = false;
        this.numFrozenColumns = 0;
        this.closedPosHidden = true;
        this.viewHeader = new LinkedList<>();
        this.excludedAccountNums = new HashSet<>();
        this.dateRange = new DateRange();
        this.isDefaultConfig = false;
        this.frameInfo = new FrameInfo();
    }


    /**
     * Constructor to use for testing
     *
     * @param reportClass           input Report Class
     * @param reportName            test report name
     * @param useAverageCostBasis            test report name
     * @param aggregationController test aggregation controller
     * @param outputSingle          irrelevant for testing
     * @param numFrozenColumns      irrelevant for testing
     * @param closedPosHidden       irrelevant for testing
     * @param viewHeader            irrelevant for testing
     * @param excludedAccountNums   irrelevant for testing
     * @param dateRange             date range to use for testing
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public ReportConfig(Class<? extends TotalReport> reportClass, String reportName, boolean useAverageCostBasis,
                        AggregationController aggregationController, boolean outputSingle, int numFrozenColumns,
                        boolean closedPosHidden, LinkedList<Integer> viewHeader, HashSet<Integer> excludedAccountNums,
                        DateRange dateRange)
            throws NoSuchFieldException, IllegalAccessException {

        this.reportClass = reportClass;
        this.reportTypeName = ReportConfig.getReportTypeName(reportClass);
        this.reportName = reportName;
        this.useAverageCostBasis = useAverageCostBasis;
        this.aggregationController = aggregationController;
        this.outputSingle = outputSingle;
        this.numFrozenColumns = numFrozenColumns;
        this.closedPosHidden = closedPosHidden;
        this.viewHeader = viewHeader;
        this.excludedAccountNums = excludedAccountNums;
        this.dateRange = dateRange;
        this.frameInfo = new FrameInfo();
    }

    /**
     * Standard Constructor for ReportConfig
     *
     * @param reportClass type of report
     * @param reportName  name of report
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public ReportConfig(Class<? extends TotalReport> reportClass, String reportName) throws NoSuchFieldException,
            IllegalAccessException, BackingStoreException {
        this.reportTypeName = ReportConfig.getReportTypeName(reportClass);
        boolean nodeExists = prefs.node(reportTypeName).nodeExists(reportName);
        Preferences thisReportPrefs = prefs.node(reportTypeName).node(nodeExists ? reportName : Prefs.STANDARD_NAME);
        ReportConfig standardConfig = getStandardReportConfig(reportClass); //used to populate defaults if pref not found
        this.reportClass = reportClass;
        this.reportName = reportName;
        this.useAverageCostBasis = thisReportPrefs.getBoolean(Prefs.USE_AVERAGE_COST_BASIS,
                standardConfig.useAverageCostBasis());
        this.aggregationController = getAggregationControllerFromPrefs(thisReportPrefs);
        this.outputSingle = thisReportPrefs.getBoolean(Prefs.OUTPUT_SINGLE, standardConfig.isOutputSingle());
        this.numFrozenColumns = thisReportPrefs.getInt(Prefs.NUM_FROZEN_COLUMNS, standardConfig.getNumFrozenColumns());
        this.closedPosHidden = thisReportPrefs.getBoolean(Prefs.CLOSED_POS_HIDDEN, standardConfig.isClosedPosHidden());
        this.viewHeader = getLinkedListFromString(thisReportPrefs.get(Prefs.VIEWHEADER,
                standardConfig.writeViewHeaderToString()));
        this.excludedAccountNums = getHashSetFromString(thisReportPrefs.get(Prefs.EXCLUDEDACCOUNTNUMS,
                standardConfig.writeExcludedAccountsToString()));
        this.dateRange = DateRange.getDateRangeFromString(thisReportPrefs.get(Prefs.DATERANGE,
                standardConfig.getDateRange().toString()));
        this.isDefaultConfig = thisReportPrefs.getBoolean(Prefs.ISSTANDARD, standardConfig.isOutputSingle());
        this.frameInfo = getFrameInfoFromPrefs(thisReportPrefs);

    }

    /**
     * Similar constructor to above, accepts simple name for report type
     *
     * @param reportClassSimpleName report type
     * @param reportName            name of report
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     */

    public ReportConfig(String reportClassSimpleName, String reportName) throws NoSuchFieldException,
            IllegalAccessException, ClassNotFoundException, BackingStoreException {
        Class<? extends TotalReport> reportClass = getClassFromName(reportClassSimpleName);
        this.reportTypeName = ReportConfig.getReportTypeName(reportClass);
        boolean nodeExists = prefs.node(reportTypeName).nodeExists(reportName);
        Preferences thisReportPrefs = prefs.node(reportTypeName).node(nodeExists ? reportName : Prefs.STANDARD_NAME);
        ReportConfig standardConfig = getStandardReportConfig(reportClass); //used to populate defaults if pref not found
        this.reportClass = reportClass;
        this.reportName = reportName;
        this.useAverageCostBasis = thisReportPrefs.getBoolean(Prefs.USE_AVERAGE_COST_BASIS,
                standardConfig.useAverageCostBasis());
        this.aggregationController = getAggregationControllerFromPrefs(thisReportPrefs);
        this.outputSingle = thisReportPrefs.getBoolean(Prefs.OUTPUT_SINGLE, standardConfig.isOutputSingle());
        this.numFrozenColumns = thisReportPrefs.getInt(Prefs.NUM_FROZEN_COLUMNS, standardConfig.getNumFrozenColumns());
        this.closedPosHidden = thisReportPrefs.getBoolean(Prefs.CLOSED_POS_HIDDEN, standardConfig.isClosedPosHidden());
        this.viewHeader = getLinkedListFromString(thisReportPrefs.get(Prefs.VIEWHEADER, standardConfig.writeViewHeaderToString()));
        this.excludedAccountNums = getHashSetFromString(thisReportPrefs.get(Prefs.EXCLUDEDACCOUNTNUMS,
                standardConfig.writeExcludedAccountsToString()));
        this.dateRange = DateRange.getDateRangeFromString(thisReportPrefs.get(Prefs.DATERANGE,
                standardConfig.getDateRange().toString()));
        this.isDefaultConfig = thisReportPrefs.getBoolean(Prefs.ISSTANDARD, standardConfig.isOutputSingle());
        this.frameInfo = getFrameInfoFromPrefs(thisReportPrefs);
    }

    public static String[] getReportNamesForClass(Class<? extends TotalReport> reportClass) throws
            BackingStoreException, NoSuchFieldException, IllegalAccessException {
        Preferences reportClassNode = Prefs.REPORT_CONFIG_PREFS.node(getReportTypeName(reportClass));
        return reportClassNode.childrenNames();
    }

    /**
     * gets Default column for aggregating class (i.e. which to sort on)
     *
     * @param reportClass Class which extends total report
     * @return "type name" of report e.g. "snapshot report"
     * @throws SecurityException
     * @throws NoSuchFieldException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public static String getReportTypeName(Class<? extends TotalReport> reportClass)
            throws SecurityException, NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException {
        Field reportTypeName = reportClass.getDeclaredField("reportTypeName");
        return (String) reportTypeName.get(String.class);
    }

    /*
    Clears ReportConfigs for give report from preferences
     */
    public static void clearAllReportConfigsForClass(Class<? extends TotalReport> reportClass)
            throws BackingStoreException, NoSuchFieldException, IllegalAccessException {
        String reportTypeName = ReportConfig.getReportTypeName(reportClass);
        Preferences reportClassNode = Prefs.REPORT_CONFIG_PREFS.node(reportTypeName);
        String[] reportNames = reportClassNode.childrenNames();
        for (String reportName : reportNames) {
            Preferences reportNode = reportClassNode.node(reportName);
            reportNode.removeNode();
        }
    }

    /**
     * clears individual preference node
     *
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws BackingStoreException
     */
    public static void clearConfigNode()
            throws NoSuchFieldException,
            IllegalAccessException, BackingStoreException {
        Prefs.REPORT_CONFIG_PREFS.removeNode();

    }

    /*
    returns default ReportConfig
     */
    public static ReportConfig getStandardReportConfig(Class<? extends TotalReport> reportClass)
            throws NoSuchFieldException, IllegalAccessException {
        String reportName = Prefs.STANDARD_NAME;
        AggregationController defaultAggregationController = AggregationController.INVACCT;
        LinkedList<Integer> viewHeader = getDefaultViewHeader(getModelHeader(reportClass));
        DateRange defaultDateRange = DateRange.getDefaultDateRange();
        HashSet<Integer> excludedAccountNums = new HashSet<>();
        ReportConfig standardConfig = new ReportConfig(reportClass, reportName, true,
                defaultAggregationController, false, 5, true, viewHeader, excludedAccountNums,
                defaultDateRange);
        standardConfig.setStandardConfig(true);
        return standardConfig;
    }

    public static void setStandardConfigInPrefs(Class<? extends TotalReport> reportClass) throws
            BackingStoreException, NoSuchFieldException, IllegalAccessException {
        Preferences reportClassNode = Prefs.REPORT_CONFIG_PREFS.node(getReportTypeName(reportClass));
        String[] childrenNames = reportClassNode.childrenNames();
        if (childrenNames.length == 0 || !Arrays.asList(childrenNames).contains(Prefs.STANDARD_NAME)) {
            clearAllReportConfigsForClass(reportClass);
            ReportConfig reportConfig = getStandardReportConfig(reportClass);
            reportConfig.saveReportConfig();
        }
    }

    /*
    Returns model header as default view header
     */
    public static LinkedList<Integer> getDefaultViewHeader(LinkedList<String> modelHeader) {
        LinkedList<Integer> viewHeader = new LinkedList<>();
        for (int i = 0; i < modelHeader.size(); i++) {
            viewHeader.add(i, i);
        }
        return viewHeader;
    }

    public static HashSet<Integer> getDefaultExcludedAccounts(){
        return new HashSet<>();
    }


    @SuppressWarnings("unchecked")
    public static LinkedList<String> getModelHeader(
            Class<? extends TotalReport> totalReportSubClass)
            throws SecurityException, NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException {
        Field modelHeader = totalReportSubClass.getDeclaredField("MODEL_HEADER");
        return (LinkedList<String>) modelHeader.get(LinkedList.class);
    }

    /**
     * Generic method to get linked list from String where significant values
     * are separated by commas
     *
     * @param prefString input String
     * @return Linked list of values
     */
    public static LinkedList<Integer> getLinkedListFromString(String prefString) {
        String[] viewHeaderStr = prefString.split(",");
        Integer[] viewHeaderInt = new Integer[viewHeaderStr.length];
        for (int i = 0; i < viewHeaderStr.length; i++) {
            viewHeaderInt[i] = Integer.parseInt(viewHeaderStr[i]);
        }
        return new LinkedList<>(Arrays.asList(viewHeaderInt));
    }

    /**
     * Generic method to get Hash Set from String where significant values
     * are separated by commas
     *
     * @param prefString input String
     * @return Linked list of values
     */
    public static HashSet<Integer> getHashSetFromString(String prefString) {
        if (prefString != null && prefString.length() > 0) {
            String[] excludedAccountStrs = prefString.split(",");
            Integer[] excludedAccountNums = new Integer[excludedAccountStrs.length];
            for (int i = 0; i < excludedAccountStrs.length; i++) {
                excludedAccountNums[i] = Integer.parseInt(excludedAccountStrs[i]);
            }
            return new HashSet<>(Arrays.asList(excludedAccountNums));
        } else {
            return new HashSet<>();
        }
    }




    /**
     * Prints node for given report
     *
     * @param reportClass type of report
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    @SuppressWarnings("unused")
    public static void printPrefNode(Class<? extends TotalReport> reportClass)
            throws NoSuchFieldException, IllegalAccessException, IOException, BackingStoreException {
        Preferences thisPrefNode = Prefs.REPORT_CONFIG_PREFS.node(getReportTypeName(reportClass));
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            thisPrefNode.exportSubtree(baos);
            String outString = baos.toString("UTF-8");
            System.out.println(outString);
        }
    }


    /**
     * prints configuration node for all reports
     * @throws IOException
     * @throws BackingStoreException
     */
    public static void printConfigNode() throws IOException, BackingStoreException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Prefs.REPORT_CONFIG_PREFS.exportSubtree(baos);
            String outString = baos.toString("UTF-8");
            System.out.println(outString);
        }
    }

    /**
     * gets default frame size based upon screen size
     *
     * @return Frame dimensions as FrameInfo
     */
    public static FrameInfo getDefaultFrameInfo() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = new Dimension((int) (screenSize.getWidth() * 0.8),
                (int) (screenSize.getHeight() * 0.8));
        Point point = new Point((screenSize.width - frameSize.width) / 2,
                (screenSize.height - frameSize.height) / 2);
        return new FrameInfo(point, frameSize);
    }

    /**
     * retrieves Frame Info from preferences
     *
     * @param preferences preference node4
     * @return dimensional values for frame
     */
    public static FrameInfo getFrameInfoFromPrefs(Preferences preferences) {
        String frameInfoStr = preferences.get(Prefs.FRAMEINFO, null);
        if (frameInfoStr != null) {
            String[] viewHeaderStr = frameInfoStr.split(",");
            Point point = new Point(Integer.parseInt(viewHeaderStr[0]), Integer.parseInt(viewHeaderStr[1]));
            Dimension dimension = new Dimension(Integer.parseInt(viewHeaderStr[2]),
                    Integer.parseInt(viewHeaderStr[3]));
            return new FrameInfo(point, dimension);
        } else {
            return new FrameInfo();
        }
    }

    public Class<? extends TotalReport> getClassFromName(String reportClassSimpleName) throws ClassNotFoundException {
        String reportClassFullName = getThisPackage() + "." + reportClassSimpleName;
        return Class.forName(reportClassFullName).asSubclass(TotalReport.class);
    }

    private String getThisPackage(){
        String className = this.getClass().getName();
        int i = className.lastIndexOf('.');
        return className.substring(0, i);

    }

    @SuppressWarnings("unused")
    public ArrayList<ReportConfig> getReportConfigsForClass(Class<? extends TotalReport> reportClass) throws
            BackingStoreException, NoSuchFieldException, IllegalAccessException {
        ArrayList<ReportConfig> reportConfigs = new ArrayList<>();
        Preferences reportClassNode = prefs.node(getReportTypeName(reportClass));
        String[] reportNames = reportClassNode.childrenNames();
        for (String reportName : reportNames) {
            reportConfigs.add(new ReportConfig(reportClass, reportName));
        }
        return reportConfigs;
    }

    /*
    Gets aggregation controller from stored preferences
     */
    public AggregationController getAggregationControllerFromPrefs(Preferences reportPref)
            throws NoSuchFieldException, IllegalAccessException {
        String aggregationModeStr = reportPref.get(Prefs.AGGREGATION_MODE, null);
        if (aggregationModeStr != null) {
            return AggregationController.valueOf(aggregationModeStr);
        } else {
            return getStandardReportConfig(reportClass).getAggregationController();
        }

    }

    public void clearReportConfigFromPrefs()
            throws NoSuchFieldException, IllegalAccessException, BackingStoreException, ClassNotFoundException {
        Class<? extends TotalReport> reportClass = this.reportClass;
        Preferences reportNode = prefs.node(ReportConfig.getReportTypeName(reportClass)).node(reportName);
        reportNode.removeNode();
    }


    public LinkedList<Integer> getViewHeader() {
        return viewHeader;
    }

    public void setViewHeader(LinkedList<Integer> viewHeader) {
        this.viewHeader = viewHeader;
    }

    public AggregationController getAggregationController() {
        return aggregationController;
    }

    public void setAggregationController(AggregationController aggregationController) {
        this.aggregationController = aggregationController;
    }

    public boolean isDefaultConfig() {
        return isDefaultConfig;
    }

    public boolean useAverageCostBasis() {return useAverageCostBasis;}

    public void setStandardConfig(boolean defaultConfig) {
        this.isDefaultConfig = defaultConfig;
    }

    public boolean isOutputSingle() {
        return outputSingle;
    }

    public void setOutputSingle(boolean outputSingle) {
        this.outputSingle = outputSingle;
    }

    public void setUseAverageCostBasis(boolean useAverageCostBasis) {this.useAverageCostBasis = useAverageCostBasis;}

    public DateRange getDateRange() {
        return dateRange;
    }

    public HashSet<Integer> getExcludedAccountNums(){
        return excludedAccountNums;
    }

    public void setDateRange(DateRange dateRange) {
        this.dateRange = dateRange;
    }
    public void setExcludedAccountNums(HashSet<Integer> excludedAccountNums){
        this.excludedAccountNums = excludedAccountNums;
    }

    public int getNumFrozenColumns() {
        return numFrozenColumns;
    }

    public void setNumFrozenColumns(int numFrozenColumns) {
        this.numFrozenColumns = numFrozenColumns;
    }

    public boolean isClosedPosHidden() {
        return closedPosHidden;
    }

    public void setClosedPosHidden(boolean closedPosHidden) {
        this.closedPosHidden = closedPosHidden;
    }

    public Class<? extends TotalReport> getReportClass() {
        return reportClass;
    }

    public String getReportName() {
        return reportName;
    }

    public String getDescription(){
        return reportTypeName + ": " + reportName;
    }

    public String getReportTypeName() {
        return reportTypeName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    public FrameInfo getFrameInfo() {
        return frameInfo;
    }

    public void setFrameInfoToPrefs(JFrame frame){
        Point point = frame.getLocationOnScreen();
        Dimension dimension = new Dimension(frame.getWidth(), frame.getHeight());
        FrameInfo currentFrameInfo = new FrameInfo(point, dimension);
        this.frameInfo = currentFrameInfo;
        Preferences thisReportPrefs = prefs.node(reportTypeName).node(
                (this.isDefaultConfig() ? this.reportName : this.reportName.trim()));
        thisReportPrefs.put(Prefs.FRAMEINFO, currentFrameInfo.writeFrameInfoForPrefs());
    }


    /**
     * writes view header to a string where integers are separated by commas
     *
     * @return string to be saved to preferences
     */
    public String writeViewHeaderToString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.viewHeader.size(); i++) {
            sb.append(i == viewHeader.size() - 1 ? viewHeader.get(i) : viewHeader.get(i) + ",");
        }
        return sb.toString();
    }

    /**
     * writes view header to a string where integers are separated by commas
     *
     * @return string to be saved to preferences
     */
    public String writeExcludedAccountsToString() {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for(Integer excludedAccountNum : excludedAccountNums){
            sb.append(i == excludedAccountNums.size() - 1 ? excludedAccountNum : excludedAccountNum + ",");
            i++;
        }
        return sb.toString();
    }



    public String toString() {
        String nl = "\n";
        return "Report Class: " + reportTypeName + nl
                + "Report Name: " + this.reportName + nl
                + "Average Cost: " + this.useAverageCostBasis + nl
                + "Aggregation Mode: " + aggregationController.getDescription() + nl
                + "Output Single? " + outputSingle + nl
                + "Number Frozen Columns: " + numFrozenColumns + nl
                + "Closed Positions Hidden? " + isClosedPosHidden() + nl
                + "View Header: " + writeViewHeaderToString() + nl
                + "Excluded Account Nums: " + writeExcludedAccountsToString() + nl
                + "DateRange: " + dateRange.toString() + nl
                + "Is Default? " + isDefaultConfig + nl
                + "Frame Info: " + frameInfo.toString();
    }

    /**
     * Saves report config to preferences
     */
    public void saveReportConfig() {
        Preferences thisReportPrefs = prefs.node(reportTypeName).node(
                (this.isDefaultConfig() ? this.reportName : this.reportName.trim()));
        thisReportPrefs.putBoolean(Prefs.USE_AVERAGE_COST_BASIS, useAverageCostBasis);
        thisReportPrefs.put(Prefs.AGGREGATION_MODE, aggregationController.name());
        thisReportPrefs.putBoolean(Prefs.OUTPUT_SINGLE, outputSingle);
        thisReportPrefs.putInt(Prefs.NUM_FROZEN_COLUMNS, numFrozenColumns);
        thisReportPrefs.putBoolean(Prefs.CLOSED_POS_HIDDEN, closedPosHidden);
        thisReportPrefs.put(Prefs.VIEWHEADER, this.writeViewHeaderToString());
        thisReportPrefs.put(Prefs.EXCLUDEDACCOUNTNUMS, this.writeExcludedAccountsToString());
        thisReportPrefs.put(Prefs.DATERANGE, dateRange.toString());
        thisReportPrefs.putBoolean(Prefs.ISSTANDARD, isDefaultConfig);
        thisReportPrefs.put(Prefs.FRAMEINFO, frameInfo.writeFrameInfoForPrefs());
    }

    /**
     * Frame size and location information
     */
    static class FrameInfo {
        Point point;
        Dimension dimension;

        public FrameInfo(Point point, Dimension dimension) {
            this.point = point;
            this.dimension = dimension;
        }

        public FrameInfo() {
            FrameInfo defaultFrameInfo = getDefaultFrameInfo();
            this.point = defaultFrameInfo.point;
            this.dimension = defaultFrameInfo.dimension;
        }

        public String writeFrameInfoForPrefs() {
            String comma = ",";
            return point.x + comma + point.y + comma + dimension.width + comma + dimension.height;
        }

        public String toString() {
            return "x: " + point.x + " y: " + point.y
                    + " width: " + dimension.width + " height: " + dimension.height;
        }

        public Point getPoint() {
            return point;
        }

        public Dimension getDimension() {
            return dimension;
        }
    }


}
