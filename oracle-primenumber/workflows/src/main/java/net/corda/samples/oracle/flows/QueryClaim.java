package net.corda.samples.oracle.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.identity.Party;

@InitiatingFlow
public class QueryClaim extends FlowLogic<Integer>{
    private final Party oracle;
    private final String nameCustomer;
    public QueryClaim(Party oracle, String nameCustomer) {
        this.oracle = oracle;
        this.nameCustomer = nameCustomer;
    }
    @Suspendable
    @Override
    public Integer call() throws FlowException {
        return initiateFlow(oracle).sendAndReceive(Integer.class, nameCustomer).unwrap(it -> it);
    }
}
