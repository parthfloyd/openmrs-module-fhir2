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
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
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
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.openmrs.module.fhir2.api.FhirDiagnosticReportService;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.openmrs.module.fhir2.api.search.param.DiagnosticReportSearchParams;
import org.openmrs.module.fhir2.providers.util.FhirProviderUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("diagnosticReportFhirR4ResourceProvider")
@R4Provider
public class DiagnosticReportFhirResourceProvider implements IResourceProvider {
	
	@Getter(PROTECTED)
	@Setter(value = PACKAGE, onMethod_ = @Autowired)
	private FhirDiagnosticReportService service;
	
	@Override
	public Class<? extends IBaseResource> getResourceType() {
		return DiagnosticReport.class;
	}
	
	@Read
	public DiagnosticReport getDiagnosticReportById(@IdParam @Nonnull IdType id) {
		DiagnosticReport diagnosticReport = service.get(id.getIdPart());
		
		if (diagnosticReport == null) {
			throw new ResourceNotFoundException("Could not find Diagnostic Report with Id " + id.getIdPart());
		}
		
		return diagnosticReport;
	}
	
	@Create
	public MethodOutcome createDiagnosticReport(@ResourceParam DiagnosticReport diagnosticReport) {
		return FhirProviderUtils.buildCreate(service.create(diagnosticReport));
	}
	
	@Update
	public MethodOutcome updateDiagnosticReport(@IdParam IdType id, @ResourceParam DiagnosticReport diagnosticReport) {
		if (id == null || id.getIdPart() == null) {
			throw new InvalidRequestException("id must be specified to update");
		}
		
		return FhirProviderUtils.buildUpdate(service.update(id.getIdPart(), diagnosticReport));
	}
	
	@Patch
	public MethodOutcome patchDiagnosticReport(@IdParam IdType id, PatchTypeEnum patchType, @ResourceParam String body,
	        RequestDetails requestDetails) {
		if (id == null || id.getIdPart() == null) {
			throw new InvalidRequestException("id must be specified to update DiagnosticReport resource");
		}
		
		DiagnosticReport diagnosticReport = service.patch(id.getIdPart(), patchType, body, requestDetails);
		
		return FhirProviderUtils.buildPatch(diagnosticReport);
	}
	
	@Delete
	@SuppressWarnings("unused")
	public OperationOutcome deleteDiagnosticReport(@IdParam @Nonnull IdType id) {
		service.delete(id.getIdPart());
		return FhirProviderUtils.buildDeleteR4();
	}
	
	@Search
	public IBundleProvider searchForDiagnosticReports(
	        @OptionalParam(name = DiagnosticReport.SP_ENCOUNTER, chainWhitelist = {
	                "" }, targetTypes = Encounter.class) ReferenceAndListParam encounterReference,
	        @OptionalParam(name = DiagnosticReport.SP_PATIENT, chainWhitelist = { "", Patient.SP_IDENTIFIER,
	                Patient.SP_GIVEN, Patient.SP_FAMILY,
	                Patient.SP_NAME }, targetTypes = Patient.class) ReferenceAndListParam patientReference,
	        @OptionalParam(name = DiagnosticReport.SP_SUBJECT, chainWhitelist = { "", Patient.SP_IDENTIFIER, Patient.SP_NAME,
	                Patient.SP_GIVEN, Patient.SP_FAMILY }) ReferenceAndListParam subjectReference,
	        @OptionalParam(name = DiagnosticReport.SP_ISSUED) DateRangeParam issueDate,
	        @OptionalParam(name = DiagnosticReport.SP_CODE) TokenAndListParam code,
	        @OptionalParam(name = DiagnosticReport.SP_RESULT) ReferenceAndListParam result,
	        @OptionalParam(name = DiagnosticReport.SP_RES_ID) TokenAndListParam id,
	        @OptionalParam(name = "_lastUpdated") DateRangeParam lastUpdated, @Sort SortSpec sort,
	        @IncludeParam(allow = { "DiagnosticReport:" + DiagnosticReport.SP_ENCOUNTER,
	                "DiagnosticReport:" + DiagnosticReport.SP_PATIENT,
	                "DiagnosticReport:" + DiagnosticReport.SP_RESULT }) HashSet<Include> includes) {
		if (patientReference == null) {
			patientReference = subjectReference;
		}
		
		if (CollectionUtils.isEmpty(includes)) {
			includes = null;
		}
		
		return service.searchForDiagnosticReports(new DiagnosticReportSearchParams(encounterReference, patientReference,
		        issueDate, code, result, id, lastUpdated, sort, includes));
	}
}
