#!/usr/bin/env python

#
# Shows how to create all transactions manually using TransactionFactory.
#

import argparse
import importlib
from abc import abstractmethod
from binascii import hexlify, unhexlify

from symbolchain.core.CryptoTypes import PrivateKey
from symbolchain.core.facade.NemFacade import NemFacade
from symbolchain.core.facade.SymbolFacade import SymbolFacade


class TransactionSample:
    def __init__(self, facade):
        self.facade = facade
        self.key_pair = self.facade.KeyPair(PrivateKey(unhexlify('11002233445566778899AABBCCDDEEFF11002233445566778899AABBCCDDEEFF')))

    def process_transaction_descriptors(self, transaction_descriptors):
        for descriptor in transaction_descriptors:
            self.set_common_fields(descriptor)
            transaction = self.facade.transaction_factory.create(descriptor)
            self.sign_and_print(transaction)

    @abstractmethod
    def set_common_fields(self, descriptor):
        pass

    def sign_and_print(self, transaction):
        signature = self.facade.sign_transaction(self.key_pair, transaction)
        self.facade.transaction_factory.attach_signature(transaction, signature)

        print(transaction)
        print(hexlify(transaction.serialize()))
        print('---- ' * 20)


class NemTransactionSample(TransactionSample):
    def __init__(self):
        super().__init__(NemFacade('testnet'))

    def set_common_fields(self, descriptor):
        descriptor.update({
            'signer_public_key': self.key_pair.public_key,
            'deadline': 12345
        })


class SymbolTransactionSample(TransactionSample):
    def __init__(self):
        super().__init__(SymbolFacade('testnet'))

    def set_common_fields(self, descriptor):
        descriptor.update({
            'signer_public_key': self.key_pair.public_key,
            'fee': 625,
            'deadline': 12345
        })


def main():
    parser = argparse.ArgumentParser(description='transaction sign example')
    parser.add_argument('--blockchain', help='blockchain', choices=('nem', 'symbol'), required=True)
    args = parser.parse_args()

    if 'nem' == args.blockchain:
        factory_names = [
            'descriptors.nem_importance_transfer',
            'descriptors.nem_transfer'
        ]
        sample = NemTransactionSample()
    else:
        factory_names = [
            'descriptors.symbol_alias',
            'descriptors.symbol_key_link',
            'descriptors.symbol_lock',
            'descriptors.symbol_metadata',
            'descriptors.symbol_mosaic',
            'descriptors.symbol_namespace',
            'descriptors.symbol_restriction_account',
            'descriptors.symbol_restriction_mosaic',
            'descriptors.symbol_transfer'
        ]
        sample = SymbolTransactionSample()

    total_descriptors_count = 0
    for factory_name in factory_names:
        transaction_descriptor_factory = getattr(importlib.import_module(factory_name), 'descriptor_factory')
        transaction_descriptors = transaction_descriptor_factory()
        sample.process_transaction_descriptors(transaction_descriptors)
        total_descriptors_count += len(transaction_descriptors)

    print('finished processing {} descriptors'.format(total_descriptors_count))


if __name__ == '__main__':
    main()
