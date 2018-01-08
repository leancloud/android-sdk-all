package com.avos.avoscloud.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * Created by fengjunwen on 2018/1/8.
 */

public class StringUtils {
  public static String join(CharSequence delimiter,
                            Iterable<? extends CharSequence> elements) {
    if (null == delimiter || null == elements) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    boolean isFirstElem = true;
    for (CharSequence cs: elements) {
      if (!isFirstElem) {
        sb.append(delimiter);
      } else {
        isFirstElem = false;
      }
      sb.append(cs);
    }
    return sb.toString();
  }

  public static boolean isDigitString(String s) {
    if (s == null) return false;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (!Character.isDigit(c)) {
        return false;
      }
    }
    return true;
  }

  public static boolean isBlankString(String str) {
    return str == null || str.trim().equals("");
  }

  public static boolean isBlankJsonContent(String content) {
    return isBlankString(content) || content.trim().equals("{}");
  }

  public static String stringFromBytes(byte[] bytes) {
    try {
      return new String(bytes, "UTF-8");
    } catch (Exception e) {
      // e.printStackTrace();
    }
    return null;
  }

  public static String md5(String string) {
    byte[] hash = null;
    try {
      hash = string.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Huh,UTF-8 should be supported?", e);
    }
    return computeMD5(hash);
  }

  public static String computeMD5(byte[] input) {
    try {
      if (null == input) {
        return null;
      }
      MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(input, 0, input.length);
      byte[] md5bytes = md.digest();

      return hexEncodeBytes(md5bytes);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public static String hexEncodeBytes(byte[] md5bytes) {
    if (null == md5bytes) {
      return "";
    }
    StringBuffer hexString = new StringBuffer();
    for (int i = 0; i < md5bytes.length; i++) {
      String hex = Integer.toHexString(0xff & md5bytes[i]);
      if (hex.length() == 1) hexString.append('0');
      hexString.append(hex);
    }
    return hexString.toString();
  }

  static Random random = new Random(System.currentTimeMillis());

  public static String getRandomString(int length) {
    String letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    StringBuilder randomString = new StringBuilder(length);

    for (int i = 0; i < length; i++) {
      randomString.append(letters.charAt(random.nextInt(letters.length())));
    }

    return randomString.toString();
  }

}
