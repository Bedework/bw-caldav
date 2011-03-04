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
  private boolean allprop;

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
