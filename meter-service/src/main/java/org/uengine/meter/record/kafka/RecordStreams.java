package org.uengine.meter.record.kafka;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface RecordStreams {
    String INPUT = "record-consumer";
    String OUTPUT = "record-producer";

    @Input("record-consumer")
    SubscribableChannel consumer();

    @Output("record-producer")
    MessageChannel producer();
}
