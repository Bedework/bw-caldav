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

import edu.rpi.cct.webdav.servlet.common.PropFindMethod;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;

import org.oasis_open.docs.ns.wscal.calws_soap.CalendarQuery;
import org.w3c.dom.Document;

import ietf.params.xml.ns.icalendar_2.Icalendar;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;

/**
 * @author douglm
 */
public class ReportBase extends CaldavReportMethod {
  /**
   * @param nsIntf
   */
  public ReportBase(final WebdavNsIntf nsIntf) {
    super();

    this.nsIntf = nsIntf;
    xml = nsIntf.getXmlEmit();
  }

  /**
   * @param qstring - query string
   * @param resourceUri
   * @return Document
   * @throws WebdavException
   */
  public Document query(final String qstring,
                        final String resourceUri) throws WebdavException {
    pm = new PropFindMethod();
    pm.init(getNsIntf(), debug, true);

    Document doc = parseContent(qstring.length(),
                                new StringReader(qstring));

    processDoc(doc);

    try {
      // Set up XmlEmit so we can process the output.
      StringWriter sw = new StringWriter();
      xml.startEmit(sw);
      cqpars.depth = 1;

      process(cqpars, resourceUri);

      String s = sw.toString();
      return parseContent(s.length(),
                          new StringReader(s));
    } catch (WebdavException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /**
   * @param resourceUri
   * @param cq
   * @return collection of nodes
   * @throws WebdavException
   */
  public Collection<WebdavNsNode> query(final String resourceUri,
                                        final CalendarQuery cq) throws WebdavException {
    WebdavNsNode node = getNsIntf().getNode(resourceUri,
                                            WebdavNsIntf.existanceMust,
                                            WebdavNsIntf.nodeTypeUnknown);

    CalendarQueryPars cqp = new CalendarQueryPars();

    cqp.filter = cq.getFilter();
    cqp.depth = 1;

    return doNodeAndChildren(cqp, node);
  }

  Icalendar fetch(final String resourceUri,
                  final String uid) throws WebdavException {
    // Build a report query and execute it.

    StringBuilder sb = new StringBuilder();

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
    pm.init(getNsIntf(), debug, true);

    Document doc = parseContent(sb.length(),
                                new StringReader(sb.toString()));

    processDoc(doc);

    /* ugly - cqpars is result of processDoc. Need to restructure */
    cqpars.depth = 1;
    process(cqpars, resourceUri);

    return null;
  }

}
