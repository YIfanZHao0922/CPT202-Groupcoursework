# Test Plan & Evidence — Project Selection System

This document supports Section 5 (Software Testing) of the Group Report.
It maps each business requirement to the tests that exercise it.

---

## 1. Test Strategy

| Layer | Tool | Purpose |
|---|---|---|
| Unit | JUnit 5 + Mockito | Validate business rules in isolation (no DB / network) |
| Integration | Spring Boot Test + MockMvc + H2 | Exercise the full HTTP → controller → service → repository → DB stack |
| Manual / Acceptance | Postman | Demo the realistic workflow against the running server |
| Frontend | Browser DevTools | Verify CORS, JWT, and end-to-end flows |

Tests run automatically on `mvn test` against the `test` Spring profile (H2 in-memory DB).

## 2. When Testing Was Performed

- **Before integration of each feature**: developers ran the relevant unit tests on their branch.
- **After feature completion**: integration tests on the `develop` branch via `mvn test`.
- **Before each release/demo**: full Postman regression run against the staged Aliyun deployment.
- **On bug discovery**: a failing unit test was added before the fix (regression test).

## 3. Defect Tracking & Resolution

Defects discovered during testing were logged as GitHub Issues with the labels `bug`, `priority/high|medium|low`, and the increment number.
A typical example from sprint 2:

> **Issue #14 — Student can apply twice to the same project**.
> Found by `ApplicationServiceTest.apply_duplicateRejected` (added once we noticed the gap).
> Fix: introduced `findFirstByProjectIdAndStudentIdAndStatus(...PENDING)` check in `ApplicationService.apply`.
> Outcome: test became green; design improved by also auto-rejecting losers when a project reaches capacity (one decision triggers cascading rejections in `decide`).

## 4. Requirements ↔ Tests

| ID | Requirement | Test |
|----|---|---|
| FR-1 | User register with role | `AuthServiceTest.register_succeeds_forNewStudent` |
| FR-2 | Cannot self-register as Admin | `AuthServiceTest.register_throws_whenSelfRegisteringAsAdmin` |
| FR-3 | Duplicate username rejected | `AuthServiceTest.register_throws_whenUsernameTaken` |
| FR-4 | JWT issued on login | `AuthControllerIntegrationTest.register_then_login_returnsJwt` |
| FR-5 | Bad creds rejected | `AuthControllerIntegrationTest.login_failsWithBadCredentials` |
| FR-6 | Protected endpoints reject anonymous | `AuthControllerIntegrationTest.protectedEndpoint_returns401_withoutToken` |
| FR-7 | Only teachers can create projects | `ProjectServiceTest.create_throws_forStudent` |
| FR-8 | Teacher cannot edit another teacher's project | `ProjectServiceTest.update_throws_whenTeacherTouchesAnotherTeachersProject` |
| FR-9 | Admin can edit any project | `ProjectServiceTest.update_succeeds_forAdminOnAnyProject` |
| BR-1 | Only students apply | `ApplicationServiceTest.apply_throws_whenTeacherTriesToApply` |
| BR-2 | One active agreed project per student | `ApplicationServiceTest.apply_throws_whenStudentAlreadyHasAcceptedProject` |
| BR-3 | Closed projects refuse new requests | `ApplicationServiceTest.apply_throws_whenProjectClosed` |
| BR-4 | Apply succeeds and promotes project to REQUESTED | `ApplicationServiceTest.apply_succeeds_forStudentOnAvailableProject` |
| BR-5 | ACCEPT at capacity → AGREED + cascade reject | `ApplicationServiceTest.decide_accept_marksProjectAgreedWhenCapacityReached` |
| BR-6 | REJECT does not increment students | `ApplicationServiceTest.decide_reject_doesNotIncrementStudents` |
| BR-7 | Only project owner can decide | `ApplicationServiceTest.decide_throws_whenNotProjectOwner` |
| BR-8 | Student withdraws own pending app | `ApplicationServiceTest.withdraw_succeeds_forOwnPendingApp` |
| BR-9 | Cannot withdraw someone else's app | `ApplicationServiceTest.withdraw_throws_whenNotOwner` |
| Sec-1 | JWT signature validation | `JwtUtilTest.validate_returnsFalse_forTamperedToken` |
| Sec-2 | Token round-trip integrity | `JwtUtilTest.generateAndParseToken_returnsOriginalClaims` |

## 5. Acceptance Test Scenarios (Postman)

These are the manual demo flows exercised during the recorded presentation.

### Scenario A — Happy path (student gets allocated)
1. Register or login as `teacher1`. Create project "AI for Healthcare", maxStudents=1.
2. Login as `student1`. Browse projects, filter by category=Machine Learning, see the new project.
3. Apply with notes.
4. Login as `teacher1`. View applications for the project.
5. Accept student1.
6. Verify: project status = `AGREED`, currentStudents = 1.
7. Login as `student1` → my applications shows ACCEPTED with feedback.

### Scenario B — Capacity full (cascade rejection)
1. Teacher creates project, max=1.
2. student1, student2 both apply.
3. Teacher accepts student1.
4. Verify: student2's pending app auto-rejected with feedback "Auto-rejected: project capacity reached".

### Scenario C — Business rule violations
1. student1 (already AGREED on project A) tries to apply to project B → 400.
2. student2 applies twice to the same project → 400.
3. student1 tries to create a project → 403.
4. teacher2 tries to edit teacher1's project → 403.
5. student1 tries to call `/api/applications/project/1` → 403.

### Scenario D — Withdraw
1. student2 applies to a project.
2. student2 withdraws BEFORE teacher decides → 200, status WITHDRAWN.
3. student2 tries to withdraw again → 400.
4. student2 tries to withdraw an ACCEPTED application → 400.

### Scenario E — Search & Filter
1. `GET /api/projects?keyword=ML` → returns ML-related projects only.
2. `GET /api/projects?status=AVAILABLE&categoryId=1` → returns available ML projects.
3. `GET /api/projects?teacherId=2` → returns only teacher2's projects.

## 6. Screenshots Checklist for the Report

- [ ] `mvn test` console output: green BUILD SUCCESS + tests run summary
- [ ] IDEA: `Tests passed: N` in the test runner panel
- [ ] Postman: Login response with token
- [ ] Postman: Successful application creation (200)
- [ ] Postman: Duplicate application (400 with rule message)
- [ ] Postman: Student → POST /api/projects (403)
- [ ] MySQL Workbench: Applications table rows showing status mix (PENDING, ACCEPTED, REJECTED, WITHDRAWN)
- [ ] MySQL Workbench: Projects table with one row in AGREED state
- [ ] Browser DevTools Network tab: Authorization header attached
- [ ] Aliyun ECS console: instance state RUNNING, port 8080 in security group
- [ ] systemd: `systemctl status pss` showing active
