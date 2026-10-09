# MediSys - how the live server is built (Render runs this file).
#
# Stage 1 builds target/medisys.war with Maven.
# Stage 2 runs it in Tomcat 11 as the ROOT app, so the site lives at "/".
# Settings (DB_URL, DB_USER, DB_PASSWORD, CLOUDINARY_URL) come from the
# host's environment variables - never from files in this image.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B clean package -DskipTests

FROM tomcat:11.0-jdk21-temurin
# Remove Tomcat's demo apps and deploy ours as the root of the site.
RUN rm -rf /usr/local/tomcat/webapps/* \
 # Trust the X-Forwarded-* headers of the hosting proxy (https, real client IP).
 && sed -i 's#</Host>#  <Valve className="org.apache.catalina.valves.RemoteIpValve" protocolHeader="X-Forwarded-Proto" />\n      </Host>#' /usr/local/tomcat/conf/server.xml
COPY --from=build /app/target/medisys.war /usr/local/tomcat/webapps/ROOT.war

# Sri Lanka time for "today", order times and reports; memory sized for a 512 MB instance.
ENV TZ=Asia/Colombo \
    JAVA_OPTS="-Duser.timezone=Asia/Colombo -Xms128m -Xmx320m -XX:+UseSerialGC"
EXPOSE 8080
CMD ["catalina.sh", "run"]
