package com.bigproject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EmployeeIntegrationTests {
  private static final String CREATE = "/api/employee-creation-requests";
  private static final String UPDATE = "/api/employee-update-requests";
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;
  private Actor maker;
  private Actor other;
  private Actor reviewer;
  private Actor admin;
  private Actor student;

  private record Actor(long id, MockHttpSession session) {}

  @BeforeAll
  void accounts() throws Exception {
    maker = account("HR");
    other = account("HR");
    reviewer = account("HR_APPROVER");
    admin = account("ADMIN");
    student = account(null);
  }

  @Test
  void creationIsPrivateUntilSubmittedAndTakesEffectOnlyOnApproval() throws Exception {
    String name = "New employee " + UUID.randomUUID();
    ObjectNode target = profile(name);
    JsonNode draft =
        response(postJson(CREATE, maker, Map.of("target", target)).andExpect(status().isCreated()));
    long id = draft.get("id").asLong();
    mvc.perform(get(CREATE + "/" + id).session(other.session())).andExpect(status().isNotFound());
    mvc.perform(get(CREATE + "/" + id + "/revisions").session(reviewer.session()))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/employee-requests").session(other.session()).param("search", name))
        .andExpect(jsonPath("$.totalCount").value(0));
    assertEmployees(name, 0);
    JsonNode pending =
        response(
            postJson(
                    CREATE + "/" + id + "/submit",
                    maker,
                    Map.of("version", 1, "reason", "Tuyển mới"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("PendingApproval")));
    assertEmployees(name, 0);
    mvc.perform(get(CREATE + "/" + id).session(reviewer.session())).andExpect(status().isOk());
    JsonNode approved = approve(CREATE, pending);
    long employeeId = approved.get("subjectId").asLong();
    mvc.perform(get("/api/employees/" + employeeId).session(maker.session()))
        .andExpect(jsonPath("$.profile.fullName").value(name))
        .andExpect(jsonPath("$.profile.contacts[0].isPrimary").value(true))
        .andExpect(jsonPath("$.employeeCode").isNotEmpty())
        .andExpect(jsonPath("$.status").value("Active"))
        .andExpect(jsonPath("$.version").value(1));
    assertEmployees(name, 1);
    mvc.perform(get("/api/employee-requests").session(maker.session()).param("search", name))
        .andExpect(jsonPath("$.totalCount").value(0));
    mvc.perform(
            get("/api/employee-requests")
                .session(maker.session())
                .param("search", name)
                .param("includeCompleted", "true")
                .param("type", "Creation")
                .param("state", "Approved"))
        .andExpect(jsonPath("$.totalCount").value(1));
    postJson(CREATE + "/" + id + "/approve", reviewer, decision(approved))
        .andExpect(status().isConflict());
  }

  @Test
  void updateCopiesBaselineAndAppliesFullReplacementAfterApproval() throws Exception {
    JsonNode created =
        approve(CREATE, submission(CREATE, profile("Original " + UUID.randomUUID())));
    long employeeId = created.get("subjectId").asLong();
    JsonNode draft =
        response(
            postJson(UPDATE, maker, Map.of("subjectId", employeeId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.baselineVersion").value(1)));
    ObjectNode target = (ObjectNode) draft.get("target").deepCopy();
    String name = "Updated " + UUID.randomUUID();
    target.put("fullName", name);
    target.putNull("avatar");
    JsonNode saved =
        response(
            mvc.perform(
                    put(UPDATE + "/" + draft.get("id").asLong() + "/draft")
                        .session(maker.session())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("version", 1, "target", target))))
                .andExpect(status().isOk()));
    assertEmployees(name, 0);
    JsonNode submitted =
        response(
            postJson(
                    UPDATE + "/" + draft.get("id").asLong() + "/submit",
                    maker,
                    Map.of("version", saved.get("version").asLong(), "reason", "Sửa hồ sơ"))
                .andExpect(status().isOk()));
    assertEmployees(name, 0);
    approve(UPDATE, submitted);
    mvc.perform(get("/api/employees/" + employeeId).session(maker.session()))
        .andExpect(jsonPath("$.profile.fullName").value(name))
        .andExpect(jsonPath("$.profile.avatar").isEmpty())
        .andExpect(jsonPath("$.version").value(2));
    mvc.perform(
            get(UPDATE
                    + "/"
                    + draft.get("id").asLong()
                    + "/revisions/"
                    + submitted.get("latestRevisionId").asLong())
                .session(reviewer.session()))
        .andExpect(
            jsonPath("$.baseline.fullName").value(draft.get("baseline").get("fullName").asText()))
        .andExpect(jsonPath("$.target.fullName").value(name));
  }

  @Test
  void rejectionCorrectionAndResubmissionPreserveExactRevisions() throws Exception {
    JsonNode pending = submission(CREATE, profile("Before correction " + UUID.randomUUID()));
    long id = pending.get("id").asLong();
    postJson(CREATE + "/" + id + "/reject", reviewer, decision(pending))
        .andExpect(status().isBadRequest());
    var reject =
        Map.of(
            "version",
            pending.get("version").asLong(),
            "revisionId",
            pending.get("latestRevisionId").asLong(),
            "reason",
            "Cần sửa tên");
    JsonNode rejected =
        response(
            postJson(CREATE + "/" + id + "/reject", reviewer, reject)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("Rejected")));
    ObjectNode corrected = (ObjectNode) pending.get("target").deepCopy();
    corrected.put("fullName", "After correction " + UUID.randomUUID());
    JsonNode saved =
        response(
            mvc.perform(
                    put(CREATE + "/" + id + "/draft")
                        .session(maker.session())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            json.writeValueAsString(
                                Map.of(
                                    "version",
                                    rejected.get("version").asLong(),
                                    "target",
                                    corrected))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("Draft")));
    mvc.perform(get(CREATE + "/" + id).session(reviewer.session()))
        .andExpect(status().isNotFound());
    JsonNode resubmitted =
        response(
            postJson(
                    CREATE + "/" + id + "/submit",
                    maker,
                    Map.of("version", saved.get("version").asLong(), "reason", "Đã sửa tên"))
                .andExpect(status().isOk()));
    postJson(
            CREATE + "/" + id + "/approve",
            reviewer,
            Map.of(
                "version",
                resubmitted.get("version").asLong(),
                "revisionId",
                pending.get("latestRevisionId").asLong()))
        .andExpect(status().isConflict());
    mvc.perform(get(CREATE + "/" + id + "/revisions").session(reviewer.session()))
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(
            jsonPath("$[0].target.fullName").value(pending.get("target").get("fullName").asText()))
        .andExpect(jsonPath("$[1].reason").value("Đã sửa tên"));
    approve(CREATE, resubmitted);
    mvc.perform(
            get(CREATE + "/" + id + "/activity-history")
                .session(maker.session())
                .param("pageSize", "2"))
        .andExpect(jsonPath("$.totalCount").value(6))
        .andExpect(jsonPath("$.items", hasSize(2)))
        .andExpect(jsonPath("$.items[0].action").value("Approved"))
        .andExpect(jsonPath("$.items[0].actorId").value(reviewer.id()));
  }

  @Test
  void revokeCancelAndSoftDeleteRetainAuditAndNeverCreateAnEmployee() throws Exception {
    JsonNode pending = submission(CREATE, profile("Revoked " + UUID.randomUUID()));
    long id = pending.get("id").asLong();
    mvc.perform(
            delete(CREATE + "/" + id).session(maker.session()).with(csrf()).param("version", "2"))
        .andExpect(status().isConflict());
    postJson(CREATE + "/" + id + "/revoke", other, Map.of("version", 2))
        .andExpect(status().isForbidden());
    JsonNode revoked =
        response(
            postJson(CREATE + "/" + id + "/revoke", maker, Map.of("version", 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("Revoked")));
    postJson(CREATE + "/" + id + "/approve", reviewer, decision(revoked))
        .andExpect(status().isConflict());
    postJson(CREATE + "/" + id + "/cancel", maker, Map.of("version", 3))
        .andExpect(status().isBadRequest());
    postJson(
            CREATE + "/" + id + "/cancel", maker, Map.of("version", 3, "reason", "Không tuyển nữa"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("Cancelled"));
    mvc.perform(
            delete(CREATE + "/" + id).session(maker.session()).with(csrf()).param("version", "4"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id));
    mvc.perform(get(CREATE + "/" + id).session(maker.session())).andExpect(status().isNotFound());
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM employee_request_revision WHERE request_id = ?",
                Long.class,
                id))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM employee_request_activity WHERE request_id = ?",
                Long.class,
                id))
        .isEqualTo(5);
    assertEmployees(pending.get("target").get("fullName").asText(), 0);
  }

  @Test
  void rolesUseRealLoginSessionsAndRequireCsrfAndAnIndependentApprover() throws Exception {
    mvc.perform(get("/api/employees")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/employees").session(student.session())).andExpect(status().isForbidden());
    postJson(CREATE, student, Map.of()).andExpect(status().isForbidden());
    postJson(CREATE, reviewer, Map.of()).andExpect(status().isForbidden());
    mvc.perform(
            post(CREATE)
                .session(maker.session())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isForbidden());
    JsonNode pending = submission(CREATE, profile("Permission " + UUID.randomUUID()));
    postJson(CREATE + "/" + pending.get("id").asLong() + "/approve", maker, decision(pending))
        .andExpect(status().isForbidden());
    JsonNode own =
        response(
            postJson(
                    CREATE + "/submissions",
                    admin,
                    Map.of(
                        "target",
                        profile("Admin requester " + UUID.randomUUID()),
                        "reason",
                        "Hire"))
                .andExpect(status().isCreated()));
    postJson(CREATE + "/" + own.get("id").asLong() + "/approve", admin, decision(own))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("employee_request_self_approval_forbidden"));
    postJson("/api/employees", admin, Map.of()).andExpect(status().isMethodNotAllowed());
  }

  @Test
  void optimisticVersionsAndPendingStateProtectTheSubmittedTarget() throws Exception {
    JsonNode pending = submission(CREATE, profile("Frozen " + UUID.randomUUID()));
    String path = CREATE + "/" + pending.get("id").asLong();
    postJson(path + "/revoke", maker, Map.of("version", 1)).andExpect(status().isConflict());
    mvc.perform(
            put(path + "/draft")
                .session(maker.session())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(Map.of("version", 2, "target", profile("Changed")))))
        .andExpect(status().isConflict());
    postJson(path + "/submit", maker, Map.of("version", 2, "reason", "Duplicate submission"))
        .andExpect(status().isConflict());
    mvc.perform(get(path.replace(CREATE, UPDATE)).session(maker.session()))
        .andExpect(status().isNotFound());
    mvc.perform(get(path + "/revisions/9223372036854775807").session(maker.session()))
        .andExpect(status().isNotFound());
  }

  @Test
  void invalidCreateAndSubmitRollsBackTheDraftAndAuditTogether() throws Exception {
    long requestsBefore = count("employee_request");
    long activitiesBefore = count("employee_request_activity");
    ObjectNode invalid = profile("Invalid date " + UUID.randomUUID());
    ((ObjectNode) invalid.get("employment")).put("probationEndDate", "2020-01-01");
    postJson(CREATE + "/submissions", maker, Map.of("target", invalid, "reason", "Hire"))
        .andExpect(status().isBadRequest());
    assertThat(count("employee_request")).isEqualTo(requestsBefore);
    assertThat(count("employee_request_activity")).isEqualTo(activitiesBefore);
    ObjectNode shape = profile("Invalid shape");
    shape.remove("contacts");
    postJson(CREATE, maker, Map.of("target", shape)).andExpect(status().isBadRequest());
    postJson(CREATE, maker, Map.of("subjectId", 1)).andExpect(status().isBadRequest());
    postJson(UPDATE, maker, Map.of()).andExpect(status().isBadRequest());
    postJson(UPDATE, maker, Map.of("subjectId", Long.MAX_VALUE)).andExpect(status().isNotFound());
  }

  @Test
  void contactAndDateRulesAreCheckedBeforeSubmission() throws Exception {
    ObjectNode invalid = profile("Missing primary");
    ((ObjectNode) invalid.get("contacts").get(0)).put("isPrimary", false);
    postJson(CREATE + "/submissions", maker, Map.of("target", invalid, "reason", "Hire"))
        .andExpect(status().isBadRequest());
    ObjectNode duplicate = profile("Duplicate email");
    ((tools.jackson.databind.node.ArrayNode) duplicate.get("contacts"))
        .add(duplicate.get("contacts").get(0).deepCopy());
    postJson(CREATE + "/submissions", maker, Map.of("target", duplicate, "reason", "Hire"))
        .andExpect(status().isBadRequest());
    ObjectNode futureBirth = profile("Future birthday");
    ((ObjectNode) futureBirth.get("personalInformation")).put("dateOfBirth", "2099-01-01");
    postJson(CREATE, maker, Map.of("target", futureBirth)).andExpect(status().isBadRequest());
    ObjectNode blank = profile("   ");
    postJson(CREATE + "/submissions", maker, Map.of("target", blank, "reason", "Hire"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void emailCollisionAtApprovalLeavesRequestPendingAndEmployeeStateUntouched() throws Exception {
    String email = UUID.randomUUID() + "@employee.test";
    JsonNode first =
        submission(CREATE, profile("First collision", email.toUpperCase(java.util.Locale.ROOT)));
    JsonNode second = submission(CREATE, profile("Second collision", email));
    approve(CREATE, first);
    long employeesBefore = count("employee");
    postJson(CREATE + "/" + second.get("id").asLong() + "/approve", reviewer, decision(second))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("employee_email_exists"));
    assertThat(count("employee")).isEqualTo(employeesBefore);
    mvc.perform(get(CREATE + "/" + second.get("id").asLong()).session(maker.session()))
        .andExpect(jsonPath("$.state").value("PendingApproval"))
        .andExpect(jsonPath("$.version").value(2));
    postJson(
            CREATE + "/submissions",
            maker,
            Map.of("target", profile("Third collision", email), "reason", "Hire"))
        .andExpect(status().isConflict());
  }

  @Test
  void competingUpdatesCannotOverwriteANewerApprovedProfile() throws Exception {
    JsonNode creation =
        approve(CREATE, submission(CREATE, profile("Baseline " + UUID.randomUUID())));
    long employeeId = creation.get("subjectId").asLong();
    ObjectNode target = (ObjectNode) creation.get("target").deepCopy();
    target.put("fullName", "First approved " + UUID.randomUUID());
    JsonNode first = updateSubmission(employeeId, target);
    target.put("fullName", "Stale overwrite " + UUID.randomUUID());
    JsonNode second = updateSubmission(employeeId, target);
    approve(UPDATE, first);
    postJson(UPDATE + "/" + second.get("id").asLong() + "/approve", reviewer, decision(second))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("employee_profile_changed_create_new_request"));
    mvc.perform(get("/api/employees/" + employeeId).session(maker.session()))
        .andExpect(
            jsonPath("$.profile.fullName").value(first.get("target").get("fullName").asText()))
        .andExpect(jsonPath("$.version").value(2));
    ObjectNode current = (ObjectNode) first.get("target").deepCopy();
    postJson(
            UPDATE + "/submissions",
            maker,
            Map.of("subjectId", employeeId, "target", current, "reason", "No change"))
        .andExpect(status().isBadRequest());
    ((ObjectNode) current.get("assignment")).put("department", "Different department");
    postJson(
            UPDATE + "/submissions",
            maker,
            Map.of("subjectId", employeeId, "target", current, "reason", "Transfer"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void concurrentApprovalsApplyTheEmployeeExactlyOnce() throws Exception {
    JsonNode pending = submission(CREATE, profile("Concurrent " + UUID.randomUUID()));
    long before = count("employee");
    var barrier = new CyclicBarrier(2);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      java.util.concurrent.Callable<Integer> approve =
          () -> {
            barrier.await(10, TimeUnit.SECONDS);
            return postJson(
                    CREATE + "/" + pending.get("id").asLong() + "/approve",
                    reviewer,
                    decision(pending))
                .andReturn()
                .getResponse()
                .getStatus();
          };
      var first = executor.submit(approve);
      var second = executor.submit(approve);
      assertThat(
              java.util.List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(200, 409);
    }
    assertThat(count("employee")).isEqualTo(before + 1);
  }

  @Test
  void listValidatesQueriesPagesInDatabaseAndEscapesWildcards() throws Exception {
    String name = "Literal%%_ " + UUID.randomUUID();
    JsonNode created = approve(CREATE, submission(CREATE, profile(name)));
    String email = created.get("target").get("contacts").get(0).get("email").asText();
    mvc.perform(
            get("/api/employees")
                .session(maker.session())
                .param("search", name)
                .param("pageSize", "1")
                .param("status", "Active")
                .param("department", "Operations")
                .param("sortBy", "fullName")
                .param("sortDirection", "desc"))
        .andExpect(jsonPath("$.totalCount").value(1))
        .andExpect(jsonPath("$.totalPages").value(1))
        .andExpect(jsonPath("$.items[0].fullName").value(name));
    assertEmployees(email, 1);
    assertEmployees("!!nonexistent%%", 0);
    for (var param :
        Map.of(
                "page",
                "0",
                "pageSize",
                "101",
                "search",
                "a",
                "sortBy",
                "id;DROP TABLE employee",
                "status",
                "Unknown")
            .entrySet()) {
      mvc.perform(
              get("/api/employees")
                  .session(maker.session())
                  .param(param.getKey(), param.getValue()))
          .andExpect(status().isBadRequest());
    }
    mvc.perform(get("/api/employee-requests").session(maker.session()).param("pageSize", "0"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/employees/9223372036854775807").session(maker.session()))
        .andExpect(status().isNotFound());
  }

  @Test
  void openApiExposesBothConcreteRequestFamilies() throws Exception {
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/employees'].get").exists())
        .andExpect(jsonPath("$.paths['/api/employee-creation-requests'].post").exists())
        .andExpect(jsonPath("$.paths['/api/employee-update-requests/{id}/approve'].post").exists());
  }

  private Actor account(String role) throws Exception {
    String email = UUID.randomUUID() + "@hr.test";
    JsonNode registered =
        response(
            mvc.perform(
                    post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            json.writeValueAsString(
                                Map.of(
                                    "name",
                                    "HR Test",
                                    "email",
                                    email,
                                    "password",
                                    "TestPass123!"))))
                .andExpect(status().isCreated()));
    long id = registered.get("id").asLong();
    if (role != null) {
      jdbc.update("INSERT INTO account_role(user_id, role) VALUES (?, ?)", id, role);
    }
    var result =
        mvc.perform(
                post("/api/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.writeValueAsString(
                            Map.of("email", email, "password", "TestPass123!"))))
            .andExpect(status().isOk())
            .andReturn();
    return new Actor(id, (MockHttpSession) result.getRequest().getSession(false));
  }

  private ObjectNode profile(String name) {
    return profile(name, UUID.randomUUID() + "@employee.test");
  }

  private ObjectNode profile(String name, String email) {
    ObjectNode profile =
        (ObjectNode)
            json.readTree(
                """
        {"avatar":"/avatars/demo.png","personalInformation":{"dateOfBirth":"1990-01-01","gender":"Male"},
         "employment":{"hireDate":"2026-01-01"},
         "assignment":{"department":"Operations","position":"Driver","workingType":"FullTime","effectiveFrom":"2026-01-01"},
         "contacts":[{"type":"Work","phoneNumber":"0901234567","isPrimary":true}],
         "addresses":[],"emergencyContacts":[],"note":"Test employee"}
        """);
    profile.put("fullName", name);
    ((ObjectNode) profile.get("contacts").get(0)).put("email", email);
    return profile;
  }

  private ResultActions postJson(String path, Actor actor, Object body) throws Exception {
    return mvc.perform(
        post(path)
            .session(actor.session())
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(body)));
  }

  private JsonNode response(ResultActions actions) throws Exception {
    return json.readTree(
        actions
            .andReturn()
            .getResponse()
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
  }

  private JsonNode submission(String family, ObjectNode profile) throws Exception {
    return response(
        postJson(family + "/submissions", maker, Map.of("target", profile, "reason", "Tuyển mới"))
            .andExpect(status().isCreated()));
  }

  private JsonNode updateSubmission(long employeeId, ObjectNode target) throws Exception {
    return response(
        postJson(
                UPDATE + "/submissions",
                maker,
                Map.of("subjectId", employeeId, "target", target, "reason", "Sửa hồ sơ"))
            .andExpect(status().isCreated()));
  }

  private Map<String, Long> decision(JsonNode request) {
    return Map.of(
        "version",
        request.get("version").asLong(),
        "revisionId",
        request.get("latestRevisionId").asLong());
  }

  private JsonNode approve(String family, JsonNode request) throws Exception {
    return response(
        postJson(
                family + "/" + request.get("id").asLong() + "/approve", reviewer, decision(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.state").value("Approved")));
  }

  private void assertEmployees(String search, int count) throws Exception {
    mvc.perform(get("/api/employees").session(maker.session()).param("search", search))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalCount").value(count));
  }

  private long count(String table) {
    return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
  }
}
