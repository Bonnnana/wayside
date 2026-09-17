#!/usr/bin/env bash
# Exercises the whole auth flow against a running API and checks each response.
#
#   Terminal 1:  cd backend/Wayside.Api && dotnet run
#   Terminal 2:  ./backend/test-auth.sh
#
# Override the host with BASE_URL=... if you're not on the default http profile.
set -uo pipefail

BASE_URL="${BASE_URL:-http://localhost:5263}"
LOG="${LOG:-/tmp/wayside-api.log}"
EMAIL="test-$(date +%s)@example.com"
PASSWORD='Wayside!23'
NEW_PASSWORD='NewPass!45'
JSON='Content-Type: application/json'

pass=0; fail=0

# check <description> <expected> <actual>
check() {
  if [[ "$2" == "$3" ]]; then
    printf '  \033[32m✓\033[0m %s\n' "$1"; pass=$((pass + 1))
  else
    printf '  \033[31m✗\033[0m %s — expected %s, got %s\n' "$1" "$2" "$3"; fail=$((fail + 1))
  fi
}

status() { curl -s -o /dev/null -w '%{http_code}' "$@"; }

if ! curl -sf "$BASE_URL/health" > /dev/null; then
  echo "No API at $BASE_URL — start it with: cd Wayside.Api && dotnet run"
  exit 1
fi

echo "Testing $BASE_URL as $EMAIL"

echo "Register"
body=$(printf '{"email":"%s","password":"%s","firstName":"Test","lastName":"User"}' "$EMAIL" "$PASSWORD")
check "creates the account" 200 "$(status -X POST "$BASE_URL/api/auth/register" -H "$JSON" -d "$body")"
check "rejects a duplicate email" 400 "$(status -X POST "$BASE_URL/api/auth/register" -H "$JSON" -d "$body")"

weak=$(printf '{"email":"weak-%s","password":"password","firstName":"A","lastName":"B"}' "$EMAIL")
check "rejects a weak password" 400 "$(status -X POST "$BASE_URL/api/auth/register" -H "$JSON" -d "$weak")"

echo "Login"
login=$(printf '{"email":"%s","password":"%s"}' "$EMAIL" "$PASSWORD")
check "accepts the right password" 200 "$(status -X POST "$BASE_URL/api/auth/login" -H "$JSON" -d "$login")"

# Capture the token from the LAST login: every login rotates the stored refresh token, so a
# token captured before another login is already stale.
resp=$(curl -s -X POST "$BASE_URL/api/auth/login" -H "$JSON" -d "$login")
refresh=$(printf '%s' "$resp" | sed -n 's/.*"refreshToken":"\([^"]*\)".*/\1/p')
check "returns a refresh token" "yes" "$([[ -n "$refresh" ]] && echo yes || echo no)"

wrong=$(printf '{"email":"%s","password":"Wrong!123"}' "$EMAIL")
check "rejects the wrong password" 401 "$(status -X POST "$BASE_URL/api/auth/login" -H "$JSON" -d "$wrong")"

# Same status AND same body as a wrong password, or the endpoint reveals who has an account.
unknown='{"email":"nobody-xyz@example.com","password":"Wrong!123"}'
a=$(curl -s -X POST "$BASE_URL/api/auth/login" -H "$JSON" -d "$wrong")
b=$(curl -s -X POST "$BASE_URL/api/auth/login" -H "$JSON" -d "$unknown")
check "unknown email is indistinguishable from a wrong password" "$a" "$b"

echo "Refresh"
resp=$(curl -s -X POST "$BASE_URL/api/auth/refresh" -H "$JSON" -d "{\"refreshToken\":\"$refresh\"}")
rotated=$(printf '%s' "$resp" | sed -n 's/.*"refreshToken":"\([^"]*\)".*/\1/p')
check "issues a new refresh token" "yes" "$([[ -n "$rotated" && "$rotated" != "$refresh" ]] && echo yes || echo no)"
check "refuses to reuse the old one" 401 \
  "$(status -X POST "$BASE_URL/api/auth/refresh" -H "$JSON" -d "{\"refreshToken\":\"$refresh\"}")"

echo "Forgot password"
check "accepts a known email" 200 \
  "$(status -X POST "$BASE_URL/api/auth/forgot-password" -H "$JSON" -d "{\"email\":\"$EMAIL\"}")"
check "answers the same for an unknown email" 200 \
  "$(status -X POST "$BASE_URL/api/auth/forgot-password" -H "$JSON" -d '{"email":"nobody-xyz@example.com"}')"

echo "Reset password"
token=$(grep -a "Password reset token for $EMAIL" "$LOG" 2>/dev/null | tail -1 | sed 's/.*: //')
if [[ -z "$token" ]]; then
  printf '  \033[33m—\033[0m reset token not found in %s (set LOG=..., or Email:Enabled is true)\n' "$LOG"
else
  reset=$(printf '{"email":"%s","token":"%s","newPassword":"%s"}' "$EMAIL" "$token" "$NEW_PASSWORD")
  check "accepts the emailed token" 200 "$(status -X POST "$BASE_URL/api/auth/reset-password" -H "$JSON" -d "$reset")"
  check "old password no longer works" 401 "$(status -X POST "$BASE_URL/api/auth/login" -H "$JSON" -d "$login")"
  newlogin=$(printf '{"email":"%s","password":"%s"}' "$EMAIL" "$NEW_PASSWORD")
  check "new password works" 200 "$(status -X POST "$BASE_URL/api/auth/login" -H "$JSON" -d "$newlogin")"
  check "changing the password ends existing sessions" 401 \
    "$(status -X POST "$BASE_URL/api/auth/refresh" -H "$JSON" -d "{\"refreshToken\":\"$rotated\"}")"
fi

printf '\n%d passed, %d failed\n' "$pass" "$fail"
[[ $fail -eq 0 ]]
