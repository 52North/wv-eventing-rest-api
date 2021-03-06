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
package org.n52.eventing.wv.dao.hibernate;

import java.util.Optional;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.n52.eventing.rest.Pagination;
import org.n52.eventing.rest.QueryResult;
import org.n52.eventing.wv.dao.UserDao;
import org.n52.eventing.wv.model.Group;
import org.n52.eventing.wv.model.WvUser;
import org.n52.eventing.wv.security.GroupPolicies;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class HibernateUserDao extends BaseHibernateDao<WvUser> implements UserDao {

    public HibernateUserDao(Session session, GroupPolicies policies) {
        super(session);
    }

    @Override
    public Optional<WvUser> retrieveByName(String name) {
        Optional<WvUser> result = super.retrieveByName(name);
        WvUser user = initializeProxies(result.isPresent() ? result.get() : null);
        return Optional.ofNullable(user);
    }

    @Override
    public QueryResult<WvUser> retrieveByGroup(Group g, Pagination pagination) {
        String param = "groupId";
        String entity = WvUser.class.getSimpleName();
        String hql = String.format("SELECT u FROM %s u join u.groups r WHERE r.usergroupid=:%s order by u.userid asc", entity, param);
        Query q = getSession().createQuery(hql);

        q.setParameter(param, g.getUsergroupid());

        return queryPagination(q, pagination, null);
    }

    public QueryResult<WvUser> retrieve(Pagination pagination, WvUser user) {
        if (user.isAdmin()) {
            return super.retrieve(pagination);
        }
        String paramUser = "userId";
        String entity = WvUser.class.getSimpleName();
        String hql = String.format(
                "SELECT DISTINCT u FROM %s u join u.groups r WHERE u.userid = :%s",
                entity, paramUser);
        Query q = getSession().createQuery(hql);

        q.setParameter(paramUser, ((Integer)user.getId()).longValue());
//        String paramUser = "userId";
//        String paramGroups = "groupIds";
//        String entity = WvUser.class.getSimpleName();
//        String hql = String.format(
//                "SELECT DISTINCT u FROM %s u join u.groups r WHERE u.userid = :%s or r.usergroupid IN (:%s) order by u.userid asc",
//                entity, paramUser, paramGroups);
//        Query q = getSession().createQuery(hql);
//
//        q.setParameter(paramUser, ((Integer)user.getId()).longValue());
//        q.setParameterList(paramGroups, user.getAdminGroups().stream().map(g -> ((Integer) g.getId()).longValue())
//                .collect(Collectors.toSet()));
        return queryPagination(q, pagination, null);
    }

    @Override
    public Optional<WvUser> retrieveById(int id) {
        Optional<WvUser> user = super.retrieveByKey("userid", Integer.toString(id));
        if (!user.isPresent()) {
            return user;
        }
        return user;
    }

    @Override
    protected Optional<WvUser> retrieveByKey(String key, String value) {
        Optional<WvUser> user = super.retrieveByKey(key, value);
        if (!user.isPresent()) {
            return user;
        }
        return user;
    }

    @Override
    public QueryResult<WvUser> retrieveByGroup(Group g) {
        return retrieveByGroup(g, null);
    }

    private WvUser initializeProxies(WvUser o) {
        if (o == null || o.getGroups() == null) {
            return o;
        }
        Hibernate.initialize(o.getGroups());
        return o;
    }

    @Override
    protected String orderField() {
        return "userid";
    }

}
