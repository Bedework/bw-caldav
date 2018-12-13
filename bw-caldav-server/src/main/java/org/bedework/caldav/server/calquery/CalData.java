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
package org.bedework.caldav.server.calquery;

import org.bedework.caldav.server.CaldavComponentNode;
import org.bedework.caldav.server.sysinterface.SysIntf;
import org.bedework.caldav.util.DumpUtil;
import org.bedework.caldav.util.ParseUtil;
import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.XmlUtil;
import org.bedework.util.xml.tagdefs.CaldavTags;
import org.bedework.util.xml.tagdefs.XcalTags;
import org.bedework.webdav.servlet.shared.WebdavBadRequest;
import org.bedework.webdav.servlet.shared.WebdavException;
import org.bedework.webdav.servlet.shared.WebdavForbidden;
import org.bedework.webdav.servlet.shared.WebdavNsNode;
import org.bedework.webdav.servlet.shared.WebdavProperty;

import ietf.params.xml.ns.caldav.AllcompType;
import ietf.params.xml.ns.caldav.AllpropType;
import ietf.params.xml.ns.caldav.CalendarDataType;
import ietf.params.xml.ns.caldav.CompType;
import ietf.params.xml.ns.caldav.ExpandType;
import ietf.params.xml.ns.caldav.LimitFreebusySetType;
import ietf.params.xml.ns.caldav.LimitRecurrenceSetType;
import ietf.params.xml.ns.caldav.PropType;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.VEvent;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

/** Class to represent a calendar-query calendar-data element
 *
 *   @author Mike Douglass   douglm  rpi.edu
 */
public class CalData extends WebdavProperty implements Logged {
  /*
      <!ELEMENT calendar-data ((comp?, (expand |
                                           limit-recurrence-set)?,
                                           limit-freebusy-set?) |
                          #PCDATA)?>

         pcdata is for response

      <!ATTLIST calendar-data return-content-type CDATA
                      "text/calendar">

      <!ELEMENT comp ((allcomp, (allprop | prop*)) |
                       (comp*, (allprop | prop*)))>

      <!ATTLIST comp name CDATA #REQUIRED>

      <!ELEMENT allcomp EMPTY>

      <!ELEMENT allprop EMPTY>

      <!ELEMENT prop EMPTY>

      <!ATTLIST prop name CDATA #REQUIRED
                     novalue (yes|no) "no">

      <!ELEMENT expand-recurrence-set EMPTY>

      <!ATTLIST expand-recurrence-set start CDATA #REQUIRED
                                      end CDATA #REQUIRED>
----------------------------------------------------------------------
         <!ELEMENT calendar-data ((comp?, (expand |
                                           limit-recurrence-set)?,
                                           limit-freebusy-set?) |
                                  #PCDATA)?>
         PCDATA value: iCalendar object

         <!ATTLIST calendar-data content-type CDATA "text/calendar">
                                 version CDATA "2.0">
         content-type value: a MIME media type
         version value: a version string

         <!ELEMENT comp ((allprop | prop*), (allcomp | comp*))>

         <!ATTLIST comp name CDATA #REQUIRED>
         name value: a calendar component name

         <!ELEMENT allcomp EMPTY>

         <!ELEMENT allprop EMPTY>

         <!ELEMENT prop EMPTY>

         <!ATTLIST prop name CDATA #REQUIRED
                     novalue (yes | no) "no">
         name value: a calendar property name
         novalue value: "yes" or "no"

         <!ELEMENT expand EMPTY>

         <!ATTLIST expand start CDATA #REQUIRED
                         end   CDATA #REQUIRED>
         start value: an iCalendar "date with UTC time"
         end value: an iCalendar "date with UTC time"

         <!ELEMENT limit-recurrence-set EMPTY>

         <!ATTLIST limit-recurrence-set start CDATA #REQUIRED
                                       end   CDATA #REQUIRED>
         start value: an iCalendar "date with UTC time"
         end value: an iCalendar "date with UTC time"

         <!ELEMENT limit-freebusy-set EMPTY>

         <!ATTLIST limit-freebusy-set start CDATA #REQUIRED
                                     end   CDATA #REQUIRED>
         start value: an iCalendar "date with UTC time"
         end value: an iCalendar "date with UTC time"

   */
  private CalendarDataType calendarData;

  /** Constructor
   *
   * @param tag  QName name
   */
  public CalData(final QName tag) {
    super(tag, null);
  }

  /**
   * @return CalendarData
   */
  public CalendarDataType getCalendarData() {
    return calendarData;
  }

  /** The given node must be the Filter element
   *
   * @param nd XML node
   * @throws WebdavException on error
   */
  public void parse(final Node nd) throws WebdavException {
    /* Either empty - show everything or
              comp + optional (expand-recurrence-set or
                               limit-recurrence-set)
     */
    final NamedNodeMap nnm = nd.getAttributes();
    final CalendarDataType cd = new CalendarDataType();

    calendarData = cd;

    if (nnm != null) {
      for (int nnmi = 0; nnmi < nnm.getLength(); nnmi++) {
        final Node attr = nnm.item(nnmi);
        final String attrName = attr.getNodeName();

        switch (attrName) {
          case "content-type":
            cd.setContentType(attr.getNodeValue());
            if (cd.getContentType() == null) {
              throw new WebdavBadRequest();
            }
            break;
          case "xmlns":
            break;
          case "version":
            if (!"2.0".equals(attr.getNodeValue())) {
              // TODO - wrong response if not in a filter
              throw new WebdavForbidden(CaldavTags.validFilter,
                                        "Invalid attribute: " + attrName);
            }
            break;
          default:
            // Bad attribute(s)
            throw new WebdavForbidden(CaldavTags.validFilter,
                                      "Invalid attribute: " + attrName);
        }
      }
    }

    final Element[] children = getChildren(nd);

    try {
      for (final Element curnode : children) {
        if (debug()) {
          trace("calendar-data node type: " +
                        curnode.getNodeType() + " name:" +
                        curnode.getNodeName());
        }

        if (XmlUtil.nodeMatches(curnode, CaldavTags.comp)) {
          if (cd.getComp() != null) {
            throw new WebdavBadRequest();
          }

          cd.setComp(parseComp(curnode));
        } else if (XmlUtil.nodeMatches(curnode, CaldavTags.expand)) {
          if (cd.getExpand() != null) {
            throw new WebdavBadRequest();
          }

          cd.setExpand((ExpandType)ParseUtil.parseUTCTimeRange(new ExpandType(),
                                                           curnode, true));
        } else if (XmlUtil.nodeMatches(curnode, CaldavTags.limitRecurrenceSet)) {
          if (cd.getLimitRecurrenceSet() != null) {
            throw new WebdavBadRequest();
          }

          cd.setLimitRecurrenceSet((LimitRecurrenceSetType)ParseUtil.parseUTCTimeRange(new LimitRecurrenceSetType(),
                                                                curnode, true));
        } else if (XmlUtil.nodeMatches(curnode, CaldavTags.limitFreebusySet)) {
          if (cd.getLimitFreebusySet() != null) {
            throw new WebdavBadRequest();
          }

          cd.setLimitFreebusySet((LimitFreebusySetType)ParseUtil.parseUTCTimeRange(new LimitFreebusySetType(),
                                                              curnode, true));
        } else {
          throw new WebdavBadRequest();
        }
      }
    } catch (final WebdavBadRequest wbr) {
      throw wbr;
    } catch (final Throwable t) {
      throw new WebdavBadRequest();
    }

    if (debug()) {
      DumpUtil.dumpCalendarData(cd);
    }
  }

  /** Given the WebdavNsNode, returns the transformed content.
   *
   * @param wdnode the node
   * @param xml output
   * @param contentType - first element from content type or null
   * @throws WebdavException on error
   */
  public void process(final WebdavNsNode wdnode,
                      final XmlEmit xml,
                      final String contentType) throws WebdavException {
    if (!(wdnode instanceof CaldavComponentNode)) {
      return;
    }

    final CaldavComponentNode node = (CaldavComponentNode)wdnode;

    final CompType comp = getCalendarData().getComp();

    if (comp == null) {
      node.writeContent(xml, null, contentType);
      return;
    }

    /* Ensure node exists */
    node.init(true);
    if (!node.getExists()) {
      throw new WebdavException(HttpServletResponse.SC_NOT_FOUND);
    }

    // Top level must be VCALENDAR at this point?
    if (!"VCALENDAR".equals(comp.getName().toUpperCase())) {
      throw new WebdavBadRequest();
    }

    if (comp.getAllcomp() != null) {
      node.writeContent(xml, null, contentType);
      return;
    }

    // Assume all properties for that level.

    // Currently we only handle VEVENT -
    // If there's no VEVENT element what does that imply?

    for (final CompType subcomp : comp.getComp()) {
      final String nm = subcomp.getName().toUpperCase();

      if ("VEVENT".equals(nm) ||
              "VTODO".equals(nm)) {
        if (subcomp.getAllprop() != null) {
          node.writeContent(xml, null, contentType);
          return;
        }

        try {
          if ((contentType != null) &&
                  contentType.equals(XcalTags.mimetype)) {
            // XXX Just return the whole lot for the moment
            node.writeContent(xml, null, contentType);
          } else {
            xml.cdataValue(transformVevent(node.getIntf(),
                                           node.getIcal(),
                                           subcomp.getProp(),
                                           contentType));
          }
        } catch (final IOException ioe) {
          throw new WebdavException(ioe);
        }
        return;
      }
    }

    // No special instructions.

    node.writeContent(xml, null, contentType);
  }

  /* Transform one or more VEVENT objects based on a list of required
   * properties.
   */
  private String transformVevent(final SysIntf intf,
                                 final Calendar ical,
                                 final Collection<PropType> props,
                                 final String contentType)  throws WebdavException {
    try {
      final Calendar nical = new Calendar();
      final PropertyList pl = ical.getProperties();
      final PropertyList npl = nical.getProperties();

      // Add all vcalendar properties to new cal
      Iterator it = pl.iterator();

      while (it.hasNext()) {
        npl.add((Property)it.next());
      }

      final ComponentList cl = ical.getComponents();
      final ComponentList ncl = nical.getComponents();

      it = cl.iterator();

      while (it.hasNext()) {
        final Component c = (Component)it.next();

        if (!(c instanceof VEvent)) {
          ncl.add(c);
        } else {
          final VEvent v = new VEvent();
          
          // This adds a DTSTAMP

          final PropertyList vpl = c.getProperties();
          final PropertyList nvpl = v.getProperties();

          nvpl.clear();
          
          for (final PropType pr: props) {
            final Property p = vpl.getProperty(pr.getName());

            if (p != null) {
              nvpl.add(p);
            }
          }

          ncl.add(v);
        }
      }

      return intf.toIcalString(nical, contentType);
    } catch (final Throwable t) {
      if (debug()) {
        getLogger().error("transformVevent exception: ", t);
      }

      throw new WebdavBadRequest();
    }
  }

  /* ====================================================================
   *                   Private parsing methods
   * ==================================================================== */

  private CompType parseComp(final Node nd) throws WebdavException {
    /* Either allcomp + (either allprop or 0 or more prop) or
              0 or more comp + (either allprop or 0 or more prop)
     */
    final String name = getOnlyAttrVal(nd, "name");
    if (name == null) {
      throw new WebdavBadRequest();
    }

    final CompType c = new CompType();
    c.setName(name);

    final Element[] children = getChildren(nd);

    boolean hadComps = false;
    boolean hadProps = false;

    for (final Element curnode : children) {
      if (debug()) {
        trace("comp node type: " +
                      curnode.getNodeType() + " name:" +
                      curnode.getNodeName());
      }

      if (XmlUtil.nodeMatches(curnode, CaldavTags.allcomp)) {
        if (hadComps) {
          throw new WebdavBadRequest();
        }

        c.setAllcomp(new AllcompType());
      } else if (XmlUtil.nodeMatches(curnode, CaldavTags.comp)) {
        if (c.getAllcomp() != null) {
          throw new WebdavBadRequest();
        }

        c.getComp().add(parseComp(curnode));
        hadComps = true;
      } else if (XmlUtil.nodeMatches(curnode, CaldavTags.allprop)) {
        if (hadProps) {
          throw new WebdavBadRequest();
        }

        c.setAllprop(new AllpropType());
      } else if (XmlUtil.nodeMatches(curnode, CaldavTags.prop)) {
        if (c.getAllprop() != null) {
          throw new WebdavBadRequest();
        }

        c.getProp().add(parseProp(curnode));
        hadProps = true;
      } else {
        throw new WebdavBadRequest();
      }
    }

    return c;
  }

  private PropType parseProp(final Node nd) throws WebdavException {
    final NamedNodeMap nnm = nd.getAttributes();

    if ((nnm == null) || (nnm.getLength() == 0)) {
      throw new WebdavBadRequest();
    }

    final String name = XmlUtil.getAttrVal(nnm, "name");
    if (name == null) {
      throw new WebdavBadRequest();
    }

    final Boolean val;

    try {
      val = XmlUtil.getYesNoAttrVal(nnm, "novalue");
    } catch (final Throwable t) {
      throw new WebdavBadRequest();
    }

    final PropType pr = new PropType();
    pr.setName(name);

    if ((val != null) && val) {
      pr.setNovalue("yes");
    }

    return pr;
  }

  private Element[] getChildren(final Node nd) throws WebdavException {
    try {
      return XmlUtil.getElementsArray(nd);
    } catch (final Throwable t) {
      if (debug()) {
        getLogger().error("<filter>: parse exception: ", t);
      }

      throw new WebdavBadRequest();
    }
  }

  /** Fetch required attribute. Return null for error
   *
   * @param nd XML node
   * @param name of attribute
   * @return String
   * @throws WebdavException on error
   */
  private String getOnlyAttrVal(final Node nd, 
                                final String name) throws WebdavException {
    final NamedNodeMap nnm = nd.getAttributes();

    if ((nnm == null) || (nnm.getLength() != 1)) {
      throw new WebdavBadRequest();
    }

    final String res = XmlUtil.getAttrVal(nnm, name);
    if (res == null) {
      throw new WebdavBadRequest();
    }

    return res;
  }

  /* ====================================================================
   *                   Logged methods
   * ==================================================================== */

  private BwLogger logger = new BwLogger();

  @Override
  public BwLogger getLogger() {
    if ((logger.getLoggedClass() == null) && (logger.getLoggedName() == null)) {
      logger.setLoggedClass(getClass());
    }

    return logger;
  }
}

