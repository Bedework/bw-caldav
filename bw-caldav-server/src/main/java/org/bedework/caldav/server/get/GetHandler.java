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
package org.bedework.caldav.server.get;

import org.bedework.caldav.server.CaldavBWIntf;
import org.bedework.caldav.server.RequestPars;
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
