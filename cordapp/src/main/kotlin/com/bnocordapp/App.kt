package com.bnocordapp

import co.paralleluniverse.fibers.Suspendable
import com.bnocordapp.TemplateContract
import com.bnocordapp.TemplateState
import net.corda.core.contracts.StateRef
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
    @Path("commoninitiator_topartyc")
    @Produces(MediaType.APPLICATION_JSON)
    fun PartyAtoC(): Response {

        val me = rpcOps.nodeInfo().legalIdentities.first().name
        val x500 = CordaX500Name("PartyC","Paris","FR")
        val route = "${me.organisation} to ${x500.organisation}"

        rpcOps.startFlow(::CommonInitiator, "CommonInitiator data from $route", x500).returnValue.get()

        return Response.ok("CommonInitiator called from $route").build()
    }

    @GET
    @Path("commoninitiator_topartyd")
    @Produces(MediaType.APPLICATION_JSON)
    fun PartyAtoD(): Response {

        val me = rpcOps.nodeInfo().legalIdentities.first().name
        val x500 = CordaX500Name("PartyD","Milan","IT")
        val route = "${me.organisation} to ${x500.organisation}"

        rpcOps.startFlow(::CommonInitiator, "CommonInitiator data from $route", x500).returnValue.get()

        return Response.ok("CommonInitiator called from $route").build()
    }

}

@Path("vault")
class VaultApi(val rpcOps: CordaRPCOps) {
    // Accessible at /api/template/templateGetEndpoint.
    @GET
    @Path("getstates")
    @Produces(MediaType.APPLICATION_JSON)
    fun getVaultStates(): Response {

        val result = rpcOps.vaultQuery(TemplateState::class.java)

//        println("result: ")
//
//        val num_results = result.states.size
//
//        val list = mutableListOf<String>()
//
//        for(i in 0..num_results){
//
//            list.add(i, "${result.statesMetadata[i].recordedTime}:  ${result.states[i].state.data.data} /n")
//        }
//
//
//        return Response.ok(list).build()


        val response = result.states.asReversed().map { "${it.state.data.data} \n "}.toString()

        return Response.ok(response).build()
    }
}


// *********
// * Flows *
// *********


/**
 * Initiators for responders using flow inheritance
 */

@StartableByRPC
@InitiatingFlow
open class CommonInitiator(val data: String, val x500: CordaX500Name) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Flow implementation goes here

        logger.info(message())


        val me: Party = serviceHub.myInfo.legalIdentities.single()
        val partyOrNull: Party? = serviceHub.networkMapCache.getPeerByLegalName(x500)

//        logger.info("MB: partyorNull = $partyCOrNull")

        if (partyOrNull != null) {
            logger.info("MB: Party $x500 found")
        } else {
            logger.info("MB: artyC $x500 not found")
            throw(FlowException("MB: Party $x500 not Found"))
        }
        val party: Party = partyOrNull
        val state = TemplateState(data, listOf(me, party))

        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val tx = TransactionBuilder(notary)
        tx.addOutputState(state, TemplateContract.ID)
        tx.addCommand(TemplateContract.Commands.Action(), me.owningKey, party.owningKey)
        tx.verify(serviceHub)

        val ptx = serviceHub.signInitialTransaction(tx)
        val session = initiateFlow(party)
        val stx = subFlow(CollectSignaturesFlow(ptx, listOf(session)))
        val ftx = subFlow(FinalityFlow(stx))

    }

    open fun message(): String = "\nMB: CommonInitiator called with data: $data\n"
}


/**
 * Responders using flow inheritance
 */


//@InitiatedBy(CommonInitiator::class)
//class CustomResponder_1(counterpartySession: FlowSession) : CommonResponder(counterpartySession) {
//    init {
//        println("\nCustomResponder_1 called\n")
//    }
//    override fun responderType(): String = "CustomResponder_1"
//}

@InitiatedBy(CommonInitiator::class)
open class CommonResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {

    open fun responderType(): String = "CommonResponder"

    @Suspendable
    override fun call() {

        logger.info("\nMB: ${responderType()} flow for ${serviceHub.myInfo.legalIdentities.single().name.organisation} responding to initiated flow from ${counterpartySession.counterparty.name.organisation }\n")


        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                val state = stx.tx.toLedgerTransaction(serviceHub).outputStates[0] as TemplateState
                logger.info("\nMB: data package received: ${state.data}\n")

                "This must be a Template transaction" using (output is TemplateState)
            }
        }


        subFlow(signedTransactionFlow)

    }
}




// ***********
// * Plugins *
// ***********
class TemplateWebPlugin : WebServerPluginRegistry {
    // A list of lambdas that create objects exposing web JAX-RS REST APIs.
    override val webApis: List<Function<CordaRPCOps, out Any>> = listOf(Function(::TemplateApi), Function(::InitiateApi),Function(::VaultApi))
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
