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

import org.bedework.util.misc.ToString;

import java.util.ArrayList;
import java.util.List;

/** Class to represent a sharing request. The contained elements allow for the
 * addition or removal of sharing rights.
 *
 * @author Mike Douglass douglm
 */
public class ShareResultType {
  private List<String> goodSharees = new ArrayList<String>();
  private List<String> badSharees = new ArrayList<String>();

  /**
   * @return list of goodSharees - never null
   */
  public List<String> getGoodSharees() {
    if (goodSharees == null) {
      goodSharees = new ArrayList<String>();
    }

    return goodSharees;
  }

  /**
   * @return list of badSharees - never null
   */
  public List<String> getBadSharees() {
    if (badSharees == null) {
      badSharees = new ArrayList<String>();
    }

    return badSharees;
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /**
   * @param val
   */
  public void addGood(final String val) {
    getGoodSharees().add(val);
  }

  /**
   * @param val
   */
  public void addBad(final String val) {
    getBadSharees().add(val);
  }

  /** Add our stuff to the StringBuffer
   *
   * @param ts
   */
  protected void toStringSegment(final ToString ts) {
    ts.append("good", getGoodSharees());
    ts.append("bad", getBadSharees());
  }

  @Override
  public String toString() {
    ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
