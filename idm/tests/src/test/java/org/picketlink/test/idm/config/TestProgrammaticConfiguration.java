/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.picketlink.test.idm.config;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.picketbox.test.ldap.AbstractLDAPTest;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.IdentityManagerFactory;
import org.picketlink.idm.config.FeatureSet.FeatureGroup;
import org.picketlink.idm.internal.config.ConfigurationBuilder;
import org.picketlink.idm.jpa.internal.JPAContextInitializer;
import org.picketlink.idm.jpa.schema.CredentialObject;
import org.picketlink.idm.jpa.schema.CredentialObjectAttribute;
import org.picketlink.idm.jpa.schema.IdentityObject;
import org.picketlink.idm.jpa.schema.IdentityObjectAttribute;
import org.picketlink.idm.jpa.schema.PartitionObject;
import org.picketlink.idm.jpa.schema.RelationshipIdentityObject;
import org.picketlink.idm.jpa.schema.RelationshipObject;
import org.picketlink.idm.jpa.schema.RelationshipObjectAttribute;
import org.picketlink.idm.model.Realm;
import org.picketlink.idm.model.SimpleUser;

/**
 * @author Pedro Silva
 * 
 */
public class TestProgrammaticConfiguration extends AbstractLDAPTest {

    private static final String BASE_DN = "dc=jboss,dc=org";
    private static final String LDAP_URL = "ldap://localhost:10389";
    private static final String ROLES_DN_SUFFIX = "ou=Roles,dc=jboss,dc=org";
    private static final String GROUP_DN_SUFFIX = "ou=Groups,dc=jboss,dc=org";
    private static final String USER_DN_SUFFIX = "ou=People,dc=jboss,dc=org";
    private static final String AGENT_DN_SUFFIX = "ou=Agent,dc=jboss,dc=org";

    private EntityManagerFactory emf;
    private EntityManager entityManager;

    @Before
    public void onInit() throws Exception {
        this.emf = Persistence.createEntityManagerFactory("jpa-identity-store-tests-pu");
        this.entityManager = emf.createEntityManager();
        this.entityManager.getTransaction().begin();

        super.importLDIF("ldap/users.ldif");
    }

    @After
    public void onDestroy() throws Exception {
        this.entityManager.getTransaction().commit();
        this.entityManager.close();
        this.emf.close();

        super.tearDown();
    }

    @Test
    public void testFileIdentityStoreConfiguration() throws Exception {
        ConfigurationBuilder configuration = new ConfigurationBuilder();

        configuration
            .fileStore()
                .alwaysCreateFiles(true)
                .workingDir("/tmp/pl-idm")
                .asyncWrite(true)
                .asyncThreadPool(10)
                .addRealm(Realm.DEFAULT_REALM)
                .addRealm("Realm")
                .addTier("Tier")
                .supportAllFeatures(); // you can also enable features individually. eg.:supportFeature(FeatureGroup.user)

        IdentityManagerFactory identityManagerFactory = configuration.buildIdentityManagerFactory();

        IdentityManager identityManager = identityManagerFactory.createIdentityManager();

        SimpleUser user = new SimpleUser("user");

        identityManager.add(user);

        Assert.assertNotNull(identityManager.getUser(user.getLoginName()));
    }

    @Test
    public void testJPAIdentityStoreConfiguration() throws Exception {
        ConfigurationBuilder configuration = new ConfigurationBuilder();

        configuration
            .jpaStore()
                .addContextInitializer(new JPAContextInitializer(emf) {
                    @Override
                    public EntityManager getEntityManager() {
                        return entityManager;
                    }
                })
                .setIdentityClass(IdentityObject.class)
                .setAttributeClass(IdentityObjectAttribute.class)
                .setRelationshipClass(RelationshipObject.class)
                .setRelationshipIdentityClass(RelationshipIdentityObject.class)
                .setRelationshipAttributeClass(RelationshipObjectAttribute.class)
                .setCredentialClass(CredentialObject.class)
                .setCredentialAttributeClass(CredentialObjectAttribute.class)
                .setPartitionClass(PartitionObject.class)
                .addRealm(Realm.DEFAULT_REALM)
                .addRealm("Realm")
                .addTier("Tier")
                .supportAllFeatures(); // you can also enable features individually. eg.: supportFeature(FeatureGroup.user)

        IdentityManagerFactory identityManagerFactory = configuration.buildIdentityManagerFactory();

        IdentityManager identityManager = identityManagerFactory.createIdentityManager();

        SimpleUser user = new SimpleUser("user");

        identityManager.add(user);

        Assert.assertNotNull(identityManager.getUser(user.getLoginName()));
    }
    
    @Test
    public void testLDAPIdentityStoreConfiguration() throws Exception {
        ConfigurationBuilder configuration = new ConfigurationBuilder();

        configuration
            .ldapStore()
                .setBaseDN(BASE_DN)
                .setBindDN("uid=admin,ou=system")
                .setBindCredential("secret")
                .setLdapURL(LDAP_URL)
                .setUserDNSuffix(USER_DN_SUFFIX)
                .setRoleDNSuffix(ROLES_DN_SUFFIX)
                .setAgentDNSuffix(AGENT_DN_SUFFIX)
                .setGroupDNSuffix(GROUP_DN_SUFFIX)
                .addRealm(Realm.DEFAULT_REALM)
                .supportFeature(
                        FeatureGroup.user, 
                        FeatureGroup.agent, 
                        FeatureGroup.user, 
                        FeatureGroup.group,
                        FeatureGroup.role, 
                        FeatureGroup.attribute, 
                        FeatureGroup.relationship, 
                        FeatureGroup.credential);

        IdentityManagerFactory identityManagerFactory = configuration.buildIdentityManagerFactory();

        IdentityManager identityManager = identityManagerFactory.createIdentityManager();

        SimpleUser user = new SimpleUser("user");

        identityManager.add(user);

        Assert.assertNotNull(identityManager.getUser(user.getLoginName()));
    }

}
