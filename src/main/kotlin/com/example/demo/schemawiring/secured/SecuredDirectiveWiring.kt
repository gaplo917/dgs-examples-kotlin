package com.example.demo.schemawiring.secured

import com.example.demo.exceptions.AccessDeniedError
import com.netflix.graphql.dgs.context.DgsContext
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.idl.SchemaDirectiveWiring
import graphql.schema.idl.SchemaDirectiveWiringEnvironment
import org.springframework.stereotype.Component


/**
 * As per graphql-java, implementation of a GraphQL schema directive such as `@secured` can be provided by using an implementation of
 * [SchemaDirectiveWiring]. <br></br>
 * This class implements the `@secured` directive as following:
 * 1. Intercepts a field resolver.
 * 2. Extracts the @secured directive from a field if present.
 * 3. Evaluates the role provided in `@secured` directive for field.
 * 4. If validation is `true`, original resolver function of the file is called.
 * 5. Else if validation is `false`,an [com.example.demo.exceptions.AccessDeniedError] is thrown and resolver function of field is **not invoked**
 */
@Component
class SecuredDirectiveWiring(directiveEvaluator: SecuredDirectiveValidator) : SchemaDirectiveWiring {
    private val directiveEvaluator: SecuredDirectiveValidator

    override fun onField(environment: SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition>): GraphQLFieldDefinition {
        val field = environment.element
        val parentType = environment.fieldsContainer

        // build a data fetcher that first checks authorisation roles before then calling the original data fetcher
        val originalDataFetcher = environment.codeRegistry.getDataFetcher(parentType, field)
        if (field.getDirective(SECURED_DIRECTIVE) == null) {
            return field
        }

        // get the role value from the directive argument from declaration
        val expressionValue = environment
            .getDirective(SECURED_DIRECTIVE)
            .getArgument(REQUIRES_ATTR)
            .argumentValue
            .value as graphql.language.StringValue

        // create an auth fetcher
        val authDataFetcher: DataFetcher<*> = DataFetcher { dataFetchingEnvironment: DataFetchingEnvironment ->
            val requestData = DgsContext.getRequestData(dataFetchingEnvironment)
            val authToken = requestData?.headers?.getFirst("X-AUTH-TOKEN")
            val resultBuilder = DataFetcherResult.newResult<Any>()
            val contextPath = "${parentType.name}.${field.name}.$SECURED_DIRECTIVE"

            val result = dataFetchingEnvironment.graphQlContext.get(contextPath) as? Boolean
                ?: directiveEvaluator.validate(expressionValue.value, authToken).also {
                    dataFetchingEnvironment.graphQlContext.put(contextPath, it)
                }

            // resolve the fetcher
            if (result) {
                originalDataFetcher[dataFetchingEnvironment]
            } else {
                val locations = listOf(field.definition.sourceLocation)
                val resultPath = dataFetchingEnvironment.executionStepInfo.path
                resultBuilder.error(AccessDeniedError(locations, resultPath)).build()
            }
        }

        // now change the field definition to have the new authorising data fetcher
        environment.codeRegistry.dataFetcher(parentType, field, authDataFetcher)
        return field
    }

    companion object {
        const val SECURED_DIRECTIVE = "secured"
        const val REQUIRES_ATTR = "requires"
    }

    init {
        this.directiveEvaluator = directiveEvaluator
    }
}