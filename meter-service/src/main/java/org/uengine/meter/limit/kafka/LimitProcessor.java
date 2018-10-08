package org.uengine.meter.limit.kafka;

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
import org.uengine.meter.limit.LimitHistory;
import org.uengine.meter.limit.LimitHistoryRepository;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.net.URLDecoder;

@Service
public class LimitProcessor {

    private final Log logger = LogFactory.getLog(getClass());

    @Autowired
    private LimitHistoryRepository limitHistoryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private LimitStreams limitStreams;

    public LimitProcessor(LimitStreams limitStreams) {
        this.limitStreams = limitStreams;
    }


    public void sendLimitMessage(final String limitMessage) {
        try {
            String decode = URLDecoder.decode(limitMessage, "utf-8");
            logger.info("Sending limitMessage : " + decode);

            MessageChannel messageChannel = limitStreams.producer();
            messageChannel.send(MessageBuilder
                    .withPayload(decode)
                    .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON)
                    .build());
        } catch (Exception ex) {

        }
    }

    @StreamListener
    public void receiveLimitMessage(@Input(LimitStreams.INPUT) Flux<String> inbound) {
        inbound
                .log()
                .subscribeOn(Schedulers.parallel())
                .subscribe(value -> {
                    try {
                        logger.info("receive limitMessage : " + value);
                        final LimitHistory limitHistory = objectMapper.readValue(value, LimitHistory.class);
                        limitHistoryRepository.save(limitHistory);
                    } catch (Exception ex) {
                        logger.error("save limitMessage failed");
                    }
                }, error -> System.err.println("CAUGHT " + error));
    }
}


