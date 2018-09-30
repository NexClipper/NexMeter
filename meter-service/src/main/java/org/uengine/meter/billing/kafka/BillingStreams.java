package org.uengine.meter.billing.kafka;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface BillingStreams {
    String INPUT = "kb-consumer";
    String OUTPUT = "kb-producer";

    @Input("kb-consumer")
    SubscribableChannel consumer();

    @Output("kb-producer")
    MessageChannel producer();
}
