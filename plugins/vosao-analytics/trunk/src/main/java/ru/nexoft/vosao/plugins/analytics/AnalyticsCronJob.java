package ru.nexoft.vosao.plugins.analytics;

import org.vosao.business.Business;
import org.vosao.business.plugin.PluginCronJob;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by ys on 19.02.11 1:03
 */
public class AnalyticsCronJob implements PluginCronJob {

  private Business business;

  public AnalyticsCronJob(Business business) {
    this.business=business;
  }

  @Override
  public boolean isShowTime(Date date) {
    Calendar cal=Calendar.getInstance();
    int hour=cal.get(Calendar.HOUR_OF_DAY);
    int minute=cal.get(Calendar.MINUTE);
    // start every 3 hours: 1, 4, 7, ..., 22
    return hour%3==1 && minute==0;
  }

  @Override
  public void run() {
    AnalyticsBusiness.retrieveAndStorePageViews(business);
  }
}
