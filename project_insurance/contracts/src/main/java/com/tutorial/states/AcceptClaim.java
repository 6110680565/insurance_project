package com.tutorial.states;
import com.tutorial.contracts.ClaimContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@BelongsToContract(ClaimContract.class)

public class AcceptClaim implements LinearState {
    private final double amount; // Amount of treatment cost.

    private final int count;
    private final String customerName;
    private final Party insurance; // who create paper insurance.
    private final Party hospital;
    private final Party issuer;
    private final double insurancePay;
    private final UniqueIdentifier linearId;


    // ALL Corda State required parameter to indicate storing parties
    private List<AbstractParty> participants;

    @ConstructorForDeserialization
    public AcceptClaim(Claim proposeClaim, Party issuer ){

        this.amount = proposeClaim.getAmount();
        this.insurancePay = (1500> proposeClaim.getAmount())? proposeClaim.getAmount():1500.0;
        this.count = proposeClaim.getCount();
        this.customerName = proposeClaim.getCustomerName();
        this.insurance = proposeClaim.getInsurance();
        this.hospital = proposeClaim.getHospital();
        this.issuer = issuer;
        this.linearId = proposeClaim.getLinearId();
        this.participants = new ArrayList<AbstractParty>();
        this.participants.add(hospital);
        this.participants.add(insurance);

    }


    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return participants;
    }

    public double getAmount() {
        return amount;
    }

    public double getInsurancePay() {
        return insurancePay;
    }

    public int  getCount()  { return count; }

    public String getCustomerName() {
        return customerName;
    }

    public Party getInsurance() {
        return insurance;
    }

    public Party getHospital() {
        return hospital;
    }

    public Party getIssuer() {
        return issuer;
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }
}
