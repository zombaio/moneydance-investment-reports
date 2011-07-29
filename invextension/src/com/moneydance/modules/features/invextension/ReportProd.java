/*
 *  SecReportPanel.java
 *  Copyright (C) 2010 Dale Furrow
 *  dkfurrow@google.com
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, a copy may be found at
 *  http://www.gnu.org/licenses/lgpl.html
 */
package com.moneydance.modules.features.invextension;

import com.moneydance.apps.md.model.Account;
import com.moneydance.modules.features.invextension.ReportTable.ColType;
import java.text.DateFormat;
import com.moneydance.modules.features.invextension.BulkSecInfo.AGG_TYPE;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.SortedSet;
import java.util.TreeMap;
import javax.swing.table.AbstractTableModel;
import com.moneydance.modules.features.invextension.ReportTable.ColSizeOption;

/** produces FromTo and Snap reports
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
*/
public final class ReportProd {

    public static final ColType[] ftColTypes = new ColType[] {
        ColType.STRING, ColType.STRING, ColType.STRING, ColType.DOUBLE3,ColType.DOUBLE3,
        ColType.DOUBLE2,ColType.DOUBLE2,ColType.DOUBLE2,ColType.DOUBLE2,ColType.DOUBLE2,
        ColType.DOUBLE2,ColType.DOUBLE2,ColType.DOUBLE2,ColType.DOUBLE2,ColType.DOUBLE2,
        ColType.DOUBLE2,ColType.DOUBLE2,ColType.DOUBLE2,ColType.DOUBLE2,ColType.DOUBLE2,
        ColType.PERCENT1,ColType.PERCENT1};
    
    public static final ColType[] snapColTypes = new ColType[] {
        ColType.STRING, ColType.STRING, ColType.STRING, ColType.DOUBLE2,ColType.DOUBLE3,
        ColType.DOUBLE2,ColType.DOUBLE2,ColType.DOUBLE2,ColType.PERCENT1,ColType.PERCENT1,
        ColType.PERCENT1,ColType.PERCENT1,ColType.PERCENT1,ColType.PERCENT1,ColType.PERCENT1,
        ColType.PERCENT1,ColType.PERCENT1,ColType.PERCENT1,ColType.DOUBLE2,ColType.DOUBLE2,
        ColType.DOUBLE2};
    
    
    

    private ReportProd(){
    }

    /**
     * produces FromTo Report for securities, accounts, and aggregate of accounts
     * @param currentInfo current transaction info
     * @param fromDateInt from date
     * @param toDateInt to date
     * @return ArrayList of String Arrays for all securities
     */
    public static Object[][] getFromToReport(BulkSecInfo currentInfo, int fromDateInt, int toDateInt) {
        Object[][]ftData = null;
        RepFromTo allInvFromTo = new RepFromTo(null, fromDateInt, toDateInt);
        RepFromTo allCashFromTo = new RepFromTo(null, fromDateInt, toDateInt);

        /*loop through investment accounts */
        for (Iterator it = currentInfo.invSec.keySet().iterator(); it.hasNext();) {
            Account invAcct = (Account) it.next();
            RepFromTo thisInvFromTo = new RepFromTo(invAcct, fromDateInt, toDateInt);
            /* loop through securities */
            for (Iterator it1 = currentInfo.invSec.get(invAcct).iterator(); it1.hasNext();) {
                Account secAcct = (Account) it1.next();
                SortedSet<TransValuesCum> transSet = currentInfo.transValuesCumMap.get(secAcct);
                RepFromTo thisSecFromTo = new RepFromTo(currentInfo, transSet, fromDateInt, toDateInt);
                ftData = addElement(ftData, thisSecFromTo.getRepFromToObject(AGG_TYPE.SEC));
                thisInvFromTo = addFT(thisSecFromTo, thisInvFromTo);
            } // end securities loop
            thisInvFromTo = getFTAggReturns(thisInvFromTo); //get aggregated returns for securities in account
            ftData = addElement(ftData, thisInvFromTo.getRepFromToObject(AGG_TYPE.ACCT_SEC));
            allInvFromTo = addFT(thisInvFromTo, allInvFromTo); // add to aggregate securities.

            SortedSet<TransValuesCum> parentSet =
                    currentInfo.transValuesCumMap.get(invAcct); //gets investment account transactions (bank txns)
            RepFromTo thisCashFromTo =
                    new RepFromTo(currentInfo, parentSet, fromDateInt, toDateInt); //get report for investment account
            allCashFromTo =
                    addFT(thisCashFromTo, allCashFromTo); //add to investment accounts (bank txns)
            RepFromTo cashReport =
                    getFTCashReturns(thisCashFromTo, thisInvFromTo); //get returns for cash account
            ftData = addElement(ftData, cashReport.getRepFromToObject(AGG_TYPE.ACCT_CASH));

            RepFromTo thisAggRetFromTo =
                    getFTAggRetWCash(thisCashFromTo, thisInvFromTo); //get  aggregated returns with cash accounted for
            ftData = addElement(ftData, thisAggRetFromTo.getRepFromToObject(AGG_TYPE.ACCT_SEC_PLUS_CASH));
        } //end investment account loop
        //get returns for aggregated investment accounts
        allInvFromTo =
                getFTAggReturns(allInvFromTo); //get aggregated returns from all securities
        ftData = addElement(ftData, allInvFromTo.getRepFromToObject(AGG_TYPE.ALL_SEC));

        RepFromTo allCashReport =
                getFTCashReturns(allCashFromTo, allInvFromTo); //get cash returns for all accounts
        ftData = addElement(ftData, allCashReport.getRepFromToObject(AGG_TYPE.ALL_CASH));

        RepFromTo allAggRetFromTo =
                getFTAggRetWCash(allCashFromTo, allInvFromTo); //get  agg returns w/ cash for all accounts
        ftData = addElement(ftData, allAggRetFromTo.getRepFromToObject(AGG_TYPE.ALL_SEC_PLUS_CASH));
        
        return ftData;



    }

    public static void outputFTObjToTable(int fromDateInt, int toDateInt,
	    Object[][] ftData) {
	//Report Table Code
        RptTableModel ftModel = new RptTableModel(ftData, RepFromTo.getRepFromToHeader());
        DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);
        String infoString = "Investment Performance--From: " +
                DateUtils.convertToShort(fromDateInt) + " To: " +
                DateUtils.convertToShort(toDateInt);
        ReportTable.CreateAndShowTable(ftModel, ftColTypes, 8 ,ColSizeOption.MAXCONTCOLRESIZE, 3, infoString);
    }

    /**
     * produces FromTo Report for securities, accounts, and aggregate of accounts
     * @param currentInfo current transaction info
     * @param snapDateInt report date
     * @return ArrayList of String Arrays for all securities
     */
    public static Object[][] getSnapReport(BulkSecInfo currentInfo, int snapDateInt) {
        Object[][] snapData = null;
        RepSnap allInvSnap = new RepSnap(null, snapDateInt);
        RepSnap allCashSnap = new RepSnap(null, snapDateInt);
        /*loop through investment accounts */
        for (Iterator it = currentInfo.invSec.keySet().iterator(); it.hasNext();) {
            Account invAcct = (Account) it.next();
            RepSnap thisInvSnap = new RepSnap(invAcct, snapDateInt);
            /* loop through securities */
            for (Iterator it1 = currentInfo.invSec.get(invAcct).iterator(); it1.hasNext();) {
                Account secAcct = (Account) it1.next();
                SortedSet<TransValuesCum> transSet = currentInfo.transValuesCumMap.get(secAcct);
                RepSnap thisSecSnap = new RepSnap(currentInfo, transSet, snapDateInt);
                snapData = addElement(snapData, thisSecSnap.getRepSnapObject(AGG_TYPE.SEC));
                thisInvSnap = addSnap(thisSecSnap, thisInvSnap);
            }// end securities loop
            RepSnap thisInvRepSnap =
                    getSnapAggReturns(thisInvSnap); //get aggregated returns for securities
            snapData = addElement(snapData, thisInvRepSnap.getRepSnapObject(AGG_TYPE.ACCT_SEC));
            allInvSnap = addSnap(thisInvSnap, allInvSnap); // add to aggregate securities.

            SortedSet<TransValuesCum> parentSet =
                    currentInfo.transValuesCumMap.get(invAcct);
            RepSnap thisCashSnap = new RepSnap(currentInfo, parentSet, snapDateInt);
            allCashSnap =
                    addSnap(thisCashSnap, allCashSnap); //add to cash accounts
            RepSnap cashReport =
                    getSnapCashReturns(thisCashSnap, thisInvSnap); //get returns for cash account
            snapData = addElement(snapData, cashReport.getRepSnapObject(AGG_TYPE.ACCT_CASH));

            RepSnap thisAggRetSnap =
                    getSnapAggRetWCash(thisCashSnap, thisInvSnap); //get  aggregated returns with cash accounted for
            snapData = addElement(snapData, thisAggRetSnap.getRepSnapObject(AGG_TYPE.ACCT_SEC_PLUS_CASH));
        }//end investment account loop

        //get returns for aggregated investment accounts
        allInvSnap = getSnapAggReturns(allInvSnap); //get aggregated returns from all securities
        snapData = addElement(snapData, allInvSnap.getRepSnapObject(AGG_TYPE.ALL_SEC));

        RepSnap allCashReport = getSnapCashReturns(allCashSnap, allInvSnap); //get cash returns for all accounts
        snapData = addElement(snapData, allCashReport.getRepSnapObject(AGG_TYPE.ALL_CASH));

        RepSnap allAggRetSnap = getSnapAggRetWCash(allCashSnap, allInvSnap); //get  agg returns w/ cash for all accounts
        snapData = addElement(snapData, allAggRetSnap.getRepSnapObject(AGG_TYPE.ALL_SEC_PLUS_CASH));
        
        return snapData;


    }

    public static void outputSnapObjToTable(int snapDateInt,
	    Object[][] SnapData) {
	//Report Table Code
        RptTableModel repSnapModel = new RptTableModel(SnapData, RepSnap.getRepSnapHeader());
        DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);
        String infoString = "Investment Performance Snapshot: " + DateUtils.convertToShort(snapDateInt);
                
        ReportTable.CreateAndShowTable(repSnapModel, snapColTypes, 5, ColSizeOption.MAXCONTCOLRESIZE, 3, infoString);
    }
    public static final Comparator<String[]> PrntAcct_Order =
            new Comparator<String[]>() {

                public int compare(String[] o1, String[] o2) {
                    int parentCmp = o1[0].compareTo(o2[0]);
                    return (parentCmp == 0 ? o1[1].compareTo(o2[1]) : parentCmp);
                }
            };

    /*
     * Next three methods are used to combine date maps for aggregate calculations  +

    /**
     * combines datemaps, either adding or subtracting cash flows
     * @param map1 input map
     * @param map2 input map
     * @param combType either "add" or "subtract"
     * @return output map
     */
    public static TreeMap<Integer, Double> combineDateMaps(TreeMap<Integer, Double> map1, TreeMap<Integer, Double> map2, String combType) {
        TreeMap<Integer, Double> outMap = new TreeMap<Integer, Double>();
        if (map1 != null) outMap.putAll(map1);

        if (map2 != null) {
            for (Iterator it = map2.keySet().iterator(); it.hasNext();) {
                Integer dateint2 = (Integer) it.next();
                Double value2 = map2.get(dateint2);

                if (outMap.get(dateint2) == null) {
                    if ("add".equals(combType)) {
                        outMap.put(dateint2,
                                value2);
                    }
                    if ("subtract".equals(combType)) {
                        outMap.put(dateint2,
                                -value2);
                    }
                } else {
                    if ("add".equals(combType)) {
                        outMap.put(dateint2,
                                map1 == null ? 0 : map1.get(dateint2) + value2);
                    }
                    if ("subtract".equals(combType)) {
                        outMap.put(dateint2,
                                 map1 == null ? 0 : map1.get(dateint2) - value2);
                    }
                }
            }
        }
        return outMap;
    }

    /*
     * combines intermediate values for start value, end value, income, expense
     * for aggregate mod-dietz returns calculations.
     */
    public static LinkedHashMap<String, Double> addDoubleMap(
            LinkedHashMap<String, Double> map1,
            LinkedHashMap<String, Double> map2) {
        LinkedHashMap<String, Double> outMap =
                new LinkedHashMap<String, Double>(map1);
        if (map2 != null) {
            for (Iterator it = map2.keySet().iterator(); it.hasNext();) {
                String retCat2 = (String) it.next();
                Double value2 = map2.get(retCat2);
                if (map1.get(retCat2) == null) {
                    outMap.put(retCat2, value2);
                } else {
                    outMap.put(retCat2, map1.get(retCat2) + value2);
                }
            }

        }
        return outMap;
    }

    /**
     * combines map of datemaps for Snap Reports, either adding or subtracting cash flows
     * @param map1 input map
     * @param map2 input map
     * @param retType either "add" or "subtract"
     * @return output map
     */
    public static LinkedHashMap<String, TreeMap<Integer, Double>> combineDateMapMap(LinkedHashMap<String, TreeMap<Integer, Double>> map1,
            LinkedHashMap<String, TreeMap<Integer, Double>> map2,
            String retType) {
        LinkedHashMap<String, TreeMap<Integer, Double>> outMap =
                new LinkedHashMap<String, TreeMap<Integer, Double>>(map1);
        if (map2 != null) {
            for (Iterator it = map2.keySet().iterator(); it.hasNext();) {
                String retCat2 = (String) it.next();
                TreeMap<Integer, Double> treeMap2 = map2.get(retCat2);
                if (map1.get(retCat2) == null) {
                    outMap.put(retCat2, treeMap2);
                } else {
                    TreeMap<Integer, Double> treeMap1 = map1.get(retCat2);
                    TreeMap<Integer, Double> tempMap =
                            new TreeMap<Integer, Double>(combineDateMaps(treeMap1, treeMap2, retType));
                    outMap.put(retCat2, tempMap);
                }
            }
        }
        return outMap;
    }

    private static LinkedHashMap<String, Integer> combineCatMap(LinkedHashMap<String, Integer> map1, LinkedHashMap<String, Integer> map2) {
        LinkedHashMap<String, Integer> outMap = new LinkedHashMap<String, Integer>(map1);
        for (Iterator it = map2.keySet().iterator(); it.hasNext();) {
            String cat2 = (String) it.next();
            Integer date2 = map2.get(cat2);
            if (map1.get(cat2) == null) {
                outMap.put(cat2, date2);
            } else {
                outMap.put(cat2, Math.min(map1.get(cat2), date2));
            }
        }
        return outMap;
    }

    public static double longToDouble(long inLong) {
        Long newInLong = new Long(inLong);
        return Double.valueOf(newInLong.toString());
    }

    /**
     * adds RepFromTo objects, in order to aggregate data at
     * account level or at aggregate level
     * @param thisSecFromTo RepFromTo associated with Security
     * @param thisInvFromTo RepFromTo associated with Investment Account
     * @return output RepFromTo
     */
    private static RepFromTo addFT(RepFromTo thisSecFromTo, RepFromTo thisInvFromTo) {
        RepFromTo outObj = thisInvFromTo;
        outObj.fromDateInt = thisInvFromTo.fromDateInt;
        outObj.toDateInt = thisInvFromTo.toDateInt;
        outObj.account = thisInvFromTo.account;
        outObj.startPos = 0.0;
        outObj.endPos = 0.0;
        outObj.startPrice = 0.0;
        outObj.endPrice = 0.0;
        outObj.startValue = thisInvFromTo.startValue + thisSecFromTo.startValue;
        outObj.endValue = thisInvFromTo.endValue + thisSecFromTo.endValue;
        outObj.buy = thisInvFromTo.buy + thisSecFromTo.buy;
        outObj.sell = thisInvFromTo.sell + thisSecFromTo.sell;
        outObj.shortSell = thisInvFromTo.shortSell + thisSecFromTo.shortSell;
        outObj.coverShort = thisInvFromTo.coverShort + thisSecFromTo.coverShort;
        outObj.income = thisInvFromTo.income + thisSecFromTo.income;
        outObj.expense = thisInvFromTo.expense + thisSecFromTo.expense;
        outObj.longBasis = thisInvFromTo.longBasis + thisSecFromTo.longBasis;
        outObj.shortBasis = thisInvFromTo.shortBasis + thisSecFromTo.shortBasis;
        outObj.realizedGain = thisInvFromTo.realizedGain + thisSecFromTo.realizedGain;
        outObj.unrealizedGain = thisInvFromTo.unrealizedGain + thisSecFromTo.unrealizedGain;
        outObj.totalGain = thisInvFromTo.totalGain + thisSecFromTo.totalGain;
        outObj.startCash = thisInvFromTo.startCash + thisSecFromTo.startCash;
        outObj.endCash = thisInvFromTo.endCash + thisSecFromTo.endCash;

        if (thisSecFromTo.account == null) { //need to handle both cases of aggregation at investment account and across investment accounts
            outObj.initBalance = thisSecFromTo.initBalance;
        } else if (thisSecFromTo.account.getAccountType() == Account.ACCOUNT_TYPE_INVESTMENT) {
            outObj.initBalance = thisInvFromTo.initBalance + thisSecFromTo.initBalance;
        }

        outObj.arMap = combineDateMaps(thisInvFromTo.arMap, thisSecFromTo.arMap, "add");
        outObj.mdMap = combineDateMaps(thisInvFromTo.mdMap, thisSecFromTo.mdMap, "add");
        outObj.transMap = combineDateMaps(thisInvFromTo.transMap, thisSecFromTo.transMap, "add");
        outObj.mdReturn = 0.0;
        outObj.annualPercentReturn = 0.0;

        return outObj;
    }

    /**
     * adds RepSnap objects, in order to aggregate data at
     * account level or at aggregate level
     * @param thisSecFromTo RepSnap associated with Security
     * @param thisInvFromTo RepSnap associated with Investment Account
     * @return output RepSnap
     */
    private static RepSnap addSnap(RepSnap thisSecSnap, RepSnap thisInvSnap) {
        RepSnap outObj = thisInvSnap;

        outObj.lastPrice = 0.0;
        outObj.endPos = 0.0;
        outObj.endValue = thisSecSnap.endValue + thisInvSnap.endValue;
        outObj.endCash = thisSecSnap.endCash + thisInvSnap.endCash;
        if (thisSecSnap.account == null) { //need to handle both cases of aggregation at investment account and across investment accounts
            outObj.initBalance = thisSecSnap.initBalance;
        } else if (thisSecSnap.account.getAccountType() == Account.ACCOUNT_TYPE_INVESTMENT) {
            outObj.initBalance = thisInvSnap.initBalance + thisSecSnap.initBalance;
        }
        outObj.avgCostBasis = thisSecSnap.avgCostBasis + thisInvSnap.avgCostBasis;
        outObj.absPriceChange = 0.0;
        outObj.pctPriceChange = 0.0;
        outObj.absValueChange = thisSecSnap.absValueChange + thisInvSnap.absValueChange;
        outObj.income = thisSecSnap.income + thisInvSnap.income;
        outObj.totalGain = thisSecSnap.totalGain + thisInvSnap.totalGain;
        outObj.totRetAll = 0.0;
        outObj.annRetAll = 0.0;
        outObj.totRet1Day = 0.0;
        outObj.totRetWk = 0.0;
        outObj.totRet4Wk = 0.0;
        outObj.totRet3Mnth = 0.0;
        outObj.totRetYTD = 0.0;
        outObj.totRetYear = 0.0;
        outObj.totRet3year = 0.0;

        outObj.retDateMap = combineCatMap(thisSecSnap.retDateMap, thisInvSnap.retDateMap);
        outObj.startValues = addDoubleMap(thisInvSnap.startValues, thisSecSnap.startValues);
        outObj.incomes = addDoubleMap(thisInvSnap.incomes, thisSecSnap.incomes);
        outObj.expenses = addDoubleMap(thisInvSnap.expenses, thisSecSnap.expenses);
        outObj.mdReturns = thisInvSnap.mdReturns;
        outObj.startCashs = addDoubleMap(thisInvSnap.startCashs, thisSecSnap.startCashs);

        outObj.mdMap = combineDateMapMap(thisInvSnap.mdMap, thisSecSnap.mdMap, "add");
        outObj.arMap = combineDateMapMap(thisInvSnap.arMap, thisSecSnap.arMap, "add");
        outObj.transMap = combineDateMapMap(thisInvSnap.transMap, thisSecSnap.transMap, "add");

        return outObj;
    }

    /**
     * gets returns as Investment Account level for aggregated securities
     * @param thisInvFromTo aggregated securities RepFromTo
     * @return RepFromTo with correct return information
     */
    private static RepFromTo getFTAggReturns(RepFromTo thisInvFromTo) {
        RepFromTo outObj = thisInvFromTo;
        //get Mod-Dietz Returns
        double mdReturnVal = RepFromTo.getMDCalc(outObj.startValue,
                outObj.endValue, outObj.income, outObj.expense,
                outObj.mdMap);
//        outObj.RepFromTo.this.mdReturn = thisReturn;
        outObj.mdReturn = mdReturnVal;

        //add start and end values to return date maps
        if (thisInvFromTo.startValue != 0) {
            RepFromTo.addValueToDateMap(thisInvFromTo.arMap,
                    thisInvFromTo.fromDateInt, -thisInvFromTo.startValue);
        }
        if (thisInvFromTo.endValue != 0) {
            RepFromTo.addValueToDateMap(thisInvFromTo.arMap,
                    thisInvFromTo.toDateInt, thisInvFromTo.endValue);
        }

        //get annualized returns
        outObj.annualPercentReturn = RepFromTo.getAnnualReturn(thisInvFromTo.arMap, mdReturnVal);

        //remove start and end values from return date maps (to avoid conflicts in aggregation)
        if (thisInvFromTo.startValue != 0) {
            RepFromTo.addValueToDateMap(thisInvFromTo.arMap,
                    thisInvFromTo.fromDateInt, thisInvFromTo.startValue);
        }
        if (thisInvFromTo.endValue != 0) {
            RepFromTo.addValueToDateMap(thisInvFromTo.arMap,
                    thisInvFromTo.toDateInt, -thisInvFromTo.endValue);
        }

        return outObj;

    }

    /**
     * gets returns as Investment Account level for aggregated securities
     * @param thisInvFromTo aggregated securities RepSnap
     * @return RepSnap with correct return information
     */
    private static RepSnap getSnapAggReturns(RepSnap thisInvSnap) {
        RepSnap outObj = thisInvSnap;
//

        for (Iterator<String> it1 = thisInvSnap.retDateMap.keySet().iterator(); it1.hasNext();) {
            String retCat = it1.next();
            // get MD returns on all start dates, only get annualized return for "All" dates

            outObj.mdReturns.put(retCat, RepFromTo.getMDCalc(thisInvSnap.startValues.get(retCat),
                    thisInvSnap.endValue, thisInvSnap.incomes.get(retCat), thisInvSnap.expenses.get(retCat),
                    thisInvSnap.mdMap.get(retCat)));

            if ("All".equals(retCat)) {
                //add start and end values to return date maps
                if (thisInvSnap.startValues.get(retCat) != 0.0) {
                    RepFromTo.addValueToDateMap(thisInvSnap.arMap.get(retCat),
                            thisInvSnap.retDateMap.get(retCat), -thisInvSnap.startValues.get(retCat));
                }
                if (thisInvSnap.endValue != 0.0) {
                    RepFromTo.addValueToDateMap(thisInvSnap.arMap.get(retCat),
                            thisInvSnap.snapDateInt, thisInvSnap.endValue);
                }
                //get return
                outObj.annRetAll = RepFromTo.getAnnualReturn(thisInvSnap.arMap.get(retCat), outObj.mdReturns.get("All"));
                outObj.income = thisInvSnap.incomes.get(retCat);

                //remove start and end values from return date maps (to avoid conflicts in aggregation)
                if (thisInvSnap.startValues.get(retCat) != 0.0) {
                    RepFromTo.addValueToDateMap(thisInvSnap.arMap.get(retCat),
                            thisInvSnap.retDateMap.get(retCat), thisInvSnap.startValues.get(retCat));
                }
                if (thisInvSnap.endValue != 0.0) {
                    RepFromTo.addValueToDateMap(thisInvSnap.arMap.get(retCat),
                            thisInvSnap.snapDateInt, -thisInvSnap.endValue);
                }

            }
        }
        outObj.totRet1Day = thisInvSnap.retDateMap.get("PREV") == null ? Double.NaN : outObj.mdReturns.get("PREV");
        outObj.totRetAll = thisInvSnap.retDateMap.get("All") == null ? Double.NaN : outObj.mdReturns.get("All");
        outObj.totRetWk = thisInvSnap.retDateMap.get("1Wk") == null ? Double.NaN : outObj.mdReturns.get("1Wk");
        outObj.totRet4Wk = thisInvSnap.retDateMap.get("4Wk") == null ? Double.NaN : outObj.mdReturns.get("4Wk");
        outObj.totRet3Mnth = thisInvSnap.retDateMap.get("3Mnth") == null ? Double.NaN : outObj.mdReturns.get("3Mnth");
        outObj.totRetYear = thisInvSnap.retDateMap.get("1Yr") == null ? Double.NaN : outObj.mdReturns.get("1Yr");
        outObj.totRet3year = thisInvSnap.retDateMap.get("3Yr") == null ? Double.NaN : outObj.mdReturns.get("3Yr");
        outObj.totRetYTD = thisInvSnap.retDateMap.get("YTD") == null ? Double.NaN : outObj.mdReturns.get("YTD");

        return outObj;

    }

    /**
     * gets RepFromTo for cash associated with an Investment Account
     * @param thisCashFromTo RepFromTo associated with Account "bank" transactions
     * @param thisInvFromTo RepFromTo associated with Securities
     * @return RepFrontTo representing income/returns for cash portion of Investment Account
     */
    private static RepFromTo getFTCashReturns(RepFromTo thisCashFromTo, RepFromTo thisInvFromTo) {
        //cashValue has start, end cash positions, income and expenses
        RepFromTo cashValue = new RepFromTo(thisInvFromTo.account, thisInvFromTo.fromDateInt, thisInvFromTo.toDateInt);
        //comboTransMDMap has purchases/sales of cash (i.e. reverse of security transactions)
        //start by adding transfers in and out of securities and investment accounts
        TreeMap<Integer, Double> comboTransMDMap = combineDateMaps(thisInvFromTo.transMap, thisCashFromTo.transMap, "add");

        //generate starting and ending cash balances, non-security related acount transactions
        double initBal = thisCashFromTo.initBalance;
        cashValue.startValue = cleanedValue(thisInvFromTo.startCash + thisCashFromTo.startCash + initBal);
        cashValue.endValue = cleanedValue(thisInvFromTo.endCash + thisCashFromTo.endCash + initBal);
        cashValue.startPos = cashValue.startValue;
        cashValue.endPos = cashValue.endValue;
        cashValue.income = thisCashFromTo.income;
        cashValue.expense = thisCashFromTo.expense;

        /*now add transfer map to map of buys/sells/income/expense (effectively, purchase/sales of cash
         caused by security activity*/
        comboTransMDMap = combineDateMaps(comboTransMDMap, thisInvFromTo.arMap, "add");
         /* cashRetMap effectively reverses sign of previous map (so cash
         buys/sells with correct sign for returns calc), and adds
         account-level income/expense transactions from arMap (e.g. account interest)*/
        TreeMap<Integer, Double> cashRetMap = combineDateMaps(thisCashFromTo.arMap, comboTransMDMap, "subtract");
        /* this handles case where fromDateInt < first transaction,
         AND initBal != 0 (i.e. startValue = initBal. In that case,
         start date needs to be adjusted to day prior to  first transaction date */
        int adjFromDateInt = cashValue.fromDateInt;
        int minDateInt = comboTransMDMap.isEmpty() ? 0 :
            DateUtils.getPrevBusinessDay(comboTransMDMap.firstKey());
        if(cashValue.startValue == initBal && cashValue.fromDateInt <= minDateInt)
            adjFromDateInt = Math.max(cashValue.fromDateInt, minDateInt );
        // add dummy (zero) values to Mod-Dietz date maps, start and end to return maps
        if(Math.abs(cashValue.startValue) > 0.0001){
            RepFromTo.addValueToDateMap(comboTransMDMap, adjFromDateInt, 0.0);
            RepFromTo.addValueToDateMap(cashRetMap, adjFromDateInt, -cashValue.startValue);
        }
        if(Math.abs(cashValue.endValue) > 0.0001){
            RepFromTo.addValueToDateMap(comboTransMDMap, thisInvFromTo.toDateInt, 0.0);
            RepFromTo.addValueToDateMap(cashRetMap, thisInvFromTo.toDateInt, cashValue.endValue);//add start and end values w/ cash balances for ret Calc
        }
        //calculate returns
        cashValue.mdReturn = RepFromTo.getMDCalc(cashValue.startValue,
                cashValue.endValue, cashValue.income, cashValue.expense, comboTransMDMap);
        cashValue.annualPercentReturn = RepFromTo.getAnnualReturn(cashRetMap, cashValue.mdReturn);
         return cashValue;
    }

    /**
     * gets RepSnap for cash associated with an Investment Account
     * @param thisCashSnap RepSnap associated with Account "bank" transactions
     * @param thisInvSnap RepSnapo associated with Securities
     * @return RepSnap representing income/returns for cash portion of Investment Account
     */
    private static RepSnap getSnapCashReturns(RepSnap thisCashSnap, RepSnap thisInvSnap) {
        //cashValue has start, end cash positions, income and expenses
        RepSnap cashValue = new RepSnap(thisInvSnap.account, thisInvSnap.snapDateInt);
        //comboTransMDMap has purchases/sales of cash (i.e. reverse of security transactions)
        //start by adding transfers in and out of securities and investment accounts
        LinkedHashMap<String, TreeMap<Integer, Double>> comboTransMDMap =
                combineDateMapMap(thisInvSnap.transMap, thisCashSnap.transMap, "add");
        //may need to adjust start dates, so create map for that
        LinkedHashMap<String, Integer> adjRetDateMap = 
                new LinkedHashMap<String, Integer>(thisInvSnap.retDateMap);


        double initBal = thisCashSnap.initBalance;
        //generate starting and ending cash balances, non-security related acount transactions
        for (Iterator it = thisInvSnap.retDateMap.keySet().iterator(); it.hasNext();) {
            String retCat = (String) it.next();
            cashValue.startValues.put(retCat, cleanedValue(thisInvSnap.startCashs.get(retCat)
                    + thisCashSnap.startCashs.get(retCat) + initBal));
            cashValue.incomes.put(retCat, thisCashSnap.incomes.get(retCat));
            cashValue.expenses.put(retCat, thisCashSnap.expenses.get(retCat));
            /* this handles case where fromDateInt < first transaction,
            AND initBal != 0 (i.e. startValue = initBal. In that case,
            start date needs to be adjusted to day prior to  first transaction date */
            int minDateInt = comboTransMDMap.get(retCat).isEmpty()? 0 :
                DateUtils.getPrevBusinessDay(
                comboTransMDMap.get(retCat).firstKey());
            if(cashValue.startValues.get(retCat) == initBal &&
                    thisInvSnap.retDateMap.get(retCat) <= minDateInt)
                adjRetDateMap.put(retCat, Math.max(
                        thisInvSnap.retDateMap.get(retCat), minDateInt));
        }

        cashValue.endValue = cleanedValue(thisInvSnap.endCash + thisCashSnap.endCash + initBal);
        cashValue.endPos = cashValue.endValue;
        cashValue.income = thisCashSnap.income; //note, we do not display expenses in this object
        //but they are tracked for returns calculations

        LinkedHashMap<String, TreeMap<Integer, Double>> adjAnnRetValues =
                new LinkedHashMap<String, TreeMap<Integer, Double>>(thisInvSnap.arMap);
        for (Iterator it = thisInvSnap.retDateMap.keySet().iterator(); it.hasNext();) {
            String retCat = (String) it.next();
            Integer thisFromDateInt = adjRetDateMap.get(retCat);
            // add dummy values to date maps (if start and end values exist)
            if (Math.abs(cashValue.startValues.get(retCat)) > 0.0001) RepFromTo.addValueToDateMap(
                    adjAnnRetValues.get(retCat), thisFromDateInt, 0.0);
            if (Math.abs(cashValue.endValue) > 0.0001) RepFromTo.addValueToDateMap(
                    adjAnnRetValues.get(retCat), cashValue.snapDateInt, 0.0);
        }

        /*now add transfer map to map of buys/sells/income/expense (effectively, purchase/sales of cash
         caused by security activity*/
        comboTransMDMap = combineDateMapMap(comboTransMDMap, adjAnnRetValues, "add");
        /*calculate period returns and annual return for all dates */
        for (Iterator it = thisInvSnap.retDateMap.keySet().iterator(); it.hasNext();) {
            String retCat = (String) it.next();
            double thisPercentReturn = RepFromTo.getMDCalc(cashValue.startValues.get(retCat),
                    cashValue.endValue, cashValue.incomes.get(retCat),
                    cashValue.expenses.get(retCat), comboTransMDMap.get(retCat));
            cashValue.mdReturns.put(retCat, thisPercentReturn);

            if ("All".equals(retCat)) {
                /* cashRetMap effectively reverses sign of previous map (so cash
                 buys/sells with correct sign for returns calc), and adds to that
                 account-level income/expense transactions from arMap (e.g. account interest)*/
                TreeMap<Integer, Double> cashRetMap = combineDateMaps(
                        thisCashSnap.arMap.get(retCat),
                        comboTransMDMap.get(retCat), "subtract");
                // add start and end values to date map
                Integer thisFromDateInt = adjRetDateMap.get(retCat);
                if(Math.abs(cashValue.startValues.get(retCat)) > 0.0001)
                    RepFromTo.addValueToDateMap(cashRetMap,
                            thisFromDateInt, -cashValue.startValues.get(retCat));
                if(Math.abs(cashValue.endValue) > 0.0001)
                    RepFromTo.addValueToDateMap(cashRetMap,
                        thisInvSnap.snapDateInt, cashValue.endValue);
                cashValue.annRetAll = RepFromTo.getAnnualReturn(cashRetMap,
                        cashValue.mdReturns.get(retCat));
                cashValue.income = thisCashSnap.incomes.get(retCat);
            }
        }
        cashValue.totRet1Day = thisInvSnap.retDateMap.get("PREV") == null ? Double.NaN : cashValue.mdReturns.get("PREV");
        cashValue.totRetAll = thisInvSnap.retDateMap.get("All") == null ? Double.NaN : cashValue.mdReturns.get("All");
        cashValue.totRetWk = thisInvSnap.retDateMap.get("1Wk") == null ? Double.NaN : cashValue.mdReturns.get("1Wk");
        cashValue.totRet4Wk = thisInvSnap.retDateMap.get("4Wk") == null ? Double.NaN : cashValue.mdReturns.get("4Wk");
        cashValue.totRet3Mnth = thisInvSnap.retDateMap.get("3Mnth") == null ? Double.NaN : cashValue.mdReturns.get("3Mnth");
        cashValue.totRetYear = thisInvSnap.retDateMap.get("1Yr") == null ? Double.NaN : cashValue.mdReturns.get("1Yr");
        cashValue.totRet3year = thisInvSnap.retDateMap.get("3Yr") == null ? Double.NaN : cashValue.mdReturns.get("3Yr");
        cashValue.totRetYTD = thisInvSnap.retDateMap.get("YTD") == null ? Double.NaN : cashValue.mdReturns.get("YTD");

        return cashValue;
    }

    /**
     * gets RepFromTo for Investment Account with Associated Cash accounted for as a Security.
     * @param thisCashFromTo RepFromTo associated with Account "bank" transactions
     * @param thisInvFromTo RepFromTo associated with Securities
     * @return RepFrontTo representing income/returns for Investment Account,
     * Cash and Securities included
     */
    private static RepFromTo getFTAggRetWCash(RepFromTo thisCashFromTo, RepFromTo thisInvFromTo) {
        RepFromTo outObj = new RepFromTo(thisInvFromTo.account, thisInvFromTo.fromDateInt, thisInvFromTo.toDateInt);

        //copy over aggregate values from aggregated securities
        outObj.buy = thisInvFromTo.buy;
        outObj.sell = thisInvFromTo.sell;
        outObj.shortSell = thisInvFromTo.shortSell;
        outObj.coverShort = thisInvFromTo.coverShort;
        outObj.longBasis = thisInvFromTo.longBasis;
        outObj.shortBasis = thisInvFromTo.shortBasis;
        outObj.realizedGain = thisInvFromTo.realizedGain;
        outObj.unrealizedGain = thisInvFromTo.unrealizedGain;
        outObj.totalGain = thisInvFromTo.totalGain;
        //add balance sheet and income statement values where applicable
        outObj.startValue = thisInvFromTo.startValue + thisCashFromTo.startValue;
        outObj.endValue = thisInvFromTo.endValue + thisCashFromTo.endValue;
        outObj.income = thisInvFromTo.income + thisCashFromTo.income;
        outObj.expense = thisInvFromTo.expense + thisCashFromTo.expense;
        outObj.startCash = thisInvFromTo.startCash + thisCashFromTo.startCash;
        outObj.endCash = thisInvFromTo.endCash + thisCashFromTo.endCash;

        //combine transfer date map
        outObj.transMap = combineDateMaps(thisInvFromTo.transMap, thisCashFromTo.transMap, "add");

        //get correct start and end balances w/ cash accounted for
        double initBal = thisCashFromTo.initBalance;
        outObj.startValue = cleanedValue(outObj.startValue + outObj.startCash + initBal);
        outObj.endValue = cleanedValue(outObj.endValue + outObj.endCash + initBal);

        //from account returns perspective, only transfers matter, so they
        //become the "buys" and "sells" for MD returns calculations and annual returns calcs

        //get MD returns
        TreeMap<Integer, Double> mdMap = outObj.transMap;
        /* reverse transfer map for returns calc purposes */
        TreeMap<Integer, Double> retMap = combineDateMaps(null, outObj.transMap, "subtract");
        /* this handles case where fromDateInt < first transaction,
        AND initBal != 0 (i.e. startValue = initBal). In that case,
        start date needs to be adjusted to day prior to  first transaction date */
        int adjFromDateInt = outObj.fromDateInt;
        int minDateInt = mdMap.isEmpty() ? 0 :
                    DateUtils.getPrevBusinessDay(mdMap.firstKey());
        if(outObj.startValue == initBal && outObj.fromDateInt <= minDateInt)
            adjFromDateInt = Math.max(outObj.fromDateInt, minDateInt );
        // add dummy values to Mod-Dietz date maps, start and end to return maps
        if(Math.abs(outObj.startValue) > 0.0001){
            RepFromTo.addValueToDateMap(mdMap, adjFromDateInt, 0.0);
            RepFromTo.addValueToDateMap(retMap, adjFromDateInt, -outObj.startValue);
        }
        if(Math.abs(outObj.endValue) > 0.0001){
            RepFromTo.addValueToDateMap(mdMap, outObj.toDateInt, 0.0);
            RepFromTo.addValueToDateMap(retMap, thisInvFromTo.toDateInt, outObj.endValue);
        }
        /*calc returns (note--no income/expenses since only transfers are considered
        i.e. endValue includes income/expenses*/
        double allMDReturn = RepFromTo.getMDCalc(outObj.startValue, outObj.endValue, 0.0, 0.0,
                mdMap);
        outObj.mdReturn = allMDReturn;
        //get annualized returns
        outObj.annualPercentReturn = RepFromTo.getAnnualReturn(retMap, allMDReturn);

        return outObj;
    }

    /**
     * gets RepSnap for Investment Account with Associated Cash accounted for as a Security.
     * @param thisCashSnap RepSnap associated with Account "bank" transactions
     * @param thisInvSnap RepSnap associated with Securities
     * @return RepFrontTo representing income/returns for Investment Account,
     * Cash and Securities included
     */
    private static RepSnap getSnapAggRetWCash(RepSnap thisCashSnap, RepSnap thisInvSnap) {
        RepSnap outObj = new RepSnap(thisInvSnap.account, thisInvSnap.snapDateInt);

        LinkedHashMap<String, Integer> adjRetDateMap =
                new LinkedHashMap<String, Integer>(thisInvSnap.retDateMap);

        double initBal = thisCashSnap.initBalance;

        //copy over aggregate values from aggregated securities
        outObj.totalGain = thisInvSnap.totalGain;
        outObj.income = thisInvSnap.income + thisCashSnap.income;
        outObj.absValueChange = thisInvSnap.absValueChange;
        //ending balance sheet values

        //combine transfer date maps
        outObj.transMap = combineDateMapMap(thisInvSnap.transMap, thisCashSnap.transMap, "add");

        //get correct start and end balances w/ cash accounted for
        for (Iterator it = thisInvSnap.retDateMap.keySet().iterator(); it.hasNext();) {
            String retCat = (String) it.next();
            outObj.startCashs.put(retCat, thisInvSnap.startCashs.get(retCat)
                    + thisCashSnap.startCashs.get(retCat));
            outObj.startValues.put(retCat, cleanedValue(outObj.startCashs.get(retCat)
                    + thisInvSnap.startValues.get(retCat) + 
                    thisCashSnap.startValues.get(retCat) + initBal));
            /* this handles case where fromDateInt < first transaction,
            AND initBal != 0 (i.e. startValue = initBal. In that case,
            start date needs to be adjusted to day prior to  first transaction date */
            int minDateInt = outObj.transMap.get(retCat).isEmpty() ? 0 :
                        DateUtils.getPrevBusinessDay(
                        outObj.transMap.get(retCat).firstKey());
            if(outObj.startValues.get(retCat) == initBal &&
                    thisInvSnap.retDateMap.get(retCat) <= minDateInt)
                adjRetDateMap.put(retCat, Math.max(
                        thisInvSnap.retDateMap.get(retCat), minDateInt));
        }
        outObj.endValue = cleanedValue(thisInvSnap.endValue + thisCashSnap.endValue
                + thisInvSnap.endCash + thisCashSnap.endCash + initBal);
        outObj.endCash = thisInvSnap.endCash + thisCashSnap.endCash + initBal;
        //get returns
        for (Iterator it = thisInvSnap.retDateMap.keySet().iterator(); it.hasNext();) {
            String retCat = (String) it.next();
            Integer thisFromDateInt = adjRetDateMap.get(retCat);
            TreeMap<Integer, Double> mdMap = outObj.transMap.get(retCat);
            // add dummy values to Mod-Dietz date maps
            if(Math.abs(outObj.startValues.get(retCat)) > 0.0001)
                RepFromTo.addValueToDateMap(mdMap, thisFromDateInt, 0.0);
            if(Math.abs(outObj.endValue) > 0.0001)
                RepFromTo.addValueToDateMap(mdMap, outObj.snapDateInt, 0.0);
            /*calc returns (note--no income/expenses since only transfers are considered
            i.e. endValue includes income/expenses*/
            double thisMDReturn = RepFromTo.getMDCalc(
                    outObj.startValues.get(retCat), outObj.endValue, 0.0, 0.0, mdMap);
            outObj.mdReturns.put(retCat, thisMDReturn);
            // get annualized returns
            if ("All".equals(retCat)) {
                TreeMap<Integer, Double> retMap = combineDateMaps(null,
                        outObj.transMap.get(retCat), "subtract");
                //add start and end values
                if(Math.abs(outObj.startValues.get(retCat)) > 0.0001)
                    RepFromTo.addValueToDateMap(retMap, thisFromDateInt,
                            -outObj.startValues.get(retCat));
                if(Math.abs(outObj.endValue) > 0.0001)
                    RepFromTo.addValueToDateMap(retMap,
                            thisInvSnap.snapDateInt, outObj.endValue);
                //calculate returns
                outObj.annRetAll = RepFromTo.getAnnualReturn(retMap,
                        outObj.mdReturns.get(retCat));
            }
        }
        outObj.totRet1Day = thisInvSnap.retDateMap.get("PREV") == null ? Double.NaN : outObj.mdReturns.get("PREV");
        outObj.totRetAll = thisInvSnap.retDateMap.get("All") == null ? Double.NaN : outObj.mdReturns.get("All");
        outObj.totRetWk = thisInvSnap.retDateMap.get("1Wk") == null ? Double.NaN : outObj.mdReturns.get("1Wk");
        outObj.totRet4Wk = thisInvSnap.retDateMap.get("4Wk") == null ? Double.NaN : outObj.mdReturns.get("4Wk");
        outObj.totRet3Mnth = thisInvSnap.retDateMap.get("3Mnth") == null ? Double.NaN : outObj.mdReturns.get("3Mnth");
        outObj.totRetYear = thisInvSnap.retDateMap.get("1Yr") == null ? Double.NaN : outObj.mdReturns.get("1Yr");
        outObj.totRet3year = thisInvSnap.retDateMap.get("3Yr") == null ? Double.NaN : outObj.mdReturns.get("3Yr");
        outObj.totRetYTD = thisInvSnap.retDateMap.get("YTD") == null ? Double.NaN : outObj.mdReturns.get("YTD");

        return outObj;
    }

    /*
     * generic method to add a one-dimensional object array to a
     * two-dimensional object array, so that line items in from/to
     * report and snap report can be aggregated for output
     */

    public static Object[][] addElement(Object[][] inArray, Object[] element) {

        if (inArray == null) {
            Object[][] outArray = new Object[1][element.length];
            outArray[0] = element;
            return outArray;
        } else {
            //Dimension new array (note initialized to 1st Row)
            Object[][] outArray = new Object[inArray.length + 1][inArray[0].length];
            for (int i = 0; i < inArray.length; i++) {
                System.arraycopy(inArray[i], 0, outArray[i], 0, inArray[i].length);
            }
            outArray[inArray.length] = element;
            return outArray;

        }
    }


    /**
     * Rounds values near zero to exactly zero
     * @param input input value
     * @return input or zero
     */
    public static double cleanedValue(double input){
        double thresh = 0.0001;
        if((input > 0 && input < thresh) || (input < 0 && input > -thresh) ){
            return 0;
        } else {
            return input;
        }
    }

    /* Class provides a generic TableModel which receives
     * data from the reporting methods above.
     */

    static class RptTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 5838000411345317854L;
        public String[] columnNames;
        public Object[][] data;

        public RptTableModel(Object[][] data, String[] columnNames) {
        super();
        this.data = data;
        this.columnNames = columnNames;
        }
        public int getColumnCount() {
            return columnNames.length;
        }
        public int getRowCount() {
            return data.length;
        }
        public String getColumnName(int col) {
            return columnNames[col];
        }
        public String[] getColumnNames(){
            return this.columnNames;
        }
        public Object getValueAt(int row, int col) {
            return data[row][col];
        }
        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }
        //allows table to be editable
        public boolean isCellEditable(int row, int col) {
            //Note that the data/cell address is constant,
            //no matter where the cell appears onscreen.
            if (col < 2) {
                return false;
            } else {
                return true;
            }
        }
        //allows table to be editable
        public void setValueAt(Object value, int row, int col) {
            data[row][col] = value;
            fireTableCellUpdated(row, col);
        }

    }
    

} //end ReportProd Class
    //unused methods follow
   // <editor-fold defaultstate="collapsed" desc="comment">
/*public static Double testSum(TreeMap<Integer, Double> inMap) {
    Double sum = 0.0;

    for (Iterator it = inMap.values().iterator(); it.hasNext();) {
    Double thisVal = (Double) it.next();
    sum = sum + thisVal;
    }
    return sum;
    }

   

    private static void testWriteCash(String accountName, double startBal,
    Double invStartCash, Double cashStartCash, Double invStartValue,
    Double cashStartValue) {
    File writeFile = new File("E:\\Temp\\" + accountName + ".csv");
    PrintWriter outputStream = null;
    try {
    outputStream = new PrintWriter(new FileWriter(writeFile));
    outputStream.println("End File,");
    outputStream.println("startBal," + startBal);
    outputStream.println("invEndCash," + invStartCash);
    outputStream.println("cashEndCash," + cashStartCash);
    outputStream.println("invEndValue," + invStartValue);
    outputStream.println("cashEndValue," + cashStartValue);


    } catch (Exception e) {
    Logger.getLogger(IOMethods.class.getName()).log(Level.SEVERE, null, e);
    } finally {
    outputStream.close();
    }
    }*/// </editor-fold>


