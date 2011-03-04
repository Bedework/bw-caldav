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
import org.bedework.caldav.util.filter.Filter;
import org.bedework.caldav.util.filter.ObjectFilter;
import org.bedework.caldav.util.filter.PresenceFilter;
import org.bedework.caldav.util.filter.PropertyFilter;

import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavForbidden;
import edu.rpi.cmt.calendar.IcalDefs;
import edu.rpi.cmt.calendar.PropertyIndex.PropertyInfoIndex;
import edu.rpi.sss.util.Util;
import edu.rpi.sss.util.xml.tagdefs.CaldavTags;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;

/** Class to represent a calendar-query comp-filter
 *
 *   @author Mike Douglass   douglm@rpi.edu
 */
public class CompFilter {
  /* Name of component VEVENT etc */
  private String name;

  private boolean isNotDefined;

  private TimeRange timeRange;

  private Collection<CompFilter> compFilters;

  private Collection<PropFilter> propFilters;

  /** Constructor
   * @param name
   */
  public CompFilter(final String name) {
    this.name = name;
  }

  /**
   * @param val
   */
  public void setName(final String val) {
    name = val;
  }

  /**
   * @return String name
   */
  public String getName() {
    return name;
  }

  /**
   * @param val
   */
  public void setIsNotDefined(final boolean val) {
    isNotDefined = val;
  }

  /**
   * @return boolean isNotDefined value
   */
  public boolean getIsNotDefined() {
    return isNotDefined;
  }

  /**
   * @param val
   */
  public void setTimeRange(final TimeRange val) {
    timeRange = val;
  }

  /**
   * @return TimeRange for filter
   */
  public TimeRange getTimeRange() {
    return timeRange;
  }

  /**
   * @return Collection of comp filters
   */
  public Collection<CompFilter> getCompFilters() {
    if (compFilters == null) {
      compFilters = new ArrayList<CompFilter>();
    }

    return compFilters;
  }

  /**
   * @return boolean true if we have comp filters
   */
  public boolean hasCompFilters() {
    return ((compFilters == null) || (compFilters.size() != 0));
  }

  /**
   * @param cf
   */
  public void addCompFilter(final CompFilter cf) {
    getCompFilters().add(cf);
  }

  /**
   * @return Collection of prop filter
   */
  public Collection<PropFilter> getPropFilters() {
    if (propFilters == null) {
      propFilters = new ArrayList<PropFilter>();
    }

    return propFilters;
  }

  /**
   * @return boolean true if we have prop filters
   */
  public boolean hasPropFilters() {
    return ((propFilters == null) || (propFilters.size() != 0));
  }

  /**
   * @param pf
   */
  public void addPropFilter(final PropFilter pf) {
    getPropFilters().add(pf);
  }

  /** Convenience method
   *
   * @return boolean true ifthis element matches all of the
   *                  named component types.
   */
  public boolean matchAll() {
    return (timeRange == null)  &&
            Util.isEmpty(compFilters) &&
            Util.isEmpty(propFilters);
  }

  /** Returns a subtree of the filter used in querying
   *
   * @param eq - so we can update time range
   * @param exprDepth - allows us to do validity checks
   * @return BwFilter - null for no filtering
   * @throws WebdavException
   */
  public Filter getQueryFilter(final EventQuery eq,
                                 final int exprDepth) throws WebdavException {
    Filter filter = null;
    int entityType = IcalDefs.entityTypeEvent;

    if (exprDepth == 0) {
      if (!"VCALENDAR".equals(getName())) {
        throw new WebdavBadRequest();
      }

//      if (Util.isEmpty(compFilters)) {
//        return null;
//      }
    } else if (exprDepth == 1) {
      // Calendar components only

      if ("VEVENT".equals(getName())) {
        filter = EntityTypeFilter.eventFilter(null, isNotDefined);
        entityType = IcalDefs.entityTypeEvent;
      }

      if ("VTODO".equals(getName())) {
        filter = EntityTypeFilter.todoFilter(null, isNotDefined);
        entityType = IcalDefs.entityTypeTodo;
      }

      if ("VJOURNAL".equals(getName())) {
        filter = EntityTypeFilter.journalFilter(null, isNotDefined);
        entityType = IcalDefs.entityTypeJournal;
      }

      if ("VFREEBUSY".equals(getName())) {
        filter = EntityTypeFilter.freebusyFilter(null, isNotDefined);
        entityType = IcalDefs.entityTypeFreeAndBusy;
      }

      if (filter == null) {
        throw new WebdavBadRequest();
      }
    } else if (exprDepth == 2) {
      // Sub-components only

      // XXX
      entityType = IcalDefs.entityTypeAlarm;

      filter = makeFilter(getName(), isNotDefined, matchAll(), timeRange, null,
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

    if (matchAll()) {
      return filter;
    }

    if (exprDepth < 2) {
      /* XXX This is wrong - if filters handle time ranges OK we should remove
       * this merge which was here so post-processing could handle it.
       */
      if (timeRange != null) {
/*        if (eq.trange == null) {
          eq.trange = timeRange;
        } else {
          eq.trange.merge(timeRange);
        } */
        EntityTimeRangeFilter etrf = new EntityTimeRangeFilter(null, timeRange);
        filter = Filter.addAndChild(filter, etrf);
      }
    }

    if (exprDepth > 0) {
      /* We are at a component level, event, todo etc.
       * If there are property filters turn this into an and of the current
       * filter with the or'd prop filters
       */
      filter = Filter.addAndChild(filter, processPropFilters(eq, entityType));
    }

    if (!Util.isEmpty(compFilters)) {
      Filter cfilters = null;
      for (CompFilter cf: compFilters) {
        cfilters = Filter.addOrChild(cfilters, cf.getQueryFilter(eq, exprDepth + 1));
      }

      filter = Filter.addAndChild(filter, cfilters);
    }

    return filter;
  }

  private Filter processPropFilters(final EventQuery eq,
                                      final int entityType) throws WebdavException {
    if (Util.isEmpty(propFilters)) {
      return null;
    }

    Filter pfilters = null;

    for (PropFilter pf: propFilters) {
      String pname = pf.getName();

      TimeRange tr = pf.getTimeRange();
      TextMatch tm = pf.getMatch();
      boolean isNotDefined = pf.getIsNotDefined();
      boolean testPresent = !isNotDefined && (tr == null) && (tm == null) &&
                            (Util.isEmpty(pf.getParamFilters()));
      Filter filter = makeFilter(pname, isNotDefined, testPresent,
                                 tr, tm, pf.getParamFilters());

      if (filter != null) {
        pfilters = Filter.addAndChild(pfilters, filter);
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

  private Filter makeFilter(final String pname, final boolean testNotDefined,
                            final boolean testPresent,
                            final TimeRange timeRange,
                            final TextMatch match,
                            final Collection<ParamFilter> paramFilters) throws WebdavException {
    Filter filter = null;

    PropertyInfoIndex pi = PropertyInfoIndex.lookupPname(pname);

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
      f.setEntity(match.getVal());
      f.setExact(false);
      boolean caseless = (match.getCaseless() == null) || (match.getCaseless());
      f.setCaseless(caseless);
      f.setNot(match.getNegated());
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

    return Filter.addAndChild(filter, processParamFilters(pi,
                                                          paramFilters));
  }

  private Filter processParamFilters(final PropertyInfoIndex parentIndex,
                                     final Collection<ParamFilter> paramFilters) throws WebdavException {
    Filter parfilters = null;

    for (ParamFilter pf: paramFilters) {
      TextMatch tm = pf.getMatch();
      boolean isNotDefined = pf.getIsNotDefined();
      boolean testPresent = isNotDefined && (tm == null);

      PropertyFilter filter = (PropertyFilter)makeFilter(pf.getName(),
                                                         isNotDefined,
                                                         testPresent,
                                                         null, tm, null);

      if (filter != null) {
        filter.setParentPropertyIndex(parentIndex);
        parfilters = Filter.addOrChild(parfilters, filter);
      }
    }

    return parfilters;
  }

  private Collection<PropFilter> addPropFilter(Collection<PropFilter> pfs,
                                               final PropFilter val) {
    if (pfs == null) {
      pfs = new ArrayList<PropFilter>();
    }

    pfs.add(val);

    return pfs;
  }

  /** Debug
   *
   * @param log
   * @param indent
   */
  public void dump(final Logger log, final String indent) {
    StringBuilder sb = new StringBuilder(indent);

    sb.append("<comp-filter name=\"");
    sb.append(name);
    sb.append("\">");
    log.debug(sb.toString());

    if (isNotDefined) {
      log.debug(indent + "  " + "<is-not-defined/>");
    } else if (timeRange != null) {
      timeRange.dump(log, indent + "  ");
    }

    if (compFilters != null) {
      for (CompFilter cf: compFilters) {
        cf.dump(log, indent + "  ");
      }
    }

    if (propFilters != null) {
      for (PropFilter pf: propFilters) {
        pf.dump(log, indent + "  ");
      }
    }

    log.debug(indent + "</comp-filter>");
  }
}

