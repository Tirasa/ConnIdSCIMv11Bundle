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
package net.tirasa.connid.bundles.scimv11;

import java.util.Map;
import java.util.Random;
import net.tirasa.connid.bundles.scimv11.utils.SCIMv11Utils;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;

public class SCIMv11ConnectorTestsUtils {

    private static final Log LOG = Log.getLog(SCIMv11ConnectorTestsUtils.class);

    public static final String USERNAME = "--testuser--";

    public static final String PASSWORD = "Password-01";

    public static final String USER_ATTRIBUTE_FAMILY_NAME = "name.familyName";

    public static final String USER_ATTRIBUTE_EMAIL_HOME_VALUE = "emails.home.value";

    public static final String USER_ATTRIBUTE_EMAIL_OTHER_VALUE = "emails.other.value";

    public static final String USER_ATTRIBUTE_EMAIL_OTHER_PRIMARY = "emails.other.primary";

    public static final String USER_ATTRIBUTE_EMAIL_OTHER_OPERATION = "emails.other.operation";

    public static final String USER_ATTRIBUTE_PHONE_OTHER_VALUE = "phoneNumbers.other.value";

    public static final String USER_ATTRIBUTE_PHONE_OTHER_PRIMARY = "phoneNumbers.other.primary";

    public static final String USER_ATTRIBUTE_PHONE_OTHER_OPERATION = "phoneNumbers.other.operation";
    
    public static final String USER_ATTRIBUTE_ADDRESS_HOME_STREETADDRESS = "addresses.home.streetAddress";

    private static final Random RANDOM = new Random();

    public static SCIMv11ConnectorConfiguration buildConfiguration(Map<String, String> configuration) {
        SCIMv11ConnectorConfiguration connectorConfiguration = new SCIMv11ConnectorConfiguration();

        for (Map.Entry<String, String> entry : configuration.entrySet()) {

            switch (entry.getKey()) {
                case "auth.baseAddress":
                    connectorConfiguration.setBaseAddress(entry.getValue());
                    break;
                case "auth.password":
                    connectorConfiguration.setPassword(SCIMv11Utils.createProtectedPassword(entry.getValue()));
                    break;
                case "auth.username":
                    connectorConfiguration.setUsername(entry.getValue());
                    break;
                case "auth.contentType":
                    connectorConfiguration.setContentType(entry.getValue());
                    break;
                case "auth.accept":
                    connectorConfiguration.setAccept(entry.getValue());
                    break;
                case "auth.bearer":
                    connectorConfiguration.setBearer(entry.getValue());
                    break;
                case "auth.customAttributesJSON":
                    connectorConfiguration.setCustomAttributesJSON(entry.getValue());
                    break;
                default:
                    LOG.info("Occurrence of an non defined parameter");
                    break;
            }
        }
        return connectorConfiguration;
    }

    public static boolean isConfigurationValid(final SCIMv11ConnectorConfiguration connectorConfiguration) {
        connectorConfiguration.validate();
        return true;
    }

    public static String createRandomName(final String namePrefix) {
        return namePrefix + StringUtil.randomString(RANDOM, (namePrefix.length() - 30));
    }
}
