package com.avos.avoscloud;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.JSONToken;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.avos.avoscloud.ops.AVOp;
import com.avos.avoscloud.ops.AddOp;
import com.avos.avoscloud.ops.AddRelationOp;
import com.avos.avoscloud.ops.AddUniqueOp;
import com.avos.avoscloud.ops.CollectionOp;
import com.avos.avoscloud.ops.CompoundOp;
import com.avos.avoscloud.ops.NullOP;
import com.avos.avoscloud.ops.RemoveOp;
import com.avos.avoscloud.ops.RemoveRelationOp;
import com.avos.avoscloud.ops.SingleValueOp;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by lbt05 on 6/4/15.
 */
class AVObjectDeserializer implements ObjectDeserializer {
  static final String LOG_TAG = AVObjectDeserializer.class.getSimpleName();
  public static final AVObjectDeserializer instance = new AVObjectDeserializer();

  @Override
  public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
    if (AVObject.class.isAssignableFrom((Class) type)) {
      Map<String, Object> objectMap = new HashMap<String, Object>();
      parser.parseObject(objectMap);
      AVObject object = null;
      try {
        object = (AVObject) ((Class) type).newInstance();
        object.setClassName((String) objectMap.get("className"));
        object.setObjectId((String) objectMap.get("objectId"));
        object.setCreatedAt((String) objectMap.get("createdAt"));
        object.setUpdatedAt((String) objectMap.get("updatedAt"));
        if (objectMap.containsKey("keyValues")) {
          // this is for the old json str cache
          Map<String, com.alibaba.fastjson.JSONObject> keyValues =
              (Map<String, com.alibaba.fastjson.JSONObject>) objectMap.get("keyValues");
          for (Map.Entry<String, JSONObject> entry : keyValues.entrySet()) {
            com.alibaba.fastjson.JSONObject keyValue = entry.getValue();
            Object values = keyValue.get("value");
            AVOp op = null;
            // 避免出现op是空的jsonObject时的出现的cast异常问题
            try {
              op = (AVOp) keyValue.get("op");
            } catch (Exception e) {

            }
            if (op != null && !(op instanceof NullOP)) {
              op = this.updateOp(op, values);
              object.operationQueue.put(entry.getKey(), op);
            } else if (!keyValue.containsKey("relationClassName")) {
              object.serverData.put(entry.getKey(), values);
            } else if (keyValue.containsKey("relationClassName")) {
              AVRelation relation = new AVRelation(object, entry.getKey());
              relation.setTargetClass(keyValue.getString("relationClassName"));
              object.serverData.put(entry.getKey(), relation);
            }
          }
        } else {
          if (objectMap.containsKey("operationQueue")) {
            object.operationQueue.putAll((Map<String, com.avos.avoscloud.ops.AVOp>) objectMap
                .get("operationQueue"));
          }
          if (objectMap.containsKey("serverData")) {
            object.serverData.putAll((Map<String, Object>) objectMap.get("serverData"));
          }

          if (AVStatus.class.isAssignableFrom((Class) type)) {
            AVStatus status = AVObject.cast(object, AVStatus.class);
            if (objectMap.containsKey("dataMap")) {
              status.setData((Map<String, Object>) objectMap.get("dataMap"));
            }
            if (objectMap.containsKey("inboxType")) {
              status.setInboxType((String) objectMap.get("inboxType"));
            }
            if (objectMap.containsKey("messageId")) {
              status.setMessageId((Long) objectMap.get("messageId"));
            }
            if (objectMap.containsKey("source")) {
              status.setSource((AVObject) objectMap.get("source"));
            }
          }
        }
        object.rebuildInstanceData();
      } catch (InstantiationException e) {
        if (AVOSCloud.isDebugLogEnabled()) {
          LogUtil.log.e(LOG_TAG, "", e);
        }
      } catch (IllegalAccessException e) {
        if (AVOSCloud.isDebugLogEnabled()) {
          LogUtil.log.e(LOG_TAG, "", e);
        }
      } catch (Exception e) {
        if (AVOSCloud.isDebugLogEnabled()) {
          LogUtil.log.e(LOG_TAG, "", e);
        }
      } finally {
        return (T) object;
      }
    }
    return (T) parser.parseObject();
  }

  @Override
  public int getFastMatchToken() {
    return JSONToken.LBRACE;
  }

  public AVOp updateOp(AVOp op, Object values) {
    if (op instanceof AddRelationOp || op instanceof RemoveRelationOp) {
      try {
        Set<JSONObject> opValues = (Set<JSONObject>) op.getValues();
        Set<AVObject> objects = new HashSet<AVObject>();
        for (JSONObject o : opValues) {
          AVObject object = JSONObject.parseObject(o.toString(), AVObject.class);
          objects.add(object);
        }
        ((CollectionOp) op).setValues(objects);
      } catch (Exception e) {}
    }
    if (op instanceof CompoundOp) {
      List<AVOp> ops = (List<AVOp>) op.getValues();
      for (AVOp singleOp : ops) {
        updateOp(singleOp, null);
      }
    }
    return op;
  }
}
