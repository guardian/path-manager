#!/usr/bin/env bash

set -e


DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
green='\x1B[0;32m'
red='\x1B[0;31m'
plain='\x1B[0m' # No Color

fail() {
    exit_code=$?
    if [[ "${exit_code}" -ne 0 ]]; then
        echo -e "\n${red}Setup script failed, please fix errors before starting Path-Manager"
    else
        echo -e "\n${green}All done! You can run Path-Manager now."
    fi
}


trap fail EXIT

installRequirements() {
  echo "installing requirements from Brewfile"
  brew bundle
}

setupNginx() {
  dev-nginx setup-app ${DIR}/nginx/nginx-mapping.yml
}

main() {
  installRequirements
  setupNginx
}

main
