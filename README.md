# Inventory & Order Management API

## Project Overview
A Spring Boot REST API for managing inventory and orders.
Built with Java 17, Spring Boot, Oracle Database.

## How to Run

### Requirements
- Java 17 (JDK)
- Maven
- Oracle Database 19c
- Git

### Database Setup
Run these commands in SQL Plus:
ALTER SESSION SET CONTAINER = orclpdb;
CREATE USER inventory_user IDENTIFIED BY inventory123;
GRANT CONNECT, RESOURCE, DBA TO inventory_user;
GRANT UNLIMITED TABLESPACE TO inventory_user;

### Clone and Run
git clone <your-repo-url>
cd Inventory.Order.Management
mvn spring-boot:run

Server starts at: http://localhost:8080

---

## Database Schema

### Tables
| Table | Primary Key | Foreign Keys | Attributes |
|-------|-------------|--------------|------------|
| CATEGORIES | Cat_id | — | name |
| PRODUCTS | Prod_id | Cat_id → CATEGORIES | name, price, stock_quantity |
| CUSTOMERS | Cus_id | — | first_name, last_name, email |
| CUSTOMERS_PHONE | id | Cus_id → CUSTOMERS | phone_Number |
| ORDERS | Ord_id | Cus_id → CUSTOMERS | status, total_price, created_at |
| ORDER_ITEMS | OrdItem_id | Ord_id → ORDERS, Prod_id → PRODUCTS | quantity, unit_price |
| ORDER_STATUS_HISTORY | history_id | Ord_id → ORDERS | old_status, new_status, changed_at |

---

## API Endpoints

### Categories
| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | /api/categories | Create a category | 201 |
| GET | /api/categories | Get all categories | 200 |

### Products
| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | /api/products | Create a product | 201 |
| POST | /api/products/{productId}/stock-adjustment | Adjust stock manually | 200 |
| GET | /api/products/low-stock?threshold= | Get low stock report | 200 |

### Customers
| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | /api/customers | Create a customer | 201 |
| POST | /api/customers/{customerId}/phones | Add phone number | 201 |
| POST | /api/customers/{customerId}/orders | Create draft order | 201 |

### Orders
| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | /api/orders/{orderId}/items | Add item to order | 201 |
| DELETE | /api/orders/{orderId}/items/{itemId} | Remove item from order | 204 |
| POST | /api/orders/{orderId}/confirm | Confirm order | 200 |
| POST | /api/orders/{orderId}/cancel | Cancel order | 200 |
| POST | /api/orders/{orderId}/status | Update order status | 200 |
| GET | /api/orders/{orderId} | Get order details | 200 |
| GET | /api/orders/{orderId}/history | Get status history | 200 |

---

## Business Rules
1. Stock is NOT deducted when adding items to a DRAFT order
2. Stock IS deducted atomically only when order is CONFIRMED
3. If any item has insufficient stock at confirmation the entire order is rejected with 409
4. Cancelling a CONFIRMED order restores all stock back
5. Order status flow: DRAFT → CONFIRMED → SHIPPED → DELIVERED
6. CONFIRMED order can be CANCELLED (stock restored)
7. SHIPPED or DELIVERED orders cannot be cancelled
8. Every status change is recorded in ORDER_STATUS_HISTORY

---

## Assumptions Made
1. unit_price is locked at confirmation time
2. No authentication required as per assignment spec
3. Email must be unique per customer
4. Stock quantity cannot go below 0
5. A customer can have multiple phone numbers
6. Phone numbers are stored in a separate table (1NF compliance)

---

## Postman Test Cases

### Happy Path Tests
| # | Method | URL | Body | Expected |
|---|--------|-----|------|----------|
| 1 | POST | /api/categories | {"name":"Electronics"} | 201 |
| 2 | POST | /api/products | {"name":"Laptop","price":999.99,"stockQuantity":10,"categoryId":1} | 201 |
| 3 | POST | /api/customers | {"firstName":"John","lastName":"Doe","email":"john@example.com"} | 201 |
| 4 | POST | /api/customers/1/phones | {"phoneNumber":"0501234567"} | 201 |
| 5 | POST | /api/customers/1/orders | {} | 201 DRAFT |
| 6 | POST | /api/orders/1/items | {"productId":1,"quantity":2} | 201 |
| 7 | POST | /api/orders/1/confirm | — | 200 CONFIRMED |
| 8 | GET | /api/orders/1/history | — | 200 DRAFT→CONFIRMED |
| 9 | POST | /api/orders/1/status | {"status":"SHIPPED"} | 200 SHIPPED |
| 10 | POST | /api/orders/1/status | {"status":"DELIVERED"} | 200 DELIVERED |
| 11 | GET | /api/products/low-stock?threshold=11 | — | 200 Laptop qty=8 |

### Business Rule Violation Tests
| # | Method | URL | Body | Expected |
|---|--------|-----|------|----------|
| 12 | POST | /api/orders/1/confirm | — | 409 already confirmed |
| 13 | POST | /api/customers/1/orders then add qty=999 then confirm | — | 409 Insufficient stock |
| 14 | POST | /api/orders/1/cancel | — | 409 cannot cancel DELIVERED |
| 15 | GET | /api/orders/999 | — | 404 not found |
| 16 | POST | /api/products/1/stock-adjustment | {"amount":-9999} | 409 below zero |

### Stock Restoration Test
| # | Method | URL | Body | Expected |
|---|--------|-----|------|----------|
| 17 | POST | /api/customers/1/orders | {} | 201 new DRAFT |
| 18 | POST | /api/orders/2/items | {"productId":1,"quantity":3} | 201 stock still 8 |
| 19 | POST | /api/orders/2/confirm | — | 200 stock now 5 |
| 20 | POST | /api/orders/2/cancel | — | 200 stock back to 8 |

---

## Normalization
- 1NF: All columns atomic, every table has PK
- 2NF: No partial dependencies (all PKs are single column)
- 3NF: No transitive dependencies
- Phone numbers extracted to CUSTOMERS_PHONE table (1NF)
- Status history extracted to ORDER_STATUS_HISTORY table
