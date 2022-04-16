package com.tutorial.flows;
import co.paralleluniverse.fibers.Suspendable;
import com.tutorial.contracts.ClaimContract;
import com.tutorial.states.Claim;

import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.util.Arrays;
import java.util.UUID;

public class AcceptClaimFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class AcceptClaimInitiator extends FlowLogic<SignedTransaction>{

        private Party hostpital;
        private UniqueIdentifier claimId;

        public AcceptClaimInitiator(Party hostpital, UniqueIdentifier claimId) {
            this.hostpital = hostpital;
            this.claimId = claimId;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            //Query the AppleStamp
            QueryCriteria.LinearStateQueryCriteria inputCriteria = new QueryCriteria.LinearStateQueryCriteria()
                    .withUuid(Arrays.asList(UUID.fromString(claimId.toString())))
                    .withStatus(Vault.StateStatus.UNCONSUMED)
                    .withRelevancyStatus(Vault.RelevancyStatus.RELEVANT);
            StateAndRef ClaimStateAndRef = getServiceHub().getVaultService().queryBy(Claim.class, inputCriteria).getStates().get(0);

            Claim originalClaim = (Claim) ClaimStateAndRef.getState().getData();

            //Modify output to address the owner change
            Claim output = originalClaim.acceptToCliam(true , this.getOurIdentity());

            /* Obtain a reference to a notary we wish to use.*/
            Party notary = ClaimStateAndRef.getState().getNotary();

            //Build Transaction
            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addInputState(ClaimStateAndRef)
                    .addOutputState(output, ClaimContract.ID)
                    .addCommand(new ClaimContract.Commands.AcceptClaim(),
                            Arrays.asList(getOurIdentity().getOwningKey(),this.hostpital.getOwningKey()));

            // Verify that the transaction is valid.
            txBuilder.verify(getServiceHub());

            // Sign the transaction.
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            // Send the state to the counterparty, and receive it back with their signature.
            FlowSession otherPartySession = initiateFlow(hostpital);
            final SignedTransaction fullySignedTx = subFlow(
                    new CollectSignaturesFlow(partSignedTx, Arrays.asList(otherPartySession)));

            // Notarise and record the transaction in both parties' vaults.
            SignedTransaction result = subFlow(new FinalityFlow(fullySignedTx, Arrays.asList(otherPartySession)));

            return result;
        }
    }

    @InitiatedBy(AcceptClaimFlow.AcceptClaimInitiator.class)
    public static class AcceptClaimResponder extends FlowLogic<Void>{
        //private variable
        private FlowSession counterpartySession;

        public AcceptClaimResponder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {
            SignedTransaction signedTransaction = subFlow(new SignTransactionFlow(counterpartySession) {
                @Override
                protected void checkTransaction(SignedTransaction stx) throws FlowException {
                }
            });

            //Stored the transaction into data base.
            subFlow(new ReceiveFinalityFlow(counterpartySession, signedTransaction.getId()));
            return null;
        }
    }
}
//flow start AcceptClaimInitiator hostpital: hostpitalA, claimId:
//run vaultQuery contractStateType: com.tutorial.states.Claim