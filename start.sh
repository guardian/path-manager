#!/bin/bash

set -e

green='\x1B[0;32m'
red='\x1B[0;31m'
plain='\x1B[0m' # No Color


runDynanmo() {
  docker-compose up -d
}
 
startSbt() {
  sbt "; project pathManager ; run"
}

runDynanmo
startSbt