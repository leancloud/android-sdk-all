package com.avos.avoscloud;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by wli on 2017/5/11.
 */

public class AVSMSOption {

  private String applicationName;

  private String operation;

  private AVSMS_TYPE smsType = null;

  private String templateName;

  private String signatureName;

  private Map<String, Object> envMap;

  private String validationToken;

  private int ttl;

  Map<String, Object> getOptionMaps() {
    Map<String, Object> map = new HashMap<>();
    fillMap("name", applicationName, map);
    fillMap("op", operation, map);
    fillMap("template", templateName, map);
    fillMap("sign", signatureName, map);
    fillMap("ttl", ttl, map);
    fillMap("validate_token", validationToken, map);

    if (null != smsType) {
      fillMap("smsType", smsType.toString(), map);
    }

    if (null != envMap && !envMap.isEmpty()) {
      map.putAll(envMap);
    }

    return map;
  }

  private Map<String, Object> fillMap(String key, String value, Map<String, Object> map) {
    if (!AVUtils.isBlankString(value)) {
      map.put(key, value);
    }
    return map;
  }

  private Map<String, Object> fillMap(String key, int value, Map<String, Object> map) {
    if (value > 0) {
      map.put(key, value);
    }
    return map;
  }

  /**
   * set the application name showed in the message
   * @param applicationName
   */
  public void setApplicationName(String applicationName) {
    this.applicationName = applicationName;
  }

  /**
   * set the operation showed in the message.
   * @param operation
   */
  public void setOperation(String operation) {
    this.operation = operation;
  }

  /**
   * set the template that has beed created
   * @param templateName
   */
  public void setTemplateName(String templateName) {
    this.templateName = templateName;
  }

  /**
   * set the signature name for the message
   * @param signatureName
   */
  public void setSignatureName(String signatureName) {
    this.signatureName = signatureName;
  }

  /**
   * set the map that will fill in template placeholders.
   * @param envMap
   */
  public void setEnvMap(Map<String, Object> envMap) {
    this.envMap = envMap;
  }

  /**
   * set the time to live of validation information
   * @param ttl
   */
  public void setTtl(int ttl) {
    this.ttl = ttl;
  }

  /**
   * set the sms type
   * @param smsType
   */
  public void setSmsType(AVSMS_TYPE smsType) {
    this.smsType = smsType;
  }

  /**
   * set the token used to validate the message request
   * @param validationToken
   */
  public void setValidationToken(String validationToken) {
    this.validationToken = validationToken;
  }

  public enum AVSMS_TYPE {
    VOICE_SMS("voice"), TEST_SMS("sms");

    private String name;

    AVSMS_TYPE(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return this.name;
    }
  }
}
