/*
 * Copyright 2026 Gary Ginzburg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsginzburg.cluster.framework.autoconfigure;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import org.gsginzburg.cluster.framework.datasource.ClusterRoutingDataSource;
import org.gsginzburg.cluster.framework.datasource.TenantShardCache;

@AutoConfiguration
@EnableConfigurationProperties(ClusterProperties.class)
@EnableScheduling
@EnableAsync
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = "org.gsginzburg.cluster",
    entityManagerFactoryRef = "entityManagerFactory",
    transactionManagerRef = "transactionManager"
)
@ComponentScan("org.gsginzburg.cluster.framework")
public class ClusterAutoConfiguration {

    @Bean
    @Primary
    public DataSource dataSource(ClusterProperties props, TenantShardCache tenantShardCache) {
        Map<Object, Object> targetDataSources = new HashMap<>();
        DataSource defaultDs = null;

        for (Map.Entry<String, ClusterProperties.ShardConfig> entry : props.getShards().entrySet()) {
            String shardId = entry.getKey();
            ClusterProperties.ShardConfig config = entry.getValue();

            if (config.getJdbcUrl() == null || config.getJdbcUrl().isBlank()) {
                continue;
            }

            HikariConfig hikari = new HikariConfig();
            hikari.setJdbcUrl(config.getJdbcUrl());
            hikari.setUsername(config.getUsername());
            hikari.setPassword(config.getPassword());
            hikari.setMaximumPoolSize(config.getMaxPoolSize());
            hikari.setMinimumIdle(config.getMinIdle());
            hikari.setPoolName("cluster-shard-" + shardId);
            // No connectionInitSql — search_path is set per-tenant in ClusterRoutingDataSource.getConnection()

            HikariDataSource ds = new HikariDataSource(hikari);
            targetDataSources.put(shardId, ds);
            if (defaultDs == null) defaultDs = ds;
        }

        ClusterRoutingDataSource routing = new ClusterRoutingDataSource(tenantShardCache);
        routing.setTargetDataSources(targetDataSources);
        routing.setDefaultTargetDataSource(defaultDs != null ? defaultDs : new HikariDataSource());
        routing.afterPropertiesSet();
        return routing;
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("org.gsginzburg.cluster");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Properties jpaProps = new Properties();
        jpaProps.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        jpaProps.setProperty("hibernate.ddl-auto", "none");
        jpaProps.setProperty("hibernate.show_sql", "false");
        em.setJpaProperties(jpaProps);
        return em;
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(
            LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        JpaTransactionManager tm = new JpaTransactionManager();
        tm.setEntityManagerFactory(entityManagerFactory.getObject());
        return tm;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
