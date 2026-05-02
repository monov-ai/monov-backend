#!/usr/bin/env bash
# Blue-Green 배포 — EC2 위에서 실행.
# 시크릿(application.yml) 은 이미 이미지 빌드 시점에 박혀있음 (cd.yml 이 APPLICATION secret → src/main/resources/application.yml).
# EC2 에 외부 yml 마운트 / .env / SCP 모두 없음.
#
# 필요한 shell env (cd.yml 이 export):
#   DOCKERHUB_USERNAME    — 이미지 prefix
#   IMAGE_TAG             — 새 이미지 태그 (기본 latest)
#   GRAFANA_PASSWORD      — grafana admin (선택)
#
# 필요한 호스트 파일:
#   /etc/nginx/upstream-{blue,green}.conf  — nginx 호스트 설정 (최초 1회)
#   /etc/nginx/conf.d/upstream-active.conf  — 위 둘 중 하나의 심볼릭 링크
#
# sudoers (deploy 사용자 NOPASSWD):
#   ec2-user ALL=(ALL) NOPASSWD: /usr/bin/ln, /usr/sbin/nginx, /usr/bin/systemctl reload nginx

set -euo pipefail

cd "$(dirname "$0")/.."  # 프로젝트 루트로

: "${DOCKERHUB_USERNAME:?DOCKERHUB_USERNAME must be set}"
: "${IMAGE_TAG:=latest}"

IMAGE="${DOCKERHUB_USERNAME}/monov-ai:${IMAGE_TAG}"
STATE_FILE=".active"

# 현재 활성 색 읽기 (없으면 첫 배포 = blue)
ACTIVE=$(cat "$STATE_FILE" 2>/dev/null || echo "blue")
if [ "$ACTIVE" = "blue" ]; then
    INACTIVE="green"
    INACTIVE_PORT=8081
else
    INACTIVE="blue"
    INACTIVE_PORT=8080
fi

echo "🔵 배포 시작 — active=$ACTIVE, 새 버전을 $INACTIVE 에 띄움"
echo "   IMAGE=$IMAGE"

# 1. 새 이미지 pull
echo ""
echo "📥 이미지 pull..."
docker pull "$IMAGE"

# 2. inactive 컨테이너 (새 이미지로) 시작
echo ""
echo "🚀 app-$INACTIVE 시작..."
docker compose up -d --no-deps "app-$INACTIVE"

# 3. healthcheck 대기 (최대 120초)
echo ""
echo "⏳ app-$INACTIVE healthcheck 대기..."
for i in $(seq 1 60); do
    if curl -fs "http://127.0.0.1:${INACTIVE_PORT}/actuator/health/readiness" > /dev/null 2>&1; then
        echo "✅ app-$INACTIVE ready (${i}회 시도)"
        break
    fi
    if [ $i -eq 60 ]; then
        echo "❌ app-$INACTIVE healthcheck 실패"
        echo "===== 최근 로그 ====="
        docker logs "monov-app-${INACTIVE}" --tail 80
        echo ""
        echo "롤백: app-$INACTIVE 종료, 트래픽은 $ACTIVE 유지"
        docker compose stop "app-$INACTIVE"
        exit 1
    fi
    sleep 2
done

# 4. nginx upstream 전환
echo ""
echo "🔀 nginx upstream → $INACTIVE 으로 전환..."
sudo ln -sf "/etc/nginx/upstream-${INACTIVE}.conf" /etc/nginx/conf.d/upstream-active.conf
if ! sudo nginx -t; then
    echo "❌ nginx -t 실패. 롤백."
    sudo ln -sf "/etc/nginx/upstream-${ACTIVE}.conf" /etc/nginx/conf.d/upstream-active.conf
    docker compose stop "app-$INACTIVE"
    exit 1
fi
sudo systemctl reload nginx

# 5. 옛 색깔 컨테이너 graceful shutdown
echo ""
echo "🛑 app-$ACTIVE 종료 (graceful)..."
docker compose stop "app-$ACTIVE" || true

# 6. state 갱신
echo "$INACTIVE" > "$STATE_FILE"

echo ""
echo "✅ 배포 완료 — active=$INACTIVE"
docker compose ps