package org.uengine.meter.record;

import io.krakens.grok.api.Grok;
import io.krakens.grok.api.GrokCompiler;
import io.krakens.grok.api.Match;
import lombok.Data;
import org.apache.commons.lang3.time.DateUtils;
import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;
import org.uengine.meter.Application;
import org.uengine.meter.record.grok.GrokRedisRepository;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@Measurement(name = "record")
public class Record {

    @Column(name = "user", tag = true)
    private String user;

    @Column(name = "unit", tag = true)
    private String unit;

    @Column(name = "time")
    private Long time;

    @Column(name = "amount")
    private Long amount;

    public Record(String log) {
        //get patterns from redis.
        final GrokRedisRepository grokRedisRepository = Application.getApplicationContext().getBean(GrokRedisRepository.class);
        final List<String> patterns = grokRedisRepository.findAll();
        for (int i = 0; i < patterns.size(); i++) {
            this.match(patterns.get(i), log);
            //if match, break.
            if (this.time != null && this.user != null && this.amount != null && this.unit != null) {
                break;
            }
        }
    }

    public boolean empty() {
        if (this.time == null || this.user == null || this.amount == null || this.unit == null) {
            return true;
        }
        return false;
    }

    public Record(String pattern, String log) {
        this.match(pattern, log);
    }

    private void match(String pattern, String log) {
        final Map<String, Object> capture = this.capture(pattern, log);

        //unit, user, timestamp required.
        //if amount is null, amount is 1 and 'amount-auto-assignment' : true
        try {
            if (capture.containsKey("timestamp")) {
                String timestamp = capture.get("timestamp").toString();
                final long time = DateUtils.parseDate(timestamp,
                        "yyyy-MM-dd'T'HH:mm:ss.SSS",
                        "yyyy-MM-dd'T'HH:mm:ss",
                        "dd.MM.yyyy HH:mm:ss",
                        "dd.MM.yyyy").getTime();
                this.time = time;
            } else {
                this.time = new Date().getTime();
            }
        } catch (ParseException ex) {
            this.time = new Date().getTime();
        }

        if (capture.containsKey("user")) {
            this.user = capture.get("user").toString();
        } else {
            this.user = "anonymous";
        }
        if (capture.containsKey("unit")) {
            this.unit = capture.get("unit").toString();
        }
        if (capture.containsKey("amount")) {
            this.amount = (Long) capture.get("amount");
        } else {
            this.amount = 1L;
        }
    }

    public static Map<String, Object> capture(String pattern, String log) {
        GrokCompiler grokCompiler = GrokCompiler.newInstance();
        grokCompiler.registerDefaultPatterns();

        final Grok grok = grokCompiler.compile(pattern);
        Match gm = grok.match(log);

        /* Get the map with matches */
        return gm.capture();
    }
}
