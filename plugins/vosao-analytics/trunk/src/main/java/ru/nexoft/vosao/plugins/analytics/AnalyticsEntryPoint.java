package ru.nexoft.vosao.plugins.analytics;

import org.vosao.business.plugin.AbstractPluginEntryPoint;

/**
 * Created by ys on 18.02.11 16:15
 */
public class AnalyticsEntryPoint extends AbstractPluginEntryPoint {
  private AnalyticsVelocityPlugin velocityPlugin;

  @Override
  public void init() {
    getJobs().add(new AnalyticsCronJob(getBusiness()));
  }

  @Override
  public Object getPluginVelocityService() {
    if (velocityPlugin==null) {
      velocityPlugin=new AnalyticsVelocityPlugin(getBusiness());
    }
    return velocityPlugin;
  }

  @Override
  public String getBundleName() {
    return "ru.nexoft.vosao.plugins.analytics.messages";
  }
}
