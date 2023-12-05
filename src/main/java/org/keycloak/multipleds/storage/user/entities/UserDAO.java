package org.keycloak.multipleds.storage.user.entities;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.keycloak.models.UserModel;

public class UserDAO {

    private final EntityManager entityManager;
    private final Logger LOG = Logger.getLogger(UserDAO.class.getName());

    public UserDAO(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public UserEntity findById(String id) {
        return findStreamById(id, -1, 1).findFirst().orElse(null);
    }

    public UserEntity findByUsername(String username) {
        return findStreamByUsername(username, -1, 1).findFirst().orElse(null);
    }

    public UserEntity findByEmail(String email) {
        return findStreamByEmail(email, -1, 1).findFirst().orElse(null);
    }

    public Stream<UserEntity> findStreamById(String id, int firstResult, int maxResults) {
        LOG.info(String.format("getUserById: %s", id));
        TypedQuery<UserEntity> query = entityManager.createNamedQuery("getUserById", UserEntity.class);
        query.setParameter("id", id);
        if (firstResult != -1) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != -1) {
            query.setMaxResults(maxResults);
        }
        return query.getResultList().stream();
    }

    public Stream<UserEntity> findStreamByUsername(String username, int firstResult, int maxResults) {
        LOG.info("findStreamByUsername: " + username);
        TypedQuery<UserEntity> query = entityManager.createNamedQuery("getUserByUsername", UserEntity.class);
        query.setParameter("username", username);
        if (firstResult != -1) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != -1) {
            query.setMaxResults(maxResults);
        }
        return query.getResultList().stream();
    }

    public Stream<UserEntity> findStreamByPattern(String pattern, int firstResult, int maxResults) {
        LOG.info("findStreamByPattern: " + pattern);
        TypedQuery<UserEntity> query = entityManager.createNamedQuery("getUserByPattern", UserEntity.class);
        query.setParameter("pattern", pattern);
        if (firstResult != -1) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != -1) {
            query.setMaxResults(maxResults);
        }
        return query.getResultList().stream();
    }

    public Stream<UserEntity> findStreamByEmail(String email, int firstResult, int maxResults) {
        LOG.info(String.format("findStreamByEmail: %s", email));
        TypedQuery<UserEntity> query = entityManager.createNamedQuery("getUserByEmail", UserEntity.class);
        query.setParameter("email", email);
        if (firstResult != -1) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != -1) {
            query.setMaxResults(maxResults);
        }
        return query.getResultList().stream();
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

    public Stream<UserEntity> findStreamAll(Map<String, String> filters, int firstResult, int maxResults) {
        LOG.info(String.format("findStreamAll by filters: [%d,%d]", firstResult, maxResults));
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            switch (entry.getKey()) {
                case UserModel.EMAIL:
                    return findStreamByEmail(entry.getValue(), firstResult, maxResults);
                case UserModel.USERNAME:
                    return findStreamByUsername(entry.getValue(), firstResult, maxResults);
                case UserModel.IDP_USER_ID:
                    return findStreamById(entry.getValue(), firstResult, maxResults);
                case UserModel.SEARCH:
                    return findStreamByPattern(entry.getValue(), firstResult, maxResults);
                default:
                    if (!UserModel.INCLUDE_SERVICE_ACCOUNT.equals(entry.getKey())) {
                        LOG.warning("Search by attribute " + entry.getKey() + " not supported");
                    }
            }
        }
        return Stream.empty();
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
