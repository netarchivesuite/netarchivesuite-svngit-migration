#!/bin/bash
cd /home/test/UNITTEST/conf
if [ -e ./kill_harvester_8082.sh ]; then
    ./kill_harvester_8082.sh 
fi
if [ -e ./kill_harvester_8081.sh ]; then
    ./kill_harvester_8081.sh 
fi

