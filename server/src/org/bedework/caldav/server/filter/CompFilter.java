/*
 Copyright (c) 2000-2005 University of Washington.  All rights reserved.

 Redistribution and use of this distribution in source and binary forms,
 with or without modification, are permitted provided that:

   The above copyright notice and this permission notice appear in
   all copies and supporting documentation;

   The name, identifiers, and trademarks of the University of Washington
   are not used in advertising or publicity without the express prior
   written permission of the University of Washington;

   Recipients acknowledge that this distribution is made available as a
   research courtesy, "as is", potentially with defects, without
   any obligation on the part of the University of Washington to
   provide support, services, or repair;

   THE UNIVERSITY OF WASHINGTON DISCLAIMS ALL WARRANTIES, EXPRESS OR
   IMPLIED, WITH REGARD TO THIS SOFTWARE, INCLUDING WITHOUT LIMITATION
   ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
   PARTICULAR PURPOSE, AND IN NO EVENT SHALL THE UNIVERSITY OF
   WASHINGTON BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
   DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
   PROFITS, WHETHER IN AN ACTION OF CONTRACT, TORT (INCLUDING
   NEGLIGENCE) OR STRICT LIABILITY, ARISING OUT OF OR IN CONNECTION WITH
   THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
/* **********************************************************************
    Copyright 2005 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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

import edu.rpi.cct.webdav.servlet.common.WebdavUtils;
import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.bedework.caldav.server.TimeRange;
import org.bedework.caldav.server.filter.Filter.EventQuery;
import org.bedework.calfacade.CalFacadeDefs;
import org.bedework.calfacade.filter.BwAndFilter;
import org.bedework.calfacade.filter.BwEntityTypeFilter;
import org.bedework.calfacade.filter.BwFilter;
import org.bedework.calfacade.filter.BwObjectFilter;
import org.bedework.calfacade.filter.BwOrFilter;
import org.bedework.calfacade.util.PropertyIndex;
import org.bedework.calfacade.util.PropertyIndex.PropertyInfo;

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
      }

      if ("VTODO".equals(getName())) {
        filter = BwEntityTypeFilter.todoFilter(null, isNotDefined);
      }

      if ("VJOURNAL".equals(getName())) {
        filter = BwEntityTypeFilter.journalFilter(null, isNotDefined);
      }

      if (filter == null) {
        throw new WebdavBadRequest();
      }
    } else if (exprDepth == 2) {
      // Sub-components only

      if ("VALARM".equals(getName())) {
        filter = BwEntityTypeFilter.alarmFilter(null, isNotDefined);
      }

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

    if (timeRange != null) {
      if (eq.trange == null) {
        eq.trange = timeRange;
      } else {
        eq.trange.merge(timeRange);
      }
    }

    if (exprDepth != 0) {
      int entityType = ((BwEntityTypeFilter)filter).getEntity();
      filter = addAndChild(filter, processPropFilters(eq, entityType));
    }

    if (!WebdavUtils.emptyCollection(compFilters)) {
      BwFilter cfilters = null;
      for (CompFilter cf: compFilters) {
        cfilters = addOrChild(cfilters, cf.getQuery(eq, exprDepth + 1));
      }

      filter = addAndChild(filter, cfilters);
    }

    return filter;
  }

  private BwFilter processPropFilters(EventQuery eq, int entityType) throws WebdavException {
    if (WebdavUtils.emptyCollection(propFilters)) {
      return null;
    }

    BwFilter pfilters = null;

    for (PropFilter pf: propFilters) {
      String pname = pf.getName();
      BwFilter filter = null;

      PropertyInfo pi = PropertyIndex.propertyInfoByPname.get(pname);
      if (pi == null) {
        // Unknown property
        throw new WebdavBadRequest("Unknown property " + pname);
      }

      if (pf.getIsNotDefined()) {
        filter = BwObjectFilter.makeFilter(null, pi.getPindex());
        if (filter != null) {
          ((BwObjectFilter)filter).setTestNotPresent();
        }
      }

      if ((pf.getTimeRange() == null) &&
          (WebdavUtils.emptyCollection(pf.getParamFilters()))) {
        // Presence check
        filter = BwObjectFilter.makeFilter(null, pi.getPindex());
        if (filter != null) {
          ((BwObjectFilter)filter).setTestPresent();
        }
      }

      if (filter == null) {
        eq.postFilter = true;

        // XXX This is wrong - if we postfilter we have to postfilter everything
        // XXX because it's an OR

        if (entityType == CalFacadeDefs.entityTypeEvent) {
          eq.eventFilters = addPropFilter(eq.eventFilters, pf);
        } else if (entityType == CalFacadeDefs.entityTypeTodo) {
          eq.todoFilters = addPropFilter(eq.todoFilters, pf);
        } else if (entityType == CalFacadeDefs.entityTypeJournal) {
          eq.journalFilters = addPropFilter(eq.journalFilters, pf);
        } else if (entityType == CalFacadeDefs.entityTypeAlarm) {
          eq.alarmFilters = addPropFilter(eq.alarmFilters, pf);
        }
      } else {
        addOrChild(pfilters, filter);
      }
    }

    return pfilters;
  }

  private Collection<PropFilter> addPropFilter(Collection<PropFilter> pfs,
                                               PropFilter val) {
    if (pfs == null) {
      pfs = new ArrayList<PropFilter>();
    }

    pfs.add(val);

    return pfs;
  }

  private BwFilter addOrChild(BwFilter filter, BwFilter child) {
    if (child == null) {
      return filter;
    }

    if (filter == null) {
      return child;
    }

    BwOrFilter orf;
    if (filter instanceof BwOrFilter) {
      orf = (BwOrFilter)filter;
    } else {
      orf = new BwOrFilter();
      orf.addChild(filter);
      filter = orf;
    }

    orf.addChild(child);

    return orf;
  }

  private BwFilter addAndChild(BwFilter filter, BwFilter child) {
    if (child == null) {
      return filter;
    }

    if (filter == null) {
      return child;
    }

    BwAndFilter andf;
    if (filter instanceof BwAndFilter) {
      andf = (BwAndFilter)filter;
    } else {
      andf = new BwAndFilter();
      andf.addChild(filter);
      filter = andf;
    }

    andf.addChild(child);

    return andf;
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
      Iterator it = compFilters.iterator();

      while (it.hasNext()) {
        CompFilter cf = (CompFilter)it.next();
        cf.dump(log, indent + "  ");
      }
    }

    if (propFilters != null) {
      Iterator it = propFilters.iterator();

      while (it.hasNext()) {
        PropFilter pf = (PropFilter)it.next();
        pf.dump(log, indent + "  ");
      }
    }

    log.debug(indent + "<comp-filter/>");
  }
}

