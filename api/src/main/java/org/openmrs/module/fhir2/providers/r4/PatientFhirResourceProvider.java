/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.fhir2.providers.r4;

import static lombok.AccessLevel.PACKAGE;
import static lombok.AccessLevel.PROTECTED;

import javax.annotation.Nonnull;

import java.util.HashSet;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.valueset.BundleTypeEnum;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Patch;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.PatchTypeEnum;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.HasAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MedicationDispense;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.FhirPatientService;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.openmrs.module.fhir2.api.search.param.OpenmrsPatientSearchParams;
import org.openmrs.module.fhir2.api.search.param.PatientSearchParams;
import org.openmrs.module.fhir2.providers.util.FhirProviderUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("patientFhirR4ResourceProvider")
@R4Provider
public class PatientFhirResourceProvider implements IResourceProvider {
	
	@Getter(PROTECTED)
	@Setter(value = PACKAGE, onMethod_ = @Autowired)
	private FhirPatientService patientService;
	
	@Override
	public Class<? extends IBaseResource> getResourceType() {
		return Patient.class;
	}
	
	@Read
	@SuppressWarnings("unused")
	public Patient getPatientById(@IdParam @Nonnull IdType id) {
		Patient patient = patientService.get(id.getIdPart());
		if (patient == null) {
			throw new ResourceNotFoundException("Could not find patient with Id " + id.getIdPart());
		}
		return patient;
	}
	
	@Create
	public MethodOutcome createPatient(@ResourceParam Patient patient) {
		return FhirProviderUtils.buildCreate(patientService.create(patient));
	}
	
	@Update
	@SuppressWarnings("unused")
	public MethodOutcome updatePatient(@IdParam IdType id, @ResourceParam Patient patient) {
		if (id == null || id.getIdPart() == null) {
			throw new InvalidRequestException("id must be specified to update");
		}
		
		patient.setId(id.getIdPart());
		
		return FhirProviderUtils.buildUpdate(patientService.update(id.getIdPart(), patient));
	}
	
	@Patch
	@SuppressWarnings("unused")
	public MethodOutcome patchPatient(@IdParam IdType id, PatchTypeEnum patchType, @ResourceParam String body,
	        RequestDetails requestDetails) {
		if (id == null || id.getIdPart() == null) {
			throw new InvalidRequestException("id must be specified to update Patient resource");
		}
		
		Patient patient = patientService.patch(id.getIdPart(), patchType, body, requestDetails);
		return FhirProviderUtils.buildPatch(patient);
	}
	
	@Delete
	@SuppressWarnings("unused")
	public OperationOutcome deletePatient(@IdParam @Nonnull IdType id) {
		patientService.delete(id.getIdPart());
		return FhirProviderUtils.buildDeleteR4();
	}
	
	@Search
	@SuppressWarnings("unused")
	public IBundleProvider searchPatients(@OptionalParam(name = Patient.SP_NAME) StringAndListParam name,
	        @OptionalParam(name = Patient.SP_GIVEN) StringAndListParam given,
	        @OptionalParam(name = Patient.SP_FAMILY) StringAndListParam family,
	        @OptionalParam(name = Patient.SP_IDENTIFIER) TokenAndListParam identifier,
	        @OptionalParam(name = Patient.SP_GENDER) TokenAndListParam gender,
	        @OptionalParam(name = Patient.SP_BIRTHDATE) DateRangeParam birthDate,
	        @OptionalParam(name = Patient.SP_DEATH_DATE) DateRangeParam deathDate,
	        @OptionalParam(name = Patient.SP_DECEASED) TokenAndListParam deceased,
	        @OptionalParam(name = Patient.SP_ADDRESS_CITY) StringAndListParam city,
	        @OptionalParam(name = Patient.SP_ADDRESS_STATE) StringAndListParam state,
	        @OptionalParam(name = Patient.SP_ADDRESS_POSTALCODE) StringAndListParam postalCode,
	        @OptionalParam(name = Patient.SP_ADDRESS_COUNTRY) StringAndListParam country,
	        @OptionalParam(name = Patient.SP_RES_ID) TokenAndListParam id,
	        @OptionalParam(name = FhirConstants.HAS_SEARCH_HANDLER) HasAndListParam hasAndListParam,
	        @OptionalParam(name = "_lastUpdated") DateRangeParam lastUpdated, @Sort SortSpec sort,
	        @IncludeParam(reverse = true, allow = { "Observation:" + Observation.SP_PATIENT,
	                "AllergyIntolerance:" + AllergyIntolerance.SP_PATIENT, "DiagnosticReport:" + DiagnosticReport.SP_PATIENT,
	                "Encounter:" + Encounter.SP_PATIENT, "MedicationRequest:" + MedicationRequest.SP_PATIENT,
	                "ServiceRequest:" + ServiceRequest.SP_PATIENT,
	                "MedicationDispense:" + MedicationDispense.SP_PRESCRIPTION }) HashSet<Include> revIncludes) {
		if (CollectionUtils.isEmpty(revIncludes)) {
			revIncludes = null;
		}
		
		return patientService.searchForPatients(new PatientSearchParams(name, given, family, identifier, gender, birthDate,
		        deathDate, deceased, city, state, postalCode, country, id, hasAndListParam, lastUpdated, sort, revIncludes));
	}
	
	@Search(queryName = "openmrsPatients")
	@SuppressWarnings("unused")
	public IBundleProvider searchOpenmrsPatients(@OptionalParam(name = "q") StringAndListParam query,
	        @OptionalParam(name = Patient.SP_GENDER) TokenAndListParam gender,
	        @OptionalParam(name = Patient.SP_BIRTHDATE) DateRangeParam birthDate,
	        @OptionalParam(name = Patient.SP_DEATH_DATE) DateRangeParam deathDate,
	        @OptionalParam(name = Patient.SP_DECEASED) TokenAndListParam deceased,
	        @OptionalParam(name = Patient.SP_ADDRESS_CITY) StringAndListParam city,
	        @OptionalParam(name = Patient.SP_ADDRESS_STATE) StringAndListParam state,
	        @OptionalParam(name = Patient.SP_ADDRESS_POSTALCODE) StringAndListParam postalCode,
	        @OptionalParam(name = Patient.SP_ADDRESS_COUNTRY) StringAndListParam country,
	        @OptionalParam(name = Patient.SP_RES_ID) TokenAndListParam id,
	        @OptionalParam(name = FhirConstants.HAS_SEARCH_HANDLER) HasAndListParam hasAndListParam,
	        @OptionalParam(name = "_lastUpdated") DateRangeParam lastUpdated, @Sort SortSpec sort,
	        @IncludeParam(reverse = true, allow = { "Observation:" + Observation.SP_PATIENT,
	                "AllergyIntolerance:" + AllergyIntolerance.SP_PATIENT, "DiagnosticReport:" + DiagnosticReport.SP_PATIENT,
	                "Encounter:" + Encounter.SP_PATIENT, "MedicationRequest:" + MedicationRequest.SP_PATIENT,
	                "ServiceRequest:" + ServiceRequest.SP_PATIENT,
	                "MedicationDispense:" + MedicationDispense.SP_PRESCRIPTION }) HashSet<Include> revIncludes) {
		if (CollectionUtils.isEmpty(revIncludes)) {
			revIncludes = null;
		}
		
		return patientService.searchForPatients(new OpenmrsPatientSearchParams(query, gender, birthDate, deathDate, deceased,
		        city, state, postalCode, country, id, hasAndListParam, lastUpdated, sort, revIncludes));
	}
	
	/**
	 * The $everything operation fetches all the information related the specified patient
	 * 
	 * @param patientId The id of the patient
	 * @return a bundle of resources which reference to or are referenced from the patient
	 */
	@Operation(name = "everything", idempotent = true, type = Patient.class, bundleType = BundleTypeEnum.SEARCHSET)
	public IBundleProvider getPatientEverything(@IdParam IdType patientId) {
		
		if (patientId == null || patientId.getIdPart() == null || patientId.getIdPart().isEmpty()) {
			return null;
		}
		
		TokenParam patientReference = new TokenParam().setValue(patientId.getIdPart());
		
		return patientService.getPatientEverything(patientReference);
	}
	
	/**
	 * The $everything operation fetches all the information related to all the patients
	 *
	 * @return a bundle of resources which reference to or are referenced from the patients
	 */
	@Operation(name = "everything", idempotent = true, type = Patient.class, bundleType = BundleTypeEnum.SEARCHSET)
	public IBundleProvider getPatientEverything() {
		return patientService.getPatientEverything();
	}
}
