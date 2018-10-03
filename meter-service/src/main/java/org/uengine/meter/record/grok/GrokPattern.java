package org.uengine.meter.record.grok;

import lombok.Data;
import org.uengine.meter.Application;

import javax.persistence.*;

@Entity
@Table(name = "grok_pattern")
@Data
public class GrokPattern {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String pattern;

    @PostUpdate
    @PostPersist
    public void updateCache() {
        final GrokRedisRepository internalService = Application.getApplicationContext().getBean(GrokRedisRepository.class);
        internalService.reset();
    }

    @PostRemove
    public void deleteCache() {
        final GrokRedisRepository internalService = Application.getApplicationContext().getBean(GrokRedisRepository.class);
        internalService.reset();
    }
}
