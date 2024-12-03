package org.smthn.integration.sqsclient;

import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
public class SqsTestListener {
    private static final Logger logger = LoggerFactory.getLogger(SqsTestListener.class);

    private CountDownLatch messageReceivedLatch;
    private String lastLogMessage;

    public void prepareReception() {
        messageReceivedLatch = new CountDownLatch(1);
    }

    @SqsListener("output-queue")
    public void receiveStringMessage(String logMessage) {
        lastLogMessage = logMessage;
        logger.info("Received message: {}", logMessage);
        messageReceivedLatch.countDown();
    }

    public boolean awaitReceived(int secondsToWait) throws InterruptedException {
        return messageReceivedLatch.await(secondsToWait, TimeUnit.SECONDS);
    }

    public String getLastLogMessage() {
        return lastLogMessage;
    }
}
