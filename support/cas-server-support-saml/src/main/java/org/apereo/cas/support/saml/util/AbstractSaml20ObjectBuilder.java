package org.apereo.cas.support.saml.util;


import org.apache.commons.lang3.StringUtils;
import org.apereo.cas.util.EncodingUtils;
import org.apereo.cas.authentication.principal.WebApplicationService;
import org.apereo.cas.support.saml.authentication.principal.SamlService;
import org.apereo.cas.util.CompressionUtils;
import org.apereo.cas.util.DateTimeUtils;

import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.AttributeValue;
import org.opensaml.saml.saml2.core.Audience;
import org.opensaml.saml.saml2.core.AudienceRestriction;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Statement;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.StatusMessage;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;

import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * This is {@link AbstractSaml20ObjectBuilder}.
 * to build saml2 objects.
 * @author Misagh Moayyed mmoayyed@unicon.net
 * @since 4.1
 */
public abstract class AbstractSaml20ObjectBuilder extends AbstractSamlObjectBuilder {
    private static final int HEX_HIGH_BITS_BITWISE_FLAG = 0x0f;
    private static final long serialVersionUID = -4325127376598205277L;

    /**
     * Gets name id.
     *
     * @param nameIdFormat the name id format
     * @param nameIdValue the name id value
     * @return the name iD
     */
    protected NameID getNameID(final String nameIdFormat, final String nameIdValue) {
        final NameID nameId = newSamlObject(NameID.class);
        nameId.setFormat(nameIdFormat);
        nameId.setValue(nameIdValue);
        return nameId;
    }

    /**
     * Create a new SAML response object.
     * @param id the id
     * @param issueInstant the issue instant
     * @param recipient the recipient
     * @param service the service
     * @return the response
     */
    public Response newResponse(final String id, final ZonedDateTime issueInstant,
                                final String recipient, final WebApplicationService service) {

        final Response samlResponse = newSamlObject(Response.class);
        samlResponse.setID(id);
        samlResponse.setIssueInstant(DateTimeUtils.dateTimeOf(issueInstant));
        samlResponse.setVersion(SAMLVersion.VERSION_20);
        samlResponse.setInResponseTo(recipient);

        if (service instanceof SamlService) {
            final SamlService samlService = (SamlService) service;

            final String requestId = samlService.getRequestID();
            if (StringUtils.isNotBlank(requestId)) {
                samlResponse.setInResponseTo(requestId);
            }
        }
        return samlResponse;
    }

    /**
     * Create a new SAML status object.
     *
     * @param codeValue the code value
     * @param statusMessage the status message
     * @return the status
     */
    public Status newStatus(final String codeValue, final String statusMessage) {
        final Status status = newSamlObject(Status.class);
        final StatusCode code = newSamlObject(StatusCode.class);
        code.setValue(codeValue);
        status.setStatusCode(code);
        if (StringUtils.isNotBlank(statusMessage)) {
            final StatusMessage message = newSamlObject(StatusMessage.class);
            message.setMessage(statusMessage);
            status.setStatusMessage(message);
        }
        return status;
    }

    /**
     * Create a new SAML1 response object.
     *
     * @param authnStatement the authn statement
     * @param issuer the issuer
     * @param issuedAt the issued at
     * @param id the id
     * @return the assertion
     */
    public Assertion newAssertion(final AuthnStatement authnStatement, final String issuer,
                                  final ZonedDateTime issuedAt, final String id) {
        final List<Statement> list = new ArrayList<>();
        list.add(authnStatement);
        return newAssertion(list, issuer, issuedAt, id);
    }

    /**
     * Create a new SAML1 response object.
     *
     * @param authnStatement the authn statement
     * @param issuer the issuer
     * @param issuedAt the issued at
     * @param id the id
     * @return the assertion
     */
    public Assertion newAssertion(final List<Statement> authnStatement, final String issuer,
                                  final ZonedDateTime issuedAt, final String id) {
        final Assertion assertion = newSamlObject(Assertion.class);
        assertion.setID(id);
        assertion.setIssueInstant(DateTimeUtils.dateTimeOf(issuedAt));
        assertion.setIssuer(newIssuer(issuer));
        assertion.getStatements().addAll(authnStatement);
        return assertion;
    }

    /**
     * New issuer.
     *
     * @param issuerValue the issuer
     * @return the issuer
     */
    public Issuer newIssuer(final String issuerValue) {
        final Issuer issuer = newSamlObject(Issuer.class);
        issuer.setValue(issuerValue);
        return issuer;
    }

    /**
     * New attribute statement.
     *
     * @param attributes      the attributes
     * @param setFriendlyName the set friendly name
     * @return the attribute statement
     */
    public AttributeStatement newAttributeStatement(final Map<String, Object> attributes, final boolean setFriendlyName) {

        final AttributeStatement attrStatement = newSamlObject(AttributeStatement.class);
        for (final Map.Entry<String, Object> e : attributes.entrySet()) {
            if (e.getValue() instanceof Collection<?> && ((Collection<?>) e.getValue()).isEmpty()) {
                logger.info("Skipping attribute {} because it does not have any values.", e.getKey());
                continue;
            }
            final Attribute attribute = newSamlObject(Attribute.class);
            attribute.setName(e.getKey());
            
            if (setFriendlyName) {
                attribute.setFriendlyName(e.getKey());
            } 
            
            if (e.getValue() instanceof Collection<?>) {
                final Collection<?> c = (Collection<?>) e.getValue();
                for (final Object value : c) {
                    attribute.getAttributeValues().add(newAttributeValue(value, AttributeValue.DEFAULT_ELEMENT_NAME));
                }
            } else {
                attribute.getAttributeValues().add(newAttributeValue(e.getValue(), AttributeValue.DEFAULT_ELEMENT_NAME));
            }
            attrStatement.getAttributes().add(attribute);
        }

        return attrStatement;
    }

    /**
     * New authn statement.
     *
     * @param contextClassRef the context class ref such as {@link AuthnContext#PASSWORD_AUTHN_CTX}
     * @param authnInstant the authn instant
     * @return the authn statement
     */
    public AuthnStatement newAuthnStatement(final String contextClassRef, final ZonedDateTime authnInstant) {
        final AuthnStatement stmt = newSamlObject(AuthnStatement.class);
        final AuthnContext ctx = newSamlObject(AuthnContext.class);

        final AuthnContextClassRef classRef = newSamlObject(AuthnContextClassRef.class);
        classRef.setAuthnContextClassRef(contextClassRef);

        ctx.setAuthnContextClassRef(classRef);
        stmt.setAuthnContext(ctx);
        stmt.setAuthnInstant(DateTimeUtils.dateTimeOf(authnInstant));

        return stmt;
    }

    /**
     * New conditions element.
     *
     * @param notBefore the not before
     * @param notOnOrAfter the not on or after
     * @param audienceUri the service id
     * @return the conditions
     */
    public Conditions newConditions(final ZonedDateTime notBefore, final ZonedDateTime notOnOrAfter, final String audienceUri) {
        final Conditions conditions = newSamlObject(Conditions.class);
        conditions.setNotBefore(DateTimeUtils.dateTimeOf(notBefore));
        conditions.setNotOnOrAfter(DateTimeUtils.dateTimeOf(notOnOrAfter));

        final AudienceRestriction audienceRestriction = newSamlObject(AudienceRestriction.class);
        final Audience audience = newSamlObject(Audience.class);
        audience.setAudienceURI(audienceUri);
        audienceRestriction.getAudiences().add(audience);
        conditions.getAudienceRestrictions().add(audienceRestriction);
        return conditions;
    }

    /**
     * New subject element.
     *
     * @param nameIdFormat the name id format
     * @param nameIdValue the name id value
     * @param recipient the recipient
     * @param notOnOrAfter the not on or after
     * @param inResponseTo the in response to
     * @return the subject
     */
    public Subject newSubject(final String nameIdFormat, final String nameIdValue,
                              final String recipient, final ZonedDateTime notOnOrAfter,
                              final String inResponseTo) {

        final SubjectConfirmation confirmation = newSamlObject(SubjectConfirmation.class);
        confirmation.setMethod(SubjectConfirmation.METHOD_BEARER);

        final SubjectConfirmationData data = newSamlObject(SubjectConfirmationData.class);
        data.setRecipient(recipient);
        data.setNotOnOrAfter(DateTimeUtils.dateTimeOf(notOnOrAfter));
        data.setInResponseTo(inResponseTo);

        confirmation.setSubjectConfirmationData(data);

        final Subject subject = newSamlObject(Subject.class);
        subject.setNameID(getNameID(nameIdFormat, nameIdValue));
        subject.getSubjectConfirmations().add(confirmation);
        return subject;
    }

    @Override
    public String generateSecureRandomId() {
        final SecureRandom generator = new SecureRandom();
        final char[] charMappings = {
                'a', 'b', 'c', 'd', 'e', 'f', 'g',
                'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o',
                'p'};

        final int charsLength = 40;
        final int generatorBytesLength = 20;
        final int shiftLength = 4;

        // 160 bits
        final byte[] bytes = new byte[generatorBytesLength];
        generator.nextBytes(bytes);

        final char[] chars = new char[charsLength];
        for (int i = 0; i < bytes.length; i++) {
            final int left = bytes[i] >> shiftLength & HEX_HIGH_BITS_BITWISE_FLAG;
            final int right = bytes[i] & HEX_HIGH_BITS_BITWISE_FLAG;
            chars[i * 2] = charMappings[left];
            chars[i * 2 + 1] = charMappings[right];
        }
        return String.valueOf(chars);
    }

    /**
     * Decode authn request xml.
     *
     * @param encodedRequestXmlString the encoded request xml string
     * @return the request
     */
    public String decodeSamlAuthnRequest(final String encodedRequestXmlString) {
        if (StringUtils.isEmpty(encodedRequestXmlString)) {
            return null;
        }

        final byte[] decodedBytes = EncodingUtils.decodeBase64(encodedRequestXmlString);
        if (decodedBytes == null) {
            return null;
        }

        final String inflated = CompressionUtils.inflate(decodedBytes);
        if (!StringUtils.isEmpty(inflated)) {
            return inflated;
        }

        return CompressionUtils.decodeByteArrayToString(decodedBytes);
    }
}
