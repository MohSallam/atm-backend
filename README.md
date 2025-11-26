# ATM API

Spring Boot 3.5 service that simulates an ATM with login, account snapshot, deposits, and withdrawals. Includes Flyway migrations, seed data, JWT auth, Swagger UI, and JaCoCo coverage reports.

## Prerequisites
- Java 17+ (`brew install openjdk@17` on macOS; ensure `JAVA_HOME` points to it)
- Docker + Docker Compose (`brew install --cask docker`; start Docker Desktop once)
- `./gradlew` (wrapper included; no separate Gradle install needed)

## Quick start (fully dockerized)
1) From the project root (where `docker-compose.yml` lives), build and start the app + Postgres:
```bash
docker compose up --build
```
2) Open Swagger UI: `http://localhost:8080/swagger-ui.html` (or `/swagger-ui/index.html`).
- App listens on `localhost:8080`; Postgres is reachable on `localhost:5433` and internally as `db:5432`.
- Defaults (override via env vars): `SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/atm`, `SPRING_DATASOURCE_USERNAME=atm_user`, `SPRING_DATASOURCE_PASSWORD=atm_password`.
- Data persists in the named volume `db_data`; stop with `docker compose down` (add `-v` to drop the volume).

## Run locally (only DB in Docker)
1) Start Postgres in Docker:
```bash
docker compose up -d db
```
2) Run the app locally (Flyway migrations + seed data will run automatically):
```bash
./gradlew bootRun
```

## Troubleshooting
- `FATAL: role "atm_user" does not exist`: The existing Postgres volume may predate the compose env vars. Either recreate the container + volume (data will be wiped):
  ```bash
  cd <project-root>
  docker compose down -v
  docker compose up -d db
  ```
  or create the role/database manually in the running container:
  ```bash
  docker exec -it atm-postgres psql -U postgres -c "CREATE ROLE atm_user WITH LOGIN PASSWORD 'atm_password';"
  docker exec -it atm-postgres psql -U postgres -c "CREATE DATABASE atm OWNER atm_user;"
  ```

## Default configuration (can be overridden with env vars or a profile)
- DB: `jdbc:postgresql://localhost:5433/atm`, user `atm_user`, password `atm_password`
- Flyway migrations: `classpath:db/migration`
- JWT secret: `security.jwt.secret` in `application.yml`
- Account lock: `security.auth.max-failed-attempts=3`, `security.auth.lock-duration-minutes=15`

## Seed data (Flyway V2)
- Cards: `4111111111111111`, `5555444433331111`, `6011000000001111`
- PIN for all demo cards: `p@ssw0rd`
- Balances: 1200.00 (Alice Carter), 300.00 (Brian Lee), 5000.00 (Carla Gomez)
- Daily withdrawal limits: 500.00, 200.00, 1000.00 respectively

## API (high-level)
- `POST /api/v1/auth/login` — body `{"cardNumber": "...", "pin": "..."}` → returns `accessToken` (Bearer JWT)
- `GET /api/v1/account` — account snapshot (auth required)
- `POST /api/v1/account/deposit` — body `{"amount": 200.00}` (auth required)
- `POST /api/v1/account/withdraw` — body `{"amount": 100.00}` (auth required)

### Example flow (cURL)
```bash
# Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"cardNumber":"4111111111111111","pin":"p@ssw0rd"}' | jq -r .accessToken)

# Snapshot
curl -s http://localhost:8080/api/v1/account \
  -H "Authorization: Bearer $TOKEN" | jq .

# Withdraw
curl -s -X POST http://localhost:8080/api/v1/account/withdraw \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"amount": 100.00}' | jq .
```

## Common use cases & expected responses
- Login success:
  - `POST /api/v1/auth/login` with `{"cardNumber":"4111111111111111","pin":"p@ssw0rd"}`
  - 200 OK, body includes `accessToken`, `tokenType`, `expiresInSeconds`, `customerId`, `customerName`
- Login invalid PIN:
  - 401 UNAUTHORIZED, body `{"code":"UNAUTHORIZED","message":"Invalid PIN"}`
- Login while locked (after too many bad PINs):
  - 423 LOCKED, body `{"code":"LOCKED","message":"Account Locked, try again later"}`
- Account snapshot:
  - `GET /api/v1/account` with `Authorization: Bearer <token>`
  - 200 OK, body includes `balance`, `withdrawnToday`, `dailyLimit`, `customerId`, `customerName`
- Deposit success:
  - `POST /api/v1/account/deposit` with `{"amount":200.00}` and auth
  - 200 OK, updated balances in body
- Withdraw over balance:
  - 409 CONFLICT, body `{"code":"CONFLICT","message":"Insufficient funds"}`
- Withdraw over daily limit:
  - 409 CONFLICT, body `{"code":"CONFLICT","message":"Daily withdrawal limit exceeded"}`

## Testing
- Unit/integration tests (uses in-memory H2 + Flyway via `application-test.yml`):
```bash
./gradlew test
```
- Full build:
```bash
./gradlew build
```

## Coverage report (JaCoCo)
- Generate:
```bash
./gradlew jacocoTestReport
```
- View HTML: open `jacoco-report/index.html`
- XML: `build/reports/jacoco/test/jacocoTestReport.xml` (for tooling)

## Useful development notes
- Flyway runs automatically on startup; adjust migrations under `src/main/resources/db/migration`.
- SpringDoc config is in `src/main/java/com/exercise/atm/config/OpenApiConfig.java`.
- Security (JWT + endpoint rules) is in `src/main/java/com/exercise/atm/config/security/SecurityConfig.java`.
- Main entrypoint: `src/main/java/com/exercise/atm/AtmApplication.java`.
