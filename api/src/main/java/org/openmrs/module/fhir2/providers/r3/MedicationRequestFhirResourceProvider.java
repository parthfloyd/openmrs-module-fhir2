/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.fhir2.providers.r3;

import static lombok.AccessLevel.PACKAGE;
import static lombok.AccessLevel.PROTECTED;

import javax.annotation.Nonnull;

import java.util.HashSet;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Patch;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.PatchTypeEnum;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.hl7.fhir.convertors.factory.VersionConvertorFactory_30_40;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Medication;
import org.hl7.fhir.dstu3.model.MedicationDispense;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.FhirMedicationRequestService;
import org.openmrs.module.fhir2.api.annotations.R3Provider;
import org.openmrs.module.fhir2.api.search.SearchQueryBundleProviderR3Wrapper;
import org.openmrs.module.fhir2.api.search.param.MedicationRequestSearchParams;
import org.openmrs.module.fhir2.providers.util.FhirProviderUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("medicationRequestFhirR3ResourceProvider")
@R3Provider
public class MedicationRequestFhirResourceProvider implements IResourceProvider {
	
	@Getter(PROTECTED)
	@Setter(value = PACKAGE, onMethod_ = @Autowired)
	private FhirMedicationRequestService medicationRequestService;
	
	@Override
	public Class<? extends IBaseResource> getResourceType() {
		return MedicationRequest.class;
	}
	
	@Read
	@SuppressWarnings("unused")
	public MedicationRequest getMedicationRequestById(@IdParam @Nonnull IdType id) {
		org.hl7.fhir.r4.model.MedicationRequest medicationRequest = medicationRequestService.get(id.getIdPart());
		if (medicationRequest == null) {
			throw new ResourceNotFoundException("Could not find medicationRequest with Id " + id.getIdPart());
		}
		
		return (MedicationRequest) VersionConvertorFactory_30_40.convertResource(medicationRequest);
	}
	
	// NOTE: POST/Create not yet supported, see: https://issues.openmrs.org/browse/FM2-568
	// NOTE: PUT/Update not supported, because Drug Orders are immutable, use PATCH
	
	@Patch
	public MethodOutcome patchMedicationRequest(@IdParam @Nonnull IdType id, PatchTypeEnum patchType,
	        @ResourceParam String body, RequestDetails requestDetails) {
		if (id == null || id.getIdPart() == null) {
			throw new InvalidRequestException("id must be specified to update resource");
		}
		
		org.hl7.fhir.r4.model.MedicationRequest medicationRequest = medicationRequestService.patch(id.getIdPart(), patchType,
		    body, requestDetails);
		
		return FhirProviderUtils.buildPatch(medicationRequest);
	}
	
	public OperationOutcome deleteMedicationRequest(@IdParam IdType id) {
		medicationRequestService.delete(id.getIdPart());
		return FhirProviderUtils.buildDeleteR3();
	}
	
	@Search
	@SuppressWarnings("unused")
	public IBundleProvider searchForMedicationRequests(
	        @OptionalParam(name = MedicationRequest.SP_PATIENT, chainWhitelist = { "", Patient.SP_IDENTIFIER,
	                Patient.SP_GIVEN, Patient.SP_FAMILY,
	                Patient.SP_NAME }, targetTypes = Patient.class) ReferenceAndListParam patientReference,
	        @OptionalParam(name = MedicationRequest.SP_SUBJECT, chainWhitelist = { "", Patient.SP_IDENTIFIER,
	                Patient.SP_GIVEN, Patient.SP_FAMILY,
	                Patient.SP_NAME }, targetTypes = Patient.class) ReferenceAndListParam subjectReference,
	        @OptionalParam(name = MedicationRequest.SP_CONTEXT, chainWhitelist = {
	                "" }, targetTypes = Encounter.class) ReferenceAndListParam encounterReference,
	        @OptionalParam(name = MedicationRequest.SP_CODE) TokenAndListParam code,
	        @OptionalParam(name = MedicationRequest.SP_REQUESTER, chainWhitelist = { "", Practitioner.SP_IDENTIFIER,
	                Practitioner.SP_GIVEN, Practitioner.SP_FAMILY,
	                Practitioner.SP_NAME }, targetTypes = Practitioner.class) ReferenceAndListParam participantReference,
	        @OptionalParam(name = MedicationRequest.SP_MEDICATION, chainWhitelist = {
	                "" }, targetTypes = Medication.class) ReferenceAndListParam medicationReference,
	        @OptionalParam(name = MedicationRequest.SP_RES_ID) TokenAndListParam id,
	        @OptionalParam(name = MedicationRequest.SP_STATUS) TokenAndListParam status,
	        @OptionalParam(name = FhirConstants.SP_FULFILLER_STATUS) TokenAndListParam fulfillerStatus,
	        @OptionalParam(name = "_lastUpdated") DateRangeParam lastUpdated,
	        @IncludeParam(allow = { "MedicationRequest:" + MedicationRequest.SP_MEDICATION,
	                "MedicationRequest:" + MedicationRequest.SP_REQUESTER,
	                "MedicationRequest:" + MedicationRequest.SP_PATIENT,
	                "MedicationRequest:" + MedicationRequest.SP_CONTEXT }) HashSet<Include> includes,
	        @IncludeParam(reverse = true, allow = {
	                "MedicationDispense:" + MedicationDispense.SP_PRESCRIPTION }) HashSet<Include> revIncludes) {
		if (patientReference == null) {
			patientReference = subjectReference;
		}
		
		if (CollectionUtils.isEmpty(includes)) {
			includes = null;
		}
		
		if (CollectionUtils.isEmpty(revIncludes)) {
			revIncludes = null;
		}
		
		return new SearchQueryBundleProviderR3Wrapper(medicationRequestService.searchForMedicationRequests(
		    new MedicationRequestSearchParams(patientReference, encounterReference, code, participantReference,
		            medicationReference, id, status, fulfillerStatus, lastUpdated, includes, revIncludes)));
	}
}
