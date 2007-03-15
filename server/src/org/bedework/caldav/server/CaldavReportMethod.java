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
import org.bedework.caldav.server.calquery.ExpandRecurrenceSet;
import org.bedework.caldav.server.calquery.FreeBusyQuery;
import org.bedework.caldav.server.calquery.LimitRecurrenceSet;
import org.bedework.caldav.server.filter.Filter;
import org.bedework.calfacade.RecurringRetrievalMode;
import org.bedework.calfacade.RecurringRetrievalMode.Rmode;
import org.bedework.davdefs.CaldavTags;
import org.bedework.davdefs.WebdavTags;
import org.bedework.icalendar.Icalendar;

import edu.rpi.cct.webdav.servlet.common.ReportMethod;
import edu.rpi.cct.webdav.servlet.common.PropFindMethod.PropRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;
import edu.rpi.cct.webdav.servlet.shared.WebdavProperty;
import edu.rpi.cct.webdav.servlet.shared.WebdavStatusCode;
import edu.rpi.sss.util.xml.XmlUtil;

import net.fortuna.ical4j.model.TimeZone;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.Reader;
import java.io.StringReader;
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
  private ArrayList<String> hrefs;

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
      CaldavBWIntf intf = (CaldavBWIntf)getNsIntf();

      Element root = doc.getDocumentElement();

      if (reportType == reportTypeFreeBusy) {
        /* Expect exactly one time-range */
        freeBusy = new FreeBusyQuery(intf, debug);
        freeBusy.parse(getOnlyChild(root));
        if (debug) {
          trace("REPORT: free-busy");
          freeBusy.dump();
        }

        return;
      }

      Collection<Element> children = getChildren(root);

      /* Two possibilities:
               <!ELEMENT calendar-multiget ((DAV:allprop |
                                      DAV:propname |
                                      DAV:prop)?, DAV:href+)>

               <!ELEMENT calendar-query ((DAV:allprop |
                                    DAV:propname |
                                    DAV:prop)?, filter, timezone?)>
       */

      if (children.isEmpty()) {
        throw new WebdavBadRequest();
      }

      Iterator<Element> chiter = children.iterator();

      /* First try for a property request */
      preq = pm.tryPropRequest(chiter.next());

      if (preq != null) {
        if (debug) {
          trace("REPORT: preq not null");
        }

        if (preq.reqType == PropRequest.ReqType.prop) {
          // Look for a calendar-data property
          for (WebdavProperty prop: preq.props) {
            if (prop instanceof CalendarData) {
              caldata = (CalendarData)prop;
            }
          }
        }
      }

      if (!chiter.hasNext()) {
        throw new WebdavBadRequest();
      }

      Element curnode = chiter.next();

      if (reportType == reportTypeQuery) {
        // Filter required next

        if (!nodeMatches(curnode, CaldavTags.filter)) {
          throw new WebdavBadRequest("Expected filter");
        }

        // Delay parsing until we see if we have a timezone
        Element filterNode = curnode;
        TimeZone tz = null;

        if (chiter.hasNext()) {
          // Only timezone allowed
          curnode = chiter.next();

          if (!nodeMatches(curnode, CaldavTags.timezone)) {
            throw new WebdavBadRequest("Expected timezone");
          }

          // Node content should be a timezone def
          String tzdef = getElementContent(curnode);
          Icalendar ical = intf.getSysi().fromIcal(null,
                                                   new StringReader(tzdef));
          Collection<TimeZone> tzs = ical.getTimeZones();
          if (tzs.isEmpty()) {
            throw new WebdavBadRequest("Expected timezone");
          }

          if (tzs.size() > 1) {
            throw new WebdavBadRequest("Expected one timezone");
          }

          tz = tzs.iterator().next();
        }

        filter = new Filter(intf, debug);
        int st = filter.parse(filterNode, tz);

        if (st != HttpServletResponse.SC_OK) {
          throw new WebdavException(st);
        }

        if (debug) {
          trace("REPORT: query");
          filter.dump();
        }

        return;
      }

      if (reportType == reportTypeMultiGet) {
        // One or more hrefs

        for (;;) {
          if (!WebdavTags.href.nodeMatches(curnode)) {
            throw new WebdavBadRequest("Expected href");
          }

          String href = XmlUtil.getElementContent(curnode);

          if ((href == null) || (href.length() == 0)) {
            throw new WebdavBadRequest("Bad href");
          }

          if (hrefs == null) {
            hrefs = new ArrayList<String>();
          }

          hrefs.add(href);
          if (!chiter.hasNext()) {
            break;
          }
          curnode = chiter.next();
        }

        if (hrefs == null) {
          // need at least 1
          throw new WebdavBadRequest("Expected href");
        }

        if (debug) {
          trace("REPORT: multi-get");

          for (String href: hrefs) {
            trace("    <DAV:href>" + href + "</DAV:href>");
          }
        }

        return;
      }

      if (debug) {
        trace("REPORT: unexpected element " + curnode.getNodeName() +
              " with type " + curnode.getNodeType());
      }
      throw new WebdavBadRequest("REPORT: unexpected element " + curnode.getNodeName() +
                                 " with type " + curnode.getNodeType());
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      error(t);
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
    WebdavNsNode node = intf.getNode(resourceUri,
                                     WebdavNsIntf.existanceMust,
                                     WebdavNsIntf.nodeTypeUnknown);

    openTag(WebdavTags.multistatus);

    int status = HttpServletResponse.SC_OK;

    Collection<WebdavNsNode> nodes = null;

    if (reportType == reportTypeQuery) {
      nodes = (Collection<WebdavNsNode>)doNodeAndChildren(node, 0, defaultDepth(depth, 0));
    } else if (reportType == reportTypeMultiGet) {
      nodes = new ArrayList<WebdavNsNode>();

      if (hrefs != null) {
        for (String hr: hrefs) {
          try {
            nodes.add(intf.getNode(intf.getUri(hr),
                                   WebdavNsIntf.existanceMust,
                                   WebdavNsIntf.nodeTypeUnknown));
          } catch (WebdavException we) {
            nodes.add((WebdavNsNode)new CaldavCalNode(we.getStatusCode(),
                                                      intf.getUri(hr), debug));
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

  private Collection<WebdavNsNode> getNodes(WebdavNsNode node)
          throws WebdavException {
    if (debug) {
      trace("getNodes: " + node.getUri());
    }

    CaldavBWIntf intf = (CaldavBWIntf)getNsIntf();

    RecurringRetrievalMode rrm;

    if (caldata == null) {
      rrm = new RecurringRetrievalMode(Rmode.overrides);
    } else if (caldata.getErs() != null) {
      /* expand with time range */
      ExpandRecurrenceSet ers = caldata.getErs();

      rrm = new RecurringRetrievalMode(Rmode.expanded,
                                       ers.getStart(), ers.getEnd());
    } else if (caldata.getLrs() != null) {
      /* Only return master event and overrides in range */
      LimitRecurrenceSet lrs = caldata.getLrs();
      rrm = new RecurringRetrievalMode(Rmode.overrides,
                                       lrs.getStart(), lrs.getEnd());
    } else {
      /* Return master + overrides */
      rrm = new RecurringRetrievalMode(Rmode.overrides);
    }

    return intf.query(node, rrm, filter);
  }

  private Collection<WebdavNsNode> doNodeAndChildren(WebdavNsNode node,
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

    Collection<WebdavNsNode> nodes = new ArrayList<WebdavNsNode>();

    curDepth++;

    if (curDepth > maxDepth) {
      return nodes;
    }

    for (WebdavNsNode child: getNsIntf().getChildren(node)) {
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
    WebdavNsNode node = intf.getNode(resourceUri,
                                     WebdavNsIntf.existanceMust,
                                     WebdavNsIntf.nodeTypeCollection);

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
          wde.printStackTrace();
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

