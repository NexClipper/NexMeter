package org.uengine.meter.limit.kafka;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface LimitStreams {
    String INPUT = "limit-consumer";
    String OUTPUT = "limit-producer";

    @Input("limit-consumer")
    SubscribableChannel consumer();

    @Output("limit-producer")
    MessageChannel producer();
}
