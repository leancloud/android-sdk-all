package com.avos.avoscloud;

import java.util.HashMap;
import java.util.Map;


/**
 * <p>
 * A AVACL is used to control which users can access or modify a particular object. Each AVObject
 * can have its own AVACL. You can grant read and write permissions separately to specific users, to
 * groups of users that belong to roles, or you can grant permissions to "the public" so that, for
 * example, any user could read a particular object but only a particular set of users could write
 * to that object.
 * </p>
 */
public class AVACL {
  private final Map<String, Object> permissionsById;
  private static String readTag = "read";
  private static String writeTag = "write";
  private static String publicTag = "*";
  private static String rolePrefix = "role:";
  static final String ACL_ATTR_NAME = "ACL";

  /**
   * Creates an ACL with no permissions granted.
   */
  public AVACL() {
    permissionsById = new HashMap<String, Object>();
  }

  /**
   * Create an ACL with permissions
   * @param aclMap
   */
  public AVACL(Map<String, Object> aclMap) {
    permissionsById = new HashMap<String, Object>();
    if (null != aclMap) {
      permissionsById.putAll(aclMap);
    }
  }

  AVACL(AVACL right) {
    permissionsById = new HashMap<String, Object>();
    permissionsById.putAll(right.permissionsById);
  }

  /**
   * Creates an ACL where only the provided user has access.
   * 
   * @param owner The only user that can read or write objects governed by this ACL.
   */
  public AVACL(AVUser owner) {
    permissionsById = new HashMap<String, Object>();
    setReadAccess(owner, true);
    setWriteAccess(owner, true);
  }

  private Map<String, Object> mapForKey(String key, boolean create) {
    Map<String, Object> map = (Map) permissionsById.get(key);
    if (map == null && create) {
      map = new HashMap<String, Object>();
      this.permissionsById.put(key, map);
    }
    return map;
  }

  private void allowRead(boolean allowed, String key) {
    Map<String, Object> map = mapForKey(key, allowed);
    if (allowed) {
      map.put(readTag, true);
    } else if (map != null) {
      map.remove(readTag);
    }
  }

  private boolean isReadAllowed(String key) {
    Map<String, Object> map = mapForKey(key, false);
    return map != null && ((Boolean) map.get(readTag)) != null
        && ((Boolean) map.get(readTag)).booleanValue();
  }

  private void allowWrite(boolean allowed, String key) {
    Map<String, Object> map = mapForKey(key, allowed);
    if (allowed) {
      map.put(writeTag, allowed);
    } else if (map != null) {
      map.remove(writeTag);
    }
  }

  private boolean isWriteAllowed(String key) {
    Map<String, Object> map = mapForKey(key, false);
    return map != null && ((Boolean) map.get(writeTag)) != null
        && ((Boolean) map.get(writeTag)).booleanValue();
  }

  /**
   * Get whether the public is allowed to read this object.
   */
  public boolean getPublicReadAccess() {
    return isReadAllowed(publicTag);
  }

  /**
   * Get whether the public is allowed to write this object.
   */
  public boolean getPublicWriteAccess() {
    return isWriteAllowed(publicTag);
  }

  /**
   * Get whether the given user id is *explicitly* allowed to read this object. Even if this returns
   * false, the user may still be able to access it if getPublicReadAccess returns true or a role
   * that the user belongs to has read access.
   */
  public boolean getReadAccess(AVUser user) {
    return getReadAccess(user.getObjectId());
  }

  /**
   * Get whether the given user id is *explicitly* allowed to read this object. Even if this returns
   * false, the user may still be able to access it if getPublicReadAccess returns true or a role
   * that the user belongs to has read access.
   */
  public boolean getReadAccess(String userId) {
    return isReadAllowed(userId);
  }

  private String roleName(String name) {
    return String.format("role:%s", name);
  }

  /**
   * Get whether users belonging to the given role are allowed to read this object. Even if this
   * returns false, the role may still be able to read it if a parent role has read access. The role
   * must already be saved on the server and its data must have been fetched in order to use this
   * method.
   * 
   * @param role The role to check for access.
   * @return true if the role has read access. false otherwise.
   */
  public boolean getRoleReadAccess(AVRole role) {
    String r = roleName(role.getName());
    return getRoleReadAccess(r);
  }

  /**
   * Get whether users belonging to the role with the given roleName are allowed to read this
   * object. Even if this returns false, the role may still be able to read it if a parent role has
   * read access.
   * 
   * @param roleName The name of the role.
   * @return true if the role has read access. false otherwise.
   */
  public boolean getRoleReadAccess(String roleName) {
    return isReadAllowed(roleName);
  }

  /**
   * 
   * Get whether users belonging to the given role are allowed to write this object. Even if this
   * returns false, the role may still be able to write it if a parent role has write access. The
   * role must already be saved on the server and its data must have been fetched in order to use
   * this method.
   * 
   * @param role The role to check for access.
   * @return true if the role has write access. false otherwise.
   */
  public boolean getRoleWriteAccess(AVRole role) {
    String r = roleName(role.getName());
    return getRoleWriteAccess(r);
  }

  /**
   * Get whether users belonging to the role with the given roleName are allowed to write this
   * object. Even if this returns false, the role may still be able to write it if a parent role has
   * write access.
   * 
   * @param roleName - The name of the role.
   * @return true if the role has write access. false otherwise.
   */
  public boolean getRoleWriteAccess(String roleName) {
    return isWriteAllowed(roleName);
  }

  /**
   * Get whether the given user id is *explicitly* allowed to write this object. Even if this
   * returns false, the user may still be able to write it if getPublicWriteAccess returns true or a
   * role that the user belongs to has write access.
   */
  public boolean getWriteAccess(AVUser user) {
    return getWriteAccess(user.getObjectId());
  }

  /**
   * Get whether the given user id is *explicitly* allowed to write this object. Even if this
   * returns false, the user may still be able to write it if getPublicWriteAccess returns true or a
   * role that the user belongs to has write access.
   */
  public boolean getWriteAccess(String userId) {
    return isWriteAllowed(userId);
  }

  /**
   * Sets a default ACL that will be applied to all AVObjects when they are created.
   * 
   * @param acl The ACL to use as a template for all AVObjects created after setDefaultACL has been
   *          called. This value will be copied and used as a template for the creation of new ACLs,
   *          so changes to the instance after setDefaultACL() has been called will not be reflected
   *          in new AVObjects.
   * @param withAccessForCurrentUser If true, the AVACL that is applied to newly-created AVObjects
   *          will provide read and write access to the AVUser.getCurrentUser() at the time of
   *          creation. If false, the provided ACL will be used without modification. If acl is
   *          null,
   *          this value is ignored.
   */
  public static void setDefaultACL(AVACL acl, boolean withAccessForCurrentUser) {
    if (acl == null) throw new IllegalArgumentException("Null ACL.");
    PaasClient.storageInstance().setDefaultACL(acl);
    if (withAccessForCurrentUser) {
      AVUser user = AVUser.getCurrentUser();
      AVACL defaultACL = PaasClient.storageInstance().getDefaultACL();
      defaultACL.setReadAccess(user, true);
      defaultACL.setWriteAccess(user, true);
    }
  }

  /**
   * Construct a AVACL object with public read/write permissions
   * 
   * @param read whether the public is allowed to read this object
   * @param write whether the public is allowed to write this object
   * @return
   */
  public static AVACL parseACLWithPublicAccess(boolean read, boolean write) {
    AVACL acl = new AVACL();
    acl.setPublicReadAccess(read);
    acl.setPublicWriteAccess(write);
    return acl;
  }

  /**
   * Set whether the public is allowed to read this object.
   */
  public void setPublicReadAccess(boolean allowed) {
    allowRead(allowed, publicTag);
  }

  /**
   * Set whether the public is allowed to write this object.
   */
  public void setPublicWriteAccess(boolean allowed) {
    allowWrite(allowed, publicTag);
  }

  /**
   * Set whether the given user id is allowed to read this object.
   */
  public void setReadAccess(AVUser user, boolean allowed) {
    setReadAccess(user.getObjectId(), allowed);
  }

  /**
   * Set whether the given user is allowed to read this object.
   */
  public void setReadAccess(String userId, boolean allowed) {
    allowRead(allowed, userId);
  }

  /**
   * Set whether users belonging to the given role are allowed to read this object. The role must
   * already be saved on the server and its data must have been fetched in order to use this method.
   * 
   * @param role The role to assign access.
   * @param allowed Whether the given role can read this object.
   */
  public void setRoleReadAccess(AVRole role, boolean allowed) {
    setRoleReadAccess(role.getName(), allowed);
  }

  /**
   * Set whether users belonging to the role with the given roleName are allowed to read this
   * object.
   * 
   * @param roleName The name of the role.
   * @param allowed Whether the given role can read this object.
   */
  public void setRoleReadAccess(String roleName, boolean allowed) {
    allowRead(allowed, getRoleTagKey(roleName));
  }

  /**
   * Set whether users belonging to the given role are allowed to write this object. The role must
   * already be saved on the server and its data must have been fetched in order to use this method.
   * 
   * @param role The role to assign access.
   * @param allowed Whether the given role can write this object.
   */
  public void setRoleWriteAccess(AVRole role, boolean allowed) {
    setRoleWriteAccess(role.getName(), allowed);
  }

  /**
   * Set whether users belonging to the role with the given roleName are allowed to write this
   * object.
   * 
   * @param roleName The name of the role.
   * @param allowed Whether the given role can write this object.
   */
  public void setRoleWriteAccess(String roleName, boolean allowed) {
    allowWrite(allowed, getRoleTagKey(roleName));
  }

  /**
   * Set whether the given user is allowed to write this object.
   */
  public void setWriteAccess(AVUser user, boolean allowed) {
    setWriteAccess(user.getObjectId(), allowed);
  }

  /**
   * Set whether the given user id is allowed to write this object.
   */
  public void setWriteAccess(String userId, boolean allowed) {
    allowWrite(allowed, userId);
  }

  Map<String, Object> getPermissionsById() {
    return permissionsById;
  }

  public Map<String, Object> getACLMap() {
    Map<String, Object> aclMap = new HashMap<String, Object>();
    aclMap.put(ACL_ATTR_NAME, getPermissionsById());
    return aclMap;
  }

  private static String getRoleTagKey(String roleName) {
    if (!AVUtils.isBlankString(roleName) && roleName.startsWith(rolePrefix)) {
      return roleName;
    }
    StringBuilder sb = new StringBuilder(rolePrefix);
    sb.append(roleName);
    return sb.toString();
  }
}
