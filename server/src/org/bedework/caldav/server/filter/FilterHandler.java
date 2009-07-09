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
package org.bedework.caldav.server.filter;

import org.bedework.caldav.server.CalDAVCollection;
import org.bedework.caldav.server.CalDAVEvent;
import org.bedework.caldav.server.CaldavBwNode;
import org.bedework.caldav.server.CaldavComponentNode;
import org.bedework.caldav.server.sysinterface.RetrievalMode;
import org.bedework.caldav.util.filter.parse.CompFilter;
import org.bedework.caldav.util.filter.parse.EventQuery;
import org.bedework.caldav.util.filter.parse.PropFilter;
import org.bedework.caldav.util.filter.parse.QueryFilter;

import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavForbidden;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;
import edu.rpi.cct.webdav.servlet.common.WebdavUtils;
import edu.rpi.cmt.calendar.IcalDefs;

import java.util.ArrayList;
import java.util.Collection;
import javax.servlet.http.HttpServletResponse;

import net.fortuna.ical4j.model.Component;

/** Class to represent a calendar-query filter
 *  <pre>
 *   <!ELEMENT filter comp-filter>
 *
 *   10.3.1 CALDAV:comp-filter XML Element
 *
 *    Name:
 *        comp-filter
 *    Namespace:
 *        urn:ietf:params:xml:ns:caldav
 *    Purpose:
 *        Limits the search to only the chosen component types.
 *    Description:
 *        The "name" attribute is an iCalendar component type (e.g., "VEVENT").
 *        When this element is present, the server should only return a component
 *        if it matches the filter, which is to say:
 *
 *       A component of the type specified by the "name" attribute
 *       exists, and the CALDAV:comp-filter is empty, OR
 *
 *       it contains at least one recurrence instance scheduled to overlap a
 *       given time range if a CALDAV:time-range XML element is specified, and
 *       that any CALDAV:prop-filter and CALDAV:comp-filter child elements
 *       also match.
 *
 *
 *    <!ELEMENT comp-filter (is-not-defined | (time-range?,
 *                              prop-filter*, comp-filter*))>
 *
 *    <!ATTLIST comp-filter name CDATA #REQUIRED>
 *
 *    10.3.2 CALDAV:prop-filter XML Element
 *
 *    Name:
 *        prop-filter
 *    Namespace:
 *        urn:ietf:params:xml:ns:caldav
 *    Purpose:
 *        Limits the search to specific properties.
 *    Description:
 *        The "name" attribute MUST contain an iCalendar property name
 *        (e.g., "ATTENDEE"). When the 'prop-filter' executes, a property matches if:
 *
 *       A property of the type specified by the "name" attribute
 *       exists, and the CALDAV:prop-filter is empty, OR it matches the
 *       CALDAV:time-range XML element or CALDAV:text-match conditions
 *       if specified, and that any CALDAV:param-filter child elements
 *       also match.
 *
 *    or:
 *
 *       A property of the type specified by the "name" attribute does
 *       not exist, and the CALDAV:is-not-defined element is specified.
 *
 *    <!ELEMENT prop-filter ((is-not-defined |
 *                              ((time-range | text-match)?,
 *                               param-filter*))>
 *
 *    <!ATTLIST prop-filter name CDATA #REQUIRED>
 *
 *    10.3.3 CALDAV:param-filter XML Element
 *
 *    Name:
 *        param-filter
 *    Namespace:
 *        urn:ietf:params:xml:ns:caldav
 *    Purpose:
 *        Limits the search to specific parameters.
 *    Description:
 *        The "param-filter" element limits the search result to the set of resources containing properties with parameters that meet the parameter filter rules. When this filter executes, a parameter matches if:
 *
 *    ("is-defined matches" OR "text-match matches")
 *
 *    <!ELEMENT param-filter (is-defined | text-match) >
 *
 *    <!ATTLIST param-filter name CDATA #REQUIRED>
 *
 * </pre>
 *
 *   @author Mike Douglass   douglm @ rpi.edu
 */
public class FilterHandler extends QueryFilter {
  /* Query we executed */
  private EventQuery eventq;

  /** Constructor
   *
   * @param debug
   */
  public FilterHandler(boolean debug) {
    super(debug);
  }

  /** Use the given query to return a collection of nodes. An exception will
   * be raised if the entire query fails for some reason (access, etc). An
   * empty collection will be returned if no objects match.
   *
   * @param wdnode    WebdavNsNode defining root of search
   * @param retrieveRecur  How we retrieve recurring events
   * @return Collection of event objects (null or empty for no result)
   * @throws WebdavException
   */
  public Collection<CalDAVEvent> query(CaldavBwNode wdnode,
                                       RetrievalMode retrieveRecur) throws WebdavException {
    try {
      eventq = getQuery();

      /*if (debug) {
      if (eventq.trange == null) {
        trace("No time-range specified for uri " + wdnode.getUri());
      } else {
        trace("time-range specified for uri " + wdnode.getUri() +
              " with start=" + eventq.trange.getStart() +
              " end=" + eventq.trange.getEnd());
      }
    }*/

      CalDAVCollection c = (CalDAVCollection)wdnode.getCollection(true);
      if (c == null) {
        return null;
      }

      Collection<CalDAVEvent> events = wdnode.getSysi().getEvents(c,
                                                                  eventq.filter,
                                                                  retrieveRecur);

      if (debug) {
        trace("Query returned " + events.size());
      }

      return events;
    } catch (WebdavForbidden cf) {
      throw cf;
    } catch (Throwable t) {
      error(t);
      throw new WebdavException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  /** Carry out any postfiltering on the collection of initialised nodes,
   *
   * @param nodes     Collection of initialised CaldavBwNode
   * @return Collection of filtered nodes (empty for no result)
   * @throws WebdavException
   */
  public Collection<WebdavNsNode> postFilter(
                   Collection<WebdavNsNode> nodes) throws WebdavException {
    if (!eventq.postFilter) {
      return nodes;
    }

    if (debug) {
      trace("post filtering needed");
    }

    CompFilter cfltr = filter;

    // Currently only handle VCALENDAR for top level.
    if (!"VCALENDAR".equals(cfltr.getName())) {
      return new ArrayList<WebdavNsNode>();
    }

    ArrayList<WebdavNsNode> filtered = new ArrayList<WebdavNsNode>();

    for (WebdavNsNode node: nodes) {
      CaldavComponentNode curnode = null;

      if (!(node instanceof CaldavComponentNode)) {
        // Cannot match to anything - don't pass it?
      } else {
        curnode = (CaldavComponentNode)node;

        int entityType = curnode.getEvent().getEntityType();

        Collection<PropFilter> pfs = null;

        if (entityType == IcalDefs.entityTypeEvent) {
          pfs = eventq.eventFilters;
        } else if (entityType == IcalDefs.entityTypeTodo) {
          pfs = eventq.todoFilters;
        } else if (entityType == IcalDefs.entityTypeJournal) {
          pfs = eventq.journalFilters;
        }

        if (!WebdavUtils.emptyCollection(pfs)) {
          Component comp = curnode.getComponent();

          for (PropFilter pf: pfs) {
            if (pf.filter(comp)) {
              filtered.add(curnode);
              break;
            }
          }
        }
      }
    }

    return filtered;
  }
}
