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
package org.bedework.caldav.server.soap;

import org.bedework.caldav.server.CaldavReportMethod;
import org.bedework.util.calendar.XcalUtil;
import org.bedework.webdav.servlet.common.PropFindMethod;
import org.bedework.webdav.servlet.shared.WebdavException;
import org.bedework.webdav.servlet.shared.WebdavNsIntf;
import org.bedework.webdav.servlet.shared.WebdavNsNode;

import ietf.params.xml.ns.icalendar_2.IcalendarType;
import org.oasis_open.docs.ws_calendar.ns.soap.CalendarQueryType;
import org.oasis_open.docs.ws_calendar.ns.soap.CompFilterType;
import org.oasis_open.docs.ws_calendar.ns.soap.ExpandType;
import org.oasis_open.docs.ws_calendar.ns.soap.FilterType;
import org.oasis_open.docs.ws_calendar.ns.soap.LimitRecurrenceSetType;
import org.oasis_open.docs.ws_calendar.ns.soap.ParamFilterType;
import org.oasis_open.docs.ws_calendar.ns.soap.PropFilterType;
import org.oasis_open.docs.ws_calendar.ns.soap.TextMatchType;
import org.oasis_open.docs.ws_calendar.ns.soap.UTCTimeRangeType;
import org.w3c.dom.Document;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;

/**
 * @author douglm
 */
public class ReportBase extends CaldavReportMethod {
  /**
   * @param nsIntf interface to underlying system
   */
  public ReportBase(final WebdavNsIntf nsIntf) {
    super();

    this.nsIntf = nsIntf;
    xml = nsIntf.getXmlEmit();
  }

  /**
   * @param qstring - query string
   * @param resourceUri uri to resource
   * @return Document
   */
  public Document query(final String qstring,
                        final String resourceUri) {
    pm = new PropFindMethod();
    pm.init(getNsIntf(), true);

    final Document doc = parseContent(qstring.length(),
                                      new StringReader(qstring));

    processDoc(doc);

    try {
      // Set up XmlEmit so we can process the output.
      final StringWriter sw = new StringWriter();
      xml.startEmit(sw);
      cqpars.depth = 1;

      process(cqpars, resourceUri);

      final String s = sw.toString();
      return parseContent(s.length(),
                          new StringReader(s));
    } catch (final WebdavException we) {
      throw we;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  /**
   * @param resourceUri uri to resource
   * @param cq query
   * @return collection of nodes
   */
  public Collection<WebdavNsNode> query(final String resourceUri,
                                        final CalendarQueryType cq) {
    final WebdavNsNode node =
            getNsIntf().getNode(resourceUri,
                                WebdavNsIntf.existanceMust,
                                WebdavNsIntf.nodeTypeUnknown,
                                false);

    final CalendarQueryPars cqp = new CalendarQueryPars();

    cqp.filter = convertFilter(cq.getFilter());
    cqp.depth = 1;

    return doNodeAndChildren(cqp, node,
                             convertExpand(cq.getExpand()),
                             convertLimitRecurrenceSet(cq.getLimitRecurrenceSet()),
                             null);
  }

  IcalendarType fetch(final String resourceUri,
                      final String uid) {
    // Build a report query and execute it.

    final StringBuilder sb = new StringBuilder();

    sb.append("<?xml version='1.0' encoding='utf-8' ?>");
    sb.append("<C:calendar-query xmlns:C='urn:ietf:params:xml:ns:caldav'>");
    sb.append("  <D:prop xmlns:D='DAV:'>");
    sb.append("    <C:calendar-data content-type='application/calendar+xml'>");
    sb.append("      <C:comp name='VCALENDAR'>");
    sb.append("        <C:comp name='VEVENT'>");
    sb.append("        </C:comp>");
    sb.append("        <C:comp name='VTODO'>");
    sb.append("        </C:comp>");
    sb.append("      </C:comp>");
    sb.append("    </C:calendar-data>");
    sb.append("  </D:prop>");
    sb.append("  <C:filter>");
    sb.append("    <C:comp-filter name='VCALENDAR'>");
    //sb.append("      <C:comp-filter name='VEVENT'>");
    //sb.append("      </C:comp-filter>");
    sb.append("    </C:comp-filter>");
    sb.append("  </C:filter>");
    sb.append("</C:calendar-query>");

    pm = new PropFindMethod();
    pm.init(getNsIntf(), true);

    final Document doc = parseContent(sb.length(),
                                      new StringReader(sb.toString()));

    processDoc(doc);

    /* ugly - cqpars is result of processDoc. Need to restructure */
    cqpars.depth = 1;
    process(cqpars, resourceUri);

    return null;
  }

  /** The SOAP FilterType is an almost exact replica of the CalDAV FilterType.
   * Unfortunately not quite so we have to translate it here.
   *
   * @param val
   * @return converted filter.
   */
  private ietf.params.xml.ns.caldav.FilterType convertFilter(final FilterType val) {
    if (val == null) {
      return null;
    }

    final ietf.params.xml.ns.caldav.FilterType filter =
        new ietf.params.xml.ns.caldav.FilterType();

    filter.setCompFilter(convertCompFilter(val.getCompFilter()));

    return filter;
  }

  private ietf.params.xml.ns.caldav.CompFilterType convertCompFilter(final CompFilterType val) {
    if (val == null) {
      return null;
    }

    final ietf.params.xml.ns.caldav.CompFilterType compFilter =
        new ietf.params.xml.ns.caldav.CompFilterType();

    if (val.getIsNotDefined() != null) {
      compFilter.setIsNotDefined(new ietf.params.xml.ns.caldav.IsNotDefinedType());
    }

    if (val.getTest() != null) {
      compFilter.setTest(val.getTest());
    }

    compFilter.setTimeRange(convertTimeRange(val.getTimeRange()));

    for (final PropFilterType pf: val.getPropFilter()) {
      compFilter.getPropFilter().add(convertPropFilter(pf));
    }

    for (final CompFilterType cf: val.getCompFilter()) {
      compFilter.getCompFilter().add(convertCompFilter(cf));
    }

    if (val.getAnyComp() != null) {
      compFilter.setName("*");
    } else if (val.getVcalendar() != null) {
      compFilter.setName("vcalendar");
    } else {
      compFilter.setName(val.getBaseComponent().getName().getLocalPart());
    }

    return compFilter;
  }

  private ietf.params.xml.ns.caldav.UTCTimeRangeType convertTimeRange(final UTCTimeRangeType val) {
    if (val == null) {
      return null;
    }

    final ietf.params.xml.ns.caldav.UTCTimeRangeType res =
        new ietf.params.xml.ns.caldav.UTCTimeRangeType();

    res.setStart(XcalUtil.getIcalFormatDateTime(val.getStart()));
    res.setEnd(XcalUtil.getIcalFormatDateTime(val.getEnd()));

    return res;
  }

  private ietf.params.xml.ns.caldav.ExpandType convertExpand(final ExpandType val) {
    if (val == null) {
      return null;
    }

    final ietf.params.xml.ns.caldav.ExpandType res =
        new ietf.params.xml.ns.caldav.ExpandType();

    res.setStart(XcalUtil.getIcalFormatDateTime(val.getStart()));
    res.setEnd(XcalUtil.getIcalFormatDateTime(val.getEnd()));

    return res;
  }

  private ietf.params.xml.ns.caldav.LimitRecurrenceSetType convertLimitRecurrenceSet(final LimitRecurrenceSetType val) {
    if (val == null) {
      return null;
    }

    final ietf.params.xml.ns.caldav.LimitRecurrenceSetType res =
        new ietf.params.xml.ns.caldav.LimitRecurrenceSetType();

    res.setStart(XcalUtil.getIcalFormatDateTime(val.getStart()));
    res.setEnd(XcalUtil.getIcalFormatDateTime(val.getEnd()));

    return res;
  }

  private ietf.params.xml.ns.caldav.PropFilterType convertPropFilter(final PropFilterType val) {
    final ietf.params.xml.ns.caldav.PropFilterType pf =
        new ietf.params.xml.ns.caldav.PropFilterType();

    if (val.getIsNotDefined() != null) {
      pf.setIsNotDefined(new ietf.params.xml.ns.caldav.IsNotDefinedType());
    }

    if (val.getTest() != null) {
      pf.setTest(val.getTest());
    }

    pf.setTimeRange(convertTimeRange(val.getTimeRange()));
    pf.setTextMatch(convertTextMatch(val.getTextMatch()));

    for (final ParamFilterType parf: val.getParamFilter()) {
      pf.getParamFilter().add(convertParamFilter(parf));
    }

    pf.setName(val.getBaseProperty().getName().getLocalPart());

    return pf;
  }

  private ietf.params.xml.ns.caldav.TextMatchType convertTextMatch(final TextMatchType val) {
    final ietf.params.xml.ns.caldav.TextMatchType tm =
        new ietf.params.xml.ns.caldav.TextMatchType();

    tm.setValue(val.getValue());
    tm.setCollation(val.getCollation());

    if (val.isNegateCondition()) {
      tm.setNegateCondition("yes");
    } else {
      tm.setNegateCondition("no");
    }

    return tm;
  }

  private ietf.params.xml.ns.caldav.ParamFilterType convertParamFilter(final ParamFilterType val) {
    final ietf.params.xml.ns.caldav.ParamFilterType pf =
        new ietf.params.xml.ns.caldav.ParamFilterType();

    if (val.getIsNotDefined() != null) {
      pf.setIsNotDefined(new ietf.params.xml.ns.caldav.IsNotDefinedType());
    }

    pf.setTextMatch(convertTextMatch(val.getTextMatch()));

    pf.setName(val.getBaseParameter().getName().getLocalPart());

    return pf;
  }
}
