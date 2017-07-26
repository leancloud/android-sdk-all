package com.avos.avoscloud.test.mock;

import com.alibaba.fastjson.annotation.JSONType;
import com.avos.avoscloud.AVClassName;
import com.avos.avoscloud.AVObject;

/**
 * Created by zhangxiaobo on 15/3/16.
 */
@AVClassName("Game")
@JSONType(asm = false)
public class Game extends AVObject {
  public Game() {}

  public Game(String name) {
    super();
    setName(name);
  }

  public String getName() {
    return getString("name");
  }

  public void setName(String value) {
    put("name", value);
  }
}
