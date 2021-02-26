package org.keycloak.multipleds.storage.user.entities;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.logging.Logger;

public class UserDAO {

    private final EntityManager entityManager;
    private final Logger LOG = Logger.getLogger(UserDAO.class.getName());

    public UserDAO(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public UserEntity findById(String id) {
        LOG.info(String.format("getUserById: %s", id));
        TypedQuery<UserEntity> query = entityManager.createNamedQuery("getUserById", UserEntity.class);
        query.setParameter("id", id);
        return query.getResultList().stream().findFirst().orElse(null);
    }

    public UserEntity findByUsername(String username) {
        LOG.info("getUserByUsername: " + username);
        TypedQuery<UserEntity> query = entityManager.createNamedQuery("getUserByUsername", UserEntity.class);
        query.setParameter("username", username);
        return query.getResultList().stream().findFirst().orElse(null);
    }

    public UserEntity findByEmail(String email) {
        LOG.info(String.format("getUserByEmail: %s", email));
        TypedQuery<UserEntity> query = entityManager.createNamedQuery("getUserByEmail", UserEntity.class);
        query.setParameter("email", email);
        return query.getResultList().stream().findFirst().orElse(null);
    }

    public List<UserEntity> findAll(int firstResult, int maxResults) {
        LOG.info(String.format("getAllUsers: [%d,%d]", firstResult, maxResults));
        TypedQuery<UserEntity> query = entityManager.createNamedQuery("getAllUsers", UserEntity.class);
        if (firstResult != -1) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != -1) {
            query.setMaxResults(maxResults);
        }
        return query.getResultList();
    }

    public List<UserEntity> search(String search, int firstResult, int maxResults) {
        LOG.info(String.format("searchForUser: [%s,%d,%d]", search, firstResult, maxResults));
        TypedQuery<UserEntity> query = entityManager.createNamedQuery("searchForUser", UserEntity.class);
        query.setParameter("search", "%" + search.toLowerCase() + "%");
        if (firstResult != -1) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != -1) {
            query.setMaxResults(maxResults);
        }
        return query.getResultList();
    }

    public int getCount() {
        LOG.info("getUserCount");
        Object count = entityManager.createNamedQuery("getUserCount").getSingleResult();
        return ((Number) count).intValue();
    }

    public void close() {
        try {
            if (entityManager != null) {
                this.entityManager.close();
            }
        } catch (Exception ex) {
            LOG.severe(ex.getMessage());
        }
    }
}
