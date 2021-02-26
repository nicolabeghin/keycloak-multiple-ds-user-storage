package org.keycloak.multipleds.storage.user;

import org.jboss.logging.Logger;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.*;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.multipleds.storage.user.entities.UserDAO;
import org.keycloak.multipleds.storage.user.entities.UserEntity;
import org.keycloak.multipleds.storage.user.models.MultipleDSUserModelDelegate;
import org.keycloak.multipleds.storage.user.utils.SHA1Utils;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.ImportedUserValidation;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;

import javax.ejb.Local;
import java.util.*;

//@Stateful
@Local(MultipleDSUserStorageProvider.class)
public class MultipleDSUserStorageProvider implements UserStorageProvider,
        UserLookupProvider,
        UserQueryProvider,
        CredentialInputUpdater,
        CredentialInputValidator,
        ImportedUserValidation {
    private final Logger logger = Logger.getLogger(MultipleDSUserStorageProvider.class);

    private final ComponentModel model;
    private final KeycloakSession session;
    private final String salt;
    private UserDAO userDAO;

    public MultipleDSUserStorageProvider(KeycloakSession session, ComponentModel model, String salt, UserDAO userDAO) {
        this.session = session;
        this.model = model;
        this.salt = salt;
        this.userDAO = userDAO;
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        UserEntity entity = userDAO.findById(id);
        if (entity == null) {
            logger.info("could not find user by id: " + id);
            return null;
        }
        return createAdapter(realm, entity);
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        return createAdapter(realm, getUserEntityByUsername(username));
    }

    private UserEntity getUserEntityByUsername(String username) {
        return userDAO.findByUsername(username);
    }

    private UserModel createAdapter(RealmModel realm, UserModel local) {
        return new MultipleDSUserModelDelegate(local);
    }

    private UserModel createAdapter(RealmModel realm, UserEntity userEntity) {
        if (userEntity == null) return null;
        UserModel local = session.userLocalStorage().getUserByUsername(userEntity.getUsername(), realm);
        if (local == null) {
            logger.info("Creating local user " + userEntity.getUsername());
            local = session.userLocalStorage().addUser(realm, userEntity.getUsername());
            local.setFirstName(userEntity.getFirstName());
            local.setLastName(userEntity.getLastName());
            if (userEntity.getEmail() != null && !ObjectUtil.isBlank(userEntity.getEmail())) {
                local.setEmail(userEntity.getEmail().trim());
            }
            local.setEnabled(userEntity.isEnabled());
            local.setFederationLink(model.getId());
            return new MultipleDSUserModelDelegate(local, userEntity);
        } else {
            logger.info("Reusing local user " + local.getUsername());
        }
        return new MultipleDSUserModelDelegate(local, userEntity);
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        UserEntity userEntity = userDAO.findByEmail(email);
        if (userEntity != null) createAdapter(realm, userEntity);
        return null;
    }

    @Override
    public UserModel validate(RealmModel realmModel, UserModel userModel) {
        UserEntity userEntity = userDAO.findByUsername(userModel.getUsername());
        if (userEntity == null || !userEntity.isEnabled()) {
            logger.warn("Username " + userModel.getUsername() + " not active anymore, evicting from Keycloak");
            return null;
        }
        return userModel;
    }

    /**
     * Returning false from a validation will just result in Red Hat Single Sign-On
     * seeing if it can validate using local storage.
     *
     * @url https://access.redhat.com/documentation/en-us/red_hat_single_sign-on/7.1/html/server_developer_guide/user-storage-spi
     */
    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) return false;
        if (session.userCredentialManager().isConfiguredLocally(realm, user, CredentialModel.PASSWORD)) {
            return false;
        }
        UserCredentialModel cred = (UserCredentialModel) input;
        String password = getPassword(user);
        return password != null && password.equals(SHA1Utils.encodeWithSalt(cred.getValue(), salt));
    }

    public String getPassword(UserModel user) {
        String password = null;
        if (user instanceof CachedUserModel) {
            password = getUserEntityByUsername(user.getUsername()).getPassword();
        } else if (user instanceof MultipleDSUserModelDelegate) {
            password = ((MultipleDSUserModelDelegate) user).getRemotePassword();
        }
        return password;
    }

    /**
     * Returning false from a update will just result in Red Hat Single Sign-On
     * seeing if it can update using local storage.
     *
     * @url https://access.redhat.com/documentation/en-us/red_hat_single_sign-on/7.1/html/server_developer_guide/user-storage-spi
     */
    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) return false;
        logger.info("Updating password on remote " + user.getUsername());
        UserEntity userEntity = getUserEntityByUsername(user.getUsername());
        UserCredentialModel cred = (UserCredentialModel) input;
        userEntity.setPassword(SHA1Utils.encodeWithSalt(cred.getValue(), salt));
        return false;
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return CredentialModel.PASSWORD.equals(credentialType);
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
        if (!supportsCredentialType(credentialType)) return;
        createAdapter(realm, user).setEnabled(false);
    }

    @Override
    public Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {
        Set<String> set = new HashSet<>();
        set.add(CredentialModel.PASSWORD);
        return set;
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return supportsCredentialType(credentialType) && getPassword(user) != null;
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        return userDAO.getCount();
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm) {
        return getUsers(realm, -1, -1);
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
        List<UserEntity> results = userDAO.findAll(firstResult, maxResults);
        List<UserModel> users = new LinkedList<>();
        for (UserEntity entity : results) users.add(createAdapter(realm, entity));
        return users;
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return searchForUser(search, realm, -1, -1);
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
        List<UserEntity> results = userDAO.search(search, firstResult, maxResults);
        List<UserModel> users = new LinkedList<>();
        for (UserEntity entity : results) users.add(createAdapter(realm, entity));
        return users;
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm, int firstResult, int maxResults) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public void preRemove(RealmModel realm) {

    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {

    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {

    }

    @Override
    public void close() {
        userDAO.close();
    }

}
