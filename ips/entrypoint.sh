#!/usr/bin/env bash

set -e
set -o pipefail

NETWORK_INTERFACE_NAME="eth0"
NETWORK_ADDRESS_CIDR=$(ip addr show "${NETWORK_INTERFACE_NAME}" | grep "inet\b" | awk '{print $2}')
NETWORK_ADDRESS=$(echo "${NETWORK_ADDRESS_CIDR}" | cut -d'/' -f1)

TARGET_ADDRESS=$(getent hosts "${FORWARD_HOST}" | awk '{print $1}')
read -r -a TARGET_PORTS <<< "${FORWARD_PORTS}"

echo "Network address: ${NETWORK_ADDRESS}"
echo "Forward host: ${FORWARD_HOST}"
echo "Forward ports: ${TARGET_PORTS[*]}"
echo "Target address: ${TARGET_ADDRESS}"

for target_port in "${TARGET_PORTS[@]}"; do
  iptables -t nat -A PREROUTING -p tcp --dport "${target_port}" -j DNAT --to-destination "${TARGET_ADDRESS}:${target_port}"
  iptables -t nat -A POSTROUTING -p tcp -d "${TARGET_ADDRESS}" --dport "${target_port}" -j SNAT --to-source "${NETWORK_ADDRESS}"
done

iptables -I FORWARD -j NFQUEUE --queue-num=1

sed -i "s|__HOME_NET__|${TARGET_ADDRESS}/32|g" /etc/snort/snort.lua
sed -i "s|__HTTP_PORTS__|${TARGET_PORTS[*]}|g" /etc/snort/snort.lua

exec "$@"
