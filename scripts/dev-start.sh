#!/usr/bin/env bash
# Usage : scripts/dev-start.sh [plain|tls]
# Démarre discovery, les 3 services (ports 8081/8082/8083) puis le gateway. Logs dans logs/.
MODE="${1:-plain}"
# Identifiants de l annuaire Eureka (protégé depuis la phase 10)
export EUREKA_USER="${EUREKA_USER:-eureka}"
export EUREKA_PASSWORD="${EUREKA_PASSWORD:-eureka-pass}"
mkdir -p logs
jarof() { ls "$1"/target/*.jar 2>/dev/null | grep -v -e '-sources' -e '\.original' | head -n 1; }

run() {  # run <module> <nom> <port> <profils> [VAR=valeur ...]
  local module="$1" name="$2" port="$3" profiles="$4"; shift 4
  local envs=("$@")
  [ -n "$port" ] && envs+=("SERVER_PORT=$port")
  [ -n "$profiles" ] && envs+=("SPRING_PROFILES_ACTIVE=$profiles")
  env "${envs[@]}" java -jar "$(jarof "$module")" > "logs/$name.log" 2>&1 &
  echo "démarré : $name (log : logs/$name.log)"
}

if [ "$MODE" = "tls" ]; then
  : "${PKI_PASS:?définir PKI_PASS}"
  export PKI_DIR="${PKI_DIR:-secrets}"
  P_CUST=tls; P_VISITS=tls; P_VETS=production,tls; P_GW=tls
  GW_OPTS="-Djavax.net.ssl.trustStore=$PKI_DIR/truststore.p12 -Djavax.net.ssl.trustStoreType=PKCS12 -Djavax.net.ssl.trustStorePassword=$PKI_PASS"
else
  P_CUST=""; P_VISITS=""; P_VETS=production; P_GW=""; GW_OPTS=""
fi

run spring-petclinic-discovery-server discovery 8761 ""
echo "attente Eureka (25 s)..."; sleep 25
run spring-petclinic-customers-service customers 8081 "$P_CUST"
run spring-petclinic-visits-service    visits    8082 "$P_VISITS"
run spring-petclinic-vets-service      vets      8083 "$P_VETS"
echo "attente des services (40 s)..."; sleep 40
run spring-petclinic-api-gateway gateway "" "$P_GW" "JAVA_TOOL_OPTIONS=$GW_OPTS"
echo "attente du gateway (30 s)..."; sleep 30
echo "Prêt. Test : curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/"