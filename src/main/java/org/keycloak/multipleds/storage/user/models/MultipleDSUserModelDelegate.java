package org.keycloak.multipleds.storage.user.models;

import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.multipleds.storage.user.entities.UserEntity;

public class MultipleDSUserModelDelegate extends UserModelDelegate {

    private final UserEntity userEntity;

    public MultipleDSUserModelDelegate(UserModel delegate) {
        super(delegate);
        this.userEntity = null;
    }

    public MultipleDSUserModelDelegate(UserModel delegate, UserEntity userEntity) {
        super(delegate);
        this.userEntity = userEntity;
    }

    public String getRemotePassword() {
        if (userEntity != null) {
            return userEntity.getPassword();
        }
        return null;
    }

}
