package org.uengine.meter.record.grok;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class GrokRedisRepository {

    private final Log logger = LogFactory.getLog(getClass());

    @Autowired
    private GrokRepository grokRepository;

    @Autowired
    private RedisTemplate redisTemplate;

    @Cacheable(value = "grok")
    public List<String> findAll() {
        logger.info("find grok from jdbc");
        final Iterable<GrokPattern> grokPatterns = grokRepository.findAll();

        return StreamSupport.stream(grokPatterns.spliterator(), false)
                .map(pattern -> {
                    return pattern.getPattern();
                })
                .collect(Collectors.toList());
    }

    @CacheEvict(value = "grok")
    @Transactional
    public void reset() {
        logger.info("reset grok cache");
    }
}
