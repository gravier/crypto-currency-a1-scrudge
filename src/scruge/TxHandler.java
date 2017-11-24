package scruge;

public class TxHandler {

    private UTXOPool _pool;
    /**
     * Creates a public ledger whose current scruge.UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the scruge.UTXOPool(scruge.UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        _pool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current scruge.UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no scruge.UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        if(tx == null) return false;
        if(!verifyIsPoolOutputEnough(tx))  return false;
        if(!verifySignatures(tx)) return false;
        return true;
    }

    private boolean verifyIsPoolOutputEnough(Transaction tx){
        double unspentSum = 0;
        double txSum = 0;
        for(UTXO ut  : _pool.getAllUTXO()){
            Transaction.Output o = _pool.getTxOutput(ut);
            unspentSum += o.value;
        }
        for(Transaction.Output o : tx.getOutputs()){
            txSum += o.value;
        }
        return unspentSum >= txSum;
    }

    private boolean verifySignatures(Transaction tx) {
        if(tx.getInputs() == null || tx.getInputs().isEmpty()) return false;
        for(Transaction.Input i : tx.getInputs()){
            UTXO matchHash = new UTXO(i.prevTxHash, i.outputIndex);
            Transaction.Output matchOutput = _pool.getTxOutput(matchHash);
            if(matchOutput == null) return false;
            if(!Crypto.verifySignature(matchOutput.address, tx.getRawTx(), i.signature)) return false;
        }
        return true;
    }

        /**
         * Handles each epoch by receiving an unordered array of proposed transactions, checking each
         * transaction for correctness, returning a mutually valid array of accepted transactions, and
         * updating the current scruge.UTXO pool as appropriate.
         */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        return null;
    }

}
