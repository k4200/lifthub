#!/bin/sh


usage() {
  echo "$0 <name> <start|stop|kill|clean> [ <server name> [ <version> ] ]"
  echo "server name: jetty, tomcat etc. (currently ignored)"
}

NAME=$1
COMMAND=$2
SERVER=$3
VERSION=$4

if [ -z "$SERVER" ]; then
  SERVER=jetty
fi

if [ -z "$VERSION" ]; then
  VERSION=6
fi


JAIL_ID=`jls | grep jails/$NAME | awk '{print $1}'`

#echo $NAME,$COMMAND,$SERVER,$VERSION

jexec -U lifthubuser $JAIL_ID /home/lifthubuser/bin/server-$SERVER.sh $COMMAND $NAME


