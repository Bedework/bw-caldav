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
package org.bedework.caldav.util.filter;

import org.bedework.util.misc.ToString;
import org.bedework.webdav.servlet.shared.WebdavException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A filter selects events (and possibly other entities) that fulfill
 * certain criteria.  For example, "All events that have the category uid
 * 012345".
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
public class FilterBase implements Serializable {
  /** The internal name of the filter
   */
  protected String name;

  /** Some sort of description - may be null
   */
  protected String description;

  /** not equals or not in
   */
  protected boolean not;

  /** Should this be cached?
   */
  protected boolean cache = true;

  protected FilterBase parent;

  /** The children of the filter */
  protected List<FilterBase> children;

  /**
   * @param name
   */
  public FilterBase(final String name) {
    setName(name);
  }
  /* ====================================================================
   *                   Bean methods
   * ==================================================================== */

  /** Set the name
   *
   * @param val    String name
   */
  protected void setName(final String val) {
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
  public void setDescription(final String val) {
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
  public void setNot(final boolean val) {
    not = val;
  }

  /** Get the not
   *
   * @return boolean   not
   */
  public boolean getNot() {
    return not;
  }

  /** Set the cache flag
   *
   * @param val    boolean not
   */
  public void setCache(final boolean val) {
    cache = val;
  }

  /** Get the cache flag
   *
   * @return boolean   cache
   */
  public boolean getCache() {
    return cache;
  }

  /** Set the parent for this calendar
   *
   * @param val   FilterVO parent object
   */
  public void setParent(final FilterBase val) {
    parent = val;
  }

  /** Get the parent
   *
   * @return FilterVO    the parent
   */
  public FilterBase getParent() {
    return parent;
  }

  /* ====================================================================
   *                   Children methods
   * ==================================================================== */

  /**  Set the set of children
   *
   * @param   val   List of children for this filter
   */
  public void setChildren(final List<FilterBase> val) {
    children = val;
  }

  /**  Get the set of children
   *
   * @return List   BwFilter children for this filter
   */
  public List<FilterBase> getChildren() {
    return children;
  }

  /** Return number of children
   *
   * @return int num children
   */
  public int getNumChildren() {
    List<FilterBase> c = getChildren();
    if (c == null) {
      return 0;
    }
    return c.size();
  }

  /**  Add a child to the set of children
   *
   * @param val     BwFilter child
   */
  public void addChild(final FilterBase val) {
    if (val == null) {
      return;
    }

    List<FilterBase> c = getChildren();

    if (c == null) {
      c = new ArrayList<FilterBase>();
      setChildren(c);
    }

    c.add(val);
    val.setParent(this);
  }

  /* ====================================================================
   *                   matching methods
   * ==================================================================== */

  /** Overridden by filters which attempt to match the object with the
   * requirements.
   *
   * @param o
   * @param userHref - for whom we are matching
   * @return true for a match
   * @throws WebdavException - on matching errors
   */
  public boolean match(final Object o,
                       final String userHref) throws WebdavException {
    return false;
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
  public static FilterBase addOrChild(final FilterBase filter, final FilterBase child) {
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
  public static FilterBase addAndChild(final FilterBase filter, final FilterBase child) {
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

  protected void stringOper(final StringBuilder sb) {
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
  protected void toStringSegment(final StringBuilder sb) {
    //sb.append("\n, name=");
    //sb.append(name);

    if (description != null) {
      sb.append("description=");
      sb.append(description);
    }

    if (parent != null) {
      sb.append(", parent=");
      sb.append(parent.getName());
    }
  }

  /** Add our stuff to the builder
   *
   * @param ts    Builder for result
   */
  protected void toStringSegment(final ToString ts) {
    //sb.append("\n, name=");
    //sb.append(name);

    ts.append("description", description);

    if (parent != null) {
      ts.append("parent", parent.getName());
    }
  }
}
