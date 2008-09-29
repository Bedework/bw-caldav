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

import edu.rpi.cct.webdav.servlet.common.PropPatchMethod;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;
import edu.rpi.sss.util.xml.tagdefs.CaldavTags;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.w3c.dom.Document;

/** Class called to handle MKCOL
 *
 *   @author Mike Douglass
 */
public class MkcalendarMethod extends PropPatchMethod {
  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.common.MethodBase#init()
   */
  public void init() {
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.common.PropPatchMethod#doMethod(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  public void doMethod(HttpServletRequest req,
                        HttpServletResponse resp) throws WebdavException {
    if (debug) {
      trace("MkcalendarMethod: doMethod");
    }

    /* Parse any content */
    Document doc = parseContent(req, resp);

    /* Create the node */
    String resourceUri = getResourceUri(req);

    WebdavNsNode node = getNsIntf().getNode(resourceUri,
                                            WebdavNsIntf.existanceNot,
                                            WebdavNsIntf.nodeTypeCollection);

    boolean success = true;

    if (doc != null) {
      success = processDoc(req, resp, doc, node, CaldavTags.mkcalendar, true);
    }

    // XXX Make calendar using properties sent in request
    if (success) {
      getNsIntf().makeCollection(req, resp, node);
    }
  }
}

