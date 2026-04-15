# Money Transfer API — cURL Testing Guide

Base URL: `http://localhost:8080`  
Authentication: **HTTP Basic Auth**

| User    | Password   | Role        |
|---------|------------|-------------|
| `admin` | `admin`    | USER, ADMIN |
| `user`  | `password` | USER        |

---

## Seed Data (from data.sql)

| ID | Holder        | Balance    | Status  |
|----|---------------|------------|---------|
| 1  | Alice Johnson | 10,000.00  | ACTIVE  |
| 2  | Bob Smith     | 5,000.00   | ACTIVE  |
| 3  | Carol White   | 7,500.00   | ACTIVE  |
| 4  | David Brown   | 2,500.00   | LOCKED  |
| 5  | Eve Davis     | 0.00       | CLOSED  |

---

## 1. Account Endpoints

### Get Account Details
```bash
curl -u admin:admin \
  http://localhost:8080/api/v1/accounts/1
```

### Get Account Balance
```bash
curl -u admin:admin \
  http://localhost:8080/api/v1/accounts/1/balance
```

### Get Transaction History (paginated)
```bash
curl -u admin:admin \
  "http://localhost:8080/api/v1/accounts/1/transactions?page=0&size=10"
```

---

## 2. Transfer Endpoints

### ✅ Successful Transfer
Transfer $500 from Alice (1) to Bob (2):
```bash
curl -u admin:admin \
  -X POST http://localhost:8080/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d "{\"fromAccountId\": 1, \"toAccountId\": 2, \"amount\": 500.00, \"idempotencyKey\": \"tx-001\"}"
```

### ✅ Another Successful Transfer
Transfer $200 from Bob (2) to Carol (3):
```bash
curl -u admin:admin \
  -X POST http://localhost:8080/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d "{\"fromAccountId\": 2, \"toAccountId\": 3, \"amount\": 200.00, \"idempotencyKey\": \"tx-002\"}"
```

---

## 3. Error Scenarios

### ❌ Insufficient Balance
```bash
curl -u admin:admin \
  -X POST http://localhost:8080/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d "{\"fromAccountId\": 1, \"toAccountId\": 2, \"amount\": 99999.00, \"idempotencyKey\": \"tx-err-001\"}"
```

### ❌ Account Not Found
```bash
curl -u admin:admin \
  -X POST http://localhost:8080/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d "{\"fromAccountId\": 999, \"toAccountId\": 2, \"amount\": 100.00, \"idempotencyKey\": \"tx-err-002\"}"
```

### ❌ Account Not Active — LOCKED (David, id=4)
```bash
curl -u admin:admin \
  -X POST http://localhost:8080/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d "{\"fromAccountId\": 4, \"toAccountId\": 1, \"amount\": 100.00, \"idempotencyKey\": \"tx-err-003\"}"
```

### ❌ Account Not Active — CLOSED (Eve, id=5)
```bash
curl -u admin:admin \
  -X POST http://localhost:8080/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d "{\"fromAccountId\": 5, \"toAccountId\": 1, \"amount\": 100.00, \"idempotencyKey\": \"tx-err-004\"}"
```

### ❌ Duplicate Transfer (Idempotency — reuse tx-001)
```bash
curl -u admin:admin \
  -X POST http://localhost:8080/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d "{\"fromAccountId\": 1, \"toAccountId\": 2, \"amount\": 500.00, \"idempotencyKey\": \"tx-001\"}"
```

### ❌ Validation — Negative Amount
```bash
curl -u admin:admin \
  -X POST http://localhost:8080/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d "{\"fromAccountId\": 1, \"toAccountId\": 2, \"amount\": -50.00, \"idempotencyKey\": \"tx-err-005\"}"
```

### ❌ Validation — Missing Fields (empty body)
```bash
curl -u admin:admin \
  -X POST http://localhost:8080/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d "{}"
```

---

## 4. Authentication Errors

### ❌ No Credentials → 401 Unauthorized
```bash
curl -X POST http://localhost:8080/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d "{\"fromAccountId\": 1, \"toAccountId\": 2, \"amount\": 100.00, \"idempotencyKey\": \"tx-noauth\"}"
```

### ❌ Wrong Credentials → 401 Unauthorized
```bash
curl -u wrong:credentials \
  http://localhost:8080/api/v1/accounts/1
```

---

## 5. Windows cmd.exe (escape inner quotes with backslash)

```cmd
curl -u admin:admin -X POST http://localhost:8080/api/v1/transfers -H "Content-Type: application/json" -d "{\"fromAccountId\": 1, \"toAccountId\": 2, \"amount\": 500.00, \"idempotencyKey\": \"tx-win-001\"}"
```

## 6. PowerShell Alternative

```powershell
$base64 = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("admin:admin"))

Invoke-RestMethod -Uri "http://localhost:8080/api/v1/transfers" `
  -Method POST `
  -Headers @{ "Content-Type" = "application/json"; "Authorization" = "Basic $base64" } `
  -Body '{"fromAccountId": 1, "toAccountId": 2, "amount": 500.00, "idempotencyKey": "tx-ps-001"}'
```
