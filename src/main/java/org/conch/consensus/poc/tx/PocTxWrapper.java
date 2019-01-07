/*
 *  Copyright © 2017-2018 Sharder Foundation.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  version 2 as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, you can visit it at:
 *  https://www.gnu.org/licenses/old-licenses/gpl-2.0.txt
 *
 *  This software uses third party libraries and open-source programs,
 *  distributed under licenses described in 3RD-PARTY-LICENSES.
 *
 */

package org.conch.consensus.poc.tx;

import org.conch.account.Account;
import org.conch.account.AccountLedger;
import org.conch.common.ConchException;
import org.conch.consensus.poc.PocProcessorImpl;
import org.conch.consensus.poc.PocScore;
import org.conch.tx.Attachment;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionType;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * poc tx series wrapper: validate , parse , apply
 */
public abstract class PocTxWrapper extends TransactionType {

    public static final byte SUBTYPE_POC_NODE_TYPE = 0; // 节点类型
    public static final byte SUBTYPE_POC_NODE_CONF = 1; // 节点配置
    public static final byte SUBTYPE_POC_WEIGHT_TABLE = 2; // 权重
    public static final byte SUBTYPE_POC_ONLINE_RATE = 3; // 在线率
    public static final byte SUBTYPE_POC_BLOCK_MISS = 4; // 出块丢失
    public static final byte SUBTYPE_POC_BC_SPEED = 5; // 分叉收敛


    public static TransactionType findTxType(byte subtype) {
        switch (subtype) {
            case SUBTYPE_POC_NODE_TYPE:
                return POC_NODE_TYPE;
            case SUBTYPE_POC_NODE_CONF:
                return POC_NODE_CONF;
            case SUBTYPE_POC_WEIGHT_TABLE:
                return POC_WEIGHT_TABLE;
            case SUBTYPE_POC_ONLINE_RATE:
                return POC_ONLINE_RATE;
            case SUBTYPE_POC_BLOCK_MISS:
                return POC_BLOCK_MISS;
            case SUBTYPE_POC_BC_SPEED:
                return POC_BC_SPEED;
            default:
                return null;
        }
    }

    private PocTxWrapper() {}


    public static final TransactionType POC_WEIGHT_TABLE = new PocTxWrapper() {

            @Override
            public byte getSubtype() {
                return SUBTYPE_POC_WEIGHT_TABLE;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return null;
            }

            @Override
            public Attachment.AbstractAttachment parseAttachment(
                    ByteBuffer buffer, byte transactionVersion) {
                return Attachment.TxBodyBase.newObj(
                        PocTxBody.PocWeightTable.class, buffer, transactionVersion);
            }

            @Override
            public Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) {
                return Attachment.TxBodyBase.newObj(PocTxBody.PocWeightTable.class, attachmentData);
            }

            @Override
            public void validateAttachment(Transaction transaction)
                    throws ConchException.ValidationException {
                PocTxBody.PocWeightTable pocWeight =
                        (PocTxBody.PocWeightTable) transaction.getAttachment();
                if (pocWeight == null) {
                    throw new ConchException.NotValidException("Invalid PocWeightTable: null");
                }
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                PocTxBody.PocWeightTable pocWeight =  (PocTxBody.PocWeightTable) transaction.getAttachment();
                PocScore.PocCalculator.setCurWeightTable(pocWeight,transaction.getHeight());
            }

            @Override
            public String getName() {
                return "pocWeightTable";
            }
    };
    

    public static final TransactionType POC_NODE_TYPE = new PocTxWrapper() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_POC_NODE_TYPE;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return null;
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            return Attachment.TxBodyBase.newObj(PocTxBody.PocNodeConf.class, buffer,transactionVersion);
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
            return Attachment.TxBodyBase.newObj(PocTxBody.PocNodeConf.class,attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
            PocTxBody.PocNodeType nodeType = (PocTxBody.PocNodeType) transaction.getAttachment();
            if (nodeType == null) {
                throw new ConchException.NotValidException("Invalid pocNodeType: null");
            }
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            PocTxBody.PocNodeType pocNodeType = (PocTxBody.PocNodeType) transaction.getAttachment();

            PocProcessorImpl.nodeTypeTxProcess(transaction.getHeight(),pocNodeType);
        }

        @Override
        public String getName() {
            return "pocNodeType";
        }
    };
    
    public static final TransactionType POC_NODE_CONF = new PocTxWrapper() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_POC_NODE_CONF;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return null;
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            return Attachment.TxBodyBase.newObj(PocTxBody.PocNodeConf.class, buffer,transactionVersion);
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
            return Attachment.TxBodyBase.newObj(PocTxBody.PocNodeConf.class,attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
            // CONF tx need be created by official site
            PocTxBody.PocNodeConf configuration = (PocTxBody.PocNodeConf) transaction.getAttachment();
            if (configuration == null) {
                throw new ConchException.NotValidException("Invalid pocNodeConf: null");
            }
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            PocTxBody.PocNodeConf pocNodeConf = (PocTxBody.PocNodeConf) transaction.getAttachment();
            PocProcessorImpl.nodeConfTxProcess(transaction.getHeight(),pocNodeConf);
        }

        @Override
        public String getName() {
            return "pocNodeConf";
        }
    };

 

    public static final TransactionType POC_ONLINE_RATE = new PocTxWrapper() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_POC_ONLINE_RATE;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return null;
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) {
            return Attachment.TxBodyBase.newObj(PocTxBody.PocOnlineRate.class, buffer, transactionVersion);
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) {
            return Attachment.TxBodyBase.newObj(PocTxBody.PocOnlineRate.class, attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
            PocTxBody.PocOnlineRate pocOnlineRate = (PocTxBody.PocOnlineRate) transaction.getAttachment();
            if (pocOnlineRate == null) {
                throw new ConchException.NotValidException("Invalid pocOnlineRate: null");
            }
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            PocTxBody.PocOnlineRate pocOnlineRate = (PocTxBody.PocOnlineRate) transaction.getAttachment();
            PocProcessorImpl.onlineRateTxProcess(transaction.getHeight(),pocOnlineRate);
        }

        @Override
        public String getName() {
            return "pocOnlineRate";
        }
    };

    public static final TransactionType POC_BLOCK_MISS = new PocTxWrapper() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_POC_BLOCK_MISS;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return null;
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) {
            return Attachment.TxBodyBase.newObj(PocTxBody.PocBlockMiss.class, buffer, transactionVersion);
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) {
            return Attachment.TxBodyBase.newObj(PocTxBody.PocBlockMiss.class, attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
            PocTxBody.PocBlockMiss pocBlockingMiss = (PocTxBody.PocBlockMiss) transaction.getAttachment();
            if (pocBlockingMiss == null) {
                throw new ConchException.NotValidException("Invalid pocBlockMiss: null");
            }
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            PocTxBody.PocBlockMiss pocBlockingMiss = (PocTxBody.PocBlockMiss) transaction.getAttachment();

            PocProcessorImpl.blockMissTxProcess(transaction.getHeight(), pocBlockingMiss);
        }

        @Override
        public String getName() {
            return "pocBlockMiss";
        }
    };

    public static final TransactionType POC_BC_SPEED = new PocTxWrapper() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_POC_BC_SPEED;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return null;
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) {
            return Attachment.TxBodyBase.newObj(PocTxBody.PocBC.class, buffer, transactionVersion);
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) {
            return Attachment.TxBodyBase.newObj(PocTxBody.PocBC.class, attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
            PocTxBody.PocBC pocBc = (PocTxBody.PocBC) transaction.getAttachment();
            if (pocBc == null) {
                throw new ConchException.NotValidException("Invalid pocBcRate: null");
            }

        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            
        }

        @Override
        public String getName() {
            return "pocBcRate";
        }
    };

    @Override
    final public byte getType() {
        return TransactionType.TYPE_POC;
    }

    @Override
    public final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    public final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
    }

    @Override
    final public boolean canHaveRecipient() {
        return false;
    }

    @Override
    final public boolean isPhasingSafe() {
        return true;
    }

}
