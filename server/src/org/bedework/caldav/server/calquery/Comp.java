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

package org.bedework.caldav.server.calquery;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;

/** Represent a comp element
 *
 *   @author Mike Douglass   douglm@rpi.edu
 */
public class Comp {
  private String name;

  /* if allcomp != true then look at comp */
  private boolean allcomp;

  private Collection<Comp> comps; // null for zero

  // true or look at props
  private boolean allprop = true;

  private Collection<Prop> props; // null for zero

  /** Constructor
   *
   * @param name
   */
  public Comp(final String name) {
    this.name = name;
  }

  /**
   * @param val
   */
  public void setName(final String val) {
    name = val;
  }

  /**
   * @return STring name
   */
  public String getName() {
    return name;
  }

  /**
   * @param val
   */
  public void setAllcomp(final boolean val) {
    allcomp = val;
  }

  /**
   * @return boolean true for allcomp
   */
  public boolean getAllcomp() {
    return allcomp;
  }

  /**
   * @return Vector of comp
   */
  public Collection<Comp> getComps() {
    if (comps == null) {
      comps = new ArrayList<Comp>();
    }

    return comps;
  }

  /**
   * @param c
   */
  public void addComp(final Comp c) {
    getComps().add(c);
  }

  /**
   * @param val
   */
  public void setAllprop(final boolean val) {
    allprop = val;
  }

  /**
   * @return boolean true for all props
   */
  public boolean getAllprop() {
    return allprop;
  }

  /**
   * @return Vector of props
   */
  public Collection<Prop> getProps() {
    if (props == null) {
      props = new ArrayList<Prop>();
    }

    return props;
  }

  /**
   * @param p
   */
  public void addProp(final Prop p) {
    getProps().add(p);
    setAllprop(false);
  }

  /** Debugging
   *
   * @param log
   * @param indent
   */
  public void dump(final Logger log, final String indent) {
    StringBuffer sb = new StringBuffer(indent);

    sb.append("<comp name=");
    sb.append(name);
    sb.append(">");
    log.debug(sb.toString());

    if (allcomp) {
      log.debug(indent + "  <allcomp/>");
    } else if (comps != null) {
      for (Comp c: comps) {
        c.dump(log, indent + "  ");
      }
    }

    if (allprop) {
      log.debug(indent + "  <allprop/>");
    } else if (props != null) {
      for (Prop pr: props) {
        pr.dump(log, indent + "  ");
      }
    }

    log.debug(indent + "</comp>");
  }
}
