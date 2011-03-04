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

import edu.rpi.cct.webdav.servlet.shared.WebdavException;

import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;

/**
 *
 *   @author Mike Douglass
 */
public class PropFilter {
  /* Name of property */
  private String name;

  private boolean isNotDefined;

  private TimeRange timeRange;

  private TextMatch match;

  private ArrayList<ParamFilter> paramFilters;

  /** Constructor
   *
   * @param name
   */
  public PropFilter(String name) {
    this.name = name;
  }

  /** Constructor
   *
   * @param name
   * @param isNotDefined
   */
  public PropFilter(String name, boolean isNotDefined) {
    this.name = name;
    this.isNotDefined = isNotDefined;
  }

  /** Constructor
   *
   * @param name
   * @param timeRange
   */
  public PropFilter(String name, TimeRange timeRange) {
    this.name = name;
    this.timeRange = timeRange;
  }

  /** Constructor
   *
   * @param name
   * @param match
   */
  public PropFilter(String name, TextMatch match) {
    this.name = name;
    this.match = match;
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
   * @return boolean
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
   * @return TimeRange
   */
  public TimeRange getTimeRange() {
    return timeRange;
  }

  /**
   * @param val
   */
  public void setMatch(TextMatch val) {
    match = val;
  }

  /**
   * @return TextMatch
   */
  public TextMatch getMatch() {
    return match;
  }

  /**
   * @return Collection
   */
  public Collection<ParamFilter> getParamFilters() {
    if (paramFilters == null) {
      paramFilters = new ArrayList<ParamFilter>();
    }

    return paramFilters;
  }

  /** Add a param filter
   *
   * @param pf
   */
  public void addParamFilter(ParamFilter pf) {
    getParamFilters().add(pf);
  }

  /** Return true if the given component matches the property filter
   *
   * NOTE *********** Not handling params yet
   *
   * @param c
   * @return boolean true if the given component matches the property filter
   * @throws WebdavException
   */
  public boolean filter(Component c) throws WebdavException {
    try {
      PropertyList pl = c.getProperties();

      if (pl == null) {
        return false;
      }

      Property prop = pl.getProperty(getName());

      if (prop == null) {
        return getIsNotDefined();
      }

      TextMatch match = getMatch();
      if (match != null) {
        return match.matches(prop.getValue());
      }

      TimeRange tr = getTimeRange();
      if (tr == null) {
        // invalid state?
        return true;
      }

      return tr.matches(prop);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** Debug
   *
   * @param log
   * @param indent
   */
  public void dump(Logger log, String indent) {
    StringBuffer sb = new StringBuffer(indent);

    sb.append("<prop-filter name=\"");
    sb.append(name);
    sb.append("\">\n");
    log.debug(sb.toString());

    if (isNotDefined) {
      log.debug(indent + "  " + "<is-not-defined/>\n");
    } else if (timeRange != null) {
      timeRange.dump(log, indent + "  ");
    } else if (match != null) {
      match.dump(log, indent + "  ");
    }

    if (paramFilters != null) {
      for (ParamFilter pf: paramFilters) {
        pf.dump(log, indent + "  ");
      }
    }

    log.debug(indent + "</prop-filter>");
  }
}

