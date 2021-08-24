/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ConfigElementLoader.java 4076 2019-06-11 17:09:30Z SFB $
 */

package org.rvpf.document.loader;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.Entity;
import org.rvpf.base.exception.ValidationException;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.util.container.KeyedValues;
import org.rvpf.config.entity.ClassDefEntity;
import org.rvpf.config.entity.ClassLibEntity;
import org.rvpf.config.entity.ClassLibEntity.Undefined;
import org.rvpf.config.entity.ValidatorDefEntity;
import org.rvpf.service.ServiceMessages;

/**
 * Config document element loader.
 */
public abstract class ConfigElementLoader
    extends DocumentElementLoader
{
    /**
     * Gets a class definition entity.
     *
     * @param key The entity key.
     *
     * @return The class definition entity.
     *
     * @throws ValidationException When appropriate.
     */
    @Nonnull
    @CheckReturnValue
    final ClassDefEntity getClassDefEntity(
            @Nonnull final String key)
        throws ValidationException
    {
        return (ClassDefEntity) getEntity(key, ClassDefEntity.ENTITY_PREFIX);
    }

    /**
     * Gets a class library entity.
     *
     * @param key The entity key.
     *
     * @return The class library entity.
     *
     * @throws ValidationException When appropriate.
     */
    @Nonnull
    @CheckReturnValue
    final ClassLibEntity getClassLibEntity(
            @Nonnull final String key)
        throws ValidationException
    {
        final Optional<? extends Entity> classLibEntity = getEntity(
            key,
            ClassLibEntity.ENTITY_PREFIX,
            true);

        if (!classLibEntity.isPresent()) {
            getLogger()
                .trace(
                    ServiceMessages.ENTITY_UNKNOWN,
                    ClassLibEntity.ELEMENT_NAME,
                    key);
        }

        return classLibEntity
            .isPresent()? (ClassLibEntity) classLibEntity
                .get(): Undefined.newBuilder().setKey(key).build();
    }

    /**
     * Puts the values for this entry.
     *
     * @param name The name to use when adding to the target.
     * @param element The XML element.
     * @param context Where to get values.
     * @param validator An optional validator.
     * @param target Where to put values.
     *
     * @throws ValidationException When appropriate.
     */
    final void putValues(
            @Nonnull final String name,
            @Nonnull final DocumentElement element,
            @Nonnull final KeyedGroups context,
            @Nonnull final Optional<? extends ValidatorDefEntity> validator,
            @Nonnull final KeyedValues target)
        throws ValidationException
    {
        final boolean overrides = element
            .getAttributeValue(OVERRIDES_ATTRIBUTE, false);
        final boolean hidden = element
            .getAttributeValue(HIDDEN_ATTRIBUTE, false);
        final Optional<String> required = element
            .getAttributeValue(REQUIRED_ATTRIBUTE, Optional.empty());
        final Optional<String> property = element
            .getAttributeValue(PROPERTY_ATTRIBUTE, Optional.empty());
        final Optional<String> env = element
            .getAttributeValue(ENV_ATTRIBUTE, Optional.empty());
        final Optional<String> eq = element
            .getAttributeValue(EQ_ATTRIBUTE, Optional.empty());
        Optional<String> classDef = element
            .getAttributeValue(CLASS_DEF_REFERENCE, Optional.empty());
        Object[] values = null;

        // Hides values when requested for a non secure destination.

        if ((validator.isPresent() && validator.get().isHidden()) || hidden) {
            target.setValuesHidden(name);
        }

        // Overrides forgets any previous value.

        if (overrides) {
            target.removeValue(name);
        }

        // Tries to get values from a specified property.

        if (required.isPresent()) {
            if (property.isPresent()) {
                throw new ValidationException(
                    ServiceMessages.CONFLICTING_ATTRIBUTES,
                    PROPERTY_ATTRIBUTE,
                    REQUIRED_ATTRIBUTE);
            }

            if (env.isPresent()) {
                throw new ValidationException(
                    ServiceMessages.CONFLICTING_ATTRIBUTES,
                    ENV_ATTRIBUTE,
                    REQUIRED_ATTRIBUTE);
            }

            if (eq.isPresent()) {
                throw new ValidationException(
                    ServiceMessages.CONFLICTING_ATTRIBUTES,
                    EQ_ATTRIBUTE,
                    REQUIRED_ATTRIBUTE);
            }

            if (classDef.isPresent()) {
                throw new ValidationException(
                    ServiceMessages.CONFLICTING_ATTRIBUTES,
                    CLASS_DEF_REFERENCE,
                    REQUIRED_ATTRIBUTE);
            }

            if ((element.getAttributeValue(
                    VALUE_ATTRIBUTE,
                    Optional.empty()).isPresent())
                    || (element.getText().trim().length() > 0)
                    || !element.getChildren(VALUE_ELEMENT).isEmpty()) {
                throw new ValidationException(
                    ServiceMessages.CONFLICTING_ATTRIBUTES,
                    VALUE_ATTRIBUTE,
                    REQUIRED_ATTRIBUTE);
            }

            values = context.getValues(required.get());

            if (values.length == 0) {
                throw new ValidationException(
                    BaseMessages.MISSING_PROPERTY,
                    required.get());
            }
        }

        if (property.isPresent()) {
            values = context.getValues(property.get());

            if (values.length == 0) {
                values = null;
            }
        }

        // Tries to get a value from an environment variable.

        if ((values == null) && env.isPresent()) {
            final String value = System.getenv(env.get());

            if (value != null) {
                values = new String[] {value};
            }
        }

        // Handles the 'eq' protection.

        if (eq.isPresent()) {
            if (values != null) {
                if ((values.length != 1) || !(values[0] instanceof String)) {
                    throw new ValidationException(
                        ServiceMessages.PROPERTY_EQ,
                        name);
                }

                if (((String) values[0])
                    .equalsIgnoreCase(
                        element
                            .getAttributeValue(VALUE_ATTRIBUTE, Optional.of(""))
                            .orElse(null)) != element.getAttributeValue(
                                    EQ_ATTRIBUTE,
                                            true)) {
                    values = _EMPTY_STRING_ARRAY;    // Cancels assignment.
                }
            } else {
                values = _EMPTY_STRING_ARRAY;    // Avoids defaults assignment.
            }
        }

        // Sets the values.

        if (values != null) {    // Accepts what we got.
            for (final Object value: values) {
                target.add(name, value);
            }
        } else if (classDef.isPresent()) {    // Gets the referenced ClassDef.
            if (element
                .getAttributeValue(VALUE_ATTRIBUTE, Optional.empty())
                .isPresent()) {
                throw new ValidationException(ServiceMessages.CLASS_DEF_VALUE);
            }

            target.add(name, getClassDefEntity(classDef.get()));
        } else {    // Gets the value attribute, text and elements.
            String value = element
                .getAttributeValue(VALUE_ATTRIBUTE, Optional.empty())
                .orElse(null);

            if (value != null) {
                target.add(name, value);
            }

            value = element.getText().trim();

            if (value.length() > 0) {
                target.add(name, value);
            }

            for (final DocumentElement valueElement:
                    element.getChildren(VALUE_ELEMENT)) {
                if (valueElement.isEnabled()) {
                    classDef = valueElement
                        .getAttributeValue(
                            CLASS_DEF_REFERENCE,
                            Optional.empty());

                    if (classDef.isPresent()) {
                        target.add(name, getClassDefEntity(classDef.get()));
                    }

                    value = valueElement
                        .getAttributeValue(VALUE_ATTRIBUTE, Optional.of(""))
                        .orElse(null);

                    if (value.length() > 0) {
                        target.add(name, value);
                    }

                    value = valueElement.getText().trim();

                    if (value.length() > 0) {
                        target.add(name, value);
                    }
                }
            }

            if (target.containsValueKey(name)) {    // Validates multiple
                // values.
                if (validator.isPresent()
                        && !validator.get().isMultiple()
                        && target.isMultiple(name)) {
                    throw new ValidationException(
                        ServiceMessages.MULTIPLE_VALUES,
                        validator.get().getTarget(),
                        name);
                }
            } else if (!(property.isPresent() || env.isPresent())) {
                target.add(name, "");
            }
        }
    }

    /** Alias element. */
    public static final String ALIAS_ELEMENT = "alias";

    /** Allow attribute. */
    public static final String ALLOW_ATTRIBUTE = "allow";

    /** Cached attribute. */
    public static final String CACHED_ATTRIBUTE = "cached";

    /** ClassDef entity element. */
    public static final String CLASS_DEF_ENTITY = "ClassDef";

    /** ClassDef reference. */
    public static final String CLASS_DEF_REFERENCE = "classDef";

    /** ClassLib entity element. */
    public static final String CLASS_LIB_ENTITY = "ClassLib";

    /** ClassLib reference. */
    public static final String CLASS_LIB_REFERENCE = "classLib";

    /** Decrypt attribute. */
    public static final String DECRYPT_ATTRIBUTE = "decrypt";

    /** Decrypt key element. */
    public static final String DECRYPT_KEY_ELEMENT = "decryptKey";

    /** Def attribute. */
    public static final String DEF_ATTRIBUTE = "def";

    /** Deny attribute. */
    public static final String DENY_ATTRIBUTE = "deny";

    /** Env attribute. */
    public static final String ENV_ATTRIBUTE = "env";

    /** Eq attribute. */
    public static final String EQ_ATTRIBUTE = "eq";

    /** Extends attribute. */
    public static final String EXTENDS_ATTRIBUTE = "extends";

    /** From attribute. */
    public static final String FROM_ATTRIBUTE = "from";

    /** Hidden attribute. */
    public static final String HIDDEN_ATTRIBUTE = "hidden";

    /** Implements element. */
    public static final String IMPLEMENTS_ELEMENT = "implements";

    /** Location attribute. */
    public static final String LOCATION_ATTRIBUTE = "location";

    /** Location element. */
    public static final String LOCATION_ELEMENT = "location";

    /** Member attribute. */
    public static final String MEMBER_ATTRIBUTE = "member";

    /** Multiple allowed attribute. */
    public static final String MULTIPLE_ATTRIBUTE = "multiple";

    /** Optional attribute. */
    public static final String OPTIONAL_ATTRIBUTE = "optional";

    /** Overrides attribute. */
    public static final String OVERRIDES_ATTRIBUTE = "overrides";

    /** Package attribute. */
    public static final String PACKAGE_ATTRIBUTE = "package";

    /** PropertiesDef entity element. */
    public static final String PROPERTIES_DEF_ENTITY = "PropertiesDef";

    /** Properties element. */
    public static final String PROPERTIES_ELEMENT = "properties";

    /** Property attribute. */
    public static final String PROPERTY_ATTRIBUTE = "property";

    /** PropertyDef entity element. */
    public static final String PROPERTY_DEF_ENTITY = "PropertyDef";

    /** Property element. */
    public static final String PROPERTY_ELEMENT = "property";

    /** Required attribute. */
    public static final String REQUIRED_ATTRIBUTE = "required";

    /** Role attribute. */
    public static final String ROLE_ATTRIBUTE = "role";

    /** Security attribute. */
    public static final String SECURITY_ATTRIBUTE = "security";

    /** Service element. */
    public static final String SERVICE_ELEMENT = "service";

    /** Stamp attribute. */
    public static final String STAMP_ATTRIBUTE = "stamp";

    /** Supersede attribute. */
    public static final String SUPERSEDE_ATTRIBUTE = "supersede";

    /** System attribute. */
    public static final String SYSTEM_ATTRIBUTE = "system";

    /** Validated attribute. */
    public static final String VALIDATED_ATTRIBUTE = "validated";

    /** Value attribute. */
    public static final String VALUE_ATTRIBUTE = "value";

    /** Value element. */
    public static final String VALUE_ELEMENT = "value";

    /** Verify attribute. */
    public static final String VERIFY_ATTRIBUTE = "verify";

    /** Verify key element. */
    public static final String VERIFY_KEY_ELEMENT = "verifyKey";
    private static final String[] _EMPTY_STRING_ARRAY = new String[0];
}

/* This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * version 2.1 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA
 */
