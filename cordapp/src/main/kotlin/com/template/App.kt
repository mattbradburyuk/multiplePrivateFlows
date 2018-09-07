package com.template

import co.paralleluniverse.fibers.Suspendable
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
    @Path("partyA")
    @Produces(MediaType.APPLICATION_JSON)
    fun PartyAEndpoint(): Response {

//        rpcOps.startFlow(::Initiator_A).returnValue.get()

        return Response.ok("partyA Initiator called").build()
    }



}

@Path("vault")
class VaultApi(val rpcOps: CordaRPCOps) {
    // Accessible at /api/template/templateGetEndpoint.
    @GET
    @Path("getStates")
    @Produces(MediaType.APPLICATION_JSON)
    fun templateGetEndpoint(): Response {

        val states = rpcOps.vaultQuery(TemplateState::class.java)

        return Response.ok(states).build()
    }
}


// *********
// * Flows *
// *********


/**
 * Initiators for responders using flow inheritance
 */


@StartableByRPC
class Initiator_A: CommonInitiator("PartyA data"){

}


@InitiatingFlow
open class CommonInitiator(val data: String) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Flow implementation goes here

        logger.info("MB: CommonInitiator called with data: $data")

        val partyCX500 = CordaX500Name("PartyC","Paris","FR")
        val me: Party = serviceHub.myInfo.legalIdentities.single()
        val partyCOrNull: Party? = serviceHub.networkMapCache.getPeerByLegalName(partyCX500)

        logger.info("MB: partyCorNull = $partyCOrNull")

        if (partyCOrNull != null) {
            logger.info("PartyC found")
        } else {
            logger.info("PartyC not found")
            throw(FlowException("PartyC not Found"))
        }
        val partyC: Party = partyCOrNull!!
        val state = TemplateState(data, listOf(me, partyC))

        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val tx = TransactionBuilder(notary)
        tx.addOutputState(state, TemplateContract.ID)
        tx.addCommand(TemplateContract.Commands.Action(), me.owningKey, partyC.owningKey)
        tx.verify(serviceHub)

        val ptx = serviceHub.signInitialTransaction(tx)
        val session = initiateFlow(partyC)
        val stx = subFlow(CollectSignaturesFlow(ptx, listOf(session)))
        val ftx = subFlow(FinalityFlow(stx))

    }
}


/**
 * Responders using flow inheritance
 */


@InitiatedBy(CommonInitiator::class)
class Responder_A(counterpartySession: FlowSession) : CommonResponder(counterpartySession){
    init {
        println("MB: Responder_A called")
    }
}


@InitiatedBy(CommonInitiator::class)
open class CommonResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    init {
        println("MB: CommonResponder called")
    }


    @Suspendable
    override fun call() {

        logger.info("MB:  ${serviceHub.myInfo.legalIdentities.single().name} Responder flow called from: ${counterpartySession.counterparty.name }")

        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be a Template transaction" using (output is TemplateState)
            }
        }

        subFlow(signedTransactionFlow)

    }
}


/**
 * responders using subflows
 */



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
