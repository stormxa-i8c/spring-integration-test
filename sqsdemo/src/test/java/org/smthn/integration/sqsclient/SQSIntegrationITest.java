package org.smthn.integration.sqsclient;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.time.Duration;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

/**
 * AWS Integration test using Localstack, a way to emulate AWS services locally
 */
@SpringBootTest
public class SQSIntegrationITest {
    private static final String LOCAL_STACK_VERSION = "localstack/localstack:3.4.0";

    @Container
    static LocalStackContainer localStack = new LocalStackContainer(DockerImageName.parse(LOCAL_STACK_VERSION));

    @Autowired
    private SqsTemplate sqsTemplate;

    @Autowired
    SqsTestListener sqsTestListener;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) throws IOException, InterruptedException {
        localStack.start();
        registry.add("spring.cloud.aws.region.static", () -> localStack.getRegion());
        registry.add("spring.cloud.aws.credentials.access-key", () -> localStack.getAccessKey());
        registry.add("spring.cloud.aws.credentials.secret-key", () -> localStack.getSecretKey());
        registry.add("spring.cloud.aws.sqs.endpoint", () -> localStack.getEndpointOverride(SQS)
                .toString());

        Awaitility.await().atMost(Duration.ofSeconds(3))
                .until(() -> localStack.isRunning());
    }

    @Test
    public void messageSentReceived() throws InterruptedException {
        sqsTestListener.prepareReception();

        sqsTemplate.send(to -> to.queue("input-queue").payload("this is an error"));
        sqsTemplate.send(to -> to.queue("input-queue").payload("This is okay"));

        Assert.assertTrue("Event not received", sqsTestListener.awaitReceived(10));
        Assert.assertNotNull(sqsTestListener.getLastLogMessage());
    }
}
