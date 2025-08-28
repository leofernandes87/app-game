FROM quay.io/wildfly/wildfly:37.0.0.Final-jdk21

USER root
#RUN microdnf -y install curl && microdnf clean all

# cria a pasta do módulo
RUN mkdir -p /opt/jboss/wildfly/modules/system/layers/base/org/postgresql/main

# copia o jar existente
COPY postgresql.jar /opt/jboss/wildfly/modules/system/layers/base/org/postgresql/main/

# copia o module.xml para o mesmo diretório
COPY module.xml /opt/jboss/wildfly/modules/system/layers/base/org/postgresql/main/module.xml

RUN chmod 644 /opt/jboss/wildfly/modules/system/layers/base/org/postgresql/main/* \
 && chown -R jboss:jboss /opt/jboss/wildfly/modules/system/layers/base/org/postgresql

# Script de configuração (driver + datasource)
COPY configure-datasource.cli /opt/jboss/wildfly/configure-ds/configure-datasource.cli

RUN chmod 644 /opt/jboss/wildfly/configure-ds/configure-datasource.cli \
 && chown -R jboss:jboss /opt/jboss/wildfly/configure-ds

# Entrypoint aplica o CLI e inicia
COPY entrypoint.sh /entrypoint.sh
RUN chmod 755 /entrypoint.sh
RUN chown jboss:jboss /entrypoint.sh
RUN command -v bash >/dev/null

USER jboss
ENTRYPOINT ["/entrypoint.sh"]
