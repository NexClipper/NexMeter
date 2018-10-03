package org.uengine.meter.record.grok;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;


@RepositoryRestResource(collectionResourceRel = "patterns", path = "patterns")
public interface GrokRepository extends CrudRepository<GrokPattern, Long> {

}
