Google Analytics plugin for Vosao CMS
(c) 2011, Yaroslav Stavnichiy, http://www.nexoft.ru

1. Setup

* Upload and install plugin's WAR-file

* Go to site configuration and setup Google Analytics ID to enable page
  tracking

* Go to plugins configuration, select Google Analytics plugin and fill in
  plugin's settings: Google Analytics user email & password. The password
  is stored as plain text, therefore we recommend registering special Google
  account with read-only access to your analytics just for this plugin.

2. How it works

Every 3 hours the plugin will retrieve Google Analytics data for your site
and store it in database.

In page templates you can access page views data as follows:

  $plugin.analytics.getPageViews($page.friendlyUrl)
    - returns the number of page views for last week

  $plugin.analytics.topPages(10)
    - returns top 10 pages

  $plugin.analytics.topPages("/blog/", 10)
    - returns top 10 pages having url that starts with /blog/ prefix;
      multiple prefixes could be listed separated by comma ','

3. Sample template script

  <h3>Popular blog posts</h3>
  <ul>
  #foreach ($page in $plugin.analytics.topPages("/blog/", 10))
     <li><a href="$page.friendlyURL">$page.title</a></li>
  #end
  </ul>
