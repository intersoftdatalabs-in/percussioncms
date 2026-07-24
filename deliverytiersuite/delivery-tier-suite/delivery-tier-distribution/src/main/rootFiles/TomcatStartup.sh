#!/bin/bash
################
# Start Tomcat
################
#set -x
# Layout (installDts copies rootFiles/* to install root):
#   <InstallRoot>/TomcatStartup.sh
#   <InstallRoot>/resolve-java-home.sh
#   <InstallRoot>/Deployment/Server/...
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
INSTALL_ROOT="${SCRIPT_DIR}"
export SERVER_DIR="${INSTALL_ROOT}/Deployment/Server"

# GH-991: install-time selection wrote java.properties; resolve it (not a
# mandatory <InstallRoot>/JRE). Precedence: java.properties > env >
# optional legacy JRE|JRE64 > PATH > fail (major 21+).
# See specs/991-system-java-home/contracts/java-home-resolution.md.
# shellcheck disable=SC1091
source "${SCRIPT_DIR}/resolve-java-home.sh" "$INSTALL_ROOT" || exit 1

export JAVA_HOME
export JRE_HOME="$JAVA_HOME"

if [ ! -x "${SERVER_DIR}/bin/catalina.sh" ]; then
    echo "TomcatStartup: missing ${SERVER_DIR}/bin/catalina.sh" >&2
    exit 1
fi

# Note: java.endorsed.dirs property is not supported on Java 9+ (fatal on 21); do not add it back.
export JAVA_OPTS="$JAVA_OPTS -Dhttps.protocols=TLSv1.2 -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv4Addresses=true -Dfile.encoding=UTF-8 -Xmx1024m -Dcatalina.base=$SERVER_DIR -Dcatalina.home=$SERVER_DIR -Djava.io.tmpdir=$SERVER_DIR/temp -Dderby.system.home=$SERVER_DIR/derbydata -Dperc.h2.data.home=$SERVER_DIR/h2data"
export CATALINA_HOME=$SERVER_DIR
"$SERVER_DIR/bin/catalina.sh" run
