FROM azul/zulu-openjdk-alpine:11-jre-headless
MAINTAINER Niklas Liechti <nliechti@ergon.ch>

# grant root privileges
USER root

# Achtung: ansible und python muss abgestimmt aufeinander werden
RUN \
    apk update && \
    apk add shadow dos2unix curl tzdata

# Clean APK cache
RUN rm -rf /var/cache/apk/*

ENV TZ Europe/Zurich

# Create a user and group used to launch processes
# On most distributions, user ID 1000 is the default for the first "regular" user
# -> helps avoiding permission issues when using volumes
RUN \
	groupadd -r solaredge-operator -g 1000 && useradd -u 1000 -r -g solaredge-operator -m -d /opt/operator -s /sbin/nologin -c "operator user" solaredge-operator && \
	chmod 755 /opt/operator

# Custom run configuration
COPY docker/start_operator.sh /run/start_operator.sh

# eventually change line endings and set executable permission where necessary
RUN \
	dos2unix /run/start_operator.sh && \
	chmod +x /run/start_operator.sh && \
	# Change owner to operator
	chown solaredge-operator:solaredge-operator /run/start_operator.sh

# Application
COPY build/libs/operator.jar /opt/operator/

RUN chown -R solaredge-operator:solaredge-operator /opt/operator

# Switch back to operator user
USER operator

# Start the main process
CMD ["/run/start_operator.sh"]
