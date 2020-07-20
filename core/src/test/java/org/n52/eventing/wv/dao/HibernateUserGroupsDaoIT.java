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
package org.n52.eventing.wv.dao;

import java.util.ArrayList;
import org.n52.eventing.wv.dao.hibernate.HibernateGroupDao;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import org.hamcrest.CoreMatchers;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.hamcrest.MatcherAssert;
import org.n52.eventing.wv.dao.hibernate.HibernateUserDao;
import org.n52.eventing.wv.database.HibernateDatabaseConnection;
import org.n52.eventing.wv.model.Group;
import org.n52.eventing.wv.model.WvUser;
import org.n52.eventing.wv.security.GroupPolicies;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class HibernateUserGroupsDaoIT {

    private HibernateDatabaseConnection hdc;
    private Session session;
    private BCryptPasswordEncoder encoder;
    private GroupPolicies policies;

    @BeforeEach
    public void setup() throws Exception {
        this.hdc = new HibernateDatabaseConnection();
        this.hdc.afterPropertiesSet();
        this.session = this.hdc.createSession();
        this.encoder = new BCryptPasswordEncoder();
        this.policies = new GroupPolicies();
    }

    @Test
    public void roundtrip() throws ImmutableException, DatabaseException  {
        HibernateGroupDao groupDao = new HibernateGroupDao(session, policies);
        HibernateUserDao userDao = new HibernateUserDao(session, policies);

        String name = this.policies.getGroupPrefix()+"publishers";
        Optional<Group> gopt = groupDao.retrieveByName(name);
        Group g;
        if (!gopt.isPresent()) {
            g = new Group(name, "Publishing users");
            g.setId(new Random().nextInt());
            groupDao.store(g);
        }
        else {
            g = gopt.get();
        }

        gopt = groupDao.retrieveByName(name);
        MatcherAssert.assertThat(gopt.isPresent(), CoreMatchers.is(true));

        List<WvUser> added = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            WvUser e1 = new WvUser();
            e1.setId(new Random().nextInt());
            e1.setName(UUID.randomUUID().toString().substring(0, 15));
            e1.setPassword(encoder.encode("asdf"));
            e1.setFirstName("peter"+i);
            e1.setLastName("chen");
            e1.setGroups(Collections.singleton(g));

            userDao.store(e1);
            added.add(e1);
        }

        for (int i = 0; i < 3; i++) {
            WvUser e1 = added.get(i);
            Optional<WvUser> r1 = userDao.retrieveById(e1.getId());
            MatcherAssert.assertThat(r1.get().getName(), CoreMatchers.equalTo(e1.getName()));
            MatcherAssert.assertThat(r1.get().getGroups(), CoreMatchers.hasItem(g));

            Optional<WvUser> r2 = userDao.retrieveByName(e1.getName());
            MatcherAssert.assertThat(r2.get().getName(), CoreMatchers.equalTo(e1.getName()));
            MatcherAssert.assertThat(r2.get().getGroups(), CoreMatchers.hasItem(g));
        }
    }

    @Test
    public void retrieveByGroupTest() throws DatabaseException {
        HibernateGroupDao groupDao = new HibernateGroupDao(session, policies);
        HibernateUserDao userDao = new HibernateUserDao(session, policies);

        String uuid = UUID.randomUUID().toString().substring(0, 12);
        String name = this.policies.getGroupPrefix()+uuid;
        Group g = new Group(uuid, name);
        g.setId(new Random().nextInt());
        groupDao.store(g);

        for (int i = 0; i < 5; i++) {
            WvUser e1 = new WvUser();
            e1.setId(new Random().nextInt());
            e1.setName(UUID.randomUUID().toString().substring(0, 15));
            e1.setPassword(encoder.encode("asdf"));
            e1.setFirstName("peter-check");
            e1.setLastName("chen");
            e1.setGroups(Collections.singleton(g));
            userDao.store(e1);
        }

        List<WvUser> r1 = userDao.retrieveByGroup(g).getResult();
        MatcherAssert.assertThat(r1.size(), CoreMatchers.is(5));
        r1.stream().forEach((u) -> {
            MatcherAssert.assertThat(u.getGroups(), CoreMatchers.hasItem(g));
        });

    }

}
