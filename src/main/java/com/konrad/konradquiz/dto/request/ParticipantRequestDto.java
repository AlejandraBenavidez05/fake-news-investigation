package com.konrad.konradquiz.dto.request;

import com.konrad.konradquiz.entity.Participant.Sex;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ParticipantRequestDto {

    @NotBlank
    @Size(min = 2, max = 100)
    private String alias;

    @NotBlank
    @Email
    private String email;

    @NotNull
    private Sex sex;

    @NotNull
    @Min(5) @Max(120)
    private Integer age;

    @NotBlank
    @Size(min = 2, max = 100)
    private String region;

    @NotNull
    private Boolean consentAcademicPurpose;

    @NotNull
    private Boolean consentParticipationProcess;

    @NotNull
    private Boolean consentDataProcessing;

    @NotNull
    private Boolean consentNoRisk;

    @NotNull
    private Boolean consentNoPayment;

    @NotNull
    private Boolean consentProjectInfo;
}