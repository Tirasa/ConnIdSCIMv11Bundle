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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import net.tirasa.connid.bundles.scimv11.dto.PagedResults;
import net.tirasa.connid.bundles.scimv11.dto.SCIMComplex;
import net.tirasa.connid.bundles.scimv11.dto.SCIMUserAddress;
import net.tirasa.connid.bundles.scimv11.dto.User;
import net.tirasa.connid.bundles.scimv11.service.NoSuchEntityException;
import net.tirasa.connid.bundles.scimv11.service.SCIMv11Client;
import net.tirasa.connid.bundles.scimv11.types.AddressCanonicalType;
import net.tirasa.connid.bundles.scimv11.types.EmailCanonicalType;
import net.tirasa.connid.bundles.scimv11.types.PhoneNumberCanonicalType;
import net.tirasa.connid.bundles.scimv11.utils.SCIMv11Attributes;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.SortKey;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.test.common.TestHelpers;
import org.identityconnectors.test.common.ToListResultsHandler;
import org.junit.BeforeClass;
import org.junit.Test;

public class SCIMv11ConnectorTests {

    private static final Log LOG = Log.getLog(SCIMv11ConnectorTests.class);

    private final static Properties PROPS = new Properties();

    private static SCIMv11ConnectorConfiguration CONF;

    private static SCIMv11Connector CONN;

    private static ConnectorFacade connector;

    private static final List<String> CUSTOMS_OTHER_SCHEMAS = new ArrayList<>();

    private static final List<String> CUSTOM_ATTRIBUTES_KEYS = new ArrayList<>();

    private static final List<String> CUSTOM_ATTRIBUTES_VALUES = new ArrayList<>();

    private static final List<String> CUSTOM_ATTRIBUTES_UPDATE_VALUES = new ArrayList<>();

    @BeforeClass
    public static void setUpConf() throws IOException {
        PROPS.load(SCIMv11ConnectorTests.class.getResourceAsStream(
                "/net/tirasa/connid/bundles/scimv11/oauth2.properties"));

        Map<String, String> configurationParameters = new HashMap<>();
        for (final String name : PROPS.stringPropertyNames()) {
            configurationParameters.put(name, PROPS.getProperty(name));
        }
        CONF = SCIMv11ConnectorTestsUtils.buildConfiguration(configurationParameters);
        CONF.setUpdateMethod("PATCH");

        Boolean isValid = SCIMv11ConnectorTestsUtils.isConfigurationValid(CONF);
        if (isValid) {
            CONN = new SCIMv11Connector();
            CONN.init(CONF);
            try {
                CONN.test();
            } catch (Exception e) {
                LOG.error(e, "While testing connector");
            }
            CONN.schema();
        }

        // custom schemas
        if (PROPS.containsKey("auth.otherSchemas")
                && PROPS.getProperty("auth.otherSchemas") != null) {
            CUSTOMS_OTHER_SCHEMAS.addAll(
                    Arrays.asList(PROPS.getProperty("auth.otherSchemas").split("\\s*,\\s*")));
        }
        CUSTOMS_OTHER_SCHEMAS.add("urn:scim:schemas:core:1.0");

        // custom attributes
        if (PROPS.containsKey("auth.customAttributesValues")
                && PROPS.getProperty("auth.customAttributesValues") != null) {
            CUSTOM_ATTRIBUTES_VALUES.addAll(
                    Arrays.asList(PROPS.getProperty("auth.customAttributesValues").split("\\s*,\\s*")));
        }
        if (PROPS.containsKey("auth.customAttributesKeys")
                && PROPS.getProperty("auth.customAttributesKeys") != null) {
            CUSTOM_ATTRIBUTES_KEYS.addAll(
                    Arrays.asList(PROPS.getProperty("auth.customAttributesKeys").split("\\s*,\\s*")));
        }
        if (PROPS.containsKey("auth.customAttributesUpdateValues")
                && PROPS.getProperty("auth.customAttributesUpdateValues") != null) {
            CUSTOM_ATTRIBUTES_UPDATE_VALUES.addAll(
                    Arrays.asList(PROPS.getProperty("auth.customAttributesUpdateValues").split("\\s*,\\s*")));
        }

        connector = newFacade();

        assertNotNull(CONF);
        assertNotNull(isValid);
        assertNotNull(CONF.getBaseAddress());
        assertNotNull(CONF.getPassword());
        assertNotNull(CONF.getUsername());
        assertNotNull(CONF.getBearer());
        assertNotNull(CONF.getAccept());
        assertNotNull(CONF.getContentType());
        assertNotNull(CONF.getCustomAttributesJSON());
        assertNotNull(CONF.getUpdateMethod());
    }

    private static ConnectorFacade newFacade() {
        ConnectorFacadeFactory factory = ConnectorFacadeFactory.getInstance();
        APIConfiguration impl = TestHelpers.createTestConfiguration(SCIMv11Connector.class, CONF);
        impl.getResultsHandlerConfiguration().setFilteredResultsHandlerInValidationMode(true);
        return factory.newInstance(impl);
    }

    private SCIMv11Client newClient() {
        return CONN.getClient();
    }

    @Test
    public void validate() {
        newFacade().validate();
    }

    @Test
    public void schema() {
        Schema schema = newFacade().schema();
        assertEquals(1, schema.getObjectClassInfo().size());

        boolean accountFound = false;
        for (ObjectClassInfo oci : schema.getObjectClassInfo()) {
            if (ObjectClass.ACCOUNT_NAME.equals(oci.getType())) {
                accountFound = true;
            }
        }
        assertTrue(accountFound);
    }

    @Test
    public void search() {
        ToListResultsHandler handler = new ToListResultsHandler();

        SearchResult result = connector.search(ObjectClass.ACCOUNT,
                null,
                handler,
                new OperationOptionsBuilder().build());
        assertNotNull(result);
        assertNull(result.getPagedResultsCookie());
        assertEquals(-1, result.getRemainingPagedResults());
        assertFalse(handler.getObjects().isEmpty());

        result = connector.search(ObjectClass.ACCOUNT,
                null,
                handler,
                new OperationOptionsBuilder().setPageSize(1).build());
        assertNotNull(result);
        assertNotNull(result.getPagedResultsCookie());
        assertEquals(-1, result.getRemainingPagedResults());

        result = connector.search(ObjectClass.ACCOUNT,
                null,
                handler,
                new OperationOptionsBuilder().setPagedResultsOffset(2).setPageSize(1).build());
        assertNotNull(result);
        assertNotNull(result.getPagedResultsCookie());
        assertEquals(-1, result.getRemainingPagedResults());
    }

    @Test
    public void pagedSearch() {
        final List<ConnectorObject> results = new ArrayList<>();
        final ResultsHandler handler = new ResultsHandler() {

            @Override
            public boolean handle(final ConnectorObject co) {
                return results.add(co);
            }
        };

        final OperationOptionsBuilder oob = new OperationOptionsBuilder();
        oob.setAttributesToGet("userName");
        oob.setPageSize(2);
        oob.setSortKeys(new SortKey("userName", false));

        connector.search(ObjectClass.ACCOUNT, null, handler, oob.build());

        assertEquals(2, results.size());

        results.clear();

        String cookie = "";
        do {
            oob.setPagedResultsCookie(cookie);
            final SearchResult searchResult = connector.search(ObjectClass.ACCOUNT, null, handler, oob.build());
            cookie = searchResult.getPagedResultsCookie();
        } while (cookie != null);

        LOG.info("Paged search results : {0}", results);

        assertTrue(results.size() > 2);
    }

    private void cleanup(
            final ConnectorFacade connector,
            final SCIMv11Client client,
            final String testUserUid) {
        if (testUserUid != null) {
            connector.delete(ObjectClass.ACCOUNT, new Uid(testUserUid), new OperationOptionsBuilder().build());
            try {
                client.deleteUser(testUserUid);
                fail(); // must fail
            } catch (RuntimeException e) {
                assertNotNull(e);
            }

            try {
                client.getUser(testUserUid);
                fail(); // must fail
            } catch (NoSuchEntityException e) {
                assertNotNull(e);
            }
        }
    }

    private void cleanup(
            final SCIMv11Client client,
            final String testUserUid) {
        if (testUserUid != null) {
            client.deleteUser(testUserUid);

            try {
                client.getUser(testUserUid);
                fail(); // must fail
            } catch (RuntimeException e) {
                assertNotNull(e);
            }
        }
    }

    @Test
    public void crud() {
        ConnectorFacade connector = newFacade();
        SCIMv11Client client = newClient();

        String testUser = null;
        UUID uid = UUID.randomUUID();

        try {
            Uid created = createUser(uid);
            testUser = created.getUidValue();

            User createdUser = readUser(testUser, client);
            assertEquals(createdUser.getId(), created.getUidValue());

            Uid updated = updateUser(created, uid);

            User updatedUser = readUser(updated.getUidValue(), client);
            LOG.info("Updated user: {0}", updatedUser);
            assertNull(updatedUser.getPassword()); // password won't be retrieved from API
//            assertNotNull(updatedUser.getPassword()); 
//            assertFalse(updatedUser.getPassword().isEmpty());
//            assertNotEquals(updatedUser.getPassword(), SCIMv11ConnectorTestsUtils.PASSWORD);

            // test removed attribute
            User user = client.getUser(updatedUser.getId());
            assertNotNull(user);
            for (SCIMComplex<EmailCanonicalType> email : user.getEmails()) {
                assertNotEquals(email.getType(), EmailCanonicalType.other);
            }
            for (SCIMComplex<PhoneNumberCanonicalType> phone : user.getPhoneNumbers()) {
                assertNotEquals(phone.getType(), PhoneNumberCanonicalType.other);
            }

        } catch (Exception e) {
            LOG.error(e, "While running crud test");
            fail(e.getMessage());
        } finally {
            cleanup(connector, client, testUser);
        }
    }

    private Uid createUser(final UUID uid) {
        Attribute password = AttributeBuilder.buildPassword(
                new GuardedString(SCIMv11ConnectorTestsUtils.PASSWORD.toCharArray()));

        Set<Attribute> userAttrs = new HashSet<>();
        userAttrs.add(AttributeBuilder.build(SCIMv11Attributes.USER_ATTRIBUTE_USERNAME,
                SCIMv11ConnectorTestsUtils.USERNAME + uid.toString()));
        userAttrs.add(AttributeBuilder.build(SCIMv11ConnectorTestsUtils.USER_ATTRIBUTE_FAMILY_NAME, "FamilyName"));
        userAttrs.add(AttributeBuilder.build(SCIMv11ConnectorTestsUtils.USER_ATTRIBUTE_EMAIL_HOME_VALUE,
                SCIMv11ConnectorTestsUtils.USERNAME + uid.toString() + "@email.com"));
        userAttrs.add(AttributeBuilder.build(SCIMv11ConnectorTestsUtils.USER_ATTRIBUTE_EMAIL_OTHER_VALUE,
                SCIMv11ConnectorTestsUtils.USERNAME + uid.toString() + "0@email.com"));
        userAttrs.add(AttributeBuilder.build(SCIMv11ConnectorTestsUtils.USER_ATTRIBUTE_EMAIL_OTHER_PRIMARY, true));
        userAttrs.add(AttributeBuilder.build(SCIMv11ConnectorTestsUtils.USER_ATTRIBUTE_PHONE_OTHER_VALUE,
                "+31234567890"));
        userAttrs.add(AttributeBuilder.build(SCIMv11ConnectorTestsUtils.USER_ATTRIBUTE_PHONE_OTHER_PRIMARY, false));
        userAttrs.add(AttributeBuilder.build(SCIMv11Attributes.USER_ATTRIBUTE_ACTIVE, true));
        userAttrs.add(password);

        // custom attributes
        addCustomAttributes(userAttrs);

        // custom schemas
        userAttrs.add(AttributeBuilder.build(SCIMv11Attributes.SCIM_USER_SCHEMAS, CUSTOMS_OTHER_SCHEMAS));

        Uid created = connector.create(ObjectClass.ACCOUNT, userAttrs, new OperationOptionsBuilder().build());
        assertNotNull(created);
        assertFalse(created.getUidValue().isEmpty());
        LOG.info("Created user uid: {0}", created);

        return created;
    }

    private Uid updateUser(final Uid created, final UUID uid) {
        Attribute password = AttributeBuilder.buildPassword(
                new GuardedString((SCIMv11ConnectorTestsUtils.PASSWORD + "01").toCharArray()));
        // UPDATE USER PASSWORD
        Set<Attribute> userAttrs = new HashSet<>();
        userAttrs.add(password);

        // custom attributes
        addCustomAttributes(userAttrs);

        // want to remove an attribute
        // Note that "value" and "primary" must also be the same of current attribute in order to proceed with deletion
        // See http://www.simplecloud.info/specs/draft-scim-api-01.html#edit-resource-with-patch
        userAttrs.add(AttributeBuilder.build(SCIMv11ConnectorTestsUtils.USER_ATTRIBUTE_EMAIL_OTHER_OPERATION,
                "delete")); // will also set type to "other"
        userAttrs.add(AttributeBuilder.build(SCIMv11ConnectorTestsUtils.USER_ATTRIBUTE_EMAIL_OTHER_VALUE,
                SCIMv11ConnectorTestsUtils.USERNAME + uid.toString() + "0@email.com"));
        userAttrs.add(AttributeBuilder.build(SCIMv11ConnectorTestsUtils.USER_ATTRIBUTE_EMAIL_OTHER_PRIMARY,
                true));
        userAttrs.add(AttributeBuilder.build(SCIMv11ConnectorTestsUtils.USER_ATTRIBUTE_PHONE_OTHER_OPERATION,
                "delete")); // will also set type to "other"
        userAttrs.add(AttributeBuilder.build(SCIMv11ConnectorTestsUtils.USER_ATTRIBUTE_PHONE_OTHER_VALUE,
                "+31234567890"));
        userAttrs.add(AttributeBuilder.build(SCIMv11ConnectorTestsUtils.USER_ATTRIBUTE_PHONE_OTHER_PRIMARY,
                false));

        // custom schemas
        userAttrs.add(AttributeBuilder.build(SCIMv11Attributes.SCIM_USER_SCHEMAS, CUSTOMS_OTHER_SCHEMAS));

        Uid updated = connector.update(
                ObjectClass.ACCOUNT, created, userAttrs, new OperationOptionsBuilder().build());
        assertNotNull(updated);
        assertFalse(updated.getUidValue().isEmpty());
        LOG.info("Updated user uid: {0}", updated);

        return updated;
    }

    private User readUser(final String id, final SCIMv11Client client)
            throws IllegalArgumentException, IllegalAccessException {
        User user = client.getUser(id);
        assertNotNull(user);
        assertNotNull(user.getId());
        assertEquals(user.getName().getFamilyName(), "FamilyName");
        LOG.info("Found user: {0}", user);

        // USER TO ATTRIBUTES
        Set<Attribute> toAttributes = user.toAttributes();
        LOG.info("User to attributes: {0}", toAttributes);
        assertTrue(hasAttribute(toAttributes, SCIMv11ConnectorTestsUtils.USER_ATTRIBUTE_FAMILY_NAME));
        assertTrue(hasAttribute(toAttributes, SCIMv11Attributes.USER_ATTRIBUTE_USERNAME));
        assertTrue(hasAttribute(toAttributes, SCIMv11ConnectorTestsUtils.USER_ATTRIBUTE_EMAIL_HOME_VALUE));
        assertTrue(hasAttribute(toAttributes, SCIMv11Attributes.SCIM_USER_SCHEMAS));
        assertTrue(hasAttribute(toAttributes, SCIMv11Attributes.USER_ATTRIBUTE_ACTIVE));

        final List<ConnectorObject> found = new ArrayList<>();
        connector.search(ObjectClass.ACCOUNT,
                new EqualsFilter(new Name(user.getUserName())),
                new ResultsHandler() {

            @Override
            public boolean handle(final ConnectorObject obj) {
                return found.add(obj);
            }
        }, new OperationOptionsBuilder().setAttributesToGet(CUSTOM_ATTRIBUTES_KEYS).build());
        assertEquals(found.size(), 1);
        assertNotNull(found.get(0));
        assertNotNull(found.get(0).getName());
        if (testCustomAttributes()) {
            LOG.info("Testing read custom attributes...");
            for (String key : CUSTOM_ATTRIBUTES_KEYS) {
                assertNotNull(found.get(0).getAttributeByName(key));
                assertNotNull(found.get(0).getAttributeByName(key).getValue());
                assertFalse(found.get(0).getAttributeByName(key).getValue().isEmpty());
            }
        }
        LOG.info("Found user using Connector search: {0}", found.get(0));

        return user;
    }

    @Test
    public void serviceTest() {
        SCIMv11Client client = newClient();

        String testUser = null;
        UUID uid = UUID.randomUUID();

        try {
            deleteUsersServiceTest(client);

            User created = createUserServiceTest(uid, client);
            testUser = created.getId();

            readUserServiceTest(testUser, client);

            readUsersServiceTest(client);

            updateUserServiceTest(testUser, client);

            CONF.setUpdateMethod("PUT");
            updateUserServiceTestPUT(testUser, newClient());
        } catch (Exception e) {
            LOG.error(e, "While running service test");
            fail(e.getMessage());
        } finally {
            cleanup(client, testUser);
        }
    }

    private User createUserServiceTest(final UUID uid, final SCIMv11Client client) {
        User user = new User();
        user.setUserName(SCIMv11ConnectorTestsUtils.USERNAME + uid.toString());
        user.setPassword(SCIMv11ConnectorTestsUtils.PASSWORD);
        user.getSchemas().addAll(CUSTOMS_OTHER_SCHEMAS);
        user.setDisplayName(SCIMv11ConnectorTestsUtils.USERNAME + uid.toString());
        user.setNickName(SCIMv11ConnectorTestsUtils.USERNAME + uid.toString());
        user.getName().setFamilyName("FamilyName");
        SCIMComplex<EmailCanonicalType> email = new SCIMComplex<>();
        email.setPrimary(true);
        email.setType(EmailCanonicalType.home);
        email.setValue(SCIMv11ConnectorTestsUtils.USERNAME + uid.toString() + "@email.com");
        user.getEmails().add(email);
        SCIMComplex<EmailCanonicalType> email2 = new SCIMComplex<>();
        email2.setPrimary(false);
        email2.setType(EmailCanonicalType.other);
        email2.setValue(SCIMv11ConnectorTestsUtils.USERNAME + uid.toString() + "@email.com");
        user.getEmails().add(email2);
        SCIMUserAddress userAddress = new SCIMUserAddress();
        userAddress.setStreetAddress("100 Universal City Plaza");
        userAddress.setLocality("Hollywood");
        userAddress.setRegion("CA");
        userAddress.setPostalCode("91608");
        userAddress.setCountry("US");
        userAddress.setPrimary(false);
        userAddress.setFormatted("100 Universal City Plaza\\nHollywood, CA 91608 USA");
        userAddress.setType(AddressCanonicalType.home);
        user.getAddresses().add(userAddress);

        User created = client.createUser(user);
        assertNotNull(created);
        assertNotNull(created.getId());
        LOG.info("Created user: {0}", created);

        return created;
    }

    private User updateUserServiceTest(final String userId, final SCIMv11Client client) {
        User user = client.getUser(userId);
        assertNotNull(user);
        assertNotNull(user.getDisplayName());
        assertFalse(user.getDisplayName().isEmpty());

        // want to remove an attribute
        String oldDisplayName = user.getDisplayName();
        String newDisplayName = "Updated displayName";
        user.setDisplayName(newDisplayName);
        for (SCIMComplex<EmailCanonicalType> email : user.getEmails()) {
            if (email.getType().equals(EmailCanonicalType.other)) {
                // Note that "value" and "primary" must also be the same of current attribute in order to proceed with deletion
                // See http://www.simplecloud.info/specs/draft-scim-api-01.html#edit-resource-with-patch
                email.setOperation("delete");
                break;
            }
        }
        for (SCIMUserAddress userAddress : user.getAddresses()) {
            if (userAddress.getType().equals(AddressCanonicalType.home)) {
                // Note that "value" and "primary" must also be the same of current attribute in order to proceed with deletion
                // See http://www.simplecloud.info/specs/draft-scim-api-01.html#edit-resource-with-patch
                userAddress.setOperation("delete");
                break;
            }
        }

        User updated = client.updateUser(user);
        assertNotNull(updated);
        assertFalse(updated.getDisplayName().equals(oldDisplayName));
        assertEquals(updated.getDisplayName(), newDisplayName);
        LOG.info("Updated user with PATCH: {0}", updated);

        // test removed attribute
        for (SCIMComplex<EmailCanonicalType> email : updated.getEmails()) {
            assertNotEquals(email.getType(), EmailCanonicalType.other);
        }

        return updated;
    }

    private User updateUserServiceTestPUT(final String userId, final SCIMv11Client client) {
        User user = client.getUser(userId);
        assertNotNull(user);
        assertNotNull(user.getDisplayName());
        assertFalse(user.getDisplayName().isEmpty());

        // want to remove an attribute
        String oldDisplayName = user.getDisplayName();
        String newDisplayName = "Updated displayName2";
        user.setDisplayName(newDisplayName);
        user.setMeta(null); // no need

        User updated = client.updateUser(user);
        assertNotNull(updated);
        assertFalse(updated.getDisplayName().equals(oldDisplayName));
        assertEquals(updated.getDisplayName(), newDisplayName);
        LOG.info("Updated user with PUT: {0}", updated);

        return updated;
    }

    private List<User> readUsersServiceTest(final SCIMv11Client client)
            throws IllegalArgumentException, IllegalAccessException {
        // GET USER
        List<User> users = client.getAllUsers();
        assertNotNull(users);
        assertFalse(users.isEmpty());
        LOG.info("Found Users: {0}", users);

        // GET USERS
        PagedResults<User> paged = client.getAllUsers(1, 1);
        assertNotNull(paged);
        assertFalse(paged.getResources().isEmpty());
        assertTrue(paged.getResources().size() == 1);
        assertEquals(paged.getStartIndex(), 1);
        assertNotEquals(paged.getTotalResults(), 1);
        assertEquals(paged.getItemsPerPage(), 1);
        LOG.info("Paged Users: {0}", paged);

        PagedResults<User> paged2 = client.getAllUsers(2, 1);
        assertNotNull(paged2);
        assertFalse(paged2.getResources().isEmpty());
        assertTrue(paged2.getResources().size() == 1);
        assertEquals(paged2.getStartIndex(), 2);
        assertNotEquals(paged2.getTotalResults(), 1);
        assertEquals(paged2.getItemsPerPage(), 1);
        LOG.info("Paged Users next page: {0}", paged2);

        return users;
    }

    private User readUserServiceTest(final String id, final SCIMv11Client client)
            throws IllegalArgumentException, IllegalAccessException {
        // GET USER
        User user = client.getUser(id);
        assertNotNull(user);
        assertNotNull(user.getId());
        LOG.info("Found User: {0}", user);

        // USER TO ATTRIBUTES
        Set<Attribute> toAttributes = user.toAttributes();
        LOG.info("User to attributes: {0}", toAttributes);
        assertTrue(hasAttribute(toAttributes, SCIMv11ConnectorTestsUtils.USER_ATTRIBUTE_FAMILY_NAME));
        assertTrue(hasAttribute(toAttributes, SCIMv11Attributes.USER_ATTRIBUTE_USERNAME));
        assertTrue(hasAttribute(toAttributes, SCIMv11ConnectorTestsUtils.USER_ATTRIBUTE_EMAIL_HOME_VALUE));
        assertTrue(hasAttribute(toAttributes, SCIMv11ConnectorTestsUtils.USER_ATTRIBUTE_EMAIL_OTHER_VALUE));
        assertTrue(hasAttribute(toAttributes, SCIMv11ConnectorTestsUtils.USER_ATTRIBUTE_ADDRESS_HOME_STREETADDRESS));
        assertTrue(hasAttribute(toAttributes, SCIMv11Attributes.SCIM_USER_SCHEMAS));
        assertTrue(hasAttribute(toAttributes, SCIMv11Attributes.USER_ATTRIBUTE_ACTIVE));

        // GET USER by userName
        List<User> users = client.getAllUsers(
                "username eq \"" + user.getUserName() + "\"");
        assertNotNull(users);
        assertFalse(users.isEmpty());
        assertNotNull(users.get(0).getId());
        LOG.info("Found User by userName: {0}", users.get(0));

        return user;
    }

    private void deleteUsersServiceTest(final SCIMv11Client client) {
        PagedResults<User> users = client.getAllUsers(
                "username sw \"" + SCIMv11ConnectorTestsUtils.USERNAME + "\"", 1, 100);
        assertNotNull(users);
        if (!users.getResources().isEmpty()) {
            for (User user : users.getResources()) {
                client.deleteUser(user.getId());
            }
        }
    }

    private void addCustomAttributes(final Set<Attribute> userAttrs) {
        if (testCustomAttributes()) {
            for (int i = 0; i < CUSTOM_ATTRIBUTES_VALUES.size(); i++) {
                userAttrs.add(AttributeBuilder.build(
                        CUSTOM_ATTRIBUTES_KEYS.get(i),
                        CUSTOM_ATTRIBUTES_VALUES.get(i)));
            }
        }
    }

    private boolean testCustomAttributes() {
        return StringUtil.isNotBlank(CONF.getCustomAttributesJSON())
                && !CUSTOM_ATTRIBUTES_KEYS.isEmpty()
                && !CUSTOM_ATTRIBUTES_VALUES.isEmpty()
                && !CUSTOM_ATTRIBUTES_UPDATE_VALUES.isEmpty();
    }

    private boolean hasAttribute(final Set<Attribute> attrs, final String name) {
        for (Attribute attr : attrs) {
            if (attr.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

}
