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

package org.bedework.caldav.server.calquery;

import java.util.Iterator;
import java.util.Vector;

import org.apache.log4j.Logger;

/** Represent a comp element
 *
 *   @author Mike Douglass   douglm@rpi.edu
 */
public class Comp {
  private String name;

  /* if allcomp != true then look at comp */
  private boolean allcomp;

  private Vector comps; // null for zero otherwise vector of comp

  // true or look at props
  private boolean allprop;

  private Vector props; // null for zero otherwise vector of prop

  /** Constructor
   *
   * @param name
   */
  public Comp(String name) {
    this.name = name;
  }

  /**
   * @param val
   */
  public void setName(String val) {
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
  public void setAllcomp(boolean val) {
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
  public Vector getComps() {
    if (comps == null) {
      comps = new Vector();
    }

    return comps;
  }

  /**
   * @param c
   */
  public void addComp(Comp c) {
    getComps().add(c);
  }

  /**
   * @param val
   */
  public void setAllprop(boolean val) {
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
  public Vector getProps() {
    if (props == null) {
      props = new Vector();
    }

    return props;
  }

  /**
   * @param p
   */
  public void addProp(Prop p) {
    getProps().add(p);
  }

  /** Debugging
   *
   * @param log
   * @param indent
   */
  public void dump(Logger log, String indent) {
    StringBuffer sb = new StringBuffer(indent);

    sb.append("<comp name=");
    sb.append(name);
    sb.append(">");
    log.debug(sb.toString());

    if (allcomp) {
      log.debug(indent + "  <allcomp/>");
    } else if (comps != null) {
      Iterator it = comps.iterator();

      while (it.hasNext()) {
        Comp c = (Comp)it.next();
        c.dump(log, indent + "  ");
      }
    }

    if (allprop) {
      log.debug(indent + "  <allprop/>");
    } else if (props != null) {
      Iterator it = props.iterator();

      while (it.hasNext()) {
        Prop pr = (Prop)it.next();
        pr.dump(log, indent + "  ");
      }
    }

    log.debug(indent + "</comp>");
  }
}


