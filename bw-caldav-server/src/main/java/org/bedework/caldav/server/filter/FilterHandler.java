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
package org.bedework.caldav.server.filter;

import org.bedework.caldav.server.CalDAVCollection;
import org.bedework.caldav.server.CalDAVEvent;
import org.bedework.caldav.server.CaldavBwNode;
import org.bedework.caldav.server.CaldavComponentNode;
import org.bedework.caldav.server.sysinterface.RetrievalMode;
import org.bedework.caldav.util.filter.FilterUtil;
import org.bedework.caldav.util.filter.parse.EventQuery;
import org.bedework.caldav.util.filter.parse.Filters;
import org.bedework.util.calendar.IcalDefs;
import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;
import org.bedework.webdav.servlet.common.WebdavUtils;
import org.bedework.webdav.servlet.shared.WebdavBadRequest;
import org.bedework.webdav.servlet.shared.WebdavException;
import org.bedework.webdav.servlet.shared.WebdavForbidden;
import org.bedework.webdav.servlet.shared.WebdavNsNode;

import ietf.params.xml.ns.caldav.CompFilterType;
import ietf.params.xml.ns.caldav.FilterType;
import ietf.params.xml.ns.caldav.PropFilterType;
import net.fortuna.ical4j.model.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

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
public class FilterHandler implements Logged {
  /* Query we executed */
  private EventQuery eventq;

  private final FilterType f;

  /** Constructor
   *
   * @param f a filter
   */
  public FilterHandler(final FilterType f) {
    this.f = f;
  }

  /** Use the given query to return a collection of nodes. An exception will
   * be raised if the entire query fails for some reason (access, etc). An
   * empty collection will be returned if no objects match.
   *
   * @param wdnode    WebdavNsNode defining root of search
   * @param retrieveList   If non-null limit required fields.
   * @param retrieveRecur  How we retrieve recurring events
   * @return Collection of event objects (null or empty for no result)
   */
  public Collection<CalDAVEvent<?>> query(final CaldavBwNode wdnode,
                                          final List<String> retrieveList,
                                          final RetrievalMode retrieveRecur) {
    try {
      eventq = Filters.getQuery(f);

      /*if (debug()) {
      if (eventq.trange == null) {
        trace("No time-range specified for uri " + wdnode.getUri());
      } else {
        trace("time-range specified for uri " + wdnode.getUri() +
              " with start=" + eventq.trange.getStart() +
              " end=" + eventq.trange.getEnd());
      }
    }*/

      final CalDAVCollection<?> c =
              (CalDAVCollection<?>)wdnode.getCollection(false);
      if (c == null) {
        return null;
      }

      final Collection<CalDAVEvent<?>> events =
              wdnode.getSysi().getEvents(c,
                                         eventq.filter,
                                         retrieveList,
                                         retrieveRecur);

      if (debug()) {
        trace("Query returned " + events.size());
      }

      return events;
    } catch (final WebdavBadRequest | WebdavForbidden wd) {
      throw wd;
    } catch (final Throwable t) {
      throw new WebdavException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  /** Carry out any postfiltering on the collection of initialised nodes,
   *
   * @param nodes     Collection of initialised CaldavBwNode
   * @return Collection of filtered nodes (empty for no result)
   */
  public Collection<WebdavNsNode> postFilter(
                   final Collection<WebdavNsNode> nodes) {
    if (!eventq.postFilter) {
      return nodes;
    }

    if (debug()) {
      trace("post filtering needed");
    }

    final CompFilterType cfltr = f.getCompFilter();

    // Currently only handle VCALENDAR for top level.
    if (!"VCALENDAR".equals(cfltr.getName())) {
      return new ArrayList<>();
    }

    final ArrayList<WebdavNsNode> filtered = new ArrayList<>();

    for (final WebdavNsNode node: nodes) {
      final CaldavComponentNode curnode;

      if (!(node instanceof CaldavComponentNode)) {
        // Cannot match to anything - don't pass it?
        continue;
      }

      curnode = (CaldavComponentNode)node;

      final int entityType = curnode.getEvent().getEntityType();

      Collection<PropFilterType> pfs = null;

      if (entityType == IcalDefs.entityTypeEvent) {
        pfs = eventq.eventFilters;
      } else if (entityType == IcalDefs.entityTypeTodo) {
        pfs = eventq.todoFilters;
      } else if (entityType == IcalDefs.entityTypeJournal) {
        pfs = eventq.journalFilters;
      }

      if (!WebdavUtils.emptyCollection(pfs)) {
        final Component comp = curnode.getComponent();

        for (final PropFilterType pf: pfs) {
          if (FilterUtil.filter(pf, comp)) {
            filtered.add(curnode);
            break;
          }
        }
      }
    }

    return filtered;
  }

  /* ====================================================================
   *                   Logged methods
   * ==================================================================== */

  private final BwLogger logger = new BwLogger();

  @Override
  public BwLogger getLogger() {
    if ((logger.getLoggedClass() == null) && (logger.getLoggedName() == null)) {
      logger.setLoggedClass(getClass());
    }

    return logger;
  }
}
