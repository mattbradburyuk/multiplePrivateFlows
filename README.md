

# Investigate adding Custom initiators and responders to override base (common) flows

Note, only the initiators worked reliably.

## Objective: 

- Create a cordapp (cordapp) with an initiating (commonInitiator) and initiatedBy (CustomResponder_1) Flow'
- In a separate module and package create a custom initiator (custom_Initiator_1)
- In a separate module and package create a custom responder (customResponder_1)
- When a node loads the custom modules they override the repective CommonInitiator and/or CommonResponder flows from the main cordapp 

## Findings - Initiator

The following pattern worked

In Cordapp module/ bnocorapp package:
```kotlin
@StartableByRPC
@InitiatingFlow
open class CommonInitiator(val data: String, val x500: CordaX500Name) : FlowLogic<Unit>() {
    // do stuff
}

```

In CustomInitiator_1 module/ customInitiator_1 package:
```kotlin
@StartableByRPC
class CustomInitiator_1(data: String,  x500: CordaX500Name) : CommonInitiator(data, x500){
    // do stuff
}
```

It looks like the subclass inherits the @InitiatingFlow annotation from its parent

the class finder finds both classes because they have different names (in contrast to below for the responder)


## Findings - Responder

The following pattern had indeterminate behavior (sometimes worked, sometimes didn't)

In Cordapp module/ bnocorapp package:
```kotlin
@InitiatedBy(CommonInitiator::class)
open class CommonResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    //do stuff
}
```

In CustomResponder_1 module /package:
```kotlin
@InitiatedBy(CommonInitiator::class)
class CustomResponder_1(counterpartySession: FlowSession) : CommonResponder(counterpartySession) {
    // do stuff
}
```
On investigation: 

- If the sub class was in the same package / module CustomRepsonder_1 would replace commonResponder
- If the sub class was in a different package / module (as above) and the name of the CustomRepsonder_1 would not replace commonResponder

- If the sub class was in a different package / module but the name of the module/ package was prefixed with an 'a' to make it alphabetically before the cordapp module (or bnocordapp package?)) the CustomRepsonder_1 would replace commonResponder

Hypothesis for whats going on: 

When the node scans the jars to pair up the initiating and intiatedBy flows. It is scanning each jar at a time in alphabetical order. When 'cordapp' is first it finds the @InitiatedBy(CommonInitiator::class) on the CommonResponder first and loads that. when it finds the CustomResponder_1 later, it ignors it.

Conversely, when the name is modified to 'aCustomResponder_1' it finds the @InitiatedBy(CommonInitiator::class) on the CustomResponder_1 first and loads that. when it finds the CommonResponder  later, it ignors it. However, this second behavior gives the ight result, because where ever the customRepsponder_1 jar is present on the node, the desired behaviour is to override the CommonResponder.

Conclusion and follow on:

- The above Custom Responder pattern should not be used as relying on alphabetical ordering of Classes is not a robust approach

- Raise a corda bug fix for the indeterminate behaviour

- Raise a request for interfaces in the @initiatedBy() annotation so that flows can subscribe to an interface rather than have to point to a concrete class with the @InitiatingFlow annotation.


## Getting Set Up

To get started, clone this repository with:

    git clone https://github.com/mattbradburyuk/multipleInitiators.git

And change directories to the newly cloned repo

     

## Building the CorDapp template:

**Unix:** 

     ./gradlew deployNodes

**Windows:**

     gradlew.bat deployNodes

Note: You'll need to re-run this build step after making any changes to
the template for these to take effect on the node.

If it goes wrong try 
    
    killall java -9
    
    ./gradlew clean
    
then retry to buildNodes

## Running the Nodes

Once the build finishes, change directories to the folder where the newly
built nodes are located:

     cd build/nodes

The Gradle build script will have created a folder for each node. You'll
see three folders, one for each node and a `runnodes` script. You can
run the nodes with:

**Unix:**

     ./runnodes

**Windows:**

    runnodes.bat

You should now have four Corda nodes running on your machine serving 
the template.


## Interacting with the CorDapp via HTTP

The nodes can be found using the following port numbers, defined in 
`build.gradle`, as well as the `node.conf` file for each node found
under `build/nodes/partyX`:

     PartyA: localhost:10007
     PartyB: localhost:10010
     PartyC: localhost:10013 
     PartyC: localhost:10016 

## http calls

go to root for each node and you can select your action

PartyA (Common initiator and responder only): http://localhost:10007
PartyB (add CustomInitiator_1): http://localhost:10010
PartyC (Common initiator and responder only): http://localhost:10013
PartyD (CustomResponder_1): http://localhost:10016

You will need to look at the logs to see the logs. All logging lines called from this code have the prefix 'MB:'

## 

(Note, this repo is based on the Cordapp Template from R3: https://github.com/corda/cordapp-template-kotlin)