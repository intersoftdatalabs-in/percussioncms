#!/bin/bash

BUILD_ID=1
BUILD_DATE=$(date -u +%Y%m%d)
BUILD_STRING="RELEASE"

echo "You can now prepare the release with ./mvn-env.sh mvn release:prepare "
echo "-DreleaseVersion=8.1.7"
echo "  -DdevelopmentVersion=8.1.8-SNAPSHOT"
echo "  -DtagName=v8.1.7"
echo "  -Darguments=\"-Dmaven.deploy.skip=true\""
echo "  -DbuildNumber=\"$BUILD_DATE\""
echo "  -DbuildId=\"$BUILD_ID\""
echo "  -DversionString=\"RELEASE\""

echo "mvn release:perform -Darguments=\"-Dmaven.deploy.skip=true\""
echo "  -DbuildNumber=\"$BUILD_DATE\""
echo "  -DbuildId=\"$BUILD_ID\""
echo "  -DversionString=\"RELEASE\""
