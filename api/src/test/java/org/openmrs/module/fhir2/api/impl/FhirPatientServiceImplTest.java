/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.fhir2.api.impl;

import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import com.github.dnault.xmlpatch.repackaged.joptsimple.internal.Strings;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Observation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAddress;
import org.openmrs.PersonName;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.FhirGlobalPropertyService;
import org.openmrs.module.fhir2.api.dao.FhirPatientDao;
import org.openmrs.module.fhir2.api.dao.FhirPatientIdentifierSystemDao;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryBundleProvider;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.search.param.OpenmrsPatientSearchParams;
import org.openmrs.module.fhir2.api.search.param.PatientSearchParams;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.module.fhir2.api.translators.PatientTranslator;

@RunWith(MockitoJUnitRunner.class)
public class FhirPatientServiceImplTest {
	
	private static final Integer PATIENT_ID = 123;
	
	private static final String PATIENT_UUID = "3434gh32-34h3j4-34jk34-3422h";
	
	private static final String WRONG_PATIENT_UUID = "Wrong uuid";
	
	private static final String PATIENT_GIVEN_NAME = "Jeannette";
	
	private static final String PATIENT_GIVEN_NAME_NOT_MATCHED = "wafula";
	
	private static final String PATIENT_FAMILY_NAME_NOT_MATCHED = "your fam";
	
	private static final String PATIENT_PARTIAL_GIVEN_NAME = "Jean";
	
	private static final String PATIENT_FAMILY_NAME = "Ricky";
	
	private static final String PATIENT_PARTIAL_FAMILY_NAME = "Claud";
	
	private static final String GENDER = "M";
	
	private static final String WRONG_GENDER = "wrong-gender";
	
	private static final String DATE = "1996-12-12";
	
	private static final String UNKNOWN_DATE = "0001-10-10";
	
	private static final String CITY = "Washington";
	
	private static final String STATE = "Washington";
	
	private static final String POSTAL_CODE = "98136";
	
	private static final String COUNTRY = "Washington";
	
	private static final String UNKNOWN_ADDRESS = "unknown address";
	
	private static final String LAST_UPDATED_DATE = "2020-09-03";
	
	private static final String WRONG_LAST_UPDATED_DATE = "2020-09-09";
	
	private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
	
	@Mock
	private PatientTranslator patientTranslator;
	
	@Mock
	private FhirPatientDao dao;
	
	@Mock
	private FhirPatientIdentifierSystemDao systemDao;
	
	@Mock
	private FhirGlobalPropertyService globalPropertyService;
	
	@Mock
	private SearchQueryInclude<org.hl7.fhir.r4.model.Patient> searchQueryInclude;
	
	@Mock
	private SearchQuery<Patient, org.hl7.fhir.r4.model.Patient, FhirPatientDao, PatientTranslator, SearchQueryInclude<org.hl7.fhir.r4.model.Patient>> searchQuery;
	
	private FhirPatientServiceImpl patientService;
	
	private org.hl7.fhir.r4.model.Patient fhirPatient;
	
	private Patient patient;
	
	@Before
	public void setUp() {
		patientService = new FhirPatientServiceImpl() {
			
			@Override
			protected void validateObject(Patient object) {
			}
		};
		
		patientService.setDao(dao);
		patientService.setSystemDao(systemDao);
		patientService.setTranslator(patientTranslator);
		patientService.setSearchQuery(searchQuery);
		patientService.setSearchQueryInclude(searchQueryInclude);
		
		PersonName name = new PersonName();
		name.setFamilyName(PATIENT_FAMILY_NAME);
		name.setGivenName(PATIENT_GIVEN_NAME);
		
		patient = new Patient();
		patient.setUuid(PATIENT_UUID);
		patient.setGender("M");
		patient.addName(name);
		
		PersonAddress address = new PersonAddress();
		address.setCityVillage(CITY);
		address.setStateProvince(STATE);
		address.setPostalCode(POSTAL_CODE);
		address.setCountry(COUNTRY);
		
		HumanName humanName = new HumanName();
		humanName.addGiven(PATIENT_GIVEN_NAME);
		humanName.setFamily(PATIENT_FAMILY_NAME);
		
		fhirPatient = new org.hl7.fhir.r4.model.Patient();
		fhirPatient.setId(PATIENT_UUID);
		fhirPatient.addName(humanName);
	}
	
	@Test
	public void getPatientByUuid_shouldRetrievePatientByUuid() {
		when(dao.get(PATIENT_UUID)).thenReturn(patient);
		when(patientTranslator.toFhirResource(patient)).thenReturn(fhirPatient);
		
		org.hl7.fhir.r4.model.Patient result = patientService.get(PATIENT_UUID);
		
		assertThat(result, notNullValue());
		assertThat(result.getId(), notNullValue());
		assertThat(result.getId(), equalTo(PATIENT_UUID));
	}
	
	@Test
	public void getById_shouldReturnPatientById() {
		when(dao.getPatientById(1)).thenReturn(patient);
		when(patientTranslator.toFhirResource(patient)).thenReturn(fhirPatient);
		
		org.hl7.fhir.r4.model.Patient result = patientService.getById(1);
		
		assertThat(result, notNullValue());
		assertThat(result.getId(), notNullValue());
		assertThat(result.getId(), equalTo(PATIENT_UUID));
	}
	
	@Test
	public void getByIds_shouldRetrievePatientsByIds() {
		when(dao.getPatientsByIds(anySet())).thenReturn(Arrays.asList(patient, patient, patient));
		when(patientTranslator.toFhirResource(patient)).thenReturn(fhirPatient);
		
		List<org.hl7.fhir.r4.model.Patient> patientList = patientService
		        .getPatientsByIds(new HashSet<>(Arrays.asList(1, 2, 3)));
		
		assertThat(patientList, notNullValue());
		assertThat(patientList, not(empty()));
		assertThat(patientList, hasSize(3));
	}
	
	@Test
	public void searchForPatients_shouldSearchForPatientsByName() {
		List<Patient> patients = new ArrayList<>();
		patients.add(patient);
		StringAndListParam stringAndListParam = new StringAndListParam().addAnd(new StringParam(PATIENT_GIVEN_NAME));
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.NAME_SEARCH_HANDLER,
		    stringAndListParam);
		
		when(dao.getSearchResults(any())).thenReturn(patients);
		when(dao.getSearchResultsCount(any())).thenReturn(1);
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		    new SearchQueryBundleProvider<>(theParams, dao, patientTranslator, globalPropertyService, searchQueryInclude));
		when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.emptySet());
		when(patientTranslator.toFhirResource(patient)).thenReturn(fhirPatient);
		when(patientTranslator.toFhirResources(anyCollection())).thenCallRealMethod();
		
		IBundleProvider results = patientService.searchForPatients(new PatientSearchParams(stringAndListParam, null, null,
		        null, null, null, null, null, null, null, null, null, null, null, null, null, null));
		
		assertThat(results, notNullValue());
		assertThat(results.size(), equalTo(1));
		
		assertThat(get(results), hasSize(equalTo(1)));
	}
	
	@Test
	public void searchForPatients_shouldSearchForPatientsByGivenName() {
		List<Patient> patients = new ArrayList<>();
		patients.add(patient);
		StringAndListParam stringAndListParam = new StringAndListParam().addAnd(new StringParam(PATIENT_GIVEN_NAME));
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.NAME_SEARCH_HANDLER,
		    stringAndListParam);
		
		when(dao.getSearchResults(any())).thenReturn(patients);
		when(dao.getSearchResultsCount(any())).thenReturn(1);
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		    new SearchQueryBundleProvider<>(theParams, dao, patientTranslator, globalPropertyService, searchQueryInclude));
		when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.emptySet());
		when(patientTranslator.toFhirResource(patient)).thenReturn(fhirPatient);
		when(patientTranslator.toFhirResources(anyCollection())).thenCallRealMethod();
		
		IBundleProvider results = patientService.searchForPatients(new PatientSearchParams(null, stringAndListParam, null,
		        null, null, null, null, null, null, null, null, null, null, null, null, null, null));
		
		assertThat(results, notNullValue());
		assertThat(results.size(), equalTo(1));
		
		assertThat(get(results), not(empty()));
		assertThat(get(results), hasSize(equalTo(1)));
	}
	
	@Test
	public void searchForPatients_shouldSearchForPatientsByFamilyName() {
		List<Patient> patients = new ArrayList<>();
		patients.add(patient);
		StringAndListParam stringAndListParam = new StringAndListParam().addAnd(new StringParam(PATIENT_FAMILY_NAME));
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.NAME_SEARCH_HANDLER,
		    stringAndListParam);
		
		when(globalPropertyService.getGlobalPropertyAsInteger(anyString(), anyInt())).thenReturn(10);
		when(dao.getSearchResults(any())).thenReturn(patients);
		when(dao.getSearchResultsCount(any())).thenReturn(1);
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		    new SearchQueryBundleProvider<>(theParams, dao, patientTranslator, globalPropertyService, searchQueryInclude));
		when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.emptySet());
		when(patientTranslator.toFhirResource(patient)).thenReturn(fhirPatient);
		when(patientTranslator.toFhirResources(anyCollection())).thenCallRealMethod();
		
		IBundleProvider results = patientService.searchForPatients(new PatientSearchParams(null, null, stringAndListParam,
		        null, null, null, null, null, null, null, null, null, null, null, null, null, null));
		
		assertThat(results.getUuid(), notNullValue());
		assertThat(results.size(), equalTo(1));
		assertThat(results.preferredPageSize(), equalTo(10));
		assertThat(get(results), not(empty()));
	}
	
	@Test
	public void searchForPatients_shouldReturnCollectionOfPatientsForPartialMatchOnGivenName() {
		List<Patient> patients = new ArrayList<>();
		patients.add(patient);
		StringAndListParam stringAndListParam = new StringAndListParam().addAnd(new StringParam(PATIENT_PARTIAL_GIVEN_NAME));
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.NAME_SEARCH_HANDLER,
		    stringAndListParam);
		
		when(dao.getSearchResults(any())).thenReturn(patients);
		when(dao.getSearchResultsCount(any())).thenReturn(1);
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		    new SearchQueryBundleProvider<>(theParams, dao, patientTranslator, globalPropertyService, searchQueryInclude));
		when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.emptySet());
		when(patientTranslator.toFhirResource(patient)).thenReturn(fhirPatient);
		when(patientTranslator.toFhirResources(anyCollection())).thenCallRealMethod();
		
		IBundleProvider results = patientService.searchForPatients(new PatientSearchParams(null, stringAndListParam, null,
		        null, null, null, null, null, null, null, null, null, null, null, null, null, null));
		
		assertThat(results, notNullValue());
		assertThat(results.size(), greaterThanOrEqualTo(1));
		assertThat(get(results), not(empty()));
	}
	
	@Test
	public void searchForPatients_shouldReturnCollectionOfPatientsForPartialMatchOnFamilyName() {
		List<Patient> patients = new ArrayList<>();
		patients.add(patient);
		StringAndListParam stringAndListParam = new StringAndListParam()
		        .addAnd(new StringParam(PATIENT_PARTIAL_FAMILY_NAME));
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.NAME_SEARCH_HANDLER,
		    stringAndListParam);
		
		when(dao.getSearchResults(any())).thenReturn(patients);
		when(dao.getSearchResultsCount(any())).thenReturn(1);
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		    new SearchQueryBundleProvider<>(theParams, dao, patientTranslator, globalPropertyService, searchQueryInclude));
		when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.emptySet());
		when(patientTranslator.toFhirResource(patient)).thenReturn(fhirPatient);
		when(patientTranslator.toFhirResources(anyCollection())).thenCallRealMethod();
		
		IBundleProvider results = patientService.searchForPatients(new PatientSearchParams(null, null, stringAndListParam,
		        null, null, null, null, null, null, null, null, null, null, null, null, null, null));
		
		assertThat(results, notNullValue());
		assertThat(results.size(), greaterThanOrEqualTo(1));
		assertThat(get(results), not(empty()));
	}
	
	@Test
	public void searchForPatients_shouldReturnEmptyCollectionWhenPatientNameNotMatched() {
		StringAndListParam stringAndListParam = new StringAndListParam()
		        .addAnd(new StringParam(PATIENT_GIVEN_NAME_NOT_MATCHED));
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.NAME_SEARCH_HANDLER,
		    stringAndListParam);
		
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		    new SearchQueryBundleProvider<>(theParams, dao, patientTranslator, globalPropertyService, searchQueryInclude));
		
		IBundleProvider results = patientService.searchForPatients(new PatientSearchParams(stringAndListParam, null, null,
		        null, null, null, null, null, null, null, null, null, null, null, null, null, null));
		
		assertThat(results, notNullValue());
		assertThat(get(results), is(empty()));
	}
	
	@Test
	public void searchForPatients_shouldReturnEmptyCollectionWhenPatientGivenNameNotMatched() {
		StringAndListParam stringAndListParam = new StringAndListParam()
		        .addAnd(new StringParam(PATIENT_GIVEN_NAME_NOT_MATCHED));
		
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.NAME_SEARCH_HANDLER,
		    stringAndListParam);
		
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		    new SearchQueryBundleProvider<>(theParams, dao, patientTranslator, globalPropertyService, searchQueryInclude));
		
		IBundleProvider results = patientService.searchForPatients(new PatientSearchParams(null, stringAndListParam, null,
		        null, null, null, null, null, null, null, null, null, null, null, null, null, null));
		
		assertThat(results, notNullValue());
		assertThat(get(results), is(empty()));
	}
	
	@Test
	public void searchForPatients_shouldReturnEmptyCollectionWhenPatientFamilyNameNotMatched() {
		StringAndListParam stringAndListParam = new StringAndListParam()
		        .addAnd(new StringParam(PATIENT_FAMILY_NAME_NOT_MATCHED));
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.NAME_SEARCH_HANDLER,
		    stringAndListParam);
		
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		    new SearchQueryBundleProvider<>(theParams, dao, patientTranslator, globalPropertyService, searchQueryInclude));
		
		IBundleProvider results = patientService.searchForPatients(new PatientSearchParams(null, null, stringAndListParam,
		        null, null, null, null, null, null, null, null, null, null, null, null, null, null));
		
		assertThat(results, notNullValue());
		assertThat(get(results), is(empty()));
	}
	
	@Test
	public void searchForPatients_shouldReturnCollectionOfPatientWhenPatientGenderMatched() {
		List<Patient> patients = new ArrayList<>();
		patients.add(patient);
		TokenAndListParam tokenAndListParam = new TokenAndListParam().addAnd(new TokenParam(GENDER));
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.NAME_SEARCH_HANDLER,
		    tokenAndListParam);
		
		when(dao.getSearchResults(any())).thenReturn(patients);
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		    new SearchQueryBundleProvider<>(theParams, dao, patientTranslator, globalPropertyService, searchQueryInclude));
		when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.emptySet());
		when(patientTranslator.toFhirResource(patient)).thenReturn(fhirPatient);
		when(patientTranslator.toFhirResources(anyCollection())).thenCallRealMethod();
		
		IBundleProvider results = patientService.searchForPatients(new PatientSearchParams(null, null, null, null,
		        tokenAndListParam, null, null, null, null, null, null, null, null, null, null, null, null));
		
		assertThat(results, notNullValue());
		assertThat(get(results), not(empty()));
		assertThat(get(results), hasSize(greaterThanOrEqualTo(1)));
	}
	
	@Test
	public void searchForPatients_shouldReturnEmptyCollectionWhenPatientGenderNotMatched() {
		TokenAndListParam tokenAndListParam = new TokenAndListParam().addAnd(new TokenParam(WRONG_GENDER));
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.NAME_SEARCH_HANDLER,
		    tokenAndListParam);
		
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		    new SearchQueryBundleProvider<>(theParams, dao, patientTranslator, globalPropertyService, searchQueryInclude));
		
		IBundleProvider results = patientService.searchForPatients(new PatientSearchParams(null, null, null, null,
		        tokenAndListParam, null, null, null, null, null, null, null, null, null, null, null, null));
		
		assertThat(results, notNullValue());
		assertThat(get(results), is(empty()));
	}
	
	@Test
	public void searchForPatients_shouldReturnCollectionOfPatientWhenPatientBirthDateMatched() throws ParseException {
		Date birthDate = dateFormatter.parse(DATE);
		patient.setBirthdate(birthDate);
		
		List<Patient> patients = new ArrayList<>();
		patients.add(patient);
		
		DateRangeParam dateRangeParam = new DateRangeParam().setLowerBound(DATE).setUpperBound(DATE);
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.DATE_RANGE_SEARCH_HANDLER,
		    dateRangeParam);
		
		when(dao.getSearchResults(any())).thenReturn(patients);
		when(dao.getSearchResultsCount(any())).thenReturn(1);
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		    new SearchQueryBundleProvider<>(theParams, dao, patientTranslator, globalPropertyService, searchQueryInclude));
		when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.emptySet());
		when(patientTranslator.toFhirResource(patient)).thenReturn(fhirPatient);
		when(patientTranslator.toFhirResources(anyCollection())).thenCallRealMethod();
		
		IBundleProvider results = patientService.searchForPatients(new PatientSearchParams(null, null, null, null, null,
		        dateRangeParam, null, null, null, null, null, null, null, null, null, null, null));
		
		assertThat(results, notNullValue());
		assertThat(results.size(), greaterThanOrEqualTo(1));
		assertThat(get(results), not(empty()));
	}
	
	@Test
	public void searchForPatients_shouldReturnEmptyCollectionWhenPatientBirthDateNotMatched() {
		DateRangeParam dateRangeParam = new DateRangeParam().setLowerBound(UNKNOWN_DATE).setUpperBound(UNKNOWN_DATE);
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.NAME_SEARCH_HANDLER,
		    dateRangeParam);
		
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		    new SearchQueryBundleProvider<>(theParams, dao, patientTranslator, globalPropertyService, searchQueryInclude));
		
		IBundleProvider results = patientService.searchForPatients(new PatientSearchParams(null, null, null, null, null,
		        dateRangeParam, null, null, null, null, null, null, null, null, null, null, null));
		
		assertThat(results, notNullValue());
		assertThat(get(results), empty());
	}
	
	@Test
	public void searchForPatients_shouldReturnCollectionOfPatientWhenPatientDeathDateMatched() throws ParseException {
		Date deathDate = dateFormatter.parse(DATE);
		patient.setDeathDate(deathDate);
		
		List<Patient> patients = new ArrayList<>();
		patients.add(patient);
		
		DateRangeParam dateRangeParam = new DateRangeParam().setLowerBound(DATE).setUpperBound(DATE);
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.DATE_RANGE_SEARCH_HANDLER,
		    dateRangeParam);
		
		when(dao.getSearchResults(any())).thenReturn(patients);
		when(dao.getSearchResultsCount(any())).thenReturn(1);
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		    new SearchQueryBundleProvider<>(theParams, dao, patientTranslator, globalPropertyService, searchQueryInclude));
		when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.emptySet());
		when(patientTranslator.toFhirResource(patient)).thenReturn(fhirPatient);
		when(patientTranslator.toFhirResources(anyCollection())).thenCallRealMethod();
		
		IBundleProvider results = patientService.searchForPatients(new PatientSearchParams(null, null, null, null, null,
		        null, dateRangeParam, null, null, null, null, null, null, null, null, null, null));
		
		assertThat(results, notNullValue());
		assertThat(results.size(), greaterThanOrEqualTo(1));
		assertThat(get(results), not(empty()));
	}
	
	@Test
	public void searchForPatients_shouldReturnEmptyCollectionWhenPatientDeathDateNotMatched() {
		DateRangeParam dateRangeParam = new DateRangeParam().setLowerBound(UNKNOWN_DATE).setUpperBound(UNKNOWN_DATE);
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.NAME_SEARCH_HANDLER,
		    dateRangeParam);
		
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		    new SearchQueryBundleProvider<>(theParams, dao, patientTranslator, globalPropertyService, searchQueryInclude));
		
		IBundleProvider results = patientService.searchForPatients(new PatientSearchParams(null, null, null, null, null,
		        null, dateRangeParam, null, null, null, null, null, null, null, null, null, null));
		
		assertThat(results, notNullValue());
		assertThat(get(results), empty());
	}
	
	@Test
	public void searchForPatients_shouldReturnCollectionOfPatientWhenPatientCityMatched() {
		List<Patient> patients = new ArrayList<>();
		patients.add(patient);
		StringAndListParam stringAndListParam = new StringAndListParam().addAnd(new StringParam(CITY));
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.NAME_SEARCH_HANDLER, "city",
		    stringAndListParam);
		
		when(dao.getSearchResults(any())).thenReturn(patients);
		when(dao.getSearchResultsCount(any())).thenReturn(1);
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		    new SearchQueryBundleProvider<>(theParams, dao, patientTranslator, globalPropertyService, searchQueryInclude));
		when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.emptySet());
		when(patientTranslator.toFhirResource(patient)).thenReturn(fhirPatient);
		when(patientTranslator.toFhirResources(anyCollection())).thenCallRealMethod();
		
		IBundleProvider results = patientService.searchForPatients(new PatientSearchParams(null, null, null, null, null,
		        null, null, null, stringAndListParam, null, null, null, null, null, null, null, null));
		
		assertThat(results, notNullValue());
		assertThat(results.size(), greaterThanOrEqualTo(1));
		assertThat(get(results), not(empty()));
	}
	
	@Test
	public void searchForPatients_shouldReturnEmptyCollectionWhenPatientCityNotMatched() {
		StringAndListParam stringAndListParam = new StringAndListParam().addAnd(new StringParam(UNKNOWN_ADDRESS));
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.NAME_SEARCH_HANDLER, "city",
		    stringAndListParam);
		
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		    new SearchQueryBundleProvider<>(theParams, dao, patientTranslator, globalPropertyService, searchQueryInclude));
		
		IBundleProvider results = patientService.searchForPatients(new PatientSearchParams(null, null, null, null, null,
		        null, null, null, stringAndListParam, null, null, null, null, null, null, null, null));
		
		assertThat(results, notNullValue());
		assertThat(get(results), empty());
	}
	
	@Test
	public void searchForPatients_shouldReturnCollectionOfPatientWhenPatientStateMatched() {
		List<Patient> patients = new ArrayList<>();
		patients.add(patient);
		StringAndListParam stringAndListParam = new StringAndListParam().addAnd(new StringParam(STATE));
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.NAME_SEARCH_HANDLER, "state",
		    stringAndListParam);
		
		when(dao.getSearchResults(any())).thenReturn(patients);
		when(dao.getSearchResultsCount(any())).thenReturn(1);
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		    new SearchQueryBundleProvider<>(theParams, dao, patientTranslator, globalPropertyService, searchQueryInclude));
		when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.emptySet());
		when(patientTranslator.toFhirResource(patient)).thenReturn(fhirPatient);
		when(patientTranslator.toFhirResources(anyCollection())).thenCallRealMethod();
		
		IBundleProvider results = patientService.searchForPatients(new PatientSearchParams(null, null, null, null, null,
		        null, null, null, null, stringAndListParam, null, null, null, null, null, null, null));
		
		assertThat(results, notNullValue());
		assertThat(results.size(), greaterThanOrEqualTo(1));
		assertThat(get(results), not(empty()));
	}
	
	@Test
	public void searchForPatients_shouldReturnEmptyCollectionWhenPatientStateNotMatched() {
		StringAndListParam stringAndListParam = new StringAndListParam().addAnd(new StringParam(UNKNOWN_ADDRESS));
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.NAME_SEARCH_HANDLER, "state",
		    stringAndListParam);
		
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		    new SearchQueryBundleProvider<>(theParams, dao, patientTranslator, globalPropertyService, searchQueryInclude));
		
		IBundleProvider results = patientService.searchForPatients(new PatientSearchParams(null, null, null, null, null,
		        null, null, null, null, stringAndListParam, null, null, null, null, null, null, null));
		
		assertThat(results, notNullValue());
		assertThat(get(results), empty());
	}
	
	@Test
	public void searchForPatients_shouldReturnCollectionOfPatientWhenPatientPostalCodeMatched() {
		List<Patient> patients = new ArrayList<>();
		patients.add(patient);
		StringAndListParam stringAndListParam = new StringAndListParam().addAnd(new StringParam(POSTAL_CODE));
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.NAME_SEARCH_HANDLER, "postalCode",
		    stringAndListParam);
		
		when(dao.getSearchResults(any())).thenReturn(patients);
		when(dao.getSearchResultsCount(any())).thenReturn(1);
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		    new SearchQueryBundleProvider<>(theParams, dao, patientTranslator, globalPropertyService, searchQueryInclude));
		when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.emptySet());
		when(patientTranslator.toFhirResource(patient)).thenReturn(fhirPatient);
		when(patientTranslator.toFhirResources(anyCollection())).thenCallRealMethod();
		
		IBundleProvider results = patientService.searchForPatients(new PatientSearchParams(null, null, null, null, null,
		        null, null, null, null, null, stringAndListParam, null, null, null, null, null, null));
		
		assertThat(results, notNullValue());
		assertThat(results.size(), greaterThanOrEqualTo(1));
		assertThat(get(results), not(empty()));
	}
	
	@Test
	public void searchForPatients_shouldReturnEmptyCollectionWhenPatientPostalCodeNotMatched() {
		StringAndListParam stringAndListParam = new StringAndListParam().addAnd(new StringParam(UNKNOWN_ADDRESS));
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.NAME_SEARCH_HANDLER, "postalCode",
		    stringAndListParam);
		
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		    new SearchQueryBundleProvider<>(theParams, dao, patientTranslator, globalPropertyService, searchQueryInclude));
		
		IBundleProvider results = patientService.searchForPatients(new PatientSearchParams(null, null, null, null, null,
		        null, null, null, null, null, stringAndListParam, null, null, null, null, null, null));
		assertThat(results, notNullValue());
		assertThat(get(results), empty());
	}
	
	@Test
	public void searchForPatients_shouldReturnCollectionOfPatientWhenPatientCountryMatched() {
		List<Patient> patients = new ArrayList<>();
		patients.add(patient);
		StringAndListParam stringAndListParam = new StringAndListParam().addAnd(new StringParam(COUNTRY));
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.NAME_SEARCH_HANDLER, "country",
		    stringAndListParam);
		
		when(dao.getSearchResults(any())).thenReturn(patients);
		when(dao.getSearchResultsCount(any())).thenReturn(1);
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		    new SearchQueryBundleProvider<>(theParams, dao, patientTranslator, globalPropertyService, searchQueryInclude));
		when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.emptySet());
		when(patientTranslator.toFhirResource(patient)).thenReturn(fhirPatient);
		when(patientTranslator.toFhirResources(anyCollection())).thenCallRealMethod();
		
		IBundleProvider results = patientService.searchForPatients(new PatientSearchParams(null, null, null, null, null,
		        null, null, null, null, null, null, stringAndListParam, null, null, null, null, null));
		
		assertThat(results, notNullValue());
		assertThat(results.size(), greaterThanOrEqualTo(1));
		assertThat(get(results), not(empty()));
	}
	
	@Test
	public void searchForPatients_shouldReturnEmptyCollectionWhenPatientCountryNotMatched() {
		StringAndListParam stringAndListParam = new StringAndListParam().addAnd(new StringParam(UNKNOWN_ADDRESS));
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.NAME_SEARCH_HANDLER,
		    stringAndListParam);
		
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		    new SearchQueryBundleProvider<>(theParams, dao, patientTranslator, globalPropertyService, searchQueryInclude));
		
		IBundleProvider results = patientService.searchForPatients(new PatientSearchParams(null, null, null, null, null,
		        null, null, null, null, null, null, stringAndListParam, null, null, null, null, null));
		
		assertThat(results, notNullValue());
		assertThat(get(results), empty());
	}
	
	@Test
	public void searchForPatients_shouldReturnCollectionOfPatientWhenUUIDMatched() {
		TokenAndListParam uuid = new TokenAndListParam().addAnd(new TokenParam(PATIENT_UUID));
		
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.COMMON_SEARCH_HANDLER,
		    FhirConstants.ID_PROPERTY, uuid);
		
		when(dao.getSearchResults(any())).thenReturn(Collections.singletonList(patient));
		when(dao.getSearchResultsCount(any())).thenReturn(1);
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		    new SearchQueryBundleProvider<>(theParams, dao, patientTranslator, globalPropertyService, searchQueryInclude));
		when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.emptySet());
		when(patientTranslator.toFhirResource(patient)).thenReturn(fhirPatient);
		when(patientTranslator.toFhirResources(anyCollection())).thenCallRealMethod();
		
		IBundleProvider results = patientService.searchForPatients(new PatientSearchParams(null, null, null, null, null,
		        null, null, null, null, null, null, null, uuid, null, null, null, null));
		
		assertThat(results, notNullValue());
		assertThat(results.size(), greaterThanOrEqualTo(1));
		assertThat(get(results), not(empty()));
	}
	
	@Test
	public void searchForPatients_shouldReturnEmptyCollectionWhenUUIDNotMatched() {
		TokenAndListParam uuid = new TokenAndListParam().addAnd(new TokenParam(WRONG_PATIENT_UUID));
		
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.COMMON_SEARCH_HANDLER,
		    FhirConstants.ID_PROPERTY, uuid);
		
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		    new SearchQueryBundleProvider<>(theParams, dao, patientTranslator, globalPropertyService, searchQueryInclude));
		
		IBundleProvider results = patientService.searchForPatients(new PatientSearchParams(null, null, null, null, null,
		        null, null, null, null, null, null, null, uuid, null, null, null, null));
		
		assertThat(results, notNullValue());
		assertThat(get(results), empty());
	}
	
	@Test
	public void searchForPatients_shouldReturnCollectionOfPatientWhenLastUpdatedMatched() {
		DateRangeParam lastUpdated = new DateRangeParam().setUpperBound(LAST_UPDATED_DATE).setLowerBound(LAST_UPDATED_DATE);
		
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.COMMON_SEARCH_HANDLER,
		    FhirConstants.LAST_UPDATED_PROPERTY, lastUpdated);
		
		when(dao.getSearchResults(any())).thenReturn(Collections.singletonList(patient));
		when(dao.getSearchResultsCount(any())).thenReturn(1);
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		    new SearchQueryBundleProvider<>(theParams, dao, patientTranslator, globalPropertyService, searchQueryInclude));
		when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.emptySet());
		when(patientTranslator.toFhirResource(patient)).thenReturn(fhirPatient);
		when(patientTranslator.toFhirResources(anyCollection())).thenCallRealMethod();
		
		IBundleProvider results = patientService.searchForPatients(new PatientSearchParams(null, null, null, null, null,
		        null, null, null, null, null, null, null, null, null, lastUpdated, null, null));
		
		assertThat(results, notNullValue());
		assertThat(results.size(), greaterThanOrEqualTo(1));
		assertThat(get(results), not(empty()));
	}
	
	@Test
	public void searchForPatients_shouldReturnEmptyCollectionWhenLastUpdatedNotMatched() {
		DateRangeParam lastUpdated = new DateRangeParam().setUpperBound(WRONG_LAST_UPDATED_DATE)
		        .setLowerBound(WRONG_LAST_UPDATED_DATE);
		
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.COMMON_SEARCH_HANDLER,
		    FhirConstants.LAST_UPDATED_PROPERTY, lastUpdated);
		
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		    new SearchQueryBundleProvider<>(theParams, dao, patientTranslator, globalPropertyService, searchQueryInclude));
		
		IBundleProvider results = patientService.searchForPatients(new PatientSearchParams(null, null, null, null, null,
		        null, null, null, null, null, null, null, null, null, lastUpdated, null, null));
		
		assertThat(results, notNullValue());
		assertThat(get(results), empty());
	}
	
	@Test
	public void searchForPatients_shouldAddReverseIncludedResourcesToResultList() {
		HashSet<Include> revIncludes = new HashSet<>();
		revIncludes.add(new Include("Observation:patient"));
		
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.REVERSE_INCLUDE_SEARCH_HANDLER,
		    revIncludes);
		
		when(dao.getSearchResults(any())).thenReturn(Collections.singletonList(patient));
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		    new SearchQueryBundleProvider<>(theParams, dao, patientTranslator, globalPropertyService, searchQueryInclude));
		when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.singleton(new Observation()));
		when(patientTranslator.toFhirResource(patient)).thenReturn(fhirPatient);
		when(patientTranslator.toFhirResources(anyCollection())).thenCallRealMethod();
		
		IBundleProvider results = patientService.searchForPatients(new PatientSearchParams(null, null, null, null, null,
		        null, null, null, null, null, null, null, null, null, null, null, revIncludes));
		
		List<IBaseResource> resultList = get(results);
		
		assertThat(results, notNullValue());
		assertThat(resultList, not(empty()));
		assertThat(resultList.size(), greaterThanOrEqualTo(2));
		assertThat(resultList, hasItem(is(instanceOf(Observation.class))));
	}
	
	@Test
	public void searchForPatients_shouldNotAddRelatedResourcesToResultListForEmptyRevInclude() {
		HashSet<Include> revIncludes = new HashSet<>();
		
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.REVERSE_INCLUDE_SEARCH_HANDLER,
		    revIncludes);
		
		when(dao.getSearchResults(any())).thenReturn(Collections.singletonList(patient));
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		    new SearchQueryBundleProvider<>(theParams, dao, patientTranslator, globalPropertyService, searchQueryInclude));
		when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.emptySet());
		when(patientTranslator.toFhirResource(patient)).thenReturn(fhirPatient);
		when(patientTranslator.toFhirResources(anyCollection())).thenCallRealMethod();
		
		IBundleProvider results = patientService.searchForPatients(new PatientSearchParams(null, null, null, null, null,
		        null, null, null, null, null, null, null, null, null, null, null, revIncludes));
		
		List<IBaseResource> resultList = get(results);
		
		assertThat(results, notNullValue());
		assertThat(resultList, not(empty()));
		assertThat(resultList.size(), greaterThanOrEqualTo(1));
	}
	
	@Test
	public void searchForPatients_shouldSearchForPatientsByOpenmrsQuery() {
		List<Patient> patients = new ArrayList<>();
		patients.add(patient);
		StringAndListParam stringAndListParam = new StringAndListParam().addAnd(new StringParam(PATIENT_GIVEN_NAME));
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.QUERY_SEARCH_HANDLER,
		    stringAndListParam);
		
		when(dao.getSearchResults(any())).thenReturn(patients);
		when(dao.getSearchResultsCount(any())).thenReturn(1);
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		    new SearchQueryBundleProvider<>(theParams, dao, patientTranslator, globalPropertyService, searchQueryInclude));
		when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.emptySet());
		when(patientTranslator.toFhirResource(patient)).thenReturn(fhirPatient);
		when(patientTranslator.toFhirResources(anyCollection())).thenCallRealMethod();
		
		IBundleProvider results = patientService
		        .searchForPatients(OpenmrsPatientSearchParams.builder().query(stringAndListParam).build());
		
		assertThat(results, notNullValue());
		assertThat(results.size(), equalTo(1));
		
		assertThat(get(results), hasSize(equalTo(1)));
	}
	
	@Test
	public void getPatientEverything_shouldReturnAllInformationAboutSpecifiedPatient() {
		TokenParam patientId = new TokenParam().setValue(PATIENT_UUID);
		
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.EVERYTHING_SEARCH_HANDLER, "")
		        .addParameter(FhirConstants.COMMON_SEARCH_HANDLER, FhirConstants.ID_PROPERTY, patientId);
		
		HashSet<Include> revIncludes = new HashSet<>();
		
		revIncludes.add(new Include(FhirConstants.OBSERVATION + ":" + FhirConstants.INCLUDE_PATIENT_PARAM));
		revIncludes.add(new Include(FhirConstants.ALLERGY_INTOLERANCE + ":" + FhirConstants.INCLUDE_PATIENT_PARAM));
		revIncludes.add(new Include(FhirConstants.DIAGNOSTIC_REPORT + ":" + FhirConstants.INCLUDE_PATIENT_PARAM));
		revIncludes.add(new Include(FhirConstants.ENCOUNTER + ":" + FhirConstants.INCLUDE_PATIENT_PARAM));
		revIncludes.add(new Include(FhirConstants.MEDICATION_REQUEST + ":" + FhirConstants.INCLUDE_PATIENT_PARAM));
		revIncludes.add(new Include(FhirConstants.SERVICE_REQUEST + ":" + FhirConstants.INCLUDE_PATIENT_PARAM));
		revIncludes.add(new Include(FhirConstants.PROCEDURE_REQUEST + ":" + FhirConstants.INCLUDE_PATIENT_PARAM));
		
		theParams.addParameter(FhirConstants.REVERSE_INCLUDE_SEARCH_HANDLER, revIncludes);
		
		when(dao.getSearchResults(any())).thenReturn(Collections.singletonList(patient));
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		    new SearchQueryBundleProvider<>(theParams, dao, patientTranslator, globalPropertyService, searchQueryInclude));
		
		when(patientTranslator.toFhirResource(patient)).thenReturn(fhirPatient);
		when(patientTranslator.toFhirResources(anyCollection())).thenCallRealMethod();
		
		IBundleProvider results = patientService.getPatientEverything(patientId);
		
		List<IBaseResource> resultList = get(results);
		
		assertThat(results, notNullValue());
		assertThat(resultList, not(empty()));
		assertThat(resultList.size(), greaterThanOrEqualTo(1));
		
	}
	
	@Test
	public void getPatientEverything_shouldReturnAllInformationAboutAllPatients() {
		SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.EVERYTHING_SEARCH_HANDLER, "");
		
		HashSet<Include> revIncludes = new HashSet<>();
		
		revIncludes.add(new Include(FhirConstants.OBSERVATION + ":" + FhirConstants.INCLUDE_PATIENT_PARAM));
		revIncludes.add(new Include(FhirConstants.ALLERGY_INTOLERANCE + ":" + FhirConstants.INCLUDE_PATIENT_PARAM));
		revIncludes.add(new Include(FhirConstants.DIAGNOSTIC_REPORT + ":" + FhirConstants.INCLUDE_PATIENT_PARAM));
		revIncludes.add(new Include(FhirConstants.ENCOUNTER + ":" + FhirConstants.INCLUDE_PATIENT_PARAM));
		revIncludes.add(new Include(FhirConstants.MEDICATION_REQUEST + ":" + FhirConstants.INCLUDE_PATIENT_PARAM));
		revIncludes.add(new Include(FhirConstants.SERVICE_REQUEST + ":" + FhirConstants.INCLUDE_PATIENT_PARAM));
		revIncludes.add(new Include(FhirConstants.PROCEDURE_REQUEST + ":" + FhirConstants.INCLUDE_PATIENT_PARAM));
		
		theParams.addParameter(FhirConstants.REVERSE_INCLUDE_SEARCH_HANDLER, revIncludes);
		
		when(dao.getSearchResults(any())).thenReturn(Collections.singletonList(patient));
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		    new SearchQueryBundleProvider<>(theParams, dao, patientTranslator, globalPropertyService, searchQueryInclude));
		
		when(patientTranslator.toFhirResource(patient)).thenReturn(fhirPatient);
		when(patientTranslator.toFhirResources(anyCollection())).thenCallRealMethod();
		
		IBundleProvider results = patientService.getPatientEverything();
		
		List<IBaseResource> resultList = get(results);
		
		assertThat(results, notNullValue());
		assertThat(resultList, not(empty()));
		assertThat(resultList.size(), greaterThanOrEqualTo(1));
	}
	
	@Test
	public void getPatientIdentifierTypeByIdentifier_shouldReturnIdentifierTypeWhenPresent() {
		String typeName = "some-type";
		
		Identifier identifier = new Identifier();
		identifier.setType(new CodeableConcept().setText(typeName));
		
		PatientIdentifierType expectedType = new PatientIdentifierType();
		expectedType.setName(typeName);
		
		when(dao.getPatientIdentifierTypeByNameOrUuid(typeName, null)).thenReturn(expectedType);
		
		PatientIdentifierType result = patientService.getPatientIdentifierTypeByIdentifier(identifier);
		assertNull(identifier.getSystem());
		assertEquals(expectedType, result);
	}
	
	@Test
	public void getPatientIdentifierTypeByIdentifier_shouldReturnIdentifierTypeWithSystemWhenSystemIsPresent() {
		String systemName = "some-system";
		String expectedTypeName = "some-type";
		String anotherTypeName = "another-type";
		
		Identifier identifierWithSystem = new Identifier();
		identifierWithSystem.setSystem(systemName);
		identifierWithSystem.setType(new CodeableConcept().setText(expectedTypeName));
		
		PatientIdentifierType expectedType = new PatientIdentifierType();
		expectedType.setName(expectedTypeName);
		
		PatientIdentifierType unexpectedType = new PatientIdentifierType();
		unexpectedType.setName(anotherTypeName);
		
		when(systemDao.getPatientIdentifierTypeByUrl(systemName)).thenReturn(expectedType);
		
		PatientIdentifierType result = patientService.getPatientIdentifierTypeByIdentifier(identifierWithSystem);
		assertNotEquals(expectedType, unexpectedType);
		assertEquals(expectedType, result);
	}
	
	@Test
	public void getPatientIdentifierTypeByIdentifier_shouldReturnNullWhenSystemAndIdentifierTypeNotPresent() {
		Identifier identifier = new Identifier();
		identifier.setType(null);
		
		PatientIdentifierType result = patientService.getPatientIdentifierTypeByIdentifier(identifier);
		assertNull(identifier.getSystem());
		assertFalse(identifier.hasType());
		assertNull(result);
	}
	
	@Test
	public void getPatientIdentifierTypeByIdentifier_shouldReturnNullWhenIdentifierTypeIsNull() {
		Identifier identifier = new Identifier();
		identifier.setType(null);
		
		PatientIdentifierType result = patientService.getPatientIdentifierTypeByIdentifier(identifier);
		assertNull(identifier.getSystem());
		assertTrue(identifier.getType().isEmpty());
		assertNull(result);
	}
	
	@Test
	public void getPatientIdentifierTypeByIdentifier_shouldReturnNullWhenIdentifierTypeTextIsNull() {
		Identifier identifier = new Identifier();
		identifier.setType(new CodeableConcept().setText(null));
		
		PatientIdentifierType result = patientService.getPatientIdentifierTypeByIdentifier(identifier);
		assertNull(identifier.getSystem());
		assertTrue(identifier.getType().isEmpty());
		assertNull(result);
	}
	
	@Test
	public void getPatientIdentifierTypeByIdentifier_shouldReturnNullWhenIdentifierTypeTextEmpty() {
		Identifier identifier = new Identifier();
		identifier.setType(new CodeableConcept().setText(Strings.EMPTY));
		
		PatientIdentifierType result = patientService.getPatientIdentifierTypeByIdentifier(identifier);
		assertNull(identifier.getSystem());
		assertTrue(identifier.getType().isEmpty());
		assertNull(result);
	}
	
	private List<IBaseResource> get(IBundleProvider results) {
		return results.getResources(0, 10);
	}
}
