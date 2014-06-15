/* ********************************************************************
    Licensed to Jasig under one or more contributor license
    agreements. See the NOTICE file distributed with this work
    for additional information regarding copyright ownership.
    Jasig licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file
    except in compliance with the License. You may obtain a
    copy of the License at:

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on
    an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied. See the License for the
    specific language governing permissions and limitations
    under the License.
*/
package org.bedework.caldav.server;

import org.bedework.webdav.servlet.common.DeleteMethod;
import org.bedework.webdav.servlet.common.GetMethod;
import org.bedework.webdav.servlet.common.HeadMethod;
import org.bedework.webdav.servlet.common.MethodBase.MethodInfo;
import org.bedework.webdav.servlet.common.OptionsMethod;
import org.bedework.webdav.servlet.common.PutMethod;
import org.bedework.webdav.servlet.common.WebdavServlet;
import org.bedework.webdav.servlet.shared.WebdavException;
import org.bedework.webdav.servlet.shared.WebdavNsIntf;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/** This class extends the webdav servlet class, implementing the abstract
 * methods and overriding others to extend/modify the behaviour.
 *
 * <p>We implement ServletContextListener methods here to load and
 * unload the configurations.</p>
 *
 * @author Mike Douglass   douglm  rpi.edu
 */
public class CaldavBWServlet extends WebdavServlet implements
        ServletContextListener {
  /* Is this a CalWS servlet? */
  private boolean calWs;

  /* ====================================================================
   *                     Abstract servlet methods
   * ==================================================================== */

  @Override
  public void init(final ServletConfig config) throws ServletException {
    calWs = Boolean.parseBoolean(config.getInitParameter("calws"));

    super.init(config);
  }

  @Override
  protected void addMethods() {
    if (calWs) {
      // Much reduced method set
      methods.clear();

      methods.put("DELETE", new MethodInfo(DeleteMethod.class, true));
      methods.put("GET", new MethodInfo(GetMethod.class, false));
      methods.put("HEAD", new MethodInfo(HeadMethod.class, false));
      methods.put("OPTIONS", new MethodInfo(OptionsMethod.class, false));
      methods.put("POST", new MethodInfo(CaldavPostMethod.class, false));  // Allow unauth POST for freebusy etc. true));
      methods.put("PUT", new MethodInfo(PutMethod.class, true));

      return;
    }

    super.addMethods();

    // Replace methods
    methods.put("MKCALENDAR", new MethodInfo(MkcalendarMethod.class, true));
    //methods.put("OPTIONS", new MethodInfo(CalDavOptionsMethod.class, false));
    methods.put("POST", new MethodInfo(CaldavPostMethod.class, false));  // Allow unauth POST for freebusy etc. true));
    methods.put("REPORT", new MethodInfo(CaldavReportMethod.class, false));
  }

  @Override
  public WebdavNsIntf getNsIntf(final HttpServletRequest req)
      throws WebdavException {
    final CaldavBWIntf wi = new CaldavBWIntf();

    wi.init(this, req, methods, dumpContent);
    return wi;
  }
  @Override
  public void contextInitialized(final ServletContextEvent sce) {
    try {
      CaldavBWIntf.contextInitialized(sce);
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  @Override
  public void contextDestroyed(final ServletContextEvent sce) {
    try {
      CaldavBWIntf.contextDestroyed(sce);
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }
}
