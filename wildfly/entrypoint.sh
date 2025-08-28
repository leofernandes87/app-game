#!/usr/bin/env bash
set -euo pipefail
WILDFLY_HOME=/opt/jboss/wildfly

# cria admin
if [ -n "${WF_MGMT_USER:-}" ] && [ -n "${WF_MGMT_PASS:-}" ]; then
  "$WILDFLY_HOME/bin/add-user.sh" --user "$WF_MGMT_USER" --password "$WF_MGMT_PASS" --silent || true
fi

# sobe admin-only para configurar
"$WILDFLY_HOME/bin/standalone.sh" -c standalone.xml -b 0.0.0.0 -bmanagement 0.0.0.0 --admin-only &
WF_PID=$!

# espera o controller ficar online (via CLI)
until "$WILDFLY_HOME/bin/jboss-cli.sh" --connect --commands=":read-attribute(name=server-state)" \
      | grep -qE 'running|reload-required|restart-required'; do
  echo "Aguardando WildFly (mgmt 9990)..."
  sleep 2
done

# aplica o CLI online; tolera 'already exists' no dev
"$WILDFLY_HOME/bin/jboss-cli.sh" --connect --file=/opt/jboss/wildfly/configure-ds/configure-datasource.cli || true

# reinicia normal (foreground)
kill $WF_PID
wait $WF_PID || true
exec "$WILDFLY_HOME/bin/standalone.sh" -c standalone.xml -b 0.0.0.0 -bmanagement 0.0.0.0
