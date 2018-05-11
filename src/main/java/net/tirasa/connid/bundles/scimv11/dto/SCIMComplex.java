/**
 * Copyright (C) 2018 ConnId (connid-dev@googlegroups.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.tirasa.connid.bundles.scimv11.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import net.tirasa.connid.bundles.scimv11.utils.SCIMv11Attributes;
import org.identityconnectors.framework.common.objects.Attribute;

public class SCIMComplex<E extends Enum<?>> {

    @JsonProperty
    private String value;

    @JsonProperty(access = Access.READ_ONLY)
    private String display;

    @JsonProperty
    private E type;

    @JsonProperty
    private boolean primary;

    @JsonProperty
    private String operation;

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public String getDisplay() {
        return display;
    }

    public E getType() {
        return type;
    }

    public String getOperation() {
        return operation;
    }

    public void setDisplay(final String display) {
        this.display = display;
    }

    public void setType(final E type) {
        this.type = type;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(final boolean primary) {
        this.primary = primary;
    }

    public void setOperation(final String operation) {
        this.operation = operation;
    }

    public Set<Attribute> toAttributes(final String id) throws IllegalArgumentException, IllegalAccessException {
        Set<Attribute> attrs = new HashSet<>();
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.getAnnotation(JsonIgnore.class) == null) {
                field.setAccessible(true);
                attrs.add(SCIMv11Attributes.doBuildAttributeFromClassField(
                        field.get(this),
                        id.concat(".")
                                .concat(type.name())
                                .concat(".")
                                .concat(field.getName()),
                        field.getType()).build());
            }
        }
        return attrs;
    }

    @Override
    public String toString() {
        return "SCIMComplex{" + "value=" + value + ", display=" + display + ", type=" + type + ", primary=" + primary
                + ", operation=" + operation + '}';
    }

}
