#!/bin/bash

sh shutdown.sh
nohup ./zstart.sh  >>/dev/null 2>&1 &
sh tail.sh &