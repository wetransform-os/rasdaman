#!/bin/bash

target=$HOME/lsis/www/testbed19/processes/
src="$HOME/rasdaman/enterprise/R16/applications/petascope/petascope_main/src/main/resources/openeo/processes"

pushd "$src" > /dev/null || exit 1
./generate_process_descriptions.py || exit 1
jq -s '.' ./*.json > "$target/processes.json"
popd > /dev/null || exit 1
