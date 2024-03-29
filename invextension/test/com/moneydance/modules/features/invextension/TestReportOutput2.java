/*
 * TestReportOutput2.java
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
import com.moneydance.apps.md.model.RootAccount;

import java.io.File;
import java.util.ArrayList;


@SuppressWarnings("unused")
public class TestReportOutput2 {
    public static final File mdTestFile = new File("./resources/testMD02.md");
    public static final File testFile = new File("E:\\Temp\\testFile.csv");
    public static final int numFrozenColumns = 5; //Irrelevant for testing purposes
    public static final boolean closedPosHidden = true; //Irrelevant for testing purposes
    public static final String reportName = "TestName"; //Irrelevant for testing purposes
    //
    private static final int fromDateInt = 20090601;
    private static final int toDateInt = 20100601;
    public static final DateRange dateRange = new DateRange(fromDateInt, toDateInt, toDateInt);

    //
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void main(String[] args) throws Exception {
        RootAccount root = FileUtils.readAccountsFromFile(mdTestFile, null);
        BulkSecInfo currentInfo = new BulkSecInfo(root, ReportConfig.getStandardReportConfig(TotalFromToReport.class));
        ArrayList<String[]> transActivityReport = currentInfo
                .listAllTransValues(currentInfo.getInvestmentWrappers());
        File transActivityReportFile = new File("E:\\Temp"
                + "\\transActivityReport.csv");
        IOUtils.writeArrayListToCSV(TransactionValues.listTransValuesHeader(),
                transActivityReport, transActivityReportFile);

        TotalReport report;

        AggregationController aggregationController = AggregationController.INVACCT;
        boolean rptOutputSingle = false;
        ReportConfig reportConfig = new ReportConfig(TotalFromToReport.class, "Test Report",
                true, aggregationController, rptOutputSingle, numFrozenColumns, closedPosHidden,
                ReportConfig.getDefaultViewHeader(TotalSnapshotReport.MODEL_HEADER),
                ReportConfig.getDefaultExcludedAccounts(),  dateRange);
        report = new TotalSnapshotReport(reportConfig);
        report.calcReport(currentInfo);

        printReport(report, 0, 4);


    }


    private static void printReport(TotalReport report, int firstSort, int secondSort)
            throws NoSuchFieldException, IllegalAccessException {
        System.out.println("report size" + report.getReports().size());
        System.out.println("Object Length" + report.getReportTable().length);

        Object[][] reportTable = report.getReportTable();
        System.out.println("Report Table Length: " + reportTable.length);
        System.out.println("Report Table Width: " + reportTable[0].length);
        TotalReport.ReportTableModel model = new TotalReport.ReportTableModel(reportTable,
                report.getModelHeader());
        StringBuffer outBuffer = writeObjectToStringBuffer(reportTable);
        IOUtils.writeResultsToFile(outBuffer, testFile);
        System.out.println("Finished!");

        TotalReportOutputPane.createAndShowTable(report);
    }


    public static StringBuffer writeObjectToStringBuffer(Object[][] object) {
        StringBuffer outBuffer = new StringBuffer();
        for (Object[] objects : object) {
            for (int j = 0; j < objects.length; j++) {
                Object element = objects[j] == null ? "*NULL*" : objects[j];
                if (j == objects.length - 1) {
                    outBuffer.append(element.toString()).append("\r\n");
                } else {
                    outBuffer.append(element.toString()).append(",");
                }
            }
        }
        return outBuffer;

    }


}
