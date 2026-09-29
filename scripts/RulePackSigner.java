import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/** Local-only publisher for the data-only rule manifest. Never commit the private key. */
class RulePackSigner {
    public static void main(String[] args) throws Exception {
        if (args.length == 0 || ((!args[0].equals("generate") || args.length != 3)
                && (!args[0].equals("sign") || args.length != 4))) {
            throw new IllegalArgumentException("generate <private.pk8> <public.der> | sign <private.pk8> <manifest.json> <manifest.sig>");
        }
        if (args[0].equals("generate")) {
            Path privatePath = Path.of(args[1]);
            Path publicPath = Path.of(args[2]);
            if (Files.exists(privatePath) || Files.exists(publicPath)) {
                throw new IllegalArgumentException("Refusing to overwrite a key");
            }
            KeyPair pair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
            Files.createFile(privatePath, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            Files.write(privatePath, pair.getPrivate().getEncoded(), StandardOpenOption.WRITE);
            Files.write(publicPath, pair.getPublic().getEncoded(), StandardOpenOption.CREATE_NEW);
            System.out.println("Generated public key: " + publicPath);
        } else if (args[0].equals("sign")) {
            byte[] keyBytes = Files.readAllBytes(Path.of(args[1]));
            var key = KeyFactory.getInstance("Ed25519").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
            Signature signer = Signature.getInstance("Ed25519");
            signer.initSign(key);
            signer.update(Files.readAllBytes(Path.of(args[2])));
            byte[] encoded = Base64.getEncoder().encode(signer.sign());
            Files.write(Path.of(args[3]), encoded, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("Signed manifest: " + args[3]);
        } else {
            throw new IllegalArgumentException("Unknown command: " + args[0]);
        }
    }
}
