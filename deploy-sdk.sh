#!/bin/bash

export PATH=$PATH:/usr/local/bin
test -f common.function && source common.function || exit 2

if hostname | grep -iE '(avos|builder)' > /dev/null 2>&1 ; then
  release_sdk_path="/Users/builder/leancloud/jenkins/workspace/avoscloud-Android-sdk-release/"
  release_doc_path="/Users/builder/leancloud/jenkins/workspace/api-docs"
  export ANDROID_HOME="/Users/builder/leancloud/android-sdk-macosx"
elif hostname | grep -i hj > /dev/null 2>&1 ; then
  release_sdk_path="/Users/hong/avos/code/avoscloud-Android-sdk-release"
  export ANDROID_HOME="/Users/hong/avos/android-sdk-macosx"
fi
branch=$(get_current_branch)
if [ x"$branch" = "xmaster" ]; then
  echo "|==> BRANCH CAN NOT BE MASTER ! EXIT"
  exit 1
fi
version=${branch//v/} # cut off character 'v'

if [ "x$1" == "xAndroid" ]; then
  echo; echo

  cd $release_sdk_path && {
    echo "|==> Prepare $release_sdk_path git repo for Android"
    echo "|==> Delete $release_sdk_path/android/release-${branch}/*.jar if existed"
    rm -fv $release_sdk_path/android/release-${branch}/*.jar
    # clean samples libs
    echo "|==> Delete $release_sdk_path/android/samples/*/libs/*.jar if existed"
    for jar_path in `find android/samples -name "libs" -type d`; do
      rm -fv $jar_path/*.jar
    done
  }

  releaseDir=build/release-${branch}

  cd -
  # build android sdk
  #cd android
  echo "|==> make local.proproperties"
  echo "sdk.dir=${ANDROID_HOME}" > local.properties
    test -d $releaseDir && {
      echo "|==> $releaseDir already exist, DELETE IT"
      rm -rfv $releaseDir
    }
   echo "|==> try to build SDK on tag:$branch"
   ./build-sdk.sh $branch && {
     echo; echo
     echo "|==> Copying android jars to $release_sdk_apth/android/ "
     cp -Rpv $releaseDir $release_sdk_path/android/
     for jar_path in `find $release_sdk_path/android/samples/ -name "libs" -type d`; do
       echo "|==> Copying android jars to $jar_path/ "
       cp -Rpv $releaseDir/*.jar $jar_path/
     done

     echo "Upload archives to Nexus"
     #./gradlew uploadArchives
     echo "Build Android API doc"
     ant javadoc.avoscloud
     cp -Rpv doc/* $release_doc_path/api/android/

     cd -
   } || fail_and_exit "$0"

else
  echo "Argument \$1 = $1, SHOULD BE Android, EXIT !"
  exit 1
fi

# add to avoscloud-Android-sdk-release and push
cd $release_sdk_path && {
  echo "|==> add changes into repos"
  git add .
  git commit -a -m "[JENKINS] $1 sdk version ${branch}"
  git pull --rebase origin master
  git push origin master
} && \
  echo "COMMIT TO GITHUB DONE !" || \
  fail_and_exit "CAN NOT COMMIT TO GITHUB"


cd $release_doc_path && {
 git add -A api/android/
 git commit -a -m "[JENKINS] $1 Android API doc update version ${branch}"
 git pull --rebase origin master
 git push origin master
} && \
 echo "COMMIT TO GITHUB DONE !" || \
 fail_and_exit "API DOC UPDATE FAILUR"

echo; echo
