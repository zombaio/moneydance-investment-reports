/* CheckRepSnap.java
 * Copyright 2011 Dale K. Furrow . All rights reserved.
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, 
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY <COPYRIGHT HOLDER> ''AS IS'' AND ANY EXPRESS 
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 * IN NO EVENT SHALL <COPYRIGHT HOLDER> OR CONTRIBUTORS BE LIABLE FOR ANY 
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND 
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF 
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.moneydance.modules.features.invextension;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * Generates dump of  intermediate values in 
 * "Snap" Report to allow for auditing
 *
 * Version 1.0 
 * @author Dale Furrow
 *
 */
public class CheckRepSnap {
    // private static final int fromDateInt = 20090601;
    private static final int toDateInt = 20100601;
    public static final Class<InvestmentAccountWrapper> invAggClass = InvestmentAccountWrapper.class;
    public static final Class<Tradeable> tradeableAggClass = Tradeable.class;
    public static final boolean catHierarchy = false;
    public static final boolean rptOutputSingle = false;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void main(String[] args) throws Exception {
	BulkSecInfo currentInfo = BulkSecInfoTest.getBaseSecurityInfoAvgCost();

	TotalSnapshotReport snapshotReport = new TotalSnapshotReport(
		currentInfo, invAggClass, tradeableAggClass, catHierarchy,
		rptOutputSingle, toDateInt);
	ArrayList<ComponentReport> componentReports = snapshotReport
		.getReports();
	for (Iterator<ComponentReport> iterator = componentReports.iterator(); iterator
		.hasNext();) {
	    ComponentReport componentReport = iterator.next();
	    printSnap(componentReport);
	}

    }

    public static void printSnap(ComponentReport componentReport)
	    throws SecurityException, IllegalArgumentException,
	    NoSuchFieldException, IllegalAccessException {
	SecuritySnapshotReport snapLine = null;
	CompositeReport<?, ?> compositeReport = null;

	if (componentReport instanceof SecuritySnapshotReport) {
	    snapLine = (SecuritySnapshotReport) componentReport;
	} else {
	    compositeReport = (CompositeReport<?, ?>) componentReport;
	    snapLine = (SecuritySnapshotReport) compositeReport.aggregateReport;
	}

	String tab = "\u0009";
	System.out.println("\n" + "Report: Snap" + "\n");
	String acctName = compositeReport != null ? compositeReport.getName()
		: snapLine.getName();
	String acctTicker = compositeReport != null ? "NoTicker"
		: snapLine.currencyWrapper.ticker;

	System.out.println("Account: " + tab + acctName + tab + "Ticker:" + tab
		+ acctTicker);
	System.out.println("Snapshot Date: " + tab + snapLine.snapDateInt);
	printRetDateMap(snapLine.returnsStartDate, "Return Dates");
	printInputMap(snapLine.startPoses, "Start Positions");
	printInputMap(snapLine.startPrices, "Start Prices");
	printInputMap(snapLine.startValues, "Start Values");

	System.out.println("EndPos: " + tab + snapLine.endPos + tab
		+ "EndPrice: " + tab + snapLine.lastPrice + tab + "EndValue:"
		+ tab + snapLine.endValue);

	printInputMap(snapLine.incomes, "Income Amounts");
	printInputMap(snapLine.expenses, "Expense Amounts");

	System.out.println("All Maps Follow: \n");
	printAllPerfMaps(snapLine.mdMap, snapLine.arMap, snapLine.transMap);

	System.out.println("Returns: \n");
	System.out.println("1-Day Ret: " + tab + snapLine.totRet1Day + tab
		+ "1-Wk Ret: " + tab + snapLine.totRetWk + tab + "4-Wk Ret: "
		+ tab + snapLine.totRet4Wk + tab + "3-Mnth Ret: " + tab
		+ snapLine.totRet3Mnth + tab + "1-Yr Ret: " + tab
		+ snapLine.totRetYear + tab + "3-Yr Ret: " + tab
		+ snapLine.totRet3year + tab + "YTD Ret: " + tab
		+ snapLine.totRetYTD + tab + "All Ret: " + tab
		+ snapLine.totRetAll + tab + "All AnnRet: " + tab
		+ snapLine.annRetAll + tab);
    }

    public static void printRetDateMap(CategoryMap<Integer> categoryMap,
	    String msg) {
	StringBuilder outStr = new StringBuilder();
	String tab = "\u0009";
	outStr.append(msg + "\n");
	String[] retCats = { "PREV", "1Wk", "4Wk", "3Mnth", "1Yr", "3Yr",
		"YTD", "All" };
	for (String retCat : retCats) {
	    Integer value = categoryMap.get(retCat) == null ? 0 : categoryMap
		    .get(retCat);
	    String dateStr = value == 0 ? "N/A" : DateUtils
		    .convertToShort(value);
	    outStr.append(retCat).append(tab).append(dateStr).append(tab);
	}
	System.out.println(outStr.toString());
    }

    public static void printInputMap(CategoryMap<Double> categoryMap, String msg) {
	StringBuilder outStr = new StringBuilder();
	String tab = "\u0009";
	String[] retCats = { "PREV", "1Wk", "4Wk", "3Mnth", "1Yr", "3Yr",
		"YTD", "All" };
	outStr.append(msg + "\n");
	for (String retCat : retCats) {
	    Double value = categoryMap.get(retCat) == null ? Double.NaN
		    : categoryMap.get(retCat);
	    outStr.append(retCat).append(tab).append(value.toString())
		    .append(tab);
	}
	System.out.println(outStr.toString());
    }

    public static void printAllPerfMaps(CategoryMap<DateMap> categoryMap,
	    CategoryMap<DateMap> categoryMap2, CategoryMap<DateMap> categoryMap3) {
	String[] retCats = { "PREV", "1Wk", "4Wk", "3Mnth", "1Yr", "3Yr",
		"YTD", "All" };
	for (String retCat : retCats) {
	    DateMap mdMap = (DateMap) (categoryMap.get(retCat) == null ? new TreeMap<Integer, Double>()
		    : categoryMap.get(retCat));
	    DateMap arMap = (DateMap) (categoryMap2.get(retCat) == null ? new TreeMap<Integer, Double>()
		    : categoryMap2.get(retCat));
	    DateMap transMap = (DateMap) (categoryMap3.get(retCat) == null ? new TreeMap<Integer, Double>()
		    : categoryMap3.get(retCat));
	    CheckRepFromTo.printPerfMaps(arMap, mdMap, transMap, "\n"
		    + "Maps: " + retCat);
	}

    }

}
