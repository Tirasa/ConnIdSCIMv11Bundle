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
package net.tirasa.connid.bundles.scimv11.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.tirasa.connid.bundles.scimv11.dto.SCIMAttribute;
import net.tirasa.connid.bundles.scimv11.dto.SCIMSchema;
import net.tirasa.connid.bundles.scimv11.service.SCIMv11Service;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;

public class SCIMv11Utils {

    private static final Log LOG = Log.getLog(SCIMv11Utils.class);

    public final static ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    public static GuardedString createProtectedPassword(final String password) {
        GuardedString guardedString = new GuardedString(password.toCharArray());
        return guardedString;
    }

    public static void handleGeneralError(final String message) {
        LOG.error("General error : {0}", message);
        throw new ConnectorException(message);
    }

    public static void handleGeneralError(final String message, final Exception ex) {
        LOG.error(ex, message);
        throw new ConnectorException(message, ex);
    }

    public static void wrapGeneralError(final String message, final Exception ex) {
        LOG.error(ex, message);
        throw ConnectorException.wrap(ex);
    }

    public static boolean isEmptyObject(final Object obj) {
        return obj == null
                || (obj instanceof List ? new ArrayList<>((List<?>) obj).isEmpty() : false)
                || (obj instanceof String ? StringUtil.isBlank(String.class.cast(obj)) : false);
    }

    public static String cleanAttributesToGet(final Set<String> attributesToGet, final String customAttributesJSON) {
        if (attributesToGet.isEmpty()) {
            return SCIMv11Attributes.defaultAttributesToGet();
        }

        SCIMSchema customAttributesObj = StringUtil.isBlank(customAttributesJSON) ? null
                : SCIMv11Service.extractSCIMSchemas(customAttributesJSON);
        String result = "";
        for (String attributeToGet : attributesToGet) {
            if (attributeToGet.contains("__")
                    || attributeToGet.contains(SCIMv11Attributes.SCIM_USER_META + ".")
                    || attributeToGet.contains(SCIMv11Attributes.SCIM_USER_ENTITLEMENTS + ".")
                    || attributeToGet.toLowerCase().contains("password")) {

            } else if (attributeToGet.contains(SCIMv11Attributes.SCIM_USER_NAME + ".")) {
                result += SCIMv11Attributes.SCIM_USER_NAME.concat(",");
            } else if (attributeToGet.contains(SCIMv11Attributes.SCIM_USER_ADDRESSES + ".")) {
                result += SCIMv11Attributes.SCIM_USER_ADDRESSES.concat(",");
            } else if (attributeToGet.contains(SCIMv11Attributes.SCIM_USER_PHONE_NUMBERS + ".")) {
                result += SCIMv11Attributes.SCIM_USER_PHONE_NUMBERS.concat(",");
            } else if (attributeToGet.contains(SCIMv11Attributes.SCIM_USER_IMS + ".")) {
                result += SCIMv11Attributes.SCIM_USER_IMS.concat(",");
            } else if (attributeToGet.contains(SCIMv11Attributes.SCIM_USER_EMAILS + ".")) {
                result += SCIMv11Attributes.SCIM_USER_EMAILS.concat(",");
            } else if (attributeToGet.contains(SCIMv11Attributes.SCIM_USER_ROLES + ".")) {
                result += SCIMv11Attributes.SCIM_USER_ROLES.concat(",");
            } else if (attributeToGet.contains(SCIMv11Attributes.SCIM_USER_GROUPS + ".")) {
                result += SCIMv11Attributes.SCIM_USER_GROUPS.concat(",");
            } else if (attributeToGet.contains(SCIMv11Attributes.SCIM_USER_PHOTOS + ".")) {
                result += SCIMv11Attributes.SCIM_USER_PHOTOS.concat(",");
            } else if (attributeToGet.contains(SCIMv11Attributes.SCIM_USER_X509CERTIFICATES + ".")) {
                result += SCIMv11Attributes.SCIM_USER_X509CERTIFICATES.concat(",");
            } else if (customAttributesObj == null) {
                result += attributeToGet.concat(",");
            } else if (!isCustomAttribute(customAttributesObj, attributeToGet)) {
                result += attributeToGet.concat(",");
            }
        }

        if (customAttributesObj != null) {
            for (SCIMAttribute attribute : customAttributesObj.getAttributes()) {
                if (!result.contains(attribute.getName() + ",")) {
                    result += attribute.getName().concat(",");
                }
            }
        }

        if (!result.contains(SCIMv11Attributes.USER_ATTRIBUTE_USERNAME + ",")) {
            result += SCIMv11Attributes.USER_ATTRIBUTE_USERNAME.concat(",");
        }
        if (!result.contains(SCIMv11Attributes.USER_ATTRIBUTE_ID + ",")) {
            result += SCIMv11Attributes.USER_ATTRIBUTE_ID.concat(",");
        }
        if (!result.contains(SCIMv11Attributes.SCIM_USER_NAME + ",")) {
            result += SCIMv11Attributes.SCIM_USER_NAME.concat(",");
        }

        return StringUtil.isBlank(result) ? SCIMv11Attributes.defaultAttributesToGet()
                : result.substring(0, result.length() - 1);
    }

    private static boolean isCustomAttribute(final SCIMSchema customAttributes, final String attribute) {
        for (SCIMAttribute customAttribute : customAttributes.getAttributes()) {
            String externalAttributeName = customAttribute.getSchema()
                    .concat(".")
                    .concat(customAttribute.getName());
            if (externalAttributeName.equals(attribute)) {
                return true;
            }
        }
        return false;
    }

}
