package com.bigproject.modules.employee.service;

import com.bigproject.modules.employee.dto.EmployeeDtos.*;
import com.bigproject.modules.employee.dto.EmployeeProfile;
import com.bigproject.modules.employee.repository.EmployeeRepository;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class EmployeeService {
  private final EmployeeRepository employees;
  private final Validator validator;

  public EmployeeService(EmployeeRepository employees, Validator validator) {
    this.employees = employees;
    this.validator = validator;
  }

  public Page<ListItem> list(
      int page,
      int size,
      String search,
      String status,
      String department,
      String sortBy,
      String direction) {
    EmployeeQuery.page(page, size);
    EmployeeQuery.allowed(
        status, Set.of("", "Active", "Suspended", "Terminated"), "invalid_employee_status");
    EmployeeQuery.allowed(
        sortBy,
        Set.of("employeeCode", "fullName", "department", "position", "status", "createdAtUtc"),
        "invalid_employee_sort");
    EmployeeQuery.allowed(direction, Set.of("asc", "desc"), "invalid_sort_direction");
    if (department.length() > 200) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_department");
    }
    return employees.list(
        page, size, EmployeeQuery.search(search), status, department, sortBy, direction);
  }

  public Detail require(long id, boolean lock) {
    return employees
        .find(id, lock)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "employee_not_found"));
  }

  public void validateTarget(EmployeeProfile target, EmployeeProfile baseline, long subjectId) {
    require(target != null, "employee_target_required");
    require(validator.validate(target).isEmpty(), "invalid_employee_profile");
    require(target.fullName() != null, "employee_full_name_required");
    var employment = target.employment();
    require(employment.hireDate() != null, "employee_hire_date_required");
    require(
        !before(employment.hireDate(), target.personalInformation().dateOfBirth()),
        "hire_date_before_birth_date");
    require(
        !before(
            target.personalInformation().citizenIdIssuedDate(),
            target.personalInformation().dateOfBirth()),
        "citizen_id_date_before_birth_date");
    require(
        !before(employment.probationStartDate(), employment.hireDate())
            && !before(employment.probationEndDate(), employment.probationStartDate())
            && !before(employment.probationEndDate(), employment.hireDate())
            && !before(employment.officialDate(), employment.hireDate())
            && !before(employment.officialDate(), employment.probationEndDate()),
        "invalid_employment_dates");
    var assignment = target.assignment();
    require(
        assignment != null
            && assignment.department() != null
            && assignment.position() != null
            && assignment.workingType() != null
            && assignment.effectiveFrom() != null,
        "employee_assignment_required");
    require(
        !before(assignment.effectiveFrom(), employment.hireDate()), "assignment_before_hire_date");
    if (baseline != null) {
      require(
          Objects.equals(baseline.assignment(), assignment),
          "employee_assignment_is_not_profile_editable");
      require(!baseline.equals(target), "employee_target_has_no_change");
    }
    require(!target.contacts().isEmpty(), "employee_contact_required");
    var emails = new HashSet<String>();
    for (var contact : target.contacts()) {
      require(
          contact.email() != null || contact.phoneNumber() != null,
          "employee_contact_value_required");
      if (contact.email() != null) {
        require(emails.add(contact.email()), "duplicate_employee_contact_email");
        if (employees.emailExists(contact.email(), subjectId)) {
          throw new ResponseStatusException(HttpStatus.CONFLICT, "employee_email_exists");
        }
      }
    }
    for (var type : EmployeeProfile.ContactType.values()) {
      var contacts = target.contacts().stream().filter(contact -> contact.type() == type).toList();
      require(
          contacts.isEmpty()
              || contacts.stream().filter(EmployeeProfile.Contact::isPrimary).count() == 1,
          "employee_contact_primary_required_per_type");
    }
    for (var address : target.addresses()) {
      require(
          hasText(address.fullAddress()) || hasText(address.addressLine1()),
          "employee_address_required");
    }
    for (var type : EmployeeProfile.AddressType.values()) {
      var addresses =
          target.addresses().stream().filter(address -> address.type() == type).toList();
      require(
          addresses.isEmpty()
              || addresses.stream().filter(EmployeeProfile.Address::isPrimary).count() == 1,
          "employee_address_primary_required_per_type");
    }
    for (var contact : target.emergencyContacts()) {
      require(
          contact.fullName() != null
              && contact.relationship() != null
              && contact.phoneNumber() != null,
          "incomplete_emergency_contact");
    }
    require(
        target.emergencyContacts().isEmpty()
            || target.emergencyContacts().stream()
                    .filter(EmployeeProfile.EmergencyContact::isPrimary)
                    .count()
                == 1,
        "employee_emergency_contact_primary_required");
  }

  private static boolean before(LocalDate value, LocalDate minimum) {
    return value != null && minimum != null && value.isBefore(minimum);
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private static void require(boolean valid, String error) {
    if (!valid) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
    }
  }
}
