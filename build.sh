#!/usr/bin/env bash
echo "Starting Path Manager build"

# Scala test
sbt test

# Scala build & upload to RiffRaff
sbt riffRaffUpload