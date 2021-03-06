package org.keycloak.multipleds.storage.user;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.transaction.jta.platform.internal.JBossAppServerJtaPlatform;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.multipleds.storage.user.entities.UserDAO;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.*;

public class MultipleDSUserStorageProviderFactory implements UserStorageProviderFactory<MultipleDSUserStorageProvider> {
    protected static final List<ProviderConfigProperty> configMetadata;
    private static final Logger logger = Logger.getLogger(MultipleDSUserStorageProviderFactory.class);
    private static final String ID = "multiple-ds-user-storage";
    private static final String DESCRIPTION = "Multiple JPA datasources User Storage Provider";
    private static final String PERSISTENCE_UNIT_NAME = "multiple-ds-user-storage-jpa";
    private static final String DATASOURCE_PROPERTY = "datasource";
    private static final String SALT_PROPERTY = "salt";

    private Map<String, EntityManagerFactory> entityManagerFactories = new HashMap<>();

    static {
        configMetadata = ProviderConfigurationBuilder.create()
                .property().name(DATASOURCE_PROPERTY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Datasource")
                .helpText("JPA datasource ie. java:jboss/datasources/SIADS")
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
            String datasource = model.getConfig().getFirst(DATASOURCE_PROPERTY);
            String salt = model.getConfig().getFirst(SALT_PROPERTY);
            logger.info("Initializing instance with datasource " + datasource);
            return new MultipleDSUserStorageProvider(session, model, salt, new UserDAO(getEntityManager(datasource).createEntityManager()));
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
     * @param datasourceName the JNDI name of the XA datasource
     * @return a configured entity manager factory
     * @url https://gist.github.com/bertramn/cbc4eec5e7b13e28099f4165a0c15b29
     */
    private EntityManagerFactory getEntityManager(String datasourceName) {
        EntityManagerFactory entityManagerFactory = entityManagerFactories.get(datasourceName);
        if (entityManagerFactory == null) {
            logger.info("Creating factory for " + datasourceName);
            Properties p = new Properties();
            p.put(AvailableSettings.DATASOURCE, datasourceName);
            p.put(AvailableSettings.JTA_PLATFORM, JBossAppServerJtaPlatform.class.getName());
            p.put("current_session_context_class", "jta");
            p.put(AvailableSettings.SHOW_SQL, false);
            p.put(AvailableSettings.FORMAT_SQL, false);

            // Adding "hibernate.classLoaders" property is critical for this to work with keycloak!!!
            p.put(AvailableSettings.CLASSLOADERS, Collections.singletonList(getClass().getClassLoader()));
            entityManagerFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, p);
            entityManagerFactories.put(datasourceName, entityManagerFactory);
        }
        return entityManagerFactory;
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
