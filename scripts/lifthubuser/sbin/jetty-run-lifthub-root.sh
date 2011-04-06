#!/bin/bash

usage() {
  echo "$0 <start|stop> <name> <port>"
  echo "$0 <kill|clean> <name>"
}

if [ $# -ne 3 -a \( $# -eq 2 -a "$1" != "kill" -a "$1" != "clean" \) ];
then
  usage
  exit 2
fi

#
NAME=$2
JAIL_WD=/home/lifthubuser/chroot/$NAME
# Server's root dir (in a jail).
SERVER_ROOT=/home/lifthubuser/servers/jetty-6
STOP_PORT=$3
LOG=$JAIL_WD$SERVER_ROOT/logs/$NAME-execute.log
PID_FILE=$JAIL_WD$SERVER_ROOT/logs/$NAME.pid


case "$1" in
  start)
    echo "starting..."
    #export LD_LIBRARY_PATH=/usr/lib/jvm/java-1.6.0-openjdk-1.6.0.0.x86_64/jre/lib/amd64/jli
    /usr/sbin/chroot $JAIL_WD su - lifthubuser -c "cd $SERVER_ROOT; bin/jetty-run-lifthub.sh start $NAME $STOP_PORT"
    ;;
  stop)
    echo "stopping..."
    java -DSTOP.PORT=$STOP_PORT -DSTOP.KEY=$NAME -jar $JAIL_WD$SERVER_ROOT/start.jar --stop > $LOG 2>&1
    ;;
  kill)
    kill -9 `cat $PID_FILE`
    ;;
  clean)
    echo "clean"
    ps aux | grep "STOP.KEY=$NAME" | grep -v grep | awk '{print $2}' | xargs kill -9
    rm -rf $JAIL_WD/tmp/Jetty*
    ;;
  *)
    usage
    exit 2
    ;;
esac

>&-
2>&-
