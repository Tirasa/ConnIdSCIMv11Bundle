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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.tirasa.connid.bundles.scimv11.dto.PagedResults;
import net.tirasa.connid.bundles.scimv11.dto.User;
import net.tirasa.connid.bundles.scimv11.service.SCIMv11Client;
import net.tirasa.connid.bundles.scimv11.utils.SCIMv11Attributes;
import net.tirasa.connid.bundles.scimv11.utils.SCIMv11Utils;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.security.SecurityUtil;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.AttributesAccessor;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;

@ConnectorClass(displayNameKey = "SCIMv11Connector.connector.display",
        configurationClass = SCIMv11ConnectorConfiguration.class)
public class SCIMv11Connector implements
        Connector, CreateOp, DeleteOp, SchemaOp, SearchOp<Filter>, TestOp, UpdateOp {

    private static final Log LOG = Log.getLog(SCIMv11Connector.class);

    private SCIMv11ConnectorConfiguration configuration;

    private Schema schema;

    private SCIMv11Client client;

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public void init(Configuration configuration) {
        LOG.ok("Init");

        this.configuration = (SCIMv11ConnectorConfiguration) configuration;
        this.configuration.validate();

        client = new SCIMv11Client(
                this.configuration.getBaseAddress(),
                this.configuration.getUsername(),
                this.configuration.getPassword(),
                this.configuration.getAccept(),
                this.configuration.getContentType(),
                this.configuration.getBearer(),
                this.configuration.getCustomAttributesJSON()
        );

        LOG.ok("Connector {0} successfully inited", getClass().getName());
    }

    @Override
    public void dispose() {
        LOG.ok("Configuration cleanup");

        configuration = null;
    }

    @Override
    public void test() {
        LOG.ok("Connector TEST");

        if (configuration != null) {
            if (client != null && client.testService()) {
                LOG.ok("Test was successfull");
            } else {
                SCIMv11Utils.handleGeneralError("Test error. Problems with client service");
            }
        } else {
            LOG.error("Test error. No instance of the configuration class");
        }
    }

    @Override
    public Schema schema() {
        LOG.ok("Building SCHEMA definition");

        if (schema == null) {
            schema = SCIMv11Attributes.buildSchema(configuration.getCustomAttributesJSON());
        }
        return schema;
    }

    @Override
    public FilterTranslator<Filter> createFilterTranslator(
            final ObjectClass objectClass,
            final OperationOptions options) {

        return new FilterTranslator<Filter>() {

            @Override
            public List<Filter> translate(final Filter filter) {
                return Collections.singletonList(filter);
            }
        };
    }

    @Override
    public void executeQuery(ObjectClass objectClass, Filter query, ResultsHandler handler, OperationOptions options) {
        LOG.ok("Connector READ");

        Attribute key = null;
        if (query instanceof EqualsFilter) {
            Attribute filterAttr = ((EqualsFilter) query).getAttribute();
            if (filterAttr instanceof Uid) {
                key = filterAttr;
            } else if (ObjectClass.ACCOUNT.equals(objectClass) || ObjectClass.GROUP.equals(objectClass)) {
                key = filterAttr;
            }
        }

        Set<String> attributesToGet = new HashSet<>();
        if (options.getAttributesToGet() != null) {
            attributesToGet.addAll(Arrays.asList(options.getAttributesToGet()));
        }

        if (ObjectClass.ACCOUNT.equals(objectClass)) {
            if (key == null) {
                List<User> users = null;
                int remainingResults = -1;
                int pagesSize = options.getPageSize() == null ? -1 : options.getPageSize();
                String cookie = options.getPagedResultsCookie();

                try {
                    if (pagesSize != -1) {
                        if (StringUtil.isNotBlank(cookie)) {
                            PagedResults<User> pagedResult =
                                    client.getAllUsers(Integer.valueOf(cookie), pagesSize);
                            users = pagedResult.getResources();

                            cookie = users.size() >= pagesSize
                                    ? String.valueOf(pagedResult.getStartIndex() + users.size())
                                    : null;
                        } else {
                            PagedResults<User> pagedResult = client.getAllUsers(1, pagesSize);
                            users = pagedResult.getResources();

                            cookie = String.valueOf(pagedResult.getStartIndex() + users.size());
                        }
                    } else {
                        users = client.getAllUsers();
                    }
                } catch (Exception e) {
                    SCIMv11Utils.wrapGeneralError("While getting Users!", e);
                }

                for (User user : users) {
                    handler.handle(fromUser(user, attributesToGet));
                }

                if (handler instanceof SearchResultsHandler) {
                    ((SearchResultsHandler) handler).handleResult(new SearchResult(cookie, remainingResults));
                }
            } else {
                User result = null;
                if (Uid.NAME.equals(key.getName()) || SCIMv11Attributes.USER_ATTRIBUTE_ID.equals(key.getName())) {
                    try {
                        result = client.getUser(AttributeUtil.getAsStringValue(key));
                    } catch (Exception e) {
                        SCIMv11Utils.wrapGeneralError("While getting User : "
                                + key.getName() + " - " + AttributeUtil.getAsStringValue(key), e);
                    }
                } else if (Name.NAME.equals(key.getName())) {
                    try {
                        List<User> users =
                                client.getAllUsers("username eq \"" + AttributeUtil.getAsStringValue(key) + "\"");
                        if (!users.isEmpty()) {
                            result = users.get(0);
                        }
                    } catch (Exception e) {
                        SCIMv11Utils.wrapGeneralError("While getting User : "
                                + key.getName() + " - " + AttributeUtil.getAsStringValue(key), e);
                    }
                }
                handler.handle(fromUser(result, attributesToGet));
            }

        } else {
            LOG.warn("Search of type {0} is not supported", objectClass.getObjectClassValue());
            throw new UnsupportedOperationException("Search of type"
                    + objectClass.getObjectClassValue() + " is not supported");
        }
    }

    @Override
    public Uid create(ObjectClass objectClass, Set<Attribute> createAttributes, OperationOptions options) {
        LOG.ok("Connector CREATE");

        if (createAttributes == null || createAttributes.isEmpty()) {
            SCIMv11Utils.handleGeneralError("Set of Attributes value is null or empty");
        }

        final AttributesAccessor accessor = new AttributesAccessor(createAttributes);

        if (ObjectClass.ACCOUNT.equals(objectClass)) {
            User user = new User();
            String username = accessor.findString(SCIMv11Attributes.USER_ATTRIBUTE_USERNAME);
            if (username == null) {
                username = accessor.findString(Name.NAME);
            }
            GuardedString password = accessor.findGuardedString(OperationalAttributes.PASSWORD_NAME);
            Attribute status = accessor.find(OperationalAttributes.ENABLE_NAME);

            try {
                // handle mandatory attributes (some attributes are handled by Service class)
                user.setUserName(username);

                if (password == null) {
                    LOG.warn("Missing password attribute");
                } else {
                    user.setPassword(SecurityUtil.decrypt(password));
                }

                if (status == null
                        || status.getValue() == null
                        || status.getValue().isEmpty()) {
                    LOG.warn("{0} attribute value not correct or not found, won't handle User status",
                            OperationalAttributes.ENABLE_NAME);
                } else {
                    user.setActive(Boolean.parseBoolean(status.getValue().get(0).toString()));
                }

                user.fromAttributes(createAttributes);

                // custom attributes
                if (StringUtil.isNotBlank(configuration.getCustomAttributesJSON())) {
                    user.fillSCIMCustomAttributes(createAttributes, configuration.getCustomAttributesJSON());
                }

                client.createUser(user);
            } catch (Exception e) {
                SCIMv11Utils.wrapGeneralError("Could not create User : " + username, e);
            }

            return new Uid(user.getId());

        } else {
            LOG.warn("Create of type {0} is not supported", objectClass.getObjectClassValue());
            throw new UnsupportedOperationException("Create of type"
                    + objectClass.getObjectClassValue() + " is not supported");
        }
    }

    @Override
    public void delete(ObjectClass objectClass, Uid uid, OperationOptions options) {
        LOG.ok("Connector DELETE");

        if (StringUtil.isBlank(uid.getUidValue())) {
            LOG.error("Uid not provided or empty ");
            throw new InvalidAttributeValueException("Uid value not provided or empty");
        }

        if (objectClass == null) {
            LOG.error("Object value not provided {0} ", objectClass);
            throw new InvalidAttributeValueException("Object value not provided");
        }

        if (ObjectClass.ACCOUNT.equals(objectClass)) {
            try {
                client.deleteUser(uid.getUidValue());
            } catch (Exception e) {
                SCIMv11Utils.wrapGeneralError("Could not delete User " + uid.getUidValue(), e);
            }

        } else {
            LOG.warn("Delete of type {0} is not supported", objectClass.getObjectClassValue());
            throw new UnsupportedOperationException("Delete of type"
                    + objectClass.getObjectClassValue() + " is not supported");
        }
    }

    @Override
    public Uid update(ObjectClass objectClass, Uid uid, Set<Attribute> replaceAttributes, OperationOptions options) {
        LOG.ok("Connector UPDATE");

        if (replaceAttributes == null || replaceAttributes.isEmpty()) {
            SCIMv11Utils.handleGeneralError("Set of Attributes value is null or empty");
        }

        final AttributesAccessor accessor = new AttributesAccessor(replaceAttributes);

        if (ObjectClass.ACCOUNT.equals(objectClass)) {
            Uid returnUid = uid;

            Attribute status = accessor.find(OperationalAttributes.ENABLE_NAME);

            User user = new User();
            user.setId(uid.getUidValue());

            if (status == null
                    || status.getValue() == null
                    || status.getValue().isEmpty()) {
                LOG.warn("{0} attribute value not correct, can't handle User  status update",
                        OperationalAttributes.ENABLE_NAME);
            } else {
                user.setActive(Boolean.parseBoolean(status.getValue().get(0).toString()));
            }

            // custom attributes
            if (StringUtil.isNotBlank(configuration.getCustomAttributesJSON())) {
                user.fillSCIMCustomAttributes(replaceAttributes, configuration.getCustomAttributesJSON());
            }

            try {
                user.fromAttributes(replaceAttributes);

                // password
                GuardedString password = accessor.getPassword() != null
                        ? accessor.getPassword()
                        : accessor.findGuardedString(OperationalAttributes.PASSWORD_NAME);
                if (password == null) {
                    LOG.warn("Missing password attribute");
                } else {
                    String decryptedPassword = SecurityUtil.decrypt(password);
                    user.setPassword(decryptedPassword);
                }

                client.updateUser(user);

                returnUid = new Uid(user.getId());
            } catch (Exception e) {
                SCIMv11Utils.wrapGeneralError(
                        "Could not update User " + uid.getUidValue() + " from attributes ", e);
            }

            return returnUid;

        } else {
            LOG.warn("Update of type {0} is not supported", objectClass.getObjectClassValue());
            throw new UnsupportedOperationException("Update of type"
                    + objectClass.getObjectClassValue() + " is not supported");
        }
    }

    public SCIMv11Client getClient() {
        return client;
    }

    private ConnectorObject fromUser(final User user, final Set<String> attributesToGet) {
        ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
        builder.setObjectClass(ObjectClass.ACCOUNT);
        builder.setUid(user.getId());
        builder.setName(user.getUserName());

        try {
            Set<Attribute> userAttributes = user.toAttributes();

            for (Attribute toAttribute : userAttributes) {
                String attributeName = toAttribute.getName();
                for (String attributeToGetName : attributesToGet) {
                    if (attributeName.equals(attributeToGetName)) {
                        builder.addAttribute(toAttribute);
                        break;
                    }
                }
            }

            // custom attributes
            if (StringUtil.isNotBlank(configuration.getCustomAttributesJSON())) {
                for (String customAttributeKey : user.getReturnedCustomAttributes().keySet()) {
                    builder.addAttribute(customAttributeKey,
                            user.getReturnedCustomAttributes().get(customAttributeKey));
                }
            }
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            LOG.error(ex, "While converting to attributes");
        }

        return builder.build();
    }

}
