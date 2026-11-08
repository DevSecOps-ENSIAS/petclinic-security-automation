#!/usr/bin/env bash
# Arrête TOUS les processus Java de la machine (à utiliser quand rien d'autre en Java ne doit rester ouvert).
if command -v taskkill >/dev/null 2>&1; then taskkill //F //IM java.exe; else pkill -f 'spring-petclinic.*\.jar'; fi