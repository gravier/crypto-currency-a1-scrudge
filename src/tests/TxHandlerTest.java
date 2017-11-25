package tests;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import scruge.*;

import java.security.*;

/**
 * Created by evaldas on 11/11/17.
 */
public class TxHandlerTest {

    private TxHandler _txHandler;
    private PublicKey _utxoAddress1;
    private PublicKey _trxAddress1;
    private PublicKey _utxoAddress2;
    private UTXOPool _pool;
    private PrivateKey _utxPrivateKey1;
    private PrivateKey _utxPrivateKey2;
    @Before
    public void init() throws NoSuchProviderException, NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();
        _utxPrivateKey1 = pair.getPrivate();
        _txHandler = new TxHandler(new UTXOPool());
        _utxoAddress1 = pair.getPublic();
        _trxAddress1 = keyGen.generateKeyPair().getPublic();
        KeyPair pair2 = keyGen.generateKeyPair();
        _utxoAddress2 = pair2.getPublic();
        _utxPrivateKey2 = pair2.getPrivate();


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

    @Test
    public void isValidEmptyTx() {
        Assert.assertTrue("empty trx is invalid", !_txHandler.isValidTx(null));
    }

    @Test
    public void isPoolNotEnoughInTx() {
        Transaction tx = new Transaction();
        tx.addOutput(10, _utxoAddress1);
        Assert.assertTrue("8 not enough in empty pool", !_txHandler.isValidTx(tx));

        initNewPool(8);
        Assert.assertTrue("10 not enough in pool with 8", !_txHandler.isValidTx(tx));
    }

    private void initNewPool(double value, int extraAmt){
        _pool = new UTXOPool();
        addNewOutputToPool(value, _utxoAddress1);
        if(extraAmt > 0){
            addNewOutputToPool(extraAmt, _utxoAddress2);
        }
        _txHandler = new TxHandler(_pool);
    }

    private void addNewOutputToPool(double value, PublicKey adr) {
        Transaction utx = new Transaction();
        utx.addOutput(value, adr);
        utx.finalize();
        _pool.addUTXO(new UTXO(utx.getHash(), 0), utx.getOutput(0));
    }

    private void initNewPool(double value) {
        initNewPool(value, 0);
    }

    private byte[] sign(PrivateKey privateKey, byte[] rawDataToSign) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(privateKey);
        sig.update(rawDataToSign);
        return sig.sign();
    }

    @Test
    public void isPoolEnoughInTx()  {
        initNewPool(0);
        Transaction tx0 = getTransaction(0);
        Assert.assertTrue("0 tx enough in pool with 0", _txHandler.isValidTx(tx0));

        initNewPool(10);
        Transaction tx1 = getTransaction(10);
        Assert.assertTrue("10 tx enough in pool with 10", _txHandler.isValidTx(tx1));

        Transaction tx2 = getTransaction(8);
        Assert.assertTrue("8 tx enough in pool with 10", _txHandler.isValidTx(tx2));
    }

    @NotNull
    private Transaction getTransaction(int amt, int repeatCount, PrivateKey signKey, byte[] hash) {
        Transaction trx = new Transaction();
        for(int i=0; i<repeatCount+1; i++) {
            trx.addInput(hash, 0);
            trx.addOutput(amt, _trxAddress1);
        }
        for(int i=0; i<repeatCount+1; i++) {
            try {
                trx.addSignature(sign(signKey, trx.getRawDataToSign(i)), i);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (SignatureException e) {
                e.printStackTrace();
            }
        }
        return trx;
    }

    private Transaction getTransaction(int amt) {
        return getTransaction(amt, 0, _utxPrivateKey1, _pool.getAllUTXO().get(0).getTxHash());
    }

    @Test
    public void handleFindsMissingInput() {
        Transaction tx0 = new Transaction();
        tx0.addOutput(4, _utxoAddress1);
        initNewPool(8);
        Assert.assertFalse("4 tx has no input", _txHandler.isValidTx(tx0));
    }

    @Test
    public void handleFindsNoSign() {
        initNewPool(10);
        Transaction tx1 = getTransaction(10);
        tx1.getInput(0).signature = null;
        Assert.assertFalse("10 tx has no sign", _txHandler.isValidTx(tx1));
    }

    @Test
    public void handleFindsTamperedTxAfterSign() {
        initNewPool(10);
        Transaction tx1 = getTransaction(10);
        tx1.getOutput(0).value -= 1;
        Assert.assertFalse("tampering after sign is BAD", _txHandler.isValidTx(tx1));
    }

    @Test
    public void handleNotAllowsDoubleClaim() {
        initNewPool(10, 10);
        Transaction tx1 = getTransaction(10, 1, _utxPrivateKey1, _pool.getAllUTXO().get(0).getTxHash());
        Assert.assertFalse("should not allow double spend from same utxo", _txHandler.isValidTx(tx1));
    }

    @Test
    public void handleRejectsNegativeOutput() {
        initNewPool(10);
        Transaction tx1 = getTransaction(-10);
        Assert.assertFalse("negative output not allowed", _txHandler.isValidTx(tx1));
    }

    @Test
    public void handleRejectsMoreOutputThenInput() {
        initNewPool(10, 10);
        Transaction tx1 = getTransaction(11);
        Assert.assertFalse("output cannot be larger than input", _txHandler.isValidTx(tx1));
    }


    @Test
    public void handleTrxReturnsAllGoodTrx() {
        initNewPool(10, 11);
        byte[] hash1 = _pool.getAllUTXO().get(0).getTxHash();
        byte[] hash2 = _pool.getAllUTXO().get(1).getTxHash();
        Transaction tx1 = getTransaction(10, 0, _utxPrivateKey1, hash1);
        Transaction tx2 = getTransaction(11, 0, _utxPrivateKey2, hash2);
        Transaction[] trxs = new Transaction[]{ tx1, tx2 };
        Assert.assertEquals( "all trx are valid", 2, _txHandler.handleTxs(trxs).length);
    }


}