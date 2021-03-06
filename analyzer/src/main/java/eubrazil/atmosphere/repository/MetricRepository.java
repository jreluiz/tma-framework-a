package eubrazil.atmosphere.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import eubr.atmosphere.tma.entity.qualitymodel.Metric;

/**
 * Dashboard services
 * @author Jorge Luiz
 */
@Repository
public interface MetricRepository extends CrudRepository<Metric, Long> {

	@Override
	Iterable<Metric> findAll();
	
}