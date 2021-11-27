package com.example.demo.schemawiring.secured

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsRuntimeWiring
import graphql.schema.idl.RuntimeWiring

@DgsComponent
class SecuredDirectiveRegistration(private val securedDirectiveWiring: SecuredDirectiveWiring) {
    /**
     * Registers schema directive wiring for `@secured` directive.
     *
     * @param builder
     * @return RuntimeWiring.Builder
     */
    @DgsRuntimeWiring
    fun addSecuredDirective(builder: RuntimeWiring.Builder): RuntimeWiring.Builder {
        return builder.directive(SecuredDirectiveWiring.SECURED_DIRECTIVE, securedDirectiveWiring)
    }
}