package org.uengine.meter.rule;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;


@RepositoryRestResource(collectionResourceRel = "units", path = "units")
public interface UnitRepository extends CrudRepository<Unit, Long> {

    @Query("select a from Unit a where a.name like CONCAT('%',:name,'%')")
    List<Unit> findLikeName(@Param("name") String name);

    Unit findByName(@Param("name") String name);
}
