package com.bigproject.modules.employee.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/** Full replacement of the profile managed by a creation/update request. */
public record EmployeeProfile(
    @Size(max = 200) String fullName,
    @Size(max = 1000) String avatar,
    @NotNull @Valid PersonalInformation personalInformation,
    @NotNull @Valid Employment employment,
    @Valid Assignment assignment,
    @NotNull @Size(max = 20) List<@NotNull @Valid Contact> contacts,
    @NotNull @Size(max = 20) List<@NotNull @Valid Address> addresses,
    @NotNull @Size(max = 20) List<@NotNull @Valid EmergencyContact> emergencyContacts,
    @Size(max = 4000) String note) {
  public EmployeeProfile {
    fullName = clean(fullName);
    avatar = clean(avatar);
    note = clean(note);
  }

  public record PersonalInformation(
      @PastOrPresent LocalDate dateOfBirth,
      Gender gender,
      @Size(max = 200) String birthPlace,
      @Size(max = 200) String hometown,
      @Size(max = 100) String nationality,
      @Size(max = 50) String maritalStatus,
      @Size(max = 50) String citizenIdNumber,
      @PastOrPresent LocalDate citizenIdIssuedDate,
      @Size(max = 200) String citizenIdIssuedPlace,
      @Size(max = 50) String taxCode) {
    public PersonalInformation {
      birthPlace = clean(birthPlace);
      hometown = clean(hometown);
      nationality = clean(nationality);
      maritalStatus = clean(maritalStatus);
      citizenIdNumber = clean(citizenIdNumber);
      citizenIdIssuedPlace = clean(citizenIdIssuedPlace);
      taxCode = clean(taxCode);
    }
  }

  public record Employment(
      LocalDate hireDate,
      LocalDate probationStartDate,
      LocalDate probationEndDate,
      LocalDate officialDate) {}

  public record Assignment(
      @Size(max = 200) String department,
      @Size(max = 200) String position,
      WorkingType workingType,
      LocalDate effectiveFrom) {
    public Assignment {
      department = clean(department);
      position = clean(position);
    }
  }

  public record Contact(
      @NotNull ContactType type,
      @Email @Size(max = 254) String email,
      @Size(max = 30) String phoneNumber,
      boolean isPrimary) {
    public Contact {
      email = clean(email);
      if (email != null) {
        email = email.toLowerCase(Locale.ROOT);
      }
      phoneNumber = clean(phoneNumber);
    }
  }

  public record Address(
      @NotNull AddressType type,
      @Size(max = 10) String countryCode,
      @Size(max = 100) String countryName,
      @Size(max = 30) String provinceCode,
      @Size(max = 100) String provinceName,
      @Size(max = 30) String districtCode,
      @Size(max = 100) String districtName,
      @Size(max = 30) String wardCode,
      @Size(max = 100) String wardName,
      @Size(max = 300) String addressLine1,
      @Size(max = 300) String addressLine2,
      @Size(max = 20) String postalCode,
      @Size(max = 1000) String fullAddress,
      boolean isPrimary) {}

  public record EmergencyContact(
      @Size(max = 200) String fullName,
      @Size(max = 100) String relationship,
      @Size(max = 30) String phoneNumber,
      @Size(max = 1000) String address,
      boolean isPrimary) {
    public EmergencyContact {
      fullName = clean(fullName);
      relationship = clean(relationship);
      phoneNumber = clean(phoneNumber);
      address = clean(address);
    }
  }

  public enum Gender {
    Male,
    Female,
    Other
  }

  public enum ContactType {
    Work,
    Personal,
    Other
  }

  public enum AddressType {
    Permanent,
    Temporary,
    Other
  }

  public enum WorkingType {
    FullTime,
    PartTime,
    Contract,
    Intern
  }

  private static String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
