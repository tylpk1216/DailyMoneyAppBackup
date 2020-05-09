package com.bottleworks.dailymoney.data;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.bottleworks.dailymoney.context.Contexts;
import com.bottleworks.dailymoney.ui.AccountUtil;
import com.bottleworks.dailymoney.ui.AccountUtil.IndentNode;



public class BalanceHelper {
    
    static String netAsset;
    static String netIncome;
    static String specialExpense;
   
	//tylpk 20120408
	static double totalAsset;
	static double totalLib;
	static double totalIncome;
	static double totalExpense;
	static double totalSpecialExpense;
	
	
    static Contexts contexts(){
        return Contexts.instance();
    }

    
    public static List<Balance> adjustTotalBalance(AccountType type, String totalName, List<Balance> items) {
        if(items.size()==0){
            return items;
        }
        List<Balance> group = new ArrayList<Balance>(items);
        double total = 0;
        for (Balance b : items) {
            b.setIndent(1);
            b.setGroup(group);
            total += b.getMoney();
        }
        Balance bt = new Balance(totalName,type.getType(), total,null);
        bt.setIndent(0);
        bt.setGroup(group);
        bt.setDate(items.get(0).getDate());
        items.add(0, bt);
        return items;
    }
    
    public static List<Balance> adjustNestedTotalBalance(AccountType type, String totalName, List<Balance> items) {
        if(items.size()==0){
            return items;
        }
        
        List<Balance> group = new ArrayList<Balance>(items);
        
        IDataProvider idp = contexts().getDataProvider();
        List<Account> accs = idp.listAccount(type);
        List<IndentNode> inodes = AccountUtil.toIndentNode(accs);
        
        List<Balance> nested = new ArrayList<Balance>();
        
        double total = 0;
        for (Balance ib : items) {
            total += ib.getMoney();
        }
        Date date = items.get(0).getDate();
        
        //the nested nodes
        for(IndentNode node:inodes){
            String fullpath = node.getFullPath();
            Balance b = new Balance(node.getName(),type.getType(),0,null);
            nested.add(b);
            b.setGroup(group);
            b.setIndent(node.getIndent()+1);
            double sum =0;
            for (Balance ib : items) {
                String in = ib.getName();
                if(in.equals(fullpath)){
                    sum += ib.getMoney();
                    b.setTarget(ib.getTarget());
                }else if(in.startsWith(fullpath+".")){
                    sum += ib.getMoney();
                    //for search detail
                    b.setTarget(idp.toAccountId(new Account(type.getType(),fullpath,0D)));
                }
                
            }
            b.setDate(date);
            b.setMoney(sum);
        }
        
        Balance top = new Balance(totalName,type.getType(), total,type);
        top.setIndent(0);
        top.setGroup(group);
        top.setDate(date);
        
        nested.add(0, top);
        
        //tylpk 20120408
        if(type.equals(AccountType.ASSET))
        {
            totalAsset = total;
        
            double netasset = totalAsset-totalLib;
        	
            Balance tmp = new Balance(netAsset, type.getType(), netasset, null);
            tmp.setIndent(0);
            tmp.setGroup(null);
            tmp.setDate(date);
        	
            nested.add(0, tmp);
        }
        else if(type.equals(AccountType.LIABILITY))
        { 
            totalLib = total;
        }
        else if(type.equals(AccountType.INCOME))
        {
            totalIncome = total;
        
            double netincome = totalIncome-totalExpense;
        	
            Balance tmp = new Balance(netIncome, type.getType(), netincome, null);
            tmp.setIndent(0);
            tmp.setGroup(null);
            tmp.setDate(date);
        	
            nested.add(0, tmp);
        }
        else if(type.equals(AccountType.EXPENSE))
        {
            totalExpense = total;
        	
            if(totalSpecialExpense != 0)
            {
                Balance tmp = new Balance(specialExpense, type.getType(), totalSpecialExpense, type);
                tmp.setIndent(0);
                tmp.setGroup(null);
                tmp.setDate(date);
        	
                nested.add(0, tmp);
            }
        }
        
        return nested;
    }    

    public static List<Balance> calculateBalanceList(AccountType type,Date start,Date end) {
        boolean nat = type==AccountType.INCOME||type==AccountType.LIABILITY;
        IDataProvider idp = contexts().getDataProvider();
        boolean calInit = true;
        if(start!=null){
            calInit = false;
        }else{
            Detail first = idp.getFirstDetail();
            //don't calculate init val if the first record date in after end data
            if(first!=null && first.getDate().after(end)){
                calInit = false;
            }
        }
        List<Account> accs = idp.listAccount(type);
        List<Balance> blist = new ArrayList<Balance>();
        for (Account acc : accs) {
            double from = idp.sumFrom(acc, start, end);
            double to = idp.sumTo(acc, start, end);
            double init = calInit?acc.getInitialValue():0;
            double b = init + (nat?(from - to):(to - from));
            Balance balance = new Balance(acc.getName(),type.getType(), b,acc);
            balance.setDate(end);
            blist.add(balance);
        }
        return blist;
    }
    
    public static Balance calculateBalance(AccountType type,Date start,Date end) {
        boolean nat = type==AccountType.INCOME||type==AccountType.LIABILITY;
        IDataProvider idp = contexts().getDataProvider();
        boolean calInit = true;
        if(start!=null){
            calInit = false;
        }else{
            Detail first = idp.getFirstDetail();
            //don't calculate init val if the first record date in after end data
            if(first!=null && first.getDate().after(end)){
                calInit = false;
            }
        }
        
        double from = idp.sumFrom(type, start, end);
        double to = idp.sumTo(type, start, end);

        double init = calInit ? idp.sumInitialValue(type) : 0;

        double b = init + (nat ? (from - to) : (to - from));
        Balance balance = new Balance(type.getDisplay(contexts().getI18n()), type.getType(), b, type);
        balance.setDate(end);
            
        return balance;
    }
    
    //tylpk 20120408
    public static void setAdditonalLabel(String netAssetString, String netIncomeString, String specialExpenseString) {
        netAsset = netAssetString;
        netIncome = netIncomeString;
        specialExpense = specialExpenseString;   
    }
    
    public static void calculateSpecialExpenseBalance(String specialExpense, Date start,Date end) {
        IDataProvider idp = contexts().getDataProvider();
        
        totalSpecialExpense = idp.sumSpecialExpenseValue(specialExpense, start, end);
    }
    
    public static Balance calculateBalance(Account acc, Date start, Date end) {
        AccountType type = AccountType.find(acc.getType());
        boolean nat = type == AccountType.INCOME || type == AccountType.LIABILITY;
        IDataProvider idp = contexts().getDataProvider();
        boolean calInit = true;
        if(start!=null){
            calInit = false;
        }else{
            Detail first = idp.getFirstDetail();
            //don't calculate init val if the first record date in after end data
            if(first!=null && first.getDate().after(end)){
                calInit = false;
            }
        }
        double from = idp.sumFrom(acc, start, end);
        double to = idp.sumTo(acc, start, end);
        double init = calInit ? acc.getInitialValue() : 0;
        double b = init + (nat ? (from - to) : (to - from));
        Balance balance = new Balance(acc.getName(), type.getType(), b, acc);
        balance.setDate(end);

        return balance;
    }
}
