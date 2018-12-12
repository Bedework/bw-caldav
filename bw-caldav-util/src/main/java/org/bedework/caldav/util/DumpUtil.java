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

import org.bedework.util.logging.SLogged;

import ietf.params.xml.ns.caldav.CalendarDataType;
import ietf.params.xml.ns.caldav.CompFilterType;
import ietf.params.xml.ns.caldav.CompType;
import ietf.params.xml.ns.caldav.FilterType;
import ietf.params.xml.ns.caldav.ParamFilterType;
import ietf.params.xml.ns.caldav.PropFilterType;
import ietf.params.xml.ns.caldav.PropType;
import ietf.params.xml.ns.caldav.TextMatchType;
import ietf.params.xml.ns.caldav.UTCTimeRangeType;

/** Various debug dump methods
 *
 *   @author Mike Douglass   douglm  bedework.edu
 */
public class DumpUtil implements SLogged {
  static {
    SLogged.setLoggerClass(DumpUtil.class);
  }

  /**
   * @param cd
   */
  public static void dumpCalendarData(final CalendarDataType cd) {
    final StringBuilder sb = new StringBuilder("  <calendar-data");

    if (cd.getContentType() != null) {
      sb.append("  content-type=\"");
      sb.append(cd.getContentType());
      sb.append("\"");
    }

    sb.append(">");
    SLogged.debug(sb.toString());

    if (cd.getComp() != null) {
      dumpComp(cd.getComp(), "    ");
    }

    if (cd.getExpand() != null) {
      dumpUTCTimeRange(cd.getExpand(), "expand", "    ");
    }

    if (cd.getLimitRecurrenceSet() != null) {
      dumpUTCTimeRange(cd.getLimitRecurrenceSet(),
                       "limit-recurrence-set", "    ");
    }

    if (cd.getLimitFreebusySet() != null) {
      dumpUTCTimeRange(cd.getLimitFreebusySet(),
                       "limit-freebusy-set", "    ");
    }

    SLogged.debug("  </calendar-data>");
  }

  /**
   * @param comp
   * @param indent
   */
  public static void dumpComp(final CompType comp,
                              final String indent) {
    StringBuffer sb = new StringBuffer(indent);

    sb.append("<comp name=");
    sb.append(comp.getName());
    sb.append(">");
    SLogged.debug(sb.toString());

    if (comp.getAllcomp() != null) {
      SLogged.debug(indent + "  <allcomp/>");
    } else {
      for (CompType c: comp.getComp()) {
        dumpComp(c, indent + "  ");
      }
    }

    if (comp.getAllprop() != null) {
      SLogged.debug(indent + "  <allprop/>");
    } else {
      for (PropType prop: comp.getProp()) {
        dumpProp(prop, indent + "  ");
      }
    }

    SLogged.debug(indent + "</comp>");
  }

  /** Debugging
   *
   * @param prop
   * @param indent
   */
  public static void dumpProp(final PropType prop,
                              final String indent) {
    final StringBuilder sb = new StringBuilder(indent);

    sb.append("<calddav:prop name=");
    sb.append(prop.getName());
    sb.append(" novalue=");
    sb.append(prop.getNovalue());
    sb.append("/>");

    SLogged.debug(sb.toString());
  }

  /**
   * @param tr
   * @param name
   * @param indent
   */
  public static void dumpUTCTimeRange(final UTCTimeRangeType tr,
                                      final String name,
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

    SLogged.debug(sb.toString());
  }

  /**
   * @param f
   */
  public static void dumpFilter(final FilterType f) {
    SLogged.debug("<filter>");
    dumpCompFilter(f.getCompFilter(), "  ");
    SLogged.debug("</filter>");
  }


  /** Debug
   *
   * @param cf
   * @param indent
   */
  public static void dumpCompFilter(final CompFilterType cf,
                                    final String indent) {
    final StringBuilder sb = new StringBuilder(indent);

    sb.append("<comp-filter name=\"");
    sb.append(cf.getName());
    sb.append("\">");
    SLogged.debug(sb.toString());

    if (cf.getIsNotDefined() != null) {
      SLogged.debug(indent + "  " + "<is-not-defined/>");
    } else if (cf.getTimeRange() != null) {
      dumpUTCTimeRange(cf.getTimeRange(), "time-range",
                       indent + "  ");
    }

    if (cf.getCompFilter() != null) {
      for (CompFilterType subcf: cf.getCompFilter()) {
        dumpCompFilter(subcf, indent + "  ");
      }
    }

    if (cf.getPropFilter() != null) {
      for (PropFilterType pf: cf.getPropFilter()) {
        dumpPropFilter(pf, indent + "  ");
      }
    }

    SLogged.debug(indent + "</comp-filter>");
  }

  /** Debug
   *
   * @param pf
   * @param indent
   */
  public static void dumpPropFilter(final PropFilterType pf,
                                    final String indent) {
    final StringBuilder sb = new StringBuilder(indent);

    sb.append("<prop-filter name=\"");
    sb.append(pf.getName());
    sb.append("\">\n");
    SLogged.debug(sb.toString());

    if (pf.getIsNotDefined() != null) {
      SLogged.debug(indent + "  " + "<is-not-defined/>\n");
    } else if (pf.getTimeRange() != null) {
      dumpUTCTimeRange(pf.getTimeRange(), "time-range",
                       indent + "  ");
    } else if (pf.getTextMatch() != null) {
      dumpTextMatch(pf.getTextMatch(), indent + "  ");
    }

    if (pf.getParamFilter() != null) {
      for (ParamFilterType parf: pf.getParamFilter()) {
        dumpParamFilter(parf, indent + "  ");
      }
    }

    SLogged.debug(indent + "</prop-filter>");
  }

  /** Debug
   *
   * @param pf
   * @param indent
   */
  public static void dumpParamFilter(final ParamFilterType pf,
                                     final String indent) {
    final StringBuilder sb = new StringBuilder(indent);

    sb.append("<param-filter name=\"");
    sb.append(pf.getName());
    sb.append(">\n");
    SLogged.debug(sb.toString());

    if (pf.getIsNotDefined() != null) {
      SLogged.debug(indent + "  " + "<is-not-defined/>\n");
    } else {
      dumpTextMatch(pf.getTextMatch(), indent + "  ");
    }

    SLogged.debug(indent + "</param-filter>");
  }

  /** Debug
   *
   * @param tm
   * @param indent
   */
  public static void dumpTextMatch(final TextMatchType tm,
                                   final String indent) {
    final StringBuilder sb = new StringBuilder(indent);

    sb.append("<text-match");
    sb.append(" collation=");
    sb.append(tm.getCollation());

    sb.append(" negate-condition=");
    sb.append(tm.getNegateCondition());

    sb.append(">");
    SLogged.debug(sb.toString());

    SLogged.debug(tm.getValue());

    SLogged.debug(indent + "</text-match>\n");
  }
}

