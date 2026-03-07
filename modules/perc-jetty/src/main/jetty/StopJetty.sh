#!/bin/bash

scriptname="$(basename "$0")"
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

rxDir=$(dirname ${DIR})
echo rxDir=$rxDir

JETTY_BASE=${DIR}/base
echo JETTY_BASE=$JETTY_BASE

PID=0

#  Check if service is running and get the PID
function check_pid {
	if [ -f "${PID_FILE}" ];then
		testpid=$(<"$PID_FILE")
		if ps -p $testpid >/dev/null 2>&1 ; then
			PID=$testpid
		else
			echo "Stale PID file found at ${PID_FILE}"
			echo "Process $testpid is not running"
			rm -f ${PID_FILE}
		fi  
	fi
}

#  Find running Jetty process by matching JETTY_BASE path
function find_running_jetty {
	# Look for java processes with jetty.base matching this installation
	while IFS= read -r pid; do
		if [ -f "/proc/$pid/cmdline" ]; then
			cmdline=$(cat /proc/$pid/cmdline 2>/dev/null | tr '\0' ' ')
			# Check if this is a Jetty process with matching base directory
			if [[ "$cmdline" == *"jetty"* ]] && [[ "$cmdline" == *"-Djetty.base=${JETTY_BASE}"* ]]; then
				PID=$pid
				echo "Found running Jetty process with PID ${PID} (no PID file)"
				return
			fi
		fi
	done < <(pgrep -f "java.*jetty")
}

##  Checking User and change to correct user if running under root or sudo
RX_USER=$(ls -ld ${rxDir} | awk '{print $3}')
if [ "${RX_USER}" != `whoami` ];then
	if [ $UID -eq 0 ]; then
		#rerun script changing user
		exec su "${RX_USER}" -- "$0" "$@"
	fi
	echo "You must run as root or the user \""${RX_USER}"\" that owns the root directory ${rxDir}"
	exit 1 
fi

PID_FILE="${JETTY_BASE}/etc/running.pid"

check_pid 

runningOnConsole=false
if [ ${PID} -gt 0 ]; then
	runningOnConsole=true
fi

## Find if this instance has been installed as a service
while read -r line; do
	service=$(basename ${line})
	serviceHome=$(bash -c "source ${line} >/dev/null 2>&1 ; echo \${JETTY_BASE}")
	echo "Found Jetty service $service in $line pointing to jetty base $serviceHome"
	if [ "$serviceHome" == "$JETTY_BASE" ]; then
		currentService=$service
	fi 
done < <(grep -l /etc/default/* -e 'JETTY_BASE' 2>/dev/null)

if [ ! -z $currentService ]; then
	PID_FILE="/var/run/rxjetty/${currentService}/rxjetty.pid"
	check_pid  
	echo
	echo "This instance is installed as service ${currentService}"
fi

# If no PID found from PID files, search for running process
if [ ${PID} -eq 0 ]; then
	find_running_jetty
fi

if [ ${PID} -eq 0 ]; then
	echo "Jetty is not running"
	exit 0
fi

echo "Stopping Jetty with PID ${PID}..."

# Try graceful shutdown first
kill -TERM ${PID} 2>/dev/null

# Wait up to 30 seconds for the process to terminate
TIMEOUT=30
COUNTER=0
while [ $COUNTER -lt $TIMEOUT ]; do
	if ! ps -p ${PID} >/dev/null 2>&1; then
		echo "Jetty stopped successfully"
		rm -f ${PID_FILE}
		exit 0
	fi
	sleep 1
	COUNTER=$((COUNTER + 1))
	if [ $((COUNTER % 5)) -eq 0 ]; then
		echo "Waiting for Jetty to stop... (${COUNTER}s)"
	fi
done

# If still running after timeout, force kill
if ps -p ${PID} >/dev/null 2>&1; then
	echo "Jetty did not stop gracefully, forcing shutdown..."
	kill -KILL ${PID} 2>/dev/null
	sleep 2
	if ps -p ${PID} >/dev/null 2>&1; then
		echo "ERROR: Failed to stop Jetty process ${PID}"
		exit 1
	else
		echo "Jetty forcefully stopped"
		rm -f ${PID_FILE}
		exit 0
	fi
fi

rm -f ${PID_FILE}
echo "Jetty stopped"
