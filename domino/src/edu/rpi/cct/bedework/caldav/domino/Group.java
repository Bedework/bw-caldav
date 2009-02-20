/* **********************************************************************
    Copyright 2006 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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
package edu.rpi.cct.bedework.caldav.domino;

import edu.rpi.cmt.access.Ace;

import java.util.Collection;
import java.util.TreeSet;

/** Group object for access suite tests.
 *
 *   @author Mike Douglass douglm@rpi.edu
 *  @version 1.0
 */
public class Group extends Principal {
  /** members of the group
   */
  private Collection<Principal> groupMembers;

  /* ====================================================================
   *                   Constructors
   * ==================================================================== */

  /** Create a group
   */
  public Group() {
    super();
  }

  /** Create a group with a given name
   *
   * @param  account            String group account name
   */
  public Group(String account) {
    super(account);
  }

  public int getKind() {
    return Ace.whoTypeGroup;
  }

  /** Set the members of the group.
   *
   * @param   val     Collection of group members.
   */
  public void setGroupMembers(Collection<Principal> val) {
    groupMembers = val;
  }

  /** Return the members of the group.
   *
   * @return Collection        group members
   */
  public Collection<Principal> getGroupMembers() {
    if (groupMembers == null) {
      groupMembers = new TreeSet<Principal>();
    }
    return groupMembers;
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /** Return true if the account name is in the group members.
   *
   * @param account
   * @param group     boolean true if we're testing for a group.
   * @return true if the account name is in the group members.
   */
  public boolean isMember(String account, boolean group) {
    for (Principal mbr: getGroupMembers()) {
      if (mbr.getAccount().equals(account)) {
        if (group == (mbr instanceof Group)) {
          return true;
        }
      }
    }

    return false;
  }

  /** Add a group member. Return true if is was added, false if it was in
   * the list
   *
   * @param mbr        Principal to add
   * @return boolean   true if added
   */
  public boolean addGroupMember(Principal mbr) {
    return getGroupMembers().add(mbr);
  }

  /** Remove a group member. Return true if is was removed, false if it was
   * not in the list
   *
   * @param mbr        Principal to remove
   * @return boolean   true if removed
   */
  public boolean removeGroupMember(Principal mbr) {
    return getGroupMembers().remove(mbr);
  }

  protected void toStringSegment(StringBuffer sb) {
    super.toStringSegment(sb);
    sb.append(", groupMembers={");
    boolean first = true;

    for (Principal mbr: getGroupMembers()) {
      String name = "";
      if (first) {
        name = null;
        first = false;
      }
      Principal.toStringSegment(sb, name, mbr);
    }

    sb.append("}");
  }

  /* ====================================================================
   *                   Object methods
   * ==================================================================== */

  public String toString() {
    StringBuffer sb = new StringBuffer();

    sb.append("Group{");
    toStringSegment(sb);
    sb.append("}");

    return sb.toString();
  }
}
