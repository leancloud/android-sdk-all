package com.avos.avoscloud;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

class UrlDirectlyUploader extends HttpClientUploader {

  AVFile avFile;

  protected UrlDirectlyUploader(AVFile avFile, SaveCallback saveCallback,
                                ProgressCallback progressCallback) {
    super(saveCallback, progressCallback);
    this.avFile = avFile;
  }

  @Override
  public AVException doWork() {

    final AVException[] exceptionSaveFile = new AVException[1];
    PaasClient.storageInstance().postObject(AVPowerfulUtils.getEndpoint(avFile, true),

        getFileRequestParameters(), true, new GenericObjectCallback() {
          @Override
          public void onSuccess(String content, AVException e) {
            if (e == null) {
              try {
                JSONObject jsonObject = new JSONObject(content);
                avFile.handleUploadedResponse(jsonObject.getString("objectId"),
                    jsonObject.getString("objectId"), avFile.getUrl());
                publishProgress(100);
              } catch (Exception ex) {
                exceptionSaveFile[0] = new AVException(ex);
              }
            } else {
              exceptionSaveFile[0] = AVErrorUtils.createException(e, content);
            }
          }

          @Override
          public void onFailure(Throwable error, String content) {
            exceptionSaveFile[0] = AVErrorUtils.createException(error, content);
          }

        });

    return exceptionSaveFile[0];
  }

  private String getFileRequestParameters() {
    Map<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("name", avFile.getName());
    parameters.put("mime_type", avFile.mimeType());
    parameters.put("metaData", avFile.getMetaData());
    parameters.put("__type", AVFile.className());
    parameters.put("url", avFile.getUrl());
    if (avFile.getACL() != null) {
      parameters.putAll(AVUtils.getParsedMap(avFile.getACL().getACLMap()));
    }
    return AVUtils.restfulServerData(parameters);
  }

}
