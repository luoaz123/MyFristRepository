#!/bin/sh

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# -----------------------------------------------------------------------------
# Start Script for the CATALINA Server
#
# $Id: JXVS.sh 562770 2011-12-04 22:13:58Z markt $
# -----------------------------------------------------------------------------

#cygwin=false;
darwin=false;
linux=false;

if [ -z "$JXVS_HOME" -o ! -d "$JXVS_HOME" ] ; then
#	if [ -d /opt/JXVS ]; then
#		JXVS_HOME="/opt/JXVS"
#	fi
	
#	if [ -d /usr/local/JXVS ]; then
#		JXVS_HOME="/usr/local/JXVS"
#	fi
	
#	if [ -d ${HOME}/opt/JXVS ]; then
#		JXVS_HOME="${HOME}/opt/JXVS"
#	fi
	
	#resolve links - $0 may be a link in JXVS's home
	PRG="$0"
	progname=`basename "$0$"`
	
	# need this for relative symlinks
	while [ -h "$PRG" ] ; do
    		ls=`ls -ld "$PRG"`
    		link=`expr "$ls" : '.*-> \(.*\)$'`
    		if expr "$link" : '/.*' > /dev/null; then
    			PRG="$link"
    		else
    			PRG=`dirname "$PRG"`"/$link"
    		fi
  	done
	
	# assumes we are in the bin directory
	JXVS_HOME=`dirname "$PRG"`/..
	
	# make it fully qualified
	JXVS_HOME=`cd "$JXVS_HOME" && pwd`
fi

JXVS_OPTS="${JXVS_OPTS} -DjxvsHome=\"${JXVS_HOME}\""

# For Cygwin, ensure paths are in UNIX format before anything is touched
#if $cygwin ; then
#	[ -n "$JXVS_HOME" ] && 
#			JXVS_HOME=`cygpath --unix "$JXVS_HOME"`
#	[ -n "$JAVA_HOME" ] &&
#			JAVA_HOME=`cypath --unix "$JAVA_HOME"`
#fi

# Override with bundled jre if it exists.
if [ -f "$JXVS_HOME/jre/bin/java" ] ; then
	JAVA_HOME="$JXVS_HOME/jre"
	JAVACMD="$JXVS_HOME/jre/bin/java"
fi

if [ -z "$JAVACMD" ] ; then
	if [ -n "$JAVA_HOME" ] ; then
		if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
			JAVACMD="$JAVA_HOME/jre/sh/java"
		else
			JAVACMD="$JAVA_HOME/bin/java"
		fi
	else
		JAVACMD=`which java 2> /dev/null`
		if [ -z "$JAVACMD" ] ; then
			JAVACMD=java
		fi
	fi
fi

if [ ! -x "$JAVACMD" ] ; then
	echo "Error: JAVA_HOME is not defined correctly."
	echo " We cannot execute $JAVACMD"
	exit 1
fi
#if [ ! -x "$LOCALCLASSPATH" ] ; then
#	LOCALCLASSPATH=./bootstrap.jar
#else
#	LOCALCLASSPATH=./bootstrap.jar:$LOCALCLASSPATH
#fi

# For Cygwin, switch paths to appropriate format before running java
#if $cygwin; then
#	if [ "$OS" = "windows_NT" ] && cygpath -m .>/dev/null 2>/dev/null ; then
#		format=nixed
#	else
#		format=windows
#	fi
#	JXVS_HOME=`cygpath --$format "$JXVS_HOME"`
#	JAVA_HOME=`cygpath --$format "$JAVA_HOME"`
#	LOCALCLASSPATH=`cygpath --path --$format "$LOCALCLASSPATH"`
#	if [ -n "$CLASSPATH" ] ; then
#		CLASSPATH=`cygpath --path --$format "$CLASSPATH"`
#	fi
#	CYGHOME=`cygpath --$format "$HOME"`
#fi

# add a second backslash to variables terminated by a backslash under cygwin

#if [ ! -z "$1" ] ; then
#	if [ "$1" = "start" ] ; then

JAVA_OPTS="$JAVA_OPTS
        -server
        -Xmx3072m
        -Xms3072m
        -Xmn500m
        -XX:PermSize=500m
        -XX:MaxPermSize=500m
        -Xss256k
        -XX:+DisableExplicitGC
        -XX:SurvivorRatio=1
        -XX:+UseConcMarkSweepGC
        -XX:+UseParNewGC
        -XX:+CMSParallelRemarkEnabled
        -XX:+UseCMSCompactAtFullCollection
        -XX:CMSFullGCsBeforeCompaction=0
        -XX:+CMSClassUnloadingEnabled
        -XX:LargePageSizeInBytes=128m
        -XX:+UseFastAccessorMethods
        -XX:+UseCMSInitiatingOccupancyOnly
        -XX:CMSInitiatingOccupancyFraction=80
        -XX:SoftRefLRUPolicyMSPerMB=0
        -XX:+PrintClassHistogram
        -XX:+PrintGCDetails
        -XX:+PrintGCTimeStamps
        -XX:+PrintHeapAtGC
        -Xloggc:gc.log"

#JAVA_OPTS="$JAVA_OPTS
#    -Dcom.sun.management.jmxremote
#    -Djava.rmi.server.hostname=192.168.1.123
#    -Dcom.sun.management.jmxremote.port=8999
#    -Dcom.sun.management.jmxremote.ssl=false
#    -Dcom.sun.management.jmxremote.authenticate=false"

                JXVS_exec_command="exec $JAVACMD $JAVA_OPTS $JXVS_OPTS -cp $JXVS_HOME/lib/startup.jar org.sainfy.jxvs.launcher.Launcher"
#	elif [ "$1" = "stop" ] ; then
#		JXVS_exec_command="exec $JAVACMD $JXVS_OPTS -cp $JXVS_HOME/bin/RMCMode.jar com.innitech.main.Bootstrap \"$1\""
#	fi
	eval $JXVS_exec_command
#else
#	echo "Usage: JXVS.sh ( commands ... )"
#	echo "commands:"
#	echo "  start             Start Catalina in a separate window"
#	echo "  stop              Stop Catalina, waiting up to 5 seconds for the process to end"
#	echo "Note: Waiting for the process to end and use of the -force option require that \$CATALINA_PID is defined"
#	exit 1
#fi
