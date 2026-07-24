#!/bin/bash
################
# Stop Tomcat
################
#set -x
# Layout (installDts copies rootFiles/* to install root):
#   <InstallRoot>/TomcatShutdown.sh
#   <InstallRoot>/resolve-java-home.sh
#   <InstallRoot>/Deployment/Server/...
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
INSTALL_ROOT="${SCRIPT_DIR}"
export SERVER_DIR="${INSTALL_ROOT}/Deployment/Server"

# Resolve Java via shared precedence contract (java.properties > env > install-dir
# JRE|JRE64 > PATH > fail, major 21). Required operator step before service start.
# See specs/991-system-java-home/contracts/java-home-resolution.md.
# shellcheck disable=SC1091
source "${SCRIPT_DIR}/resolve-java-home.sh" "$INSTALL_ROOT" || exit 1

export JAVA_HOME
export JRE_HOME="$JAVA_HOME"

if [ ! -x "${SERVER_DIR}/bin/catalina.sh" ]; then
    echo "TomcatShutdown: missing ${SERVER_DIR}/bin/catalina.sh" >&2
    exit 1
fi

export JAVA_OPTS="$JAVA_OPTS -Dhttps.protocols=TLSv1.2 -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv4Addresses=true -Dfile.encoding=UTF-8 -Xmx1024m -Djava.endorsed.dirs=$SERVER_DIR/endorsed  -Dcatalina.base=$SERVER_DIR -Dcatalina.home=$SERVER_DIR -Djava.io.tmpdir=$SERVER_DIR/temp -Dderby.system.home=$SERVER_DIR/derbydata"
export CATALINA_HOME=$SERVER_DIR
"$SERVER_DIR/bin/catalina.sh" stop
