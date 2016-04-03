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
package org.bedework.caldav.util;

import ietf.params.xml.ns.caldav.CalendarDataType;
import ietf.params.xml.ns.caldav.CompFilterType;
import ietf.params.xml.ns.caldav.CompType;
import ietf.params.xml.ns.caldav.FilterType;
import ietf.params.xml.ns.caldav.ParamFilterType;
import ietf.params.xml.ns.caldav.PropFilterType;
import ietf.params.xml.ns.caldav.PropType;
import ietf.params.xml.ns.caldav.TextMatchType;
import ietf.params.xml.ns.caldav.UTCTimeRangeType;
import org.apache.log4j.Logger;

/** Various debug dump methods
 *
 *   @author Mike Douglass   douglm  bedework.edu
 */
public class DumpUtil {
  /**
   * @param cd
   * @param log
   */
  public static void dumpCalendarData(final CalendarDataType cd,
                                      final Logger log) {
    StringBuffer sb = new StringBuffer("  <calendar-data");

    if (cd.getContentType() != null) {
      sb.append("  content-type=\"");
      sb.append(cd.getContentType());
      sb.append("\"");
    }

    sb.append(">");
    log.debug(sb.toString());

    if (cd.getComp() != null) {
      dumpComp(cd.getComp(), log, "    ");
    }

    if (cd.getExpand() != null) {
      dumpUTCTimeRange(cd.getExpand(), "expand", log, "    ");
    }

    if (cd.getLimitRecurrenceSet() != null) {
      dumpUTCTimeRange(cd.getLimitRecurrenceSet(),
                       "limit-recurrence-set", log, "    ");
    }

    if (cd.getLimitFreebusySet() != null) {
      dumpUTCTimeRange(cd.getLimitFreebusySet(),
                       "limit-freebusy-set", log, "    ");
    }

    log.debug("  </calendar-data>");
  }

  /**
   * @param comp
   * @param log
   * @param indent
   */
  public static void dumpComp(final CompType comp,
                              final Logger log,
                              final String indent) {
    StringBuffer sb = new StringBuffer(indent);

    sb.append("<comp name=");
    sb.append(comp.getName());
    sb.append(">");
    log.debug(sb.toString());

    if (comp.getAllcomp() != null) {
      log.debug(indent + "  <allcomp/>");
    } else {
      for (CompType c: comp.getComp()) {
        dumpComp(c, log, indent + "  ");
      }
    }

    if (comp.getAllprop() != null) {
      log.debug(indent + "  <allprop/>");
    } else {
      for (PropType prop: comp.getProp()) {
        dumpProp(prop, log, indent + "  ");
      }
    }

    log.debug(indent + "</comp>");
  }

  /** Debugging
   *
   * @param prop
   * @param log
   * @param indent
   */
  public static void dumpProp(final PropType prop,
                              final Logger log, final String indent) {
    StringBuffer sb = new StringBuffer(indent);

    sb.append("<calddav:prop name=");
    sb.append(prop.getName());
    sb.append(" novalue=");
    sb.append(prop.getNovalue());
    sb.append("/>");

    log.debug(sb.toString());
  }

  /**
   * @param tr
   * @param name
   * @param log
   * @param indent
   */
  public static void dumpUTCTimeRange(final UTCTimeRangeType tr,
                                      final String name,
                                      final Logger log,
                                      final String indent) {
    StringBuilder sb = new StringBuilder(indent);

    sb.append("<");
    sb.append(name);
    sb.append(" ");

    if (tr.getStart() != null) {
      sb.append("start=");
      sb.append(tr.getStart());
    }

    if (tr.getEnd() != null) {
      if (tr.getStart() != null) {
        sb.append(" ");
      }
      sb.append("end=");
      sb.append(tr.getEnd());
    }

    sb.append("/>");

    log.debug(sb.toString());
  }

  /**
   * @param f
   * @param log
   */
  public static void dumpFilter(final FilterType f,
                                      final Logger log) {
    log.debug("<filter>");
    dumpCompFilter(f.getCompFilter(), log, "  ");
    log.debug("</filter>");
  }


  /** Debug
   *
   * @param cf
   * @param log
   * @param indent
   */
  public static void dumpCompFilter(final CompFilterType cf,
                                    final Logger log, final String indent) {
    StringBuilder sb = new StringBuilder(indent);

    sb.append("<comp-filter name=\"");
    sb.append(cf.getName());
    sb.append("\">");
    log.debug(sb.toString());

    if (cf.getIsNotDefined() != null) {
      log.debug(indent + "  " + "<is-not-defined/>");
    } else if (cf.getTimeRange() != null) {
      dumpUTCTimeRange(cf.getTimeRange(), "time-range", log,
                       indent + "  ");
    }

    if (cf.getCompFilter() != null) {
      for (CompFilterType subcf: cf.getCompFilter()) {
        dumpCompFilter(subcf, log, indent + "  ");
      }
    }

    if (cf.getPropFilter() != null) {
      for (PropFilterType pf: cf.getPropFilter()) {
        dumpPropFilter(pf, log, indent + "  ");
      }
    }

    log.debug(indent + "</comp-filter>");
  }

  /** Debug
   *
   * @param pf
   * @param log
   * @param indent
   */
  public static void dumpPropFilter(final PropFilterType pf,
                                    final Logger log, final String indent) {
    StringBuilder sb = new StringBuilder(indent);

    sb.append("<prop-filter name=\"");
    sb.append(pf.getName());
    sb.append("\">\n");
    log.debug(sb.toString());

    if (pf.getIsNotDefined() != null) {
      log.debug(indent + "  " + "<is-not-defined/>\n");
    } else if (pf.getTimeRange() != null) {
      dumpUTCTimeRange(pf.getTimeRange(), "time-range", log,
                       indent + "  ");
    } else if (pf.getTextMatch() != null) {
      dumpTextMatch(pf.getTextMatch(), log, indent + "  ");
    }

    if (pf.getParamFilter() != null) {
      for (ParamFilterType parf: pf.getParamFilter()) {
        dumpParamFilter(parf, log, indent + "  ");
      }
    }

    log.debug(indent + "</prop-filter>");
  }

  /** Debug
   *
   * @param pf
   * @param log
   * @param indent
   */
  public static void dumpParamFilter(final ParamFilterType pf,
                                     final Logger log, final String indent) {
    StringBuilder sb = new StringBuilder(indent);

    sb.append("<param-filter name=\"");
    sb.append(pf.getName());
    sb.append(">\n");
    log.debug(sb.toString());

    if (pf.getIsNotDefined() != null) {
      log.debug(indent + "  " + "<is-not-defined/>\n");
    } else {
      dumpTextMatch(pf.getTextMatch(), log, indent + "  ");
    }

    log.debug(indent + "</param-filter>");
  }

  /** Debug
   *
   * @param tm
   * @param log
   * @param indent
   */
  public static void dumpTextMatch(final TextMatchType tm,
                                   final Logger log, final String indent) {
    StringBuilder sb = new StringBuilder(indent);

    sb.append("<text-match");
    sb.append(" collation=");
    sb.append(tm.getCollation());

    sb.append(" negate-condition=");
    sb.append(tm.getNegateCondition());

    sb.append(">");
    log.debug(sb.toString());

    log.debug(tm.getValue());

    log.debug(indent + "</text-match>\n");
  }
}

