#!/bin/bash

export PATH=$PATH:/usr/local/bin

test -f common.function && source common.function || exit 2

set -e

if [ $# != 1 ] ; then
  echo "USAGE: $0 VERSION"
  echo " e.g.: $0 1.4.4"
  exit 1;
fi
version=$1
echo "Building sdk $version..."
##replace VERSIONrsions
sed -i '' "s/VERSION_NAME=.*/VERSION_NAME=$version/" gradle.properties
sed -i '' "s/sdkVersion = .*;/sdkVersion = \"$version\";/" avoscloud-sdk/src/main/java/com/avos/avoscloud/PaasClient.java

sed -i '' "s/include ':Statistics'//" settings.gradle
sed -i '' "s/include ':paas_unit_test_application'//" settings.gradle

gradle clean androidReleaseJar uploadArchives

releaseDir="build/release-$version"

rm -rf "$releaseDir"
mkdir "$releaseDir"
mkdir -p "$releaseDir/avoscloud-feedback/libs"
mkdir -p "$releaseDir/avoscloud-search/libs/"
mkdir -p "$releaseDir/avoscloud-sns/libs/"

cp avoscloud-sdk/build/libs/avoscloud-sdk-*.jar $releaseDir/

cp avoscloud-push/build/libs/avoscloud-push-*.jar $releaseDir/

cp avoscloud-mixpush/build/libs/avoscloud-mixpush-*.jar $releaseDir/
cp avoscloud-mixpush/libs/HwPush_SDK_*.jar $releaseDir/
cp avoscloud-mixpush/libs/MiPush_SDK_Client_*.jar $releaseDir/

cp avoscloud-gcm/build/libs/avoscloud-gcm-*.jar $releaseDir/

cp avoscloud-statistics/build/libs/avoscloud-statistics-*.jar $releaseDir/

cp avoscloud-sns/build/libs/avoscloud-sns-*.jar $releaseDir/
cp -r avoscloud-sns/src/main/res $releaseDir/avoscloud-sns/
cp -r avoscloud-sns/build/libs/avoscloud-sns-*.jar $releaseDir/avoscloud-sns/libs
cd $releaseDir && zip -r avoscloud-sns-${version}.zip avoscloud-sns && rm -rf avoscloud-sns || fail_and_exit "$0"
cd -

cp avoscloud-feedback/build/libs/avoscloud-feedback-*.jar $releaseDir/avoscloud-feedback/libs
#cp avoscloud/build/libs/avoscloud-*.jar $releaseDir/avosfeedback/libs
#cp libs/android-async-http-*.jar $releaseDir/avosfeedback/libs
#cp libs/fastjson.jar $releaseDir/avosfeedback/libs
#cp libs/httpmime-4.2.4.jar $releaseDir/avosfeedback/libs
cp -r avoscloud-feedback/src/main/res $releaseDir/avoscloud-feedback/
cd $releaseDir && zip -r avoscloud-feedback-${version}.zip avoscloud-feedback && rm -rf avoscloud-feedback || fail_and_exit "$0"
cd -

cp avoscloud-search/build/libs/avoscloud-search-*.jar $releaseDir/avoscloud-search/libs/
cp avoscloud-search/build/libs/avoscloud-search-*.jar $releaseDir/
cp -r avoscloud-search/src/main/res $releaseDir/avoscloud-search/
cd $releaseDir && zip -r avoscloud-search-${version}.zip avoscloud-search && rm -rf avoscloud-search ||fail_and_exit "$0"
cd -
cp -rf libs/*  $releaseDir/

echo "Build sdk $version done!"

