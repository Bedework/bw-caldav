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

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/** Class to parse and process query filters.
 *
 *   @author Mike Douglass   douglm rpi.edu
 */
public class Filters {
  /** Convenience method
   *
   * @param cf
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
   * @param tm
   * @return boolean true if this element matches all of the
   *                  named component types.
   */
  public static boolean caseless(final TextMatchType tm) {
    return tm.getCollation().equals("i;ascii-casemap");
  }

  /** Given a caldav like xml filter parse it
   *
   * @param xmlStr
   * @return Filter
   * @throws WebdavException
   */
  public static FilterType parse(final String xmlStr) throws WebdavException {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);

      DocumentBuilder builder = factory.newDocumentBuilder();

      Document doc = builder.parse(new InputSource(new StringReader(xmlStr)));

      return parse(doc.getDocumentElement());
    } catch (WebdavException cfe) {
      throw cfe;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** The given node must be the Filter element
   *
   * @param nd
   * @return Filter
   * @throws WebdavException
   */
  public static FilterType parse(final Node nd) throws WebdavException {
    try {
      JAXBContext jc = JAXBContext.newInstance("ietf.params.xml.ns.caldav");
      Unmarshaller u = jc.createUnmarshaller();

      JAXBElement jel = (JAXBElement)u.unmarshal(nd);
      if (jel == null) {
        return null;
      }

      return (FilterType)jel.getValue();
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** Return an object encapsulating the filter query.
   *
   * @param f
   * @return EventQuery
   * @throws WebdavException
   */
  public static EventQuery getQuery(final FilterType f) throws WebdavException {
    EventQuery eventq = new EventQuery();

    eventq.filter = getQueryFilter(f.getCompFilter(), eventq, 0);

    return eventq;
  }

  /** Returns a subtree of the filter used in querying
   *
   * @param cf
   * @param eq - so we can update time range
   * @param exprDepth - allows us to do validity checks
   * @return Filter - null for no filtering
   * @throws WebdavException
   */
  public static FilterBase getQueryFilter(final CompFilterType cf,
                                          final EventQuery eq,
                                          final int exprDepth) throws WebdavException {
    FilterBase filter = null;
    int entityType = IcalDefs.entityTypeEvent;

    boolean isNotDefined = cf.getIsNotDefined() != null;
    boolean andThem = "allof".equals(cf.getTest());
    String name = cf.getName().toUpperCase();

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

      filter = makeFilter(name, isNotDefined, matchAll(cf),
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
        EntityTimeRangeFilter etrf = new EntityTimeRangeFilter(null,
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
      for (CompFilterType subcf: cf.getCompFilter()) {
        FilterBase subqf = getQueryFilter(subcf,
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

  private static TimeRange makeTimeRange(final UTCTimeRangeType utr) throws WebdavException {
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
        throw new WebdavBadRequest(CaldavTags.validFilter, "Invalid time-range - no start and no end");
      }

      return new TimeRange(st, et);
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavBadRequest(CaldavTags.validFilter, "Invalid time-range");
    }
  }

  private static FilterBase processPropFilters(final CompFilterType cf,
                                    final EventQuery eq,
                                    final int entityType) throws WebdavException {
    if (Util.isEmpty(cf.getPropFilter())) {
      return null;
    }

    FilterBase pfilters = null;
    boolean andThem = "allof".equals(cf.getTest());

    for (PropFilterType pf: cf.getPropFilter()) {
      String pname = pf.getName();

      UTCTimeRangeType utr = pf.getTimeRange();
      TextMatchType tm = pf.getTextMatch();
      boolean isNotDefined = pf.getIsNotDefined() != null;
      boolean testPresent = !isNotDefined && (utr == null) && (tm == null) &&
                            (Util.isEmpty(pf.getParamFilter()));
      TimeRange tr = null;
      if (utr != null) {
        tr = makeTimeRange(utr);
      }
      boolean andParams = "allof".equals(pf.getTest());

      FilterBase filter = makeFilter(pname, isNotDefined, testPresent,
                                 tr,
                                 tm, andParams, pf.getParamFilter());

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

        /** Add the propfilter to the post filter collection
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
                                       final boolean testNotDefined,
                                       final boolean testPresent,
                                       final TimeRange timeRange,
                                       final TextMatchType match,
                                       final boolean andParamFilters,
                            final Collection<ParamFilterType> paramFilters) throws WebdavException {
    FilterBase filter = null;

    PropertyInfoIndex pi = PropertyInfoIndex.fromName(pname);

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
      filter = ObjectFilter.makeFilter(null, pi, timeRange);
    } else if (match != null) {
      ObjectFilter<String> f = new ObjectFilter<String>(null, pi);
      f.setEntity(match.getValue());
      f.setExact(false);

      boolean caseless = match.getCollation().equals("i;ascii-casemap");
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
                                     final Collection<ParamFilterType> paramFilters) throws WebdavException {
    FilterBase parfilters = null;

    for (ParamFilterType pf: paramFilters) {
      TextMatchType tm = pf.getTextMatch();
      boolean isNotDefined = pf.getIsNotDefined() != null;
      boolean testPresent = isNotDefined && (tm == null);

      PropertyFilter filter = (PropertyFilter)makeFilter(pf.getName(),
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
      pfs = new ArrayList<PropFilterType>();
    }

    pfs.add(val);

    return pfs;
  }
}

