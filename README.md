<div align="center">
  <img src="https://img.shields.io/badge/Spring_Boot-3.3.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 17" />
  <img src="https://img.shields.io/badge/Supabase-3ECF8E?style=for-the-badge&logo=supabase&logoColor=white" alt="Supabase" />
  <img src="https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL" />
  <img src="https://img.shields.io/badge/JavaScript-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black" alt="JavaScript" />
  <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker" />
</div>

<h1 align="center">LeaveFlow</h1>
<p align="center"><strong>A modern, full-stack Employee Leave Management System</strong></p>

<div align="center">
  <a href="https://leaveflow-dev.onrender.com/"><strong>🚀 View Live Demo</strong></a>
</div>
<br>

LeaveFlow is a comprehensive Employee Leave Management System designed to simplify the process of requesting, tracking, and approving employee time-off. It provides separate portals for **Employees** and **Administrators**, enforcing business rules, automated leave balance deduction, and secure role-based access.

---

## 🌟 Key Features

- **🛡️ Secure Authentication**: Powered by Supabase Auth with ES256 asymmetric JWT verification (via JWKS).
- **👥 Role-Based Access Control (RBAC)**: Distinct permissions for `ADMIN` and `EMPLOYEE` roles.
- **📅 Leave Tracking**: Employees can view their leave history, check real-time balances, and apply for leaves.
- **✅ Admin Dashboard**: Admins can approve/reject leaves, manage employee accounts, and adjust balances.
- **⚡ Smart Validations**: Automatic exclusion of weekends, overlap detection, and balance checks.
- **🐳 Dockerized Deployment**: Multi-stage Docker build ready for cloud deployment.

---

## 🛠️ Technology Stack

| Component      | Technology              | Description                                      |
| -------------- | ----------------------- | ------------------------------------------------ |
| **Backend**    | Spring Boot 3.3.4       | Core framework, REST API, Spring Data JPA        |
| **Language**   | Java 17                 | Programming language                             |
| **Frontend**   | HTML5, CSS3, Vanilla JS | Served directly via Spring Boot static resources |
| **Database**   | PostgreSQL              | Relational database hosted on Supabase           |
| **Auth**       | Supabase Auth           | User management & JWT generation (ES256)         |
| **Build Tool** | Maven 3.9.6             | Dependency & build management                    |
| **Hosting**    | Render.com              | Cloud platform via Docker containers             |

---

## 📂 Project Structure

A clean, monolithic structure with the frontend seamlessly embedded within the Spring Boot application.

```
LeaveFlow/
├── pom.xml
├── Dockerfile
├── .env                          # Local credentials (gitignored)
├── .env.example                  # Template for .env
├── src/
│   ├── main/
│   │   ├── java/com/example/leaveflow/
│   │   │   ├── controller/       # REST endpoints (API Layer)
│   │   │   ├── dto/              # Request/Response objects
│   │   │   ├── entity/           # JPA database entities
│   │   │   ├── enums/            # Role, LeaveType, LeaveStatus
│   │   │   ├── exception/        # Global error handling
│   │   │   ├── repository/       # Spring Data JPA repositories
│   │   │   ├── security/         # JWT filter, auth utilities
│   │   │   └── service/          # Core Business logic
│   │   └── resources/
│   │       ├── application.properties
│   │       └── static/           # Frontend (HTML, CSS, JS) - Served at Root (/)
│   └── test/                     # Unit tests (JUnit 5 + Mockito)
├── project_doc.md                # Full technical project documentation
└── viva.md                       # Viva Q&A reference
```

---

## 🚀 Quick Start

### 1. Prerequisites

- **Java 17** Installed
- **Maven 3.9+** Installed
- A configured **Supabase** project (Auth & Database)

### 2. Configure Environment

Clone the repository and set up your local environment variables:

```bash
cp .env.example .env
# Open .env and fill in your Supabase DB credentials and Service Role Key
```

### 3. Run Locally

```bash
mvn clean spring-boot:run
```

Once the application starts, open your browser and navigate to: **[http://localhost:8080](http://localhost:8080)**

---

## 📡 API Reference

### 🧑‍💼 Employees (`/api/employees`)

| Method  | URL                              | Auth Required    | Description                     |
| ------- | -------------------------------- | ---------------- | ------------------------------- |
| `POST`  | `/api/employees`                 | 🛡️ Admin         | Create a new employee           |
| `GET`   | `/api/employees`                 | 🛡️ Admin         | List all employees              |
| `GET`   | `/api/employees/{id}`            | 🛡️ Admin / Self  | Get employee by ID              |
| `GET`   | `/api/employees/code/{code}`     | 🛡️ Admin         | Search by employee code         |
| `GET`   | `/api/employees/email/{email}`   | 👤 Authenticated | Lookup by email (used on login) |
| `PUT`   | `/api/employees/{id}`            | 🛡️ Admin         | Update employee details         |
| `PATCH` | `/api/employees/{id}/deactivate` | 🛡️ Admin         | Soft-delete an employee         |
| `PATCH` | `/api/employees/{id}/reactivate` | 🛡️ Admin         | Reactivate an employee          |
| `PATCH` | `/api/employees/{id}/balance`    | 🛡️ Admin         | Manually edit leave balance     |
| `GET`   | `/api/employees/{id}/leaves`     | 🛡️ Admin / Self  | Get an employee's leave history |

### 🏖️ Leaves (`/api/leaves`)

| Method  | URL                        | Auth Required      | Description                         |
| ------- | -------------------------- | ------------------ | ----------------------------------- |
| `POST`  | `/api/leaves`              | 👤 Employee (Self) | Apply for leave                     |
| `GET`   | `/api/leaves`              | 🛡️ Admin           | List all leave requests             |
| `GET`   | `/api/leaves/{id}`         | 🛡️ Admin           | Get leave request by ID             |
| `PATCH` | `/api/leaves/{id}/approve` | 🛡️ Admin           | Approve leave & deduct balance      |
| `PATCH` | `/api/leaves/{id}/reject`  | 🛡️ Admin           | Reject leave (with required reason) |

---

## 🔐 Authentication & Security

- **Frontend Handling**: Supabase JS SDK manages user login/logout via `signInWithPassword()`.
- **Backend Verification**: A custom `JwtFilter` intercepts every API request. It verifies the ES256 JWT signature locally against Supabase's JWKS public key endpoints.
- **Authorization Enforcement**: `AuthUtils` enforces strict role-based access control (Admin, Employee, or Self-or-Admin) at the controller level.

---

## ⚙️ Environment Variables

The application relies on the following environment variables (loaded via `spring-dotenv`):

| Variable                    | Description                                                                            |
| --------------------------- | -------------------------------------------------------------------------------------- |
| `DATABASE_URL`              | JDBC connection string. **Use Supabase pooler for cloud (with `?prepareThreshold=0`)** |
| `DATABASE_USERNAME`         | DB username (e.g., `postgres.<project-ref>` for connection pooler)                     |
| `DATABASE_PASSWORD`         | DB password                                                                            |
| `SUPABASE_URL`              | Your Supabase project URL (`https://<ref>.supabase.co`)                                |
| `SUPABASE_SERVICE_ROLE_KEY` | Server-side admin key (secret) used to auto-create auth users                          |
| `PORT`                      | Server port (auto-set by Render in production, defaults to 8080)                       |

---

## 🐳 Docker Deployment

The project includes a multi-stage `Dockerfile` optimized for small image sizes and fast builds.

```bash
# Build the Docker image
docker build -t leaveflow .

# Run the container (passing environment variables)
docker run -p 8080:8080 --env-file .env leaveflow
```

---

## ☁️ Deploy to Render

1. Push your code to a GitHub repository.
2. In Render, create a new **Web Service** and connect your repository.
3. Set **Runtime** to `Docker`.
4. Add all necessary environment variables in the Render dashboard.
5. Deploy! (Render will automatically assign the `PORT` variable).

---

## 🧪 Testing

The project is thoroughly tested with 31 comprehensive unit and integration tests covering Services and Controllers.

```bash
mvn test
```

_Test Classes: `EmployeeServiceTest`, `LeaveServiceTest`, `EmployeeControllerTest`._

---

## 📄 License

This project is licensed under the MIT License.
