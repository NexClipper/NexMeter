package org.uengine.meter.record.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.uengine.meter.limit.LimiterService;
import org.uengine.meter.record.Record;
import org.uengine.meter.record.RecordInfluxRepository;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;

@Service
public class RecordProcessor {

    private final Log logger = LogFactory.getLog(getClass());

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RecordInfluxRepository recordInfluxRepository;

    @Autowired
    private LimiterService limiterService;

    private RecordStreams recordStreams;

    public RecordProcessor(RecordStreams recordStreams) {
        this.recordStreams = recordStreams;
    }


    public void sendRecordMessage(final RecordMessage recordMessage) {
        try {

            final String message = objectMapper.writeValueAsString(recordMessage);

            String decode = URLDecoder.decode(message, "utf-8");
            //logger.info("Sending recordMessage : " + decode);

            MessageChannel messageChannel = recordStreams.producer();
            messageChannel.send(MessageBuilder
                    .withPayload(decode)
                    .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON)
                    .build());
        } catch (Exception ex) {

        }
    }

    @StreamListener
    public void receiveRecordMessage(@Input(RecordStreams.INPUT) Flux<String> inbound) {
        inbound
                .log()
                .subscribeOn(Schedulers.parallel())
                .subscribe(value -> {
                    try {
                        //logger.info("receive recordMessage : " + value);
                        final RecordMessage recordMessage = objectMapper.readValue(value, RecordMessage.class);

                        //if log type
                        long count = 0L;
                        if (RecordMessage.RecordMessageType.LOG.equals(recordMessage.getType())) {
                            final String[] split = recordMessage.getMessage().split("\n");
                            count = Arrays.stream(split)
                                    .map(line -> new Record(line))
                                    .filter(record -> record.valid())
                                    .map(record -> recordInfluxRepository.write(record))
                                    .map(record -> limiterService.consume(record, false))
                                    .count();
                        }
                        //if json type
                        if (RecordMessage.RecordMessageType.JSON.equals(recordMessage.getType())) {
                            final List<Record> records = (List<Record>) objectMapper.readValue(recordMessage.getMessage(), new TypeReference<List<Record>>() {
                            });
                            count = records.stream()
                                    .map(record -> record.completeDomain())
                                    .filter(record -> record.valid())
                                    .map(record -> recordInfluxRepository.write(record))
                                    .map(record -> limiterService.consume(record, false))
                                    .count();
                        }
                        logger.info(count + " record processed");

                    } catch (Exception ex) {
                        logger.error("insert Record failed");
                        ex.printStackTrace();
                    } finally {

                    }
                }, error -> System.err.println("CAUGHT " + error));
    }
}


