/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.dialog;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.LayoutStyle.ComponentPlacement;

import org.ethereum.vm.util.HashUtil;
import org.semux.core.Transaction;
import org.semux.core.TransactionResult;
import org.semux.core.TransactionType;
import org.semux.crypto.Hex;
import org.semux.gui.SwingUtil;
import org.semux.message.GuiMessages;

public class TransactionDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    public TransactionDialog(JFrame parent, Transaction tx, TransactionResult result) {
        super(null, GuiMessages.get("Transaction"), ModalityType.MODELESS);
        setName("TransactionDialog");

        JLabel lblHash = new JLabel(GuiMessages.get("Hash") + ":");
        JLabel lblType = new JLabel(GuiMessages.get("Type") + ":");
        JLabel lblFrom = new JLabel(GuiMessages.get("From") + ":");
        JLabel lblTo = new JLabel(GuiMessages.get("To") + ":");
        JLabel lblValue = new JLabel(GuiMessages.get("Value") + ":");
        JLabel lblFee = new JLabel(GuiMessages.get("Fee") + ":");
        JLabel lblNonce = new JLabel(GuiMessages.get("Nonce") + ":");
        JLabel lblTimestamp = new JLabel(GuiMessages.get("Timestamp") + ":");
        JLabel lblData = new JLabel(GuiMessages.get("Data") + ":");

        JLabel lblSucceeded = new JLabel(GuiMessages.get("Success"));
        JLabel lblGasProvided = new JLabel(GuiMessages.get("GasProvided"));
        JLabel lblGasUsed = new JLabel(GuiMessages.get("GasUsed"));
        JLabel lblOutput = new JLabel(GuiMessages.get("Output"));

        String notAvailable = GuiMessages.get("NotAvailable");

        JTextArea hash = SwingUtil.textAreaWithCopyPopup(Hex.encode0x(tx.getHash()));
        hash.setName("hashText");
        JLabel type = new JLabel(tx.getType().name());
        type.setName("typeText");
        JTextArea from = SwingUtil.textAreaWithCopyPopup(Hex.encode0x(tx.getFrom()));
        from.setName("fromText");
        JTextArea to = SwingUtil.textAreaWithCopyPopup(Hex.encode0x(tx.getTo()));

        // for creates, the TO value should display the contract address
        if (tx.getType() == TransactionType.CREATE) {
            to.setText(Hex.encode0x(HashUtil.calcNewAddress(tx.getFrom(), tx.getNonce())));
        }
        to.setName("toText");
        JLabel value = new JLabel(SwingUtil.formatAmount((tx.getValue())));
        value.setName("valueText");
        JLabel fee = new JLabel(SwingUtil.formatAmount((tx.getFee())));
        fee.setName("feeText");
        JLabel nonce = new JLabel(SwingUtil.formatNumber(tx.getNonce()));
        nonce.setName("nonceText");
        JLabel timestamp = new JLabel(SwingUtil.formatTimestamp(tx.getTimestamp()));
        timestamp.setName("timestampText");

        JLabel gasProvided = new JLabel(SwingUtil.formatNumber(tx.getGas()));
        gasProvided.setName("gasProvidedText");

        JLabel success = new JLabel(result == null ? notAvailable : Boolean.toString(result.getCode().isSuccess()));

        String gasUsedValue = result == null ? notAvailable : SwingUtil.formatNumber(result.getGasUsed());
        JLabel gasUsed = new JLabel(gasUsedValue);
        gasUsed.setName("gasUsedText");

        String outputValue = result == null ? notAvailable : Hex.encode0x(result.getReturnData());
        JTextArea output = SwingUtil.textAreaWithCopyPopup(outputValue);
        output.setName("outputText");
        output.setLineWrap(true);
        JScrollPane outputScroll = new JScrollPane(output);

        JTextArea data = SwingUtil.textAreaWithCopyPopup(Hex.encode0x(tx.getData()));
        data.setName("dataText");
        data.setLineWrap(true);
        JScrollPane dataScroll = new JScrollPane(data);

        // @formatter:off
        GroupLayout groupLayout = new GroupLayout(getContentPane());
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGap(42)
                    .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                        .addComponent(lblOutput)
                        .addComponent(lblGasUsed)
                        .addComponent(lblGasProvided)
                        .addComponent(lblSucceeded)
                        .addComponent(lblData)
                        .addComponent(lblTimestamp)
                        .addComponent(lblNonce)
                        .addComponent(lblFee)
                        .addComponent(lblValue)
                        .addComponent(lblTo)
                        .addComponent(lblFrom)
                        .addComponent(lblType)
                        .addComponent(lblHash))
                    .addGap(18)
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addComponent(hash)
                        .addComponent(type)
                        .addComponent(from)
                        .addComponent(to)
                        .addComponent(value)
                        .addComponent(fee)
                        .addComponent(nonce)
                        .addComponent(timestamp)
                        .addComponent(dataScroll, GroupLayout.PREFERRED_SIZE, 450, GroupLayout.PREFERRED_SIZE)
                        .addComponent(success)
                        .addComponent(gasProvided)
                        .addComponent(gasUsed)
                        .addComponent(outputScroll, GroupLayout.PREFERRED_SIZE, 450, GroupLayout.PREFERRED_SIZE))
                    .addContainerGap(19, Short.MAX_VALUE))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addGap(20)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblHash)
                        .addComponent(hash))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblType)
                        .addComponent(type))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblFrom)
                        .addComponent(from))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblTo)
                        .addComponent(to))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblValue)
                        .addComponent(value))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblFee)
                        .addComponent(fee))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblNonce)
                        .addComponent(nonce))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblTimestamp)
                        .addComponent(timestamp))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblData)
                        .addComponent(dataScroll, GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblSucceeded)
                        .addComponent(success))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                            .addComponent(lblGasProvided)
                            .addComponent(gasProvided))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                            .addComponent(lblGasUsed)
                            .addComponent(gasUsed))
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblOutput)
                        .addComponent(outputScroll, GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE))
                    .addContainerGap(20, Short.MAX_VALUE))
        );
        getContentPane().setLayout(groupLayout);
        // @formatter:on

        this.setTitle(GuiMessages.get("Transaction"));
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setIconImage(SwingUtil.loadImage("logo", 128, 128).getImage());
        this.pack();
        this.setLocationRelativeTo(parent);
        this.setResizable(false);
    }
}
