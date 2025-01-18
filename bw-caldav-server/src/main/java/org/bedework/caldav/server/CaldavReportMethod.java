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
package org.bedework.caldav.server;

import org.bedework.caldav.server.calquery.CalData;
import org.bedework.caldav.server.calquery.FreeBusyQuery;
import org.bedework.caldav.server.sysinterface.RetrievalMode;
import org.bedework.caldav.server.sysinterface.SysIntf.IcalResultType;
import org.bedework.caldav.util.DumpUtil;
import org.bedework.caldav.util.filter.parse.EventQuery;
import org.bedework.caldav.util.filter.parse.Filters;
import org.bedework.util.misc.Util;
import org.bedework.base.response.GetEntityResponse;
import org.bedework.util.timezones.Timezones;
import org.bedework.util.xml.XmlUtil;
import org.bedework.util.xml.tagdefs.CaldavTags;
import org.bedework.util.xml.tagdefs.WebdavTags;
import org.bedework.webdav.servlet.common.PropFindMethod.PropRequest;
import org.bedework.webdav.servlet.common.ReportMethod;
import org.bedework.webdav.servlet.shared.WebdavBadRequest;
import org.bedework.webdav.servlet.shared.WebdavException;
import org.bedework.webdav.servlet.shared.WebdavForbidden;
import org.bedework.webdav.servlet.shared.WebdavNsIntf;
import org.bedework.webdav.servlet.shared.WebdavNsNode;
import org.bedework.webdav.servlet.shared.WebdavProperty;
import org.bedework.webdav.servlet.shared.WebdavStatusCode;

import ietf.params.xml.ns.caldav.CalendarDataType;
import ietf.params.xml.ns.caldav.CompType;
import ietf.params.xml.ns.caldav.ExpandType;
import ietf.params.xml.ns.caldav.FilterType;
import ietf.params.xml.ns.caldav.LimitRecurrenceSetType;
import ietf.params.xml.ns.caldav.PropType;
import net.fortuna.ical4j.model.TimeZone;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.StringReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

/** Class called to handle CalDAV REPORT.
 *
 *   @author Mike Douglass   douglm rpi.edu
 */
public class CaldavReportMethod extends ReportMethod {
  /* The parsed results go here. We see:
   *  1. Free-busy request
   *  2. Query - optional props + filter
   *  3. Multi-get - optional props + one or more hrefs
   */

  private FreeBusyQuery freeBusy;

  protected static class CalendarQueryPars {
    public FilterType filter;
    public String tzid;
    public int depth;

    public CalendarQueryPars() {}
  }

  protected CalendarQueryPars cqpars;
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
   */
  @Override
  protected void process(final Document doc,
                         final int depth,
                         final HttpServletRequest req,
                         final HttpServletResponse resp) {
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
   * @param doc parsed document
   * @return index or <0 for unknown.
   */
  protected int getCaldavReportType(final Document doc) {
    try {
      final Element root = doc.getDocumentElement();

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
    } catch (final Throwable t) {
      System.err.println(t.getMessage());
      if (debug()) {
        error(t);
      }

      throw new WebdavException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  /* We process the parsed document and produce a Collection of request
   * objects to process.
   *
   * @param doc
   */
  protected void processDoc(final Document doc) {
    try {
      final CaldavBWIntf intf = (CaldavBWIntf)getNsIntf();

      final Element root = doc.getDocumentElement();

      if (reportType == reportTypeFreeBusy) {
        /* Expect exactly one time-range */

        final GetEntityResponse<Element> childResp =
                getOnlyChild(root);
        if (!childResp.isOk()) {
          throw new WebdavBadRequest(childResp.getMessage());
        }
        freeBusy = new FreeBusyQuery();
        freeBusy.parse(childResp.getEntity());
        if (debug()) {
          debug("REPORT: free-busy");
          freeBusy.dump();
        }

        return;
      }

      final Collection<Element> children = getChildren(root);

      /* Two possibilities:
               <!ELEMENT calendar-multiget ((DAV:allprop |
                                      DAV:propname |
                                      DAV:prop)?, DAV:href+)>

               <!ELEMENT calendar-query ((DAV:allprop |
                                    DAV:propname |
                                    DAV:prop)?, filter, timezone?)>
       */

      /* NOTE the first release of IOS5 has the filter before the properties
       * for the calendar query. Discovered during the Prague CalConnect
       * interop and apparently not fixed for the first release. Note that the
       * spec does specify the order.
       */

      if (children.isEmpty()) {
        throw new WebdavBadRequest();
      }

      final Iterator<Element> chiter = children.iterator();

      Element curnode = chiter.next();

      if (reportType == reportTypeQuery) {
        /* First try for a property request */
        preq = pm.tryPropRequest(curnode);

        if (preq != null) {
          if (!chiter.hasNext()) {
            throw new WebdavBadRequest();
          }

          curnode = chiter.next();
        }

        cqpars = new CalendarQueryPars();

        if (!XmlUtil.nodeMatches(curnode, CaldavTags.filter)) {
          throw new WebdavForbidden(CaldavTags.validFilter,
                                    "Expected filter");
        }

        cqpars.filter = Filters.parse(curnode);

        if (debug()) {
          debug("REPORT: query");
          DumpUtil.dumpFilter(cqpars.filter);
        }

        if (chiter.hasNext()) {
          curnode = chiter.next();
        } else {
          curnode = null;
        }

        if ((preq == null) && (curnode != null)) {
          // Try for the props again
          preq = pm.tryPropRequest(curnode);

          if ((preq != null) && (chiter.hasNext())) {
            curnode = chiter.next();
          } else {
            curnode = null;
          }
        }

        if (curnode != null) {
          // Only timezone allowed

          if (intf.getSysi().getSystemProperties().getTimezonesByReference() &&
                  XmlUtil.nodeMatches(curnode, CaldavTags.timezoneId)) {
            cqpars.tzid = getElementContent(curnode);

            final TimeZone tz = Timezones.getTz(cqpars.tzid);
            if (tz == null) {
              throw new WebdavForbidden(CaldavTags.validTimezone,
                                        "Unknown timezone " + cqpars.tzid);
            }
            return;
          }
          
          if (!XmlUtil.nodeMatches(curnode, CaldavTags.timezone)) {
            throw new WebdavForbidden(CaldavTags.validTimezone,
                                      "Missing timezone");
          }

          // Node content should be a timezone def
          final String tzdef = getElementContent(curnode);
          final SysiIcalendar ical;
          
          try {
            ical = intf.getSysi().fromIcal(null,
                                           new StringReader(tzdef),
                                           "text/calendar",
                                           IcalResultType.TimeZone,
                                           false);
          } catch (final Throwable t) {
            throw new WebdavForbidden(CaldavTags.validCalendarData,
                                      t.getLocalizedMessage());
          }
          final Collection<TimeZone> tzs = ical.getTimeZones();
          cqpars.tzid = tzs.iterator().next().getID();
        }

        return;
      }

      if (reportType == reportTypeMultiGet) {
        /* First try for a property request */
        preq = pm.tryPropRequest(curnode);

        if (preq != null) {
          if (!chiter.hasNext()) {
            throw new WebdavBadRequest();
          }

          curnode = chiter.next();
        }

        // One or more hrefs

        for (;;) {
          if (!XmlUtil.nodeMatches(curnode, WebdavTags.href)) {
            throw new WebdavBadRequest("Expected href");
          }

          String href = XmlUtil.getElementContent(curnode);

          if (href != null) {
            final String decoded;
            try {
              decoded = URLDecoder.decode(href,
                                          StandardCharsets.UTF_8);
            } catch (final Throwable t) {
              throw new WebdavBadRequest("bad href: " + href);
            }

            href = decoded;
          }

          if ((href == null) || (href.isEmpty())) {
            throw new WebdavBadRequest("Bad href");
          }

          if (hrefs == null) {
            hrefs = new ArrayList<>();
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

        if (debug()) {
          debug("REPORT: multi-get");

          for (final String href: hrefs) {
            debug("    <DAV:href>" + href + "</DAV:href>");
          }
        }

        return;
      }

      if (debug()) {
        debug("REPORT: unexpected element " + curnode.getNodeName() +
              " with type " + curnode.getNodeType());
      }
      throw new WebdavBadRequest("REPORT: unexpected element " + curnode.getNodeName() +
                                 " with type " + curnode.getNodeType());
    } catch (final WebdavException wde) {
      throw wde;
    } catch (final Throwable t) {
      error(t);
      throw new WebdavException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * @param req http request
   * @param resp http response
   * @param depth from depth header
   */
  public void processResp(final HttpServletRequest req,
                          final HttpServletResponse resp,
                          final int depth) {
    resp.setStatus(WebdavStatusCode.SC_MULTI_STATUS);
    resp.setContentType("text/xml;charset=utf-8");

    startEmit(resp);

    final String resourceUri = getResourceUri(req);

    if (reportType == reportTypeQuery) {
      cqpars.depth = depth;
    }

    process(cqpars, resourceUri);
  }

  protected void process(final CalendarQueryPars cqp,
                         final String resourceUri) {
    final CaldavBWIntf intf = (CaldavBWIntf)getNsIntf();
    final WebdavNsNode node = intf.getNode(resourceUri,
                                           WebdavNsIntf.existanceMust,
                                           WebdavNsIntf.nodeTypeUnknown,
                                           false);

    final int status = HttpServletResponse.SC_OK;

    Collection<WebdavNsNode> nodes = null;
    final Collection<String> badHrefs = new ArrayList<>();

    if (reportType == reportTypeQuery) {
      nodes = doNodeAndChildren(cqp, node);
    } else if (reportType == reportTypeMultiGet) {
      nodes = getMgetNodes(hrefs, badHrefs);
    }

    openTag(WebdavTags.multistatus);

    if (status != HttpServletResponse.SC_OK) {
      if (debug()) {
        debug("REPORT status " + status);
      }
      // Entire request failed.
      node.setStatus(status);
      doNodeProperties(node);
    } else if (nodes != null) {
      for (final WebdavNsNode curnode: nodes) {
        doNodeProperties(curnode);
      }
    }

    if (!Util.isEmpty(badHrefs)) {
      for (final String hr: badHrefs) {
        openTag(WebdavTags.response);
        property(WebdavTags.href, intf.getSysi().getUrlHandler().prefix(hr));
        property(WebdavTags.status, "HTTP/1.1 " + HttpServletResponse.SC_NOT_FOUND);
        closeTag(WebdavTags.response);
      }
    }

    closeTag(WebdavTags.multistatus);

    flush();
  }

  /** Return collection of nodes specified by list of hrefs.
   *
   * @param hrefs hrefs to find
   * @param badHrefs list of unsatisfied hrefs
   * @return Collection of nodes
   */
  public Collection<WebdavNsNode> getMgetNodes(final Collection<String> hrefs,
                                               final Collection<String> badHrefs) {
    final Collection<WebdavNsNode> nodes = new ArrayList<>();
    final CaldavBWIntf intf = (CaldavBWIntf)getNsIntf();

    if (hrefs == null) {
      return nodes;
    }

    for (final String hr: hrefs) {
      WebdavNsNode n;
      try {
        n = intf.getNode(intf.getUri(hr),
                         WebdavNsIntf.existanceMust,
                         WebdavNsIntf.nodeTypeUnknown,
                         false);
      } catch (final WebdavException we) {
        if (hr.endsWith("/")) {
          n = new CaldavCalNode(intf.getSysi(),
                                we.getStatusCode(),
                                intf.getUri(hr));
        } else {
          n = new CaldavComponentNode(intf.getSysi(),
                                      we.getStatusCode(),
                                      intf.getUri(hr));
        }
      }

      if (n != null) {
        nodes.add(n);
      } else {
        badHrefs.add(hr);
      }
    }

    return nodes;
  }

  protected Collection<WebdavNsNode> doNodeAndChildren(
          final CalendarQueryPars cqp,
          final WebdavNsNode node) {
    List<String> retrieveList = null;
    CalData caldata = null;

    if (preq != null) {
      if (debug()) {
        debug("REPORT: preq not null");
      }

      if (preq.reqType == PropRequest.ReqType.prop) {
        // Look for a calendar-data property
        for (final WebdavProperty prop: preq.props) {
          if (retrieveList == null) {
            retrieveList = new ArrayList<>();
          }

          if (prop instanceof CalData) {
            caldata = (CalData)prop;
          } else if (!addPropname(prop.getTag(),
                                  retrieveList)) {
            retrieveList = null;
            break;
          }
        }
      }
    }

    CompType comp = null;
    ExpandType expand = null;
    LimitRecurrenceSetType lrs = null;

    if (caldata != null) {
      final CalendarDataType cd = caldata.getCalendarData();
      comp = cd.getComp();

      expand = cd.getExpand();
      lrs = cd.getLimitRecurrenceSet();
    }

    /* This isn't ideal - we build a list which is an accumulation of all
     * properties for all components.
     */
    if (comp == null) {
      if (caldata != null) {
        // Retrieve everything
        retrieveList = null;
      }
    } else if (comp.getAllcomp() != null) {
      // Retrieve everything
      retrieveList = null;
    } else if (comp.getName().equalsIgnoreCase("VCALENDAR")) {
      // Should have "VACALENDAR" as outer

      if (comp.getComp().isEmpty()) {
        retrieveList = null;
      } else {
        for (final CompType calcomp: comp.getComp()) {
          final String nm = calcomp.getName().toUpperCase();
          if (nm.equals("VEVENT") ||
              nm.equals("VTODO") ||
              nm.equals("VJOURNAL") ||
              nm.equals("VAVAILABILITY")) {
            if ((calcomp.getAllprop() != null) ||
                Util.isEmpty(calcomp.getProp())) {
              retrieveList = null;
              break;
            }

            if (retrieveList == null) {
              retrieveList = new ArrayList<>();
            }

            for (final PropType p: calcomp.getProp()) {
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

    return doNodeAndChildren(cqp, node, expand, lrs, retrieveList);
  }

  protected Collection<WebdavNsNode> doNodeAndChildren(final CalendarQueryPars cqp,
                                    final WebdavNsNode node,
                                    final ExpandType expand,
                                    final LimitRecurrenceSetType lrs,
                                    final List<String> retrieveList) {
    RetrievalMode rm = null;

    if (expand != null) {
      /* expand with time range */
      rm = new RetrievalMode();
      rm.setExpand(expand);
    } else if (lrs != null) {
      /* Only return master event and overrides in range */
      rm = new RetrievalMode();
      rm.setLimitRecurrenceSet(lrs);
    }

    return doNodeAndChildren(cqp, node, 0, defaultDepth(cqp.depth, 0),
                             rm, retrieveList);
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  private Collection<WebdavNsNode> doNodeAndChildren(
          final CalendarQueryPars cqp,
          final WebdavNsNode node,
          int curDepth,
          final int maxDepth,
          final RetrievalMode rm,
          final List<String> retrieveList) {
    if (debug()) {
      debug("doNodeAndChildren: curDepth=" + curDepth +
            " maxDepth=" + maxDepth + " uri=" + node.getUri());
    }

    final Collection<WebdavNsNode> nodes = new ArrayList<>();

    if (node instanceof CaldavComponentNode) {
      // Targetted directly at component
      nodes.add(node);
      return nodes;
    }

    if (!(node instanceof final CaldavCalNode calnode)) {
      throw new WebdavBadRequest();
    }

    /* TODO - should we return info about the collection?
     * Probably if the filter allows it.
     */
    curDepth++;

    if (curDepth > maxDepth) {
      return nodes;
    }

    if (calnode.isCalendarCollection()) {
      return getNodes(cqp, node, rm, retrieveList);
    }

    final EventQuery eq;

    if (cqp.filter == null) {
      eq = null;
    } else {
      eq = Filters.getQuery(cqp.filter);
    }

    final Supplier<Object> filters = () -> {
      if (eq == null) {
        return null;
      }

      return eq.filter;
    };

    for (final WebdavNsNode child:
            getNsIntf().getChildren(node, filters)) {
      nodes.addAll(doNodeAndChildren(cqp,
                                     child,
                                     curDepth,
                                     maxDepth,
                                     rm,
                                     retrieveList));
    }

    return nodes;
  }

  private Collection<WebdavNsNode> getNodes(
          final CalendarQueryPars cqp,
          final WebdavNsNode node,
          final RetrievalMode rm,
          final List<String> retrieveList) {
    if (debug()) {
      debug("getNodes: " + node.getUri());
    }

    final CaldavBWIntf intf = (CaldavBWIntf)getNsIntf();

    return intf.query(node, retrieveList, rm, cqp.filter);
  }

  private boolean addPropname(final QName tag,
                              final List<String> retrieveList) {
    if (tag.equals(WebdavTags.getetag)) {
      retrieveList.add(tag.toString());
      return true;
    }

    return false;
  }

  /** Handle free/busy response
   *
   * @param req http request
   * @param resp http response
   * @param depth from depth header
   */
  public void processFbResp(final HttpServletRequest req,
                            final HttpServletResponse resp,
                            final int depth) {
    resp.setStatus(HttpServletResponse.SC_OK);
    resp.setContentType("text/calendar;charset=utf-8");

    final String resourceUri = getResourceUri(req);

    final CaldavBWIntf intf = (CaldavBWIntf)getNsIntf();
    final WebdavNsNode node =
            intf.getNode(resourceUri,
                         WebdavNsIntf.existanceMust,
                         WebdavNsIntf.nodeTypeCollection,
                         false);

    if (!(node instanceof final CaldavCalNode cnode)) {
      if (debug()) {
        debug("Expected CaldavCalNode - got " + node);
      }
      throw new WebdavBadRequest();
    }

    intf.getFreeBusy(cnode, freeBusy, defaultDepth(depth, 0));
    resp.setContentLength(-1);

    try {
      cnode.writeContent(null, resp.getWriter(), "text/calendar");
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }

    /*
    Writer out;
    try {
      out = resp.getWriter();
    } catch (Throwable t) {
      throw new WebdavException(t);
    }

    /** Get the content now to set up length, type etc.
     * /
    Content c = getNsIntf().getContent(req, resp, node);
    if ((c == null) || (c.rdr == null)) {
      if (debug()) {
        debug("status: " + HttpServletResponse.SC_NO_CONTENT);
      }

      resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    } else {
      resp.setContentType(c.contentType);
      resp.setContentLength((int)c.contentLength);
      if (debug()) {
        debug("send content - length=" + c.contentLength);
      }

      writeContent(c.rdr, out);
    }
    */
  }

  // XXX Make the following part of the interface.

  /* size of buffer used for copying content to response.
   * /
  private static final int bufferSize = 4096;

  private void writeContent(final Reader in, final Writer out) {
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
  }*/
}

