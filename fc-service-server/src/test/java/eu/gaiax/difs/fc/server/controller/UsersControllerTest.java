package eu.gaiax.difs.fc.server.controller;

import static eu.gaiax.difs.fc.server.util.CommonConstants.CATALOGUE_ADMIN_ROLE;
import static eu.gaiax.difs.fc.server.util.CommonConstants.SD_ADMIN_ROLE;
import static eu.gaiax.difs.fc.server.util.TestCommonConstants.CATALOGUE_ADMIN_ROLE_WITH_PREFIX;
import static eu.gaiax.difs.fc.server.util.TestCommonConstants.DEFAULT_GAIAX_REALM_ROLE;
import static eu.gaiax.difs.fc.server.util.TestCommonConstants.DEFAULT_PARTICIPANT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.keycloak.OAuth2Constants.CLIENT_ID;
import static org.keycloak.OAuth2Constants.CLIENT_SECRET;
import static org.keycloak.OAuth2Constants.GRANT_TYPE;
import static org.keycloak.OAuth2Constants.PASSWORD;
import static org.keycloak.OAuth2Constants.USERNAME;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.gaiax.difs.fc.api.generated.model.Error;
import eu.gaiax.difs.fc.core.dao.UserDao;

import eu.gaiax.difs.fc.server.controller.common.EmbeddedKeycloakTest;
import eu.gaiax.difs.fc.testsupport.config.EmbeddedKeycloakApplication;
import java.util.List;

import java.util.UUID;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.BasicAuthFilter;
import org.keycloak.admin.client.token.TokenService;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.services.managers.RealmManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.resource.BearerTokenAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.event.annotation.BeforeTestClass;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.gaiax.difs.fc.api.generated.model.User;
import eu.gaiax.difs.fc.api.generated.model.UserProfile;
import eu.gaiax.difs.fc.api.generated.model.UserProfiles;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;

@Slf4j
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {"server.port=9091"})
public class UsersControllerTest extends EmbeddedKeycloakTest {
    @Value("${keycloak.realm}")
    private String realmName;
    @Value("${keycloak.resource}")
    private String clientId;
    @Value("${keycloak.credentials.secret}")
    private String clientSecret;
    @Value("${keycloak.auth-server-url}")
    private String serverUrl;
    @Autowired
    private WebApplicationContext context;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserDao userDao;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeTestClass
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    public void userAuthShouldReturnUnauthorizedResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/users")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    public void userAuthShouldReturnForbiddenResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/users")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    public void addUserShouldReturnCreatedResponse() throws Exception {
        User user = getTestUser("name1", "surname2");
        String response = mockMvc
            .perform(MockMvcRequestBuilders.post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        UserProfile profile = objectMapper.readValue(response, UserProfile.class);
        assertThatResponseUserHasValidData(user, profile);
        userDao.delete(profile.getId());
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    public void addDuplicateSDReturnConflictWithKeycloak() throws Exception {
        User user = getTestUser("name2", "surname2");
        UserProfile existed = userDao.create(user);
        String response = mockMvc.perform(MockMvcRequestBuilders.post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
            .andExpect(status().isConflict())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Error error = objectMapper.readValue(response, Error.class);
        assertNotNull(error);
        assertEquals("User exists with same username", error.getMessage());
        userDao.delete(existed.getId());
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX},
        username = "catalog_admin", password = "catalog_admin")
    public void getUserShouldReturnSuccessResponse() throws Exception {
        User user = getTestUser("name3", "surname3");
        UserProfile existed = userDao.create(user);
        String response = mockMvc
            .perform(MockMvcRequestBuilders.get("/users/{userId}", existed.getId())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        UserProfile profile = objectMapper.readValue(response, UserProfile.class);
        assertThatResponseUserHasValidData(user, profile);
        userDao.delete(existed.getId());
    }
    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    public void wrongUserShouldReturnNotFoundResponse() throws Exception {
        mockMvc
            .perform(MockMvcRequestBuilders.get("/users/{userId}", "123")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX},
        username = "catalog_admin", password = "catalog_admin")
    public void getUsersShouldReturnSuccessResponse() throws Exception {
        User user = getTestUser("name4", "surname4");
        UserProfile existed = userDao.create(user);
        MvcResult result = mockMvc
            .perform(MockMvcRequestBuilders.get("/users?offset={offset}&limit={limit}", null, null)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
        UserProfiles users = objectMapper.readValue(result.getResponse().getContentAsString(), UserProfiles.class);
        assertNotNull(users);
        // Counts with catalogue administrator user
        assertEquals(2, users.getItems().size());
        userDao.delete(existed.getId());
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX},
        username = "catalog_admin", password = "catalog_admin")
    public void getUsersWithTotalCountShouldReturnSuccessResponse() throws Exception {
        User user = getTestUser("name4", "surname4");
        UserProfile existed = userDao.create(user);
        MvcResult result = mockMvc
            .perform(MockMvcRequestBuilders.get("/users", null, null)
                .contentType(MediaType.APPLICATION_JSON)
                .param("offset","1")
                .param("limit","1"))
            .andExpect(status().isOk())
            .andReturn();
        UserProfiles users = objectMapper.readValue(result.getResponse().getContentAsString(), UserProfiles.class);
        assertNotNull(users);
        // Counts with catalogue administrator user
        assertEquals(2, users.getTotalCount());
        assertEquals(1, users.getItems().size());
        userDao.delete(existed.getId());
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    public void deleteUserShouldReturnSuccessResponse() throws Exception {
        User user = getTestUser("name5", "surname5");
        UserProfile existed = userDao.create(user);
        String response = mockMvc
            .perform(MockMvcRequestBuilders.delete("/users/{userId}", existed.getId()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        UserProfile profile = objectMapper.readValue(response, UserProfile.class);
        assertThatResponseUserHasValidData(user, profile);
    }

    @Test
    public void deleteUserAndKeycloakAccessShouldReturnUnauthorizedError() throws Exception {
        UserModel user = createUser("newuser", "newuser", CATALOGUE_ADMIN_ROLE);
        AccessTokenResponse accessTokenResponse = grantToken("newuser", "newuser");

        String response = mockMvc
            .perform(MockMvcRequestBuilders.delete("/users/{userId}", user.getId())
                .with(authentication(new BearerTokenAuthenticationToken(accessTokenResponse.getToken())))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        UserProfile profile = objectMapper.readValue(response, UserProfile.class);

        assertThrows(NotFoundException.class, () -> userDao.delete(profile.getId()));
        assertThrows(NotAuthorizedException.class, () -> grantToken("newuser", "newuser"));
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    public void updateUserShouldReturnSuccessResponse() throws Exception {
        User user = getTestUser("name6", "surname6");
        UserProfile existed = userDao.create(user);
        user = getTestUser("changed name", "changed surname");
        user.addRoleIdsItem("Ro-MU-CA");
        String response = mockMvc
            .perform(MockMvcRequestBuilders.put("/users/{userId}", existed.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        UserProfile profile = objectMapper.readValue(response, UserProfile.class);
        assertThatResponseUserHasValidData(user, profile);
        userDao.delete(existed.getId());
    }


    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    public void updateUserRolesShouldReturnSuccessResponse() throws Exception {
        User user = getTestUser("name7", "surname7");
        UserProfile existed = userDao.create(user);

        String response = mockMvc
            .perform(MockMvcRequestBuilders.put("/users/{userId}/roles", existed.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(SD_ADMIN_ROLE))))
            .andExpect(status().isOk())
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        UserProfile profile = objectMapper.readValue(response, UserProfile.class);
        assertEquals(2, profile.getRoleIds().size());
        assertTrue(profile.getRoleIds().containsAll(List.of(SD_ADMIN_ROLE, "default-roles-gaia-x")));
        userDao.delete(existed.getId());
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    public void changeUserPermissionShouldReturnSuccessResponse() throws Exception {
        UserProfile existed = userDao.create(getTestUser("new_user", "new_user")
            .roleIds(List.of("default-roles-gaia-x")));
        assertEquals(1, existed.getRoleIds().size());
        assertTrue( existed.getRoleIds().contains("default-roles-gaia-x"));

        String response = mockMvc
            .perform(MockMvcRequestBuilders.put("/users/{userId}/roles", existed.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(SD_ADMIN_ROLE))))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        UserProfile updated = objectMapper.readValue(response, UserProfile.class);
        assertEquals(2, updated.getRoleIds().size());
        assertTrue(updated.getRoleIds().containsAll(List.of(SD_ADMIN_ROLE, DEFAULT_GAIAX_REALM_ROLE)));
        userDao.delete(existed.getId());
    }

    @Test
    @Disabled("Need to fix a bug with roles when creating a user in Keycloak")
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    public void updateDuplicatedUserRoleShouldReturnSuccessResponse() throws Exception {
        User user = getTestUser("name8", "surname8");
        UserProfile existed = userDao.create(user);
        String response = mockMvc.perform(MockMvcRequestBuilders.put("/users/{userId}/roles", existed.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(SD_ADMIN_ROLE))))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        UserProfile newProfile = objectMapper.readValue(response, UserProfile.class);
        assertNotNull(newProfile);
        assertEquals(2, newProfile.getRoleIds().size());
        assertTrue(newProfile.getRoleIds().containsAll(List.of(SD_ADMIN_ROLE, DEFAULT_GAIAX_REALM_ROLE)));
    }

    private User getTestUser(String firstName, String lastName) {
        return new User()
            .email(firstName + "." + lastName + "@test.org")
            .participantId(DEFAULT_PARTICIPANT_ID)
            .firstName(firstName)
            .lastName(lastName)
            .addRoleIdsItem(SD_ADMIN_ROLE)
            .addRoleIdsItem(DEFAULT_GAIAX_REALM_ROLE);
    }

    private static void assertThatResponseUserHasValidData(final User excepted, final UserProfile actual) {
        assertNotNull(actual);
        assertNotNull(actual.getId());
        assertNotNull(actual.getUsername());
        assertEquals(excepted.getEmail(), actual.getEmail());
        assertEquals(excepted.getFirstName(), actual.getFirstName());
        assertEquals(excepted.getLastName(), actual.getLastName());
        assertEquals(excepted.getRoleIds().size(), actual.getRoleIds().size());
        assertTrue(actual.getRoleIds().containsAll(excepted.getRoleIds()));
        assertEquals(excepted.getParticipantId(), actual.getParticipantId());
    }

    public AccessTokenResponse grantToken(String username, String password) {
        MultivaluedMap<String, String> body = new MultivaluedHashMap<>();
        body.add(GRANT_TYPE, PASSWORD);
        body.add(USERNAME, username);
        body.add(PASSWORD, password);
        body.add(CLIENT_ID, clientId);
        body.add(CLIENT_SECRET, clientSecret);

        WebTarget target = new ResteasyClientBuilder().connectionPoolSize(5).build().target(serverUrl);
        target.register(new BasicAuthFilter(clientId, clientSecret));
        return Keycloak.getClientProvider().targetProxy(target, TokenService.class).grantToken(realmName, body);
    }

    private UserModel createUser(String username, String password, String role) {
        KeycloakSession session = EmbeddedKeycloakApplication.getSessionFactory().create();
        try {
            session.getTransactionManager().begin();
            RealmManager manager = new RealmManager(session);
            RealmModel realm = manager.getRealm(realmName);
            RoleModel roleModel = realm.getRole(role);
            UserModel userModel = session.users().getUserByUsername(realm, username);
            if (userModel == null) {
                userModel = session.users().addUser(realm, UUID.randomUUID().toString(), username, true, true);
                userModel.grantRole(roleModel);
                userModel.setEmail(username + "@test.com");
                userModel.setEnabled(true);
                session.userCredentialManager()
                    .updateCredential(realm, userModel, UserCredentialModel.password(password));
            }
            session.getTransactionManager().commit();
            session.close();
            return userModel;
        } catch (Exception ex) {
            session.getTransactionManager().rollback();
            session.close();
            throw ex;
        }
    }
}
