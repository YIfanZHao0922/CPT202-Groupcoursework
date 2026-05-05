[README.md](https://github.com/user-attachments/files/27395311/README.md)
[README.md](https://github.com/user-attachments/files/27356874/README.md)
# CPT202 Project Selection System — Backend (Spring Boot)

A Java 17 + Spring Boot 3.2 backend for the Online Project Selection System (Option C).
Compatible with the existing MySQL schema (`project_selection_system`) provided by the database/frontend team.

---

## 1. Tech Stack

| Layer | Technology |
|---|---|
| Language / Runtime | Java 17 |
| Build | Maven 3.9+ |
| Framework | Spring Boot 3.2.5 (Web, Data JPA, Security, Validation) |
| Auth | JWT (jjwt 0.12.5) + BCrypt |
| Database | MySQL 8.x (production) / H2 (tests) |
| ORM | Hibernate / JPA |
| Testing | JUnit 5, Mockito, Spring Security Test, MockMvc |

---

## 2. Project Layout

```
project-selection-system/
├── pom.xml
├── src/main/java/com/cpt202/pss/
│   ├── ProjectSelectionSystemApplication.java
│   ├── config/        # Security, CORS, DataSeeder
│   ├── controller/    # REST endpoints
│   ├── dto/           # Request/response DTOs
│   ├── entity/        # JPA entities (User, Project, Category, Application)
│   ├── exception/     # BusinessException + global handler
│   ├── repository/    # Spring Data JPA repos
│   ├── security/      # JWT util, filter, UserDetailsService
│   ├── service/       # Business logic
│   └── util/          # SecurityUtils
├── src/main/resources/application.yml
├── src/test/java/...  # unit + integration tests
├── postman/           # API collection for demos
└── docs/              # extra documentation
```

---

## 3. Run on Local IDEA

### 3.1 Prerequisites
- JDK 17 (`java -version`)
- Maven 3.9+ (or use IDEA's bundled Maven)
- MySQL 8.x running on `localhost:3306`
- IntelliJ IDEA 2023.x or newer

### 3.2 Database setup
1. Start MySQL.
2. Create the database (the frontend team's SQL dump already does this — re-run if needed):
   ```sql
   CREATE DATABASE project_selection_system DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
   ```
3. Import the schema dump (`新建 文本文档(1).txt`):
   ```bash
   mysql -u root -p project_selection_system < schema.sql
   ```

### 3.3 Configure DB credentials
Edit `src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/project_selection_system?...
    username: root
    password: YOUR_PASSWORD
```

### 3.4 Open in IDEA
1. **File → Open** → select the `project-selection-system` folder (the one containing `pom.xml`).
2. IDEA detects it as a Maven project and downloads dependencies automatically.
3. Enable annotation processing for Lombok:
   - **Settings → Build, Execution, Deployment → Compiler → Annotation Processors** → tick *Enable annotation processing*.
   - Install the **Lombok** plugin if not already installed.
4. Run `ProjectSelectionSystemApplication.java` (right-click → **Run**).
5. Server starts at `http://localhost:8080`.

### 3.5 First-run demo data
On first start, `DataSeeder` populates the DB with demo users (only when the `users` table is empty):

| Username | Password    | Role    |
|----------|-------------|---------|
| admin    | admin123    | Admin   |
| teacher1 | teacher123  | Teacher |
| teacher2 | teacher123  | Teacher |
| student1 | student123  | Student |
| student2 | student123  | Student |
| student3 | student123  | Student |

Plus 3 categories and 4 sample projects.

> If you do NOT want demo data because the FE team already loaded their own:
> delete `DataSeeder.java`, or skip — it self-disables once any user exists.

### 3.6 Smoke test
```bash
curl http://localhost:8080/api/categories
# → {"code":200,"message":"success","data":[...]}
```

---

## 4. Run Tests

```bash
mvn test
```

Tests use H2 in-memory DB via `application-test.yml` (no MySQL required).

Coverage:
- `JwtUtilTest` — token generation, parsing, tampering detection
- `AuthServiceTest` — registration / role guards / duplicate detection (Mockito)
- `ProjectServiceTest` — ownership rules, role-based permissions (Mockito)
- `ApplicationServiceTest` — apply / withdraw / decide workflow including the *one-active-agreement* rule (Mockito)
- `AuthControllerIntegrationTest` — full HTTP round-trip register → login → JWT → protected endpoint (MockMvc)
- `ProjectSelectionSystemApplicationTests` — context load smoke test

To capture screenshots for the report:
```bash
mvn test -Dsurefire.useFile=false
# Take a screenshot of the green "BUILD SUCCESS" + Tests run summary
```

---

## 5. REST API Reference (summary)

Base URL: `http://localhost:8080`.
All protected endpoints require header: `Authorization: Bearer <token>`.

### 5.1 Auth (public)
| Method | Path | Body |
|---|---|---|
| POST | `/api/auth/register` | `{username, password, email, fullName, role}` (role ∈ Student\|Teacher) |
| POST | `/api/auth/login`    | `{username, password}` → returns JWT |
| POST | `/api/auth/logout`   | — (client should drop token) |

### 5.2 Users
| Method | Path | Role |
|---|---|---|
| GET    | `/api/users/me` | any |
| PUT    | `/api/users/me` | any |
| POST   | `/api/users/me/change-password` | any |
| GET    | `/api/users?keyword=` | Admin |
| PUT    | `/api/users/{id}` | Admin |
| DELETE | `/api/users/{id}` | Admin |

### 5.3 Categories
| Method | Path | Role |
|---|---|---|
| GET    | `/api/categories` | public |
| POST   | `/api/categories` | Admin |
| PUT    | `/api/categories/{id}` | Admin |
| DELETE | `/api/categories/{id}` | Admin |

### 5.4 Projects
| Method | Path | Role |
|---|---|---|
| GET    | `/api/projects?keyword=&status=&teacherId=&categoryId=` | any auth |
| GET    | `/api/projects/{id}` | any auth |
| GET    | `/api/projects/mine` | Teacher / Admin |
| POST   | `/api/projects` | Teacher / Admin |
| PUT    | `/api/projects/{id}` | Teacher (own) / Admin |
| PATCH  | `/api/projects/{id}/status` body `{status}` | Teacher (own) / Admin |
| DELETE | `/api/projects/{id}` | Teacher (own) / Admin |

### 5.5 Applications
| Method | Path | Role |
|---|---|---|
| GET    | `/api/applications/mine` | Student |
| GET    | `/api/applications/project/{projectId}` | Teacher (own) / Admin |
| GET    | `/api/applications/{id}` | applicant / project owner / Admin |
| POST   | `/api/applications` body `{projectId, notes}` | Student |
| POST   | `/api/applications/{id}/withdraw` | Student (own, PENDING only) |
| POST   | `/api/applications/{id}/decision` body `{status: ACCEPTED\|REJECTED, feedback}` | Teacher (own) / Admin |

### Standard response shape
```json
{ "code": 200, "message": "...", "data": { /* ... */ } }
```

---

## 6. Frontend Integration

The frontend (jQuery) should:

1. **Login flow**:
   ```javascript
   $.ajax({
     url: 'http://localhost:8080/api/auth/login',
     type: 'POST',
     contentType: 'application/json',
     data: JSON.stringify({username, password}),
     success: r => localStorage.setItem('jwt', r.data.token)
   });
   ```

2. **Add token to every subsequent request**:
   ```javascript
   $.ajaxSetup({
     beforeSend: function(xhr) {
       const t = localStorage.getItem('jwt');
       if (t) xhr.setRequestHeader('Authorization', 'Bearer ' + t);
     }
   });
   ```

3. **Allowed origins** are configured in `application.yml > app.cors.allowed-origins`.
   Add the frontend's URL there (e.g. `http://localhost:5500` for Live Server).

---

## 7. Deploy to Aliyun ECS (assignment hosting)

Per the AliyunServerUserGuide, allowed ports include **8080, 8090, 9001, 9002, 80, 443**.
We use **8080** (default `server.port`).

### 7.1 Build
```bash
mvn clean package -DskipTests
# Produces target/project-selection-system.jar
```

### 7.2 Upload
```bash
scp target/project-selection-system.jar root@<your-ecs-ip>:/opt/pss/
```

### 7.3 First-time server setup (Ubuntu 22.04)
```bash
ssh root@<your-ecs-ip>

# Install JDK 17
apt update && apt install -y openjdk-17-jdk

# Install MySQL 8
apt install -y mysql-server
systemctl enable --now mysql

# Create DB
mysql -e "CREATE DATABASE project_selection_system DEFAULT CHARSET utf8mb4;"
mysql -e "CREATE USER 'pss'@'localhost' IDENTIFIED BY 'StrongPassword!';"
mysql -e "GRANT ALL ON project_selection_system.* TO 'pss'@'localhost'; FLUSH PRIVILEGES;"

# Import schema
mysql project_selection_system < schema.sql
```

### 7.4 Run
```bash
cd /opt/pss
nohup java -jar project-selection-system.jar \
  --spring.datasource.username=pss \
  --spring.datasource.password='StrongPassword!' \
  --APP_JWT_SECRET='generate-a-long-random-secret-here' \
  > app.log 2>&1 &
```

Verify:
```bash
curl http://localhost:8080/api/categories
```

### 7.5 Make accessible to the internet
- The Aliyun guide already opens 8080 — confirm via the ECS console's *Security Group*.
- Access from outside: `http://<ecs-public-ip>:8080`.
- Update `app.cors.allowed-origins` to include your frontend's hosted URL.

### 7.6 (Optional) systemd service
```ini
# /etc/systemd/system/pss.service
[Unit]
Description=CPT202 PSS
After=network.target mysql.service

[Service]
Type=simple
WorkingDirectory=/opt/pss
ExecStart=/usr/bin/java -jar /opt/pss/project-selection-system.jar
Restart=on-failure
Environment=APP_JWT_SECRET=YOUR_LONG_RANDOM_SECRET

[Install]
WantedBy=multi-user.target
```
```bash
systemctl daemon-reload
systemctl enable --now pss
systemctl status pss
```

---

## 8. Mapping to Assignment 2 Requirements

| Requirement | Where it lives |
|---|---|
| Authentication (register/login/logout) | `AuthController`, JWT in `security/` |
| Role-based access control | `@PreAuthorize` on controllers + `SecurityConfig` |
| Database-backed storage | MySQL via JPA (`entity/`, `repository/`) |
| CRUD for core entities | Project, Category, Application, User services |
| Workflow / status changes | Project status (AVAILABLE→REQUESTED→AGREED→CLOSED) and Application status (PENDING→ACCEPTED/REJECTED/WITHDRAWN) — `ApplicationService.decide` |
| Search / filter | `ProjectRepository.search` + `ProjectController` query params |
| Validation | `@Valid` + Bean Validation annotations on DTOs; `GlobalExceptionHandler` returns 400 with field errors |
| Testing evidence | JUnit 5 + Mockito unit tests, MockMvc integration tests, Postman collection |
| Reports/usable interface | Frontend (jQuery, separate repo) consumes these REST endpoints |

---

## 9. Suggested Screenshots for the Report

To satisfy the Software Testing section (20%) and Software Demonstration:

1. **`mvn test` output** — terminal screenshot with all tests passing.
2. **Postman**:
   - login → 200 with JWT
   - apply for project → 200
   - duplicate apply → 400 with `"You already have a pending request"`
   - student tries to create project → 403
3. **MySQL Workbench**: rows showing the demo data, with one project status = `AGREED`.
4. **IDEA console**: Spring Boot startup banner + `Tomcat started on port 8080`.
5. **Frontend integration**: browser DevTools Network tab showing `Authorization: Bearer ...` header.

For deployment evidence:
6. **Aliyun ECS console**: instance running, security group with port 8080.
7. **`systemctl status pss`** showing the service active.
8. **Public URL** from any browser hitting `/api/categories`.

---

## 10. Troubleshooting

| Symptom | Fix |
|---|---|
| `Unable to obtain connection from database` | Verify MySQL running; check `username/password` in `application.yml` |
| `403 Forbidden` on otherwise valid endpoint | Token missing/expired/wrong role — re-login |
| `CORS error` in browser | Add the FE origin to `app.cors.allowed-origins` |
| `Lombok-related "cannot find symbol"` errors in IDEA | Install Lombok plugin + enable annotation processing |
| Demo data not appearing | Either the `users` table is non-empty (seeder skips) or the `test` profile is active |
