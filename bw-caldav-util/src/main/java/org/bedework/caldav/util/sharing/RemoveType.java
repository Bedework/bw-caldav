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

/** Class to represent removal of access rights in a sharing request.
 *
 * @author Mike Douglass douglm
 */
public class RemoveType {
  private String href;

  /**
   * @param val the href
   */
  public void setHref(final String val) {
    href = val;
  }

  /**
   * @return the href
   */
  public String getHref() {
    return href;
  }

  /* ==============================================================
   *                   Convenience methods
   * ============================================================== */

  /** Add our stuff to the StringBuffer
   *
   * @param ts for output
   */
  protected void toStringSegment(final ToString ts) {
    ts.append("href", getHref());
  }

  @Override
  public String toString() {
    final ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
