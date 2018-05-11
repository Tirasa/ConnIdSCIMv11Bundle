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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class SCIMAttribute {

    @JsonProperty
    private String type;

    @JsonProperty
    private String name;

    @JsonProperty
    private Boolean multiValued;

    @JsonProperty
    private String multiValuedAttributeChildName;

    @JsonProperty
    private String description;

    @JsonProperty
    private String schema;

    @JsonProperty
    private Boolean readOnly;

    @JsonProperty
    private Boolean required;

    @JsonProperty
    private Boolean caseExact;

    @JsonProperty
    private final List<String> canonicalValues = new ArrayList<>();

    @JsonProperty
    private final List<SCIMAttribute> subAttributes = new ArrayList<>();

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Boolean getMultiValued() {
        return multiValued;
    }

    public void setMultiValued(final Boolean multiValued) {
        this.multiValued = multiValued;
    }

    public String getMultiValuedAttributeChildName() {
        return multiValuedAttributeChildName;
    }

    public void setMultiValuedAttributeChildName(final String multiValuedAttributeChildName) {
        this.multiValuedAttributeChildName = multiValuedAttributeChildName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(final String schema) {
        this.schema = schema;
    }

    public Boolean getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(final Boolean readOnly) {
        this.readOnly = readOnly;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(final Boolean required) {
        this.required = required;
    }

    public Boolean getCaseExact() {
        return caseExact;
    }

    public void setCaseExact(final Boolean caseExact) {
        this.caseExact = caseExact;
    }

    public List<String> getCanonicalValues() {
        return canonicalValues;
    }

    public List<SCIMAttribute> getSubAttributes() {
        return subAttributes;
    }

    @Override
    public String toString() {
        return "SCIMAttribute{" + "type=" + type + ", name=" + name + ", multiValued=" + multiValued
                + ", multiValuedAttributeChildName=" + multiValuedAttributeChildName + ", description=" + description
                + ", schema=" + schema + ", readOnly=" + readOnly + ", required=" + required + ", caseExact="
                + caseExact + ", canonicalValues=" + canonicalValues + ", subAttributes=" + subAttributes + '}';
    }

}
