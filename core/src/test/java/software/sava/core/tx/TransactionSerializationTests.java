package software.sava.core.tx;

import org.junit.jupiter.api.Test;
import software.sava.core.accounts.PublicKey;
import software.sava.core.accounts.lookup.AddressLookupTable;
import software.sava.core.accounts.meta.AccountMeta;
import software.sava.core.accounts.meta.LookupTableAccountMeta;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.sava.core.accounts.PublicKey.fromBase58Encoded;
import static software.sava.core.accounts.meta.AccountMeta.*;

final class TransactionSerializationTests {

  private void testMultipleLookupTables(final TransactionSkeleton skeleton,
                                        final Map<PublicKey, AddressLookupTable> lookupTableMap) {
    lookupTableMap.values().forEach(table -> assertTrue(table.isActive()));

    final var accountMetas = skeleton.parseAccounts(lookupTableMap);

    assertEquals(44, accountMetas.length);
    final var feePayer = accountMetas[0];
    assertEquals(createFeePayer(fromBase58Encoded("Ee7oGQLorSg8tapxb4ym7Y7vAZUuYHMZEgC6boQQJrcP")), feePayer);
    assertEquals(createWrite(fromBase58Encoded("7u7cD7NxcZEuzRCBaYo8uVpotRdqZwez47vvuwzCov43")), accountMetas[1]);
    assertEquals(createInvoked(fromBase58Encoded("11111111111111111111111111111111")), accountMetas[13]);
    assertEquals(createInvoked(fromBase58Encoded("ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL")), accountMetas[17]);
    assertEquals(createRead(fromBase58Encoded("BQ72nSv9f3PRyRKCBnHLVrerrv37CYTHm5h3s9VSGQDV")), accountMetas[18]);
    assertEquals(createRead(fromBase58Encoded("EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v")), accountMetas[20]);
    assertEquals(createWrite(fromBase58Encoded("4Ep5XNqaDYbFAQHHdhtFXgaPuhDH6uajSG8ZfwxHzwBQ")), accountMetas[21]);
    assertEquals(createWrite(fromBase58Encoded("E4W3xuwX692AuEjx35hKXwk4YZVpvsd2bZdkMvX4TuAR")), accountMetas[34]);
    assertEquals(createRead(fromBase58Encoded("2wT8Yq49kHgDzXuPxZSaeLaH1qbmGXtEyPy64bL7aD3c")), accountMetas[35]);
    assertEquals(createRead(fromBase58Encoded("So11111111111111111111111111111111111111112")), accountMetas[43]);

    final var instructions = skeleton.parseInstructions(accountMetas);
    validateInstructions(instructions);

    final var lookupTableMetas = Arrays.stream(skeleton.lookupTableAccounts())
        .map(lookupTableMap::get)
        .map(LookupTableAccountMeta::createMeta)
        .toArray(LookupTableAccountMeta[]::new);

    final var transaction = Transaction.createTx(feePayer, Arrays.asList(instructions), lookupTableMetas);
    assertEquals(skeleton.version(), transaction.version());
    assertEquals(skeleton.numSigners(), transaction.numSigners());

    final var instructions2 = transaction.instructions();
    assertEquals(instructions.length, instructions2.size());

    transaction.setRecentBlockHash(skeleton.base58BlockHash());
    final var serializedTx = transaction.serialized();

    final var skeleton2 = TransactionSkeleton.deserializeSkeleton(serializedTx);
    assertEquals(skeleton.base58BlockHash(), skeleton2.base58BlockHash());
    assertEquals(skeleton.version(), skeleton2.version());
    assertEquals(skeleton.numSigners(), skeleton2.numSigners());

    final var accountMetas2 = skeleton2.parseAccounts(lookupTableMap);
    assertEquals(feePayer, accountMetas2[0]);
    assertEquals(
        Arrays.stream(accountMetas)
            .sorted(Comparator.comparing(AccountMeta::publicKey))
            .toList(),
        Arrays.stream(accountMetas2)
            .sorted(Comparator.comparing(AccountMeta::publicKey))
            .toList()
    );

    final var accounts = Arrays.stream(instructions)
        .map(Instruction::accounts)
        .flatMap(List::stream)
        .toList();
    final var accounts2 = instructions2.stream()
        .map(Instruction::accounts)
        .flatMap(List::stream)
        .toList();
    assertEquals(accounts, accounts2);

    final var instructions3 = skeleton2.parseInstructions(accountMetas2);
    validateInstructions(instructions3);

    assertEquals(instructions2, Arrays.asList(instructions3));

    final var accounts3 = Arrays.stream(instructions3)
        .map(Instruction::accounts)
        .flatMap(List::stream)
        .toList();
    assertEquals(accounts, accounts3);
  }


  @Test
  void testMultipleLookupTables() {
    final var decoder = Base64.getDecoder();
    final byte[] txData = decoder.decode("ARRw8IKW5g68p4JJly8WK0FeERS1fM8oT768hiqhTUrPKVHA4J3MBvvwB66GG89jl9y58uuLU/d/n5feDm4zQwWAAQAIFcqqGywepKTH2miwSZYlM16b9eL6wOUsHXTJaN/PC3HgZn/LU4VSNrEZJg9hJjEvfwMjmNLCvJDpEyG0zH7Y1yRxM32R3yvnX/HBzojB9fApPOfuRWL27j4EX6B/qtMR3XhSHLF5zruFibVWotXslNJJhoL9+bsq9a1k5JHMQVPajH0GWPl/GBdrvsyAB5oM/DmI+Zim1wtJQfiwcEBhFzuUgNyv8Vh5jpvncwqzug9EmaNqS45ePcJKtUQiZPlalJsmP4XXkRUa4+mVUmtqiwBVOG/QonNYNeS6dAGAu/3DqrPZuTBfiy3tOi8VNLMXUE+81x/vGukWDpnYFI3QNg+w7x9VxI11rNstskEtkvrc5QLhZQOukJJeNJk94SA2P7NrsyJHdN8r61t9/sUBVZUA442cNIW1DRpGPYY4a2gc1iOzs8gYW9R6p6JGvSdImG/6cHUvETiJ/1E3vnj4qH3YZtww6EGz12R/jfuL2SSQ+eiarZ25MhB4xt6Laek7DtrMTZNG4CL8qwpNHlcIBHj7kn2An7H7CibSAoGDAb+EAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADBkZv5SEXMv/srbpyw5vnvIzlu8X3EmssQ5s6QAAAAAR51VvyMcBu7nTFbs5oFQf9sbLeo/SOUQKxzaJWvBOPBt324ddloZPZy+FGzut5rBy0he1fWzeROoz1hX7/AKmMlyWPTiSJ8bs9ECkUjg2DC1oTmdr/EIQEjnvY2+n4WZqAC/9MhzaIlsIPwUBz6/HLWqN1/oH+Tb3IK6Tft154tD/6J/XX9kp0wJsfKVh53ksJqzbfyd1RSzIap7OM5ejG+nrzvtutOj1l82qryXQxsbvkwtL24OR8pgIDRS9dYb3DM82VQy2lvZ9Qvf6uHNlQm3hqNgBIjxRpPlzIfSsyBw4ABQIVkgYAEQYACwArDRABAQ0CAAsMAgAAAOgqAAAAAAAAEAELAREPQhASAAsCAQorFAwPEw8oISgfIAIJKisiKBIQECkoBwYFDygeKBsaCQgqJx0oEhAQKSgEHA8jJBYSCAEVFxgZECYmJSzBIJszQdacgQIDAAAAJmQAASZkAQIZZAID6CoAAAAAAADPBgAAAAAAAEYAFBADCwAAAQkNAgADDAIAAADoAwAAAAAAAAP0NIJPcZr9hSIT6pDVuJOcbRK/6LTNqsuEbqKZDXrYfgVgWl5iWwRhY19dkmh5SwRhhBKoWpjF2AJWdmNKtd10oVa1ARee8ZKsxDUFPj88QBcEFjsYPUZ44JFRsIq/hqRW5K1MXfzQWSrJGK/c9uGPL+lhu51GBD9CPT4BLg==");
    final var skeleton = TransactionSkeleton.deserializeSkeleton(txData);

    assertEquals("DmkhBgQnEMFRou2j5qYUvXvvv46pBwaCcbTXPAu5HYZP", skeleton.base58BlockHash());

    final Map<PublicKey, String> tableData = Map.of(
        fromBase58Encoded("HSGvDUXpsitvzxwKfFGuUXDqkwwmHKuKFefKZrihWwXs"), "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",
        fromBase58Encoded("ArWvRMTGQwouxpVd38MZnTJoCxJP6Y8SE6dyQzTJYe5r"), "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",
        fromBase58Encoded("5k6Si9jd2Z2qQgt3NvfekHTZ7rAmveaskvSeRyGZA7am"), "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"
    );

    var lookupTableMap = tableData.entrySet().stream().collect(Collectors
        .toUnmodifiableMap(Map.Entry::getKey, e -> AddressLookupTable.read(e.getKey(), decoder.decode(e.getValue()))));
    testMultipleLookupTables(skeleton, lookupTableMap);

    lookupTableMap = tableData.entrySet().stream().collect(Collectors
        .toUnmodifiableMap(Map.Entry::getKey, e -> AddressLookupTable.readWithoutReverseLookup(e.getKey(), decoder.decode(e.getValue()))));
    testMultipleLookupTables(skeleton, lookupTableMap);
  }

  private void validateInstructions(final Instruction[] instructions) {
    assertEquals(7, instructions.length);
    assertEquals(createInvoked(fromBase58Encoded("ComputeBudget111111111111111111111111111111")), instructions[0].programId());
    assertEquals(createInvoked(fromBase58Encoded("ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL")), instructions[1].programId());
    assertEquals(createInvoked(fromBase58Encoded("11111111111111111111111111111111")), instructions[2].programId());
    assertEquals(createInvoked(fromBase58Encoded("TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA")), instructions[3].programId());
    assertEquals(createInvoked(fromBase58Encoded("JUP6LkbZbjS1jKKwapdHNy74zcZ3tLUZoi5QNyVTaV4")), instructions[4].programId());
    assertEquals(createInvoked(fromBase58Encoded("TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA")), instructions[5].programId());
    assertEquals(createInvoked(fromBase58Encoded("11111111111111111111111111111111")), instructions[6].programId());
  }
}
