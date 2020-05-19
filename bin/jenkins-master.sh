#!/usr/bin/env bash

#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

usage="Usage: jenkins-master.sh  (start|stop|status)"

cygwin=false
case "`uname`" in
   CYGWIN*) cygwin=true;;
esac

if [ $# -lt 0 ]; then
  echo $usage
  exit 1
fi


if [ -z "${JENKINS_HOME}" ]; then
  source "$(dirname "$0")"/find-jenkins-home
fi

bin_dir="$(cd "$(dirname "$0")"; pwd)"

# 指定 jenkins.war 所在的目录,  生产中建议通过 Jetty\Tomcat等部署
if [ -z "${JENKINS_WAR_HOME}" ]; then
  export JENKINS_WAR_HOME="$(cd "$(dirname "$0")"/../build/war; pwd)"
fi


if [ "$JENKINS_MASTER_LOG_DIR" = "" ]; then
  JENKINS_MASTER_LOG_DIR="${bin_dir}/logs"
fi
mkdir -p "$JENKINS_MASTER_LOG_DIR"

if [ "$JENKINS_MASTER_PID_DIR" = "" ]; then
  JENKINS_MASTER_PID_DIR="${bin_dir}/pid"
fi


log="$JENKINS_MASTER_LOG_DIR/jenkins-master.log"
pid="$JENKINS_MASTER_PID_DIR/jenkins-mater.pid"


option=$1
shift

START() {
  mkdir -p "$JENKINS_MASTER_PID_DIR"
  if [ -f "$pid" ]; then
    TARGET_ID="$(cat "$pid")"
    if [[ $(ps -p "$TARGET_ID" -o comm=) =~ "java" ]]; then
      echo "$command running as process $TARGET_ID.  Stop it first."
      exit 1
    fi
  fi

  nohup java -jar ${JENKINS_WAR_HOME}/jenkins.war  >> $log 2>&1 < /dev/null &
  newpid="$!"

  echo "$newpid" > "$pid"
  # Poll for up to 5 seconds for the java process to start
  for i in {1..10}
  do
    if [[ $(ps -p "$newpid" -o comm=) =~ "java" ]]; then
       break
    fi
    sleep 0.5
  done

  sleep 2
  # Check if the process has died; in that case we'll tail the log so the user can see
  if [[ ! $(ps -p "$newpid" -o comm=) =~ "java" ]]; then
     echo "failed to launch: $@"
     tail -10 "$log" | sed 's/^/  /'
     echo "full log in $log"
  fi

}

STOP(){
    if [ -f $pid ]; then
      TARGET_ID="$(cat "$pid")"
      if [[ $(ps -p "$TARGET_ID" -o comm=) =~ "java" ]]; then
        echo "stopping $command"
        kill "$TARGET_ID" && rm -f "$pid"
        for i in {1..10}
        do
           if [[ ! $(ps -p "$TARGET_ID" -o comm=) =~ "java" ]]; then
              break
           fi
           sleep 1
        done

         if [[ $(ps -p "$TARGET_ID" -o comm=) =~ "java" ]]; then
            echo "failed to stop: $@"
         fi
      else
        echo "no $command to stop"
      fi

    else
      echo "no $command to stop"
    fi
}


case $option in
  (start|START)
    START
    ;;
  (stop|STOP)
    STOP
    ;;
  (status)
    if [ -f $pid ]; then
      TARGET_ID="$(cat "$pid")"
      if [[ $(ps -p "$TARGET_ID" -o comm=) =~ "java" ]]; then
        echo $command is running, pid is $TARGET_ID
        exit 0
      else
        echo $pid file is present but $command not running
        exit 1
      fi
    else
      echo $command not running
      exit 2
    fi
    ;;
  (*)
     echo $usage
     exit 1
     ;;
esac
