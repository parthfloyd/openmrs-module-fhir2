/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.fhir2.api.translators.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.ConceptService;
import org.openmrs.api.LocationService;
import org.openmrs.api.PersonService;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;

@RunWith(MockitoJUnitRunner.class)
public class PersonAttributeTranslatorImplTest {

    private static final String PERSON_ATTRIBUTE_UUID = "12o3et5kl3-2e323-23g23-232h3y343s";

    private static final String ATTRIBUTE_TYPE_UUID = "14d4f066-15f5-102d-96e4-000c29c2a5d7";

    private static final String ATTRIBUTE_TYPE_NAME = "Contact";

    private static final String STRING_ATTRIBUTE_VALUE = "254723723456";

    private static final String BOOLEAN_ATTRIBUTE_VALUE = "true";

    private static final String CONCEPT_ATTRIBUTE_VALUE = "1000";

    private static final String LOCATION_ATTRIBUTE_VALUE = "2000";

    private static final String LOCATION_NAME = "Test Location";

    @Mock
    private LocationService locationService;

    @Mock
    private PersonService personService;

    @Mock
    private ConceptService conceptService;

    @Mock
    private ConceptTranslator conceptTranslator;

    @InjectMocks
    private PersonAttributeTranslatorImpl personAttributeTranslator;

    private PersonAttribute personAttribute;

    private PersonAttributeType personAttributeType;

    @Before
    public void setup() {
        personAttributeType = new PersonAttributeType();
        personAttributeType.setUuid(ATTRIBUTE_TYPE_UUID);
        personAttributeType.setName(ATTRIBUTE_TYPE_NAME);
        personAttributeType.setFormat("java.lang.String");

        personAttribute = new PersonAttribute();
        personAttribute.setUuid(PERSON_ATTRIBUTE_UUID);
        personAttribute.setAttributeType(personAttributeType);
        personAttribute.setValue(STRING_ATTRIBUTE_VALUE);

        when(personService.getPersonAttributeTypeByName(ATTRIBUTE_TYPE_NAME)).thenReturn(personAttributeType);
    }

    @Test
    public void shouldTranslatePersonAttributeToFhirExtension() {
        Extension result = personAttributeTranslator.toFhirResource(personAttribute);

        assertThat(result, notNullValue());
        assertThat(result.getUrl(), equalTo(FhirConstants.OPENMRS_FHIR_EXT_PERSON_ATTRIBUTE_TYPE + "#" + ATTRIBUTE_TYPE_NAME));
        assertThat(result.getValue(), notNullValue());
        assertThat(((StringType) result.getValue()).getValue(), equalTo(STRING_ATTRIBUTE_VALUE));
    }

    @Test
    public void shouldThrowExceptionWhenPersonAttributeTypeIsNull() {
        personAttribute.setAttributeType(null);

        assertThrows(IllegalArgumentException.class, () -> personAttributeTranslator.toFhirResource(personAttribute));
    }

    @Test
    public void shouldTranslateStringPersonAttributeToFhirExtension() {
        personAttributeType.setFormat("java.lang.String");
        personAttribute.setValue(STRING_ATTRIBUTE_VALUE);

        Extension result = personAttributeTranslator.toFhirResource(personAttribute);

        assertThat(result, notNullValue());
        assertThat(result.getValue() instanceof StringType, is(true));
        assertThat(((StringType) result.getValue()).getValue(), equalTo(STRING_ATTRIBUTE_VALUE));
    }

    @Test
    public void shouldTranslateBooleanPersonAttributeToFhirExtension() {
        personAttributeType.setFormat("java.lang.Boolean");
        personAttribute.setValue(BOOLEAN_ATTRIBUTE_VALUE);

        Extension result = personAttributeTranslator.toFhirResource(personAttribute);

        assertThat(result, notNullValue());
        assertThat(result.getValue() instanceof BooleanType, is(true));
        assertThat(((BooleanType) result.getValue()).getValue(), is(true));
    }

    @Test
    public void shouldTranslateLocationPersonAttributeToFhirExtension() {
        personAttributeType.setFormat("org.openmrs.Location");
        personAttribute.setValue(LOCATION_ATTRIBUTE_VALUE);

        Location location = new Location();
        location.setName(LOCATION_NAME);
        when(locationService.getLocation(LOCATION_ATTRIBUTE_VALUE)).thenReturn(location);

        Extension result = personAttributeTranslator.toFhirResource(personAttribute);

        assertThat(result, notNullValue());
        assertThat(result.getValue() instanceof Reference, is(true));
        Reference reference = (Reference) result.getValue();
        assertThat(reference.getReference(), equalTo("Location/" + LOCATION_ATTRIBUTE_VALUE));
        assertThat(reference.getDisplay(), equalTo(LOCATION_NAME));
        assertThat(reference.getType(), equalTo("Location"));
    }

    @Test
    public void shouldTranslateConceptPersonAttributeToFhirExtension() {
        personAttributeType.setFormat("org.openmrs.Concept");
        personAttribute.setValue(CONCEPT_ATTRIBUTE_VALUE);

        Concept concept = new Concept();
        concept.setConceptId(Integer.parseInt(CONCEPT_ATTRIBUTE_VALUE));
        when(conceptService.getConcept(CONCEPT_ATTRIBUTE_VALUE)).thenReturn(concept);

        CodeableConcept codeableConcept = new CodeableConcept();
        when(conceptTranslator.toFhirResource(concept)).thenReturn(codeableConcept);

        Extension result = personAttributeTranslator.toFhirResource(personAttribute);

        assertThat(result, notNullValue());
        assertThat(result.getValue() instanceof CodeableConcept, is(true));
        verify(conceptTranslator).toFhirResource(concept);
    }


    @Test
    public void shouldTranslateFhirExtensionToPersonAttribute() {
        Extension extension = new Extension();
        extension.setUrl(FhirConstants.OPENMRS_FHIR_EXT_PERSON_ATTRIBUTE);

        Extension valueExtension = new Extension();
        valueExtension.setUrl(FhirConstants.OPENMRS_FHIR_EXT_PERSON_ATTRIBUTE_TYPE + "#" + ATTRIBUTE_TYPE_NAME);
        valueExtension.setValue(new StringType(STRING_ATTRIBUTE_VALUE));

        extension.addExtension(valueExtension);

        PersonAttribute result = personAttributeTranslator.toOpenmrsType(extension);

        assertThat(result, notNullValue());
        assertThat(result.getAttributeType(), equalTo(personAttributeType));
        assertThat(result.getValue(), equalTo(STRING_ATTRIBUTE_VALUE));
    }

    @Test
    public void shouldReturnNullWhenExtensionIsInvalid() {
        Extension extension = new Extension();
        extension.setUrl("invalid-url");

        PersonAttribute result = personAttributeTranslator.toOpenmrsType(extension);

        assertThat(result, equalTo(null));
    }

    @Test
    public void shouldTranslateStringTypeExtensionToPersonAttribute() {
        Extension extension = new Extension();
        extension.setUrl(FhirConstants.OPENMRS_FHIR_EXT_PERSON_ATTRIBUTE);

        Extension valueExtension = new Extension();
        valueExtension.setUrl(FhirConstants.OPENMRS_FHIR_EXT_PERSON_ATTRIBUTE_TYPE + "#" + ATTRIBUTE_TYPE_NAME);
        valueExtension.setValue(new StringType(STRING_ATTRIBUTE_VALUE));

        extension.addExtension(valueExtension);

        PersonAttribute result = personAttributeTranslator.toOpenmrsType(extension);

        assertThat(result, notNullValue());
        assertThat(result.getAttributeType(), equalTo(personAttributeType));
        assertThat(result.getValue(), equalTo(STRING_ATTRIBUTE_VALUE));
    }

    @Test
    public void shouldTranslateBooleanTypeExtensionToPersonAttribute() {
        Extension extension = new Extension();
        extension.setUrl(FhirConstants.OPENMRS_FHIR_EXT_PERSON_ATTRIBUTE);

        Extension valueExtension = new Extension();
        valueExtension.setUrl(FhirConstants.OPENMRS_FHIR_EXT_PERSON_ATTRIBUTE_TYPE + "#" + ATTRIBUTE_TYPE_NAME);
        valueExtension.setValue(new BooleanType(true));

        extension.addExtension(valueExtension);

        PersonAttribute result = personAttributeTranslator.toOpenmrsType(extension);

        assertThat(result, notNullValue());
        assertThat(result.getAttributeType(), equalTo(personAttributeType));
        assertThat(result.getValue(), equalTo("true"));
    }

    @Test
    public void shouldTranslateReferenceTypeExtensionToPersonAttribute() {
        Extension extension = new Extension();
        extension.setUrl(FhirConstants.OPENMRS_FHIR_EXT_PERSON_ATTRIBUTE);

        Extension valueExtension = new Extension();
        valueExtension.setUrl(FhirConstants.OPENMRS_FHIR_EXT_PERSON_ATTRIBUTE_TYPE + "#" + ATTRIBUTE_TYPE_NAME);

        Reference reference = new Reference();
        reference.setReference("Location/" + LOCATION_ATTRIBUTE_VALUE);
        valueExtension.setValue(reference);

        extension.addExtension(valueExtension);

        PersonAttribute result = personAttributeTranslator.toOpenmrsType(extension);

        assertThat(result, notNullValue());
        assertThat(result.getAttributeType(), equalTo(personAttributeType));
        assertThat(result.getValue(), equalTo(LOCATION_ATTRIBUTE_VALUE));
    }

    @Test
    public void shouldTranslateCodeableConceptTypeExtensionToPersonAttribute() {
        Extension extension = new Extension();
        extension.setUrl(FhirConstants.OPENMRS_FHIR_EXT_PERSON_ATTRIBUTE);

        Extension valueExtension = new Extension();
        valueExtension.setUrl(FhirConstants.OPENMRS_FHIR_EXT_PERSON_ATTRIBUTE_TYPE + "#" + ATTRIBUTE_TYPE_NAME);

        CodeableConcept codeableConcept = new CodeableConcept();
        codeableConcept.setText("Test");
        valueExtension.setValue(codeableConcept);

        extension.addExtension(valueExtension);

        Concept concept = new Concept();
        concept.setConceptId(Integer.parseInt(CONCEPT_ATTRIBUTE_VALUE));
        when(conceptTranslator.toOpenmrsType(codeableConcept)).thenReturn(concept);

        PersonAttribute result = personAttributeTranslator.toOpenmrsType(extension);

        assertThat(result, notNullValue());
        assertThat(result.getAttributeType(), equalTo(personAttributeType));
        assertThat(result.getValue(), equalTo(CONCEPT_ATTRIBUTE_VALUE));
    }

    @Test
    public void shouldThrowExceptionWhenPersonAttributeTypeNotFound() {
        when(personService.getPersonAttributeTypeByName(anyString())).thenReturn(null);

        Extension extension = new Extension();
        extension.setUrl(FhirConstants.OPENMRS_FHIR_EXT_PERSON_ATTRIBUTE);

        Extension valueExtension = new Extension();
        valueExtension.setUrl(FhirConstants.OPENMRS_FHIR_EXT_PERSON_ATTRIBUTE_TYPE + "#NonExistentType");
        valueExtension.setValue(new StringType(STRING_ATTRIBUTE_VALUE));

        extension.addExtension(valueExtension);

        assertThrows(IllegalStateException.class, () -> personAttributeTranslator.toOpenmrsType(extension));
    }

}