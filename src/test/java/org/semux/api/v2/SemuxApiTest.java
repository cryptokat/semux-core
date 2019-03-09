/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
/**
 * Semux
 * Semux is an experimental high-performance blockchain platform that powers decentralized application.
 *
 * OpenAPI spec version: 1.0.2
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.semux.api.v2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.semux.TestUtils.createBlock;
import static org.semux.TestUtils.createTransaction;
import static org.semux.core.Amount.Unit.NANO_SEM;
import static org.semux.core.Amount.Unit.SEM;
import static org.semux.core.TransactionType.COINBASE;
import static org.semux.core.TransactionType.TRANSFER;
import static org.semux.core.TransactionType.UNVOTE;
import static org.semux.core.TransactionType.VOTE;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;

import org.junit.Test;
import org.semux.Network;
import org.semux.TestUtils;
import org.semux.api.v2.model.AddNodeResponse;
import org.semux.api.v2.model.BlockType;
import org.semux.api.v2.model.ComposeRawTransactionResponse;
import org.semux.api.v2.model.CreateAccountResponse;
import org.semux.api.v2.model.DelegateType;
import org.semux.api.v2.model.DeleteAccountResponse;
import org.semux.api.v2.model.DoTransactionResponse;
import org.semux.api.v2.model.GetAccountPendingTransactionsResponse;
import org.semux.api.v2.model.GetAccountResponse;
import org.semux.api.v2.model.GetAccountTransactionsResponse;
import org.semux.api.v2.model.GetAccountVotesResponse;
import org.semux.api.v2.model.GetBlockResponse;
import org.semux.api.v2.model.GetDelegateResponse;
import org.semux.api.v2.model.GetDelegatesResponse;
import org.semux.api.v2.model.GetInfoResponse;
import org.semux.api.v2.model.GetLatestBlockNumberResponse;
import org.semux.api.v2.model.GetLatestBlockResponse;
import org.semux.api.v2.model.GetPeersResponse;
import org.semux.api.v2.model.GetPendingTransactionsResponse;
import org.semux.api.v2.model.GetSyncingProgressResponse;
import org.semux.api.v2.model.GetTransactionLimitsResponse;
import org.semux.api.v2.model.GetTransactionResponse;
import org.semux.api.v2.model.GetValidatorsResponse;
import org.semux.api.v2.model.GetVoteResponse;
import org.semux.api.v2.model.GetVotesResponse;
import org.semux.api.v2.model.InfoType;
import org.semux.api.v2.model.ListAccountsResponse;
import org.semux.api.v2.model.PeerType;
import org.semux.api.v2.model.SignMessageResponse;
import org.semux.api.v2.model.SignRawTransactionResponse;
import org.semux.api.v2.model.SyncingProgressType;
import org.semux.api.v2.model.VerifyMessageResponse;
import org.semux.consensus.SemuxSync;
import org.semux.core.Amount;
import org.semux.core.Block;
import org.semux.core.Genesis;
import org.semux.core.PendingManager;
import org.semux.core.Transaction;
import org.semux.core.TransactionResult;
import org.semux.core.state.Delegate;
import org.semux.core.state.DelegateState;
import org.semux.crypto.Hex;
import org.semux.crypto.Key;
import org.semux.net.ChannelManager;
import org.semux.net.Peer;
import org.semux.net.filter.FilterRule;
import org.semux.net.filter.SemuxIpFilter;
import org.semux.util.Bytes;

import io.netty.handler.ipfilter.IpFilterRuleType;

/**
 * API tests for {@link SemuxApiImpl}
 */
public class SemuxApiTest extends SemuxApiTestBase {

    @Test
    public void addNodeTest() {
        String node = "127.0.0.1:5162";
        AddNodeResponse response = api.addNode(node);
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(1, nodeMgr.queueSize());
    }

    @Test
    public void addToBlacklistTest() throws UnknownHostException {
        ChannelManager channelManagerSpy = spy(kernelRule.getKernel().getChannelManager());
        kernelRule.getKernel().setChannelManager(channelManagerSpy);

        // blacklist 8.8.8.8
        assertTrue(api.addToBlacklist("8.8.8.8").isSuccess());
        verify(channelManagerSpy).closeBlacklistedChannels();

        // assert that 8.8.8.8 is no longer acceptable
        InetSocketAddress inetSocketAddress = mock(InetSocketAddress.class);
        when(inetSocketAddress.getAddress()).thenReturn(InetAddress.getByName("8.8.8.8"));
        assertFalse(channelMgr.isAcceptable(inetSocketAddress));

        // assert that ipfilter.json is persisted
        File ipfilterJson = new File(config.configDir(), SemuxIpFilter.CONFIG_FILE);
        assertTrue(ipfilterJson.exists());
    }

    @Test
    public void addToWhitelistTest() throws UnknownHostException {
        // reject all connections
        channelMgr.getIpFilter().appendRule(new FilterRule("0.0.0.0/0", IpFilterRuleType.REJECT));

        // whitelist 8.8.8.8
        assertTrue(api.addToWhitelist("8.8.8.8").isSuccess());

        // assert that 8.8.8.8 is acceptable
        InetSocketAddress inetSocketAddress = mock(InetSocketAddress.class);
        when(inetSocketAddress.getAddress()).thenReturn(InetAddress.getByName("8.8.8.8"));
        assertTrue(channelMgr.isAcceptable(inetSocketAddress));

        // assert that ipfilter.json is persisted
        File ipfilterJson = new File(config.configDir(), SemuxIpFilter.CONFIG_FILE);
        assertTrue(ipfilterJson.exists());
    }

    @Test
    public void createAccountTest() {
        int size = wallet.getAccounts().size();
        CreateAccountResponse response = api.createAccount(null, null);
        assertTrue(response.isSuccess());
        assertEquals(size + 1, wallet.getAccounts().size());
    }

    @Test
    public void createAccountImportPrivateKeyTest() {
        int size = wallet.getAccounts().size();
        String privateKey = "302e020100300506032b657004220420acbd5f2cb2b6053f704376d12df99f2aa163d267a755c7f1d9fe55d2a2dc5405";
        CreateAccountResponse response = api.createAccount(null, privateKey);
        assertTrue(response.isSuccess());
        assertEquals(size + 1, wallet.getAccounts().size());
        assertEquals("23a6049381fd2cfb0661d9de206613b83d53d7df", wallet.getAccounts().get(size).toAddressString());
    }

    @Test
    public void getAccountTest() {
        // create an account
        Key key = new Key();
        accountState.adjustAvailable(key.toAddress(), SEM.of(1000));
        chain.addBlock(createBlock(
                chain.getLatestBlockNumber() + 1,
                Collections.singletonList(createTransaction(config, key, key, Amount.ZERO)),
                Collections.singletonList(new TransactionResult())));

        // request api endpoint
        GetAccountResponse response = api.getAccount(key.toAddressString());
        assertTrue(response.isSuccess());
        assertEquals(SEM.of(1000).getNano(), Long.parseLong(response.getResult().getAvailable()));
        assertEquals(Integer.valueOf(1), response.getResult().getTransactionCount());
    }

    @Test
    public void deleteAccountTest() {
        Key account = wallet.getAccounts().get(0);
        DeleteAccountResponse resp = api.deleteAccount(account.toAddressString());
        assertTrue(resp.isSuccess());
        assertThat(wallet.getAccounts()).doesNotContain(account);
    }

    @Test
    public void getAccountTransactionsTest() {
        Transaction tx = createTransaction(config);
        TransactionResult res = new TransactionResult();
        Block block = createBlock(chain.getLatestBlockNumber() + 1, Collections.singletonList(tx),
                Collections.singletonList(res));
        chain.addBlock(block);

        GetAccountTransactionsResponse response = api.getAccountTransactions(Hex.encode(tx.getFrom()), "0", "1024");
        assertTrue(response.isSuccess());
        assertNotNull(response.getResult());
    }

    @Test
    public void getAccountPendingTransactionsTest() {
        Key from = new Key();
        Key to = new Key();
        Transaction tx0 = createTransaction(config);
        Transaction tx1 = createTransaction(config, from, to, Amount.ZERO);
        Transaction tx2 = createTransaction(config, to, from, Amount.ZERO);
        chain.getAccountState().adjustAvailable(tx0.getFrom(), config.minTransactionFee());
        chain.getAccountState().adjustAvailable(from.toAddress(), config.minTransactionFee());
        chain.getAccountState().adjustAvailable(to.toAddress(), config.minTransactionFee());
        assert (pendingMgr.addTransactionSync(tx0).accepted == 1);
        assert (pendingMgr.addTransactionSync(tx1).accepted == 1);
        assert (pendingMgr.addTransactionSync(tx2).accepted == 1);

        GetAccountPendingTransactionsResponse response;

        response = api.getAccountPendingTransactions(Hex.encode0x(from.toAddress()), "0", "1");
        assertThat(response.getResult()).hasSize(1);
        assertThat(response.getResult().get(0).getHash()).isEqualTo(Hex.encode0x(tx1.getHash()));

        response = api.getAccountPendingTransactions(Hex.encode0x(from.toAddress()), "0", "2");
        assertThat(response.getResult()).hasSize(2);
        assertThat(response.getResult().get(0).getHash()).isEqualTo(Hex.encode0x(tx1.getHash()));
        assertThat(response.getResult().get(1).getHash()).isEqualTo(Hex.encode0x(tx2.getHash()));

        response = api.getAccountPendingTransactions(Hex.encode0x(from.toAddress()), "1", "2");
        assertThat(response.getResult()).hasSize(1);
        assertThat(response.getResult().get(0).getHash()).isEqualTo(Hex.encode0x(tx2.getHash()));

        response = api.getAccountPendingTransactions(Hex.encode0x(from.toAddress()), "2", "3");
        assertThat(response.getResult()).hasSize(0);
    }

    @Test
    public void getAccountVotesTest() {
        DelegateState delegateState = chain.getDelegateState();
        List<Delegate> delegates = delegateState.getDelegates();
        Key voter = new Key();
        for (int i = 0; i < delegates.size(); i++) {
            delegateState.vote(voter.toAddress(), delegates.get(i).getAddress(), Amount.Unit.NANO_SEM.of(i + 1));
        }

        GetAccountVotesResponse resp = api.getAccountVotes(Hex.encode0x(voter.toAddress()));
        assertTrue(resp.isSuccess());
        assertThat(resp.getResult()).hasSize(delegates.size());
        assertThat(resp.getResult().get(0))
                .hasFieldOrPropertyWithValue("votes", "1")
                .hasFieldOrPropertyWithValue("delegate.address", Hex.encode0x(delegates.get(0).getAddress()));
    }

    @Test
    public void getBlockByHashTest() {
        Genesis gen = chain.getGenesis();
        GetBlockResponse response = api.getBlockByHash(Hex.encode0x(gen.getHash()));
        assertTrue(response.isSuccess());
        assertEquals(Hex.encode0x(gen.getHash()), response.getResult().getHash());
        assertNotNull(response.getResult().getTransactions());
    }

    @Test
    public void getBlockByNumberTest() {
        Genesis gen = chain.getGenesis();
        GetBlockResponse response = api.getBlockByNumber(String.valueOf(gen.getNumber()));
        assertTrue(response.isSuccess());
        assertEquals(Hex.encode0x(gen.getHash()), response.getResult().getHash());
        assertNotNull(response.getResult().getTransactions());
    }

    @Test
    public void getDelegateTest() {
        Genesis gen = chain.getGenesis();
        for (Map.Entry<String, byte[]> entry : gen.getDelegates().entrySet()) {
            GetDelegateResponse response = api.getDelegate(Hex.encode0x(entry.getValue()));
            assertTrue(response.isSuccess());
            assertEquals(entry.getKey(), response.getResult().getName());
            assertTrue("is validator", response.getResult().isValidator());
        }
    }

    @Test
    public void getDelegatesTest() {
        Genesis gen = chain.getGenesis();
        GetDelegatesResponse response = api.getDelegates();

        Collection<byte[]> delegates = gen.getDelegates().values();
        List<DelegateType> result = response.getResult();

        assertEquals(delegates.size(), result.size());
        assertEquals(
                delegates.stream().map(Hex::encode0x).sorted().collect(Collectors.toList()),
                result.stream().map(DelegateType::getAddress).sorted().collect(Collectors.toList()));

        assertTrue("is validator", result.get(0).isValidator());
    }

    @Test
    public void getInfoTest() {
        GetInfoResponse response = api.getInfo();
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getResult());
        assertEquals(InfoType.NetworkEnum.DEVNET.name(), response.getResult().getNetwork());
        assertEquals(config.getClientCapabilities().toList(), response.getResult().getCapabilities());
        assertEquals("0", response.getResult().getLatestBlockNumber());
        assertEquals(Hex.encode0x(chain.getLatestBlock().getHash()), response.getResult().getLatestBlockHash());
        assertEquals(Integer.valueOf(0), response.getResult().getActivePeers());
        assertEquals(Integer.valueOf(0), response.getResult().getPendingTransactions());
        assertEquals(config.getClientId(), response.getResult().getClientId());
        assertEquals(Hex.encode0x(wallet.getAccount(0).toAddress()), response.getResult().getCoinbase());
    }

    @Test
    public void getLatestBlockTest() {
        // test genesis block
        Genesis genesisBlock = chain.getGenesis();
        GetLatestBlockResponse response = api.getLatestBlock();
        assertNotNull(response);
        assertTrue(response.isSuccess());

        BlockType blockJson = response.getResult();
        assertEquals(Hex.encode0x(genesisBlock.getHash()), blockJson.getHash());
        assertEquals(genesisBlock.getNumber(), Long.parseLong(blockJson.getNumber()));
        assertEquals(Hex.encode0x(genesisBlock.getCoinbase()), blockJson.getCoinbase());
        assertEquals(Hex.encode0x(genesisBlock.getParentHash()), blockJson.getParentHash());
        assertEquals(genesisBlock.getTimestamp(), Long.parseLong(blockJson.getTimestamp()));
        assertEquals(Hex.encode0x(genesisBlock.getTransactionsRoot()), blockJson.getTransactionsRoot());
        assertEquals(Hex.encode0x(genesisBlock.getData()), blockJson.getData());

        // add 1 block
        Block firstBlock = TestUtils.createBlock(
                1,
                Collections.emptyList(),
                Collections.emptyList());
        chain.addBlock(firstBlock);

        response = api.getLatestBlock();
        assertNotNull(response);
        assertTrue(response.isSuccess());

        blockJson = response.getResult();
        assertEquals(Hex.encode0x(firstBlock.getHash()), blockJson.getHash());
        assertEquals(firstBlock.getNumber(), Long.parseLong(blockJson.getNumber()));
        assertEquals(Hex.encode0x(firstBlock.getCoinbase()), blockJson.getCoinbase());
        assertEquals(Hex.encode0x(firstBlock.getParentHash()), blockJson.getParentHash());
        assertEquals(firstBlock.getTimestamp(), Long.parseLong(blockJson.getTimestamp()));
        assertEquals(Hex.encode0x(firstBlock.getTransactionsRoot()), blockJson.getTransactionsRoot());
        assertEquals(1, blockJson.getTransactions().size()); // coinbase tx should be included
        assertEquals(COINBASE.toString(), blockJson.getTransactions().get(0).getType());
        assertEquals(Hex.encode0x(firstBlock.getData()), blockJson.getData());
    }

    @Test
    public void getLatestBlockNumberTest() {
        GetLatestBlockNumberResponse response = api.getLatestBlockNumber();
        assertNotNull(response);
        assertEquals(chain.getLatestBlock().getNumber(), Long.parseLong(response.getResult()));
    }

    @Test
    public void getPeersTest() {
        channelMgr = spy(kernelRule.getKernel().getChannelManager());
        List<Peer> peers = Arrays.asList(
                new Peer(Network.DEVNET, (short) 1, "peer1", "1.2.3.4", 5161, "client1",
                        config.getClientCapabilities().toArray(), 1),
                new Peer(Network.DEVNET, (short) 2, "peer2", "2.3.4.5", 5171, "client2",
                        config.getClientCapabilities().toArray(), 2));
        when(channelMgr.getActivePeers()).thenReturn(peers);
        kernelRule.getKernel().setChannelManager(channelMgr);

        GetPeersResponse response = api.getPeers();
        assertTrue(response.isSuccess());
        List<PeerType> result = response.getResult();

        assertNotNull(result);
        assertEquals(peers.size(), result.size());
        for (int i = 0; i < peers.size(); i++) {
            PeerType peerJson = result.get(i);
            Peer peer = peers.get(i);
            assertEquals(peer.getIp(), peerJson.getIp());
            assertEquals(peer.getPort(), peerJson.getPort().intValue());
            assertEquals(peer.getNetworkVersion(), peerJson.getNetworkVersion().shortValue());
            assertEquals(peer.getClientId(), peerJson.getClientId());
            assertEquals(Hex.PREF + peer.getPeerId(), peerJson.getPeerId());
            assertEquals(peer.getLatestBlockNumber(), Long.parseLong(peerJson.getLatestBlockNumber()));
            assertEquals(peer.getLatency(), Long.parseLong(peerJson.getLatency()));
            assertArrayEquals(peer.getCapabilities(), peerJson.getCapabilities().toArray());
        }
    }

    @Test
    public void getPendingTransactionsTest() {
        Transaction tx = createTransaction(config);
        TransactionResult result = new TransactionResult();
        PendingManager pendingManager = spy(kernelRule.getKernel().getPendingManager());
        when(pendingManager.getPendingTransactions()).thenReturn(
                Collections.singletonList(new PendingManager.PendingTransaction(tx, result)));
        kernelRule.getKernel().setPendingManager(pendingManager);

        GetPendingTransactionsResponse response = api.getPendingTransactions();
        assertTrue(response.isSuccess());
        assertNotNull(response.getResult());
        assertThat(response.getResult()).hasSize(1);
    }

    @Test
    public void getTransactionTest() {
        Key from = new Key(), to = new Key();
        Transaction tx = createTransaction(config, from, to, Amount.Unit.SEM.of(1));
        TransactionResult res = new TransactionResult();
        Block block = createBlock(chain.getLatestBlockNumber() + 1, Collections.singletonList(tx),
                Collections.singletonList(res));
        chain.addBlock(block);

        GetTransactionResponse response = api.getTransaction(Hex.encode(tx.getHash()));
        assertTrue(response.isSuccess());
        assertEquals(Hex.encode0x(to.toAddress()), response.getResult().getTo());
        assertEquals(Hex.encode0x(tx.getHash()), response.getResult().getHash());
        assertEquals(Hex.encode0x(Bytes.EMPTY_BYTES), response.getResult().getData());
        assertEquals(tx.getFee().getNano(), Long.parseLong(response.getResult().getFee()));
        assertEquals(Hex.encode0x(tx.getFrom()), response.getResult().getFrom());
        assertEquals(tx.getNonce(), Long.parseLong(response.getResult().getNonce()));
        assertEquals(tx.getTimestamp(), Long.parseLong(response.getResult().getTimestamp()));
        assertEquals(tx.getType().toString(), response.getResult().getType());
        assertEquals(tx.getValue().getNano(), Long.parseLong(response.getResult().getValue()));
    }

    @Test
    public void getTransactionLimitsTest() {
        for (org.semux.core.TransactionType type : org.semux.core.TransactionType.values()) {
            GetTransactionLimitsResponse response = api.getTransactionLimits(type.toString());
            assertNotNull(response);
            assertTrue(response.isSuccess());
            assertEquals(config.maxTransactionDataSize(type),
                    response.getResult().getMaxTransactionDataSize().intValue());
            assertEquals(config.minTransactionFee().getNano(),
                    Long.parseLong(response.getResult().getMinTransactionFee()));

            if (type.equals(org.semux.core.TransactionType.DELEGATE)) {
                assertEquals(config.minDelegateBurnAmount().getNano(),
                        Long.parseLong(response.getResult().getMinDelegateBurnAmount()));
            } else {
                assertNull(response.getResult().getMinDelegateBurnAmount());
            }
        }
    }

    @Test
    public void getValidatorsTest() {
        Genesis gen = chain.getGenesis();
        GetValidatorsResponse response = api.getValidators();
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(gen.getDelegates().size(), response.getResult().size());
        assertThat(gen.getDelegates().entrySet().stream().map(e -> Hex.encode0x(e.getValue())).sorted()
                .collect(Collectors.toList()))
                        .isEqualTo(response.getResult().stream().sorted().collect(Collectors.toList()));
    }

    @Test
    public void getVoteTest() {
        Key key = new Key();
        Key key2 = new Key();
        DelegateState ds = chain.getDelegateState();
        ds.register(key2.toAddress(), Bytes.of("test"));
        ds.vote(key.toAddress(), key2.toAddress(), NANO_SEM.of(200));

        GetVoteResponse response = api.getVote(key2.toAddressString(), key.toAddressString());
        assertTrue(response.isSuccess());
        assertEquals(200L, Long.parseLong(response.getResult()));
    }

    @Test
    public void getVotesTest() {
        Key voterKey = new Key();
        Key delegateKey = new Key();
        DelegateState ds = chain.getDelegateState();
        assertTrue(ds.register(delegateKey.toAddress(), Bytes.of("test")));
        assertTrue(ds.vote(voterKey.toAddress(), delegateKey.toAddress(), NANO_SEM.of(200)));
        ds.commit();

        GetVotesResponse response = api.getVotes(delegateKey.toAddressString());
        assertTrue(response.isSuccess());
        assertEquals(200L, Long.parseLong(response.getResult().get(Hex.PREF + voterKey.toAddressString())));
    }

    @Test
    public void listAccountsTest() {
        ListAccountsResponse response = api.listAccounts();
        assertNotNull(response);
        assertThat(response.getResult())
                .hasSize(wallet.size())
                .isEqualTo(wallet.getAccounts().parallelStream()
                        .map(acc -> Hex.PREF + acc.toAddressString())
                        .collect(Collectors.toList()));
    }

    @Test
    public void registerDelegateTest() throws InterruptedException {
        String from = wallet.getAccount(0).toAddressString();
        String fee = String.valueOf(config.minTransactionFee().getNano());
        String data = Hex.encode(Bytes.of("test_delegate"));
        DoTransactionResponse response = api.registerDelegate(from, data, fee, null, null);
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getResult());

        Thread.sleep(200);

        List<PendingManager.PendingTransaction> list = pendingMgr.getPendingTransactions();
        assertFalse(list.isEmpty());
        assertArrayEquals(list.get(list.size() - 1).transaction.getHash(), Hex.decode0x(response.getResult()));
        assertEquals(list.get(list.size() - 1).transaction.getType(), org.semux.core.TransactionType.DELEGATE);
    }

    @Test
    public void broadcastRawTransactionTest() throws InterruptedException {
        Key from = new Key();
        Key to = new Key();
        Amount value = Amount.ZERO;
        kernelRule.getKernel().getBlockchain().getAccountState().adjustAvailable(from.toAddress(),
                config.minTransactionFee());
        Transaction tx = createTransaction(config, from, to, value);

        DoTransactionResponse response = api.broadcastRawTransaction(Hex.encode(tx.toBytes()), null);
        assertTrue(response.isSuccess());

        Thread.sleep(200);
        List<PendingManager.PendingTransaction> list = pendingMgr.getPendingTransactions();
        assertThat(list).hasSize(1);
        assertArrayEquals(list.get(list.size() - 1).transaction.getHash(), tx.getHash());
    }

    @Test(expected = BadRequestException.class)
    public void broadcastRawTransactionValidateNonceTest() {
        Key from = new Key();
        Key to = new Key();
        Amount value = Amount.ZERO;

        // mock state
        kernelRule.getKernel().getBlockchain().getAccountState().adjustAvailable(from.toAddress(),
                config.minTransactionFee());
        PendingManager pendingManager = spy(pendingMgr);
        when(pendingManager.getNonce(from.toAddress())).thenReturn(100L);
        kernelRule.getKernel().setPendingManager(pendingManager);

        Transaction tx = createTransaction(config, from, to, value, 101L);
        api.broadcastRawTransaction(Hex.encode(tx.toBytes()), true);
    }

    @Test
    public void broadcastRawTransactionNoValidateNonceTest() {
        Key from = new Key();
        Key to = new Key();
        Amount value = Amount.ZERO;

        // mock state
        kernelRule.getKernel().getBlockchain().getAccountState().adjustAvailable(from.toAddress(),
                config.minTransactionFee());
        PendingManager pendingManager = spy(pendingMgr);
        when(pendingManager.getNonce(from.toAddress())).thenReturn(100L);
        kernelRule.getKernel().setPendingManager(pendingManager);

        Transaction tx = createTransaction(config, from, to, value, 101L);
        DoTransactionResponse resp = api.broadcastRawTransaction(Hex.encode(tx.toBytes()), false);
        assertTrue(resp.isSuccess());
    }

    @Test
    public void signMessageTest() {
        String address = wallet.getAccount(0).toAddressString();
        String addressOther = wallet.getAccount(1).toAddressString();

        String message = "helloworld";
        SignMessageResponse response = api.signMessage(address, message);
        assertTrue(response.isSuccess());
        String signature = response.getResult();
        VerifyMessageResponse verifyMessageResponse = api.verifyMessage(address, message, signature);
        assertTrue(verifyMessageResponse.isSuccess());
        assertTrue(verifyMessageResponse.isValidSignature());

        // verify no messing with fromaddress
        verifyMessageResponse = api.verifyMessage(addressOther, message, signature);
        assertTrue(verifyMessageResponse.isSuccess());
        assertFalse(verifyMessageResponse.isValidSignature());

        // verify no messing with message
        verifyMessageResponse = api.verifyMessage(address, message + "other", signature);
        assertTrue(verifyMessageResponse.isSuccess());
        assertFalse(verifyMessageResponse.isValidSignature());
    }

    @Test
    public void transferTest() throws InterruptedException {
        Key key = new Key();
        String value = "1000000000";
        String from = wallet.getAccount(0).toAddressString();
        String to = key.toAddressString();
        String fee = "5432100";
        String nonce = null;
        Boolean validateNonce = null;
        String data = Hex.encode(Bytes.of("test_transfer"));

        DoTransactionResponse response = api.transfer(from, to, value, fee, nonce, validateNonce, data);
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getResult());

        Thread.sleep(200);

        List<PendingManager.PendingTransaction> list = pendingMgr.getPendingTransactions();
        assertFalse(list.isEmpty());
        Transaction tx = list.get(list.size() - 1).transaction;
        assertArrayEquals(tx.getHash(), Hex.decode0x(response.getResult()));
        assertEquals(TRANSFER, tx.getType());
        assertEquals(Amount.Unit.NANO_SEM.of(Long.parseLong(fee)), tx.getFee());
        assertEquals(data, Hex.encode(tx.getData()));
    }

    @Test
    public void transferWithHighNonceTest() {
        Key key = new Key();
        String value = "1000000000";
        String from = wallet.getAccount(0).toAddressString();
        String to = key.toAddressString();
        String fee = "5432100";
        String nonce = "999";
        Boolean validateNonce = null;
        String data = null;

        DoTransactionResponse resp = api.transfer(from, to, value, fee, nonce, validateNonce, data);
        assertTrue(resp.isSuccess());
    }

    @Test(expected = BadRequestException.class)
    public void transferValidateNonceTest() {
        Key key = new Key();
        String value = "1000000000";
        String from = wallet.getAccount(0).toAddressString();
        String to = key.toAddressString();
        String fee = "5432100";
        String nonce = "999";
        Boolean validateNonce = true;
        String data = null;

        api.transfer(from, to, value, fee, nonce, validateNonce, data);
    }

    @Test
    public void transferDefaultFeeTest() throws InterruptedException {
        Key key = new Key();
        String value = "1000000000";
        String from = wallet.getAccount(0).toAddressString();
        String to = key.toAddressString();

        DoTransactionResponse response = api.transfer(from, to, value, null, null, null, null);
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getResult());

        Thread.sleep(200);

        List<PendingManager.PendingTransaction> list = pendingMgr.getPendingTransactions();
        assertFalse(list.isEmpty());
        Transaction tx = list.get(list.size() - 1).transaction;
        assertArrayEquals(tx.getHash(), Hex.decode0x(response.getResult()));
        assertEquals(TRANSFER, tx.getType());
        assertEquals(config.minTransactionFee(), tx.getFee());
    }

    @Test
    public void unvoteTest() throws InterruptedException {
        Key delegate = new Key();
        delegateState.register(delegate.toAddress(), Bytes.of("test_unvote"));

        Amount amount = NANO_SEM.of(1000000000);
        byte[] voter = wallet.getAccounts().get(0).toAddress();
        accountState.adjustLocked(voter, amount);
        delegateState.vote(voter, delegate.toAddress(), amount);

        String from = wallet.getAccount(0).toAddressString();
        String to = delegate.toAddressString();
        String value = String.valueOf(amount.getNano());
        String fee = "50000000";

        DoTransactionResponse response = api.unvote(from, to, value, fee, null, null);
        assertNotNull(response);

        assertTrue(response.isSuccess());
        assertNotNull(response.getResult());

        Thread.sleep(200);

        List<PendingManager.PendingTransaction> list = pendingMgr.getPendingTransactions();
        assertFalse(list.isEmpty());
        assertArrayEquals(list.get(list.size() - 1).transaction.getHash(), Hex.decode0x(response.getResult()));
        assertEquals(UNVOTE, list.get(list.size() - 1).transaction.getType());
    }

    @Test
    public void voteTest() throws InterruptedException {
        Key delegate = new Key();
        delegateState.register(delegate.toAddress(), Bytes.of("test_unvote"));

        Amount amount = NANO_SEM.of(1000000000);
        byte[] voter = wallet.getAccounts().get(0).toAddress();
        accountState.adjustLocked(voter, amount);
        delegateState.vote(voter, delegate.toAddress(), amount);

        String from = wallet.getAccount(0).toAddressString();
        String to = delegate.toAddressString();
        String value = String.valueOf(amount.getNano());
        String fee = String.valueOf(config.minTransactionFee().getNano());

        DoTransactionResponse response = api.vote(from, to, value, fee, null, null);
        assertTrue(response.isSuccess());
        assertNotNull(response.getResult());

        Thread.sleep(200);

        List<PendingManager.PendingTransaction> list = pendingMgr.getPendingTransactions();
        assertFalse(list.isEmpty());
        assertArrayEquals(list.get(list.size() - 1).transaction.getHash(), Hex.decode0x(response.getResult()));
        assertEquals(VOTE, list.get(list.size() - 1).transaction.getType());
    }

    @Test
    public void composeRawTransactionTransferTest() {
        String network = "TESTNET";
        String type = "TRANSFER";
        String to = "0xdb7cadb25fdcdd546fb0268524107582c3f8999c";
        String value = "123456789";
        String fee = String.valueOf(config.minTransactionFee().getNano());
        String nonce = "123";
        String timestamp = "1523028482000";
        String data = Hex.encode0x("test data".getBytes());

        ComposeRawTransactionResponse resp = api.composeRawTransaction(
                network,
                type,
                fee,
                nonce,
                to,
                value,
                timestamp,
                data);

        assertTrue(resp.isSuccess());
        assertEquals(
                "0x010114db7cadb25fdcdd546fb0268524107582c3f8999c00000000075bcd1500000000004c4b40000000000000007b000001629b9257d009746573742064617461",
                resp.getResult());
    }

    @Test
    public void composeRawTransactionDelegateTest() {
        String network = "TESTNET";
        String type = "DELEGATE";
        String to = "";
        String value = "";
        String fee = String.valueOf(config.minTransactionFee().getNano());
        String nonce = "123";
        String timestamp = "1523028482000";
        String data = Hex.encode0x("semux1".getBytes());

        ComposeRawTransactionResponse resp = api.composeRawTransaction(
                network,
                type,
                fee,
                nonce,
                to,
                value,
                timestamp,
                data);

        assertTrue(resp.isSuccess());
        assertEquals(
                "0x0102140000000000000000000000000000000000000000000000e8d4a5100000000000004c4b40000000000000007b000001629b9257d00673656d757831",
                resp.getResult());
    }

    @Test
    public void composeRawTransactionVoteTest() {
        String network = "TESTNET";
        String type = "VOTE";
        String to = "0xdb7cadb25fdcdd546fb0268524107582c3f8999c";
        String value = "123";
        String fee = String.valueOf(config.minTransactionFee().getNano());
        String nonce = "123";
        String timestamp = "1523028482000";
        String data = Hex.encode0x("semux1".getBytes());

        ComposeRawTransactionResponse resp = api.composeRawTransaction(
                network,
                type,
                fee,
                nonce,
                to,
                value,
                timestamp,
                data);

        assertTrue(resp.isSuccess());
        assertEquals(
                "0x010314db7cadb25fdcdd546fb0268524107582c3f8999c000000000000007b00000000004c4b40000000000000007b000001629b9257d00673656d757831",
                resp.getResult());
    }

    @Test
    public void signRawTransactionTest() throws InvalidKeySpecException {
        Key key = new Key(Hex.decode0x(
                "0x302e020100300506032b6570042204207ea3e3e2ce1e2c4e7696f09a252a1b9d58948bc942c0b42092080a896c43649f"));
        kernelRule.getKernel().getWallet().addAccount(key);

        String rawTx = "0x010114db7cadb25fdcdd546fb0268524107582c3f8999c00000000075bcd1500000000004c4b40000000000000007b000001629b9257d009746573742064617461";
        SignRawTransactionResponse resp = api.signRawTransaction(rawTx, key.toAddressString());

        assertTrue(resp.isSuccess());
        assertEquals(
                "0x208ee0cd0b520f9685b2c2219984d31a229419a2c485729faa609291827888601741010114db7cadb25fdcdd546fb0268524107582c3f8999c00000000075bcd1500000000004c4b40000000000000007b000001629b9257d00974657374206461746160a6f2d0fb1a1573e556004420f5281978b7006444a67829c25d00905bafdfb23e941052fbe7112aae9d5b4a790165261f4da347c90e74fd84c316bb38a391d304057f987e38f88037e8019cbb774dda106fc051fc4a6320a00294fe1866d08442",
                resp.getResult());
    }

    @Test
    public void getSyncingProgressStoppedTest() {
        SemuxSync semuxSync = mock(SemuxSync.class);
        when(semuxSync.isRunning()).thenReturn(false);
        kernelRule.getKernel().setSyncManager(semuxSync);

        GetSyncingProgressResponse resp = api.getSyncingProgress();
        SyncingProgressType result = resp.getResult();
        assertTrue(resp.isSuccess());
        assertFalse(result.isSyncing());
        assertNull(result.getStartingHeight());
        assertNull(result.getCurrentHeight());
        assertNull(result.getTargetHeight());
    }

    @Test
    public void getSyncingProgressStartedTest() {
        SemuxSync semuxSync = mock(SemuxSync.class);
        when(semuxSync.isRunning()).thenReturn(true);
        when(semuxSync.getProgress()).thenReturn(new SemuxSync.SemuxSyncProgress(
                1,
                10,
                100,
                Duration.ofSeconds(1000)));
        kernelRule.getKernel().setSyncManager(semuxSync);

        GetSyncingProgressResponse resp = api.getSyncingProgress();
        SyncingProgressType result = resp.getResult();
        assertTrue(resp.isSuccess());
        assertTrue(result.isSyncing());
        assertEquals("1", result.getStartingHeight());
        assertEquals("10", result.getCurrentHeight());
        assertEquals("100", result.getTargetHeight());
    }
}
