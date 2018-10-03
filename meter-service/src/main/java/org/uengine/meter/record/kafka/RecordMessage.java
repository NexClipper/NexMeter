package org.uengine.meter.record.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RecordMessage {
    private RecordMessageType type;
    private String message;

    public static enum RecordMessageType {
        JSON, LOG
    }
}
