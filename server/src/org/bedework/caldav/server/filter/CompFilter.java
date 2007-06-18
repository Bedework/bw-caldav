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

import org.bedework.caldav.server.filter.Filter.EventQuery;
import org.bedework.calfacade.base.TimeRange;
import org.bedework.calfacade.CalFacadeDefs;
import org.bedework.calfacade.filter.BwEntityTimeRangeFilter;
import org.bedework.calfacade.filter.BwEntityTypeFilter;
import org.bedework.calfacade.filter.BwFilter;
import org.bedework.calfacade.filter.BwObjectFilter;
import org.bedework.calfacade.filter.BwPresenceFilter;
import org.bedework.calfacade.filter.BwPropertyFilter;
import org.bedework.calfacade.util.PropertyIndex;
import org.bedework.calfacade.util.PropertyIndex.PropertyInfo;

import edu.rpi.cct.webdav.servlet.common.WebdavUtils;
import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavForbidden;
import edu.rpi.sss.util.xml.tagdefs.CaldavTags;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;

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
  public CompFilter(String name) {
    this.name = name;
  }

  /**
   * @param val
   */
  public void setName(String val) {
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
  public void setIsNotDefined(boolean val) {
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
  public void setTimeRange(TimeRange val) {
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
  public void addCompFilter(CompFilter cf) {
    getCompFilters().add(cf);
  }

  /**
   * @return Vector of prop filter
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
  public void addPropFilter(PropFilter pf) {
    getPropFilters().add(pf);
  }

  /** Convenience method
   *
   * @return boolean true ifthis element matches all of the
   *                  named component types.
   */
  public boolean matchAll() {
    return (timeRange == null)  &&
           WebdavUtils.emptyCollection(compFilters) &&
           WebdavUtils.emptyCollection(propFilters);
  }

  /** Returns a subtree of the filter used in querying
   *
   * @param eq - so we can update time range
   * @param exprDepth - allows us to do validity checks
   * @return BwFilter - null for no filtering
   * @throws WebdavException
   */
  public BwFilter getQuery(EventQuery eq, int exprDepth) throws WebdavException {
    BwFilter filter = null;
    int entityType = CalFacadeDefs.entityTypeEvent;

    if (exprDepth == 0) {
      if (!"VCALENDAR".equals(getName())) {
        throw new WebdavBadRequest();
      }

      if (WebdavUtils.emptyCollection(compFilters)) {
        return null;
      }
    } else if (exprDepth == 1) {
      // Calendar components only

      if ("VEVENT".equals(getName())) {
        filter = BwEntityTypeFilter.eventFilter(null, isNotDefined);
        entityType = CalFacadeDefs.entityTypeEvent;
      }

      if ("VTODO".equals(getName())) {
        filter = BwEntityTypeFilter.todoFilter(null, isNotDefined);
        entityType = CalFacadeDefs.entityTypeTodo;
      }

      if ("VJOURNAL".equals(getName())) {
        filter = BwEntityTypeFilter.journalFilter(null, isNotDefined);
        entityType = CalFacadeDefs.entityTypeJournal;
      }

      if ("VFREEBUSY".equals(getName())) {
        filter = BwEntityTypeFilter.freebusyFilter(null, isNotDefined);
        entityType = CalFacadeDefs.entityTypeFreeAndBusy;
      }

      if (filter == null) {
        throw new WebdavBadRequest();
      }
    } else if (exprDepth == 2) {
      // Sub-components only

      // XXX
      entityType = CalFacadeDefs.entityTypeAlarm;
      PropertyInfo pi = PropertyIndex.propertyInfoByPname.get(getName());
      if (pi == null) {
        // Unknown property
        throw new WebdavBadRequest("Unknown property " + getName());
      }

      filter = makeFilter(pi, isNotDefined, matchAll(), timeRange, null,
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

    if (exprDepth == 1) {
      /* XXX This is wrong - if filters handle time ranges OK we should remove
       * this merge which was here so post-processing could handle it.
       */
      if (timeRange != null) {
/*        if (eq.trange == null) {
          eq.trange = timeRange;
        } else {
          eq.trange.merge(timeRange);
        } */
        BwEntityTimeRangeFilter etrf = new BwEntityTimeRangeFilter(null, timeRange);
        filter = BwFilter.addAndChild(filter, etrf);
      }
    }

    if (exprDepth > 0) {
      /* We are at a component level, event, todo etc.
       * If there are property filters turn this into an and of the current
       * filter with the or'd prop filters
       */
      filter = BwFilter.addAndChild(filter, processPropFilters(eq, entityType));
    }

    if (!WebdavUtils.emptyCollection(compFilters)) {
      BwFilter cfilters = null;
      for (CompFilter cf: compFilters) {
        cfilters = BwFilter.addOrChild(cfilters, cf.getQuery(eq, exprDepth + 1));
      }

      filter = BwFilter.addAndChild(filter, cfilters);
    }

    return filter;
  }

  private BwFilter processPropFilters(EventQuery eq,
                                      int entityType) throws WebdavException {
    if (WebdavUtils.emptyCollection(propFilters)) {
      return null;
    }

    BwFilter pfilters = null;

    for (PropFilter pf: propFilters) {
      String pname = pf.getName();

      PropertyInfo pi = PropertyIndex.propertyInfoByPname.get(pname);
      if (pi == null) {
        // Unknown property
        throw new WebdavForbidden(CaldavTags.supportedFilter,
                                  "Unknown property " + pname);
      }

      TimeRange tr = pf.getTimeRange();
      TextMatch tm = pf.getMatch();
      boolean isNotDefined = pf.getIsNotDefined();
      boolean testPresent = !isNotDefined && (tr == null) && (tm == null) &&
                            (WebdavUtils.emptyCollection(pf.getParamFilters()));
      BwFilter filter = makeFilter(pi, isNotDefined, testPresent,
                                   tr, tm, pf.getParamFilters());

      if (filter != null) {
        pfilters = BwFilter.addOrChild(pfilters, filter);
      } else {
        eq.postFilter = true;

        // XXX This is wrong - if we postfilter we have to postfilter everything
        // XXX because it's an OR

        /** Add the propfilter to the post filter collection
         */
        if (entityType == CalFacadeDefs.entityTypeEvent) {
          eq.eventFilters = addPropFilter(eq.eventFilters, pf);
        } else if (entityType == CalFacadeDefs.entityTypeTodo) {
          eq.todoFilters = addPropFilter(eq.todoFilters, pf);
        } else if (entityType == CalFacadeDefs.entityTypeJournal) {
          eq.journalFilters = addPropFilter(eq.journalFilters, pf);
        } else if (entityType == CalFacadeDefs.entityTypeAlarm) {
          eq.alarmFilters = addPropFilter(eq.alarmFilters, pf);
        }
      }
    }

    return pfilters;
  }

  private BwFilter makeFilter(PropertyInfo pi, boolean testNotDefined,
                              boolean testPresent,
                              TimeRange timeRange,
                              TextMatch match,
                              Collection<ParamFilter> paramFilters) throws WebdavException {
    BwFilter filter = null;

    if (testNotDefined) {
      filter = new BwPresenceFilter(null, pi.getPindex(), false);
    } else if (testPresent) {
      // Presence check
      filter = new BwPresenceFilter(null, pi.getPindex(), true);
    } else if (timeRange != null) {
      filter = BwObjectFilter.makeFilter(null, pi.getPindex(), timeRange);
    } else if (match != null) {
      BwObjectFilter<String> f = new BwObjectFilter<String>(null, pi.getPindex());
      f.setEntity(match.getVal());
      f.setExact(false);
      boolean caseless = (match.getCaseless() == null) || (match.getCaseless());
      f.setCaseless(caseless);
      f.setNot(match.getNegated());
      filter = f;
    } else {
      // Must have param filters
      if (WebdavUtils.emptyCollection(paramFilters)) {
        throw new WebdavBadRequest();
      }
    }

    if (WebdavUtils.emptyCollection(paramFilters)) {
      return filter;
    }

    return BwFilter.addAndChild(filter, processParamFilters(pi, paramFilters));
  }

  private BwFilter processParamFilters(PropertyInfo parentPi,
                                       Collection<ParamFilter> paramFilters) throws WebdavException {
    BwFilter parfilters = null;

    for (ParamFilter pf: paramFilters) {
      PropertyInfo pi = PropertyIndex.propertyInfoByPname.get(pf.getName());
      if (pi == null) {
        // Unknown property
        throw new WebdavForbidden(CaldavTags.supportedFilter,
                                  "Unknown parameter " + pf.getName());
      }

      TextMatch tm = pf.getMatch();
      boolean isNotDefined = pf.getIsNotDefined();
      boolean testPresent = isNotDefined && (tm == null);
      BwPropertyFilter filter = (BwPropertyFilter)makeFilter(pi, isNotDefined,
                                                             testPresent,
                                                             null, tm, null);

      if (filter != null) {
        filter.setParentPropertyIndex(parentPi.getPindex());
        parfilters = BwFilter.addOrChild(parfilters, filter);
      }
    }

    return parfilters;
  }

  private Collection<PropFilter> addPropFilter(Collection<PropFilter> pfs,
                                               PropFilter val) {
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
  public void dump(Logger log, String indent) {
    StringBuffer sb = new StringBuffer(indent);

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

