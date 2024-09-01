FROM maven:3.8.6-openjdk-11-slim AS build
ADD . /app
WORKDIR /app
USER root
RUN mvn clean package -Dmaven.test.skip=true

FROM gcr.io/dataflow-templates-base/java11-template-launcher-base

ARG WORKDIR=/dataflow/template
RUN mkdir -p ${WORKDIR}
WORKDIR ${WORKDIR}

COPY --from=build /app/target/bq-to-jdbc-googlecloud-1.0-SNAPSHOT.jar ${WORKDIR}/target/

ENV FLEX_TEMPLATE_JAVA_MAIN_CLASS="org.sigma.BqToJdbc"
ENV FLEX_TEMPLATE_JAVA_CLASSPATH="${WORKDIR}/target/bq-to-jdbc-googlecloud-1.0-SNAPSHOT.jar"

ENTRYPOINT ["/opt/google/dataflow/java_template_launcher"]