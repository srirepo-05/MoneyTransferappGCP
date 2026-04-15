# Backend Handover Report for Frontend Team

This document gives everything needed to integrate the frontend with the Money Transfer backend.

## 1) Quick Facts

- **Project**: MoneyTransfer backend (Spring Boot)
- **API base URL**: `http://localhost:8080`
- **Context path**: `/`
- **API prefix**: `/api/v1`
- **Auth**: HTTP Basic Auth (stateless)
- **Default API users**:
  - `admin` / `admin` (roles: `USER`, `ADMIN`)
  - `user` / `password` (role: `USER`)
- **Database (current runtime config)**: MySQL
- **Seed data**: Loaded automatically from `src/main/resources/db/schema.sql` and `src/main/resources/db/data.sql`

## 2) Run Backend Locally (for frontend integration)

1. Ensure MySQL is running and create DB:
   - DB name: `moneytransfer`
2. Ensure credentials match backend config in `src/main/resources/application.properties`:
   - username: `root`
   - password: `admin`
3. Start backend from project root:

```powershell
Set-Location "C:\Users\YD5\Documents\GCP capstone\backend\MoneyTransferAppGCP"
.\mvnw.cmd spring-boot:run
```

When backend is up, API is reachable at `http://localhost:8080`.

## 3) Authentication for Frontend

All `/api/v1/**` endpoints require Basic Auth.

### Browser/frontend header format

`Authorization: Basic <base64(username:password)>`

### JavaScript helper

```js
const API_BASE_URL = "http://localhost:8080";

function basicAuthHeader(username, password) {
  const token = btoa(`${username}:${password}`);
  return `Basic ${token}`;
}
```

## 4) CORS Note (Important)

This backend currently does **not** expose an explicit CORS configuration.

- If frontend runs on a different origin (example: `http://localhost:3000`), browser calls can fail with CORS/preflight issues.
- For local frontend development, use one of these:
  1. A dev proxy in frontend toolchain (preferred for now)
  2. Add CORS config on backend (requires backend code change)

## 5) API Contract

## 5.1 Accounts

### `GET /api/v1/accounts/{id}`
Returns full account details.

- **Success**: `200 OK`
- **Response**:

```json
{
  "id": 1,
  "holderName": "Alice Johnson",
  "balance": 10000.00,
  "status": "ACTIVE",
  "lastUpdated": "2026-04-15T10:12:30.123"
}
```

`status` enum values:
- `ACTIVE`
- `LOCKED`
- `CLOSED`

---

### `GET /api/v1/accounts/{id}/balance`
Returns only account balance.

- **Success**: `200 OK`
- **Response**:

```json
{
  "balance": 10000.00
}
```

---

### `GET /api/v1/accounts/{id}/transactions?page=0&size=20`
Returns paginated transfer history (transactions where account is sender or receiver).

- **Success**: `200 OK`
- **Query params**:
  - `page` (default `0`)
  - `size` (default `20`)
  - optional sorting params supported by Spring Data (`sort=createdOn,desc` etc.)
- **Response shape**: Spring `Page<TransferResponse>`

```json
{
  "content": [
    {
      "transactionId": "a1b2c3d4-0001-0001-0001-000000000001",
      "fromAccountId": 1,
      "toAccountId": 2,
      "amount": 500.00,
      "status": "SUCCESS",
      "failureReason": null,
      "idempotencyKey": "seed-tx-001",
      "createdOn": "2026-04-13T08:00:00"
    }
  ],
  "pageable": {},
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0,
  "first": true,
  "last": true,
  "numberOfElements": 1,
  "sort": {},
  "empty": false
}
```

> Frontend should rely mainly on: `content`, `totalElements`, `totalPages`, `size`, `number`, `first`, `last`, `empty`.

## 5.2 Transfers

### `POST /api/v1/transfers`
Executes transfer from one account to another.

- **Success**: `201 Created`
- **Request body**:

```json
{
  "fromAccountId": 1,
  "toAccountId": 2,
  "amount": 500.00,
  "idempotencyKey": "web-uuid-001"
}
```

- **Validation rules**:
  - `fromAccountId`: required
  - `toAccountId`: required
  - `amount`: required, minimum `0.01`, max digits `15` integer + `4` fraction
  - `idempotencyKey`: required, non-blank

- **Success response** (`TransferResponse`):

```json
{
  "transactionId": "4d234b6d-ec0f-4f2d-8f29-9f9f4f5d6f82",
  "fromAccountId": 1,
  "toAccountId": 2,
  "amount": 500.00,
  "status": "SUCCESS",
  "failureReason": null,
  "idempotencyKey": "web-uuid-001",
  "createdOn": "2026-04-15T10:18:45.500"
}
```

`status` enum values:
- `SUCCESS`
- `FAILED`

## 6) Error Contract

All error responses use `ErrorResponse` from global exception handling.

### Generic error format

```json
{
  "status": 409,
  "error": "DUPLICATE_TRANSFER",
  "message": "Transfer with idempotency key 'web-uuid-001' already exists",
  "path": "/api/v1/transfers",
  "timestamp": "2026-04-15T10:20:01.123",
  "fieldErrors": null
}
```

### Validation error format (`400 VALIDATION_FAILED`)

```json
{
  "status": 400,
  "error": "VALIDATION_FAILED",
  "message": "Request validation failed. Check fieldErrors for details.",
  "path": "/api/v1/transfers",
  "timestamp": "2026-04-15T10:20:01.123",
  "fieldErrors": [
    {
      "field": "amount",
      "rejectedValue": "-50.00",
      "message": "Transfer amount must be at least 0.01"
    }
  ]
}
```

### HTTP status mapping

- `400`: `VALIDATION_FAILED`, `INVALID_ARGUMENT`
- `401`: Unauthorized (missing/wrong Basic credentials)
- `404`: `ACCOUNT_NOT_FOUND`
- `409`: `ACCOUNT_NOT_ACTIVE`, `INSUFFICIENT_BALANCE`, `DUPLICATE_TRANSFER`
- `500`: `INTERNAL_ERROR`

## 7) Idempotency Behavior (Frontend-critical)

- Idempotency is driven by `idempotencyKey` in transfer request.
- If the same key is sent again, backend returns `409 DUPLICATE_TRANSFER`.
- Current behavior is **reject duplicate**, not replaying previous success payload.

Frontend recommendation:
- Generate a new UUID per user transfer intent.
- Persist key in UI state while request is in-flight.
- On retry of same user intent (network uncertainty), reuse same key and handle `409` as "already processed / duplicate submit".

## 8) Seed Data Available for UI Development

Default seeded accounts:

| ID | Holder        | Balance   | Status |
|----|---------------|-----------|--------|
| 1  | Alice Johnson | 10000.00  | ACTIVE |
| 2  | Bob Smith     | 5000.00   | ACTIVE |
| 3  | Carol White   | 7500.00   | ACTIVE |
| 4  | David Brown   | 2500.00   | LOCKED |
| 5  | Eve Davis     | 0.00      | CLOSED |

Useful UI test flows:
- Success: `1 -> 2`, amount `500`
- Insufficient balance: very large amount from account `1`
- Not active: transfer from `4` (LOCKED) or `5` (CLOSED)
- Duplicate transfer: submit same `idempotencyKey` twice

## 9) Frontend Integration Snippets

### Transfer call (fetch)

```js
async function createTransfer(payload, username = "admin", password = "admin") {
  const res = await fetch("http://localhost:8080/api/v1/transfers", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Authorization": `Basic ${btoa(`${username}:${password}`)}`
    },
    body: JSON.stringify(payload)
  });

  const data = await res.json();

  if (!res.ok) {
    throw {
      httpStatus: res.status,
      code: data?.error,
      message: data?.message,
      fieldErrors: data?.fieldErrors || []
    };
  }

  return data;
}
```

### Account transactions call (fetch)

```js
async function getAccountTransactions(accountId, page = 0, size = 20, username = "admin", password = "admin") {
  const url = `http://localhost:8080/api/v1/accounts/${accountId}/transactions?page=${page}&size=${size}`;

  const res = await fetch(url, {
    headers: {
      "Authorization": `Basic ${btoa(`${username}:${password}`)}`
    }
  });

  const data = await res.json();

  if (!res.ok) {
    throw data;
  }

  return data;
}
```

## 10) Known Integration Gotchas

- CORS is not explicitly configured yet (cross-origin browser calls can fail).
- Basic Auth credentials are hardcoded in backend config (`SecurityConfig`), suitable for dev only.
- Transactions endpoint returns Spring Data `Page` wrapper; frontend should map pagination fields explicitly.
- Duplicate idempotency key returns conflict (`409`) by design.

## 11) Reference Docs

- API cURL examples: `TESTING.md`
- Main runtime config: `src/main/resources/application.properties`
- Security rules: `src/main/java/com/example/moneytransfer/config/SecurityConfig.java`
- Exception contract: `src/main/java/com/example/moneytransfer/exception/GlobalExceptionHandler.java`

