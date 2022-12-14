#!/bin/bash

# Start netprobe
cd /opt/itrs/netprobe
export LOG_FILENAME=/var/log/netprobe.log
./netprobe.linux_64 -port 7036 -nopassword &

# Set password
sed -i "s/^;admin_password =.*/admin_password = ${ADMIN_PW}/" /etc/grafana/grafana.ini

# Load dashboards in background, but after Grafana has started
(
  sleep 20
  cd /var/tmp/ansible
  ansible-playbook -i localhost import-dbs.yml
  sleep 5
  kill -1 `ps -ef | grep grafana-server | grep -v grep | awk '{print $2}'`
) &

# Start prometheus
grafana-server --config=/etc/grafana/grafana.ini --homepath /usr/share/grafana

# Load dashboards with
# https://grafana.com/docs/grafana/latest/http_api/dashboard/
