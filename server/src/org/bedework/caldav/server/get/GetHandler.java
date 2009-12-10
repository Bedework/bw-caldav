/* **********************************************************************
    Copyright 2009 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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
package org.bedework.caldav.server.get;

import org.bedework.caldav.server.CaldavBWIntf;
import org.bedework.caldav.server.PostMethod.RequestPars;
import org.bedework.caldav.server.sysinterface.SysIntf;

import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;
import edu.rpi.sss.util.xml.XmlEmit;

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

/** Class extended by classes which handle special GET requests, e.g. the
 * freebusy service, web calendars, ischedule etc.
 *
 * @author Mike Douglass
 */
public abstract class GetHandler {
  protected CaldavBWIntf intf;

  protected XmlEmit xml;

  /**
   * @param intf
   */
  public GetHandler(final CaldavBWIntf intf) {
    this.intf = intf;

    xml = intf.getXmlEmit();
  }

  /**
   * @param req
   * @param resp
   * @param pars
   * @throws WebdavException
   */
  public abstract void process(final HttpServletRequest req,
                               final HttpServletResponse resp,
                               final RequestPars pars) throws WebdavException;
  /**
   * @return current account
   */
  public String getAccount() {
    return intf.getAccount();
  }

  /**
   * @return SysIntf
   */
  public SysIntf getSysi() {
    return intf.getSysi();
  }

  /* ====================================================================
   *                   XmlUtil wrappers
   * ==================================================================== */

  protected void startEmit(final HttpServletResponse resp) throws WebdavException {
    try {
      xml.startEmit(resp.getWriter());
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** Returns the immediate children of a node.
  *
  * @param node             node in question
  * @return Collection      of WebdavNsNode children
  * @throws WebdavException
  */
 public Collection<WebdavNsNode> getChildren(final WebdavNsNode node)
     throws WebdavException {
    return intf.getChildren(node);
  }

  /** Retrieves a node by uri, following any links.
  *
  * @param uri              String decoded uri of the node to retrieve
  * @param existance        Say's something about the state of existance
  * @param nodeType         Say's something about the type of node
  * @return WebdavNsNode    node specified by the URI or the node aliased by
  *                         the node at the URI.
  * @throws WebdavException
  */
  public WebdavNsNode getNode(final String uri,
                              final int existance,
                              final int nodeType) throws WebdavException {
    return intf.getNode(uri, existance, nodeType);
  }

  protected void openTag(final QName tag) throws WebdavException {
    try {
      xml.openTag(tag);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** open with attribute
   * @param tag
   * @param attrName
   * @param attrVal
   * @throws WebdavException
   */
  public void openTag(final QName tag,
                      final String attrName, final String attrVal) throws WebdavException {
    try {
      xml.openTag(tag, attrName, attrVal);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** Emit an empty tag
   *
   * @param tag
   * @throws WebdavException
   */
  public void emptyTag(final QName tag) throws WebdavException {
    try {
      xml.emptyTag(tag);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** Emit a property
   *
   * @param tag
   * @param val
   * @throws WebdavException
   */
  public void property(final QName tag, final String val) throws WebdavException {
    try {
      xml.property(tag, val);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  protected void closeTag(final QName tag) throws WebdavException {
    try {
      xml.closeTag(tag);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }
}
