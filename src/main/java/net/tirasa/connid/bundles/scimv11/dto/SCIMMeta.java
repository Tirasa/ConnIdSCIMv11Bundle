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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.tirasa.connid.bundles.scimv11.utils.SCIMv11Attributes;
import org.identityconnectors.framework.common.objects.Attribute;

public class SCIMMeta {

    @JsonProperty
    private String created;

    @JsonProperty
    private String lastModified;

    @JsonProperty
    private String location;

    @JsonProperty
    private String version;

    @JsonProperty
    private List<String> attributes = new ArrayList<>();

    public String getCreated() {
        return created;
    }

    public void setCreated(final String created) {
        this.created = created;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(final String lastModified) {
        this.lastModified = lastModified;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public List<String> getAttributes() {
        return attributes;
    }

    public Set<Attribute> toAttributes() throws IllegalArgumentException, IllegalAccessException {
        Set<Attribute> attrs = new HashSet<>();
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.getAnnotation(JsonIgnore.class) == null) {
                field.setAccessible(true);
                attrs.add(SCIMv11Attributes.doBuildAttributeFromClassField(
                        field.get(this),
                        SCIMv11Attributes.SCIM_USER_META.concat(".")
                                .concat(field.getName()),
                        field.getType()).build());
            }
        }
        return attrs;
    }

    @Override
    public String toString() {
        return "SCIMMeta{" + "created=" + created + ", lastModified=" + lastModified
                + ", location=" + location + ", version=" + version + ", attributes=" + attributes + '}';
    }

}
