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
package org.bedework.caldav.util.sharing;

import org.bedework.base.ToString;

import java.util.ArrayList;
import java.util.List;

/** Class to represent a sharing request. The contained elements allow for the
 * addition or removal of sharing rights.
 *
 * @author Mike Douglass douglm
 */
public class ShareType {
  private List<SetType> set;
  private List<RemoveType> remove;

  /**
   * @return list of SetType - never null
   */
  public List<SetType> getSet() {
    if (set == null) {
      set = new ArrayList<>();
    }

    return set;
  }

  /**
   * @return list of RemoveType - never null
   */
  public List<RemoveType> getRemove() {
    if (remove == null) {
      remove = new ArrayList<>();
    }

    return remove;
  }

  /* ==============================================================
   *                   Convenience methods
   * ============================================================== */

  /** Add our stuff to the StringBuffer
   *
   * @param ts for output
   */
  protected void toStringSegment(final ToString ts) {
    ts.append("set", getSet());
    ts.append("remove", getRemove());
  }

  @Override
  public String toString() {
    final ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
