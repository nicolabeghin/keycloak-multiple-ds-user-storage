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
import org.keycloak.storage.UserStoragePrivateUtil;

import java.util.*;
import java.util.stream.Stream;

//@Stateful
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
    public UserModel getUserById(RealmModel realm, String id) {
        UserEntity entity = userDAO.findById(id);
        if (entity == null) {
            logger.info("could not find user by id: " + id);
            return null;
        }
        return createAdapter(realm, entity);
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        return createAdapter(realm, getUserEntityByUsername(username));
    }

    private UserEntity getUserEntityByUsername(String username) {
        return userDAO.findByUsername(username);
    }

    private UserModel createAdapter(RealmModel realm, UserModel local) {
        return new MultipleDSUserModelDelegate(local);
    }

    // https://www.keycloak.org/docs/latest/upgrading/#changes-in-code-keycloaksession-code
    private UserModel createAdapter(RealmModel realm, UserEntity userEntity) {
        if (userEntity == null) return null;
        UserModel local = UserStoragePrivateUtil.userLocalStorage(session).getUserByUsername(realm, userEntity.getUsername());
        if (local == null) {
            logger.info("Creating local user " + userEntity.getUsername());
            local = UserStoragePrivateUtil.userLocalStorage(session).addUser(realm, userEntity.getUsername());
            local.setFirstName(userEntity.getFirstName());
            local.setLastName(userEntity.getLastName());
            local.setEnabled(userEntity.isEnabled());
            local.setFederationLink(model.getId());
        } else {
            logger.info("Reusing local user " + local.getUsername());
        }
        
        // update local email from remote one
        if (userEntity.getEmail() != null && !ObjectUtil.isBlank(userEntity.getEmail())) { // email available from remote
            if (local.getEmail() == null || ObjectUtil.isBlank(local.getEmail())) { // local email not available
                logger.info("Setting up local user " + local.getUsername() + " with email " + userEntity.getEmail());
                local.setEmail(userEntity.getEmail().trim());
            } else if (false == userEntity.getEmail().equalsIgnoreCase(local.getEmail())) { // local email different from remote
                logger.info("Updating local user " + local.getUsername() + " with email " + userEntity.getEmail() + " (previously " + local.getEmail() + ")");
                local.setEmail(userEntity.getEmail().trim());
            }
        }
        
        return new MultipleDSUserModelDelegate(local, userEntity);
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
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
        if (user != null && user.credentialManager().isConfiguredLocally(CredentialModel.PASSWORD)) {
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
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return supportsCredentialType(credentialType) && getPassword(user) != null;
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        return userDAO.getCount();
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

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel arg1, Integer arg2, Integer arg3) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getGroupMembersStream'");
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String arg1, String arg2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'searchForUserByUserAttributeStream'");
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> filters, Integer firstResult, Integer maxResults) {
        return userDAO.findStreamAll(filters, firstResult, maxResults).map(entity -> createAdapter(realm, entity));
    }

    @Override
    public Stream<String> getDisableableCredentialTypesStream(RealmModel arg0, UserModel arg1) {
        return Stream.of(CredentialModel.PASSWORD);
    }

}
