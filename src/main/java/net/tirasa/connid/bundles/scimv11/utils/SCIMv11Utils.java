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
package net.tirasa.connid.bundles.scimv11.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
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
                || (obj instanceof String ? StringUtils.isBlank(String.class.cast(obj)) : false);
    }

}
