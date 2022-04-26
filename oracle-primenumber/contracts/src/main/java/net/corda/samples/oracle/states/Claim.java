package net.corda.samples.oracle.states;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.samples.oracle.contracts.ClaimContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
@BelongsToContract(ClaimContract.class)

public class Claim implements LinearState {
    private String hospitalNumber;
    private String insuranceID;
    private final double amount; // Treatment cost.
    private final double insurancePay; // Cliam amount
    private final int count;
    private String treatmentReceiptNumber; // หมายเลขใบเสร็จค่ารักษา
    private final Party insurance; // who create paper insurance.
    private final Party hospital;
    private boolean isAccept;
    private final Party issuer;

    private final UniqueIdentifier linearId;

    // ALL Corda State required parameter to indicate storing parties
    private List<AbstractParty> participants;
    
    public Claim(double amount,
                 int count,
                 String hospitalNumber,
                 String insuranceID,
                 Party insurance,
                 Party hospital,
                 Party issuer,
                 UniqueIdentifier linearId){

        this.amount = amount;
        this.count = count;
        this.hospitalNumber = hospitalNumber;
        this.insuranceID = insuranceID;
        this.insurance = insurance;
        this.hospital = hospital;
        this.isAccept = false;
        this.issuer = issuer;
        this.insurancePay= 0;
        this.linearId = linearId;
        this.participants = new ArrayList<AbstractParty>();
        this.participants.add(hospital);
        this.participants.add(insurance);
    }
    @ConstructorForDeserialization
    public Claim(double amount,
                 int count,
                 String hospitalNumber,
                 String insuranceID,
                 Party insurance,
                 Party hospital,
                 boolean isAccept,
                 Party issuer,
                 UniqueIdentifier linearId){

        this.amount = amount;
        this.insurancePay = (1500> amount)? amount:1500.0;
        this.count = count;
        this.hospitalNumber = hospitalNumber;
        this.insuranceID = insuranceID;
        this.insurance = insurance;
        this.hospital = hospital;
        this.isAccept = isAccept;
        this.issuer = issuer;
        this.linearId = linearId;
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
    public int  getCount()  { return count; }

    public String getHospitalNumber() {
        return hospitalNumber;
    }

    public String getInsuranceID(){ return insuranceID;}

    public Party getInsurance() {
        return insurance;
    }

    public Party getHospital() {
        return hospital;
    }
    public double getInsurancePay() {
        return insurancePay;
    }
    public Claim acceptToCliam(boolean isAccept, Party issuer){
        Claim newClaim =  new Claim(this.amount,this.count,this.hospitalNumber,this.insuranceID, this.insurance,this.hospital,isAccept,issuer,this.linearId);
        return newClaim;
    }


    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }
}
