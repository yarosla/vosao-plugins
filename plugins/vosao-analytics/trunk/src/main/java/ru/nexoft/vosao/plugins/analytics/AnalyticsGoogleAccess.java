package ru.nexoft.vosao.plugins.analytics;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by ys on 18.02.11 22:43
 */
public class AnalyticsGoogleAccess {
  public static final String AUTH_URL="https://www.google.com/accounts/ClientLogin";
  public static final String ACCOUNT_URL="https://www.google.com/analytics/feeds/accounts/default";
  public static final String DATA_URL="https://www.google.com/analytics/feeds/data";
  public static final String UTF8_ENCODING="utf-8";
  public static final String UTF8_APPLICATION_FORM_URLENCODED="application/x-www-form-urlencoded; charset=utf-8";

  public static Map<String, Integer> retrievePageViews(String webPropertyId, String userEmail, String userPassword, String startDate, String endDate, int maxResults) {
    String authTokens=httpPost(AUTH_URL, new String[] {"accountType", "GOOGLE", "Email", userEmail,
        "Passwd", userPassword, "service", "analytics", "source", "vosao-analytics-plugin-1"}, null);
    if (isEmpty(authTokens)) {
      System.err.println("Google Analytics authentication failure");
      return null;
    }
    //System.out.println("authTokens = "+authTokens);
    int idx1=authTokens.indexOf("Auth=");
    String authToken=null;
    if (idx1>=0) {
      int idx2=authTokens.indexOf('\n', idx1+5);
      authToken=idx2>=0 ? authTokens.substring(idx1, idx2).trim() : authTokens.substring(idx1).trim();
    }
    if (isEmpty(authToken)) {
      System.err.println("Google Analytics authentication failure (no Auth token in response)");
      return null;
    }
    String accountFeed=httpGet(ACCOUNT_URL, null/*new String[] {"prettyprint", "true"}*/, authToken);
    //System.out.println("accountFeed = "+accountFeed);
    String tableId=parseAccountFeed(accountFeed, webPropertyId);
    if (isEmpty(tableId)) {
      System.err.println("Google Analytics ID "+webPropertyId+" not accessible by this user");
      return null;
    }
    //System.out.println("tableId = "+tableId);
    String dataFeed=httpGet(DATA_URL, new String[] {"ids", tableId, "start-date", startDate, "end-date", endDate,
        "dimensions", "ga:pagePath", "metrics", "ga:pageViews", "sort", "-ga:pageViews",
        "max-results", Integer.toString(maxResults)/*, "prettyprint", "true"*/}, authToken);
    //System.out.println("dataFeed = "+dataFeed);
    return parseDataFeed(dataFeed);
  }

  private static String parseAccountFeed(String accountFeed, String findWebPropertyId) {
    try {
      XMLStreamReader xmlr=createXMLStreamReader(accountFeed);
      String webPropertyId=null;
      String tableId=null;
      while (xmlr.hasNext()) {
        int eventType=xmlr.next();
        if (eventType==XMLStreamConstants.START_ELEMENT) {
          String tagName=xmlr.getLocalName();
          if ("entry".equals(tagName)) {
            // reset
            webPropertyId=null;
            tableId=null;
          }
          else if ("tableId".equals(tagName)) {
            tableId=xmlr.getElementText().trim();
          }
          else if ("property".equals(tagName) && "ga:webPropertyId".equals(xmlr.getAttributeValue(null, "name"))) {
            webPropertyId=xmlr.getAttributeValue(null, "value");
          }
        }
        else if (eventType==XMLStreamConstants.END_ELEMENT && "entry".equals(xmlr.getLocalName())) {
          if (findWebPropertyId.equals(webPropertyId) && tableId!=null) return tableId;
        }
      }
      return null; // not found
    }
    catch (XMLStreamException e) {
      e.printStackTrace();
      return null;
    }
  }

  private static Map<String, Integer> parseDataFeed(String dataFeed) {
    try {
      Map<String, Integer> pageViews=new LinkedHashMap<String, Integer>();
      XMLStreamReader xmlr=createXMLStreamReader(dataFeed);
      String dimension=null;
      Integer metric=null;
      while (xmlr.hasNext()) {
        int eventType=xmlr.next();
        if (eventType==XMLStreamConstants.START_ELEMENT) {
          String tagName=xmlr.getLocalName();
          if ("entry".equals(tagName)) {
            // reset
            dimension=null;
            metric=null;
          }
          else if ("dimension".equals(tagName)) {
            dimension=xmlr.getAttributeValue(null, "value").trim();
          }
          else if ("metric".equals(tagName)) {
            String value=xmlr.getAttributeValue(null, "value").trim();
            try {
              metric=Integer.parseInt(value);
            }
            catch (NumberFormatException e) {
              //e.printStackTrace();
              System.err.println("Google Analytics parse error: metric value must be integer ["+value+"]");
            }
          }
        }
        else if (eventType==XMLStreamConstants.END_ELEMENT && "entry".equals(xmlr.getLocalName())) {
          if (dimension!=null && metric!=null) {
            pageViews.put(dimension, metric);
            //System.out.println(dimension+"\t"+metric);
          }
        }
      }
      return pageViews;
    }
    catch (XMLStreamException e) {
      e.printStackTrace();
      return null;
    }
  }

  private static XMLStreamReader createXMLStreamReader(String xml) throws XMLStreamException {
    XMLInputFactory xmlif=XMLInputFactory.newInstance();
    xmlif.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
    xmlif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
    xmlif.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    Reader cs=new StringReader(xml);
    return xmlif.createXMLStreamReader(cs);
  }

  public static String httpGet(String url, String[] parms, String authToken) {
    return httpRequest(false, url, parms, authToken);
  }

  public static String httpPost(String url, String[] parms, String authToken) {
    return httpRequest(true, url, parms, authToken);
  }

  public static String httpRequest(boolean postMethod, String url, String[] parms, String authToken) {
    try {
      StringBuilder data=new StringBuilder();
      if (parms!=null) {
        for (int i=0; i+1<parms.length; i+=2) {
          if (i>0) data.append('&');
          data.append(escapeURL(parms[i]));
          data.append('=');
          data.append(escapeURL(parms[i+1]));
        }
      }
      if (!postMethod && data.length()>0) {
        url+=(url.indexOf('?')>=0 ? "&" : "?")+data;
      }
      HttpURLConnection con=(HttpURLConnection)new URL(url).openConnection();
      con.setConnectTimeout(10000);
      con.setDoInput(true);
      con.setDoOutput(postMethod);
      con.setReadTimeout(60000);
      con.setAllowUserInteraction(false);
      con.setInstanceFollowRedirects(true);
      if (postMethod) {
        con.setRequestMethod("POST");
        con.addRequestProperty("Content-Type", UTF8_APPLICATION_FORM_URLENCODED);
      }
      if (authToken!=null) {
        con.addRequestProperty("Authorization", "GoogleLogin "+authToken);
        con.addRequestProperty("GData-Version", "2");
      }
      con.connect();
      OutputStream os=null;
      if (postMethod) {
        os=con.getOutputStream();
        writeToStream(data.toString(), os, UTF8_ENCODING);
      }
      int status=con.getResponseCode();
      InputStream is=con.getInputStream();
      String result=readFromStream(is, UTF8_ENCODING);
      if (os!=null) os.close();
      is.close();
      return status==200 ? result : null;
    }
    catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static String escapeURL(String s) {
    if (isEmpty(s)) return "";
    try {
      return java.net.URLEncoder.encode(s, UTF8_ENCODING);
    }
    catch (UnsupportedEncodingException ex) {
      return "";
    }
  }

  public static String readFromStream(InputStream is, String encoding) {
    StringBuilder sb=new StringBuilder();
    try {
      InputStreamReader isr=new InputStreamReader(is, encoding);
      if ("utf-8".equalsIgnoreCase(encoding)) { // strip UTF-8 BOM if present
        int bom=isr.read();
        if (bom!=-1 && bom!=0xFEFF) sb.append((char)bom); // not EOF, not BOM - append as char
      }
      char[] cbuf=new char[4*1024];
      int len;
      while ((len=isr.read(cbuf))>0) sb.append(cbuf, 0, len);
      //isr.close();
      return sb.toString();
    }
    catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static boolean writeToStream(String content, OutputStream os, String encoding) {
    try {
      OutputStreamWriter osw=new OutputStreamWriter(os, encoding);
      osw.write(noNull(content));
      osw.flush();
      return true;
    }
    catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public static String trimToNull(String s) { String s1; return s==null || ((s1=s.trim()).length()==0)?null:s1; }
  public static String blankToNull(String s) { return s==null || (s.trim().length()==0)?null:s; }
  public static String emptyToNull(String s) { return s.length()==0?null:s; }
  public static String noNull(String s) { return s==null?"":s; }
  public static String noNull(String s, String val) { return s==null?val:s; }
  public static boolean isBlank(String s) { return (s==null||s.trim().length()==0); }
  public static boolean isEmpty(String s) { return (s==null||s.length()==0); }
}
