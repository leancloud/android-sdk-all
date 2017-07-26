package com.avos.avoscloud;

/**
 * <p>
 * Represents a Role on the AVOSCloud server. AVRoles represent groupings of AVUsers for the
 * purposes of granting permissions (e.g. specifying a AVACL for a AVObject). Roles are specified by
 * their sets of child users and child roles, all of which are granted any permissions that the
 * parent role has.
 * </p>
 * <p>
 * Roles must have a name (which cannot be changed after creation of the role), and must specify an
 * ACL.
 * </p>
 */
public final class AVRole extends AVObject {
  private String name;
  public static final String className = "_Role";

  public static final String AVROLE_ENDPOINT = "roles";

  public AVRole() {
    super(className);
  }

  /**
   * Constructs a new AVRole with the given name. If no default ACL has been specified, you must
   * provide an ACL for the role.
   * 
   * @param name The name of the Role to create.
   */
  public AVRole(String name) {
    super(className);
    this.name = name;
    acl = PaasClient.storageInstance().getDefaultACL();
    if (acl == null) {
      throw new IllegalStateException(
          "There is no default ACL,please provide an ACL for the role with AVRole(String name, AVACL acl) constructor.");
    }
    this.put("name", name);
  }

  /**
   * Constructs a new AVRole with the given name.
   * 
   * @param name The name of the Role to create.
   * @param acl The ACL for this role. Roles must have an ACL.
   */
  public AVRole(String name, AVACL acl) {
    super(className);
    this.name = name;
    if (acl == null) {
      throw new IllegalArgumentException("Null ACL.");
    }
    this.acl = acl;
    this.put("name", name);
  }

  /**
   * Gets the name of the role.
   * 
   * @return the name of the role.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Gets a AVQuery over the Role collection.
   * 
   * @return A new query over the Role collection.
   */
  public static AVQuery<AVRole> getQuery() {
    AVQuery<AVRole> query =
        new AVQuery<AVRole>(AVPowerfulUtils.getAVClassName(AVRole.class.getSimpleName()));
    return query;
  }

  /**
   * Gets the AVRelation for the AVRoles that are direct children of this role. These roles' users
   * are granted any privileges that this role has been granted (e.g. read or write access through
   * ACLs). You can add or remove child roles from this role through this relation.
   * 
   * @return the relation for the roles belonging to this role.
   */
  public AVRelation getRoles() {
    return super.getRelation(AVROLE_ENDPOINT);
  }

  /**
   * Gets the AVRelation for the AVUsers that are direct children of this role. These users are
   * granted any privileges that this role has been granted (e.g. read or write access through
   * ACLs). You can add or remove users from the role through this relation.
   * 
   * @return the relation for the users belonging to this role.
   */
  public AVRelation getUsers() {
    return super.getRelation(AVUser.AVUSER_ENDPOINT);
  }

  /**
   * Add a key-value pair to this object. It is recommended to name keys in
   * partialCamelCaseLikeThis.
   * 
   * @param key Keys must be alphanumerical plus underscore, and start with a letter.
   * @param value Values may be numerical, String, JSONObject, JSONArray, JSONObject.NULL, or other
   *          AVObjects.
   */
  public void put(String key, Object value) {
    super.put(key, value);
  }

  /**
   * <p>
   * Sets the name for a role. This value must be set before the role has been saved to the server,
   * and cannot be set once the role has been saved.
   * </p>
   * <p>
   * A role's name can only contain alphanumeric characters, _, -, and spaces.
   * </p>
   * 
   * @param name The name of the role.
   */
  public void setName(String name) {
    this.name = name;
    this.put("name", name);
  }

  public static transient final Creator CREATOR = AVObjectCreator.instance;
}
