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
package net.tirasa.connid.bundles.scimv11.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.tirasa.connid.bundles.scimv11.dto.PagedResults;
import net.tirasa.connid.bundles.scimv11.dto.User;
import net.tirasa.connid.bundles.scimv11.utils.SCIMv11Utils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;

public class SCIMv11Client extends SCIMv11Service {

    private static final Log LOG = Log.getLog(SCIMv11Client.class);

    public SCIMv11Client(
            final String baseAddress,
            final String username,
            final GuardedString password,
            final String accept,
            final String contentType,
            final String bearer,
            final String customAttributesJSON,
            final String updateMethod) {
        super(baseAddress, username, password, accept, contentType, bearer, customAttributesJSON, updateMethod);
    }

    /**
     *
     * @return List of Users
     */
    public List<User> getAllUsers() {
        WebClient webClient = getWebclient("Users", null);
        return doGetAllUsers(webClient).getResources();
    }

    /**
     *
     * @param filterQuery to filter results
     * @return Filtered list of Users
     */
    public List<User> getAllUsers(final String filterQuery) {
        Map<String, String> params = new HashMap<>();
        params.put("filter", filterQuery);
        WebClient webClient = getWebclient("Users", params);
        return doGetAllUsers(webClient).getResources();
    }

    /**
     *
     * @param startIndex
     * @param count
     * @return Paged list of Users
     */
    public PagedResults<User> getAllUsers(final Integer startIndex, final Integer count) {
        Map<String, String> params = new HashMap<>();
        params.put("startIndex", String.valueOf(startIndex));
        if (count != null) {
            params.put("count", String.valueOf(count));
        }
        WebClient webClient = getWebclient("Users", params);
        return doGetAllUsers(webClient);
    }

    /**
     *
     * @param filterQuery
     * @param startIndex
     * @param count
     * @return Paged and Filtered list of Users
     */
    public PagedResults<User> getAllUsers(final String filterQuery, final Integer startIndex, final Integer count) {
        Map<String, String> params = new HashMap<>();
        params.put("startIndex", String.valueOf(startIndex));
        if (count != null) {
            params.put("count", String.valueOf(count));
        }
        params.put("filter", filterQuery);
        WebClient webClient = getWebclient("Users", params);
        return doGetAllUsers(webClient);
    }

    /**
     *
     * @param userId
     * @return User with userId id
     */
    public User getUser(final String userId) {
        WebClient webClient = getWebclient("Users", null)
                .path(userId);
        return doGetUser(webClient);
    }

    /**
     *
     * @param user
     * @return Created User
     */
    public User createUser(final User user) {
        return User.class.cast(doCreateUser(user));
    }

    /**
     *
     * @param user
     * @return Update User
     */
    public User updateUser(final User user) {
        return User.class.cast(doUpdateUser(user));
    }

    /**
     *
     * @param userId
     */
    public void deleteUser(final String userId) {
        WebClient webClient = getWebclient("Users", null)
                .path(userId);
        doDeleteUser(userId, webClient);
    }

    /**
     *
     * @param userId
     */
    public void activateUser(final String userId) {
        doActivateUser(userId);
    }

    public boolean testService() {
        return getAllUsers(1, 1) != null;
    }

    private PagedResults<User> doGetAllUsers(final WebClient webClient) {
        PagedResults<User> resources = null;
        JsonNode node = null;
        try {
            node = doGet(webClient);
            resources = SCIMv11Utils.MAPPER.readValue(
                    node.toString(),
                    new TypeReference<PagedResults<User>>() {
            });
        } catch (IOException ex) {
            LOG.error(ex, "While converting from JSON to Users");
        }

        if (resources == null) {
            SCIMv11Utils.handleGeneralError("While retrieving users from service");
        }

        // check custom attributes
        readCustomAttributes(resources, node.get(RESPONSE_RESOURCES));

        return resources;
    }

    private User doGetUser(final WebClient webClient) {
        User user = null;
        JsonNode node = null;
        try {
            node = doGet(webClient);
            user = SCIMv11Utils.MAPPER.readValue(
                    node.toString(),
                    User.class);
        } catch (IOException ex) {
            LOG.error(ex, "While converting from JSON to User");
        }

        if (user == null) {
            SCIMv11Utils.handleGeneralError("While retrieving user from service after create");
        }

        // check custom attributes
        readCustomAttributes(user, node);

        return user;
    }

    private User doCreateUser(final User user) {
        doCreate(user, getWebclient("Users", null));
        return user;
    }

    private User doUpdateUser(final User user) {
        if (StringUtil.isBlank(user.getId())) {
            SCIMv11Utils.handleGeneralError("Missing required user id attribute for update");
        }

        User updated = null;
        try {
            updated = SCIMv11Utils.MAPPER.readValue(
                    doUpdate(user, getWebclient("Users", null)
                            .path(user.getId())).toString(),
                    User.class);
        } catch (IOException ex) {
            LOG.error(ex, "While converting from JSON to User");
        }

        if (updated == null) {
            SCIMv11Utils.handleGeneralError("While retrieving user from service after update");
        }

        return updated;
    }

    private void doDeleteUser(final String userId, final WebClient webClient) {
        doDelete(userId, webClient);
    }

    private void doActivateUser(final String userId) {
        doActivate(userId, getWebclient("activation", null)
                .path("tokens"));
    }

}
