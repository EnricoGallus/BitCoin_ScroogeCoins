import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Signature;

/**
 * Created by enrico on 26.01.17.
 */
class TxHandlerTest {

    @Test
    void Test() throws Exception
    {
        final String message = "Hello world is a stupid message to be signed";

        final KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();

        final Signature privSig = Signature.getInstance("SHA1withRSA");

        privSig.initSign(keyPair.getPrivate());
        privSig.update(message.getBytes());

        byte[] signature = privSig.sign();

        final Signature pubSig = Signature.getInstance("SHA1withRSA");

        PublicKey publicKey = keyPair.getPublic();
        pubSig.initVerify(publicKey);
        pubSig.update(message.getBytes());

        byte[] hash = new byte[256];

        Transaction transaction = new Transaction();
        transaction.addInput(hash, 0);
        transaction.addSignature(signature, 0);
        transaction.addOutput(1.6, publicKey);

        UTXOPool pool = new UTXOPool();
        pool.addUTXO(new UTXO(hash, 0), transaction.getOutput(0));

        boolean result = new TxHandler(pool).isValidTx(transaction);

        assertTrue(result);
    }
}
