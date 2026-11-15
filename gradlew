#!/usr/bin/env sh
#
# Gradle startup script for Unix
#

# Attempt to find the gradle wrapper jar
APP_HOME="$(cd "${0%/*}" && pwd)"
APP_HOME="${APP_HOME%%${0##*/}}"

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

# Find the java executable
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

if [ ! -x "$JAVACMD" ] ; then
    echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH." >&2
    exit 1
fi

# Use the maximum available VM heap, and enforce a reasonable minimum.
DEFAULT_JVM_OPTS="-Xmx6g -XX:+HeapDumpOnOutOfMemoryError"

# Collect all the options for the JVM
JVM_OPTS="$DEFAULT_JVM_OPTS $GRADLE_OPTS $JAVA_OPTS"

# Launch the JVM
exec "$JAVACMD" $JVM_OPTS \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain "$@"
