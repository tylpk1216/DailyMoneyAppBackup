package com.bottleworks.dailymoney.ui.report;

import java.text.DecimalFormat;
import java.util.List;

import org.achartengine.ChartFactory;
import org.achartengine.model.CategorySeries;
import org.achartengine.renderer.DefaultRenderer;

import android.content.Context;
import android.content.Intent;

import com.bottleworks.dailymoney.data.AccountType;
import com.bottleworks.dailymoney.data.Balance;

//tylpk 20120408
import com.bottleworks.commons.util.Logger;

public class BalancePieChart extends AbstractChart {

    DecimalFormat percentageFormat = new DecimalFormat("##0");
    
    //tylpk 20120407
    int ratio = 5;
    int fontSize = 12;
    
    public BalancePieChart(Context context, int orientation, float dpRatio, int pieRatio, int pieFontsize) {
        super(context, orientation, dpRatio);
        
        //tylpk 20120407
        ratio = pieRatio;
        fontSize = pieFontsize;
    }

    public Intent createIntent(AccountType at, List<Balance> balances) {
        double total = 0;
        for (Balance b : balances) {
            total += b.getMoney() <= 0 ? 0 : b.getMoney();
        }
        CategorySeries series = new CategorySeries(at.getDisplay(i18n));
        
        //tylpk 20120407
        double others = 0;
        
        for (Balance b : balances) {
            if (b.getMoney() > 0) {
                
            	StringBuilder d = new StringBuilder();
            	d.append(b.getName()).append("(").append(b.getMoney()).append("-").append(total).append(")");
                Logger.d(d.toString());
                
                StringBuilder sb = new StringBuilder();
                
                String shortName;
                String[] splitName = b.getName().split("\\.");
                
                //only one level
                if(splitName.length > 0)
                    shortName = splitName[splitName.length-1];
                else
                    shortName = b.getName();
                	
                //remove the prefix number
                int i;
                for(i = 0; i < shortName.length();i++)
                    if(!Character.isDigit(shortName.charAt(i)))
                        break;
                shortName = shortName.substring(i);
                
                sb.append(shortName);
                double p = (b.getMoney() * 100) / total;
                if (p >= ratio) {
                    sb.append("(").append(percentageFormat.format(p)).append("%)");
                    series.add(sb.toString(), b.getMoney());
                }
                else
                {
                    others += p;
                }
            }
        }
        
        //tylpk 20120407
        if(others > 0)
        {
            StringBuilder sbOthers = new StringBuilder();
            sbOthers.append("Others(").append(percentageFormat.format(others)).append("%)");;
            series.add(sbOthers.toString(), others*total/100);
        }
        
        int[] color = createColor(series.getItemCount());
        DefaultRenderer renderer = buildCategoryRenderer(color);
        
        //tylpk 20120407
        renderer.setLabelsTextSize(fontSize * dpRatio);
        renderer.setLegendTextSize(fontSize * dpRatio);
        return ChartFactory.getPieChartIntent(context, series, renderer, series.getTitle());
    }
}
