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

import java.util.Set;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class GroupPoliciesTest {

    @Test
    public void testConfigFileParsing() {
        GroupPolicies p = new GroupPolicies();
        Set<String> admins = p.getAdminGroupNames();

        MatcherAssert.assertThat(admins.size(), CoreMatchers.is(2));
        MatcherAssert.assertThat(admins, CoreMatchers.hasItem("admins-test"));
        MatcherAssert.assertThat(admins, CoreMatchers.hasItem("administrators-test"));

        Set<String> editors = p.getEditorGroupNames();

        MatcherAssert.assertThat(editors.size(), CoreMatchers.is(2));
        MatcherAssert.assertThat(editors, CoreMatchers.hasItem("editors-test"));
        MatcherAssert.assertThat(editors, CoreMatchers.hasItem("publishers-test"));
    }

}
