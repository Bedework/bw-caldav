/* **********************************************************************
    Copyright 2007 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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
import org.bedework.caldav.server.filter.QueryFilter;
import org.bedework.caldav.server.sysinterface.RetrievalMode;

import edu.rpi.cct.webdav.servlet.common.ReportMethod;
import edu.rpi.cct.webdav.servlet.common.PropFindMethod.PropRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;
import edu.rpi.cct.webdav.servlet.shared.WebdavProperty;
import edu.rpi.cct.webdav.servlet.shared.WebdavStatusCode;
import edu.rpi.sss.util.xml.XmlUtil;
import edu.rpi.sss.util.xml.tagdefs.CaldavTags;
import edu.rpi.sss.util.xml.tagdefs.WebdavTags;

import net.fortuna.ical4j.model.TimeZone;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.URLDecoder;
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
  private QueryFilter filter;
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
      processFbResp(req, resp, depth);
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

      if (XmlUtil.nodeMatches(root, CaldavTags.calendarQuery)) {
        return reportTypeQuery;
      }

      if (XmlUtil.nodeMatches(root, CaldavTags.calendarMultiget)) {
        return reportTypeMultiGet;
      }

      if (XmlUtil.nodeMatches(root, CaldavTags.freeBusyQuery)) {
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
        freeBusy = new FreeBusyQuery(debug);
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

        if (!XmlUtil.nodeMatches(curnode, CaldavTags.filter)) {
          throw new WebdavBadRequest("Expected filter");
        }

        // Delay parsing until we see if we have a timezone
        Element filterNode = curnode;
        String tzid = null;

        if (chiter.hasNext()) {
          // Only timezone allowed
          curnode = chiter.next();

          if (!XmlUtil.nodeMatches(curnode, CaldavTags.timezone)) {
            throw new WebdavBadRequest("Expected timezone");
          }

          // Node content should be a timezone def
          String tzdef = getElementContent(curnode);
          SysiIcalendar ical = intf.getSysi().fromIcal(null,
                                                   new StringReader(tzdef));
          Collection<TimeZone> tzs = ical.getTimeZones();
          if (tzs.isEmpty()) {
            throw new WebdavBadRequest("Expected timezone");
          }

          if (tzs.size() > 1) {
            throw new WebdavBadRequest("Expected one timezone");
          }

          tzid = tzs.iterator().next().getID();
        }

        filter = new QueryFilter(debug);
        filter.parse(filterNode, tzid);

        if (debug) {
          trace("REPORT: query");
          filter.dump();
        }

        return;
      }

      if (reportType == reportTypeMultiGet) {
        // One or more hrefs

        for (;;) {
          if (!XmlUtil.nodeMatches(curnode, WebdavTags.href)) {
            throw new WebdavBadRequest("Expected href");
          }

          String href = XmlUtil.getElementContent(curnode);

          if (href != null) {
            String decoded;
            try {
              decoded = URLDecoder.decode(href, "UTF8");
            } catch (Throwable t) {
              throw new WebdavBadRequest("bad href: " + href);
            }

            href = decoded;
          }

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
    resp.setStatus(WebdavStatusCode.SC_MULTI_STATUS);
    resp.setContentType("text/xml; charset=UTF-8");

    startEmit(resp);

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
            if (hr.endsWith("/")) {
              nodes.add((WebdavNsNode)new CaldavCalNode(intf.getSysi(),
                                                        we.getStatusCode(),
                                                        intf.getUri(hr), debug));
            } else {
              nodes.add((WebdavNsNode)new CaldavComponentNode(intf.getSysi(),
                                                              we.getStatusCode(),
                                                              intf.getUri(hr), debug));
            }
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
      for (WebdavNsNode curnode: nodes) {
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

    RetrievalMode rm = null;

    if (caldata != null) {
      if (caldata.getErs() != null) {
        /* expand with time range */
        ExpandRecurrenceSet ers = caldata.getErs();

        rm = RetrievalMode.getExpanded(ers.getStart(), ers.getEnd());
      } else if (caldata.getLrs() != null) {
        /* Only return master event and overrides in range */
        LimitRecurrenceSet lrs = caldata.getLrs();
        rm = RetrievalMode.getLimited(lrs.getStart(), lrs.getEnd());
      }
    }

    return intf.query(node, rm, filter);
  }

  private Collection<WebdavNsNode> doNodeAndChildren(WebdavNsNode node,
                                       int curDepth,
                                       int maxDepth) throws WebdavException {
    if (node instanceof CaldavComponentNode) {
      // Targetted directly at component
      Collection<WebdavNsNode> nodes = new ArrayList<WebdavNsNode>();

      nodes.add(node);
      return nodes;
    }

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
   * @param depth
   * @throws WebdavException
   */
  public void processFbResp(HttpServletRequest req,
                            HttpServletResponse resp,
                            int depth) throws WebdavException {
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
      throw new WebdavBadRequest();
    }

    intf.getFreeBusy((CaldavCalNode)node, freeBusy, defaultDepth(depth, 0));

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

