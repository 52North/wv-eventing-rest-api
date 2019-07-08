/*
 * Copyright (C) 2016 - 2019 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public
 * License version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 */
package org.n52.eventing.wv.dao.hibernate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.n52.eventing.rest.Pagination;
import org.n52.eventing.rest.QueryResult;
import org.n52.eventing.wv.dao.GroupDao;
import org.n52.eventing.wv.model.Group;
import org.n52.eventing.wv.model.WvUser;
import org.n52.eventing.wv.security.GroupPolicies;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class HibernateGroupDao extends BaseHibernateDao<Group> implements GroupDao {

    private final GroupPolicies policies;

    public HibernateGroupDao(Session s, GroupPolicies gp) {
        super(s);
        this.policies = gp;
    }

    @Override
    protected List<Predicate> customCriteria(CriteriaBuilder criteriaBuilder, Root<Group> from) {
        return Collections.singletonList(criteriaBuilder.like(from.get("name"), policies.getGroupPrefix()+"%"));
    }

    public QueryResult<Group> retrieve(Pagination pagination, WvUser user) {
        if (user.isAdmin()) {
            return super.retrieve(pagination);
        }
        String paramUser = "userId";
        String entity = Group.class.getSimpleName();
        String hql = String.format(
                "SELECT DISTINCT g FROM %s g join g.users r WHERE r.userid = :%s",
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
    public Optional<Group> retrieveById(int id) {
        return retrieveByKey("usergroupid", Integer.toString(id, 10));
    }

    @Override
    protected String orderField() {
        return "usergroupid";
    }


}
