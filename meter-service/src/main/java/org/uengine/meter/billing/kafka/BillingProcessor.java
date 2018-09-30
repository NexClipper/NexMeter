package org.uengine.meter.billing.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.uengine.meter.billing.BillingController;
import org.uengine.meter.billing.BillingService;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Map;

@Service
public class BillingProcessor {

    private final Log logger = LogFactory.getLog(getClass());

    @Autowired
    private BillingService billingService;

    @Autowired
    private BillingConfig billingConfig;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BillingController billingController;

    private BillingStreams billingStreams;

    public BillingProcessor(BillingStreams billingStreams) {
        this.billingStreams = billingStreams;
    }


    public void sendBillingMessage(final String billingMessage) {
        try {
            String decode = URLDecoder.decode(billingMessage, "utf-8");
            logger.info("Sending billingMessage : " + decode);

            MessageChannel messageChannel = billingStreams.producer();
            messageChannel.send(MessageBuilder
                    .withPayload(decode)
                    .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON)
                    .build());
        } catch (Exception ex) {

        }
    }

    @StreamListener
    public void receiveBillingMessage(@Input(BillingStreams.INPUT) Flux<String> inbound) {
        inbound
                .log()
                .subscribeOn(Schedulers.parallel())
                .subscribe(value -> {
                    try {
                        logger.info("receive BillingMessage : " + value);
                        Map map = new ObjectMapper().readValue(value, Map.class);
                        String eventType = map.get("eventType").toString();
                        String accountId = map.get("accountId").toString();

                        String[] acceptTypes = new String[]{
                                "ACCOUNT_CREATION",
                                "ACCOUNT_CHANGE",
                                "SUBSCRIPTION_CREATION",
                                "SUBSCRIPTION_PHASE",
                                "SUBSCRIPTION_CHANGE",
                                "SUBSCRIPTION_CANCEL",
                                "SUBSCRIPTION_UNCANCEL",
                                "SUBSCRIPTION_BCD_CHANGE"
                        };

                        if (Arrays.asList(acceptTypes).contains(eventType)) {
                            billingService.updateUserSubscriptionsByAccountId(accountId);
                        }
                    } catch (Exception ex) {
                        logger.error("update UserSubscriptions failed");
                    } finally {
                        try {
                            Map map = new ObjectMapper().readValue(value, Map.class);
                            billingController.emitterSend(map);
                        } catch (Exception ex) {
                            //Nothing, just notification
                        }
                    }
                }, error -> System.err.println("CAUGHT " + error));
    }
}


