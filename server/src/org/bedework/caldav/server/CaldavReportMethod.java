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
import org.bedework.caldav.server.calquery.Comp;
import org.bedework.caldav.server.calquery.Expand;
import org.bedework.caldav.server.calquery.FreeBusyQuery;
import org.bedework.caldav.server.calquery.LimitRecurrenceSet;
import org.bedework.caldav.server.calquery.Prop;
import org.bedework.caldav.server.filter.FilterHandler;
import org.bedework.caldav.server.sysinterface.RetrievalMode;

import edu.rpi.cct.webdav.servlet.common.ReportMethod;
import edu.rpi.cct.webdav.servlet.common.PropFindMethod.PropRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;
import edu.rpi.cct.webdav.servlet.shared.WebdavProperty;
import edu.rpi.cct.webdav.servlet.shared.WebdavStatusCode;
import edu.rpi.sss.util.Util;
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
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

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
  private FilterHandler filter;
  private ArrayList<String> hrefs;

  // ENUM
  private final static int reportTypeQuery = 0;
  private final static int reportTypeMultiGet = 1;
  private final static int reportTypeFreeBusy = 2;

  private int reportType;

  /** Called at each request
   */
  @Override
  public void init() {
  }

  /* We process the parsed document and produce a response
   *
   * @param doc
   * @throws WebdavException
   */
  @Override
  protected void process(final Document doc,
                         final int depth,
                         final HttpServletRequest req,
                         final HttpServletResponse resp) throws WebdavException {
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
  protected int getCaldavReportType(final Document doc) throws WebdavException {
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
  private void processDoc(final Document doc) throws WebdavException {
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

        filter = new FilterHandler(debug);
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
  public void processResp(final HttpServletRequest req,
                          final HttpServletResponse resp,
                          final int depth) throws WebdavException {
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
    Collection<String> badHrefs = new ArrayList<String>();

    if (reportType == reportTypeQuery) {
      nodes = doNodeAndChildren(node, 0, defaultDepth(depth, 0));
    } else if (reportType == reportTypeMultiGet) {
      nodes = new ArrayList<WebdavNsNode>();

      if (hrefs != null) {
        for (String hr: hrefs) {
          WebdavNsNode n = null;
          try {
            n = intf.getNode(intf.getUri(hr),
                             WebdavNsIntf.existanceMust,
                             WebdavNsIntf.nodeTypeUnknown);
          } catch (WebdavException we) {
            if (hr.endsWith("/")) {
              n = new CaldavCalNode(intf.getSysi(),
                                    we.getStatusCode(),
                                    intf.getUri(hr), debug);
            } else {
              n = new CaldavComponentNode(intf.getSysi(),
                                          we.getStatusCode(),
                                          intf.getUri(hr), debug);
            }
          }

          if (n != null) {
            nodes.add(n);
          } else {
            badHrefs.add(hr);
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

    if (!Util.isEmpty(badHrefs)) {
      for (String hr: badHrefs) {
        openTag(WebdavTags.response);
        property(WebdavTags.href, intf.getSysi().getUrlHandler().prefix(hr));
        property(WebdavTags.status, "HTTP/1.1 " + HttpServletResponse.SC_NOT_FOUND);
        closeTag(WebdavTags.response);
      }
    }

    closeTag(WebdavTags.multistatus);

    flush();
  }

  private Collection<WebdavNsNode> getNodes(final WebdavNsNode node)
          throws WebdavException {
    if (debug) {
      trace("getNodes: " + node.getUri());
    }

    CaldavBWIntf intf = (CaldavBWIntf)getNsIntf();

    RetrievalMode rm = null;
    List<String> retrieveList = null;
    CalendarData caldata = null;

    if (preq != null) {
      if (debug) {
        trace("REPORT: preq not null");
      }

      if (preq.reqType == PropRequest.ReqType.prop) {
        // Look for a calendar-data property
        for (WebdavProperty prop: preq.props) {
          if (retrieveList == null) {
            retrieveList = new ArrayList<String>();
          }

          if (prop instanceof CalendarData) {
            caldata = (CalendarData)prop;
          } else if (!addPropname(prop.getTag(),
                                  retrieveList)) {
            retrieveList = null;
            break;
          }
        }
      }
    }

    Comp comp = null;

    if (caldata != null) {
      comp = caldata.getComp();

      if (caldata.getErs() != null) {
        /* expand with time range */
        Expand ers = caldata.getErs();

        rm = RetrievalMode.getExpanded(ers.getStart(), ers.getEnd());
      } else if (caldata.getLrs() != null) {
        /* Only return master event and overrides in range */
        LimitRecurrenceSet lrs = caldata.getLrs();
        rm = RetrievalMode.getLimited(lrs.getStart(), lrs.getEnd());
      }
    }

    /* This isn't ideal - we build a list which is an accumulation of all
     * properties for all components.
     */
    if ((comp != null) && !comp.getAllcomp()) {
      // Should have "VACALENDAR" as outer
      if (comp.getName().equals("VCALENDAR")) {
        for (Comp calcomp: comp.getComps()) {
          if (calcomp.getName().equals("VEVENT") ||
              calcomp.getName().equals("VTODO") ||
              calcomp.getName().equals("VJOURNAL")) {
            if (calcomp.getAllprop() || Util.isEmpty(calcomp.getProps())) {
              retrieveList = null;
              break;
            }

            if (retrieveList == null) {
              retrieveList = new ArrayList<String>();
            }

            for (Prop p: calcomp.getProps()) {
              if (!retrieveList.contains(p.getName())) {
                retrieveList.add(p.getName());
              }
            }
          }
        }
      }
    }

    if (Util.isEmpty(retrieveList)) {
      retrieveList = null;
    }

    return intf.query(node, retrieveList, rm, filter);
  }

  private boolean addPropname(final QName tag,
                              final List<String> retrieveList) {
    if (tag.equals(WebdavTags.getetag)) {
      retrieveList.add(tag.toString());
      return true;
    }

    return false;
  }

  private Collection<WebdavNsNode> doNodeAndChildren(final WebdavNsNode node,
                                       int curDepth,
                                       final int maxDepth) throws WebdavException {
    if (debug) {
      trace("doNodeAndChildren: curDepth=" + curDepth +
            " maxDepth=" + maxDepth + " uri=" + node.getUri());
    }

    Collection<WebdavNsNode> nodes = new ArrayList<WebdavNsNode>();

    if (node instanceof CaldavComponentNode) {
      // Targetted directly at component
      nodes.add(node);
      return nodes;
    }

    if (!(node instanceof CaldavCalNode)) {
      throw new WebdavBadRequest();
    }

    CaldavCalNode calnode = (CaldavCalNode)node;

    /* TODO - should we return info about the collection?
     * Probably if the filter allows it.
     */
    curDepth++;

    if (curDepth > maxDepth) {
      return nodes;
    }

    if (calnode.isCalendarCollection()) {
      return getNodes(node);
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
  public void processFbResp(final HttpServletRequest req,
                            final HttpServletResponse resp,
                            final int depth) throws WebdavException {
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
    Reader in = getNsIntf().getContent(req, resp, node);
    resp.setContentLength((int)node.getContentLen());
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

  private void writeContent(final Reader in, final Writer out)
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

