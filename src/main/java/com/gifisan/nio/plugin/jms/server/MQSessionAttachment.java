package com.gifisan.nio.plugin.jms.server;

import com.gifisan.nio.Attachment;

public class MQSessionAttachment implements Attachment {

	private MQContext			context				;
	private TransactionSection	transactionSection		;
	private Consumer			consumer				;

	public MQSessionAttachment(MQContext context) {
		this.context = context;
	}

	public TransactionSection getTransactionSection() {
		return transactionSection;
	}

	public void setTransactionSection(TransactionSection transactionSection) {
		this.transactionSection = transactionSection;
	}

	public MQContext getContext() {
		return context;
	}

	protected Consumer getConsumer() {
		return consumer;
	}

	protected void setConsumer(Consumer consumer) {
		this.consumer = consumer;
	}
	
	

}
