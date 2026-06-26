#!/usr/bin/env bash
set -euo pipefail
cd /root/task
docker compose up -d
printf '\nLocal services are starting.\n'
printf 'Application health: http://127.0.0.1:8080/actuator/health\n'
printf 'Admin orders endpoint: http://127.0.0.1:8080/api/admin/orders?status=PAID&page=0&size=50\n'
printf 'Container logs: docker compose logs -f app\n'
