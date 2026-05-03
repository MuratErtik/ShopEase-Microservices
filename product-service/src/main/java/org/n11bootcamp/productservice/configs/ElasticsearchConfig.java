package org.n11bootcamp.productservice.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.convert.DefaultElasticsearchTypeMapper;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchTypeMapper;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;

@Configuration
public class ElasticsearchConfig {

    //this conf not useful for monolith architecture

    //this method removes _class from the ES document (_class come from java)
    //also it provides more flexibility for language compatibility
    //MappingElasticsearchConverter manages conversion of POJO and ES document
    @Bean
    MappingElasticsearchConverter elasticsearchConverter(SimpleElasticsearchMappingContext mappingContext) {
        return new MappingElasticsearchConverter(mappingContext) {
            private final ElasticsearchTypeMapper noTypeMapper = new DefaultElasticsearchTypeMapper(null);

            @Override
            public ElasticsearchTypeMapper getTypeMapper() {
                return noTypeMapper; //It takes control of conversion from spring
            }
        };
    }
}