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
package org.bedework.caldav.util.filter.parse;

import org.bedework.caldav.util.TimeRange;
import org.bedework.caldav.util.filter.EntityTimeRangeFilter;
import org.bedework.caldav.util.filter.EntityTypeFilter;
import org.bedework.caldav.util.filter.FilterBase;
import org.bedework.caldav.util.filter.ObjectFilter;
import org.bedework.caldav.util.filter.PresenceFilter;
import org.bedework.caldav.util.filter.PropertyFilter;
import org.bedework.util.calendar.IcalDefs;
import org.bedework.util.calendar.PropertyIndex.PropertyInfoIndex;
import org.bedework.util.calendar.XcalUtil;
import org.bedework.util.misc.Util;
import org.bedework.util.xml.tagdefs.CaldavTags;
import org.bedework.webdav.servlet.shared.WebdavBadRequest;
import org.bedework.webdav.servlet.shared.WebdavException;
import org.bedework.webdav.servlet.shared.WebdavForbidden;

import ietf.params.xml.ns.caldav.CompFilterType;
import ietf.params.xml.ns.caldav.FilterType;
import ietf.params.xml.ns.caldav.ParamFilterType;
import ietf.params.xml.ns.caldav.PropFilterType;
import ietf.params.xml.ns.caldav.TextMatchType;
import ietf.params.xml.ns.caldav.UTCTimeRangeType;
import net.fortuna.ical4j.model.DateTime;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/** Class to parse and process query filters.
 *
 *   @author Mike Douglass   douglm rpi.edu
 */
public class Filters {
  /** Convenience method
   *
   * @param cf comp filter
   * @return boolean true if this element matches all of the
   *                  named component types.
   */
  public static boolean matchAll(final CompFilterType cf) {
    return (cf.getTimeRange() == null)  &&
            Util.isEmpty(cf.getCompFilter()) &&
            Util.isEmpty(cf.getPropFilter());
  }

  /** Convenience method
   *
   * @param tm text match
   * @return boolean true if this element matches all of the
   *                  named component types.
   */
  public static boolean caseless(final TextMatchType tm) {
    return tm.getCollation().equals("i;ascii-casemap");
  }

  /** Given a caldav like xml filter parse it
   *
   * @param xmlStr xml filter
   * @return Filter
   */
  public static FilterType parse(final String xmlStr) {
    try {
      final DocumentBuilderFactory factory =
              DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);

      final DocumentBuilder builder = factory.newDocumentBuilder();

      final Document doc =
              builder.parse(new InputSource(new StringReader(xmlStr)));

      return parse(doc.getDocumentElement());
    } catch (final WebdavException cfe) {
      throw cfe;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** The given node must be the Filter element
   *
   * @param nd node
   * @return Filter
   */
  public static FilterType parse(final Node nd) {
    try {
      final JAXBContext jc = JAXBContext.newInstance("ietf.params.xml.ns.caldav");
      final Unmarshaller u = jc.createUnmarshaller();

      final JAXBElement<?> jel = (JAXBElement<?>)u.unmarshal(nd);
      if (jel == null) {
        return null;
      }

      return (FilterType)jel.getValue();
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** Return an object encapsulating the filter query.
   *
   * @param f filter
   * @return EventQuery
   */
  public static EventQuery getQuery(final FilterType f) {
    final EventQuery eventq = new EventQuery();

    eventq.filter = getQueryFilter(f.getCompFilter(), eventq, 0);

    return eventq;
  }

  /** Returns a subtree of the filter used in querying
   *
   * @param cf comp filter
   * @param eq - so we can update time range
   * @param exprDepth - allows us to do validity checks
   * @return Filter - null for no filtering
   */
  public static FilterBase getQueryFilter(final CompFilterType cf,
                                          final EventQuery eq,
                                          final int exprDepth) {
    FilterBase filter = null;
    int entityType = IcalDefs.entityTypeEvent;

    final boolean isNotDefined = cf.getIsNotDefined() != null;
    final boolean andThem = "allof".equals(cf.getTest());
    final String name = cf.getName().toUpperCase();

    if (exprDepth == 0) {
      if (!"VCALENDAR".equals(name)) {
        throw new WebdavBadRequest();
      }

//      if (Util.isEmpty(compFilters)) {
//        return null;
//      }
    } else if (exprDepth == 1) {
      // Calendar components only
      filter = EntityTypeFilter.makeIcalEntityTypeFilter(null, name, false);

      entityType = ((EntityTypeFilter)filter).getEntity();
    } else if (exprDepth == 2) {
      // Sub-components only

      // XXX
      entityType = IcalDefs.entityTypeAlarm;

      filter = makeFilter(name, entityType, isNotDefined, matchAll(cf),
                          makeTimeRange(cf.getTimeRange()), null, false,
                          null);

      if (filter == null) {
        throw new WebdavBadRequest();
      }
    } else {
      throw new WebdavBadRequest("expr too deep");
    }

    if ((filter != null) && isNotDefined) {
      filter.setNot(true);
    }

    if (matchAll(cf)) {
      return filter;
    }

    if (exprDepth < 2) {
      /* XXX This is wrong - if filters handle time ranges OK we should remove
       * this merge which was here so post-processing could handle it.
       */
      if (cf.getTimeRange() != null) {
/*        if (eq.trange == null) {
          eq.trange = timeRange;
        } else {
          eq.trange.merge(timeRange);
        } */
        final EntityTimeRangeFilter etrf = 
                new EntityTimeRangeFilter(null,
                                          entityType,
                                          makeTimeRange(cf.getTimeRange()));
        filter = FilterBase.addAndChild(filter, etrf);
      }
    }

    if (exprDepth > 0) {
      /* We are at a component level, event, todo etc.
       * If there are property filters turn this into an and of the current
       * filter with the or'd prop filters
       */
      filter = FilterBase.addAndChild(filter, processPropFilters(cf, eq, entityType));
    }

    if (!Util.isEmpty(cf.getCompFilter())) {
      FilterBase cfilters = null;
      for (final CompFilterType subcf: cf.getCompFilter()) {
        final FilterBase subqf = getQueryFilter(subcf,
                                                eq, exprDepth + 1);
        if (andThem) {
          cfilters = FilterBase.addAndChild(cfilters, subqf);
        } else {
          cfilters = FilterBase.addOrChild(cfilters, subqf);
        }
      }

      filter = FilterBase.addAndChild(filter, cfilters);
    }

    return filter;
  }

  private static TimeRange makeTimeRange(final UTCTimeRangeType utr) {
    if (utr == null) {
      return null;
    }

    try {
      DateTime st = null;
      DateTime et = null;

      if (utr.getStart() != null) {
        st = new DateTime(XcalUtil.getIcalFormatDateTime(utr.getStart()));
      }

      if (utr.getEnd() != null) {
        et = new DateTime(XcalUtil.getIcalFormatDateTime(utr.getEnd()));
      }

      if ((st == null) && (et == null)) {
        throw new WebdavForbidden(CaldavTags.validFilter, "Invalid time-range - no start and no end");
      }

      if ((st != null) && !st.isUtc()) {
        throw new WebdavForbidden(CaldavTags.validFilter, "Invalid time-range - start not UTC");
      }

      if ((et != null) && !et.isUtc()) {
        throw new WebdavForbidden(CaldavTags.validFilter, "Invalid time-range - end not UTC");
      }

      return new TimeRange(st, et);
    } catch (final WebdavException wde) {
      throw wde;
    } catch (final Throwable t) {
      throw new WebdavForbidden(CaldavTags.validFilter, "Invalid time-range");
    }
  }

  private static FilterBase processPropFilters(final CompFilterType cf,
                                    final EventQuery eq,
                                    final int entityType) {
    if (Util.isEmpty(cf.getPropFilter())) {
      return null;
    }

    FilterBase pfilters = null;
    final boolean andThem = "allof" .equals(cf.getTest());

    for (final PropFilterType pf: cf.getPropFilter()) {
      final String pname = pf.getName();

      final UTCTimeRangeType utr = pf.getTimeRange();
      final TextMatchType tm = pf.getTextMatch();
      final boolean isNotDefined = pf.getIsNotDefined() != null;
      final boolean testPresent =
              !isNotDefined && (utr == null) && (tm == null) &&
                      (Util.isEmpty(pf.getParamFilter()));
      TimeRange tr = null;
      if (utr != null) {
        tr = makeTimeRange(utr);
      }
      final boolean andParams = "allof" .equals(pf.getTest());

      final FilterBase filter = makeFilter(pname,
                                           -1,
                                           isNotDefined,
                                           testPresent,
                                           tr,
                                           tm, andParams,
                                           pf.getParamFilter());

      if (filter != null) {
        if (andThem) {
          pfilters = FilterBase.addAndChild(pfilters, filter);
        } else {
          pfilters = FilterBase.addOrChild(pfilters, filter);
        }
      } else {
        eq.postFilter = true;

        // XXX This is wrong - if we postfilter we have to postfilter everything
        // XXX because it's an OR

        /* Add the propfilter to the post filter collection
         */
        if (entityType == IcalDefs.entityTypeEvent) {
          eq.eventFilters = addPropFilter(eq.eventFilters, pf);
        } else if (entityType == IcalDefs.entityTypeTodo) {
          eq.todoFilters = addPropFilter(eq.todoFilters, pf);
        } else if (entityType == IcalDefs.entityTypeJournal) {
          eq.journalFilters = addPropFilter(eq.journalFilters, pf);
        } else if (entityType == IcalDefs.entityTypeAlarm) {
          eq.alarmFilters = addPropFilter(eq.alarmFilters, pf);
        }
      }
    }

    return pfilters;
  }

  private static FilterBase makeFilter(final String pname,
                                       final int entityType, 
                                       final boolean testNotDefined,
                                       final boolean testPresent,
                                       final TimeRange timeRange,
                                       final TextMatchType match,
                                       final boolean andParamFilters,
                            final Collection<ParamFilterType> paramFilters) {
    FilterBase filter = null;

    final PropertyInfoIndex pi = PropertyInfoIndex.fromName(pname);

    if (pi == null) {
      // Unknown property
      throw new WebdavForbidden(CaldavTags.supportedFilter,
                                "Unknown property " + pname);
    }

    if (testNotDefined) {
      filter = new PresenceFilter(null, pi, false);
    } else if (testPresent) {
      // Presence check
      filter = new PresenceFilter(null, pi, true);
    } else if (timeRange != null) {
      if (entityType < 0) {
        filter = ObjectFilter.makeFilter(null, pi, timeRange, null, null);
      } else {
        filter = new EntityTimeRangeFilter(null, entityType,
                                           timeRange);
      }
    } else if (match != null) {
      final ObjectFilter<String> f = new ObjectFilter<String>(null, pi);
      f.setEntity(match.getValue());
      f.setExact(false);

      final boolean caseless = 
              match.getCollation().equals("i;ascii-casemap") &&
              pi != PropertyInfoIndex.UID;
      f.setCaseless(caseless);
      f.setNot(match.getNegateCondition().equals("yes"));
      filter = f;
    } else {
      // Must have param filters
      if (Util.isEmpty(paramFilters)) {
        throw new WebdavBadRequest();
      }
    }

    if (Util.isEmpty(paramFilters)) {
      return filter;
    }

    return FilterBase.addAndChild(filter, processParamFilters(pi,
                                                              andParamFilters,
                                                              paramFilters));
  }

  private static FilterBase processParamFilters(final PropertyInfoIndex parentIndex,
                                                final boolean andThem,
                                     final Collection<ParamFilterType> paramFilters) {
    FilterBase parfilters = null;

    for (final ParamFilterType pf: paramFilters) {
      final TextMatchType tm = pf.getTextMatch();
      final boolean isNotDefined = pf.getIsNotDefined() != null;
      final boolean testPresent = isNotDefined && (tm == null);

      final PropertyFilter filter = 
              (PropertyFilter)makeFilter(pf.getName(),
                                         -1,
                                         isNotDefined,
                                         testPresent,
                                         null, tm, false, null);

      if (filter != null) {
        filter.setParentPropertyIndex(parentIndex);
        if (andThem) {
          parfilters = FilterBase.addAndChild(parfilters, filter);
        } else {
          parfilters = FilterBase.addOrChild(parfilters, filter);
        }
      }
    }

    return parfilters;
  }

  private static List<PropFilterType> addPropFilter(List<PropFilterType> pfs,
                                         final PropFilterType val) {
    if (pfs == null) {
      pfs = new ArrayList<>();
    }

    pfs.add(val);

    return pfs;
  }
}

