/*
 * Copyright (C) 2016-2020 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as publishedby the Free
 * Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of the
 * following licenses, the combination of the program with the linked library is
 * not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed under
 * the aforementioned licenses, is permitted by the copyright holders if the
 * distribution is compliant with both the GNU General Public License version 2
 * and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */
package org.n52.eventing.wv.security;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import org.hamcrest.CoreMatchers;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.n52.eventing.wv.dao.DatabaseException;
import org.n52.eventing.wv.dao.ImmutableException;
import org.n52.eventing.wv.dao.hibernate.HibernateGroupDao;
import org.n52.eventing.wv.dao.hibernate.HibernateUserDao;
import org.n52.eventing.wv.database.HibernateDatabaseConnection;
import org.n52.eventing.wv.model.Group;
import org.n52.eventing.wv.model.WvUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class UserSecurityServiceIT {

    private HibernateUserDao userDao;
    private UserSecurityService userSecurityService;
    private BCryptPasswordEncoder encoder;
    private Session session;
    private HibernateGroupDao groupDao;
    private WvUser createdUser;
    private Group createdGroup;

    @Before
    public void setup() throws Exception {
        HibernateDatabaseConnection hdc = new HibernateDatabaseConnection();
        hdc.afterPropertiesSet();
        this.session = hdc.createSession();

        this.userDao = new HibernateUserDao(session, new GroupPolicies());
        this.groupDao = new HibernateGroupDao(session, new GroupPolicies());

        this.encoder = new BCryptPasswordEncoder();

        this.userSecurityService = new UserSecurityService();
        this.userSecurityService.setPasswordEncoder(this.encoder);
        this.userSecurityService.setDatabaseConnection(hdc);
        this.userSecurityService.setGroupPolicies(new GroupPolicies());
    }

    @After
    public void cleanup() throws DatabaseException {
        this.userDao.remove(createdUser);
        this.groupDao.remove(createdGroup);
    }

    @Test
    public void testPrincipals() throws DatabaseException, ImmutableException {
        this.createdUser = new WvUser();
        String password = "asdf";
        createdUser.setName(UUID.randomUUID().toString().substring(0, 8));
        createdUser.setPassword(encoder.encode(password));
        createdUser.setGroups(Collections.singleton(new Group("admins-test", "admin users")));
        createdUser.getGroups().forEach(g -> {
            try {
                groupDao.store(g);
                this.createdGroup = g;
            } catch (DatabaseException ex) {
                ex.printStackTrace();
            }
        });

        if (!userDao.retrieveByName(createdUser.getName()).isPresent()) {
            userDao.store(createdUser);
        }

        UsernamePasswordAuthenticationToken result = this.userSecurityService.authenticate(
                new UsernamePasswordAuthenticationToken(createdUser.getName(), password));

        Assert.assertThat(result, CoreMatchers.notNullValue());
        Assert.assertThat(result.getPrincipal(), CoreMatchers.instanceOf(UserPrinciple.class));
        Assert.assertThat(((UserPrinciple) result.getPrincipal()).getUser().getName(), CoreMatchers.equalTo(createdUser.getName()));

        Collection<GrantedAuthority> auths = result.getAuthorities();
        auths.forEach(ga -> Assert.assertThat(ga, CoreMatchers.instanceOf(GroupPrinciple.class)));

        long adminCount = auths.stream().filter((GrantedAuthority ga) -> {
            return ((GroupPrinciple) ga).getAuthority().equals("admins");
        }).count();

        Assert.assertThat(adminCount, CoreMatchers.is(1L));
    }

}
