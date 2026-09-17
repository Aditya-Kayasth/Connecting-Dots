# Free Hosting Guide: Spring Boot Microservices on Oracle Cloud

**Your setup:** Java 25, Spring Boot 4.x, monorepo under `connecting-dots-backend/` with `eureka-server`, `gateway-service`, `core-service`, `ai-service`, each with a working multi-stage Dockerfile on `eclipse-temurin:25`, plus `connecting-dots-frontend/` (Next.js, containerized, but deployed separately on Vercel — not part of this VM). Neon Postgres, self-hosted Redis, QStash.

**Target:** One Oracle Cloud **Ampere A1** VM (always-on, no cold starts), your existing Docker Compose adapted for production, auto-HTTPS via Caddy, GitHub Actions building and deploying automatically on push.

**Note on Oracle's free tier:** as of mid-2026 Oracle quietly cut the Always Free Ampere A1 allowance from 4 OCPU/24GB to **2 OCPU/12GB RAM**. That's what this guide assumes. It's enough for your 4 services + Redis if you cap each JVM's memory — covered in Step 5.

**Good news:** `eclipse-temurin:25` is an official multi-arch image — it already publishes ARM64 builds under the same tag. Your Dockerfiles need no changes for the CPU architecture. The only thing to check is whether any service pulls in a native/x86-only dependency (uncommon in plain Spring Boot + JDBC/Redis clients — you're almost certainly fine).

---

## Step 1 — Create your Oracle Cloud account

1. Go to oracle.com/cloud/free and sign up. You'll need a phone number and a card for identity verification — **you will not be charged** as long as you stay on Always Free resources (no auto-upgrade happens without your explicit action).
2. Pick your **home region** at signup and don't change it later — free resources are pinned to this region. Choose one geographically close to you; if it says "out of capacity" for Ampere A1, note that Singapore/Tokyo/Frankfurt tend to have better free-tier availability than US regions.
3. Signups occasionally get flagged for manual review (common complaint, not something you did wrong) — if that happens, it can take a day or two, just wait it out.

## Step 2 — Create the VM (Ampere A1, Ubuntu)

1. Console → **Compute → Instances → Create Instance**.
2. Name it (e.g. `backend-host`).
3. **Image and shape** → Edit → choose **Ubuntu 24.04** → Change shape → **Ampere** → `VM.Standard.A1.Flex` → set **2 OCPUs / 12 GB memory** (matches the current free limit — going higher risks the instance being reclaimed).
4. **Networking**: keep the default VCN, and tick **"Assign a public IPv4 address."**
5. **Add SSH keys**: let Oracle generate a key pair and download the private key (`.key` file) — you'll use it to SSH in and later as a GitHub Actions secret. Keep it safe, don't commit it anywhere.
6. Boot volume: default 50GB is fine (you have 200GB free total).
7. Create. Wait a few minutes for it to go "Running," then note its **public IP**.

### Reserve the IP (so it never changes)
By default the public IP is ephemeral. Go to **Networking → IP Management → Reserved Public IPs**, reserve one (free), and attach it to your instance. This matters because your domain will point at this exact IP.

### Open the firewall (the #1 gotcha)
Oracle blocks everything but SSH by default, in **two places** — miss either one and your site won't load even though the VM is fine:

1. **Security List** (Networking → Virtual Cloud Networks → your VCN → Security Lists → Default Security List): add Ingress Rules for `0.0.0.0/0`, ports **80** and **443** (TCP).
2. **The VM's own firewall** (Ubuntu ships with iptables rules that also block these ports). SSH into the VM and run:
   ```bash
   sudo iptables -I INPUT -p tcp --dport 80 -j ACCEPT
   sudo iptables -I INPUT -p tcp --dport 443 -j ACCEPT
   sudo netfilter-persistent save
   ```

## Step 3 — Free subdomain with DuckDNS

1. Go to duckdns.org, sign in (GitHub login is fine), and create a subdomain, e.g. `yourproject.duckdns.org`.
2. Point it at your VM's reserved public IP — paste the IP into DuckDNS's dashboard and save.
3. Since you have a **static/reserved** IP, you don't need DuckDNS's auto-update cron script — set it once and leave it.

## Step 4 — Base server setup

SSH into the VM (`ssh -i your-key.key ubuntu@your-ip`) and install Docker:

```bash
sudo apt update && sudo apt upgrade -y
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
newgrp docker
sudo systemctl enable docker
```

Verify: `docker run hello-world`. Log out and back in if you get a permissions error.

## Step 5 — Two small checks on your existing Dockerfiles

You don't need to rewrite anything, but check these two things in each of the 4 Dockerfiles:

**1. Memory capping.** If your final stage doesn't already set `JAVA_OPTS`/equivalent, add this so each JVM sizes itself to the memory limit you'll give it in Compose (Step 6), rather than assuming the host's full RAM:

```dockerfile
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

`UseSerialGC` is the right GC choice at this scale — G1 (Spring Boot's default) spends RAM on bookkeeping that only pays off with more heap than you have here.

**2. `curl` for the healthcheck.** Your compose file already has a smart `healthcheck` on `eureka-server` using `curl`. If your final image stage is based on a slim/alpine JRE image (likely, since `eclipse-temurin:25-jre-alpine` or similar is standard), **curl isn't installed by default** and that healthcheck will silently fail, which then blocks `gateway-service` and the others from ever starting (they `depend_on: condition: service_healthy`). Add this line to the `eureka-server` Dockerfile's final stage:

```dockerfile
# Alpine base:
RUN apk add --no-cache curl
# Debian/slim base instead, use:
# RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
```

Quick way to confirm this is actually an issue: run `docker compose up` locally and watch whether `eureka-server` ever reports `healthy` in `docker compose ps`. If it does, you already have curl and can skip this.

## Step 6 — docker-compose.prod.yml

Keep your existing `docker-compose.yml` exactly as-is for local dev (with `frontend` and the `build:` lines) — don't touch it. Add a **separate** `docker-compose.prod.yml` at the repo root for the VM, which pulls prebuilt images from GHCR instead of building locally, and drops `frontend` (that lives on Vercel):

```yaml
services:
  eureka-server:
    image: ghcr.io/<your-github-username>/connecting-dots-v2-eureka-server:latest
    container_name: eureka-server
    restart: unless-stopped
    mem_limit: 500m
    healthcheck:
      test: ["CMD-SHELL", "wget --no-verbose --tries=1 --spider http://localhost:8761/actuator/health || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis-cache:
    image: redis:7-alpine
    container_name: redis-cache
    restart: unless-stopped
    mem_limit: 300m
    volumes:
      - redis-data:/data

  gateway-service:
    image: ghcr.io/<your-github-username>/connecting-dots-v2-gateway-service:latest
    container_name: gateway-service
    restart: unless-stopped
    mem_limit: 500m
    environment:
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
      - REDIS_HOST=redis-cache
      - REDIS_PORT=6379
    depends_on:
      eureka-server:
        condition: service_healthy
      redis-cache:
        condition: service_started

  core-service:
    image: ghcr.io/<your-github-username>/connecting-dots-v2-core-service:latest
    container_name: core-service
    restart: unless-stopped
    mem_limit: 600m
    env_file: .env
    environment:
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
      - REDIS_HOST=redis-cache
      - REDIS_PORT=6379
      - AI_SERVICE_URL=http://ai-service:8082
    depends_on:
      eureka-server:
        condition: service_healthy
      redis-cache:
        condition: service_started

  ai-service:
    image: ghcr.io/<your-github-username>/connecting-dots-v2-ai-service:latest
    container_name: ai-service
    restart: unless-stopped
    mem_limit: 500m
    env_file: .env
    environment:
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
      - REDIS_HOST=redis-cache
      - REDIS_PORT=6379
      - CORE_SERVICE_URL=http://core-service:8081
    depends_on:
      eureka-server:
        condition: service_healthy
      redis-cache:
        condition: service_started

  caddy:
    image: caddy:2-alpine
    container_name: caddy
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./Caddyfile:/etc/caddy/Caddyfile
      - caddy-data:/data

volumes:
  redis-data:
  caddy-data:

networks:
  default:
    name: connecting-dots-network
    driver: bridge
```

That's ~2.4GB of container memory limits total, comfortably inside 12GB with room for the OS and Docker itself. Adjust a `mem_limit` up if a service OOMs — check with `docker stats`.

**Two changes from your local file worth noting:**
- **`REDIS_PASSWORD` is dropped.** Locally your `redis-cache` container has no auth, and this setup keeps that — self-hosting Redis on the same VM/Docker network as your services, same as your local setup, so there's no exposed password to manage. If you'd rather use a managed Redis (e.g. Upstash's free tier) instead of self-hosting, keep `REDIS_PASSWORD` in `.env` and point `REDIS_HOST`/`REDIS_PORT` at that instead — either works, self-hosting is just one less external dependency and one less thing that can hit a free-tier limit.
- **No host `ports:` published** except Caddy's 80/443. Your services still talk to each other fine over the internal `connecting-dots-network` by container name — they just aren't directly reachable from outside the VM anymore, only through Caddy. (Oracle's firewall would block them anyway since only 80/443 are open, but it's cleaner to not publish them at all.)

### `.env` (on the VM only — never commit this)
```
DB_URL=jdbc:postgresql://<your-neon-host>/<db>?sslmode=require
DB_USERNAME=...
DB_PASSWORD=...
QSTASH_URL=...
QSTASH_TOKEN=...
QSTASH_CURRENT_SIGNING_KEY=...
QSTASH_NEXT_SIGNING_KEY=...
AI_WEBHOOK_URL=https://yourproject.duckdns.org/...
CLOUDINARY_CLOUD_NAME=...
CLOUDINARY_API_KEY=...
CLOUDINARY_API_SECRET=...
JWT_SECRET=...
INTERNAL_SERVICE_SECRET=...
GEMINI_API_KEY=...
```
Note `AI_WEBHOOK_URL` — if QStash calls back into `ai-service` over the internet, that URL needs to be your real public domain (Step 7), not `localhost`, once you're off your desktop.

## Step 7 — Caddy for free automatic HTTPS

`Caddyfile` (next to `docker-compose.prod.yml`):

```
yourproject.duckdns.org {
    reverse_proxy gateway-service:8080
}
```

Only the gateway is exposed publicly, on purpose — Eureka's dashboard and your individual services shouldn't be reachable from the internet directly; your Gateway is the front door. Caddy fetches and renews a Let's Encrypt certificate automatically on first request — nothing else to configure. This is also the piece that makes QStash's webhook callbacks work: QStash needs a real HTTPS endpoint to deliver to, and now you have one (make sure `AI_WEBHOOK_URL` in `.env` points here, routed through the gateway's actual path to `ai-service`).

## Step 8 — GitHub Container Registry + Actions (the CI/CD part)

Since everything's one repo, one workflow file builds all 4 backend images and deploys. Note the `paths` filter — pushes that only touch `connecting-dots-frontend/` won't trigger a backend rebuild (Vercel handles that side independently via its own GitHub integration).

**`.github/workflows/deploy.yml`:**

```yaml
name: Build and Deploy Backend

on:
  push:
    branches: [main]
    paths:
      - 'connecting-dots-backend/**'

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    strategy:
      matrix:
        service: [eureka-server, gateway-service, core-service, ai-service]
    steps:
      - uses: actions/checkout@v4

      - name: Log in to GHCR
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Set up QEMU (for ARM64 builds)
        uses: docker/setup-qemu-action@v3

      - name: Set up Buildx
        uses: docker/setup-buildx-action@v3

      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: ./connecting-dots-backend/${{ matrix.service }}
          platforms: linux/arm64
          push: true
          tags: ghcr.io/${{ github.repository_owner }}/${{ github.event.repository.name }}-${{ matrix.service }}:latest

  deploy:
    needs: build-and-push
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Copy compose files to server
        uses: appleboy/scp-action@v0.1.7
        with:
          host: ${{ secrets.ORACLE_HOST }}
          username: ubuntu
          key: ${{ secrets.ORACLE_SSH_KEY }}
          source: "docker-compose.prod.yml,Caddyfile"
          target: "~/app"

      - name: Deploy on server
        uses: appleboy/ssh-action@v1.0.3
        with:
          host: ${{ secrets.ORACLE_HOST }}
          username: ubuntu
          key: ${{ secrets.ORACLE_SSH_KEY }}
          script: |
            cd ~/app
            echo "${{ secrets.ENV_FILE }}" > .env
            docker compose -f docker-compose.prod.yml pull
            docker compose -f docker-compose.prod.yml up -d
            docker image prune -f
```

`platforms: linux/arm64` is important — the GitHub Actions runner is x86, but your VM is ARM, so Buildx cross-compiles via QEMU. Builds will be a bit slower than native but this is the simplest path with zero extra setup.

**GitHub secrets to add** (repo → Settings → Secrets and variables → Actions):
- `ORACLE_HOST` — your VM's public IP
- `ORACLE_SSH_KEY` — the full contents of the private key file from Step 2
- `ENV_FILE` — the full contents of your `.env` file (Neon + QStash credentials), pasted as one secret

This way your real secrets never touch the repo — they're injected at deploy time straight onto the server.

## Step 9 — First deploy

On the VM, once:
```bash
mkdir -p ~/app && cd ~/app
```
Then push to `main` on GitHub. The workflow builds all 4 images, pushes them to GHCR, copies your compose files over, and starts everything. Check it worked:
```bash
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f gateway-service
docker compose -f docker-compose.prod.yml exec eureka-server curl -f http://localhost:8761/actuator/health
```
Then from your own machine: `https://yourproject.duckdns.org` should route through Caddy → `gateway-service`.

### Update Vercel's env var
Your frontend currently points `NEXT_PUBLIC_API_BASE_URL` at `http://localhost:8080`. In Vercel's project settings, set it to `https://yourproject.duckdns.org` instead, then redeploy the frontend (Vercel does this automatically on the next push, or trigger it manually once now). Until you do this, your live frontend will still be trying to call your laptop.

## Step 10 — Keeping the free VM alive

Oracle can reclaim (delete) an Always Free instance that's been essentially idle for a long stretch, and it also enforces the 2 OCPU/12GB cap strictly now — going over it risks the instance being stopped. Two easy habits:
- Keep total `mem_limit`s under 12GB with margin, as above.
- A live web service getting occasional requests won't be flagged as idle, but if you expect long gaps with zero traffic, a simple uptime pinger (e.g. a free UptimeRobot check hitting your domain every few minutes) keeps it active and also alerts you if it goes down.

## Troubleshooting quick reference

| Symptom | Likely cause |
|---|---|
| Site unreachable from browser, but VM shows running | Firewall — check both the OCI Security List **and** `iptables` on the VM (Step 2) |
| Caddy won't get a certificate | DNS hasn't propagated yet, or ports 80/443 aren't actually open — test with `curl -v http://yourproject.duckdns.org` |
| A service keeps restarting / OOM-killed | Check `docker stats`, raise that service's `mem_limit`, lower another's |
| GitHub Actions SSH step fails | Check the private key secret has no extra blank lines/whitespace; `ORACLE_HOST` is the bare IP, no `https://` |
| `eureka-server` never becomes `healthy`, everything else stuck waiting | Missing `curl` in the eureka-server image — see Step 5 |
| `core-service`/`ai-service` can't reach Eureka | Confirm `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` uses the container name `eureka-server`, not `localhost` |
| Frontend on Vercel gets network errors | `NEXT_PUBLIC_API_BASE_URL` on Vercel still points at `localhost:8080` — update it (Step 9) |
| QStash callback never arrives | `AI_WEBHOOK_URL` in `.env` still points at a local/tunnel URL instead of `https://yourproject.duckdns.org` |

---

**What you get at the end:** push to `main` → GitHub Actions builds and pushes 4 ARM images (`eureka-server`, `gateway-service`, `core-service`, `ai-service`) → SSHes into your Oracle VM → pulls and restarts everything → live at `https://yourproject.duckdns.org` with HTTPS, backed by your existing Neon DB and QStash, self-hosted Redis, zero monthly cost, and no cold starts. Your frontend keeps deploying to Vercel exactly as before, just pointed at the new backend URL.
