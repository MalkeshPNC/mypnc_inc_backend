# PNC Masters API

Spring Boot API for customers, contacts, salespeople, and documents. Backed by MySQL, Spring Data JPA, and Flyway.

## Requirements

- Java 21
- Maven 3.9+ (or Maven Wrapper, when generated)
- MySQL 8+

Create the database before starting the application:

```sql
CREATE DATABASE mypncinc CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Set the connection values in the environment. The defaults target a local MySQL instance with the `root` user and an empty password; use explicit values outside local development:

```powershell
$env:MYSQL_URL = "jdbc:mysql://localhost:3306/mypncinc?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:MYSQL_USER = "root"
$env:MYSQL_PASSWORD = "your-password"
```

Optional document storage override (default is `./data/documents`):

```powershell
$env:DOCUMENT_STORAGE_ROOT = "D:\data\documents"
```

Flyway creates the customer, contact, salesperson, customer-salesperson, and document tables on application startup. Contacts are linked to customers through the required `cust_id` foreign key.

If you already applied an older copy of V1 or V2 and Flyway reports a checksum mismatch after pulling these SQL fixes, run `mvn flyway:repair` once (with the same `MYSQL_*` values), then start the app again.

## Run

```powershell
mvn spring-boot:run
```

Run tests and create the executable package with:

```powershell
mvn test
mvn package
```

The API is available at `http://localhost:8080`. OpenAPI UI is at `http://localhost:8080/swagger-ui/index.html`. Health is at `http://localhost:8080/actuator/health`.

## Endpoints

| Method | Path | Description |
| --- | --- | --- |
| GET | `/api/v1/customers` | List customers |
| GET | `/api/v1/customers/{id}` | Get one customer |
| POST | `/api/v1/customers` | Create a customer |
| PUT | `/api/v1/customers/{id}` | Replace a customer |
| DELETE | `/api/v1/customers/{id}` | Soft-delete a customer |
| GET | `/api/v1/contacts` | List contacts |
| GET | `/api/v1/contacts/{id}` | Get one contact |
| GET | `/api/v1/contacts/customer/{customerId}` | List contacts for a customer |
| GET | `/api/v1/contacts/company/{companyName}` | Search contacts by company name |
| POST | `/api/v1/contacts` | Create a contact |
| PUT | `/api/v1/contacts/{id}` | Replace a contact |
| DELETE | `/api/v1/contacts/{id}` | Delete a contact |
| GET | `/api/v1/salespersons` | List salespeople |
| GET | `/api/v1/salespersons/{id}` | Get one salesperson |
| POST | `/api/v1/salespersons` | Create a salesperson |
| PUT | `/api/v1/salespersons/{id}` | Replace a salesperson |
| DELETE | `/api/v1/salespersons/{id}` | Delete a salesperson |
| GET | `/api/v1/documents` | List documents (`customerId` and `category` optional) |
| POST | `/api/v1/documents` | Upload a document (`category` required, `customerId` optional) |
| GET | `/api/v1/documents/{id}/content` | Download document content |
| DELETE | `/api/v1/documents/{id}` | Delete a document |

Example create request:

```json
{
  "customer": "Acme Inc",
  "companyLogo": "https://example.com/logo.png",
  "salesPersons": [
    {
      "salesPersonId": 1,
      "commission": 12.50
    }
  ],
  "referredBy": "Partner network",
  "remarks": "Preferred account",
  "address": "1 Main Street",
  "city": "Boston",
  "state": "MA",
  "zip": "02108",
  "billtoAddress": "1 Main Street, Boston, MA 02108",
  "shiptoAddress": "1 Main Street, Boston, MA 02108",
  "automailOn": true
}
```

`customer` is required. When `custEntryDt` is omitted, the server sets it to the current date and time. `salesPersons` can contain multiple assignments, and commission belongs to each customer-salesperson assignment. Missing customer or salesperson IDs return `404`, and invalid request bodies return `400` with field-level validation details.

Example contact create request:

```json
{
  "customerId": 1,
  "firstName": "John",
  "lastName": "Doe",
  "phone": "555-1234",
  "email": "john@example.com",
  "contactPerson": "John Doe"
}
```

`customerId` is required and must reference an existing customer. Contact responses include `customerId` rather than the nested customer object.
