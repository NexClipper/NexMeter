package org.uengine.meter.limit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.uengine.meter.rule.Unit;

import java.util.List;


@RepositoryRestResource(collectionResourceRel = "limits", path = "limits")
public interface LimitHistoryRepository extends PagingAndSortingRepository<LimitHistory, Long> {

    Page<LimitHistory> findByUser(@Param("user") String user, Pageable pageable);

    Page<LimitHistory> findByUnit(@Param("unit") String unit, Pageable pageable);

    Page<LimitHistory> findByUnitAndUser(@Param("unit") String unit, @Param("user") String user, Pageable pageable);
}

