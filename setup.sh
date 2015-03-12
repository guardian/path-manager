#!/bin/bash


green='\x1B[0;32m'
red='\x1B[0;31m'
plain='\x1B[0m' # No Color

dynamoDir=dynamo
dynamoDistFile=$dynamoDir/dynamodb_local_latest.tar.gz

# Check for required programs

test $(which wget)
if [ $? != "0" ]; then
    echo -e "${red}wget not found: please install using 'sudo apt-get install wget' (GNU/Linux) or 'brew install wget' (OS X)${plain}"
    exit 1
fi

mkdir -p $dynamoDir

if [ ! -f "$dynamoDistFile" ]; then
    distUri="http://dynamodb-local.s3-website-us-west-2.amazonaws.com/dynamodb_local_latest.tar.gz"
	echo -e "${green}Downloading $distUri${plain}"
	wget $distUri -O $dynamoDistFile

	echo -e "${green}Extracting $dynamoDistFile to $dynamoDir${plain}"
    tar -C $dynamoDir -xzf $dynamoDistFile
fi

# try and figure out if dynamo is running
curl -s http://localhost:10005 > /dev/null
DYNAMO_RUNNING=$?

if [ $DYNAMO_RUNNING == "0" ]; then
    echo -e "${green}Dynamo already appears to be running fine, leave it alone${plain}"
else
    echo -e "${green}Starting dynamo local on port 10005${plain}"

    pushd $dynamoDir

    nohup java -Djava.library.path=./DynamoDBLocal_lib -jar DynamoDBLocal.jar -port 10005 --sharedDb > /dev/null &

    popd

    echo -e "${green}Done${plain}"
fi