#!/bin/bash
#
# This script starts/stops the Lifthub Management App.
#

usage() {
  echo "$0 <start|stop>"
}

if [ $# -ne 1 ];
then
  usage
  exit 2
fi


SERVER_ROOT=/home/lifthub/server/jetty-6
#STOP_PORT=$3
STOP_PORT=8100
STOP_KEY=lifthub
LOG=/dev/null

cd $SERVER_ROOT

case "$1" in
  start)
    echo "starting..."
    # Needed for Gitosis, but now it's probably useless.
    export GIT_SSH=ssh
    java -DSTOP.PORT=$STOP_PORT -DSTOP.KEY=$NAME -jar start.jar etc/jetty.xml &
    ;;
  stop)
    echo "stopping..."
    java -DSTOP.PORT=$STOP_PORT -DSTOP.KEY=$NAME -jar start.jar --stop
    ;;
  *)
    usage
    exit 2
    ;;
esac

>&-
2>&-

