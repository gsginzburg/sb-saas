/*
 * Copyright 2026 Gary Ginzburg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsginzburg.shared.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Generic base class for converting between a DTO type {@code D} and a domain
 * model type {@code M}.
 *
 * <p>For each property that exists (by name) in both types and is both readable
 * and writable, the converter will apply the first matching coercion rule:
 * <ol>
 *   <li>Direct copy when the target type is assignment-compatible with the source.</li>
 *   <li>{@link UUID}↔{@link String} when the property name ends with {@code "id"}
 *       (case-insensitive).</li>
 *   <li>{@link Enum}→{@link String} via {@link Enum#name()}.</li>
 *   <li>{@link String}→{@link Enum} via {@link Enum#valueOf(Class, String)}.</li>
 * </ol>
 * Properties declared on {@link Object} are excluded. Any mapping that cannot
 * be resolved automatically is left to the subclass via
 * {@link #afterToDto(Object, Object)} and {@link #afterToModel(Object, Object)}.
 *
 * <p><strong>Jakarta Bean Validation annotations are completely transparent to this
 * converter.</strong> Property discovery relies solely on getter/setter method pairs
 * returned by {@link java.beans.Introspector#getBeanInfo(Class, Class)}; field-level
 * annotations ({@code @NotBlank}, {@code @NotNull}, {@code @Email}, etc.) are never
 * read or evaluated. Validation is exclusively the responsibility of the Spring MVC
 * layer ({@code @Valid} on {@code @RequestBody} parameters).
 *
 * @param <D> the DTO type
 * @param <M> the domain model type
 */
public abstract class DtoConverter<D, M> {

    private final Class<D> dtoClass;
    private final Class<M> modelClass;
    private final Map<String, PropertyDescriptor> dtoProps;
    private final Map<String, PropertyDescriptor> modelProps;

    protected DtoConverter(Class<D> dtoClass, Class<M> modelClass) {
        this.dtoClass = dtoClass;
        this.modelClass = modelClass;
        this.dtoProps = readWriteProperties(dtoClass);
        this.modelProps = readWriteProperties(modelClass);
    }

    /**
     * Convert a domain model instance to a DTO.
     * Automatic property mapping runs first; {@link #afterToDto} runs after.
     */
    public D toDto(M model) {
        try {
            D dto = dtoClass.getDeclaredConstructor().newInstance();
            for (Map.Entry<String, PropertyDescriptor> entry : modelProps.entrySet()) {
                PropertyDescriptor dtoProp = dtoProps.get(entry.getKey());
                if (dtoProp == null) continue;
                copyProperty(entry.getKey(), model, entry.getValue(), dto, dtoProp);
            }
            afterToDto(model, dto);
            return dto;
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException("toDto failed for " + modelClass.getSimpleName(), ex);
        }
    }

    /**
     * Convert a DTO instance to a domain model.
     * Automatic property mapping runs first; {@link #afterToModel} runs after.
     */
    public M toModel(D dto) {
        try {
            M model = modelClass.getDeclaredConstructor().newInstance();
            for (Map.Entry<String, PropertyDescriptor> entry : dtoProps.entrySet()) {
                PropertyDescriptor modelProp = modelProps.get(entry.getKey());
                if (modelProp == null) continue;
                copyProperty(entry.getKey(), dto, entry.getValue(), model, modelProp);
            }
            afterToModel(dto, model);
            return model;
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException("toModel failed for " + dtoClass.getSimpleName(), ex);
        }
    }

    /**
     * Hook called after automatic property copying in {@link #toDto}.
     * Override to handle properties the generic converter cannot resolve
     * (e.g. enum-to-String, nested associations, computed fields).
     */
    protected void afterToDto(M model, D dto) {}

    /**
     * Hook called after automatic property copying in {@link #toModel}.
     * Override to handle properties the generic converter cannot resolve.
     */
    protected void afterToModel(D dto, M model) {}

    // ── internals ────────────────────────────────────────────────────────────

    private static Map<String, PropertyDescriptor> readWriteProperties(Class<?> cls) {
        try {
            // Object.class as stopClass excludes all properties declared on Object itself
            BeanInfo info = Introspector.getBeanInfo(cls, Object.class);
            return Arrays.stream(info.getPropertyDescriptors())
                    .filter(pd -> pd.getReadMethod() != null && pd.getWriteMethod() != null)
                    .collect(Collectors.toMap(PropertyDescriptor::getName, Function.identity()));
        } catch (IntrospectionException ex) {
            throw new IllegalArgumentException("Cannot introspect " + cls.getName(), ex);
        }
    }

    private static void copyProperty(String name,
                                     Object src, PropertyDescriptor srcProp,
                                     Object dst, PropertyDescriptor dstProp)
            throws IllegalAccessException, InvocationTargetException {
        Object value = srcProp.getReadMethod().invoke(src);
        if (value == null) return;

        Object coerced = coerce(name, value, srcProp.getPropertyType(), dstProp.getPropertyType());
        if (coerced != null) {
            dstProp.getWriteMethod().invoke(dst, coerced);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object coerce(String name, Object value, Class<?> srcType, Class<?> dstType) {
        if (dstType.isAssignableFrom(srcType)) {
            return value;
        }
        if (name.toLowerCase().endsWith("id")) {
            if (srcType == UUID.class && dstType == String.class) return value.toString();
            if (srcType == String.class && dstType == UUID.class) return UUID.fromString((String) value);
        }
        if (srcType.isEnum() && dstType == String.class) {
            return ((Enum<?>) value).name();
        }
        if (srcType == String.class && dstType.isEnum()) {
            return Enum.valueOf((Class<Enum>) dstType, (String) value);
        }
        return null;
    }
}
