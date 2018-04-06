#!/bin/bash

./shutdown.sh
nohup ./zstart.sh  >>/dev/null 2>&1 &
sh tail.sh &