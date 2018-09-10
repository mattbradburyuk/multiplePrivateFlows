package com.customResponder_1


import com.bnocordapp.CommonInitiator
import com.bnocordapp.CommonResponder
import net.corda.core.flows.*
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.serialization.SerializationWhitelist
import net.corda.webserver.services.WebServerPluginRegistry
import java.util.function.Function



// *****************
// * API Endpoints *
// *

// *********
// * Flows *
// *********


/**
 * Initiators for responders using flow inheritance
 */



@InitiatedBy(CommonInitiator::class)
open class customResponder_1(counterpartySession: FlowSession) : CommonResponder(counterpartySession) {
    init {
        println("MB: CustomResponder_1 called")
    }
}

// ***********
// * Plugins *
// ***********
class TemplateWebPlugin : WebServerPluginRegistry {
    // A list of lambdas that create objects exposing web JAX-RS REST APIs.
    override val webApis: List<Function<CordaRPCOps, out Any>> = listOf()
    //A list of directories in the resources directory that will be served by Jetty under /web.
    // This template's web frontend is accessible at /web/template.
    override val staticServeDirs: Map<String, String> = mapOf(
        // This will serve the templateWeb directory in resources to /web/template
        "templatey" to javaClass.classLoader.getResource("templateWeb").toExternalForm()
    )
}

// Serialization whitelist.
class TemplateSerializationWhitelist : SerializationWhitelist {
    override val whitelist: List<Class<*>> = listOf(TemplateData::class.java)
}

// This class is not annotated with @CordaSerializable, so it must be added to the serialization whitelist, above, if
// we want to send it to other nodes within a flow.
data class TemplateData(val payload: String)
