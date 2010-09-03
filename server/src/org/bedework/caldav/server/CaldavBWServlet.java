/* **********************************************************************
    Copyright 2008 Rensselaer Polytechnic Institute. All worldwide rights reserved.

    Redistribution and use of this distribution in source and binary forms,
    with or without modification, are permitted provided that:
       The above copyright notice and this permission notice appear in all
        copies and supporting documentation;

        The name, identifiers, and trademarks of Rensselaer Polytechnic
        Institute are not used in advertising or publicity without the
        express prior written permission of Rensselaer Polytechnic Institute;

    DISCLAIMER: The software is distributed" AS IS" without any express or
    implied warranty, including but not limited to, any implied warranties
    of merchantability or fitness for a particular purpose or any warrant)'
    of non-infringement of any current or pending patent rights. The authors
    of the software make no representations about the suitability of this
    software for any particular purpose. The entire risk as to the quality
    and performance of the software is with the user. Should the software
    prove defective, the user assumes the cost of all necessary servicing,
    repair or correction. In particular, neither Rensselaer Polytechnic
    Institute, nor the authors of the software are liable for any indirect,
    special, consequential, or incidental damages related to the software,
    to the maximum extent the law permits.
*/

package org.bedework.caldav.server;

import edu.rpi.cct.webdav.servlet.common.DeleteMethod;
import edu.rpi.cct.webdav.servlet.common.GetMethod;
import edu.rpi.cct.webdav.servlet.common.HeadMethod;
import edu.rpi.cct.webdav.servlet.common.OptionsMethod;
import edu.rpi.cct.webdav.servlet.common.PutMethod;
import edu.rpi.cct.webdav.servlet.common.WebdavServlet;
import edu.rpi.cct.webdav.servlet.common.MethodBase.MethodInfo;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/** This class extends the webdav servlet class, implementing the abstract
 * methods and overriding others to extend/modify the behaviour.
 *
 * @author Mike Douglass   douglm  rpi.edu
 */
public class CaldavBWServlet extends WebdavServlet {
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

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.common.WebdavServlet#addMethods()
   */
  @Override
  protected void addMethods() {
    if (calWs) {
      // Much reduced method set
      methods.clear();

      methods.put("DELETE", new MethodInfo(DeleteMethod.class, true));
      methods.put("GET", new MethodInfo(GetMethod.class, false));
      methods.put("HEAD", new MethodInfo(HeadMethod.class, false));
      methods.put("OPTIONS", new MethodInfo(OptionsMethod.class, false));
      methods.put("POST", new MethodInfo(PostMethod.class, false));  // Allow unauth POST for freebusy etc. true));
      methods.put("PUT", new MethodInfo(PutMethod.class, true));

      return;
    }

    super.addMethods();

    // Replace methods
    methods.put("MKCALENDAR", new MethodInfo(MkcalendarMethod.class, true));
    //methods.put("OPTIONS", new MethodInfo(CalDavOptionsMethod.class, false));
    methods.put("POST", new MethodInfo(PostMethod.class, false));  // Allow unauth POST for freebusy etc. true));
    methods.put("REPORT", new MethodInfo(CaldavReportMethod.class, false));
  }

  @Override
  public WebdavNsIntf getNsIntf(final HttpServletRequest req)
      throws WebdavException {
    CaldavBWIntf wi = new CaldavBWIntf();

    wi.init(this, req, debug, methods, dumpContent);
    return wi;
  }
}
