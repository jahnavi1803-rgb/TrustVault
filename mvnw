#!/bin/sh
# ----------------------------------------------------------------------------
# Maven Wrapper script for Unix/Linux/macOS
# ----------------------------------------------------------------------------

WRAPPER_JAR=".mvn/wrapper/maven-wrapper.jar"

if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Downloading maven-wrapper.jar..."
    curl -sLo "$WRAPPER_JAR" https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar
fi

if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

exec "$JAVACMD" -Dmaven.multiModuleProjectDirectory="$PWD" -cp "$WRAPPER_JAR" org.apache.maven.wrapper.MavenWrapperMain "$@"
