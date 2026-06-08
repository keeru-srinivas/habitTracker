#!/bin/bash
#
# Habit Tracker — deploy FastAPI + nginx + Let's Encrypt (habit.thatinsaneguy.com)
#
# Usage (from this directory):
#   ./deploy.sh                    # app + pm2 (no nginx unless root)
#   sudo ./deploy.sh               # full: venv, pm2, nginx, certbot
#
# Optional env:
#   HABITTRACKER_PORT=9210        # must match nginx upstream in nginx-habittracker.conf
#   DOMAIN=habit.thatinsaneguy.com
#   CERTBOT_EMAIL=you@example.com   # required for first non-interactive cert (Let's Encrypt)
#   SKIP_SSL=1                    # only HTTP nginx + app (no certbot)
#   SKIP_NGINX=1                  # only app (venv + pm2)
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_ROOT="${SCRIPT_DIR}"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ECOSYSTEM="${APP_ROOT}/ecosystem.config.cjs"
NGINX_TEMPLATE="${APP_ROOT}/nginx-habittracker.conf"

DOMAIN="${DOMAIN:-habit.thatinsaneguy.com}"
# Same default as config.py HABITTRACKER_PORT
PORT="${HABITTRACKER_PORT:-9210}"

GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

print_status() { echo -e "${GREEN}[INFO]${NC} $1"; }
print_header() { echo -e "${BLUE}$1${NC}"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }
print_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }

require_cmd() {
    if ! command -v "$1" &>/dev/null; then
        print_error "Missing command: $1"
        return 1
    fi
}

# --- Optional git pull (repo root = parent of backend/) ---------------------------
if [ -d "${REPO_ROOT}/.git" ] && command -v git &>/dev/null; then
    print_header "📥 Git (optional)"
    (cd "${REPO_ROOT}" && git pull --ff-only 2>/dev/null) && print_status "git pull OK" || print_warn "git pull skipped or failed"
fi

# --- App venv + deps --------------------------------------------------------------
print_header "📦 Python venv & dependencies"
cd "${APP_ROOT}"

if [ ! -f "requirements.txt" ]; then
    print_error "requirements.txt missing in ${APP_ROOT}"
    exit 1
fi

if [ ! -d "venv" ]; then
    print_status "Creating venv…"
    python3 -m venv venv
fi
# shellcheck disable=SC1091
source venv/bin/activate
pip install --upgrade pip >/dev/null
pip install -r requirements.txt

# --- Patch ecosystem port in nginx template awareness ------------------------------
export HABITTRACKER_PORT="${PORT}"

# --- PM2 ---------------------------------------------------------------------------
print_header "🚀 PM2 (uvicorn)"
if ! command -v pm2 &>/dev/null; then
    print_error "PM2 is not installed. Install: npm install -g pm2"
    exit 1
fi

if [ ! -f "${ECOSYSTEM}" ]; then
    print_error "Missing ${ECOSYSTEM}"
    exit 1
fi

print_status "Stopping existing habittracker process…"
pm2 delete habittracker >/dev/null 2>&1 || true

print_status "Freeing port ${PORT}…"
for pid in $(lsof -t -i ":${PORT}" 2>/dev/null || true); do
    kill "${pid}" 2>/dev/null || true
done
sleep 1
for pid in $(lsof -t -i ":${PORT}" 2>/dev/null || true); do
    kill -9 "${pid}" 2>/dev/null || true
done

HABITTRACKER_PORT="${PORT}" pm2 start "${ECOSYSTEM}"
pm2 save >/dev/null 2>&1 || true

print_status "Waiting for app…"
sleep 2
if curl -sf "http://127.0.0.1:${PORT}/api/health" >/dev/null; then
    print_status "Health check OK: http://127.0.0.1:${PORT}/api/health"
else
    print_warn "Health check failed — check: cd ${APP_ROOT} && pm2 logs habittracker"
fi

if [ "${SKIP_NGINX:-0}" = "1" ]; then
    print_header "✅ Done (SKIP_NGINX=1)"
    print_status "Local: http://127.0.0.1:${PORT}  API: /api/  Docs: /docs"
    exit 0
fi

# --- Nginx directory detection -----------------------------------------------------
nginx_install_conf() {
    local src="$1"
    local name="$2"
    if [ -d /etc/nginx/sites-available ]; then
        cp "${src}" "/etc/nginx/sites-available/${name}"
        mkdir -p /etc/nginx/sites-enabled
        ln -sf "/etc/nginx/sites-available/${name}" "/etc/nginx/sites-enabled/${name}"
        rm -f /etc/nginx/sites-enabled/default 2>/dev/null || true
        print_status "Installed site (Debian/Ubuntu style): sites-available/${name}"
        return 0
    fi
    if [ -d /etc/nginx/conf.d ]; then
        cp "${src}" "/etc/nginx/conf.d/${name}.conf"
        print_status "Installed site (conf.d): conf.d/${name}.conf"
        return 0
    fi
    return 1
}

substitute_port_in_conf() {
    # Ensure upstream port matches HABITTRACKER_PORT (default 9210 in template)
    local tmp
    tmp="$(mktemp)"
    sed "s/127.0.0.1:9210/127.0.0.1:${PORT}/g" "${NGINX_TEMPLATE}" >"${tmp}"
    echo "${tmp}"
}

# --- Nginx + SSL (requires root) ---------------------------------------------------
echo ""
print_header "🌐 Nginx + TLS"

if [ "${EUID:-$(id -u)}" -ne 0 ]; then
    print_warn "Not root — skipping nginx/certbot. Run on the server:"
    echo "  cd ${APP_ROOT} && sudo CERTBOT_EMAIL=your@email.com ./deploy.sh"
    echo "Or copy nginx manually:"
    echo "  sudo cp ${NGINX_TEMPLATE} /etc/nginx/sites-available/${DOMAIN}"
    echo "  (edit proxy_pass port if not ${PORT})"
    exit 0
fi

if ! command -v nginx &>/dev/null; then
    print_error "nginx not installed. e.g. apt install nginx / pacman -S nginx"
    exit 1
fi

if [ ! -f "${NGINX_TEMPLATE}" ]; then
    print_error "Missing ${NGINX_TEMPLATE}"
    exit 1
fi

CONF_TMP="$(substitute_port_in_conf)"
cleanup_conf() { rm -f "${CONF_TMP}"; }
trap cleanup_conf EXIT

if ! nginx_install_conf "${CONF_TMP}" "${DOMAIN}"; then
    print_error "Could not find /etc/nginx/sites-available or /etc/nginx/conf.d"
    exit 1
fi

print_status "Testing nginx configuration…"
if nginx -t 2>/dev/null; then
    systemctl reload nginx 2>/dev/null || service nginx reload 2>/dev/null || true
    print_status "Nginx reloaded (HTTP)"
else
    print_error "nginx -t failed — fix config and reload"
    exit 1
fi

if [ "${SKIP_SSL:-0}" = "1" ]; then
    print_warn "SKIP_SSL=1 — HTTP only. Set DNS A record for ${DOMAIN} to this server."
    print_header "✅ Deploy complete (no TLS)"
    print_status "http://${DOMAIN}/"
    exit 0
fi

# --- Certbot -----------------------------------------------------------------------
print_header "🔒 Let's Encrypt (certbot)"

if ! command -v certbot &>/dev/null; then
    print_status "Installing certbot…"
    if command -v apt-get &>/dev/null; then
        apt-get update -qq && apt-get install -y certbot python3-certbot-nginx
    elif command -v pacman &>/dev/null; then
        pacman -Sy --noconfirm certbot certbot-nginx 2>/dev/null || print_warn "pacman install certbot failed — install manually"
    elif command -v dnf &>/dev/null; then
        dnf install -y certbot python3-certbot-nginx 2>/dev/null || true
    else
        print_warn "Install certbot + nginx plugin for your OS, then re-run deploy."
    fi
fi

if ! command -v certbot &>/dev/null; then
    print_error "certbot not found after install attempt"
    exit 1
fi

EMAIL="${CERTBOT_EMAIL:-}"
if [ -z "${EMAIL}" ] && [ -f "${APP_ROOT}/.env" ]; then
    # Optional: CERTBOT_EMAIL= in .env without sourcing whole file
    EMAIL="$(grep -E '^[[:space:]]*CERTBOT_EMAIL=' "${APP_ROOT}/.env" 2>/dev/null | tail -1 | cut -d= -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//;s/^"//;s/"$//')" || true
fi

CERTBOT_COMMON=(--nginx -d "${DOMAIN}" --agree-tos --non-interactive)
if [ -n "${EMAIL}" ]; then
    CERTBOT_COMMON+=(--email "${EMAIL}")
else
    print_warn "CERTBOT_EMAIL not set — using register-unsafely-without-email (not recommended for production)"
    CERTBOT_COMMON+=(--register-unsafely-without-email)
fi

print_status "Requesting / renewing certificate for ${DOMAIN}…"
set +e
# Obtain or renew; --redirect adds HTTPS + HTTP→HTTPS in nginx when successful
certbot "${CERTBOT_COMMON[@]}" --redirect
CB=$?
set -e

if [ "${CB}" -ne 0 ]; then
    print_warn "certbot non-interactive failed (exit ${CB}). Trying interactive fallback…"
    printf '\nA\n1\n' | certbot --nginx -d "${DOMAIN}" || print_warn "certbot failed — ensure DNS A record for ${DOMAIN} points to this host and port 80 is reachable"
fi

if nginx -t 2>/dev/null; then
    systemctl reload nginx 2>/dev/null || service nginx reload 2>/dev/null || true
else
    print_error "nginx -t failed after certbot"
    exit 1
fi

echo ""
print_header "✅ Deployment complete"
print_status "Site + API: https://${DOMAIN}/"
print_status "OpenAPI:      https://${DOMAIN}/docs"
print_status "Local upstream: http://127.0.0.1:${PORT}"
echo ""
print_status "PM2: pm2 status | pm2 logs habittracker | pm2 restart habittracker"
