#!/bin/bash

if [ ! -d "src/main/resources/templates/fds" ]; then
  cd "fivium-design-system-core"
  npm install && npx gulp build # Build FDS components
  cd ..
  npm install && npx gulp buildAll # Build app specific components
fi

docker-compose -f docker/docker-compose.yml up -d
../gradlew :example:bootRun
