package com.avos.avoscloud;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.alibaba.fastjson.serializer.SerializerFeature;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * Created by lbt05 on 6/3/15.
 */
class AVObjectSerializer implements ObjectSerializer {
  public static final AVObjectSerializer instance = new AVObjectSerializer();

  @Override
  public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features)
      throws IOException {
    SerializeWriter out = serializer.getWriter();
    AVObject avObject = (AVObject) object;
    out.write('{');
    out.writeFieldValue(' ', "@type", avObject.getClass().getName());
    out.writeFieldValue(',', "objectId", avObject.getObjectId());
    out.writeFieldValue(',', "updatedAt", AVUtils.getAVObjectUpdatedAt(avObject));
    out.writeFieldValue(',', "createdAt", AVUtils.getAVObjectCreatedAt(avObject));
    String className = AVUtils.getAVObjectClassName(avObject.getClass());
    out.writeFieldValue(',', "className", className == null ? avObject.getClassName() : className);
    out.write(',');

    if (avObject instanceof AVStatus) {
      AVStatus status = (AVStatus) avObject;
      out.writeFieldName("dataMap");
      out.write(JSON.toJSONString(status.getData(), ObjectValueFilter.instance,
          SerializerFeature.WriteClassName,
          SerializerFeature.DisableCircularReferenceDetect));

      out.write(',');
      out.writeFieldName("inboxType");
      out.write(status.getInboxType());
      out.write(',');
      out.writeFieldName("messageId");
      out.write(Long.toString(status.getMessageId()));
      if (status.getSource() != null) {
        out.write(',');
        out.writeFieldName("source");
        out.write(JSON.toJSONString(status.getSource(), ObjectValueFilter.instance,
            SerializerFeature.WriteClassName,
            SerializerFeature.DisableCircularReferenceDetect));
      }
    } else {
      out.writeFieldName("serverData");
      out.write(JSON.toJSONString(avObject.serverData, ObjectValueFilter.instance,
          SerializerFeature.WriteClassName,
          SerializerFeature.DisableCircularReferenceDetect));
      if (!avObject.operationQueue.isEmpty()) {
        out.write(',');
        out.writeFieldName("operationQueue");
        out.write(JSON.toJSONString(avObject.operationQueue, ObjectValueFilter.instance,
            SerializerFeature.WriteClassName,
            SerializerFeature.DisableCircularReferenceDetect));
      }
    }
    out.write('}');
  }
}
