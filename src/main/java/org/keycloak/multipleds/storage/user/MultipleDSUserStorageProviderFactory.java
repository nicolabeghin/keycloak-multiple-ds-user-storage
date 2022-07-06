package org.keycloak.multipleds.storage.user;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.multipleds.storage.user.entities.UserDAO;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.List;

public class MultipleDSUserStorageProviderFactory implements UserStorageProviderFactory<MultipleDSUserStorageProvider> {
    protected static final List<ProviderConfigProperty> configMetadata;
    private static final Logger logger = Logger.getLogger(MultipleDSUserStorageProviderFactory.class);
    private static final String ID = "multiple-ds-user-storage";
    private static final String DESCRIPTION = "Multiple JPA datasources User Storage Provider";
    private static final String PERSISTENCE_UNIT_NAME = "multiple-ds-user-storage-jpa";
    private static final String DATASOURCE_PROPERTY = "datasource";
    private static final String SALT_PROPERTY = "salt";

    static {
        configMetadata = ProviderConfigurationBuilder.create()
                .property().name(DATASOURCE_PROPERTY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Datasource")
                .helpText("JPA datasource ie. user-store")
                .add()
                .property().name(SALT_PROPERTY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Salt")
                .helpText("CakePHP Auth from Configure::write('Security.salt')")
                .add().build();
    }

    @Override
    public MultipleDSUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        try {
            // JBoss to Quarkus: remove prefix "java:jboss/datasources/" if existing
            String datasource = model.getConfig().getFirst(DATASOURCE_PROPERTY).replace("java:jboss/datasources/", "");
            String salt = model.getConfig().getFirst(SALT_PROPERTY);
            logger.info("Initializing instance with datasource " + datasource);
            return new MultipleDSUserStorageProvider(session, model, salt, new UserDAO(getEntityManager(session, datasource)));
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config)
            throws ComponentValidationException {
        String datasource = config.getConfig().getFirst(DATASOURCE_PROPERTY);
        String salt = config.getConfig().getFirst(SALT_PROPERTY);
        if (datasource == null || datasource.isEmpty()) {
            logger.error("Datasource not defined");
            throw new ComponentValidationException("Datasource not defined");
        }
        if (salt == null || salt.isEmpty()) {
            logger.error("Salt not defined");
            throw new ComponentValidationException("Salt not defined");
        }
    }

    /**
     * Create a JTA enabled and enrolled {@link EntityManagerFactory} that can be injected into the {@link MultipleDSUserStorageProvider}.
     *
     * @param datasourceName name of the datasource
     * @return entity manager for the given datasource
     */
    private EntityManager getEntityManager(KeycloakSession session, String datasourceName) {
        EntityManager em = session.getProvider(JpaConnectionProvider.class, datasourceName).getEntityManager();
        if (em == null) {
            logger.error("Entity manager is null for datasource " + datasourceName);
        }
        return em;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configMetadata;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getHelpText() {
        return DESCRIPTION;
    }

}
