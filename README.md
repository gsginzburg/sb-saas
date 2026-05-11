# fast-saas-spring-boot: Distributed Multi-Tenant SaaS Platform

A Spring Boot / PostgreSQL framework for building distributed SaaS platforms with:

- Central **dispatch service** for auth, tenant, user, and cluster management
- **Cluster framework** — a Spring Boot starter that every tenant cluster application inherits
- Multi-shard PostgreSQL routing with automatic tenant-to-shard mapping
- JWT-based authentication with OAuth2 hooks for AWS Cognito and Firebase
- Angular 18 + PrimeNG management UI and sample tenant UI

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│  Browser                                            │
│  ┌─────────────────┐   ┌──────────────────────────┐ │
│  │  ui-dispatch    │   │  ui-cluster-sample       │ │
│  │  :4200          │   │  :4201                   │ │
│  └────────┬────────┘   └────────────┬─────────────┘ │
└───────────┼──────────────────────────┼───────────────┘
            │ REST                     │ REST
   ┌─────────▼────────┐      ┌─────────▼──────────────┐
   │ dispatch-service │      │  cluster-sample         │
   │ :8080            │◄─────│  :8081                  │
   │                  │ HTTP │  (cluster-framework)    │
   └─────────┬────────┘      └───────┬────────┬────────┘
             │                       │        │
   ┌─────────▼────────┐  ┌───────────▼─┐  ┌───▼────────┐
   │ postgres-dispatch│  │postgres-    │  │postgres-   │
   │ :5432            │  │shard1 :5432 │  │shard2 :5432│
   └──────────────────┘  └─────────────┘  └────────────┘
```

### Modules

| Module              | Description                                                        |
|---------------------|--------------------------------------------------------------------|
| `shared-core`       | Shared DTOs, JWT types (`TokenType`, `UserType`, `UserRole`), exceptions, `JwtService` |
| `dispatch-service`  | Central auth and management service (port 8080). Owns tenants, users, clusters |
| `cluster-framework` | Spring Boot auto-configuration starter for tenant-aware cluster apps |
| `cluster-sample`    | Sample cluster application built on `cluster-framework` (port 8081) |
| `ui-dispatch`       | Angular 18 management UI — backoffice login, tenant/cluster CRUD (port 4200) |
| `ui-cluster-sample` | Angular 18 sample tenant UI — exchange-token login, tenant-scoped views (port 4201) |

---

## Quick Start

### Prerequisites

- Docker Desktop (or Docker Engine + Docker Compose v2)
- Java 21
- Node.js 20+
- Maven 3.9+

### Start everything with Docker Compose

```bash
# 1. Build all Spring Boot fat-jars
mvn package -DskipTests

# 2. Build images and start all services
docker compose up --build

# 3. Verify services are healthy
docker compose ps
```

Services will be available at:

| Service            | URL                                  |
|--------------------|--------------------------------------|
| dispatch-service   | http://localhost:8080                |
| dispatch Swagger   | http://localhost:8080/swagger-ui.html |
| cluster-sample     | http://localhost:8081                |
| cluster Swagger    | http://localhost:8081/swagger-ui.html |
| ui-dispatch        | http://localhost:4200                |
| ui-cluster-sample  | http://localhost:4201                |

### Shut down

```bash
# Stop all containers (data volumes are preserved)
docker compose down

# Stop and delete volumes (wipes all database data)
docker compose down -v
```

---

## Local Development

Use the dev compose file to start only the databases, then run each service from your IDE or terminal.

### Step 1 — Start dev databases

```bash
docker compose -f docker-compose.dev.yml up -d
```

This exposes:

| Container           | Host port | JDBC URL                                        |
|---------------------|-----------|-------------------------------------------------|
| postgres-dispatch   | 5432      | `jdbc:postgresql://localhost:5432/dispatch`     |
| postgres-shard1     | 5433      | `jdbc:postgresql://localhost:5433/cluster`      |
| postgres-shard2     | 5434      | `jdbc:postgresql://localhost:5434/cluster`      |

### Step 2 — Run dispatch-service

```bash
cd dispatch-service
mvn spring-boot:run
# Runs on http://localhost:8080
# Liquibase runs automatically on startup and seeds the admin user
```

### Step 3 — Run cluster-sample

```bash
cd cluster-sample
mvn spring-boot:run
# Runs on http://localhost:8081
# Connects to dispatch-service at http://localhost:8080
```

### Step 4 — Run Angular UIs

```bash
# Terminal 1: dispatch management UI
cd ui-dispatch
npm install
npm start   # http://localhost:4200

# Terminal 2: cluster sample tenant UI
cd ui-cluster-sample
npm install
npm start   # http://localhost:4201
```

---

## Default Credentials

The `V002` Liquibase seed migration creates an initial admin user:

| Field    | Value                |
|----------|----------------------|
| Email    | `admin@dispatch.local` |
| Password | `Admin@1234`         |
| Role     | `BACKOFFICE_ADMIN`   |

Log in at http://localhost:4200 (or POST to `/api/auth/login` on port 8080).

---

## JWT Authentication Flow

```
1. User POSTs to   POST /api/auth/login          (dispatch-service :8080)
                   { "email": "...", "password": "..." }

2a. BACKOFFICE user → receives { token: "<BACKOFFICE JWT>", tokenType: "BACKOFFICE" }
    Valid 8 hours.  Use as Bearer on all /api/** dispatch endpoints.

2b. TENANT user    → receives { token: "<TENANT_EXCHANGE JWT>", tokenType: "TENANT_EXCHANGE",
                                clusterUrl: "http://...:8081" }
    Exchange token is valid for 5 minutes only.

3. Frontend POSTs to  POST /api/auth/exchange     (cluster-sample :8081)
                      Authorization: Bearer <TENANT_EXCHANGE JWT>

4. Cluster validates exchange token against dispatch, issues CLUSTER_SESSION JWT.
   Response: { token: "<CLUSTER_SESSION JWT>", tokenType: "CLUSTER_SESSION" }

5. All cluster API calls use: Authorization: Bearer <CLUSTER_SESSION JWT>
```

---

## Configuration Reference

### dispatch-service

All values can be overridden via environment variables (defaults shown for local dev):

| Environment variable    | Default                                         | Description                               |
|-------------------------|-------------------------------------------------|-------------------------------------------|
| `DISPATCH_DB_URL`       | `jdbc:postgresql://localhost:5432/dispatch`     | PostgreSQL JDBC URL                       |
| `DISPATCH_DB_USER`      | `dispatch`                                      | Database username                         |
| `DISPATCH_DB_PASS`      | `dispatch`                                      | Database password                         |
| `JWT_SECRET`            | *(insecure dev default)*                        | HS256 secret — must be 256+ bits in prod  |
| `CORS_ORIGINS`          | `http://localhost:4200,http://localhost:4201`   | Comma-separated allowed CORS origins      |
| `AUTH_PROVIDER`         | `native`                                        | `native`, `cognito`, or `firebase`        |
| `COGNITO_USER_POOL_ID`  | *(empty)*                                       | Required when `AUTH_PROVIDER=cognito`     |
| `COGNITO_REGION`        | `us-east-1`                                     | AWS region for Cognito                    |
| `FIREBASE_PROJECT_ID`   | *(empty)*                                       | Required when `AUTH_PROVIDER=firebase`    |

### cluster-sample

| Environment variable | Default                              | Description                                     |
|----------------------|--------------------------------------|-------------------------------------------------|
| `CLUSTER_ID`         | *(required)*                         | Unique cluster identifier registered in dispatch |
| `CLUSTER_NAME`       | *(required)*                         | Human-readable cluster name                     |
| `DISPATCH_URL`       | `http://localhost:8080`              | Base URL of the dispatch service                |
| `JWT_SECRET`         | *(insecure dev default)*             | Must match dispatch-service JWT secret          |
| `SHARD1_DB_URL`      | `jdbc:postgresql://localhost:5433/cluster` | JDBC URL for shard 1                      |
| `SHARD1_DB_USER`     | `cluster`                            | Shard 1 database username                       |
| `SHARD1_DB_PASS`     | `cluster_pass`                       | Shard 1 database password                       |
| `SHARD2_DB_URL`      | `jdbc:postgresql://localhost:5434/cluster` | JDBC URL for shard 2                      |
| `SHARD2_DB_USER`     | `cluster`                            | Shard 2 database username                       |
| `SHARD2_DB_PASS`     | `cluster_pass`                       | Shard 2 database password                       |
| `CORS_ORIGINS`       | `http://localhost:4201`              | Comma-separated allowed CORS origins            |

Shards are configured under `cluster.shards.shard-1` and `cluster.shards.shard-2` in `application.yml`.
The environment variables above map to those keys via Spring's relaxed binding.

---

## Extending the Platform

### Adding a new cluster application

1. Create a new Maven module (e.g. `cluster-billing`) with `cluster-framework` as a dependency.
2. Add your JPA entities in `org.gsginzburg.<module>.domain.model`, annotated with the tenant schema.
3. Add Liquibase migrations to `src/main/resources/db/changelog/changelog-instance.xml`.
4. Add your Spring MVC controllers and service classes.
5. Configure `cluster.shards.*` in `application.yml` following the same pattern as `cluster-sample`.
6. Add a `Dockerfile` alongside the module and wire the new service into `docker-compose.yml`.

### Adding new dispatch tables and APIs

1. Add a numbered Liquibase changeset to `dispatch-service/src/main/resources/db/changelog/changes/`.
2. Add a JPA entity in `org.gsginzburg.dispatch.domain.model`.
3. Add the Spring Data repository, service, and `@RestController`.
4. Document the new endpoint in the OpenAPI annotations so it appears in Swagger UI.

### Switching authentication provider

Set `AUTH_PROVIDER` to `cognito` or `firebase` and supply the corresponding environment variables.
No code changes are required — the dispatch service auto-configures the selected provider.

---

## License

[Apache License 2.0](LICENSE)
