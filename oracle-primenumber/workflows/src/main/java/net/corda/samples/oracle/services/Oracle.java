package net.corda.samples.oracle.services;

import net.corda.core.contracts.Command;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.node.ServiceHub;
import net.corda.core.node.services.CordaService;
import net.corda.core.serialization.SingletonSerializeAsToken;
import net.corda.core.transactions.ComponentVisibilityException;
import net.corda.core.transactions.FilteredTransaction;
import net.corda.core.transactions.FilteredTransactionVerificationException;
import net.corda.samples.oracle.contracts.ClaimContract;
import net.corda.samples.oracle.contracts.PrimeContract;


import java.net.HttpURLConnection;
import java.net.URL;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.security.PublicKey;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;


// We sub-class 'SingletonSerializeAsToken' to ensure that instances of this class are never serialised by Kryo.
// When a flows is check-pointed, the annotated @Suspendable methods and any object referenced from within those
// annotated methods are serialised onto the stack. Kryo, the reflection based serialisation framework we use, crawls
// the object graph and serialises anything it encounters, producing a graph of serialised objects.
// This can cause issues. For example, we do not want to serialise large objects on to the stack or objects which may
// reference databases or other external services (which cannot be serialised!). Therefore we mark certain objects with
// tokens. When Kryo encounters one of these tokens, it doesn't serialise the object. Instead, it creates a
// reference to the type of the object. When flows are de-serialised, the token is used to connect up the object
// reference to an instance which should already exist on the stack.
@CordaService
public class Oracle extends SingletonSerializeAsToken {

    static class MaxSizeHashMap<K, V> extends LinkedHashMap<K, V> {
        private final Integer maxSize;

        public MaxSizeHashMap() {
            this.maxSize = 1024;
        }

        public MaxSizeHashMap(int maxSize) {
            this.maxSize = maxSize;
        }

        @Override
        public boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return this.size() > maxSize;
        }
    }

    private final ServiceHub services;
    // Set the types of this to whatever query() takes and returns
    private final MaxSizeHashMap<Integer, Integer> cache = new MaxSizeHashMap<>();
    private final PublicKey myKey;

    public Oracle(ServiceHub services) {
        this.services = services;
        this.myKey = services.getMyInfo().getLegalIdentities().get(0).getOwningKey();
    }

    // The reason why prime numbers were chosen is because they are easy to reason about and reduce the mental load
    // for this tutorial application.
    // Clearly, most developers can generate a list of primes and all but the largest prime numbers can be verified
    // deterministically in reasonable time. As such, it would be possible to add a constraint in the
    // [PrimeContract.verify] function that checks the nth prime is indeed the specified number.
    public Integer query(Integer n) {
        if (n < 1) {
            throw new IllegalArgumentException("n must be at least one.");
        }

        return getNthPrime(n);
    }
    public Integer query2(String insuranceID) {
        if (insuranceID == "") {
            throw new IllegalArgumentException("nameCustomer must be at least one Character.");
        }

        return getCountCliam(insuranceID);
    }

    // Signs over a transaction if the specified Nth prime for a particular N is correct.
    // This function takes a filtered transaction which is a partial Merkle tree. Any parts of the transaction which
    // the oracle doesn't need to see in order to verify the correctness of the nth prime have been removed. In this
    // case, all but the [PrimeContract.Create] commands have been removed. If the Nth prime is correct then the oracle
    // signs over the Merkle root (the hash) of the transaction.
    public TransactionSignature sign(FilteredTransaction ftx) throws FilteredTransactionVerificationException {
        // Check the partial Merkle tree is valid.
        ftx.verify();

        // Is it a Merkle tree we are willing to sign over?
        boolean isValidMerkleTree = ftx.checkWithFun(this::isCommandWithCorrectPrimeAndIAmSigner);
        try {
            /**
             * Function that checks if all of the commands that should be signed by the input public key are visible.
             * This functionality is required from Oracles to check that all of the commands they should sign are visible.
             */
            ftx.checkCommandVisibility(services.getMyInfo().getLegalIdentities().get(0).getOwningKey());
        } catch (ComponentVisibilityException e) {
            e.printStackTrace();
        }

        if (isValidMerkleTree) {
            return services.createSignature(ftx, myKey);
        } else {
            throw new IllegalArgumentException("Oracle signature requested over invalid transaction.");
        }
    }

    /**
     * Returns true if the component is an Create command that:
     * - States the correct prime
     * - Has the oracle listed as a signer
     */
    private boolean isCommandWithCorrectPrimeAndIAmSigner(Object elem) {
        if (elem instanceof Command && ((Command) elem).getValue() instanceof PrimeContract.Commands.Create) {
            PrimeContract.Commands.Create cmdData = (PrimeContract.Commands.Create) ((Command) elem).getValue();
            return (((Command) elem).getSigners().contains(myKey) && query(cmdData.getN()).equals(cmdData.getNthPrime()));
        }
        else if (elem instanceof Command && ((Command) elem).getValue() instanceof ClaimContract.Commands.CreateClaim) {
            ClaimContract.Commands.CreateClaim cmdData = (ClaimContract.Commands.CreateClaim) ((Command) elem).getValue();
            return (((Command) elem).getSigners().contains(myKey) && query2(cmdData.getInsuranceID()).equals(cmdData.getCount()));
        }
        return false;
    }

    // generates prime number
    private Integer getNthPrime(Integer n) {
        int count = 0, num = 0, i = 0;
        while (count < n) {
            num = num + 1;
            for (i = 2; i <= num; i++) { //Here we will loop from 2 to num
                if (num % i == 0) {
                    break;
                }
            }
            if (i == num) {//if it is a prime number
                count = count + 1;
            }
        }
        return num;
    }

    // generates countClaim
    private Integer getCountCliam(String insuranceID) {
        int count = -1;
        try {

            URL url = new URL("http://localhost:3000/getcount/"+insuranceID);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();

            //Getting the response code
            int responsecode = conn.getResponseCode();

            if (responsecode != 200) {
                throw new RuntimeException("HttpResponseCode: " + responsecode);
            } else {

                String inline = "";
                Scanner scanner = new Scanner(url.openStream());

                //Write all the JSON data into a string using a scanner
                while (scanner.hasNext()) {
                    inline += scanner.nextLine();
                }

                //Close the scanner
                scanner.close();

                //Using the JSON simple library parse the string into a json object
                JSONParser jParse = new JSONParser();
                JSONObject json_data = (JSONObject) jParse.parse(inline);
                System.out.println(json_data);
                //Get the required object from the above created object
                System.out.println(json_data.get("countClaim").getClass().getSimpleName());
                Long count1 = (Long) json_data.get("countClaim");
                //Get the required data using its key
                count = count1.intValue();


            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }
}
