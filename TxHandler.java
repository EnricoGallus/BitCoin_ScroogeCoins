import java.util.ArrayList;
import java.util.HashSet;

public class TxHandler {

    private UTXOPool UtxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        UtxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {

        HashSet<UTXO> currentUtxos = new HashSet<UTXO>();
        double inputSum = 0;
        double outputSum = 0;

        for (int i = 0; i < tx.numInputs(); i++){
            Transaction.Input input = tx.getInput(i);
            UTXO toValidateUtxo = new UTXO(input.prevTxHash, input.outputIndex);
            // (3) no UTXO is claimed multiple times
            if (currentUtxos.contains(toValidateUtxo))
            {
                return false;
            }

            currentUtxos.add(toValidateUtxo);
            // (1) all outputs claimed are in the current UTXO pool
            if (!UtxoPool.contains(toValidateUtxo))
            {
                return false;
            }

            Transaction.Output output = tx.getOutput(input.outputIndex);
            if (output == null)
            {
                // isn't this possible? shouldn't we handle this case?
                return false;
            }

            inputSum += output.value;

            // (2) signatures are valid
            if (!Crypto.verifySignature(output.address, tx.getRawDataToSign(i), input.signature))
            {
                return false;
            }
        }

        for (int j=0; j < tx.numOutputs(); j++) {
            double value = tx.getOutput(j).value;
            // (4) all of output values are non-negative
            if (value < 0) {
                return false;
            }

            outputSum += value;
        }

        // shouldn't we use decimal as amount type???

        // (5) the sum of input values is greater than or equal to the sum of its output
        return inputSum < outputSum;

    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        ArrayList<Transaction> correctTxs = new ArrayList<Transaction>();
        for (int i = 0; i < possibleTxs.length; i++)
        {
            Transaction current = possibleTxs[i];
            if (isValidTx(current))
            {
                for (int j = 0; j < current.numInputs(); j++)
                {
                    Transaction.Input currentInput = current.getInput(j);
                    UTXO existingUtxo = new UTXO(currentInput.prevTxHash, currentInput.outputIndex);
                    UtxoPool.removeUTXO(existingUtxo);
                    UTXO newUtxo = new UTXO(current.getHash(), j);
                    UtxoPool.addUTXO(newUtxo, current.getOutput(j));
                }

                correctTxs.add(current);
            }
        }

        return correctTxs.toArray(new Transaction[correctTxs.size()]);
    }

}
