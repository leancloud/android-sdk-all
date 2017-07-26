package com.avos.avoscloud.test.mock;

import com.alibaba.fastjson.annotation.JSONType;
import com.avos.avoscloud.AVClassName;
import com.avos.avoscloud.AVObject;

/**
 * Created by zhangxiaobo on 15/3/13.
 */
@AVClassName("GameScore")
@JSONType(asm = false)
public class GameScore extends AVObject {
  public String name;
}
