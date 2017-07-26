package com.avos.avoscloud;

/**
 * Created by wli on 2017/5/9.
 */
public class AVCaptchaOption {

  /**
   * 宽度，单位是像素，下面的高度也是，会根据 size 自动调整，默认 85，范围在 60 - 200
   */
  private int width = 0;

  /**
   * 高度，默认值30，范围在 30 - 100
   */
  private int height = 0;

  int getWidth() {
    return width;
  }

  /**
   * Width of captcha image, in pixels.
   * Defaults to 85. Minimum is 85, maximum is 200.
   * @param width
   */
  public void setWidth(int width) {
    this.width = width;
  }

  int getHeight() {
    return height;
  }

  /**
   * Height of captcha image, in pixels.
   * Defaults to 30. Minimum is 30, maximum is 100.
   * @param height
   */
  public void setHeight(int height) {
    this.height = height;
  }
}
