/*
 * Copyright 2021 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.demo.instrumentation

import graphql.ExecutionResult
import graphql.execution.instrumentation.InstrumentationContext
import graphql.execution.instrumentation.InstrumentationState
import graphql.execution.instrumentation.SimpleInstrumentation
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters
import graphql.schema.DataFetcher
import graphql.schema.GraphQLNonNull
import graphql.schema.GraphQLObjectType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

/**
 * Example Instrumentation class that prints the time each datafetcher takes.
 */
@Component
class ExampleTracingInstrumentation: SimpleInstrumentation() {

    val logger : Logger = LoggerFactory.getLogger(ExampleTracingInstrumentation::class.java)

    override fun createState(): InstrumentationState {
        return TraceState()
    }

    override fun beginExecution(parameters: InstrumentationExecutionParameters): InstrumentationContext<ExecutionResult> {
        val state: TraceState = parameters.getInstrumentationState()
        state.traceStartTime = System.nanoTime()

        return super.beginExecution(parameters)
    }

    override fun instrumentDataFetcher(dataFetcher: DataFetcher<*>, parameters: InstrumentationFieldFetchParameters): DataFetcher<*> {

        // We only care about user code
        if(parameters.isTrivialDataFetcher || parameters.executionStepInfo.path.toString().startsWith("/__schema")) {
            return dataFetcher
        }

        val dataFetcherName = findDatafetcherTag(parameters)

        return DataFetcher { environment ->
            val startTime = System.nanoTime()
            val result = dataFetcher.get(environment)
            if(result is CompletableFuture<*>) {
                result.whenComplete { _,_ ->
                    val totalTime = (System.nanoTime() - startTime) / 1000000.0
                    logger.info("Async datafetcher '{}' took {}ms", dataFetcherName, totalTime)
                }
            } else {
                val totalTime = (System.nanoTime() - startTime) / 1000000.0
                logger.info("Datafetcher '{}': {}ms", dataFetcherName, totalTime)
            }

            result
        }
    }

    override fun instrumentExecutionResult(executionResult: ExecutionResult, parameters: InstrumentationExecutionParameters): CompletableFuture<ExecutionResult> {
        val state: TraceState = parameters.getInstrumentationState()
        val totalTime = (System.nanoTime() - state.traceStartTime) / 1000000.0
        logger.info("Total execution time: {}ms", totalTime)

        return super.instrumentExecutionResult(executionResult, parameters)
    }

    private fun findDatafetcherTag(parameters: InstrumentationFieldFetchParameters): String {
        val type = parameters.executionStepInfo.parent.type
        val parentType = if (type is GraphQLNonNull) {
            type.wrappedType as GraphQLObjectType
        } else {
            type as GraphQLObjectType
        }

        return "${parentType.name}.${parameters.executionStepInfo.path.segmentName}"
    }

    data class TraceState(var traceStartTime: Long = 0): InstrumentationState
}