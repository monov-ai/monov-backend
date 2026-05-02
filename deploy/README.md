# 배포 가이드 — Single EC2 + Docker + Blue-Green + GitHub Actions

## 아키텍처

```
인터넷 :443
    ↓ SSL termination (Certbot 발급한 cert)
[ Nginx (호스트 직접 운영) ]
    ↓ proxy_pass http://monov_app
[ upstream monov_app ]      ← /etc/nginx/conf.d/upstream-active.conf (심볼릭 링크)
    ↓ 127.0.0.1:8080  또는  127.0.0.1:8081
[ Docker Compose stack on EC2 ]
    ├── monov-app-blue   (:8080 → 127.0.0.1)
    ├── monov-app-green  (:8081 → 127.0.0.1)
    ├── monov-redis      (docker network 내부)
    ├── monov-prometheus (docker network 내부, scrape)
    └── monov-grafana    (127.0.0.1:3000, SSH tunnel 로 접근)

외부:
    └── RDS (MySQL)
```

## 시크릿 관리 — `APPLICATION` 한 개에 yml 통째로

**EC2 에 시크릿 파일 안 둡니다.** GitHub Repository Secret `APPLICATION` 하나에 `application.yml` 전체를 넣어두고, CI 가 빌드 직전에 풀어서 이미지에 박아버립니다.

```
git push main
   ↓
GitHub Actions (cd.yml)
   ├─ build-and-push job
   │   ├─ secrets.APPLICATION → src/main/resources/application.yml (heredoc)
   │   ├─ ./gradlew bootJar  ← 이 시점에 yml 이 jar 안에 들어감
   │   └─ docker build/push  ← 이미지 안에 yml 박힘
   └─ deploy job
       └─ ssh EC2 → cd ~/monov-ai && ./deploy/deploy.sh (blue↔green 전환만)
```

→ EC2 위에 `.env` / yml / config 디렉토리 전부 없음. 이미지만 갈아끼움.
→ 배포 경로는 `~/monov-ai` 로 고정 (워크플로 안에 박혀있음).

⚠️ **이미지 안에 시크릿이 박혀있으니 Docker Hub repo 는 반드시 private.**

### 필요한 GitHub Repository Secrets

Settings → Secrets and variables → Actions → New repository secret

| Secret | 의미 |
|---|---|
| `APPLICATION` | `application.yml` 전체 내용 (DB / S3 / OAuth / JWT 등 모든 키 포함) |
| `DOCKERHUB_USERNAME` | Docker Hub ID |
| `DOCKERHUB_TOKEN` | Docker Hub Access Token (Settings → Security → New Access Token) |
| `EC2_HOST` | EC2 public IP/도메인 |
| `EC2_USER` | SSH 사용자 (예: `ubuntu`) |
| `EC2_SSH_KEY` | SSH private key 본문 (`-----BEGIN ...-----`) |
| `GRAFANA_PASSWORD` | 모니터링 대시보드 admin |

→ 키 추가/변경 시: `APPLICATION` 시크릿만 수정 → 다음 push 부터 자동 반영. EC2 손대지 X.

## EC2 최초 셋업 (한 번만)

### 1. Docker / Docker Compose 설치
```bash
sudo apt update
sudo apt install -y docker.io docker-compose-plugin
sudo usermod -aG docker $USER
# 재로그인
```

### 2. 프로젝트 디렉토리 준비
```bash
# 경로는 ~/monov-ai 로 고정 (cd.yml 이 이 경로로 cd 함)
mkdir -p ~/monov-ai && cd ~/monov-ai
git clone <repo> .          # docker-compose.yml + deploy/ 만 있어도 OK

# 시크릿은 이미지에 박혀있으므로 EC2 에 yml/.env 둘 일 없음
```

### 3. nginx upstream conf 배치
```bash
sudo cp deploy/nginx/upstream-blue.conf  /etc/nginx/upstream-blue.conf
sudo cp deploy/nginx/upstream-green.conf /etc/nginx/upstream-green.conf

# 초기 활성 = blue
sudo ln -sf /etc/nginx/upstream-blue.conf /etc/nginx/conf.d/upstream-active.conf
```

### 4. 기존 server block 수정
호스트의 nginx 가 이미 SSL 설정되어 있으니, server block 의 `location /` 에 `proxy_pass http://monov_app;` 사용하도록 수정.
참고: `deploy/nginx/monov-server.conf.example`

```bash
sudo nginx -t && sudo systemctl reload nginx
```

### 5. sudoers — passwordless nginx reload
배포 사용자(예: `ubuntu`) 의 NOPASSWD 추가:
```bash
sudo visudo -f /etc/sudoers.d/deploy-nginx
```
```
ubuntu ALL=(ALL) NOPASSWD: /usr/bin/ln, /usr/sbin/nginx, /usr/bin/systemctl reload nginx
```

### 6. 첫 배포는 GitHub Actions 한 번 돌리면 됨
- `git push main` 또는 Actions 탭에서 "Run workflow"
- workflow 가 yml 박힌 이미지 build/push → EC2 에서 pull → blue-green 전환
- `deploy.sh` 가 첫 실행 시 `.active` 파일 없으면 blue 로 시작

### (선택) 인프라 컨테이너 미리 띄워두기
배포 전에 redis / prometheus / grafana 만 띄워두면 첫 배포 빨라짐:
```bash
DOCKERHUB_USERNAME=woals2840 docker compose up -d redis prometheus grafana
```

## 일상 운영

### 배포
- **자동**: `git push main` → Actions 자동 실행
- **수동 (Actions UI)**: Actions 탭 → "CD" → "Run workflow"
- **수동 (EC2 직접)**: 이미지가 이미 push 된 상태라면
  ```bash
  ssh ec2 "cd ~/monov-ai && DOCKERHUB_USERNAME=woals2840 IMAGE_TAG=<sha> ./deploy/deploy.sh"
  ```

### 롤백
배포 직후 문제 발견 시:
```bash
ssh ec2
cd ~/monov-ai
ACTIVE=$(cat .active)
[ "$ACTIVE" = "blue" ] && OLD=green || OLD=blue
sudo ln -sf /etc/nginx/upstream-$OLD.conf /etc/nginx/conf.d/upstream-active.conf
sudo nginx -t && sudo systemctl reload nginx
echo $OLD > .active
docker compose up -d app-$OLD     # 멈춰있을 가능성
docker compose stop app-$ACTIVE
```

### 로그 확인
```bash
docker logs monov-app-blue --tail 100 -f
docker logs monov-app-green --tail 100 -f
```

### Grafana 접근 (외부 노출 X — SSH tunnel)
```bash
# 로컬 PC 에서
ssh -L 3000:localhost:3000 ec2-host
# 브라우저에서 http://localhost:3000 (admin / GRAFANA_PASSWORD)
```

추천 dashboard:
- Spring Boot Statistics (ID: `4701`) — JVM, HTTP, DB pool

## 트러블슈팅

### 배포 후 readiness 실패
- `docker logs monov-app-{색}` 확인
- 흔한 원인: DB 연결 실패 (RDS 보안그룹), `APPLICATION` 시크릿의 yml 값 오류
- `APPLICATION` 시크릿 점검 → workflow 재실행 (이미지부터 다시 빌드되어야 yml 갱신됨)

### 두 컨테이너 모두 메모리 부족
- t3.small (2GB) 은 부족 — t3.medium (4GB) 이상 권장
- Spring Boot 인스턴스 1개 = ~700MB ~ 1GB

### 스키마 마이그레이션
- `application-prod.yml` 의 `ddl-auto: validate` — Hibernate 가 검증만 함, 변경 X
- 스키마 변경 시: Flyway 도입 필수 (별도 작업)

## 주의사항

⚠️ **Blue-Green 의 약점 — DB 스키마 호환성**
- blue 와 green 이 같은 DB 본다 → 스키마 변경 시 양쪽 모두 호환되어야 함
- 컬럼 추가는 OK, 삭제/타입 변경은 2단계 (추가 → 새 코드 배포 → 옛 컬럼 제거 배포)

⚠️ **Grafana / Prometheus 외부 노출 안 됨**
- `127.0.0.1` 바인딩 + docker network 만
- 외부 access 필요 시 nginx subdomain 추가 + cert + basic auth