package software.sava.core.tx;

import software.sava.core.accounts.PublicKey;
import software.sava.core.accounts.lookup.AddressLookupTable;
import software.sava.core.accounts.meta.AccountMeta;
import software.sava.core.encoding.CompactU16Encoding;

import java.util.Arrays;
import java.util.Map;

import static software.sava.core.accounts.PublicKey.PUBLIC_KEY_LENGTH;
import static software.sava.core.encoding.CompactU16Encoding.signedByte;
import static software.sava.core.tx.Transaction.MESSAGE_VERSION_0_PREFIX;
import static software.sava.core.tx.Transaction.SIGNATURE_LENGTH;
import static software.sava.core.tx.TransactionSkeletonRecord.LEGACY_INVOKED_INDEXES;
import static software.sava.core.tx.TransactionSkeletonRecord.NO_TABLES;

public interface TransactionSkeleton {

  static TransactionSkeleton deserializeSkeleton(final byte[] data) {
    int o = 0;
    final int numSignatures = CompactU16Encoding.decode(data, o);
    ++o;
    o += (numSignatures * SIGNATURE_LENGTH);

    int version = data[o++] & 0xFF;
    final int numRequiredSignatures;
    if (signedByte(version)) {
      version &= 0x7F;
      numRequiredSignatures = data[o++];
    } else {
      numRequiredSignatures = version;
      version = MESSAGE_VERSION_0_PREFIX;
    }
    final int numReadonlySignedAccounts = data[o++];
    final int numReadonlyUnsignedAccounts = data[o++];

    final int numIncludedAccounts = CompactU16Encoding.decode(data, o);
    ++o;
    final int accountsOffset = o;
    o += numIncludedAccounts << 5;

    final int recentBlockHashIndex = o;
    o += Transaction.BLOCK_HASH_LENGTH;

    final int numInstructions = CompactU16Encoding.decode(data, o);
    ++o;
    final int instructionsOffset = o;
    if (version >= 0) {
      final int[] invokedIndexes = new int[numInstructions];
      for (int i = 0, numAccounts, len; i < numInstructions; ++i) {
        invokedIndexes[i] = data[o++] & 0xFF;
        numAccounts = CompactU16Encoding.decode(data, o);
        o += 1 + numAccounts;
        len = CompactU16Encoding.decode(data, o);
        o += 1 + len;
      }
      if (o < data.length) {
        final int numLookupTables = CompactU16Encoding.decode(data, o);
        if (numLookupTables > 0) {
          ++o;
          final int lookupTablesOffset = o;
          final PublicKey[] lookupTableAccounts = new PublicKey[numLookupTables];
          int numAccounts = numIncludedAccounts;
          for (int t = 0, numWriteIndexes, numReadIndexes; t < numLookupTables; ++t) {
            lookupTableAccounts[t] = PublicKey.readPubKey(data, o);
            o += PUBLIC_KEY_LENGTH;
            numWriteIndexes = CompactU16Encoding.decode(data, o);
            o += 1 + numWriteIndexes;
            numAccounts += numWriteIndexes;
            numReadIndexes = CompactU16Encoding.decode(data, o);
            o += 1 + numReadIndexes;
            numAccounts += numReadIndexes;
          }
          Arrays.sort(invokedIndexes);
          return new TransactionSkeletonRecord(
              data,
              version, numRequiredSignatures, numReadonlySignedAccounts, numReadonlyUnsignedAccounts,
              numIncludedAccounts, accountsOffset,
              recentBlockHashIndex,
              numInstructions, instructionsOffset, invokedIndexes,
              lookupTablesOffset, lookupTableAccounts,
              numAccounts
          );
        }
      }
    } else {
      for (int i = 0, numAccounts, len; i < numInstructions; ++i) {
        ++o; // program index
        numAccounts = CompactU16Encoding.decode(data, o);
        o += 1 + numAccounts;
        len = CompactU16Encoding.decode(data, o);
        o += 1 + len;
      }
    }
    return new TransactionSkeletonRecord(
        data,
        version, numRequiredSignatures, numReadonlySignedAccounts, numReadonlyUnsignedAccounts,
        numIncludedAccounts, accountsOffset,
        recentBlockHashIndex,
        numInstructions, instructionsOffset, LEGACY_INVOKED_INDEXES,
        -1, NO_TABLES,
        numIncludedAccounts
    );
  }

  byte[] data();

  int version();

  int numSignatures();

  int numSigners();

  int recentBlockHashIndex();

  byte[] blockHash();

  String base58BlockHash();

  PublicKey[] lookupTableAccounts();

  AccountMeta[] parseAccounts();

  AccountMeta[] parseAccounts(final Map<PublicKey, AddressLookupTable> lookupTables);

  AccountMeta[] parseAccounts(final AddressLookupTable lookupTable);

  Instruction[] parseInstructions(final AccountMeta[] accounts);
}
