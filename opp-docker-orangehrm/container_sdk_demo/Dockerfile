FROM tomcat:9-jre8

ARG KEYSTORE_PASSWORD

COPY okta-connector-orangehrm-1.0.war /usr/local/tomcat/webapps/
COPY server.xml /usr/local/tomcat/conf/server.xml
COPY sdk_demo.p12 /pki/sdk_demo.p12
ENV CATALINA_OPTS="-DKEYSTORE_PASSWORD=$KEYSTORE_PASSWORD"
