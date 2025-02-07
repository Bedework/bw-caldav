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
import org.bedework.util.xml.XmlEmit;
import org.bedework.webdav.servlet.shared.WebdavException;
import org.bedework.webdav.servlet.shared.WebdavNsNode;

import java.util.Collection;
import java.util.function.Supplier;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
   * @param intf server interface
   */
  public GetHandler(final CaldavBWIntf intf) {
    this.intf = intf;

    xml = intf.getXmlEmit();
  }

  /**
   * @param req http request
   * @param resp http response
   * @param pars
   */
  public abstract void process(HttpServletRequest req,
                               HttpServletResponse resp,
                               RequestPars pars);
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

  /* ==============================================================
   *                   XmlUtil wrappers
   * ============================================================== */

  protected void startEmit(final HttpServletResponse resp) {
    try {
      xml.startEmit(resp.getWriter());
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** Returns the immediate children of a node.
  *
  * @param node             node in question
  * @return Collection      of WebdavNsNode children
  */
 public Collection<WebdavNsNode> getChildren(
         final WebdavNsNode node,
         final Supplier<Object> filterGetter) {
    return intf.getChildren(node, filterGetter);
  }

  /** Retrieves a node by uri, following any links.
  *
  * @param uri              String decoded uri of the node to retrieve
  * @param existance        Say's something about the state of existance
  * @param nodeType         Say's something about the type of node
  * @return WebdavNsNode    node specified by the URI or the node aliased by
  *                         the node at the URI.
  */
  public WebdavNsNode getNode(final String uri,
                              final int existance,
                              final int nodeType) {
    return intf.getNode(uri, existance, nodeType,
                        false);
  }

  protected void openTag(final QName tag) {
    try {
      xml.openTag(tag);
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** open with attribute
   * @param tag QName
   * @param attrName attrbute name
   * @param attrVal and value
   */
  public void openTag(final QName tag,
                      final String attrName,
                      final String attrVal) {
    try {
      xml.openTag(tag, attrName, attrVal);
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** Emit an empty tag
   *
   * @param tag QName
   */
  public void emptyTag(final QName tag) {
    try {
      xml.emptyTag(tag);
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** Emit a property
   *
   * @param tag QName
   * @param val element value
   */
  public void property(final QName tag, final String val) {
    try {
      xml.property(tag, val);
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  protected void closeTag(final QName tag) {
    try {
      xml.closeTag(tag);
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }
}
