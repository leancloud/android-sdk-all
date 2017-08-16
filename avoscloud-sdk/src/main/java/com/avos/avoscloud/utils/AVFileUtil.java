package com.avos.avoscloud.utils;

import com.avos.avoscloud.AVFile;
import com.avos.avoscloud.AVUtils;

/**
 * Created by fengjunwen on 2017/8/16.
 */

public class AVFileUtil {
  public static String getFileMimeType(AVFile avFile) {
    String fileName = avFile.getName();
    String fileUrl = avFile.getUrl();
    String mimeType = AVFile.DEFAULTMIMETYPE;
    if (!AVUtils.isBlankString(fileName)) {
      mimeType = AVUtils.getMimeTypeFromLocalFile(fileName);
    } else if (!AVUtils.isBlankString(fileUrl)) {
      mimeType = AVUtils.getMimeTypeFromUrl(fileUrl);
    }
    return mimeType;
  }
}
