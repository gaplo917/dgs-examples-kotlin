package com.example.demo.exceptions

import graphql.ErrorType
import graphql.GraphQLError
import graphql.execution.ResultPath
import graphql.language.SourceLocation


/**
 * Represents error condition where an unauthorised user tries to access protected content
 */
class AccessDeniedError(locations: List<SourceLocation>, resultPath: ResultPath) : GraphQLError {
    private val message: String
    private val locations: List<SourceLocation>
    private val resultPath: ResultPath
    override fun getMessage(): String {
        return message
    }

    override fun getLocations(): List<SourceLocation> {
        return locations
    }

    override fun getPath(): List<Any> {
        return resultPath.toList()
    }

    override fun getErrorType(): ErrorType {
        return ErrorType.DataFetchingException
    }

    override fun getExtensions(): Map<String, Any> {
        val errorsMap: MutableMap<String, Any> = HashMap()
        errorsMap["errorType"] = ERROR
        errorsMap["message"] = "errors.unauthorizedAccess"
        return errorsMap
    }

    companion object {
        private const val ERROR = "UNAUTHORIZED_ACCESS"
    }

    init {
        message = "Exception while fetching data (" + resultPath.segmentToString() + "): not authorized"
        this.locations = locations
        this.resultPath = resultPath
    }
}