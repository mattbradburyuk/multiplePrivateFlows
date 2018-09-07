package com.customInitiator_1

import co.paralleluniverse.fibers.Suspendable
import com.bnocordapp.CommonInitiator
import com.bnocordapp.TemplateState
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.serialization.SerializationWhitelist
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.webserver.services.WebServerPluginRegistry
import java.util.function.Function
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
//import com.bnocordapp.

// *****************
// * API Endpoints *
// *****************
@Path("template")
class TemplateApi(val rpcOps: CordaRPCOps) {
    // Accessible at /api/template/templateGetEndpoint.
    @GET
    @Path("templateGetEndpoint")
    @Produces(MediaType.APPLICATION_JSON)
    fun templateGetEndpoint(): Response {
        return Response.ok("Template GET endpoint.").build()
    }
}

@Path("initiate")
class InitiateApi(val rpcOps: CordaRPCOps) {

    @GET
    @Path("custominitiator_1")
    @Produces(MediaType.APPLICATION_JSON)
    fun PartyAEndpoint(): Response {

        rpcOps.startFlow(::CustomInitiator_1).returnValue.get()

        return Response.ok("partyA Initiator called").build()
    }



}

//@Path("vault")
//class VaultApi(val rpcOps: CordaRPCOps) {
//    // Accessible at /api/template/templateGetEndpoint.
//    @GET
//    @Path("getStates")
//    @Produces(MediaType.APPLICATION_JSON)
//    fun templateGetEndpoint(): Response {
//
//        val states = rpcOps.vaultQuery(TemplateState::class.java)
//
//        return Response.ok(states).build()
//    }
//}


// *********
// * Flows *
// *********


/**
 * Initiators for responders using flow inheritance
 */


@StartableByRPC
class CustomInitiator_1(data: String,  x500: CordaX500Name) : CommonInitiator(data, x500)





// ***********
// * Plugins *
// ***********
class TemplateWebPlugin : WebServerPluginRegistry {
    // A list of lambdas that create objects exposing web JAX-RS REST APIs.
    override val webApis: List<Function<CordaRPCOps, out Any>> = listOf(Function(::TemplateApi), Function(::InitiateApi))
    //A list of directories in the resources directory that will be served by Jetty under /web.
    // This template's web frontend is accessible at /web/template.
    override val staticServeDirs: Map<String, String> = mapOf(
        // This will serve the templateWeb directory in resources to /web/template
        "template" to javaClass.classLoader.getResource("templateWeb").toExternalForm()
    )
}

// Serialization whitelist.
class TemplateSerializationWhitelist : SerializationWhitelist {
    override val whitelist: List<Class<*>> = listOf(TemplateData::class.java)
}

// This class is not annotated with @CordaSerializable, so it must be added to the serialization whitelist, above, if
// we want to send it to other nodes within a flow.
data class TemplateData(val payload: String)
