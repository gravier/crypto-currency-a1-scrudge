package tests;

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
    private PublicKey _address;
    private UTXOPool _pool;
    @Before
    public void init() throws NoSuchProviderException, NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA", "SUN");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
        keyGen.initialize(1024, random);
        KeyPair pair = keyGen.generateKeyPair();
        PrivateKey priv = pair.getPrivate();
        PublicKey pub = pair.getPublic();
        _txHandler = new TxHandler(new UTXOPool());
        _address = pair.getPublic();

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
        tx.addOutput(10, _address);
        Assert.assertTrue("8 not enough in empty pool", !_txHandler.isValidTx(tx));

        initNewPool(8);
        Assert.assertTrue("10 not enough in pool with 8", !_txHandler.isValidTx(tx));
    }

    private void initNewPool(double value){
        Transaction utx = new Transaction();
        utx.addOutput(value, _address);
        utx.finalize();
        UTXOPool pool = new UTXOPool();
        pool.addUTXO(new UTXO(utx.getHash(), 0), utx.getOutput(0));
        _txHandler = new TxHandler(pool);
    }

    @Test
    public void isPoolEnoughInTx() {
        //TODO change to check not in pull but in input+output  same trx
        Transaction tx0 = new Transaction();
        tx0.addOutput(0, _address);
        Assert.assertTrue("0 tx enough in pool with 0", _txHandler.isValidTx(tx0));

        Transaction tx1 = new Transaction();
        tx1.addOutput(10, _address);
        initNewPool(10);
        Assert.assertTrue("10 tx enough in pool with 10", _txHandler.isValidTx(tx1));

        Transaction tx2 = new Transaction();
        tx2.addOutput(8, _address);
        Assert.assertTrue("8 tx enough in pool with 10", _txHandler.isValidTx(tx2));
    }

    @Test
    public void handleFindsWrongSignature() {
        Transaction tx0 = new Transaction();
        tx0.addOutput(4, _address);
        initNewPool(8);
        Assert.assertFalse("4 tx has no signature", _txHandler.isValidTx(tx0));
    }

}