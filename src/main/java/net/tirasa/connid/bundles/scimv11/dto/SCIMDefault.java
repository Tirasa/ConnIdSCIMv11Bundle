/**
 * Copyright © 2018 ConnId (connid-dev@googlegroups.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.HashSet;
import java.util.Set;
import net.tirasa.connid.bundles.scimv11.utils.SCIMv11Attributes;
import org.identityconnectors.framework.common.objects.Attribute;

public class SCIMDefault {

    @JsonProperty
    private String value;

    public void setValue(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public Set<Attribute> toAttributes(final String id) throws IllegalArgumentException, IllegalAccessException {
        Set<Attribute> attrs = new HashSet<>();
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (!field.isAnnotationPresent(JsonIgnore.class)) {
                field.setAccessible(true);
                attrs.add(SCIMv11Attributes.doBuildAttributeFromClassField(
                        field.get(this),
                        id.concat(".")
                                .concat("default")
                                .concat(".")
                                .concat(field.getName()),
                        field.getType()).build());
            }
        }
        return attrs;
    }

    @Override
    public String toString() {
        return "SCIMDefault{" + "value=" + value + '}';
    }

}
