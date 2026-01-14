package com.parking.parkinglot.ejb;


import com.parking.parkinglot.common.CarDto;
import com.parking.parkinglot.common.UserDto;
import com.parking.parkinglot.entities.Car;
import com.parking.parkinglot.entities.User;
import com.parking.parkinglot.entities.UserGroup;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

@Stateless
public class UsersBean {

    private static final Logger LOG = Logger.getLogger(UsersBean.class.getName());

    @PersistenceContext
    EntityManager entityManager;

    @Inject
    PasswordBean passwordBean;


    public List<UserDto> copyUsersToDto(List<User> users) {
        List<UserDto> dtos = new ArrayList<>();
        for (User user : users) {
            dtos.add(
                    new UserDto(user.getId(), user.getEmail(), user.getPassword(), user.getUsername())
            );
        }
        return dtos;
    }

    public List<UserDto> findAllUsers() {
        LOG.info("findAllUsers");
        try {
            TypedQuery<User> typedQuery = entityManager.createQuery("SELECT u FROM User u", User.class);
            List<User> users = typedQuery.getResultList();
            return copyUsersToDto(users);
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
    }

    public Collection<String> findUsernamesByUserIds(Collection<Long> userIds) {
        List<String> usernames =
                entityManager.createQuery(
                                "SELECT u.username FROM User u WHERE u.id IN :userIds", String.class)
                        .setParameter("userIds", userIds)
                        .getResultList();
        return usernames;
    }

    public UserDto findById(Long userId) {
        LOG.info("findById");

        if (userId < 1) {
            userId = 1L;
        }

        try {
            TypedQuery<User> typedQuery = entityManager.createQuery("SELECT p FROM User p WHERE p.id = :id", User.class).setParameter("id", userId);
            User user = typedQuery.getSingleResult();
            return new UserDto(user.getId(), user.getEmail(), user.getPassword(), user.getUsername());
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
    }


    public void createUser(String username, String email, String password, Collection<String> groups) {
        LOG.info("createUser");
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPassword(passwordBean.convertToSha256(password));
        entityManager.persist(newUser);
        assignGroupsToUser(username, groups);
    }

    public void updateUser(Long userId, String username, String email, String password, Collection<String> groups) {
        LOG.info("updateUser");


        User user = entityManager.find(User.class, userId);

        user.setUsername(username);
        user.setEmail(email);
        if (!password.isEmpty()) {
            user.setPassword(passwordBean.convertToSha256(password));
        }
        entityManager.persist(user);
        assignGroupsToUser(username, groups);
    }

    private void assignGroupsToUser(String username, Collection<String> groups) {
        LOG.info("assignGroupsToUser");
        for (String group : groups) {
            UserGroup userGroup = new UserGroup();
            userGroup.setUsername(username);
            userGroup.setUserGroup(group);
            entityManager.persist(userGroup);
        }
    }

    public List<String> getGroupsFromUser(String username) {
        LOG.info("getGroupsFromUser");

        try {
            TypedQuery<String> typedQuery = entityManager.createQuery("SELECT p FROM UserGroup p WHERE p.username = :username", String.class).setParameter("username", username);
            return typedQuery.getResultList();
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
    }
 }
