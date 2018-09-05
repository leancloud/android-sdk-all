# LeanCloud Android SDK

## Announcements

从 2018 年 9 月开始，我们新推出了一个 [统一 SDK（Java Unified SDK）](/leancloud/java-sdk-all)，新版本 SDK 可以兼容 Java / Android / 云引擎 三种运行环境和平台，今后 LeanCloud Android 开发团队会主要维护 Java Unified SDK，本 SDK 则还会被继续维护一年的时间（截止到 2019 年 9 月底，期间主要是对既存问题进行修复，不会再增加新功能），请大家尽快迁移到新版本 SDK 上。

## TODO
* Android 编译的 targetSDK 确认：我发现最低 10，最高 19。
* 模块名称统一：有的是 avoscloud 前缀有的是 avos 前缀。
* 测试组件名字要不要改改？paas_unit_test_application 有点尴尬。
* 自动化运维相关脚本需要补充。

## 简介
项目包括：

* avoscloud：核心 SDK
* avoscloudfeedback：反馈组件
* avospush：推送组件
* avossearch：应用内搜索组件
* avossns：社交组件
* avosstatistics：统计组件
* avoscloud-mixpush：混合推送组件

测试组件包括：

* paas_unit_test_application
* 通过 [Robolectric](http://robolectric.org/) 对项目进行单元测试，测试代码位于每个项目的 src/androidTest
* [如何编写调试单元测试](https://github.com/leancloud/android-sdk/wiki/%E5%A6%82%E4%BD%95%E7%BC%96%E5%86%99%E8%B0%83%E8%AF%95%E5%8D%95%E5%85%83%E6%B5%8B%E8%AF%95)

## 编译

项目使用 [Gradle](http://www.gradle.org/) v2.2.1 管理，所以需要先在本地安装 Gradle，或者使用项目的 `gradle wrapper`

### 关于 gradle wrapper

主页在 [这里](http://www.gradle.org/docs/current/userguide/gradle_wrapper.html)，简单来说它可以帮你自动下载合适的 gradle 并安装。涉及到几个文件：

```
simple/
  gradlew <- *nix 运行脚本
  gradlew.bat <- Windows 运行脚本
  gradle/wrapper/
    gradle-wrapper.jar
    gradle-wrapper.properties <- 一些配置，比如 gradle 下载地址及安装地址
```

第一次执行 gradlew （注意末尾有个 `w` 表明是 wapper）时，可能会出现下面的情况：

```
> ./gradlew build
Downloading https://services.gradle.org/distributions/gradle-2.2.1-bin.zip
.............................................................................
```

说明本地没有 gradle，正在从服务器下载并安装，这个过程耗时可能比较长（一个 40MB 的包），注意有可能下载的是一个 `gradle-2.2.1-all.zip` 而不是 `gradle-2.2.1-bin.zip`，这个耗时会更长（56MB 的包）。所以如果想快一点，可以修改 `gradle-wrapper.properties`，尽量下载 `bin` 而不是 `all`。

gradle 下载完成后，默认情况保存路径：`$HOME/.gradle/wrapper/dists/gradle-2.2.1-bin/88n1whbyjvxg3s40jzz5ur27/gradle-2.2.1`。

**提示**：不知道什么原因，gradle 可能会下多个相同版本的程序，保存在不同的 Hash 目录中（比如你看到我当前是 `88n1` 开头的这个目录），但程序是一模一样的。所以，你可能会弄得很恼火……所以建议还是自己安装和配置 gradle 环境。

### 打包

执行下面命令可以打包项目：

```
./build-sdk-noupload.sh 1.0.0
```
> 1.0.0 指版本号，可任意指定

如果你看到下面的提示，就代表打包成功：

```
BUILD SUCCESSFUL

Total time: 27.543 secs
```

可以到 `./build/release-1.0.0` 目录下查看打包出来的文件。

对于 Android 组件包，产出分两种：

#### jar 包

就是将组件所有 Java 文件打成的 jar 包，保存在如下目录：

```
./$MODULE/build/libs/bundles/$ARTIFACT_ID-$VERSION.jar
```
这个文件其实来自下面这个文件

```
./$MODULE/build/intermediates/bundles/{debug,release}/classes.jar
```

是通过每个模块的 `build.gradle` 文件中的任务控制的：

```
android.libraryVariants.all { variant ->
    def name = variant.buildType.name
    def task = project.tasks.create "jar${name.capitalize()}", Jar
    task.dependsOn variant.javaCompile
    task.from variant.javaCompile.destinationDir
    artifacts.add('archives', task);
}
```

#### aar 包

关于 aar 包的介绍在 [这里](http://tools.android.com/tech-docs/new-build-system/aar-format)，简单来说就是将 Android 组件的 Class 及资源一起打包，应用依赖时会将资源也解压缩，并且会做合并。具体示例在 [Feedback 引用 arr 包](#) 处解释。

arr 包保存路径：

```
./$MODULE/build/outputs/aar/avoscloud-{debug,release}.aar
```

### 文档打包

TODO

### 单元测试及代码覆盖率统计
执行下面命令可以进行单元测试并统计代码覆盖率：

```
./gradlew clean test jacoco
```
测试结果保存在:

```
./$MODULE/build/test-report/index.html
```
Jacoco 生成的代码覆盖率统计保存在:

```
./$MODULE/build/reports/jacoco/jacocoTestReport/html/index.html
```

## 导入 Android Studio

直接在 Android Studio 使用 File -> import project 即可。

## 发布

### snapshot 和 release

TODO

如果确认开发和测试都已经完成，可以将包发布到仓库，供用户下载：

```
./gradlew uploadArchives
```
在执行过程中，你能看到很多这样的提示信息：

```
:avoscloud:uploadArchives
Uploading: cn/leancloud/android/avoscloud-sdk/2.6.9.5-SNAPSHOT/avoscloud-sdk-2.6.9.5-20150109.065448-1.jar to repository remote at http://maven.mei.fm/nexus/content/repositories/snapshots
Transferring 320K from remote
Uploaded 320K
Uploading: cn/leancloud/android/avoscloud-sdk/2.6.9.5-SNAPSHOT/avoscloud-sdk-2.6.9.5-20150109.065448-1.aar to repository remote at http://maven.mei.fm/nexus/content/repositories/snapshots
Transferring 297K from remote
Uploaded 297K
```
说明此刻正在上传打好的 `jar` 和 `aar` 包。

**提示**：如果要上传包到仓库，需要有仓库部署权限的账号密码，一般该信息保存在 `$HOME/.gradle/gradle.properties` 中，内容如下：

```
NEXUS_USERNAME=admin
NEXUS_PASSWORD=xxxxxxxx
```

需要部署权限的，找 SA 领取。

### 自动化发布
TODO

