package com.example.demo.schemawiring.secured

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTDecodeException
import com.auth0.jwt.interfaces.DecodedJWT
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Evaluates a SpEL expression
 */
@Component
class SecuredDirectiveValidator {

    @Value("\${jwt.secret}")
    private lateinit var jwtSecret: String

    private val jwtVerifier by lazy {
        JWT.require(Algorithm.HMAC256(jwtSecret)).build()
    }

    /**
     * Validate the scope inside the jwt token
     */
    fun validate(role: String?, authToken: String?): Boolean {
        println("testing")
        return verifyJwt(token = authToken, role = role)
    }

    fun verifyJwt(token: String?, role: String?): Boolean {
        if(token == null || role == null) {
            return false
        }
        return try {
            val jwt: DecodedJWT = jwtVerifier.verify(token)
            val roles = jwt.claims["scopes"]?.asArray(String::class.java)
            roles != null && roles.contains(role)
        } catch (e: JWTDecodeException) {
            false
        }
    }
}
