import java.util.ArrayList;
import java.util.List;

/**
 * Created by cosmin on 14.08.2017.
 */
public class TxHandler {

    private UTXOPool utxoPool;
    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
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
        double previousTxOutSum = 0;
        double currentTxOutSum = 0;
        int size = tx.getInputs().size();
        UTXOPool pool = new UTXOPool();
        for (int i = 0; i < size; i++) {
            Transaction.Input input = tx.getInput(i);
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            if (!utxoPool.contains(utxo))
                return false;
            Transaction.Output output = utxoPool.getTxOutput(utxo);
            if (!Crypto.verifySignature(output.address, tx.getRawDataToSign(i), input.signature ))
                return false;
            if (pool.contains(utxo))
                return false;
            pool.addUTXO(utxo, output);
            previousTxOutSum += output.value;
        }

        ArrayList<Transaction.Output> outputArrayList = new ArrayList(tx.getOutputs());
        for (Transaction.Output output : outputArrayList) {
            if (output.value < 0) {
                return false;
            }
            currentTxOutSum += output.value;
        }
        return currentTxOutSum <= previousTxOutSum;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        List<Transaction> validTransactions = new ArrayList<>();

        for (Transaction t : possibleTxs) {
            if (isValidTx(t)) {
                validTransactions.add(t);
                for (Transaction.Input input : t.getInputs()) {
                    UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                    utxoPool.removeUTXO(utxo);
                }
                for (int i = 0; i < t.numOutputs(); i++) {
                    Transaction.Output output = t.getOutput(i);
                    UTXO utxo = new UTXO(t.getHash(), i);
                    utxoPool.addUTXO(utxo, output);
                }
            }
        }
        Transaction[] toReturn = new Transaction[validTransactions.size()];
        validTransactions.toArray(toReturn);
        return toReturn;
    }

}
