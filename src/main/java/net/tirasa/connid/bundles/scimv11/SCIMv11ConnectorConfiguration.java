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
package net.tirasa.connid.bundles.scimv11;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.ws.rs.core.MediaType;
import net.tirasa.connid.bundles.scimv11.dto.SCIMSchema;
import net.tirasa.connid.bundles.scimv11.utils.SCIMv11Utils;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.security.SecurityUtil;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;
import org.identityconnectors.framework.spi.StatefulConfiguration;

/**
 * Connector configuration class. It contains all the needed methods for
 * processing the connector configuration.
 */
public class SCIMv11ConnectorConfiguration extends AbstractConfiguration implements StatefulConfiguration {

    private static final Log LOG = Log.getLog(SCIMv11ConnectorConfiguration.class);

    private String username;

    private GuardedString password;

    private String baseAddress;

    private String customAttributesJSON;

    private String updateMethod = "PATCH";

    private String accept = MediaType.APPLICATION_JSON;

    private String contentType = MediaType.APPLICATION_JSON;

    private String clientId;

    private String clientSecret;

    private String accessTokenNodeId = "access_token";

    private String accessTokenBaseAddress;

    private String accessTokenContentType = MediaType.APPLICATION_FORM_URLENCODED;

    @ConfigurationProperty(order = 1, displayMessageKey = "baseAddress.display",
            helpMessageKey = "baseAddress.help", required = true)
    public String getBaseAddress() {
        return baseAddress;
    }

    public void setBaseAddress(final String baseAddress) {
        this.baseAddress = baseAddress;
    }

    @ConfigurationProperty(displayMessageKey = "accept.display",
            helpMessageKey = "accept.help", order = 2, required = true)
    public String getAccept() {
        return accept;
    }

    public void setAccept(final String accept) {
        this.accept = accept;
    }

    @ConfigurationProperty(displayMessageKey = "contentType.display",
            helpMessageKey = "contentType.help", order = 3, required = true)
    public String getContentType() {
        return contentType;
    }

    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }

    @ConfigurationProperty(displayMessageKey = "username.display",
            helpMessageKey = "username.help", order = 4)
    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    @ConfigurationProperty(displayMessageKey = "password.display",
            helpMessageKey = "password.help", order = 5, confidential = true)
    public GuardedString getPassword() {
        return password;
    }

    public void setPassword(final GuardedString password) {
        this.password = password;
    }


    @ConfigurationProperty(displayMessageKey = "clientId.display",
            helpMessageKey = "clientId.help", order = 6)
    public String getCliendId() {
        return clientId;
    }

    public void setCliendId(final String clientId) {
        this.clientId = clientId;
    }

    @ConfigurationProperty(displayMessageKey = "clientSecret.display",
            helpMessageKey = "clientSecret.help", order = 7, confidential = true)
    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(final String clientSecret) {
        this.clientSecret = clientSecret;
    }

    @ConfigurationProperty(displayMessageKey = "customAttributesJSON.display",
            helpMessageKey = "customAttributesJSON.help", order = 8)
    public String getCustomAttributesJSON() {
        return customAttributesJSON;
    }

    public void setCustomAttributesJSON(final String customAttributesJSON) {
        this.customAttributesJSON = customAttributesJSON;
    }

    @ConfigurationProperty(displayMessageKey = "updateMethod.display",
            helpMessageKey = "updateMethod.help", order = 9)
    public String getUpdateMethod() {
        return updateMethod;
    }

    public void setUpdateMethod(final String updateMethod) {
        this.updateMethod = updateMethod;
    }

    @ConfigurationProperty(displayMessageKey = "accessTokenNodeId.display",
            helpMessageKey = "accessTokenNodeId.help", order = 10)
    public String getAccessTokenNodeId() {
        return accessTokenNodeId;
    }

    public void setAccessTokenNodeId(final String accessTokenNodeId) {
        this.accessTokenNodeId = accessTokenNodeId;
    }

    @ConfigurationProperty(displayMessageKey = "accessTokenBaseAddress.display",
            helpMessageKey = "accessTokenBaseAddress.help", order = 11)
    public String getAccessTokenBaseAddress() {
        return accessTokenBaseAddress;
    }

    public void setAccessTokenBaseAddress(final String accessTokenBaseAddress) {
        this.accessTokenBaseAddress = accessTokenBaseAddress;
    }

    @ConfigurationProperty(displayMessageKey = "accessTokenContentType.display",
            helpMessageKey = "accessTokenContentType.help", order = 12)
    public String getAccessTokenContentType() {
        return accessTokenContentType;
    }

    public void setAccessTokenContentType(final String accessTokenContentType) {
        this.accessTokenContentType = accessTokenContentType;
    }

    @Override
    public void validate() {
        if (StringUtil.isBlank(baseAddress)) {
            failValidation("Base address cannot be null or empty.");
        }
        try {
            new URL(baseAddress);
        } catch (MalformedURLException e) {
            LOG.error(e, "While validating baseAddress");
            failValidation("Base address must be a valid URL.");
        }
        if (StringUtil.isBlank(username)) {
            failValidation("Username cannot be null or empty.");
        }
        if (StringUtil.isBlank(SecurityUtil.decrypt(password))) {
            failValidation("Password Id cannot be null or empty.");
        }
        if (StringUtil.isNotBlank(customAttributesJSON)) {
            try {
                SCIMv11Utils.MAPPER.readValue(customAttributesJSON, SCIMSchema.class);
            } catch (IOException e) {
                LOG.error(e, "While validating customAttributesJSON");
                failValidation("'customAttributesJSON' parameter must be a valid "
                        + "Resource Schema Representation JSON.");
            }
        }
        if (StringUtil.isNotBlank(updateMethod)
                && !updateMethod.equalsIgnoreCase("PATCH")
                && !updateMethod.equalsIgnoreCase("PUT")) {
            failValidation("Update method is not valid; must be 'PUT' or 'PATCH'.");
        }
    }

    @Override
    public void release() {
    }

    private void failValidation(String key, Object... args) {
        String message = getConnectorMessages().format(key, null, args);
        throw new ConfigurationException(message);
    }

}
