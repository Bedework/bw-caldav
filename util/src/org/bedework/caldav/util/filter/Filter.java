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
package org.bedework.caldav.util.filter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A filter selects events (and possibly other entities) that fulfill
 * certain criteria.  For example, "All events that have the category
 * 'Lecture'".
 *
 * <p>All filters must be expressible as a db search expresssion. Entity
 * filters select events that own a given entity or own an entity within a
 * set. This translates to <br/>
 *    event.location = given-location or <br/>
 *    event.location in given-location-set <br/>
 *
 * <p>The test may be negated to give != and not in.
 *
 * <p>Some filters can have any number of children such as an OrFilter.
 *
 * @author Mike Douglass
 * @version 1.1
 */
public class Filter implements Serializable {
  /** The internal name of the filter
   */
  protected String name;

  /** Some sort of description - may be null
   */
  protected String description;

  /** not equals or not in
   */
  protected boolean not;

  protected Filter parent;

  /** The children of the filter */
  protected List<Filter> children;

  /**
   * @param name
   */
  public Filter(String name) {
    setName(name);
  }
  /* ====================================================================
   *                   Bean methods
   * ==================================================================== */

  /** Set the name
   *
   * @param val    String name
   */
  protected void setName(String val) {
    name = val;
  }

  /** Get the name
   *
   * @return String   name
   */
  public String getName() {
    return name;
  }

  /** Set the description
   *
   * @param val    description
   */
  public void setDescription(String val) {
    description = val;
  }

  /** Get the description
   *
   *  @return String   description
   */
  public String getDescription() {
    return description;
  }

  /** Set the not
   *
   * @param val    boolean not
   */
  public void setNot(boolean val) {
    not = val;
  }

  /** Get the not
   *
   * @return boolean   not
   */
  public boolean getNot() {
    return not;
  }

  /** Set the parent for this calendar
   *
   * @param val   FilterVO parent object
   */
  public void setParent(Filter val) {
    parent = val;
  }

  /** Get the parent
   *
   * @return FilterVO    the parent
   */
  public Filter getParent() {
    return parent;
  }

  /* ====================================================================
   *                   Children methods
   * ==================================================================== */

  /**  Set the set of children
   *
   * @param   val   List of children for this filter
   */
  public void setChildren(List<Filter> val) {
    children = val;
  }

  /**  Get the set of children
   *
   * @return List   BwFilter children for this filter
   */
  public List<Filter> getChildren() {
    return children;
  }

  /** Return number of children
   *
   * @return int num children
   */
  public int getNumChildren() {
    List<Filter> c = getChildren();
    if (c == null) {
      return 0;
    }
    return c.size();
  }

  /**  Add a child to the set of children
   *
   * @param val     BwFilter child
   */
  public void addChild(Filter val) {
    List<Filter> c = getChildren();

    if (c == null) {
      c = new ArrayList<Filter>();
      setChildren(c);
    }

    c.add(val);
    val.setParent(this);
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /** Given a possibly null filter and a possibly null child:
   *
   * <p>If the child is null just return the filter.
   *
   * <p>If the filter is null return the child.
   *
   * <p>Both non-null, so if the filter is an or filter add the child to the
   * filter and return.
   *
   * <p>Otherwise, add the filter to a new or filter, add the child to the or
   * filter and return the or filter.
   *
   * <p>This allows us to parse zero or more terms which should be or'd if there
   * is more than one. The result of repeatedly calling
   * <pre>filter = addOrChild(filter, child)</pre> is either null, a filter
   * or an 'or' filter.
   *
   * @param filter
   * @param child
   * @return a filter
   */
  public static Filter addOrChild(Filter filter, Filter child) {
    if (child == null) {
      return filter;
    }

    if (filter == null) {
      return child;
    }

    OrFilter orf;
    if (filter instanceof OrFilter) {
      orf = (OrFilter)filter;
    } else {
      orf = new OrFilter();
      orf.addChild(filter);
    }

    orf.addChild(child);

    return orf;
  }

  /** See addOrChild.
   *
   * @param filter
   * @param child
   * @return filter
   */
  public static Filter addAndChild(Filter filter, Filter child) {
    if (child == null) {
      return filter;
    }

    if (filter == null) {
      return child;
    }

    AndFilter andf;
    if (filter instanceof AndFilter) {
      andf = (AndFilter)filter;
    } else {
      andf = new AndFilter();
      andf.addChild(filter);
    }

    andf.addChild(child);

    return andf;
  }

  protected void stringOper(StringBuilder sb) {
    if (getNot()) {
      sb.append(" != ");
    } else {
      sb.append(" = ");
    }
  }

  /** Add our stuff to the StringBuffer
   *
   * @param sb    StringBuilder for result
   */
   protected void toStringSegment(StringBuilder sb) {
    sb.append("\n, name=");
    sb.append(name);
    sb.append(", description=");
    sb.append(description);
    sb.append(", parent=");
    if (parent == null) {
      sb.append("null");
    } else {
      sb.append(parent.getName());
    }
  }
}
