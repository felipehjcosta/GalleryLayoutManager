#!/bin/bash

if [ "$TRAVIS_TAG" != "" ]; then
  echo -e 'Build Branch for Release => Branch ['$TRAVIS_BRANCH']  Tag ['$TRAVIS_TAG']'
  ./gradlew clean build lib:bintrayUpload -PbintrayUser="${BINTRAY_USER}" -PbintrayKey="${BINTRAY_PASSWORD}" -PdryRun=false
else
    echo -e 'Normal Build => Branch ['$TRAVIS_BRANCH']'
  ./gradlew clean build
fi