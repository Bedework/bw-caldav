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
package org.bedework.caldav.util.notifications;

import org.bedework.util.misc.ToString;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.tagdefs.BedeworkServerTags;

import java.util.ArrayList;
import java.util.List;

/**
       <!ELEMENT processors processor*>
 *
 * @author Mike Douglass douglm
 */
public class ProcessorsType {
  private List<ProcessorType> processor;

  /**
   * @return the processor list
   */
  public List<ProcessorType> getProcessor() {
    if (processor == null) {
      processor = new ArrayList<>();
    }

    return processor;
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /**
   * @param xml
   * @throws Throwable
   */
  public void toXml(final XmlEmit xml) throws Throwable {
    xml.openTag(BedeworkServerTags.processors);
    for (ProcessorType p: getProcessor()) {
      p.toXml(xml);
    }
    xml.closeTag(BedeworkServerTags.processors);
  }

  /** Add our stuff to the StringBuffer
   *
   * @param ts for output
   */
  protected void toStringSegment(final ToString ts) {
    ts.append("processor", getProcessor(), true);
  }

  @Override
  public String toString() {
    final ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
