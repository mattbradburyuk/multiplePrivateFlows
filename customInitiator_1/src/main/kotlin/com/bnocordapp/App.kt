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
//@Path("template")
//class TemplateApi(val rpcOps: CordaRPCOps) {
//    // Accessible at /api/template/templateGetEndpoint.
//    @GET
//    @Path("templateGetEndpoint")
//    @Produces(MediaType.APPLICATION_JSON)
//    fun templateGetEndpoint(): Response {
//        return Response.ok("Template GET endpoint.").build()
//    }
//}

@Path("initiate")
class InitiateApi(val rpcOps: CordaRPCOps) {

    @GET
    @Path("custominitiator_1_topartyc")
    @Produces(MediaType.APPLICATION_JSON)
    fun PartyAtoC(): Response {

        val me = rpcOps.nodeInfo().legalIdentities.first().name
        val x500 = CordaX500Name("PartyC","Paris","FR")
        rpcOps.startFlow(::CustomInitiator_1, "CustomInitiator_1 data from a to c", x500).returnValue.get()

        return Response.ok("CustomInitiator_1 called from $me to $x500").build()
    }
    @GET
    @Path("custominitiator_1_topartyd")
    @Produces(MediaType.APPLICATION_JSON)
    fun PartyAtoD(): Response {

        val me = rpcOps.nodeInfo().legalIdentities.first().name
        val x500 = CordaX500Name("PartyD","Milan","IT")
        rpcOps.startFlow(::CustomInitiator_1, "CustomInitiator_1 data from a to d", x500).returnValue.get()

        return Response.ok("CustomInitiator_1 called from $me to $x500").build()
    }


}


// *********
// * Flows *
// *********


/**
 * Initiators for responders using flow inheritance
 */


@StartableByRPC
class CustomInitiator_1(data: String,  x500: CordaX500Name) : CommonInitiator(data, x500){

    override fun message(): String = "MB: CustomInitiator_1 called with data: $data"

}





// ***********
// * Plugins *
// ***********
class TemplateWebPlugin : WebServerPluginRegistry {
    // A list of lambdas that create objects exposing web JAX-RS REST APIs.
    override val webApis: List<Function<CordaRPCOps, out Any>> = listOf(Function(::InitiateApi))
    //A list of directories in the resources directory that will be served by Jetty under /web.
    // This template's web frontend is accessible at /web/template.
    override val staticServeDirs: Map<String, String> = mapOf(
        // This will serve the templateWeb directory in resources to /web/template
        "templatex" to javaClass.classLoader.getResource("templateWeb").toExternalForm()
    )
}

// Serialization whitelist.
class TemplateSerializationWhitelist : SerializationWhitelist {
    override val whitelist: List<Class<*>> = listOf(TemplateData::class.java)
}

// This class is not annotated with @CordaSerializable, so it must be added to the serialization whitelist, above, if
// we want to send it to other nodes within a flow.
data class TemplateData(val payload: String)
