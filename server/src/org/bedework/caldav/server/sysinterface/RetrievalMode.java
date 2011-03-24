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
package org.bedework.caldav.server.sysinterface;

import ietf.params.xml.ns.caldav.Expand;
import ietf.params.xml.ns.caldav.LimitFreebusySet;
import ietf.params.xml.ns.caldav.LimitRecurrenceSet;

import java.io.Serializable;

/**
 * @author douglm
 *
 */
public class RetrievalMode implements Serializable {
  /**
   * Values which define how to retrieve recurring events. We have the
   * following choices
   *   No limits
   *   Limit Recurrence set - with range
   *   Expand recurrences
   */

  private Expand expand;
  private LimitRecurrenceSet limitRecurrenceSet;
  private LimitFreebusySet limitFreebusySet;

  /**
   * Sets the value of the expand property.
   *
   * @param val
   *     allowed object is
   *     {@link Expand }
   *
   */
  public void setExpand(final Expand val) {
      expand = val;
  }

  /**
   * Gets the value of the expand property.
   *
   * @return
   *     possible object is
   *     {@link Expand }
   *
   */
  public Expand getExpand() {
      return expand;
  }

  /**
   * Sets the value of the limitRecurrenceSet property.
   *
   * @param val
   *     allowed object is
   *     {@link LimitRecurrenceSet }
   *
   */
  public void setLimitRecurrenceSet(final LimitRecurrenceSet val) {
      limitRecurrenceSet = val;
  }

  /**
   * Gets the value of the limitRecurrenceSet property.
   *
   * @return
   *     possible object is
   *     {@link LimitRecurrenceSet }
   *
   */
  public LimitRecurrenceSet getLimitRecurrenceSet() {
      return limitRecurrenceSet;
  }

  /**
   * Sets the value of the limitFreebusySet property.
   *
   * @param val
   *     allowed object is
   *     {@link LimitFreebusySet }
   *
   */
  public void setLimitFreebusySet(final LimitFreebusySet val) {
      limitFreebusySet = val;
  }

  /**
   * Gets the value of the limitFreebusySet property.
   *
   * @return
   *     possible object is
   *     {@link LimitFreebusySet }
   *
   */
  public LimitFreebusySet getLimitFreebusySet() {
      return limitFreebusySet;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("RetrievalMode{");

    String start = null;
    String end = null;
    String name = null;

    if (expand != null) {
      name = "expand";
      start = expand.getStart();
      end = expand.getEnd();
    } else if (limitFreebusySet != null) {
      name = "limit-freebusy-set";
      start = limitFreebusySet.getStart();
      end = limitFreebusySet.getEnd();
    } else if (limitRecurrenceSet != null) {
      name = "limit-recurrence-set";
      start = limitRecurrenceSet.getStart();
      end = limitRecurrenceSet.getEnd();
    }

    sb.append(name);
    sb.append(", ");

    sb.append(", start=");
    sb.append(start);

    sb.append(", end=");
    sb.append(end);

    sb.append("}");
    return sb.toString();
  }
}
