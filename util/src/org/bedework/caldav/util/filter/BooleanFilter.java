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
package org.bedework.caldav.util.filter;

import java.util.Collection;

/** A filter that allows us to indicate true/false.
 *
 * @author Mike Douglass
 * @version 1.0
 */
public class BooleanFilter extends Filter {
  private boolean val;

  /** */
  public static final BooleanFilter falseFilter = new BooleanFilter(false);
  /** */
  public static final BooleanFilter trueFilter = new BooleanFilter(true);

  /**
   * @param val
   */
  public BooleanFilter(boolean val) {
    super("Boolean");
  }

  /**
   * @return boolean true/false
   */
  public boolean getValue() {
    return val;
  }

  /* ====================================================================
   *                   Object methods
   * ==================================================================== */

  public String toString() {
    StringBuilder sb = new StringBuilder("BooleanFilter{");

    super.toStringSegment(sb);
    sb.append(", value=");
    sb.append(getValue());

    Collection<Filter> c = getChildren();

    if (c != null) {
      for (Filter f: c) {
        sb.append("\n");
        sb.append(f);
      }
    }

    return sb.toString();
  }
}
