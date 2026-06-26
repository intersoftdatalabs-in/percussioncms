#!/bin/bash

BUILD_ID=1
BUILD_DATE=$(date -u +%Y%m%d)
BUILD_STRING="RELEASE"

echo "You can now prepare the release with ./mvn-env.sh release:prepare -DreleaseVersion=8.1.7 -DdevelopmentVersion=8.1.8-SNAPSHOT -DtagName=v8.1.7 -Darguments=\"-Dmaven.deploy.skip=true -DbuildNumber=$BUILD_DATE -DbuildId=$BUILD_ID -DversionString=RELEASE\""

echo "./mvn-env.sh release:perform -Darguments=\"-Dmaven.deploy.skip=true -DbuildNumber=$BUILD_DATE -DbuildId=$BUILD_ID -DversionString=RELEASE\""
