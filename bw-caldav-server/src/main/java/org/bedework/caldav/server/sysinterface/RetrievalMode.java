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

import org.bedework.base.ToString;

import ietf.params.xml.ns.caldav.ExpandType;
import ietf.params.xml.ns.caldav.LimitFreebusySetType;
import ietf.params.xml.ns.caldav.LimitRecurrenceSetType;

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

  private ExpandType expand;
  private LimitRecurrenceSetType limitRecurrenceSet;
  private LimitFreebusySetType limitFreebusySet;

  /**
   * Sets the value of the expand property.
   *
   * @param val
   *     allowed object is
   *     {@link ExpandType }
   *
   */
  public void setExpand(final ExpandType val) {
      expand = val;
  }

  /**
   * Gets the value of the expand property.
   *
   * @return
   *     possible object is
   *     {@link ExpandType }
   *
   */
  public ExpandType getExpand() {
      return expand;
  }

  /**
   * Sets the value of the limitRecurrenceSet property.
   *
   * @param val
   *     allowed object is
   *     {@link LimitRecurrenceSetType }
   *
   */
  public void setLimitRecurrenceSet(final LimitRecurrenceSetType val) {
      limitRecurrenceSet = val;
  }

  /**
   * Gets the value of the limitRecurrenceSet property.
   *
   * @return
   *     possible object is
   *     {@link LimitRecurrenceSetType }
   *
   */
  public LimitRecurrenceSetType getLimitRecurrenceSet() {
      return limitRecurrenceSet;
  }

  /**
   * Sets the value of the limitFreebusySet property.
   *
   * @param val
   *     allowed object is
   *     {@link LimitFreebusySetType }
   *
   */
  public void setLimitFreebusySet(final LimitFreebusySetType val) {
      limitFreebusySet = val;
  }

  /**
   * Gets the value of the limitFreebusySet property.
   *
   * @return
   *     possible object is
   *     {@link LimitFreebusySetType }
   *
   */
  public LimitFreebusySetType getLimitFreebusySet() {
      return limitFreebusySet;
  }

  @Override
  public String toString() {
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

    return new ToString(this).append(name)
                             .append("start", start)
                             .append("end", end)
                             .toString();
  }
}
