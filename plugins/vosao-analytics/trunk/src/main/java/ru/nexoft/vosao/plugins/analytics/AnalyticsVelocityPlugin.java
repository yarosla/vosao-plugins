package ru.nexoft.vosao.plugins.analytics;

import org.vosao.business.Business;
import org.vosao.entity.PageEntity;
import org.vosao.entity.PluginEntity;
import org.vosao.velocity.plugin.AbstractVelocityPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by ys on 18.02.11 16:20
 */
public class AnalyticsVelocityPlugin extends AbstractVelocityPlugin {

  private Map<String,Integer> pageViewsCached;
  private long pageViewsCachedTime;

  public AnalyticsVelocityPlugin(Business business) {
    setBusiness(business);
  }

  private Map<String,Integer> getPageViewsCached() {
    // cache for one hour
    if (pageViewsCached!=null && (System.currentTimeMillis()-pageViewsCachedTime)<3600*1000L) return pageViewsCached;
    pageViewsCachedTime=System.currentTimeMillis();
    return pageViewsCached=AnalyticsBusiness.getPageViews(getBusiness());
  }

  public void forseRetrieve() {
    pageViewsCached=AnalyticsBusiness.retrieveAndStorePageViews(getBusiness());
    pageViewsCachedTime=System.currentTimeMillis();
  }

  public int getPageViews(String pageUrl) {
    Map<String,Integer> pageViews=getPageViewsCached();
    return pageViews!=null && pageViews.containsKey(pageUrl)? pageViews.get(pageUrl) : 0;
  }

  public List<PageEntity> topPages(int limit) {
    List<PageEntity> topPages=new ArrayList<PageEntity>();
    Map<String,Integer> pageViews=getPageViewsCached();
    if (pageViews==null || pageViews.isEmpty()) return topPages;
    for (String pageUrl : pageViews.keySet()) {
      PageEntity pageEntity=getDao().getPageDao().getByUrl(pageUrl);
      if (pageEntity!=null) {
        topPages.add(pageEntity);
        if (--limit<=0) break;
      }
    }
    return topPages;
  }

  public List<PageEntity> topPages(String pathPrefixList, int limit) {
    if (pathPrefixList==null || pathPrefixList.length()==0) return topPages(limit);

    String[] pathPrefixes=pathPrefixList.split("\\s*,\\s*");
    List<PageEntity> topPages=new ArrayList<PageEntity>();
    Map<String,Integer> pageViews=getPageViewsCached();
    if (pageViews==null || pageViews.isEmpty()) return topPages;
    for (String pageUrl : pageViews.keySet()) {
      for (String prefix : pathPrefixes) {
        if (pageUrl.startsWith(prefix)) {
          PageEntity pageEntity=getDao().getPageDao().getByUrl(pageUrl);
          if (pageEntity!=null) {
            topPages.add(pageEntity);
            if (--limit<=0) break;
          }
          break;
        }
      }
    }
    return topPages;
  }

  private PluginEntity getPlugin() {
    return getBusiness().getDao().getPluginDao().getByName("analytics");
  }

  public String test() {
    String configXML=getPlugin().getConfigData();
    return "{analytics-plugin "+configXML+"}";
  }

  public String testCall() {
    Map<String,Integer> pageViews=getPageViewsCached();
    if (pageViews==null || pageViews.isEmpty()) return "no analytics found";

    StringBuilder sb=new StringBuilder();
    for (Map.Entry<String, Integer> entry : pageViews.entrySet()) {
      sb.append(entry.getKey());
      sb.append(" - ");
      sb.append(entry.getValue());
      sb.append("<br/>");
    }
    return sb.toString();
  }
}
