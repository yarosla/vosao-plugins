package ru.nexoft.vosao.plugins.analytics;

import com.google.appengine.api.datastore.*;
import org.vosao.business.Business;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ys on 19.02.11 10:26
 */
public class AnalyticsBusiness {
  private static final Key KEY_PAGE_VIEWS_ENTITY=KeyFactory.createKey("AnalyticsPageViews", "table");

  private static Pattern USER_EMAIL=Pattern.compile("<ga-user>(.*)</ga-user>");
  private static Pattern USER_PASSWORD=Pattern.compile("<ga-password>(.*)</ga-password>");

  public static Map<String,Integer> retrieveAndStorePageViews(Business business) {
    Calendar cal=Calendar.getInstance();
    String endDate=formatSqlDate(cal.getTime());
    cal.add(Calendar.DATE, -7);
    String startDate=formatSqlDate(cal.getTime());
    String webPropertyId=business.getConfigBusiness().getConfig().getGoogleAnalyticsId();
    if (isBlank(webPropertyId)) {
      System.err.println("Google Analytics ID not configured");
      return null;
    }
    webPropertyId=webPropertyId.trim();
    String configXML=business.getDao().getPluginDao().getByName("analytics").getConfigData();
    if (isBlank(configXML)) {
      System.err.println("Google Analytics ID not configured");
      return null;
    }
    String userEmail=null;
    String userPassword=null;
    Matcher matcher=USER_EMAIL.matcher(configXML);
    if (matcher.find()) {
      userEmail=matcher.group(1).trim();
    }
    matcher=USER_PASSWORD.matcher(configXML);
    if (matcher.find()) {
      userPassword=matcher.group(1).trim();
    }
    if (isEmpty(userEmail) || isEmpty(userPassword)) {
      System.err.println("Google Analytics user email and password not configured");
      return null;
    }

    Map<String,Integer> pageViews=AnalyticsGoogleAccess.retrievePageViews(webPropertyId, userEmail, userPassword, startDate, endDate, 100);
    if (pageViews.isEmpty()) {
      System.err.println("Google Analytics returned empty data set");
      return null;
    }
    StringBuilder sb=new StringBuilder();
    for (Map.Entry<String, Integer> entry : pageViews.entrySet()) {
      sb.append(entry.getKey());
      sb.append('\t');
      sb.append(entry.getValue());
      sb.append('\n');
    }
    Text pageViewData=new Text(sb.toString());
    DatastoreService datastore=business.getDao().getSystemService().getDatastore();
    Entity pageViewsEntity=new Entity(KEY_PAGE_VIEWS_ENTITY);
    pageViewsEntity.setUnindexedProperty("pageViews", pageViewData);
    pageViewsEntity.setUnindexedProperty("pageViewsRetrievedOn", new Date());
    datastore.put(pageViewsEntity);
    return pageViews;
  }

  public static Map<String,Integer> getPageViews(Business business) {
    DatastoreService datastore=business.getDao().getSystemService().getDatastore();
    try {
      Entity pageViewsEntity=datastore.get(KEY_PAGE_VIEWS_ENTITY);
      Text pageViewsData=(Text)pageViewsEntity.getProperty("pageViews");
      Map<String,Integer> pageViews=new LinkedHashMap<String, Integer>();
      for (String page : split(pageViewsData.getValue(), '\n')) {
        String[] keyValue=split(page, '\t');
        if (keyValue.length==2 && !isEmpty(keyValue[0])) {
          try {
            int value=Integer.parseInt(keyValue[1]);
            pageViews.put(keyValue[0], value);
          }
          catch (NumberFormatException e) {
            e.printStackTrace();
          }
        }
      }
      return pageViews;
    }
    catch (Exception e) {
      // entity not found, etc.
      return retrieveAndStorePageViews(business);
    }
  }

  public static String[] split(String s, char separator) {
    // this is meant to be faster than String.split() when separator is not regexp
    if (s==null) return null;
    ArrayList<String> parts=new ArrayList<String>();
    int beginIndex=0, endIndex;
    while ((endIndex=s.indexOf(separator, beginIndex))>=0) {
      parts.add(s.substring(beginIndex, endIndex));
      beginIndex=endIndex+1;
    }
    parts.add(s.substring(beginIndex));
    String[] a=new String[parts.size()];
    return parts.toArray(a);
  }

  private static final SimpleDateFormat sqlDate=new SimpleDateFormat("yyyy-MM-dd");

  public static String formatSqlDate(java.util.Date d) {
    if (d==null) return "";
    synchronized (sqlDate) {
      return sqlDate.format(d);
    }
  }

  public static boolean isBlank(String s) { return (s==null||s.trim().length()==0); }
  public static boolean isEmpty(String s) { return (s==null||s.length()==0); }
}
