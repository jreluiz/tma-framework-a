package eubrazil.atmosphere.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import eubr.atmosphere.tma.qualitymodel.entity.HistoricalData;

@Repository
public interface HistoricalDataRepository extends CrudRepository<HistoricalData, Long> {
	
}
