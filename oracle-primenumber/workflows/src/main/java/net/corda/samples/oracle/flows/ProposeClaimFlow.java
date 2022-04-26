package net.corda.samples.oracle.flows;
import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Command;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.transactions.FilteredTransaction;
import net.corda.samples.oracle.contracts.ClaimContract;
import net.corda.samples.oracle.contracts.PrimeContract;
import net.corda.samples.oracle.states.Claim;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.contracts.UniqueIdentifier;

import java.util.Arrays;
public class ProposeClaimFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class ProposeClaimInitiator  extends FlowLogic<SignedTransaction>{

        private String insuranceID; //เลขประกัน
        private Party insurance;  // insurance Party
        private double amount;
        private String hospitalNumber;

        public ProposeClaimInitiator(String hospitalNumber,String insuranceID,Party insurance, double amount ) {
            this.hospitalNumber = hospitalNumber;
            this.insuranceID = insuranceID;
            this.insurance = insurance;
            this.amount = amount;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            /* Obtain a reference to a notary we wish to use.
            /** Explicit selection of notary by CordaX500Name - argument can by coded in flows or parsed from config (Preferred)*/
            //final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=Bangkok,C=TH"));

            //
            //oracle เพื่อรับ count
            CordaX500Name oracleName = new CordaX500Name("Oracle", "Bangkok", "TH");
            Party oracle = getServiceHub().getNetworkMapCache().getNodeByLegalName(oracleName)
                    .getLegalIdentities().get(0);
            if (oracle == null) {
                throw new IllegalArgumentException("Requested oracle");
            }
            //

            //Building the output ClaimState
            int count = subFlow(new QueryClaim(oracle,this.insuranceID));

            UniqueIdentifier uniqueID = new UniqueIdentifier();
            System.out.println("ClaimID: " + uniqueID);
            Claim newClaim = new Claim(this.amount,count,this.hospitalNumber,this.insuranceID,this.insurance,this.getOurIdentity(),this.getOurIdentity(),uniqueID);
            //Compositing the transaction
            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addOutputState(newClaim)
                    .addCommand(new ClaimContract.Commands.CreateClaim(this.insuranceID,count),
                            Arrays.asList(oracle.getOwningKey(),getOurIdentity().getOwningKey(),insurance.getOwningKey()));
            // Verify that the transaction is valid.
            txBuilder.verify(getServiceHub());

            // Sign the transaction.
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            FilteredTransaction ftx = partSignedTx.buildFilteredTransaction(o -> {
                if (o instanceof Command && ((Command) o).getSigners().contains(oracle.getOwningKey())
                        && ((Command) o).getValue() instanceof ClaimContract.Commands.CreateClaim) {
                    return true;
                }
                return false;
            });

            TransactionSignature oracleSignature = subFlow(new SignPrime(oracle, ftx));
            SignedTransaction stx = partSignedTx.withAdditionalSignature(oracleSignature);

            // Send the state to the counterparty, and receive it back with their signature.
            FlowSession otherPartySession = initiateFlow(insurance);
            final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(stx, Arrays.asList(otherPartySession)));
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(new FinalityFlow(fullySignedTx, Arrays.asList(otherPartySession)));
        }

    }
    @InitiatedBy(ProposeClaimInitiator.class)
    public static class ProposeClaimResponder extends FlowLogic<Void>{

        //private variable
        private FlowSession counterpartySession;

        public ProposeClaimResponder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {
            SignedTransaction signedTransaction = subFlow(new SignTransactionFlow(counterpartySession) {
                @Override
                @Suspendable
                protected void checkTransaction(SignedTransaction stx) throws FlowException {
                    /*
                     * SignTransactionFlow will automatically verify the transaction and its signatures before signing it.
                     * However, just because a transaction is contractually valid doesn’t mean we necessarily want to sign.
                     * What if we don’t want to deal with the counterparty in question, or the value is too high,
                     * or we’re not happy with the transaction’s structure? checkTransaction
                     * allows us to define these additional checks. If any of these conditions are not met,
                     * we will not sign the transaction - even if the transaction and its signatures are contractually valid.
                     * ----------
                     * For this hello-world cordapp, we will not implement any additional checks.
                     * */
                }
            });

            //Stored the transaction into data base.
            subFlow(new ReceiveFinalityFlow(counterpartySession, signedTransaction.getId()));
            return null;
        }
    }

}
//flow start ProposeClaimInitiator hospitalNumber: 002715, insuranceID: A0840672, insurance: InsuranceA, amount: 1000.0
//run vaultQuery contractStateType: net.corda.samples.oracle.states.Claim

// insuranceID:A0840659