package org.keycloak.multipleds.storage.user.entities;

import jakarta.persistence.*;

@NamedQueries({
        @NamedQuery(name = "getUserByUsername", query = "select u from UserEntity u where u.username = :username AND enabled=true"),
        @NamedQuery(name = "getUserById", query = "select u from UserEntity u where u.id = :id AND enabled=true"),
        @NamedQuery(name = "getUserByEmail", query = "select u from UserEntity u where u.email = :email AND enabled=true"),
        @NamedQuery(name = "getUserByPattern", query = "select u from UserEntity u where (u.email = :pattern OR u.username = :pattern) AND enabled=true"),
        @NamedQuery(name = "getUserCount", query = "select count(u) from UserEntity u WHERE enabled=true"),
        @NamedQuery(name = "getAllUsers", query = "select u from UserEntity u WHERE enabled=true"),
        @NamedQuery(name = "searchForUser", query = "select u from UserEntity u where " +
                "( lower(u.username) like :search or u.email like :search ) AND enabled=true order by u.username"),
})
@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    private String id;

    @Column(name = "utente")
    private String username;
    @Column(name = "name")
    private String firstName;
    @Column(name = "surname")
    private String lastName;
    private String email;
    private String password;
    @Column(name = "attivo")
    private boolean enabled;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
