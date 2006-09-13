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

import org.bedework.caldav.server.calquery.CalendarData;
import org.bedework.caldav.server.calquery.FreeBusyQuery;
import org.bedework.caldav.server.filter.Filter;
import org.bedework.calfacade.CalFacadeDefs;
import org.bedework.davdefs.CaldavTags;
import org.bedework.davdefs.WebdavTags;

import edu.rpi.cct.webdav.servlet.common.PropFindMethod;
import edu.rpi.cct.webdav.servlet.common.ReportMethod;
import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;
import edu.rpi.cct.webdav.servlet.shared.WebdavStatusCode;
import edu.rpi.sss.util.xml.XmlUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Class called to handle OPTIONS. We should determine what the current
 * url refers to and send a response which shows the allowable methods on that
 * resource.
 *
 *   @author Mike Douglass   douglm@rpi.edu
 */
public class CaldavReportMethod extends ReportMethod {
  /* The parsed results go here. We see:
   *  1. Free-busy request
   *  2. Query - optional props + filter
   *  3. Multi-get - optional props + one or more hrefs
   */

  private FreeBusyQuery freeBusy;
  //private CalendarData caldata;
  private Filter filter;
  private ArrayList hrefs;

  CalendarData caldata;

  // ENUM
  private final static int reportTypeQuery = 0;
  private final static int reportTypeMultiGet = 1;
  private final static int reportTypeFreeBusy = 2;

  private int reportType;

  /** Called at each request
   */
  public void init() {
  }

  /* We process the parsed document and produce a response
   *
   * @param doc
   * @throws WebdavException
   */
  protected void process(Document doc,
                         int depth,
                         HttpServletRequest req,
                         HttpServletResponse resp) throws WebdavException {
    reportType = getCaldavReportType(doc);

    if (reportType < 0) {
      super.process(doc, depth, req, resp);
      return;
    }

    processDoc(doc);

    if (reportType == reportTypeFreeBusy) {
      processFbResp(req, resp);
    } else {
      processResp(req, resp, depth);
    }
  }

  /** See if we recognize this report type and return an index.
   *
   * @param doc
   * @return index or <0 for unknown.
   * @throws WebdavException
   */
  protected int getCaldavReportType(Document doc) throws WebdavException {
    try {
      Element root = doc.getDocumentElement();

      if (CaldavTags.calendarQuery.nodeMatches(root)) {
        return reportTypeQuery;
      }

      if (CaldavTags.calendarMultiget.nodeMatches(root)) {
        return reportTypeMultiGet;
      }

      if (CaldavTags.freeBusyQuery.nodeMatches(root)) {
        return reportTypeFreeBusy;
      }

      return -1;
    } catch (Throwable t) {
      System.err.println(t.getMessage());
      if (debug) {
        t.printStackTrace();
      }

      throw new WebdavException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  /* We process the parsed document and produce a Collection of request
   * objects to process.
   *
   * @param doc
   * @throws WebdavException
   */
  private void processDoc(Document doc) throws WebdavException {
    try {
      WebdavNsIntf intf = getNsIntf();

      Element root = doc.getDocumentElement();

      Element[] children = getChildren(root);

      for (int i = 0; i < children.length; i++) {
        Element curnode = children[i];

        /** The names come through looking something like A:allprop etc.
         */

        String nm = curnode.getLocalName();
        String ns = curnode.getNamespaceURI();

        if (debug) {
          trace("reqtype: " + nm + " ns: " + ns);
        }

        if (reportType == reportTypeFreeBusy) {
          freeBusy = new FreeBusyQuery(intf, debug);
          freeBusy.parse(curnode);
        } else {
          /* Two possibilities:
               <!ELEMENT calendar-multiget ((DAV:allprop |
                                      DAV:propname |
                                      DAV:prop)?, DAV:href+)>

               <!ELEMENT calendar-query ((DAV:allprop |
                                    DAV:propname |
                                    DAV:prop)?, filter, timezone?)>
           */

          /* First try for a property request */
          PropFindMethod.PropRequest pr = pm.tryPropRequest(curnode);

          if (pr != null) {
            if (preq != null) {
              if (debug) {
                trace("REPORT: preq not null");
              }
              throw new WebdavBadRequest();
            }
            preq = pr;
          } else if ((reportType == reportTypeQuery) &&
                     nodeMatches(curnode, CaldavTags.filter)) {
            if (filter != null) {
              if (debug) {
                trace("REPORT: filter not null");
              }
              throw new WebdavBadRequest();
            }

            filter = new Filter(intf, debug);
            int st = filter.parse(curnode);

            if (st != HttpServletResponse.SC_OK) {
              throw new WebdavException(st);
            }
          } else if ((reportType == reportTypeMultiGet) &&
              WebdavTags.href.nodeMatches(curnode)) {
            String href = XmlUtil.getElementContent(curnode);

            if ((href == null) || (href.length() == 0)) {
              throw new WebdavBadRequest();
            }

            if (hrefs == null) {
              hrefs = new ArrayList();
            }

            hrefs.add(href);
          } else {
            if (debug) {
              trace("REPORT: unexpected element");
            }
            throw new WebdavBadRequest();
          }
        }
      }

      // Final validation
      if (reportType == reportTypeMultiGet) {
        if (hrefs == null) {
          // need at least 1
          throw new WebdavBadRequest();
        }
      } else if (reportType == reportTypeQuery) {
        if (filter == null) {
          // filter required
          throw new WebdavBadRequest();
        }
      }

      if (preq != null) {
        // Look for a calendar-data property
        Iterator it = preq.iterateProperties();
        while (it.hasNext()) {
          Object o = it.next();
          if (o instanceof CalendarData) {
            caldata = (CalendarData)o;
          }
        }
      }

      if (debug) {
        trace("REPORT: ");
        if (reportType == reportTypeFreeBusy) {
          trace("free-busy");
          freeBusy.dump();
        } else if (reportType == reportTypeQuery) {
          // Query - optional props + filter
          filter.dump();
        } else if (reportType == reportTypeMultiGet) {
          // Multi-get - optional props + one or more hrefs

          Iterator it = hrefs.iterator();
          while (it.hasNext()) {
            String href = (String)it.next();
            trace("    <DAV:href>" + href + "</DAV:href>");
          }
        } else {
        }
      }
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      System.err.println(t.getMessage());
      if (debug) {
        t.printStackTrace();
      }

      throw new WebdavException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * @param req
   * @param resp
   * @param depth
   * @throws WebdavException
   */
  public void processResp(HttpServletRequest req,
                          HttpServletResponse resp,
                          int depth) throws WebdavException {
    startEmit(resp);

    resp.setStatus(WebdavStatusCode.SC_MULTI_STATUS);
    resp.setContentType("text/xml");

    String resourceUri = getResourceUri(req);

    CaldavBWIntf intf = (CaldavBWIntf)getNsIntf();
    WebdavNsNode node = intf.getNode(resourceUri);

    openTag(WebdavTags.multistatus);

    int status = HttpServletResponse.SC_OK;

    Collection nodes = null;

    if (reportType == reportTypeQuery) {
      nodes = doNodeAndChildren(node, 0, defaultDepth(depth, 0));
    } else if (reportType == reportTypeMultiGet) {
      nodes = new ArrayList();

      if (hrefs != null) {
        Iterator it = hrefs.iterator();
        while (it.hasNext()) {
          String href = intf.getUri((String)it.next());

          try {
            nodes.add(intf.getNode(href));
          } catch (WebdavException we) {
            nodes.add(new CaldavCalNode(we.getStatusCode(), debug));
          }
        }
      }
    }

    if (status != HttpServletResponse.SC_OK) {
      if (debug) {
        trace("REPORT status " + status);
      }
      // Entire request failed.
      node.setStatus(status);
      doNodeProperties(node);
    } else if (nodes != null) {
      Iterator it = nodes.iterator();
      while (it.hasNext()) {
        WebdavNsNode curnode = (WebdavNsNode)it.next();

        doNodeProperties(curnode);
      }
    }

    closeTag(WebdavTags.multistatus);

    flush();
  }

  private Collection getNodes(WebdavNsNode node) throws WebdavException {
    try {
      if (debug) {
        trace("getNodes: " + node.getUri());
      }

      CaldavBWIntf intf = (CaldavBWIntf)getNsIntf();

      int retrieveRecur;

      if (caldata == null) {
        retrieveRecur = CalFacadeDefs.retrieveRecurMaster;
      } else if (caldata.getErs() != null) {
        /* expand XXX use range */
        retrieveRecur = CalFacadeDefs.retrieveRecurExpanded;
      } else if (caldata.getLrs() != null) {
        /* expand XXX use range */
        retrieveRecur = CalFacadeDefs.retrieveRecurOverrides;
      } else {
        retrieveRecur = CalFacadeDefs.retrieveRecurMaster;
      }

      return intf.query(node, retrieveRecur, filter);
    } catch (WebdavException wde) {
      throw new WebdavException(wde.getStatusCode());
    }
  }

  private Collection doNodeAndChildren(WebdavNsNode node,
                                       int curDepth,
                                       int maxDepth) throws WebdavException {
    if (!(node instanceof CaldavCalNode)) {
      throw new WebdavBadRequest();
    }

    if (debug) {
      trace("doNodeAndChildren: curDepth=" + curDepth +
            " maxDepth=" + maxDepth + " uri=" + node.getUri());
    }

    CaldavCalNode calnode = (CaldavCalNode)node;

    if (calnode.isCalendarCollection()) {
      return getNodes(node);
    }

    Collection nodes = new ArrayList();

    curDepth++;

    if (curDepth >= maxDepth) {
      return nodes;
    }

    Iterator children = getNsIntf().getChildren(node);

    while (children.hasNext()) {
      WebdavNsNode child = (WebdavNsNode)children.next();

      nodes.addAll(doNodeAndChildren(child, curDepth, maxDepth));
    }

    return nodes;
  }

  /** Handle free/busy response
   *
   * @param req
   * @param resp
   * @throws WebdavException
   */
  public void processFbResp(HttpServletRequest req,
                            HttpServletResponse resp) throws WebdavException {
    resp.setStatus(HttpServletResponse.SC_OK);
    resp.setContentType("text/calendar; charset=UTF-8");

    String resourceUri = getResourceUri(req);

    CaldavBWIntf intf = (CaldavBWIntf)getNsIntf();
    WebdavNsNode node = intf.getNode(resourceUri);

    if (!(node instanceof CaldavCalNode)) {
      if (debug) {
        trace("Expected CaldavCalNode - got " + node);
      }
      node.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } else {
      try {
        intf.getFreeBusy((CaldavCalNode)node, freeBusy);
      } catch (WebdavException wde) {
        if (debug) {
          trace("intf.getFreeBusy exception");
        }
        node.setStatus(wde.getStatusCode());
      }

      Writer out;
      try {
        out = resp.getWriter();
      } catch (Throwable t) {
        throw new WebdavException(t);
      }

      /** Get the content now to set up length, type etc.
       */
      Reader in = getNsIntf().getContent(node);
      resp.setContentLength(node.getContentLen());
      if (in == null) {
        if (debug) {
          debugMsg("status: " + HttpServletResponse.SC_NO_CONTENT);
        }

        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
      } else {
        if (debug) {
          debugMsg("send content - length=" + node.getContentLen());
        }

        writeContent(in, out);
      }
    }
  }

  // XXX Make the following part of the interface.

  /** size of buffer used for copying content to response.
   */
  private static final int bufferSize = 4096;

  private void writeContent(Reader in, Writer out)
      throws WebdavException {
    try {
      char[] buff = new char[bufferSize];
      int len;

      while (true) {
        len = in.read(buff);

        if (len < 0) {
          break;
        }

        out.write(buff, 0, len);
      }
    } catch (Throwable t) {
      throw new WebdavException(t);
    } finally {
      try {
        in.close();
      } catch (Throwable t) {}
      try {
        out.close();
      } catch (Throwable t) {}
    }
  }
}

