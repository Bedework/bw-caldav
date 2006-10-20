/*
 Copyright (c) 2000-2005 University of Washington.  All rights reserved.

 Redistribution and use of this distribution in source and binary forms,
 with or without modification, are permitted provided that:

   The above copyright notice and this permission notice appear in
   all copies and supporting documentation;

   The name, identifiers, and trademarks of the University of Washington
   are not used in advertising or publicity without the express prior
   written permission of the University of Washington;

   Recipients acknowledge that this distribution is made available as a
   research courtesy, "as is", potentially with defects, without
   any obligation on the part of the University of Washington to
   provide support, services, or repair;

   THE UNIVERSITY OF WASHINGTON DISCLAIMS ALL WARRANTIES, EXPRESS OR
   IMPLIED, WITH REGARD TO THIS SOFTWARE, INCLUDING WITHOUT LIMITATION
   ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
   PARTICULAR PURPOSE, AND IN NO EVENT SHALL THE UNIVERSITY OF
   WASHINGTON BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
   DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
   PROFITS, WHETHER IN AN ACTION OF CONTRACT, TORT (INCLUDING
   NEGLIGENCE) OR STRICT LIABILITY, ARISING OUT OF OR IN CONNECTION WITH
   THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
/* **********************************************************************
    Copyright 2005 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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

import edu.rpi.cct.webdav.servlet.common.MethodBase;
import edu.rpi.cct.webdav.servlet.common.WebdavServlet;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;

import java.util.HashMap;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletConfig;

/** This class extends the webdav servlet class, implementing the abstract
 * methods and overriding others to extend/modify the behaviour.
 *
 * @author Mike Douglass   douglm@rpi.edu
 */
public class CaldavBWServlet extends WebdavServlet {
//  private ServletConfig config;

  /** Global resources for the servlet - not to be modified.
   */
  protected Properties props;

  //  private static final String intfName = "edu.rpi.cct.uwcal.webdav.intfname";

  private String id = null;
  /* ====================================================================
   *                     Abstract servlet methods
   * ==================================================================== */

  public String getId() {
    if (id != null) {
      return id;
    }

    if (props == null) {
      return getClass().getName();
    }

    id = props.getProperty("edu.rpi.cct.uwcal.appname");
    if (id == null) {
      id = getClass().getName();
    }

    return id;
  }

  public void addMethods(WebdavNsIntf nsIntf) throws WebdavException{
    HashMap<String, MethodBase> methods = nsIntf.getMethods();

    super.addMethods(nsIntf);

    // Replace methods
    methods.put("MKCALENDAR", new MkcalendarMethod());
    //methods.put("OPTIONS", new CalDavOptionsMethod());
    methods.put("POST", new PostMethod());
    methods.put("REPORT", new CaldavReportMethod());
  }

  public WebdavNsIntf getNsIntf(HttpServletRequest req,
                                ServletConfig config,
                                Properties props)
      throws WebdavException {
    //    this.config = config;
    this.props = props;

    CaldavBWIntf wi = new CaldavBWIntf();

    return wi;
  }
}
