package com.company.config;

import com.company.observability.SqlStatementCounter;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PersistenceObservationConfig {
    @Bean
    HibernatePropertiesCustomizer statementInspectorCustomizer(StatementInspector statementInspector) {
        return properties -> properties.put("hibernate.session_factory.statement_inspector", statementInspector);
    }

    @Bean
    StatementInspector statementInspector(SqlStatementCounter counter) {
        return sql -> {
            counter.increment();
            return sql;
        };
    }
}
